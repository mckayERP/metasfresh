package org.adempiere.ad.dao.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.ad.dao.IQueryFilterModifier;
import org.adempiere.ad.dao.ISqlQueryFilter;
import org.adempiere.ad.service.IDeveloperModeBL;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.model.ModelColumn;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.IQuery;
import org.compiere.util.CLogger;

/**
 * Filters out only records which are present in sub-query.
 * 
 * @author tsa
 * 
 * @param <T>
 */
public class InSubQueryFilter<T> implements IQueryFilter<T>, ISqlQueryFilter
{
	private final String tableName;
	private final String columnName;
	private final String subQueryColumnName;
	private final IQueryFilterModifier modifier;
	private final IQuery<?> subQuery;

	//
	private boolean sqlBuilt = false;
	private String sqlWhereClause = null;
	private List<Object> sqlParams = null;
	private List<Object> _subQueryValues = null;

	private static final transient CLogger logger = CLogger.getCLogger(InSubQueryFilter.class);
	/**
	 * 
	 * @param columnName this query match column
	 * @param subQueryColumnName sub query match column
	 * @param subQuery sub query
	 */
	public InSubQueryFilter(final String tableName, final String columnName, final String subQueryColumnName, final IQuery<?> subQuery)
	{
		this(tableName, columnName, NullQueryFilterModifier.instance, subQueryColumnName, subQuery);
	}

	public InSubQueryFilter(final ModelColumn<T, ?> column, final String subQueryColumnName, final IQuery<?> subQuery)
	{
		this(column.getTableName(), column.getColumnName(), NullQueryFilterModifier.instance, subQueryColumnName, subQuery);
	}

	public InSubQueryFilter(final String tableName, final String columnName, final IQueryFilterModifier modifier, final String subQueryColumnName, final IQuery<?> subQuery)
	{
		super();
		this.tableName = tableName;
		this.columnName = columnName;
		this.subQueryColumnName = subQueryColumnName;
		this.modifier = modifier;
		this.subQuery = subQuery;
	}

	@Override
	public String toString()
	{
		return "InSubQueryFilter [" + (columnName != null ? "columnName=" + columnName + ", " : "")
				+ (subQueryColumnName != null ? "subQueryColumnName=" + subQueryColumnName + ", " : "")
				+ (modifier != null ? "modifier=" + modifier + ", " : "")
				+ (subQuery != null ? "subQuery=" + subQuery + ", " : "")
				+ "sqlBuilt=" + sqlBuilt + ", "
				+ (sqlWhereClause != null ? "sqlWhereClause=" + sqlWhereClause + ", " : "")
				+ (sqlParams != null ? "sqlParams=" + sqlParams + ", " : "")
				+ (_subQueryValues != null ? "_subQueryValues=" + _subQueryValues : "")
				+ "]";
	}

	@Override
	public String getSql()
	{
		buildSql();
		return sqlWhereClause;
	}

	@Override
	public List<Object> getSqlParams(Properties ctx)
	{
		buildSql();
		return sqlParams;
	}

	/**
	 * Build the sub Query SQL using EXISTS if the table names of the query and the subQuery are different or using IN otherwise
	 */
	private final void buildSql()
	{
		if (sqlBuilt)
		{
			return;
		}

		final TypedSqlQuery<?> subQueryImpl = TypedSqlQuery.cast(subQuery);

		String subWhereClause;
		if (tableName == null || tableName.equals(subQuery.getTableName()))
			
		{
			//task 08957
			// In case the sub query is done on the same table as the parent table, we can't write it with "EXISTS"
			// Because we don't have aliases. 
			// Therefore, in such cases, we have to keep the writing with "IN"
			subWhereClause = buildSql_UsingIN(subQueryImpl);
		}
		else
		{
			// For all the other cases it is more efficient to write the subquery using "EXISTS"
			subWhereClause = buildSql_UsingEXISTS(subQueryImpl);
		}

		final List<Object> subQueryParams = subQueryImpl.getParametersEffective();

		this.sqlWhereClause = subWhereClause.toString();
		this.sqlParams = subQueryParams;
		this.sqlBuilt = true;
	}

