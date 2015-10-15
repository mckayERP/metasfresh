package de.metas.handlingunits.client.terminal.ddorder.api.impl;

/*
 * #%L
 * de.metas.handlingunits.client
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.adempiere.ad.dao.ICompositeQueryFilter;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.impl.EqualsQueryFilter;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.adempiere.util.collections.Predicate;
import org.adempiere.warehouse.api.IWarehouseDAO;
import org.compiere.model.IQuery;
import org.compiere.model.I_M_Locator;
import org.compiere.model.I_M_Warehouse;
import org.eevolution.model.I_DD_Order;
import org.eevolution.model.I_DD_OrderLine;
import org.eevolution.model.X_DD_Order;
import org.eevolution.model.X_DD_OrderLine;
import org.eevolution.model.X_M_Warehouse_Routing;

import de.metas.handlingunits.client.terminal.ddorder.model.IDDOrderTableRow;
import de.metas.handlingunits.client.terminal.select.api.IPOSTableRow;
import de.metas.handlingunits.client.terminal.select.api.impl.AbstractFiltering;
import de.metas.handlingunits.ddorder.api.IHUDDOrderBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.storage.IHUProductStorage;

public class DDOrderFiltering extends AbstractFiltering
{
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends IPOSTableRow> getTableRowType()
	{
		return IDDOrderTableRow.class;
	}

	@Override
	public List<I_M_Warehouse> retrieveWarehouses(final Properties ctx)
	{
		return Services.get(IWarehouseDAO.class).retrieveWarehouses(ctx, X_M_Warehouse_Routing.DOCBASETYPE_DistributionOrder);
	}

	@Override
	public List<IPOSTableRow> retrieveTableRows(final Properties ctx, final int warehouseId)
	{
		final ICompositeQueryFilter<I_DD_OrderLine> filters = createFilter(ctx, warehouseId);

		final IQueryBuilder<I_DD_OrderLine> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_DD_OrderLine.class)
				.setContext(ctx, ITrx.TRXNAME_None)
				.filter(filters);

		// order by
		queryBuilder.orderBy()
				.addColumn(I_DD_OrderLine.COLUMNNAME_DateOrdered)
				.addColumn(I_DD_OrderLine.COLUMNNAME_DD_OrderLine_ID);

		final List<I_DD_OrderLine> ddOrderLines = queryBuilder.create()
				.setApplyAccessFilterRW(true)
				.list(I_DD_OrderLine.class);

		return new DDOrderTableRowAggregator()
				.addDD_OrderLines(ddOrderLines)
				.createNotAggregatedRows();
	}

	@Override
	public List<IPOSTableRow> filter(final List<IPOSTableRow> rows, final Predicate<IPOSTableRow> filter)
	{
		//
		// Call super to match the non-aggregated rows
		final List<IPOSTableRow> rowsMatched = super.filter(rows, filter);

		//
		// Aggregate matched rows
		final DDOrderTableRowAggregator aggregator = new DDOrderTableRowAggregator();
		for (final IPOSTableRow row : rowsMatched)
		{
			final IDDOrderTableRow ddOrderTableRow = getDDOrderTableRow(row);
			aggregator.addDDOrderTableRow(ddOrderTableRow);
		}

		return aggregator.aggregate();
	}

	private ICompositeQueryFilter<I_DD_OrderLine> createFilter(final Properties ctx, final int warehouseId)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final ICompositeQueryFilter<I_DD_OrderLine> filters = queryBL.createCompositeQueryFilter(I_DD_OrderLine.class);

		// Only active
		filters.addOnlyActiveRecordsFilter();

		// Only for context AD_Client_ID
		filters.addOnlyContextClient(ctx);

		//
		// Only those lines which were NOT flagged by user as delivered (when he pressed on CloseLines button)
		filters.addInArrayFilter(I_DD_OrderLine.COLUMN_IsDelivered_Override,
				X_DD_OrderLine.ISDELIVERED_OVERRIDE_No // explicitelly flagged as not delivered
				, null // accept null value too (which is considered as not flagged by user)
		);

		//
		// Only lines from Completed documents
		{
			final IQueryBuilder<I_DD_Order> ddOrderQueryBuilder = queryBL.createQueryBuilder(I_DD_Order.class)
					.setContext(ctx, ITrx.TRXNAME_None);

			final ICompositeQueryFilter<I_DD_Order> ddOrderFilters = ddOrderQueryBuilder.getFilters();
			ddOrderFilters.addOnlyActiveRecordsFilter();
			ddOrderFilters.addOnlyContextClient(ctx);
			ddOrderFilters.addInArrayFilter(I_DD_Order.COLUMN_DocStatus, X_DD_Order.DOCSTATUS_Completed);

			final IQuery<I_DD_Order> ddOrderQuery = ddOrderQueryBuilder.create();

			filters.addInSubQueryFilter(I_DD_OrderLine.COLUMNNAME_DD_Order_ID, I_DD_Order.COLUMNNAME_DD_Order_ID, ddOrderQuery);
		}

		//
		// Filter by warehouse
		if (warehouseId > 0)
		{
			final IQuery<I_M_Locator> locatorQuery = queryBL.createQueryBuilder(I_M_Locator.class)
					.setContext(ctx, ITrx.TRXNAME_None)
					.filter(new EqualsQueryFilter<I_M_Locator>(I_M_Locator.COLUMNNAME_M_Warehouse_ID, warehouseId))
					.create()
					.setOnlyActiveRecords(true);
			filters.addInSubQueryFilter(I_DD_OrderLine.COLUMNNAME_M_LocatorTo_ID, I_M_Locator.COLUMNNAME_M_Locator_ID, locatorQuery);
		}

		return filters;
	}

	public List<I_DD_Order> getDDOrders(final List<IPOSTableRow> rows)
	{
		if (rows == null || rows.isEmpty())
		{
			return Collections.emptyList();
		}

		final Set<Integer> seenDDOrderIds = new HashSet<Integer>();
		final List<I_DD_Order> result = new ArrayList<I_DD_Order>();
		for (final IPOSTableRow row : rows)
		{
			final IDDOrderTableRow ddOrderRow = getDDOrderTableRow(row);

			I_DD_Order ddOrder = null;
			for (final I_DD_OrderLine ddOrderLine : ddOrderRow.getDD_OrderLines())
			{
				ddOrder = ddOrderLine.getDD_Order();
				final int ddOrderId = ddOrder.getDD_Order_ID();
				if (!seenDDOrderIds.add(ddOrderId))
				{
					// already added
					ddOrder = null;
					continue;
				}
			}

			if (ddOrder != null)
			{
				result.add(ddOrder);
			}
		}

		return result;
	}

	public final IDDOrderTableRow getDDOrderTableRow(final IPOSTableRow row)
	{
		return (IDDOrderTableRow)row;
	}

	public List<I_DD_OrderLine> getDDOrderLines(final IPOSTableRow row)
	{
		final IDDOrderTableRow ddOrderRow = getDDOrderTableRow(row);
		final List<I_DD_OrderLine> ddOrderLines = ddOrderRow.getDD_OrderLines();
		return ddOrderLines;
	}

	@Override
	public Object getReferencedObject(final IPOSTableRow row)
	{
		if (row == null)
		{
			return null;
		}

		return getDDOrderLines(row); // TODO check if it's alright to return the list in this case
	}

	@Override
	public void closeRows(final Set<IPOSTableRow> rows)
	{
		if (rows == null || rows.isEmpty())
		{
			return;
		}

		//
		// Iterate rows and close them one by one
		for (final IPOSTableRow row : rows)
		{
			final List<I_DD_OrderLine> ddOrderLines = getDDOrderLines(row);
			for (final I_DD_OrderLine ddOrderLine : ddOrderLines)
			{
				ddOrderLine.setIsDelivered_Override(X_DD_OrderLine.ISDELIVERED_OVERRIDE_Yes);
				InterfaceWrapperHelper.save(ddOrderLine);
			}
		}
	}

	@Override
	public void processRows(final Properties ctx, final Set<IPOSTableRow> rows, final Set<I_M_HU> selectedHUs)
	{
		throw new UnsupportedOperationException("Please use de.metas.handlingunits.client.terminal.select.api.impl.DDOrderFiltering.processRows(Properties, IPOSTableRow, Set<IHUProductStorage>)");
	}

	public void processDDOrderLines(final Collection<I_DD_OrderLine> ddOrderLines, final Collection<IHUProductStorage> huProductStorages)
	{
		final IHUDDOrderBL huDDOrderBL = Services.get(IHUDDOrderBL.class);
		huDDOrderBL.createMovements(ddOrderLines, huProductStorages);
	}
}