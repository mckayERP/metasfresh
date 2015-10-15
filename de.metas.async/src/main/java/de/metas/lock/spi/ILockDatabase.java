package de.metas.lock.spi;

/*
 * #%L
 * de.metas.async
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


import org.adempiere.ad.dao.IQueryBuilder;
import org.compiere.model.IQuery;

import de.metas.lock.api.ILock;
import de.metas.lock.api.ILockCommand;
import de.metas.lock.api.ILockManager;
import de.metas.lock.api.IUnlockCommand;
import de.metas.lock.api.LockOwner;

/**
 * Locks database.
 * 
 * @author tsa
 *
 */
public interface ILockDatabase
{
	boolean isLocked(Class<?> modelClass, int recordId, ILock lockedBy);

	boolean isLocked(int adTableId, int recordId, ILock lockedBy);

	boolean isLocked(Object model, ILock lockedBy);

	ILock lock(ILockCommand lockCommand);

	int unlock(IUnlockCommand unlockCommand);

	<T> T retrieveAndLock(IQuery<T> query, Class<T> clazz);

	String getNotLockedWhereClause(String tableName, String joinColumnNameFQ);

	/**
	 * See {@link ILockManager#getLockedWhereClause(Class, String, ILock)}.
	 * 
	 * @param modelClass
	 * @param joinColumnNameFQ
	 * @param lock
	 * @return
	 */
	String getLockedWhereClause(Class<?> modelClass, String joinColumnNameFQ, ILock lock);

	ILock retrieveLockForOwner(LockOwner lockOwner);

	/**
	 * See {@link ILockManager#getLockedRecordsQueryBuilder(Class)}.
	 * 
	 * @param modelClass
	 * @return
	 */
	<T> IQueryBuilder<T> getLockedRecordsQueryBuilder(Class<T> modelClass);
}