	private String buildSql_UsingEXISTS(final TypedSqlQuery<?> subQueryImpl)
	{
		final String subQueryColumnNameWithModifier = modifier.getColumnSql(this.subQueryColumnName);

		final StringBuilder subQuerySelectClause = new StringBuilder()
				.append("SELECT ").append(1)
				.append(" FROM ").append(subQueryImpl.getTableName());
		final boolean subQueryUseOrderByClause = false;

		final StringBuilder linkingQuery = new StringBuilder()
				.append(subQuery.getTableName() + "." + subQueryColumnName)
				.append(" = ")
				.append(this.tableName + "." + this.columnName);

		final String initialWhereClause = subQueryImpl.getWhereClause();
		final TypedSqlQuery<?> subQueryImpNew = subQueryImpl.setWhereClause(linkingQuery.toString() + " AND " + initialWhereClause);
		final String subQuerySql = subQueryImpNew.buildSQL(subQuerySelectClause, subQueryUseOrderByClause)
				//
				// Make sure the ID column for which we are search for is NOT NULL.
				// Otherwise, if there is any row where this column is null, PostgreSQL will return false no matter what (fuck you PG for that, btw).
				+ " and " + subQueryColumnNameWithModifier + " is not null";

		final StringBuilder sqlWhereClause = new StringBuilder()
				.append(" EXISTS (")
				.append(subQuerySql)
				.append(")");

		return sqlWhereClause.toString();
	}

	private String buildSql_UsingIN(final TypedSqlQuery<?> subQueryImpl) 
	{
		// warn the dev in case a query needs IN instead of EXISTS
		if (Services.get(IDeveloperModeBL.class).isEnabled())
		{
			final AdempiereException e = new AdempiereException("The query has to be written with In instead of EXISTS because the tablename is the same for both query and sub query:"
					+  this.toString());
			
			logger.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		
		logger.log(Level.FINE, "The query has to be written with In instead of EXISTS because the tablename is the same for both query and sub query:"
				+  this.toString());
	
		
		final String subQueryColumnNameWithModifier = modifier.getColumnSql(this.subQueryColumnName);

		final StringBuilder subQuerySelectClause = new StringBuilder()
				.append("SELECT ").append(subQueryColumnNameWithModifier)
				.append(" FROM ").append(subQueryImpl.getTableName());
		final boolean subQueryUseOrderByClause = false;
		final String subQuerySql = subQueryImpl.buildSQL(subQuerySelectClause, subQueryUseOrderByClause)
				//
				// Make sure the ID column for which we are search for is NOT NULL.
				// Otherwise, if there is any row where this column is null, PostgreSQL will return false no matter what (fuck you PG for that, btw).
				+ " and " + subQueryColumnNameWithModifier + " is not null";

		final String columnNameWithModifier = modifier.getColumnSql(this.columnName);
		final StringBuilder sqlWhereClause = new StringBuilder()
				.append(columnNameWithModifier).append(" IN (")
				.append(subQuerySql)
				.append(")");

		return sqlWhereClause.toString();
	}

	@Override
	public boolean accept(T model)
	{
		final List<Object> subQueryValues = getSubQueryValues(model);
		if (subQueryValues.isEmpty())
		{
			return false;
		}

		final Object modelValue0 = InterfaceWrapperHelper.getValue(model, columnName).orNull();
		final Object modelValue = modifier.convertValue(columnName, modelValue0, model);

		for (final Object subQueryValue : subQueryValues)
		{
			if (Check.equals(subQueryValue, modelValue))
			{
				return true;
			}
		}

		return false;
	}

	private List<Object> getSubQueryValues(final T model)
	{
		if (_subQueryValues != null)
		{
			return _subQueryValues;
		}

		final List<?> subQueryResult = subQuery.list();

		final List<Object> subQueryValues = new ArrayList<Object>(subQueryResult.size());
		for (Object subModel : subQueryResult)
		{
			final Object value0 = InterfaceWrapperHelper.getValue(subModel, this.subQueryColumnName).orNull();
			final Object value = modifier.convertValue(IQueryFilterModifier.COLUMNNAME_Constant, value0, model);
			subQueryValues.add(value);
		}

		this._subQueryValues = subQueryValues;
		return subQueryValues;
	}
}