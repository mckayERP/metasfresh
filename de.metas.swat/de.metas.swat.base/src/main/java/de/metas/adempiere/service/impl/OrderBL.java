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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.ad.dao.IQueryAggregateBuilder;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.persistence.ModelDynAttributeAccessor;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.bpartner.service.IBPartnerBL;
import org.adempiere.bpartner.service.IBPartnerDAO;
import org.adempiere.misc.service.IPOService;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.model.MFreightCost;
import org.adempiere.model.POWrapper;
import org.adempiere.order.service.IOrderPA;
import org.adempiere.pricing.api.IPriceListDAO;
import org.adempiere.product.service.IProductPA;
import org.adempiere.uom.api.IUOMConversionBL;
import org.adempiere.uom.api.IUOMConversionContext;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.api.IMsgBL;
import org.adempiere.util.collections.ListUtils;
import org.adempiere.util.proxy.Cached;
import org.compiere.model.IQuery;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_BP_Relation;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderTax;
import org.compiere.model.I_C_Tax;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_PriceList;
import org.compiere.model.I_M_PriceList_Version;
import org.compiere.model.I_M_Product;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPricingSystem;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import de.metas.adempiere.model.I_C_BPartner_Location;
import de.metas.adempiere.service.IOrderBL;
import de.metas.adempiere.service.IOrderDAO;
import de.metas.adempiere.util.CacheCtx;
import de.metas.adempiere.util.CacheTrx;
import de.metas.freighcost.api.IFreightCostBL;
import de.metas.interfaces.I_C_BPartner;
import de.metas.interfaces.I_C_OrderLine;

public class OrderBL implements IOrderBL
{
	public static final String MSG_NO_FREIGHT_COST_DETAIL = "freightCost.Order.noFreightCostDetail";
	private static final String MSG_NO_PO_PRICELIST_FOUND = "NoPOPriceListFound";
	private static final String MSG_NO_SO_PRICELIST_FOUND = "NoSOPriceListFound";

	private static final CLogger logger = CLogger.getCLogger(OrderBL.class);

	@Override
	public String checkFreightCost(final Properties ctx, final I_C_Order order, final boolean nullIfOk, final String trxName)
	{
		if (!order.isSOTrx())
		{
			OrderBL.logger.log(Level.FINE, "{0} is no SO", order);
			return nullIfOk ? null : "";
		}

		final int bPartnerId = order.getC_BPartner_ID();
		final int bPartnerLocationId = order.getC_BPartner_Location_ID();
		final int shipperId = order.getM_Shipper_ID();

		if (bPartnerId == 0 || bPartnerLocationId == 0 || shipperId == 0)
		{
			OrderBL.logger.fine("Can't check cause freight cost info is not yet complete for " + order);
			return nullIfOk ? null : "";
		}

		final IFreightCostBL freightCostBL = Services.get(IFreightCostBL.class);
		final de.metas.adempiere.model.I_C_Order o = InterfaceWrapperHelper.create(order, de.metas.adempiere.model.I_C_Order.class);
		if (freightCostBL.checkIfFree(o))
		{
			OrderBL.logger.fine("No freight cost for " + order);
			return nullIfOk ? null : "";
		}

		final MFreightCost freightCost =
				MFreightCost.retrieveFor(ctx,
						bPartnerId,
						bPartnerLocationId,
						shipperId,
						order.getAD_Org_ID(),
						order.getDateOrdered(),
						trxName);

		if (freightCost == null)
		{
			return Services.get(IMsgBL.class).getMsg(ctx, OrderBL.MSG_NO_FREIGHT_COST_DETAIL);
		}
		return nullIfOk ? null : "";
	}

	@Override
	public String setPricingSystemId(final I_C_Order order, final boolean nullIfOk, final String trxName)
	{
		if (order.getM_PricingSystem_ID() > 0)
		{
			OrderBL.logger.fine("order " + order.getDocumentNo() + " already has a pricing system. Doing nothing");
			return nullIfOk ? null : "";
		}
		final int bPartnerId = order.getC_BPartner_ID();
		if (bPartnerId <= 0)
		{
			OrderBL.logger.fine("order " + order.getDocumentNo() + " has no C_BPartner_ID. Doing nothing");
			return nullIfOk ? null : "";
		}

		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final IBPartnerDAO bPartnerPA = Services.get(IBPartnerDAO.class);
		final int pricingSysId = bPartnerPA.retrievePricingSystemId(ctx, bPartnerId, order.isSOTrx(), trxName);

		order.setM_PricingSystem_ID(pricingSysId);

		return setPriceList(order, nullIfOk, pricingSysId, trxName);
	}

