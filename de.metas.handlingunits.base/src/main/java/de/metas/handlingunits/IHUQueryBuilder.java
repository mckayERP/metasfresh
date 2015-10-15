package de.metas.handlingunits;

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


import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.mm.attributes.api.IAttributeSet;
import org.adempiere.model.ModelColumn;
import org.compiere.model.IQuery;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_Warehouse;

import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_PI_Version;
import de.metas.handlingunits.model.I_M_Locator;

/**
 * Developer friendly Query Builder which is oriented on Handling Units concerns.
 *
 * Defaults:
 * <ul>
 * <li>only top level HUs are matched by default
 * </ul>
 *
 * @author tsa
 *
 */
public interface IHUQueryBuilder
{
	/**
	 * @return user readable string about attributes filters
	 */
	String getAttributesSummary();

	/** Creates a copy of this object */
	IHUQueryBuilder copy();

	/** Creates {@link IQuery} object from this builder */
	IQuery<I_M_HU> createQuery();

	/** Creates {@link IQueryBuilder} object from this builder */
	IQueryBuilder<I_M_HU> createQueryBuilder();

	/**
	 * Create an {@link IQueryFilter} based on what we set in our builder.
	 *
	 * NOTE: Using directly {@link IQueryFilter#accept(Object)} won't work in all cases (e.g. when matching attributes because other tables are involved)
	 *
	 * @return created query filter
	 */
	IQueryFilter<I_M_HU> createQueryFilter();

	/** Retrieves all HUs which are matching our criterias */
	List<I_M_HU> list();

	/** Retrieves all HUs which are matching our criterias, but no more then <code>limit</code> number. */
	List<I_M_HU> list(final int limit);

	/**
	 * Retrieves first {@link I_M_HU}.
	 *
	 * If there are more HUs an exception will be thrown.
	 *
	 * @return HU or <code>null</code>
	 */
	I_M_HU firstOnly();

	/** Retrieves first {@link I_M_HU} */
	I_M_HU first();

	/** Counts how many {@link I_M_HU}s are matched by our criterias */
	int count();

	/**
	 * Collects a unique list of models from on of {@link I_M_HU}'s columns.
	 *
	 * e.g. Collect all business partners from matched HUs
	 *
	 * <pre>
	 * List&lt;I_C_BPartner&gt; bpartners = myHUQueryBuilder.collect(I_M_HU.COLUMN_C_BPartner_ID);
	 * </pre>
	 *
	 * @param huColumn
	 * @return list of collected models
	 */
	<T> List<T> collect(ModelColumn<I_M_HU, T> huColumn);

	/**
	 * Sets the context in which the query will run.
	 *
	 * @param ctx
	 * @param trxName
	 * @return this
	 */
	IHUQueryBuilder setContext(final Properties ctx, final String trxName);

	/**
	 * Sets the context in which the query will run.
	 *
	 * @param contextProvider
	 * @return this
	 */
	IHUQueryBuilder setContext(final Object contextProvider);

	/**
	 * Filter only those HUs which have given product(s) in their storages.
	 *
	 * NOTE: given product(s) are appended to the list of previously specified ones
	 *
	 * @param productIds
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithProductIds(final Collection<Integer> productIds);

	/**
	 * Filter only those HUs which have given product(s) in their storages.
	 *
	 * NOTE: given product(s) are appended to the list of previously specified ones
	 *
	 * @param productId
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithProductId(int productId);

	/**
	 * Filter only those HUs which have given product(s) in their storages.
	 *
	 * NOTE: given product(s) are appended to the list of previously specified ones
	 *
	 * @param product
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithProduct(org.compiere.model.I_M_Product product);

	/**
	 * Filter only those HUs which are in any of the given warehouse(s).
	 *
	 * NOTE: given warehouse(s) are appended to the list of previously specified ones
	 *
	 * @param warehouseIds
	 * @return this
	 */
	IHUQueryBuilder addOnlyInWarehouseIds(final Collection<Integer> warehouseIds);

	/**
	 * Filter only those HUs which are in any of the given warehouse(s).
	 *
	 * NOTE: given warehouse(s) are appended to the list of previously specified ones
	 *
	 * @param warehouseId
	 * @return this
	 */
	IHUQueryBuilder addOnlyInWarehouseId(final int warehouseId);

	/**
	 * Filter only those HUs which are in any of the given warehouse(s).
	 *
	 * NOTE: given warehouse(s) are appended to the list of previously specified ones
	 *
	 * @param warehouses
	 * @return this
	 */
	IHUQueryBuilder addOnlyInWarehouses(Collection<? extends I_M_Warehouse> warehouses);

