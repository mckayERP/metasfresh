package de.metas.adempiere.service.impl;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.bpartner.service.IBPartnerDAO;
import org.adempiere.document.service.IDocActionBL;
import org.adempiere.document.service.IDocTypeBL;
import org.adempiere.model.GridTabWrapper;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.pricing.api.IEditablePricingContext;
import org.adempiere.pricing.api.IPriceListDAO;
import org.adempiere.pricing.api.IPricingBL;
import org.adempiere.pricing.api.IPricingContext;
import org.adempiere.pricing.api.IPricingResult;
import org.adempiere.pricing.exceptions.ProductNotOnPriceListException;
import org.adempiere.uom.api.IUOMConversionBL;
import org.adempiere.util.Check;
import org.adempiere.util.LegacyAdapters;
import org.adempiere.util.Pair;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BPartner_Location;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_Tax;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_PriceList_Version;
import org.compiere.model.I_M_Shipper;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MTax;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import de.metas.adempiere.model.I_M_Product;
import de.metas.adempiere.service.IOrderBL;
import de.metas.adempiere.service.IOrderLineBL;
import de.metas.interfaces.I_C_OrderLine;
import de.metas.tax.api.ITaxBL;

public class OrderLineBL implements IOrderLineBL
{

	private static final CLogger logger = CLogger.getCLogger(OrderLineBL.class);
	public static final String SYSCONFIG_CountryAttribute = "de.metas.swat.CountryAttribute";

	private final Set<Integer> ignoredOlIds = new HashSet<Integer>();

	public static final String CTX_EnforcePriceLimit = "EnforcePriceLimit";
	public static final String CTX_DiscountSchema = "DiscountSchema";

	// task 08002
	public static final String DYNATTR_DoNotRecalculatePrices = IOrderLineBL.class.getName() + "#DoNotRecalcualtePrices";

	@Override
	public void setPricesIfNotIgnored(final Properties ctx,
			final I_C_OrderLine orderLine,
			final int priceListId,
			final BigDecimal priceQty,
			final BigDecimal factor,
			boolean usePriceUOM,
			final String trxName_NOTUSED)
	{
		// FIXME refator and/or keep in sync with #updatePrices

		if (ignoredOlIds.contains(orderLine.getC_OrderLine_ID()))
		{
			return;
		}

		final int productId = orderLine.getM_Product_ID();
		final int bPartnerId = orderLine.getC_BPartner_ID();
		if (productId <= 0 || bPartnerId <= 0)
		{
			return;
		}

		//
		// Calculate Pricing Result
		final IEditablePricingContext pricingCtx = createPricingContext(orderLine, priceListId, priceQty);
		pricingCtx.setConvertPriceToContextUOM(!usePriceUOM);
		final IPricingResult pricingResult = Services.get(IPricingBL.class).calculatePrice(pricingCtx);
		if (!pricingResult.isCalculated())
		{
			throw new ProductNotOnPriceListException(pricingCtx, orderLine.getLine());
		}

		//
		// PriceList
		final BigDecimal priceListStdOld = orderLine.getPriceList_Std();
		final BigDecimal priceList = pricingResult.getPriceList();
		orderLine.setPriceList_Std(priceList);
		if (priceListStdOld.compareTo(priceList) != 0)
		{
			orderLine.setPriceList(priceList);
		}

		//
		// PriceLimit, PriceStd, Price_UOM_ID
		orderLine.setPriceLimit(pricingResult.getPriceLimit());
		orderLine.setPriceStd(pricingResult.getPriceStd());
		orderLine.setPrice_UOM_ID(pricingResult.getPrice_UOM_ID()); // 07090: when setting a priceActual, we also need to specify a PriceUOM

		//
		// Set PriceEntered
		if (orderLine.getPriceEntered().signum() == 0 && !orderLine.isManualPrice()) // task 06727
		{
			// priceEntered is not set, so set it from the PL
			orderLine.setPriceEntered(pricingResult.getPriceStd());
		}

		//
		// Discount
		if (orderLine.getDiscount().signum() == 0 && !orderLine.isManualDiscount())  // task 06727
		{
			// pp.getDiscount is the discount between priceList and priceStd
			// -> useless for us
			// metas: Achtung, Rabatt wird aus PriceList und PriceStd ermittelt, nicht aus Discount Schema
			orderLine.setDiscount(pricingResult.getDiscount());
		}

		//
		// Calculate PriceActual from PriceEntered and Discount
		if (orderLine.getPriceActual().signum() == 0)
		{
			calculatePriceActual(orderLine, pricingResult.getPrecision());
		}

		//
		// C_Currency_ID, Price_UOM_ID(again?), M_PriceList_Version_ID
		orderLine.setC_Currency_ID(pricingResult.getC_Currency_ID());
		orderLine.setPrice_UOM_ID(pricingResult.getPrice_UOM_ID()); // task 06942
		orderLine.setM_PriceList_Version_ID(pricingResult.getM_PriceList_Version_ID());

		updateLineNetAmt(orderLine, priceQty, factor);
	}