	@Override
	public String setPriceList(final I_C_Order order,
			final boolean nullIfOk, final int pricingSysId, final String trxName)
	{
		// metas: When PricingSystem is set, PriceList needs to be set, too.
		final int bPartnerLocId = order.getC_BPartner_Location_ID();

		if (bPartnerLocId <= 0 || pricingSysId <= 0)
		{
			return nullIfOk ? null : "";
		}

		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final IProductPA productPA = Services.get(IProductPA.class);
		final I_M_PriceList priceList = productPA.retrievePriceListByPricingSyst(
				ctx, pricingSysId, bPartnerLocId, order.isSOTrx(), trxName);

		if (priceList != null)
		{
			order.setM_PriceList_ID(priceList.getM_PriceList_ID());
		}
		else
		{
			// return error message
			final String adMsg = order.isSOTrx() ? OrderBL.MSG_NO_SO_PRICELIST_FOUND : OrderBL.MSG_NO_PO_PRICELIST_FOUND;
			final MPricingSystem pricingSystem = new MPricingSystem(Env.getCtx(), pricingSysId, trxName);

			final Object[] args = { pricingSystem.getName() };

			return Services.get(IMsgBL.class).getMsg(Env.getCtx(), adMsg, args);
		}
		// metas: end

		return nullIfOk ? null : "";
	}

	@Override
	public String checkForPriceList(final I_C_Order order, final boolean nullIfOk, final String trxName)
	{
		final int bPartnerLocId = order.getC_BPartner_Location_ID();
		final int pricingSysId = order.getM_PricingSystem_ID();

		if (bPartnerLocId <= 0 || pricingSysId <= 0)
		{
			return nullIfOk ? null : "";
		}

		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final IProductPA productPA = Services.get(IProductPA.class);

		final I_M_PriceList pl = productPA.retrievePriceListByPricingSyst(
				ctx, pricingSysId, bPartnerLocId, order.isSOTrx(), trxName);

		if (pl == null)
		{
			final String adMsg = order.isSOTrx() ? OrderBL.MSG_NO_SO_PRICELIST_FOUND : OrderBL.MSG_NO_PO_PRICELIST_FOUND;
			final MPricingSystem pricingSystem = new MPricingSystem(Env.getCtx(), pricingSysId, trxName);

			final Object[] args = { pricingSystem.getName() };

			return Services.get(IMsgBL.class).getMsg(Env.getCtx(), adMsg, args);
		}
		return nullIfOk ? null : "";
	}

	@Override
	public BigDecimal retrieveTaxAmt(final I_C_Order order)
	{
		final PO po = POWrapper.getPO(order);
		final Properties ctx = po == null ? Env.getCtx() : po.getCtx();
		final String trxName = po == null ? null : po.get_TrxName();

		return new Query(ctx, I_C_OrderTax.Table_Name, I_C_OrderTax.COLUMNNAME_C_Order_ID + "=?", trxName)
				.setParameters(order.getC_Order_ID())
				.setOnlyActiveRecords(true)
				.aggregate(I_C_OrderTax.COLUMNNAME_TaxAmt, IQuery.AGGREGATE_SUM);
	}

	@Override
	public boolean updateFreightAmt(final Properties ctx, final I_C_Order order, final String trxName)
	{
		final IFreightCostBL freightCostBL = Services.get(IFreightCostBL.class);
		final de.metas.adempiere.model.I_C_Order o = InterfaceWrapperHelper.create(order, de.metas.adempiere.model.I_C_Order.class);
		final BigDecimal freightCostAmt = freightCostBL.computeFreightCostForOrder(ctx, o, trxName);

		if (order.getFreightAmt().compareTo(freightCostAmt) != 0)
		{
			order.setFreightAmt(freightCostAmt);
			return true;
		}
		return false;
	}