	/**
	 *
	 * @return an unmodifiable set containing the <code>M_Warehouse_ID</code>s that were previously specified by invocations of {@link #addOnlyInWarehouseId(int)},
	 *         {@link #addOnlyInWarehouses(Collection)} and {@link #addOnlyInWarehouseIds(Collection)}.
	 */
	Set<Integer> getOnlyInWarehouseIds();

	/**
	 * Set if we shall exclude the after picking locators (i.e. where {@link I_M_Locator#isAfterPickingLocator()} returns <code>true</code>).
	 *
	 * @param excludeAfterPickingLocator
	 * @return this
	 */
	IHUQueryBuilder setExcludeAfterPickingLocator(boolean excludeAfterPickingLocator);

	/**
	 * Filter only those HUs which are assigned to any of the given bpartner(s).
	 *
	 * NOTE: given bpartner(s) are appended to the list of previously specified ones.
	 *
	 * @param bPartnerId HU's BPartner that can be accepted. If it's <code>null</code> we will search for HU's without BPartner too.
	 * @return this
	 */
	IHUQueryBuilder addOnlyInBPartnerId(final Integer bPartnerId);

	/**
	 *
	 * @return an unmodifiable set containing the <code>C_BPartner_ID</code>s that were previously specified by invocations of {@link #addOnlyInBPartnerId(Integer)}.
	 */
	Set<Integer> getOnlyInBPartnerIds();

	/**
	 * Filter only those HUs which have M_HU.C_BPartner_ID set.
	 *
	 * @param onlyIfAssignedToBPartner
	 * @return this
	 */
	IHUQueryBuilder setOnlyIfAssignedToBPartner(final boolean onlyIfAssignedToBPartner);

	/**
	 * Filter only those HUs which are assigned to any of the given bpartner location(s).
	 *
	 * NOTE: given bpartner location(s) are appended to the list of previously specified ones.
	 *
	 * @param bpartnerLocationId HU's BPartner Location that can be accepted. If it's <code>null</code> we will search for HU's without BPartner Location too.
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithBPartnerLocationId(final Integer bpartnerLocationId);

	/**
	 * Match only top level HUs.
	 *
	 * It's same as calling {@link #setOnlyTopLevelHUs(boolean)} with <code>true</code>.
	 *
	 * @return this
	 * @see #setOnlyTopLevelHUs(boolean)
	 */
	IHUQueryBuilder setOnlyTopLevelHUs();

	/**
	 * Sets if we shall match only top level HUs or not.
	 *
	 * A HU is considered top level when it does not have any parent.
	 *
	 * NOTE: in case you are matching only top level HUs, then {@link #setM_HU_Item_Parent_ID(int)} will be ignored
	 *
	 * @param onlyTopLevelHUs true if only top level HUs shall be matched; false if any HU shall be matched
	 * @return this
	 */
	IHUQueryBuilder setOnlyTopLevelHUs(boolean onlyTopLevelHUs);

	/**
	 * Sets parent HU Item that our HUs needs to have.
	 *
	 * In case we are matching only top level HUs, this parameter is ignored.
	 *
	 * @param huItemId
	 * @return
	 */
	IHUQueryBuilder setM_HU_Item_Parent_ID(final int huItemId);

	IHUQueryBuilder setM_HU_Parent_ID(final int parentHUId);

	/**
	 * Sets HU's HUStatus to be matched.
	 *
	 * If <code>null</code> then all HU statuses are matched.
	 *
	 * NOTE: this is a short version for clearing all HU Statuses to be included and then if not null, adding this HUStatus.
	 *
	 * @param huStatus
	 * @return this
	 */
	IHUQueryBuilder setHUStatus(String huStatus);

	/**
	 * Adds an HU Status that shall be included. So ONLY those HUs which have a status that was added by this method will be included.
	 *
	 * @param huStatus
	 * @return this
	 */
	IHUQueryBuilder addHUStatusToInclude(String huStatus);

	/**
	 * Adds an HU Status that shall be excluded. So all HUs which have that status will be excluded.
	 * <p>
	 * <b>NOTE:</b> even if you specifically set this status to the list of statuses to include (e.g. {@link #setHUStatus(String)}): if the status is present in exclude list then it will be excluded
	 * no matter what.
	 *
	 * @param huStatus HUStatus to exclude (not null)
	 * @return this
	 */
	IHUQueryBuilder addHUStatusToExclude(String huStatus);

