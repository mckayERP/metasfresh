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


import java.util.List;

import org.adempiere.ad.dao.IQueryFilter;

/**
 * An extension of {@link SqlQueryFilter} which also implements {@link IQueryFilter} (which is typed).
 * 
 * @author tsa
 * 
 * @param <T>
 */
public final class TypedSqlQueryFilter<T> extends SqlQueryFilter implements IQueryFilter<T>
{
	public TypedSqlQueryFilter(String sql)
	{
		super(sql);
	}

	public TypedSqlQueryFilter(String sql, Object[] params)
	{
		super(sql, params);
	}

	public TypedSqlQueryFilter(String sql, List<Object> params)
	{
		super(sql, params);
	}

	@Override
	public boolean accept(T model)
	{
		throw new UnsupportedOperationException();
	}
}