	@Override
	public void setTaxAmtInfoIfNotIgnored(final Properties ctx, final I_C_OrderLine ol, final String trxName)
	{
		if (ignoredOlIds.contains(ol.getC_OrderLine_ID()))
		{
			return;
		}

		final int taxId = ol.getC_Tax_ID();
		final boolean taxIncluded = isTaxIncluded(ol);
		final BigDecimal lineAmout = ol.getLineNetAmt();
		final int taxPrecision = getPrecision(ol);

		final I_C_Tax tax = MTax.get(ctx, taxId);

		final ITaxBL taxBL = Services.get(ITaxBL.class);
		final BigDecimal taxAmtInfo = taxBL.calculateTax(tax, lineAmout, taxIncluded, taxPrecision);

		ol.setTaxAmtInfo(taxAmtInfo);
	}

	@Override
	public void setPrices(final I_C_OrderLine ol)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
		final String trxName = InterfaceWrapperHelper.getTrxName(ol);
		final boolean usePriceUOM = false;
		setPricesIfNotIgnored(ctx, ol, usePriceUOM, trxName);
	}

	@Override
	public void setPricesIfNotIgnored(final Properties ctx, final I_C_OrderLine ol, boolean usePriceUOM, final String trxName)
	{
		if (ignoredOlIds.contains(ol.getC_OrderLine_ID()))
		{
			return;
		}
		final org.compiere.model.I_C_Order order = ol.getC_Order();

		final int productId = ol.getM_Product_ID();

		final int priceListId = order.getM_PriceList_ID();

		if (priceListId <= 0 || productId <= 0)
		{
			return;
		}

		// 06278 : If we have a UOM in product price, we use that one.
		setPricesIfNotIgnored(ctx, ol, priceListId, ol.getQtyOrdered(), BigDecimal.ONE, usePriceUOM, trxName);
	}

	@Override
	public void setShipperIfNotIgnored(final Properties ctx, final I_C_OrderLine ol, final boolean force, final String trxName)
	{
		if (ignoredOlIds.contains(ol.getC_OrderLine_ID()))
		{
			return;
		}

		final org.compiere.model.I_C_Order order = ol.getC_Order();

		if (!force && ol.getM_Shipper_ID() > 0)
		{
			logger.fine("Nothing to do: force=false and M_Shipper_ID=" + ol.getM_Shipper_ID());
			return;
		}

		final int orderShipperId = order.getM_Shipper_ID();
		if (orderShipperId > 0)
		{
			logger.info("Setting M_Shipper_ID=" + orderShipperId + " from " + order);
			ol.setM_Shipper_ID(orderShipperId);
		}
		else
		{
			logger.fine("Looking for M_Shipper_ID via ship-to-bpartner of " + order);

			final int bPartnerID = order.getC_BPartner_ID();
			if (bPartnerID <= 0)
			{
				logger.warning(order + " has no ship-to-bpartner");
				return;
			}

			final I_M_Shipper shipper = Services.get(IBPartnerDAO.class).retrieveShipper(bPartnerID, null);
			if (shipper == null)
			{
				// task 07034: nothing to do
				return;
			}

			final int bPartnerShipperId = shipper.getM_Shipper_ID();

			logger.info("Setting M_Shipper_ID=" + bPartnerShipperId + " from ship-to-bpartner");
			ol.setM_Shipper_ID(bPartnerShipperId);
		}
	}

	@Override
	public void calculatePriceActualIfNotIgnored(final I_C_OrderLine ol, final int precision)
	{
		if (ignoredOlIds.contains(ol.getC_OrderLine_ID()))
		{
			return;
		}
		calculatePriceActual(ol, precision);
	}

	@Override
	public BigDecimal subtractDiscount(final BigDecimal baseAmount, final BigDecimal discount, final int precision)
	{
		BigDecimal multiplier = Env.ONEHUNDRED.subtract(discount);
		multiplier = multiplier.divide(Env.ONEHUNDRED, precision * 3, RoundingMode.HALF_UP);

		final BigDecimal result = baseAmount.multiply(multiplier).setScale(precision, RoundingMode.HALF_UP);
		return result;
	}

	@Override
	public void ignore(final int orderLineId)
	{
		ignoredOlIds.add(orderLineId);
	}

	@Override
	public void unignore(final int orderLineId)
	{
		ignoredOlIds.remove(orderLineId);
	}

	@Override
	public int getC_TaxCategory_ID(final org.compiere.model.I_C_OrderLine orderLine)
	{
		// In case we have a charge, use the tax category from charge
		if (orderLine.getC_Charge_ID() > 0)
		{
			return orderLine.getC_Charge().getC_TaxCategory_ID();
		}

		final IPricingContext pricingCtx = createPricingContext(orderLine);
		final IPricingResult pricingResult = Services.get(IPricingBL.class).calculatePrice(pricingCtx);
		if (!pricingResult.isCalculated())
		{
			return -1;
		}
		return pricingResult.getC_TaxCategory_ID();
	}

	@Override
	public IEditablePricingContext createPricingContext(org.compiere.model.I_C_OrderLine orderLine)
	{
		final I_C_Order order = orderLine.getC_Order();
		final int priceListId = order.getM_PriceList_ID();

		final BigDecimal priceQty = calculateQtyEnteredInPriceUOM(orderLine);

		return createPricingContext(orderLine, priceListId, priceQty);
	}

	public IEditablePricingContext createPricingContext(org.compiere.model.I_C_OrderLine orderLine,
			final int priceListId,
			final BigDecimal priceQty
			)
	{
		final org.compiere.model.I_C_Order order = orderLine.getC_Order();

		final boolean isSOTrx = order.isSOTrx();

		final int productId = orderLine.getM_Product_ID();

		int bPartnerId = orderLine.getC_BPartner_ID();
		if (bPartnerId <= 0)
		{
			bPartnerId = order.getC_BPartner_ID();
		}

		final Timestamp date = getPriceDate(orderLine, order);

		final I_C_OrderLine ol = InterfaceWrapperHelper.create(orderLine, I_C_OrderLine.class);

		final IEditablePricingContext pricingCtx = Services.get(IPricingBL.class).createInitialContext(
				productId,
				bPartnerId,
				ol.getPrice_UOM_ID(), // task 06942
				priceQty,
				isSOTrx);
		pricingCtx.setPriceDate(date);

		// 03152: setting the 'ol' to allow the subscription system to compute the right price
		pricingCtx.setReferencedObject(orderLine);

		pricingCtx.setM_PriceList_ID(priceListId);
		// PLV is only accurate if PL selected in header
		// metas: relay on M_PriceList_ID only, don't use M_PriceList_Version_ID
		// pricingCtx.setM_PriceList_Version_ID(orderLine.getM_PriceList_Version_ID());

		return pricingCtx;
	}

	/**
	 * task 07080
	 * 
	 * @param orderLine
	 * @param order
	 * @return
	 */
	private Timestamp getPriceDate(final org.compiere.model.I_C_OrderLine orderLine,
			final org.compiere.model.I_C_Order order)
	{
		Timestamp date = orderLine.getDatePromised();
		// if null, then get date promised from order
		if (date == null)
		{
			date = order.getDatePromised();
		}
		// still null, then get date ordered from order line
		if (date == null)
		{
			date = orderLine.getDateOrdered();
		}
		// still null, then get date ordered from order
		if (date == null)
		{
			date = order.getDateOrdered();
		}
		return date;
	}

	@Override
	public I_C_OrderLine createOrderLine(final org.compiere.model.I_C_Order order)
	{
		final MOrderLine olPO = new MOrderLine((MOrder)LegacyAdapters.convertToPO(order));

		final I_C_OrderLine ol = InterfaceWrapperHelper.create(olPO, I_C_OrderLine.class);

		if (order.isSOTrx() && order.isDropShip())
		{
			int C_BPartner_ID = order.getDropShip_BPartner_ID() > 0 ? order.getDropShip_BPartner_ID() : order.getC_BPartner_ID();
			ol.setC_BPartner_ID(C_BPartner_ID);

			final I_C_BPartner_Location deliveryLocation = Services.get(IOrderBL.class).getShipToLocation(order);
			int C_BPartner_Location_ID = deliveryLocation != null ? deliveryLocation.getC_BPartner_Location_ID() : -1;

			ol.setC_BPartner_Location_ID(C_BPartner_Location_ID);

			int AD_User_ID = order.getDropShip_User_ID() > 0 ? order.getDropShip_User_ID() : order.getAD_User_ID();
			ol.setAD_User_ID(AD_User_ID);
		}
		return ol;
	}

	@Override
	public <T extends I_C_OrderLine> T createOrderLine(org.compiere.model.I_C_Order order, Class<T> orderLineClass)
	{
		final I_C_OrderLine orderLine = createOrderLine(order);
		return InterfaceWrapperHelper.create(orderLine, orderLineClass);
	}

	@Override
	public void updateLineNetAmt(final I_C_OrderLine ol, final BigDecimal qtyEntered, final BigDecimal factor)
	{
		if (qtyEntered != null)
		{
			final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
			final I_C_Order order = ol.getC_Order();
			final int priceListId = order.getM_PriceList_ID();

			//
			// We need to get the quantity in the pricing's UOM (if different)
			final BigDecimal convertedQty = calculateQtyEnteredInPriceUOM(ol);

			// this code has been borrowed from
			// org.compiere.model.CalloutOrder.amt
			final int stdPrecision = MPriceList.getStandardPrecision(ctx, priceListId);

			BigDecimal lineNetAmt = convertedQty.multiply(factor.multiply(ol.getPriceActual()));

			if (lineNetAmt.scale() > stdPrecision)
			{
				lineNetAmt = lineNetAmt.setScale(stdPrecision, BigDecimal.ROUND_HALF_UP);
			}
			logger.info("LineNetAmt=" + lineNetAmt);
			ol.setLineNetAmt(lineNetAmt);
		}
	}

	@Override
	public void updatePrices(final I_C_OrderLine orderLine)
	{
		// FIXME refator and/or keep in sync with #setPricesIfNotIgnored

		// Product was not set yet. There is no point to calculate the prices
		if (orderLine.getM_Product_ID() <= 0)
		{
			return;
		}

		//
		// Calculate Pricing Result
		final IEditablePricingContext pricingCtx = createPricingContext(orderLine);
		final boolean userPriceUOM = InterfaceWrapperHelper.isNew(orderLine);
		pricingCtx.setConvertPriceToContextUOM(!userPriceUOM);
		final IPricingResult pricingResult = Services.get(IPricingBL.class).calculatePrice(pricingCtx);
		if (!pricingResult.isCalculated())
		{
			throw new ProductNotOnPriceListException(pricingCtx, orderLine.getLine());
		}

		//
		// PriceList
		final BigDecimal priceListStdOld = orderLine.getPriceList_Std();
		final BigDecimal priceList = pricingResult.getPriceList();
		orderLine.setPriceList_Std(priceList);
		if (priceListStdOld.compareTo(priceList) != 0)
		{
			orderLine.setPriceList(priceList);
		}

		//
		// PriceLimit, PriceStd, Price_UOM_ID
		orderLine.setPriceLimit(pricingResult.getPriceLimit());
		orderLine.setPriceStd(pricingResult.getPriceStd());
		orderLine.setPrice_UOM_ID(pricingResult.getPrice_UOM_ID()); // 07090: when setting a priceActual, we also need to specify a PriceUOM

		//
		// Set PriceEntered and PriceActual only if IsManualPrice=N
		if (!orderLine.isManualPrice())
		{
			orderLine.setPriceEntered(pricingResult.getPriceStd());
			orderLine.setPriceActual(pricingResult.getPriceStd());
		}

		//
		// Discount
		// NOTE: Subscription prices do not work with Purchase Orders.
		if (pricingCtx.isSOTrx())
		{
			if (!orderLine.isManualDiscount())
			{
				// Override discount only if is not manual
				// Note: only the sales order widnow has the field 'isManualDiscount'
				orderLine.setDiscount(pricingResult.getDiscount());
			}
		}
		else
		{
			orderLine.setDiscount(pricingResult.getDiscount());
		}

		//
		// Calculate PriceActual from PriceEntered and Discount
		calculatePriceActual(orderLine, pricingResult.getPrecision());

		//
		// C_Currency_ID, Price_UOM_ID(again?), M_PriceList_Version_ID
		orderLine.setC_Currency_ID(pricingResult.getC_Currency_ID());
		orderLine.setPrice_UOM_ID(pricingResult.getPrice_UOM_ID()); // task 06942
		orderLine.setM_PriceList_Version_ID(pricingResult.getM_PriceList_Version_ID());

		//
		// UI
		final Properties ctx = InterfaceWrapperHelper.getCtx(orderLine);
		final int WindowNo = GridTabWrapper.getWindowNo(orderLine);
		Env.setContext(ctx, WindowNo, CTX_EnforcePriceLimit, pricingResult.isEnforcePriceLimit());
		Env.setContext(ctx, WindowNo, CTX_DiscountSchema, pricingResult.isUsesDiscountSchema());
	}

	@Override
	public void updateQtyReserved(final I_C_OrderLine orderLine)
	{
		if (orderLine == null)
		{
			logger.log(Level.FINE, "Given orderLine is NULL; returning");
			return; // not our business
		}

		// make two simple checks that work without loading additional stuff
		if (orderLine.getM_Product_ID() <= 0)
		{
			logger.log(Level.FINE, "Given orderLine {0} has M_Product_ID<=0; setting QtyReserved=0.", orderLine);
			orderLine.setQtyReserved(BigDecimal.ZERO);
			return;
		}
		if (orderLine.getQtyOrdered().signum() <= 0)
		{
			logger.log(Level.FINE, "Given orderLine {0} has QtyOrdered<=0; setting QtyReserved=0.", orderLine);
			orderLine.setQtyReserved(BigDecimal.ZERO);
			return;
		}

		final IDocTypeBL docTypeBL = Services.get(IDocTypeBL.class);
		final IDocActionBL docActionBL = Services.get(IDocActionBL.class);

		final I_C_Order order = orderLine.getC_Order();

		if (!docActionBL.isStatusOneOf(order,
				DocAction.STATUS_InProgress, DocAction.STATUS_Completed, DocAction.STATUS_Closed))
		{
			logger.log(Level.FINE, "C_Order {0} of given orderLine {1} has DocStatus {2}; setting QtyReserved=0.",
					new Object[] { order, orderLine, order.getDocStatus() });
			orderLine.setQtyReserved(BigDecimal.ZERO);
			return;
		}
		if (order.getC_DocType_ID() > 0 && docTypeBL.isProposal(order.getC_DocType()))
		{
			logger.log(Level.FINE, "C_Order {0} of given orderLine {1} has C_DocType {2} which is a proposal; setting QtyReserved=0.",
					new Object[] { order, orderLine, order.getC_DocType() });
			orderLine.setQtyReserved(BigDecimal.ZERO);
			return;
		}

		if (!orderLine.getM_Product().isStocked())
		{
			logger.log(Level.FINE, "Given orderLine {0} has M_Product {1} which is not stocked; setting QtyReserved=0.",
					new Object[] { orderLine, orderLine.getM_Product() });
			orderLine.setQtyReserved(BigDecimal.ZERO);
			return;
		}

		final BigDecimal qtyReservedRaw = orderLine.getQtyOrdered().subtract(orderLine.getQtyDelivered());
		final BigDecimal qtyReserved = BigDecimal.ZERO.max(qtyReservedRaw); // not less than zero

		logger.log(Level.FINE, "Given orderLine {0} has QtyOrdered={1} and QtyDelivered={2}; setting QtyReserved={3}.",
				new Object[] { orderLine, orderLine.getQtyOrdered(), orderLine.getQtyDelivered(), qtyReserved });
		orderLine.setQtyReserved(qtyReserved);
	}

	@Override
	public void calculatePriceActual(final I_C_OrderLine orderLine, final int precision)
	{
		final BigDecimal discount = orderLine.getDiscount();
		final BigDecimal priceEntered = orderLine.getPriceEntered();

		BigDecimal priceActual;
		if (priceEntered.signum() == 0)
		{
			priceActual = priceEntered;
		}
		else
		{
			final int precisionToUse;
			if (precision >= 0)
			{
				precisionToUse = precision;
			}
			else
			{
				// checks to avoid unexplained NPEs
				Check.errorIf(orderLine.getC_Order_ID() <= 0, "Optional 'precision' param was not set but param 'orderLine' {0} has no order", orderLine);
				final I_C_Order order = orderLine.getC_Order();
				Check.errorIf(order.getM_PriceList_ID() <= 0, "Optional 'precision' param was not set but the order of param 'orderLine' {0} has no price list", orderLine);

				precisionToUse = order.getM_PriceList().getPricePrecision();
			}
			priceActual = subtractDiscount(priceEntered, discount, precisionToUse);
		}

		orderLine.setPriceActual(priceActual);
	}

	@Override
	public void setM_Product_ID(final I_C_OrderLine orderLine, int M_Product_ID, boolean setUOM)
	{
		if (setUOM)
		{
			final Properties ctx = InterfaceWrapperHelper.getCtx(orderLine);
			final I_M_Product product = InterfaceWrapperHelper.create(ctx, M_Product_ID, I_M_Product.class, ITrx.TRXNAME_None);
			orderLine.setM_Product(product);
			orderLine.setC_UOM_ID(product.getC_UOM_ID());
		}
		else
		{
			orderLine.setM_Product_ID(M_Product_ID);
		}
		orderLine.setM_AttributeSetInstance_ID(0);
	}	// setM_Product_ID

	@Override
	public I_M_PriceList_Version getPriceListVersion(I_C_OrderLine orderLine)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(orderLine);
		final String trxName = InterfaceWrapperHelper.getTrxName(orderLine);

		if (orderLine.getM_PriceList_Version_ID() > 0)
		{
			return InterfaceWrapperHelper.create(ctx, orderLine.getM_PriceList_Version_ID(), I_M_PriceList_Version.class, trxName);
		}
		else
		{
			// If the line doesn't have a pricelist version, take the one from order.
			final I_C_Order order = orderLine.getC_Order();

			return Services.get(IPriceListDAO.class).retrievePriceListVersion(order.getM_PriceList(), getPriceDate(orderLine, order));

		}
	}

	@Override
	public BigDecimal calculateQtyEnteredInPriceUOM(final org.compiere.model.I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(orderLine, "orderLine not null");

		final BigDecimal qtyEntered = orderLine.getQtyEntered();

		final I_C_OrderLine orderLineToUse = InterfaceWrapperHelper.create(orderLine, I_C_OrderLine.class);
		final BigDecimal qtyInPriceUOM = convertToPriceUOM(qtyEntered, orderLineToUse);
		return qtyInPriceUOM;
	}

	@Override
	public BigDecimal calculateQtyOrdered(final org.compiere.model.I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(orderLine, "orderLine not null");

		final IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);
		final Properties ctx = InterfaceWrapperHelper.getCtx(orderLine);

		final BigDecimal qtyEntered = orderLine.getQtyEntered();

		if (orderLine.getM_Product_ID() <= 0 || orderLine.getC_UOM_ID() <= 0)
		{
			return qtyEntered;
		}

		final BigDecimal qtyOrdered = uomConversionBL.convertToProductUOM(ctx, orderLine.getM_Product(), orderLine.getC_UOM(), qtyEntered);
		return qtyOrdered;
	}

	private BigDecimal convertToPriceUOM(final BigDecimal qty, final I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(qty, "qty not null");

		final I_C_UOM qtyUOM = orderLine.getC_UOM();
		if (qtyUOM == null || qtyUOM.getC_UOM_ID() <= 0)
		{
			return qty;
		}

		final I_C_UOM priceUOM = orderLine.getPrice_UOM();
		if (priceUOM == null || priceUOM.getC_UOM_ID() <= 0)
		{
			return qty;
		}

		if (qtyUOM.getC_UOM_ID() == priceUOM.getC_UOM_ID())
		{
			return qty;
		}

		final org.compiere.model.I_M_Product product = orderLine.getM_Product();
		final BigDecimal qtyInPriceUOM = Services.get(IUOMConversionBL.class).convertQty(product, qty, qtyUOM, priceUOM);
		return qtyInPriceUOM;
	}

	@Override
	public <T extends org.compiere.model.I_C_OrderLine> IQueryFilter<Pair<T, T>> createSOLineToPOLineCopyHandlerFilter()
	{
		return new IQueryFilter<Pair<T, T>>()
		{
			@Override
			public boolean accept(final Pair<T, T> model)
			{
				Check.assumeNotNull(model, "Param 'model not null");

				if (model.getFirst() == null || model.getSecond() == null)
				{
					// nothing to do if we don't have both a copy source and destination
					return false;
				}

				final T orderLineFrom = model.getFirst(); // 'orderLineFrom' is the original sales order line
				final T orderLineTo = model.getSecond(); // 'orderLineTo' is the newly created purchase order line

				if (orderLineTo.getLink_OrderLine_ID() != orderLineFrom.getC_OrderLine_ID())
				{
					// nothing to do if not linked orderLine
					return false;
				}

				final I_C_Order orderFrom = orderLineFrom.getC_Order();
				Check.assumeNotNull(orderFrom, "C_Order not set in {0}" + orderLineTo);
				if (!orderFrom.isSOTrx())
				{
					// nothing to do if our source is a purchase order
					return false;
				}

				final I_C_Order orderTo = orderLineTo.getC_Order();
				Check.assumeNotNull(orderLineTo, "C_Order not set in {0}", orderLineFrom);
				if (orderTo.isSOTrx())
				{
					// nothing to do if our destination is a sales order
					return false;
				}

				return true;
			}
		};
	}

	@Override
	public boolean isTaxIncluded(final org.compiere.model.I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(orderLine, "orderLine not null");

		final I_C_Tax tax = orderLine.getC_Tax();

		final org.compiere.model.I_C_Order order = orderLine.getC_Order();
		return Services.get(IOrderBL.class).isTaxIncluded(order, tax);
	}

	@Override
	public int getPrecision(final org.compiere.model.I_C_OrderLine orderLine)
	{
		final org.compiere.model.I_C_Order order = orderLine.getC_Order();
		return Services.get(IOrderBL.class).getPrecision(order);
	}

}