	/**
	 * Filter only those HUs which have <code>attribute</code> with given <code>value</code>.
	 *
	 * @param attribute
	 * @param value
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithAttribute(I_M_Attribute attribute, Object value);

	/**
	 * Filter only those HUs which have <code>attribute</code> with any of the given <code>values</code>.
	 *
	 * <p>
	 * <b>IMPORTANT:</b> other than e.g. in {@link #addOnlyInWarehouses(Collection)}, the conditions specified by successive method invocations are <b>AND</b>ed. So, only HUs that have <b>all</b> (as
	 * opposed to any) of the specified attributes and values will match the query.
	 *
	 * @param attribute
	 * @param attributeValueType
	 * @param values list of accepted values
	 * @return this
	 */
	IHUQueryBuilder addOnlyWithAttributeInList(I_M_Attribute attribute, String attributeValueType, List<? extends Object> values);

	/**
	 * Filter only those HUs which have given internal barcode.
	 *
	 * NOTE: i.e. is searching for M_HU.Value and <b>NOT</b> by SSCC18 or any other barcode
	 *
	 * @param barcode
	 * @return this
	 */
	IHUQueryBuilder setOnlyWithBarcode(String barcode);

	/**
	 * Add miscelanous filter to be applied.
	 *
	 * NOTE: use this method when none of other filters from here applies.
	 *
	 * @param filter filter to be applied
	 * @return this
	 */
	IHUQueryBuilder addFilter(IQueryFilter<I_M_HU> filter);

	/**
	 * Set sub-query filter. It will be bound on M_HU_ID.
	 *
	 * @param huSubQueryFilter {@link I_M_HU}s subquery or null
	 * @return this
	 */
	IHUQueryBuilder setInSubQueryFilter(IQuery<I_M_HU> huSubQueryFilter);

	/**
	 * Filter only those HUs which are locked (i.e. which have {@link I_M_HU#isLocked()} set to <code>true</code>).
	 *
	 * @param onlyLocked
	 * @return this
	 */
	IHUQueryBuilder setOnlyLocked(boolean onlyLocked);

	/**
	 * Ask this builder to throw an error if there were no HUs retrieved.
	 *
	 * @param errorADMessage error AD_Message to be used in exception; if <code>null</code> then a standard error message will be used
	 * @return this
	 * @see #setErrorIfNoHUs(boolean, String)
	 */
	IHUQueryBuilder setErrorIfNoHUs(String errorADMessage);

	/**
	 * Ask this builder to throw or to not throw an error if there were no HUs retrieved.
	 *
	 * Applies only if we call methods like {@link #list()}, {@link #firstOnly()}, {@link #first()} etc.
	 *
	 * Does not apply to {@link #collect(ModelColumn)}.
	 *
	 * @param errorIfNoHUs if true, an exception need to be thrown when there were no HUs retrieved.
	 * @param errorADMessage error AD_Message to be used in exception
	 * @return this
	 */
	IHUQueryBuilder setErrorIfNoHUs(boolean errorIfNoHUs, String errorADMessage);

	/**
	 *
	 * @return true if we shall throw an exception there were no HUs found
	 * @see #setErrorIfNoHUs(boolean, String)
	 * @see #setErrorIfNoHUs(String)
	 */
	boolean isErrorIfNoItems();

	/**
	 * Adds HUs which needs to be excluded when we retrieve them.
	 *
	 * @param husToExclude
	 * @return this
	 */
	IHUQueryBuilder addHUsToExclude(Collection<I_M_HU> husToExclude);

	/**
	 * Adds HUs which needs to be excluded when we retrieve them.
	 *
	 * @param huIdsToExclude
	 * @return
	 */
	IHUQueryBuilder addHUIdsToExclude(Collection<Integer> huIdsToExclude);

	/**
	 * Adds the {@link I_M_HU_PI_Version} to include.
	 *
	 * @param huPIVersionId
	 * @return this
	 */
	IHUQueryBuilder addPIVersionToInclude(int huPIVersionId);

	/**
	 * Check if given {@link IAttributeSet} is matching the attributes from our query.
	 *
	 * @param attributes
	 * @return true if matches
	 */
	boolean matches(IAttributeSet attributes);

	/**
	 * Sets if the HUs which are currently open or are in a picking slot queue, shall be excluded.
	 *
	 * @param excludeHUsOnPickingSlot
	 * @return this
	 */
	IHUQueryBuilder setExcludeHUsOnPickingSlot(boolean excludeHUsOnPickingSlot);

	/**
	 * Set if the Locator of the HU shall be AfterPicking
	 *
	 * @param includeAfterPickingLocator
	 * @return
	 */
	IHUQueryBuilder setIncludeAfterPickingLocator(boolean includeAfterPickingLocator);

	/**
	 * Entries with empty storage will be the only ones retrieved
	 *
	 * @return
	 */
	IHUQueryBuilder setEmptyStorageOnly();

	/**
	 * Entries with  not empty storage will be the only ones retrieved
	 * 
	 * @return
	 */
	IHUQueryBuilder setNotEmptyStorageOnly();
}