	@Override
	public boolean setBill_User_ID(final org.compiere.model.I_C_Order order)
	{
		// First try: if order and bill partner and location are the same, and the contact is set
		// we can use the same contact
		if (order.getC_BPartner_ID() == order.getBill_BPartner_ID()
				&& order.getC_BPartner_Location_ID() == order.getBill_Location_ID()
				&& order.getAD_User_ID() > 0)
		{
			order.setBill_User_ID(order.getAD_User_ID());
			return true;
		}

		final IBPartnerBL bpartnerService = Services.get(IBPartnerBL.class);
		final I_AD_User billContact;
		// Case: Bill Location is set, we can use it to retrieve the contact for that location
		if (order.getBill_Location_ID() > 0)
		{
			final I_C_BPartner_Location billLocation = POWrapper.create(order.getBill_Location(), I_C_BPartner_Location.class);
			billContact = bpartnerService.retrieveUserForLoc(billLocation);
		}
		// Case: Bill Location is NOT set, we search for default bill contact
		else
		{
			final Properties ctx = POWrapper.getCtx(order);
			final String trxName = POWrapper.getTrxName(order);
			final int bPartnerId = order.getBill_BPartner_ID();
			billContact = bpartnerService.retrieveBillContact(ctx, bPartnerId, trxName);
		}

		if (billContact == null)
		{
			return false;
		}

		order.setBill_User_ID(billContact.getAD_User_ID());
		return true;
	}

	@Override
	public void setM_PricingSystem_ID(final I_C_Order order)
	{
		final String trxName = InterfaceWrapperHelper.getTrxName(order);
		final Properties ctx = InterfaceWrapperHelper.getCtx(order);

		if (order.isSOTrx())
		{
			int pricingSysId = order.getM_PricingSystem_ID();
			if (pricingSysId <= 0)
			{
				pricingSysId = Services.get(IBPartnerDAO.class).retrievePricingSystemId(ctx, order.getBill_BPartner_ID(), order.isSOTrx(), trxName);
			}
			order.setM_PricingSystem_ID(pricingSysId);
			order.setM_PriceList_ID(retrievePriceListId(order));
		}
	}

	@Override
	public int retrievePriceListId(final I_C_Order order)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final String trxName = InterfaceWrapperHelper.getTrxName(order);

