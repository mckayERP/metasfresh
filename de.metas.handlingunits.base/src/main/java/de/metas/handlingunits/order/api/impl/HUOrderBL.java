package de.metas.handlingunits.order.api.impl;

/*
 * #%L
 * de.metas.handlingunits.base
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
import java.util.Date;
import java.util.Properties;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_M_Product;

import de.metas.adempiere.service.IOrderLineBL;
import de.metas.handlingunits.IHUCapacityBL;
import de.metas.handlingunits.IHUPIItemProductDAO;
import de.metas.handlingunits.IHUPIItemProductQuery;
import de.metas.handlingunits.model.I_C_Order;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.handlingunits.model.X_M_HU_PI_Version;
import de.metas.handlingunits.order.api.IHUOrderBL;
import de.metas.interfaces.I_C_OrderLine;

public class HUOrderBL implements IHUOrderBL
{
	@Override
	public void updateOrderLine(final de.metas.interfaces.I_C_OrderLine olPO, final String columnName)
	{
		//
		// services
		final IHUPIItemProductDAO hupiItemProductDAO = Services.get(IHUPIItemProductDAO.class);

		final de.metas.handlingunits.model.I_C_OrderLine ol = InterfaceWrapperHelper.create(olPO, de.metas.handlingunits.model.I_C_OrderLine.class);

		final String huUnitType = X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit;

		final I_M_HU_PI_Item_Product pip;

		if (olPO.getM_Product_ID() <= 0)
		{
			// No product selected. Nothing to do.
			return;
		}

		// the record is new
		// search for the right combination
		if (InterfaceWrapperHelper.isNew(ol))
		{
			if (ol.getM_HU_PI_Item_Product_ID() > 0)
			{
				pip = ol.getM_HU_PI_Item_Product();
			}
			else
			{
				// pip = Services.get(IHUPIItemProductDAO.class).retrieveMaterialItemProduct(olPO.getM_Product(), olPO.getC_BPartner(), olPO.getDateOrdered(), huUnitType);
				// 06730 : Removed functionality for now.
				pip = null;
			}
		}
		// the record is not new
		else
		{
			// if the partner had changed or product had changed, need to search for the proper combination
			if (isProductChanged(olPO, columnName) || isBPartnerChanged(olPO, columnName))
			{
				pip = hupiItemProductDAO.retrieveMaterialItemProduct(olPO.getM_Product(), olPO.getC_BPartner(), olPO.getDateOrdered(), huUnitType,
						true); // allowInfiniteCapacity = true
			}
			// use the existing pip
			else if (ol.getM_HU_PI_Item_Product_ID() > 0 && (isQtyChanged(olPO, columnName) || isM_HU_PI_Item_ProductChanged(olPO, columnName)))
			{
				pip = ol.getM_HU_PI_Item_Product();
			}
			// the pip was reset
			else
			{
				pip = null;
			}
		}

		if (!ol.isManualQtyItemCapacity() && pip == null)
		{
			ol.setPackDescription("");
			ol.setQtyItemCapacity(null);
			//
			// if no pip it should be VirtualPI
			final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
			final I_M_HU_PI_Item_Product pipVirtual = hupiItemProductDAO.retrieveVirtualPIMaterialItemProduct(ctx);
			ol.setM_HU_PI_Item_Product(pipVirtual);
			// 05825 : Update Prices and set prices
			Services.get(IOrderLineBL.class).updatePrices(ol);
			final String trxName = InterfaceWrapperHelper.getTrxName(ol);
			Services.get(IOrderLineBL.class).setPricesIfNotIgnored(ctx, ol,
					InterfaceWrapperHelper.isNew(ol), // usePriceUOM
					trxName);
		}
		// If is not null, set all related items
		if (!ol.isManualQtyItemCapacity() && pip != null)
		{
			ol.setM_HU_PI_Item_Product_ID(pip.getM_HU_PI_Item_Product_ID());
			//
			BigDecimal qtyCap = BigDecimal.ZERO;
			if (!Services.get(IHUCapacityBL.class).isInfiniteCapacity(pip))
			{
				final I_M_Product product = ol.getM_Product();
				qtyCap = Services.get(IHUCapacityBL.class).getCapacity(pip, product, pip.getC_UOM()).getCapacity();
				Check.assume(qtyCap.signum() != 0, "Zero capacity for M_HU_PI_Item_Product {0}", pip.getM_HU_PI_Item_Product_ID());
			}
			final String description = pip.getDescription();
			final StringBuilder packDescription = new StringBuilder();
			packDescription.append(Check.isEmpty(description, true) ? "" : description);
			ol.setPackDescription(packDescription.toString());

			// 05131 : Changed column from virtual to real.
			if (!ol.isManualQtyItemCapacity())
			{
				ol.setQtyItemCapacity(qtyCap);
			}
			// 05825 : Update Prices and set prices
			Services.get(IOrderLineBL.class).updatePrices(ol);
			final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
			final String trxName = InterfaceWrapperHelper.getTrxName(ol);
			Services.get(IOrderLineBL.class).setPricesIfNotIgnored(ctx, ol,
					InterfaceWrapperHelper.isNew(ol), // usePriceUOM
					trxName);
		}
	}

	private boolean isM_HU_PI_Item_ProductChanged(final I_C_OrderLine orderLine, final String columnName)
	{
		return InterfaceWrapperHelper.getPO(orderLine).is_ValueChanged(de.metas.handlingunits.model.I_C_OrderLine.COLUMNNAME_M_HU_PI_Item_Product_ID)
				|| de.metas.handlingunits.model.I_C_OrderLine.COLUMNNAME_M_HU_PI_Item_Product_ID.equals(columnName);
	}

	private boolean isQtyChanged(final I_C_OrderLine orderLine, final String columnName)
	{
		return InterfaceWrapperHelper.getPO(orderLine).is_ValueChanged(org.compiere.model.I_C_OrderLine.COLUMNNAME_QtyEntered)
				|| org.compiere.model.I_C_OrderLine.COLUMNNAME_QtyEntered.equals(columnName);
	}

	private boolean isProductChanged(final I_C_OrderLine orderLine, final String columnName)
	{
		return InterfaceWrapperHelper.getPO(orderLine).is_ValueChanged(org.compiere.model.I_C_OrderLine.COLUMNNAME_M_Product_ID)
				|| org.compiere.model.I_C_OrderLine.COLUMNNAME_M_Product_ID.equals(columnName);
	}

	private boolean isBPartnerChanged(final I_C_OrderLine orderLine, final String columnName)
	{
		return InterfaceWrapperHelper.getPO(orderLine).is_ValueChanged(org.compiere.model.I_C_OrderLine.COLUMNNAME_C_BPartner_ID)
				|| org.compiere.model.I_C_OrderLine.COLUMNNAME_C_BPartner_ID.equals(columnName);
	}

	@Override
	public boolean updateQtyCU(final I_C_Order order)
	{
		final I_M_HU_PI_Item_Product huPIP = order.getM_HU_PI_Item_Product();

		if (huPIP == null)
		{
			// nothing to do: the M_HU_PI_Item_Product was not yet set.
			return false;
		}

		if (huPIP.isInfiniteCapacity())
		{
			// nothing to do. The qty CU shall remain as it was manually set by the user
			return false;
		}

		final BigDecimal qtyTU = order.getQty_FastInput_TU();

		Check.assume(qtyTU.signum() >= 0, "Qty TU must be positive");

		// In case the Qty TU is set to 0, Qty CU shall be also set to 0
		if (qtyTU.compareTo(BigDecimal.ZERO) == 0)
		{
			order.setQty_FastInput(BigDecimal.ZERO);
		}
		else
		{
			final BigDecimal capacity = huPIP.getQty();

			order.setQty_FastInput(capacity.multiply(qtyTU));
		}

		return true;
	}

	@Override
	public boolean updateQtyTU(final I_C_Order order)
	{
		final I_M_HU_PI_Item_Product huPIP = order.getM_HU_PI_Item_Product();

		if (huPIP == null)
		{
			// nothing to do: the M_HU_PI_Item_Product was not yet set.
			return false;
		}

		if (huPIP.isInfiniteCapacity())
		{
			// nothing to do. The qty CU shall remain as it was manually set by the user
			return false;
		}

		final BigDecimal qtyCU = order.getQty_FastInput();

		Check.assume(qtyCU.signum() >= 0, "Qty CU must be positive");

		if (qtyCU.compareTo(BigDecimal.ZERO) == 0)
		{
			order.setQty_FastInput_TU(BigDecimal.ZERO);
		}
		else
		{
			final BigDecimal capacity = huPIP.getQty();

			order.setQty_FastInput_TU(qtyCU.divide(capacity, 0, BigDecimal.ROUND_UP));
		}

		// qty TU was modified
		return true;
	}

	@Override
	public void updateQtys(final I_C_Order order, final String columnname)
	{
		final I_M_HU_PI_Item_Product huPIP = order.getM_HU_PI_Item_Product();

		if (huPIP == null)
		{
			// nothing to do: the M_HU_PI_Item_Product was not yet set.
			return;
		}

		if (huPIP.isInfiniteCapacity())
		{
			// nothing to do. The qty CU shall remain as it was manually set by the user
			return;
		}
		else if (I_C_Order.COLUMNNAME_M_HU_PI_Item_Product_ID.equals(columnname))
		{
			final BigDecimal qtyCU = order.getQty_FastInput();
			if (qtyCU != null && qtyCU.signum() > 0)
			{
				updateQtyTU(order);
			}
			else
			{
				final BigDecimal qtyTU = order.getQty_FastInput_TU();
				if (qtyTU != null && qtyTU.signum() > 0)
				{
					updateQtyCU(order);
				}
			}

			return;
		}
		else if (I_C_Order.COLUMNNAME_Qty_FastInput_TU.equals(columnname))
		{

			updateQtyCU(order);
			return;
		}
		else if (de.metas.adempiere.model.I_C_Order.COLUMNNAME_Qty_FastInput.equals(columnname))
		{
			updateQtyTU(order);
			return;
		}
	}

	@Override
	public boolean hasTUs(final I_C_Order order)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(order);
		final int bpartnerId = order.getC_BPartner_ID();
		final int productId = order.getM_Product_ID();
		final Date date = order.getDateOrdered();

		return hasTUs(ctx, bpartnerId, productId, date);
	}

	@Override
	public boolean hasTUs(final Properties ctx, final int bpartnerId, final int productId, final Date date)
	{
		final IHUPIItemProductDAO piItemProductDAO = Services.get(IHUPIItemProductDAO.class);

		final IHUPIItemProductQuery queryVO = piItemProductDAO.createHUPIItemProductQuery();
		queryVO.setC_BPartner_ID(bpartnerId);
		queryVO.setM_Product_ID(productId);
		queryVO.setDate(date);
		queryVO.setAllowAnyProduct(false);
		queryVO.setHU_UnitType(X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit);

		return piItemProductDAO.matches(ctx, queryVO, ITrx.TRXNAME_None);
	}
}