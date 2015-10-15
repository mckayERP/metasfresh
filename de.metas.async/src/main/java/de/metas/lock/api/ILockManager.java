package de.metas.lock.api;

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
import org.adempiere.util.ISingletonService;
import org.compiere.model.IQuery;

import de.metas.lock.exceptions.LockFailedException;

/**
 * Lock manager - this is the starting point for manipulating the locks
 * 
 * @author tsa
 *
 */
public interface ILockManager extends ISingletonService
{
	/** @return true if given record is locked */
	boolean isLocked(int adTableId, int recordId);

	/** @return true if given record is locked */
	boolean isLocked(Class<?> modelClass, int recordId);

	/**
	 * 
	 * @param modelClass
	 * @param recordId
	 * @param lockedBy lock or null
	 * @return true if the record is locked by given {@link ILock}. If <code>lockedBy</code> is null, the underlying API will check if the record is locked by any lock.
	 */
	boolean isLocked(Class<?> modelClass, int recordId, ILock lockedBy);

	/** Starting building a lock command */
	ILockCommand lock();

	/**
	 * Tries to lock given persistent model
	 * 
	 * @param model
	 * @return true if locked, false if the record was already locked
	 */
	boolean lock(Object model);

	/**
	 * Tries to lock given persistent model
	 * 
	 * @param adTableId
	 * @param recordId
	 * @return true if locked, false if the record was already locked
	 */
	boolean lock(int adTableId, int recordId);

	/** Starting building a unlock command */
	IUnlockCommand unlock();

	/**
	 * Tries to unlock given persistent model
	 * 
	 * @param model
	 * @return true if unlocked
	 */
	boolean unlock(Object model);

	/** @return true if given model is locked by any lock */
	boolean isLocked(Object model);

	/**
	 * Retrieves next model from query and locks it (using {@link LockOwner#NONE}.
	 * 
	 * @return retrieved record (already locked)
	 */
	<T> T retrieveAndLock(IQuery<T> query, Class<T> clazz);

	/**
	 * Builds a SQL where clause to be used in other queries to filter the results.
	 * 
	 * For example, for table name 'MyTable' and join column 'MyTable.MyTable_ID', this method could return:
	 * 
	 * <pre>
	 * NOT EXISTS ("SELECT 1 FROM LockTable zz WHERE zz.AD_Table_ID=12345 AND zz.Record_ID=MyTable.MyTable_ID
	 * </pre>
	 * 
	 * but this is dependent on implementation.
	 * 
	 * @param tableName
	 * @param joinColumnName fully qualified record id column name
	 * @return SQL where clause
	 */
	String getNotLockedWhereClause(String tableName, String joinColumnNameFQ);

	/**
	 * Builds a SQL where clause used to filter records locked by a given {@link ILock}. Also see {@link #getNotLockedWhereClause(String, String)} for information.
	 * 
	 * @param modelClass
	 * @param joinColumnNameFQ
	 * @param lock may <b>not</b> be <code>null</code>
	 * @return SQL where clause
	 */
	String getLockedWhereClause(Class<?> modelClass, String joinColumnNameFQ, ILock lock);

	/**
	 * Gets existing lock for given owner name
	 * 
	 * @param lockOwner
	 * @return existing lock
	 * @throws LockFailedException in case lock does not exist
	 */
	ILock getExistingLockForOwner(LockOwner lockOwner);

	/**
	 * Create and return a query builder that allows to retrieve all records of the given <code>modelClass</code> which are currently locked.
	 * <p>
	 * Note that the query builder does not specify any ordering.
	 * 
	 * @param modelClass
	 * @return
	 */
	<T> IQueryBuilder<T> getLockedRecordsQueryBuilder(Class<T> modelClass);
}