		return retrievePriceListId(
				ctx,
				order.getC_BPartner_Location_ID(),
				order.getBill_Location_ID(),
				order.getM_PricingSystem_ID(),
				order.isSOTrx(),
				trxName);
	}

	@Cached
	/* package */int retrievePriceListId(
			final @CacheCtx Properties ctx,
			final int C_BPartner_Location_ID,
			final int Bill_Location_ID,
			final int M_PricingSystem_ID,
			final boolean isSOTrx,
			final @CacheTrx String trxName)
	{
		final int pricingSysId = M_PricingSystem_ID;

		final int bPartnerLocationId = C_BPartner_Location_ID;
		if (bPartnerLocationId == 0)
		{
			// fallback
			// order.getBill_Location_ID();
			// TODO: why is not setting bPartnerLocationId to Bill_Location_ID?
		}
		final I_M_PriceList pl =
				Services.get(IProductPA.class).retrievePriceListByPricingSyst(ctx, pricingSysId, bPartnerLocationId, isSOTrx, trxName);
		if (pl != null)
		{
			return pl.getM_PriceList_ID();
		}
		return 0;
	}

	@Override
	public void setDocTypeTargetId(final I_C_Order order)
	{
		final int clientId = order.getAD_Client_ID();

		if (order.isSOTrx()) // SO = Std Order
		{
			setDocTypeTargetId(order, MOrder.DocSubType_Standard);
			return;
		}
		// PO

		final int adOrgId = order.getAD_Org_ID();

		final String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ " WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + adOrgId + ") AND DocBaseType='POO' "
				+ " ORDER BY AD_Org_ID DESC, IsDefault DESC, DocSubType NULLS FIRST";
		final int C_DocType_ID = DB.getSQLValueEx(null, sql, clientId);
		if (C_DocType_ID <= 0)
		{
			OrderBL.logger.severe("No POO found for AD_Client_ID=" + clientId);
		}
		else
		{
			OrderBL.logger.fine("(PO) - " + C_DocType_ID);
			order.setC_DocTypeTarget_ID(C_DocType_ID);
		}
	}

	@Override
	public void setDocTypeTargetId(final I_C_Order order, final String docSubType)
	{
		final int clientId = order.getAD_Client_ID();
		final int adOrgId = order.getAD_Org_ID();

		final int C_DocType_ID = retrieveDocTypeId(clientId, adOrgId, docSubType);

		if (C_DocType_ID <= 0)
		{
			OrderBL.logger.severe("Not found for AD_Client_ID=" + order.getAD_Client_ID() + ", SubType=" + docSubType);
		}
		else
		{
			OrderBL.logger.fine("(SO) - " + docSubType);
			order.setC_DocTypeTarget_ID(C_DocType_ID);
			order.setIsSOTrx(true);
		}
	}

	@Override
	public int retrieveDocTypeId(final int clientId, final int adOrgId, final String docSubType)
	{
		final String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + adOrgId
				+ ") AND DocSubType=? "
				+ "ORDER BY IsDefault DESC, AD_Org_ID DESC";

		final int C_DocType_ID = DB.getSQLValueEx(null, sql, clientId, docSubType);
		return C_DocType_ID;
	}

	@Override
	public void updateAddresses(final org.compiere.model.I_C_Order order)
	{
		final de.metas.adempiere.model.I_C_Order orderEx = InterfaceWrapperHelper.create(order, de.metas.adempiere.model.I_C_Order.class);

		for (final I_C_OrderLine line : Services.get(IOrderDAO.class).retrieveOrderLines(orderEx))
		{
			if (line instanceof MOrderLine && order instanceof MOrder)
			{
				((MOrderLine)line).setHeaderInfo((MOrder)order);
			}

			if (orderEx.isDropShip() && orderEx.getDropShip_BPartner_ID() > 0)
			{
				line.setC_BPartner_ID(orderEx.getDropShip_BPartner_ID());
			}
			else
			{
				line.setC_BPartner_ID(orderEx.getC_BPartner_ID());
			}

			if (orderEx.isDropShip() && orderEx.getDropShip_Location_ID() > 0)
			{
				line.setC_BPartner_Location_ID(orderEx.getDropShip_Location_ID());
				line.setBPartnerAddress(orderEx.getDeliveryToAddress());

			}
			else
			{
				line.setC_BPartner_Location_ID(orderEx.getC_BPartner_Location_ID());
				line.setBPartnerAddress(orderEx.getBPartnerAddress());
			}

			if (orderEx.isDropShip() && orderEx.getDropShip_User_ID() > 0)
			{
				line.setAD_User_ID(orderEx.getDropShip_User_ID());
			}
			else
			{
				line.setAD_User_ID(orderEx.getAD_User_ID());
			}

			InterfaceWrapperHelper.save(line);
		}
	}

	@Override
	public String evaluateOrderDeliveryViaRule(I_C_Order order)
	{
		if (Check.isEmpty(order.getDeliveryViaRule(), true))
		{
			return findDeliveryViaRule(order);
		}

		return order.getDeliveryViaRule();
	}

	/**
	 * Retrieve the deliveryViaRule based on partner form order
	 * 
	 * @param order
	 * @return
	 */
	private String findDeliveryViaRule(final I_C_Order order)
	{

		if (order.getC_BPartner_ID() <= 0)
		{
			return null;
		}

		final I_C_BPartner bp = InterfaceWrapperHelper.create(order.getC_BPartner(), I_C_BPartner.class);

		final String deliveryViaRule;

		if (order.isSOTrx())
		{
			deliveryViaRule = bp.getDeliveryViaRule();
		}
		else
		{
			deliveryViaRule = bp.getPO_DeliveryViaRule();
		}

		if (Check.isEmpty(deliveryViaRule, true))
		{
			return null;
		}

		return deliveryViaRule;
	}

	@Override
	public I_M_PriceList_Version getPriceListVersion(final I_C_Order order)
	{
		if (order == null)
		{
			return null;
		}
		final IPriceListDAO priceListDAO = Services.get(IPriceListDAO.class);

		final Timestamp orderDate;
		if (order.getDatePromised() != null)
		{
			orderDate = order.getDatePromised();
		}
		else
		{
			orderDate = order.getDateOrdered();
		}

		final I_M_PriceList_Version plv = priceListDAO.retrievePriceListVersion(order.getM_PriceList(), orderDate);
		return plv;
	}

	/**
	 * Set Business Partner Defaults & Details. SOTrx should be set.
	 * 
	 * @param bp business partner
	 */
	@Override
	public void setBPartner(final org.compiere.model.I_C_Order order, final org.compiere.model.I_C_BPartner bp)
	{
		if (bp == null)
		{
			return;
		}

		order.setC_BPartner_ID(bp.getC_BPartner_ID());

		final boolean isSOTrx = order.isSOTrx();
		//
		// Defaults Payment Term
		int ii = 0;
		if (isSOTrx)
		{
			ii = bp.getC_PaymentTerm_ID();
		}
		else
		{
			ii = bp.getPO_PaymentTerm_ID();
		}
		if (ii != 0)
		{
			order.setC_PaymentTerm_ID(ii);
		}

		//
		// Default Price List
		if (isSOTrx)
		{
			ii = bp.getM_PriceList_ID();
		}
		else
		{
			ii = bp.getPO_PriceList_ID();
		}
		if (ii != 0)
		{
			order.setM_PriceList_ID(ii);
		}

		//
		// Default Delivery/Via Rule
		String ss = bp.getDeliveryRule();
		if (ss != null)
		{
			order.setDeliveryRule(ss);
		}
		if(isSOTrx)
		{
			ss = bp.getDeliveryViaRule();
		}
		else
		{
			ss = bp.getPO_DeliveryViaRule();
		}
		
		if (ss != null)
		{
			order.setDeliveryViaRule(ss);
		}

		//
		// Default Invoice/Payment Rule
		ss = bp.getInvoiceRule();
		if (ss != null)
		{
			order.setInvoiceRule(ss);
		}
		ss = bp.getPaymentRule();
		if (ss != null)
		{
			order.setPaymentRule(ss);
		}

		//
		// Sales Rep
		ii = bp.getSalesRep_ID();
		if (ii != 0)
		{
			order.setSalesRep_ID(ii);
		}

		setBPLocation(order, bp);
		setBillLocation(order, bp, null);

		final IBPartnerDAO bPartnerDAO = Services.get(IBPartnerDAO.class);

		// Set Contact
		// final List<I_AD_User> contacts = bPartnerDAO.retrieveContacts(bp.getC_BPartner_ID(), false, null);
		// if (contacts != null && contacts.size() == 1)
		// {
		// order.setAD_User_ID(contacts.get(0).getAD_User_ID());
		// }

		// 08812
		// set the fit contact

		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final int bpartnerId = bp.getC_BPartner_ID();

		// keep the trxName null, as it was before
		final String trxName = ITrx.TRXNAME_None;
		final I_AD_User contact = bPartnerDAO.retrieveContact(ctx, bpartnerId, isSOTrx, trxName);

		// keep the functionality as it was. Do not set null user
		if (contact != null)
		{
			order.setAD_User(contact);
		}
	}

	@Override
	public void setBPLocation(final org.compiere.model.I_C_Order order, final org.compiere.model.I_C_BPartner bp)
	{
		final IBPartnerDAO bPartnerDAO = Services.get(IBPartnerDAO.class);

		final List<I_C_BPartner_Location> locations = bPartnerDAO.retrieveBPartnerLocations(bp.getC_BPartner_ID(), false, ITrx.TRXNAME_None);

		// Set Locations
		final List<I_C_BPartner_Location> shipLocations = new ArrayList<I_C_BPartner_Location>();
		boolean foundLoc = false;
		for (final I_C_BPartner_Location loc : locations)
		{
			if (loc.isShipTo() && loc.isActive())
			{
				shipLocations.add(loc);
			}

			final de.metas.adempiere.model.I_C_BPartner_Location bpLoc = InterfaceWrapperHelper.create(loc, de.metas.adempiere.model.I_C_BPartner_Location.class);
			if (bpLoc.isShipToDefault())
			{
				order.setC_BPartner_Location_ID(bpLoc.getC_BPartner_Location_ID());
				foundLoc = true;
			}
		}

		// set first ship location if is not set
		if (!foundLoc)
		{
			if (!shipLocations.isEmpty())
			{
				order.setC_BPartner_Location_ID(shipLocations.get(0).getC_BPartner_Location_ID());
			}
			else if (!locations.isEmpty())
			{
				// set to first
				if (order.getC_BPartner_Location_ID() == 0)
				{
					order.setC_BPartner_Location_ID(locations.get(0)
							.getC_BPartner_Location_ID());
				}
			}
		}

		if (!foundLoc)
		{
			logger.log(Level.SEVERE, "MOrder.setBPartner - Has no Ship To Address: " + bp);
		}
	}

	/**
	 * 
	 * 
	 * @param order
	 */
	@Override
	public boolean setBillLocation(final I_C_Order order)
	{
		if (order.getC_BPartner() == null)
		{
			return false; // nothing to be done
		}

		//
		// First, try to set the bill location from the C_BPartner
		setBillLocation(order, order.getC_BPartner(), null);
		if (order.getBill_Location_ID() > 0)
		{
			return true; // found it
		}

		final IBPartnerDAO bPartnerDAO = Services.get(IBPartnerDAO.class);

		//
		// Search in relation and try to find an adequate Bill Partner if the bill location could not be found
		final I_C_BP_Relation billPartnerRelation = bPartnerDAO.retrieveBillBPartnerRelationFirstEncountered(order,
				order.getC_BPartner(),
				InterfaceWrapperHelper.create(order.getC_BPartner_Location(), de.metas.adempiere.model.I_C_BPartner_Location.class));

		if (billPartnerRelation == null)
		{
			return false; // didn't find it
		}

		final I_C_BPartner partnerToUse = InterfaceWrapperHelper.create(billPartnerRelation.getC_BPartnerRelation(), I_C_BPartner.class);
		final I_C_BPartner_Location defaultLocation = InterfaceWrapperHelper.create(billPartnerRelation.getC_BPartnerRelation_Location(), I_C_BPartner_Location.class);
		setBillLocation(order, partnerToUse, defaultLocation);
		return true; // found it
	}

	private boolean setBillLocation(
			final org.compiere.model.I_C_Order order,
			final org.compiere.model.I_C_BPartner billBPartner,
			final org.compiere.model.I_C_BPartner_Location defaultBillLocation)
	{
		if (billBPartner == null)
		{
			return false;
		}

		int billLocationIdToUse = 0;
		boolean foundLoc = false;

		if (defaultBillLocation != null && defaultBillLocation.getC_BPartner_Location_ID() > 0)
		{
			billLocationIdToUse = defaultBillLocation.getC_BPartner_Location_ID();
			foundLoc = true;
		}

		if (!foundLoc)
		{
			final IBPartnerDAO bPartnerDAO = Services.get(IBPartnerDAO.class);
			final List<I_C_BPartner_Location> locations = bPartnerDAO.retrieveBPartnerLocations(billBPartner.getC_BPartner_ID(), false, ITrx.TRXNAME_None);

			// Set Locations
			final List<I_C_BPartner_Location> invLocations = new ArrayList<I_C_BPartner_Location>();
			for (final I_C_BPartner_Location loc : locations)
			{
				if (foundLoc)
				{
					break;
				}

				final de.metas.adempiere.model.I_C_BPartner_Location bpLoc = InterfaceWrapperHelper.create(loc, de.metas.adempiere.model.I_C_BPartner_Location.class);
				if (bpLoc.isBillToDefault())
				{
					billLocationIdToUse = bpLoc.getC_BPartner_Location_ID();
					foundLoc = true;
				}

				if (loc.isBillTo())
				{
					invLocations.add(loc);
				}
			}

			// set first invoice location if is not set
			if (!foundLoc)
			{
				if (!invLocations.isEmpty())
				{
					final I_C_BPartner_Location firstInvLocation = invLocations.get(0);

					billLocationIdToUse = firstInvLocation.getC_BPartner_Location_ID();
				}
				else if (!locations.isEmpty())
				{
					// set to first
					if (order.getBill_Location_ID() == 0)
					{
						final I_C_BPartner_Location firstRetrievedLocation = locations.get(0);
						billLocationIdToUse = firstRetrievedLocation.getC_BPartner_Location_ID();
					}
				}
			}
		}

		order.setBill_BPartner_ID(billBPartner.getC_BPartner_ID());
		order.setBill_Location_ID(billLocationIdToUse);

		if (billLocationIdToUse > 0)
		{
			foundLoc = true;
		}

		// 07138
		// We don't need a SEVERE log for this. even though the partner doesn't have a bill to address
		// there are still fallbacks on the relation, etc
		// In case no address is found, the caller is responsible for deciding what to to (e.g. show a user error).

		if (!foundLoc)
		{
			logger.log(Level.FINE, "MOrder.setBPartner - Has no Bill To Address: " + billBPartner);
		}
		return foundLoc;
	}

	@Override
	public void setOrder(final org.compiere.model.I_C_OrderLine orderLine, final I_C_Order order, final String trxName)
	{
		orderLine.setC_Order_ID(order.getC_Order_ID());

		Services.get(IPOService.class).copyClientOrg(order, orderLine);

		orderLine.setC_BPartner_ID(order.getC_BPartner_ID());
		orderLine.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
		orderLine.setM_Warehouse_ID(order.getM_Warehouse_ID());
		orderLine.setDateOrdered(order.getDateOrdered());
		orderLine.setDatePromised(order.getDatePromised());
		orderLine.setC_Currency_ID(order.getC_Currency_ID());
	}

	@Override
	public void setProduct(final org.compiere.model.I_C_OrderLine orderLine, final I_M_Product product)
	{
		if (product != null)
		{
			orderLine.setM_Product_ID(product.getM_Product_ID());
			orderLine.setC_UOM_ID(product.getC_UOM_ID());
		}
		else
		{
			orderLine.setM_Product_ID(0);
			orderLine.setC_UOM_ID(0);
		}
		orderLine.setM_AttributeSetInstance_ID(0);
	}

	@Override
	public int getPrecision(final org.compiere.model.I_C_Order order)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		return MCurrency.getStdPrecision(ctx, order.getC_Currency_ID());
	}

	@Override
	public boolean isTaxIncluded(final org.compiere.model.I_C_Order order, I_C_Tax tax)
	{
		Check.assumeNotNull(order, "order not null");

		if (tax != null && tax.isWholeTax())
		{
			return true;
		}

		return order.isTaxIncluded();
	}

	@Override
	public void closeLine(final org.compiere.model.I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(orderLine, "orderLine not null");

		if (orderLine.getQtyDelivered().compareTo(orderLine.getQtyOrdered()) >= 0) // they delivered at least the ordered qty => nothing to do
		{
			return; // Do nothing
		}

		orderLine.setQtyOrdered(orderLine.getQtyDelivered());
		InterfaceWrapperHelper.save(orderLine); // saving, just to be on the save side in case reserveStock() does a refresh or sth

		final I_C_Order order = orderLine.getC_Order();
		Services.get(IOrderPA.class).reserveStock(order, orderLine); // FIXME: move reserveStock method to an orderBL service
	}

	@Override
	public void reopenLine(final org.compiere.model.I_C_OrderLine orderLine)
	{
		Check.assumeNotNull(orderLine, "orderLine not null");

		//
		// Create conversion context
		final IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);
		final IUOMConversionContext uomConversionCtx = uomConversionBL.createConversionContext(orderLine.getM_Product());

		//
		// Calculate QtyOrdered as QtyEntered converted to stocking UOM
		final BigDecimal qtyEntered = orderLine.getQtyEntered();
		final I_C_UOM qtyEnteredUOM = orderLine.getC_UOM();
		final BigDecimal qtyOrdered = uomConversionBL.convertQtyToProductUOM(uomConversionCtx, qtyEntered, qtyEnteredUOM);

		//
		// Set QtyOrdered
		orderLine.setQtyOrdered(qtyOrdered);
		InterfaceWrapperHelper.save(orderLine); // saving, just to be on the save side in case reserveStock() does a refresh or sth

		//
		// Update qty reservation
		final I_C_Order order = orderLine.getC_Order();
		Services.get(IOrderPA.class).reserveStock(order, orderLine); // FIXME: move reserveStock method to an orderBL service

	}

	@Override
	public org.compiere.model.I_C_BPartner getShipToPartner(final I_C_Order order)
	{
		if (order.isDropShip())
		{
			// check for isDropShip to avoid returning a "stale" dropship-partner
			return order.getDropShip_BPartner_ID() > 0 ? order.getDropShip_BPartner() : order.getC_BPartner();
		}
		return order.getC_BPartner();
	}

	@Override
	public org.compiere.model.I_C_BPartner_Location getShipToLocation(final I_C_Order order)
	{
		if (order.isDropShip())
		{
			// check for isDropShip to avoid returning a "stale" dropship-partner
			return order.getDropShip_Location_ID() > 0 ? order.getDropShip_Location() : order.getC_BPartner_Location();
		}
		return order.getC_BPartner_Location();
	}

	@Override
	public org.compiere.model.I_AD_User getShipToUser(final I_C_Order order)
	{
		if (order.isDropShip())
		{
			// check for isDropShip to avoid returning a "stale" dropship-partner
			return order.getDropShip_User_ID() > 0 ? order.getDropShip_User() : order.getAD_User();
		}
		return order.getAD_User();
	}

	@Override
	public org.compiere.model.I_C_BPartner_Location getBillToLocation(I_C_Order order)
	{
		return order.getBill_Location_ID() > 0 ? order.getBill_Location() : order.getC_BPartner_Location();
	}

	private static final ModelDynAttributeAccessor<org.compiere.model.I_C_Order, BigDecimal> DYNATTR_QtyInvoicedSum = new ModelDynAttributeAccessor<>("QtyInvoicedSum", BigDecimal.class);
	private static final ModelDynAttributeAccessor<org.compiere.model.I_C_Order, BigDecimal> DYNATTR_QtyDeliveredSum = new ModelDynAttributeAccessor<>("QtyDeliveredSum", BigDecimal.class);
	private static final ModelDynAttributeAccessor<org.compiere.model.I_C_Order, BigDecimal> DYNATTR_QtyOrderedSum = new ModelDynAttributeAccessor<>("QtyOrderedSum", BigDecimal.class);

	@Override
	public void updateOrderQtySums(final org.compiere.model.I_C_Order order)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final IQueryAggregateBuilder<org.compiere.model.I_C_OrderLine, org.compiere.model.I_C_Order> aggregateOnOrder = queryBL
				.createQueryBuilder(org.compiere.model.I_C_OrderLine.class)
				.setContext(order)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(org.compiere.model.I_C_OrderLine.COLUMNNAME_C_Order_ID, order.getC_Order_ID())
				.addEqualsFilter(I_C_OrderLine.COLUMNNAME_IsPackagingMaterial, false)
				.aggregateOnColumn(org.compiere.model.I_C_OrderLine.COLUMN_C_Order_ID);

		aggregateOnOrder.sum(DYNATTR_QtyInvoicedSum, org.compiere.model.I_C_OrderLine.COLUMN_QtyInvoiced);
		aggregateOnOrder.sum(DYNATTR_QtyDeliveredSum, org.compiere.model.I_C_OrderLine.COLUMN_QtyDelivered);
		aggregateOnOrder.sum(DYNATTR_QtyOrderedSum, org.compiere.model.I_C_OrderLine.COLUMN_QtyOrdered);

		final List<org.compiere.model.I_C_Order> queryiedOrders = aggregateOnOrder.aggregate();
		final org.compiere.model.I_C_Order queriedOrder = ListUtils.singleElement(queryiedOrders);

		final de.metas.order.model.I_C_Order fOrder = InterfaceWrapperHelper.create(order, de.metas.order.model.I_C_Order.class);
		fOrder.setQtyInvoiced(DYNATTR_QtyInvoicedSum.getValue(queriedOrder));
		fOrder.setQtyMoved(DYNATTR_QtyDeliveredSum.getValue(queriedOrder));
		fOrder.setQtyOrdered(DYNATTR_QtyOrderedSum.getValue(queriedOrder));

		InterfaceWrapperHelper.save(fOrder);
	}
}