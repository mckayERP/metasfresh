package org.eevolution.api.impl;

/*
 * #%L
 * de.metas.adempiere.libero.libero
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
import java.util.Date;
import java.util.Properties;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.document.service.IDocActionBL;
import org.adempiere.document.service.IDocTypeDAO;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mmovement.api.IMovementBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.warehouse.api.IWarehouseBL;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Locator;
import org.compiere.model.I_M_Movement;
import org.compiere.model.I_M_MovementLine;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.X_C_DocType;
import org.compiere.process.DocAction;
import org.compiere.util.TimeUtil;
import org.eevolution.api.IDDOrderBL;
import org.eevolution.api.IDDOrderMovementBuilder;
import org.eevolution.exceptions.LiberoException;
import org.eevolution.model.I_DD_Order;
import org.eevolution.model.I_DD_OrderLine;
import org.eevolution.model.I_DD_OrderLine_Alternative;
import org.eevolution.model.I_DD_OrderLine_Or_Alternative;

public class DD_Order_MovementBuilder implements IDDOrderMovementBuilder
{
	private final IDDOrderBL ddOrderBL = Services.get(IDDOrderBL.class);

	private I_DD_Order ddOrder;
	private Timestamp movementDate;
	private I_M_Movement movement;

	private final void assumeNoMovementHeaderCreated()
	{
		Check.assumeNull(movement, LiberoException.class, "movement header shall not be created");
	}

	@Override
	public void setDD_Order(final I_DD_Order ddOrder)
	{
		assumeNoMovementHeaderCreated();
		this.ddOrder = ddOrder;
	}

	@Override
	public I_DD_Order getDD_Order()
	{
		Check.assumeNotNull(ddOrder, LiberoException.class, "ddOrder not null");
		return ddOrder;
	}

	@Override
	public void setMovementDate(final Date movementDate)
	{
		assumeNoMovementHeaderCreated();
		this.movementDate = TimeUtil.asTimestamp(movementDate);
	}

	@Override
	public Timestamp getMovementDate()
	{
		Check.assumeNotNull(movementDate, LiberoException.class, "movementDate not null");
		return movementDate;
	}

	@Override
	public I_M_Movement getCreateMovementHeader()
	{
		if (movement != null)
		{
			return movement;
		}

		final I_DD_Order order = getDD_Order();
		final Timestamp movementDate = getMovementDate();

		Check.assumeNotNull(order, LiberoException.class, "order not null");
		Check.assumeNotNull(movementDate, LiberoException.class, "movementDate not null");

		movement = InterfaceWrapperHelper.newInstance(I_M_Movement.class, order);
		movement.setAD_Org_ID(order.getAD_Org_ID());

		//
		// Document Type
		final Properties ctx = InterfaceWrapperHelper.getCtx(movement);
		final int docTypeId = Services.get(IDocTypeDAO.class).getDocTypeId(ctx,
				X_C_DocType.DOCBASETYPE_MaterialMovement,
				movement.getAD_Client_ID(), movement.getAD_Org_ID(),
				ITrx.TRXNAME_None);
		movement.setC_DocType_ID(docTypeId);

		//
		// Reference to DD Order
		movement.setDD_Order_ID(order.getDD_Order_ID());
		movement.setPOReference(order.getPOReference());
		movement.setDescription(order.getDescription());

		//
		// Internal contact user
		movement.setSalesRep_ID(order.getSalesRep_ID());

		//
		// BPartner (i.e. shipper BP)
		movement.setC_BPartner_ID(order.getC_BPartner_ID());
		movement.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());	// shipment address
		movement.setAD_User_ID(order.getAD_User_ID());

		//
		// Shipper
		movement.setM_Shipper_ID(order.getM_Shipper_ID());
		movement.setFreightCostRule(order.getFreightCostRule());
		movement.setFreightAmt(order.getFreightAmt());

		//
		// Delivery Rules & Priority
		movement.setDeliveryRule(order.getDeliveryRule());
		movement.setDeliveryViaRule(order.getDeliveryViaRule());
		movement.setPriorityRule(order.getPriorityRule());

		//
		// Dates
		movement.setMovementDate(movementDate);

		//
		// Charge (if any)
		movement.setC_Charge_ID(order.getC_Charge_ID());
		movement.setChargeAmt(order.getChargeAmt());

		//
		// Dimensions
		// 07689: This set of the activity is harmless, even though this column is currently hidden
		movement.setC_Activity_ID(order.getC_Activity_ID());
		movement.setC_Campaign_ID(order.getC_Campaign_ID());
		movement.setC_Project_ID(order.getC_Project_ID());
		movement.setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		movement.setUser1_ID(order.getUser1_ID());
		movement.setUser2_ID(order.getUser2_ID());

		InterfaceWrapperHelper.save(movement);
		return movement;
	}

	@Override
	public I_M_MovementLine addMovementLineReceipt(final I_DD_OrderLine_Or_Alternative ddOrderLineOrAlt)
	{
		final BigDecimal movementQtySrc = ddOrderBL.getQtyToReceive(ddOrderLineOrAlt);
		final I_C_UOM movementQtyUOM = ddOrderLineOrAlt.getC_UOM();
		return addMovementLineReceipt(ddOrderLineOrAlt, movementQtySrc, movementQtyUOM);
	}

	@Override
	public I_M_MovementLine addMovementLineReceipt(final I_DD_OrderLine_Or_Alternative ddOrderLineOrAlt
			, final BigDecimal movementQtySrc, final I_C_UOM movementQtyUOM)
	{
		final boolean isReceipt = true;
		return addMovementLine(ddOrderLineOrAlt, isReceipt, movementQtySrc, movementQtyUOM);
	}

	@Override
	public I_M_MovementLine addMovementLineShipment(final I_DD_OrderLine_Or_Alternative ddOrderLineOrAlt)
	{
		final BigDecimal movementQtySrc = ddOrderBL.getQtyToShip(ddOrderLineOrAlt);
		final I_C_UOM movementQtyUOM = ddOrderLineOrAlt.getC_UOM();
		return addMovementLineShipment(ddOrderLineOrAlt, movementQtySrc, movementQtyUOM);
	}

	@Override
	public I_M_MovementLine addMovementLineShipment(final I_DD_OrderLine_Or_Alternative ddOrderLineOrAlt
			, final BigDecimal movementQtySrc, final I_C_UOM movementQtyUOM)
	{
		final boolean isReceipt = false;
		return addMovementLine(ddOrderLineOrAlt, isReceipt, movementQtySrc, movementQtyUOM);
	}

	private I_M_MovementLine addMovementLine(final I_DD_OrderLine_Or_Alternative fromDDOrderLineOrAlt
			, final boolean isReceipt
			, final BigDecimal movementQtySrc, final I_C_UOM movementQtyUOM)
	{
		final I_M_Movement movement = getCreateMovementHeader();

		final I_M_MovementLine movementLine = InterfaceWrapperHelper.newInstance(I_M_MovementLine.class, movement);
		movementLine.setAD_Org_ID(movement.getAD_Org_ID());
		movementLine.setM_Movement(movement);

		//
		// Load actual interface which was passed on
		final I_DD_OrderLine ddOrderLine;
		final I_DD_OrderLine_Alternative ddOrderLineAlt;
		if (InterfaceWrapperHelper.isInstanceOf(fromDDOrderLineOrAlt, I_DD_OrderLine.class))
		{
			ddOrderLineAlt = null;
			ddOrderLine = InterfaceWrapperHelper.create(fromDDOrderLineOrAlt, I_DD_OrderLine.class);
		}
		else if (InterfaceWrapperHelper.isInstanceOf(fromDDOrderLineOrAlt, I_DD_OrderLine_Alternative.class))
		{
			ddOrderLineAlt = InterfaceWrapperHelper.create(fromDDOrderLineOrAlt, I_DD_OrderLine_Alternative.class);
			ddOrderLine = ddOrderLineAlt.getDD_OrderLine();
		}
		else
		{
			//
			// Shall not happen; developer error
			throw new AdempiereException("Invalid I_DD_OrderLine_Or_Alternative implementation passed; Expected {0} or {1}, but was {2}",
					new Object[] { I_DD_OrderLine.class, I_DD_OrderLine_Alternative.class, fromDDOrderLineOrAlt });
		}

		movementLine.setDD_OrderLine(ddOrderLine);
		movementLine.setDD_OrderLine_Alternative(ddOrderLineAlt);

		movementLine.setLine(ddOrderLine.getLine());
		movementLine.setDescription(ddOrderLine.getDescription());

		//
		// Product & ASI
		final I_M_Product product = fromDDOrderLineOrAlt.getM_Product();
		Check.assumeNotNull(product, LiberoException.class, "product not null");
		movementLine.setM_Product(product);
		movementLine.setM_AttributeSetInstance_ID(fromDDOrderLineOrAlt.getM_AttributeSetInstance_ID());
		movementLine.setM_AttributeSetInstanceTo_ID(ddOrderLine.getM_AttributeSetInstanceTo_ID());
		//

		//
		// Get InTransit Locator/Warehouse
		final I_DD_Order ddOrder = ddOrderLine.getDD_Order();
		final I_M_Warehouse warehouseInTransit = ddOrder.getM_Warehouse();
		final I_M_Locator locatorInTransit = Services.get(IWarehouseBL.class).getDefaultLocator(warehouseInTransit);

		// final BigDecimal movementQtySrc;
		// final I_C_UOM movementQtyUOM = fromDDOrderLine.getC_UOM();

		// Movement-Receipt: InTransit -> Destination Locator
		final I_M_Locator locatorTo;
		if (isReceipt)
		{
			// movementQtySrc = getQtyToReceive(fromDDOrderLine);
			movementLine.setM_Locator_ID(locatorInTransit.getM_Locator_ID());

			locatorTo = ddOrderLine.getM_LocatorTo();
		}
		// Movement-Shipment: Source Locator -> InTransit
		else
		{
			// movementQtySrc = getQtyToShip(fromDDOrderLine);
			movementLine.setM_Locator_ID(ddOrderLine.getM_Locator_ID());

			locatorTo = locatorInTransit;
		}

		movementLine.setM_LocatorTo(locatorTo);

		Services.get(IMovementBL.class).setMovementQty(movementLine, movementQtySrc, movementQtyUOM);
		InterfaceWrapperHelper.save(movementLine);
		// 07629
		// set the activity from the warehouse of locator To
		// only in case it is different from the activity of the product acct
		Services.get(IMovementBL.class).setC_Activities(movementLine);

		return movementLine;
	}

	@Override
	public I_M_Movement process()
	{
		Check.assumeNotNull(movement, LiberoException.class, "movement not null");

		Services.get(IDocActionBL.class).processEx(movement, DocAction.ACTION_Complete, DocAction.STATUS_Completed);
		return movement;
	}
}