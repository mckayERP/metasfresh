/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.ad.trx.api.OnTrxMissingPolicy;
import org.adempiere.ad.trx.spi.ITrxListener;
import org.adempiere.ad.trx.spi.TrxListenerAdapter;
import org.adempiere.event.Event;
import org.adempiere.event.IEventBus;
import org.adempiere.event.IEventBusFactory;
import org.adempiere.event.IEventListener;
import org.adempiere.event.Topic;
import org.adempiere.event.Type;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.WeakList;
import org.adempiere.util.jmx.JMXRegistry;
import org.adempiere.util.jmx.JMXRegistry.OnJMXAlreadyExistsPolicy;
import org.adempiere.util.lang.ITableRecordReference;
import org.adempiere.util.lang.impl.TableRecordReference;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Adempiere Cache Management
 *
 * @author Jorg Janke
 * @version $Id: CacheMgt.java,v 1.2 2006/07/30 00:54:35 jjanke Exp $
 */
public final class CacheMgt
{
	/**
	 * Get Cache Management
	 * 
	 * @return Cache Manager
	 */
	public static final CacheMgt get()
	{
		return s_cache;
	}	// get

	/** Singleton */
	private static final CacheMgt s_cache = new CacheMgt();

	public static final String JMX_BASE_NAME = "org.adempiere.cache";

	/**
	 * Private Constructor
	 */
	private CacheMgt()
	{
		super();

		JMXRegistry.get().registerJMX(new JMXCacheMgt(), OnJMXAlreadyExistsPolicy.Replace);
	}

	public static final int RECORD_ID_ALL = -1;

	/** List of Instances */
	private final WeakList<CacheInterface> cacheInstances = new WeakList<CacheInterface>();
	private final ReentrantLock cacheInstancesLock = cacheInstances.getReentrantLock();

	/**
	 * List of Table Names.
	 * 
	 * i.e. map of TableName to "how many cache instances do we have for that table name"
	 */
	private final Map<String, AtomicInteger> tableNames = new HashMap<>();

	/** Logger */
	static final transient CLogger log = CLogger.getCLogger(CacheMgt.class);

	/**
	 * Enable caches for the given table to be invalidated by remote events. Example: if a user somewhere else opens/closes a period, we can allow the system to invalidate the local cache to avoid it
	 * becoming stale.
	 * 
	 * @param tableName
	 */
	public final void enableRemoteCacheInvalidationForTableName(final String tableName)
	{
		RemoteCacheInvalidationHandler.instance.enableForTableName(tableName);
	}

	/**************************************************************************
	 * Register Cache Instance
	 *
	 * @param instance Cache
	 * @return true if added
	 */
	public boolean register(final CacheInterface instance)
	{
		if (instance == null)
		{
			return false;
		}

		cacheInstancesLock.lock();
		try
		{
			final String tableName = getTableNameOrNull(instance);
			final boolean registerWeak;
			if (tableName != null)
			{
				//
				// Add to TableName count
				AtomicInteger count = tableNames.get(tableName);
				if (count == null)
				{
					count = new AtomicInteger(0);
					tableNames.put(tableName, count);
				}
				count.incrementAndGet();

				registerWeak = true;
			}
			else
			{
				// NOTE: if the cache is not providing an TableName, we register them with a hard-reference because probably is a cache listener
				registerWeak = false;
			}

			return cacheInstances.add(instance, registerWeak);
		}
		finally
		{
			cacheInstancesLock.unlock();
		}
	}	// register

	/**
	 * Un-Register Cache Instance
	 *
	 * @param instance Cache
	 * @return true if removed
	 */
	public boolean unregister(final CacheInterface instance)
	{
		if (instance == null)
		{
			return false;
		}

		final String tableName = getTableNameOrNull(instance);

		cacheInstancesLock.lock();
		try
		{
			int countRemoved = 0;

			// Could be included multiple times
			final int size = cacheInstances.size();
			for (int i = size - 1; i >= 0; i--)
			{
				final CacheInterface stored = cacheInstances.get(i);
				if (instance.equals(stored))
				{
					cacheInstances.remove(i);
					countRemoved++;
				}
			}

			//
			// Remove it from tableNames
			if (tableName != null)
			{
				final AtomicInteger count = tableNames.remove(tableName);
				if (count == null)
				{
					// let it removed
				}
				else
				{
					final int countNew = count.get() - countRemoved;
					if (countNew > 0)
					{
						count.set(countNew);
						tableNames.put(tableName, count);
					}
				}
			}

			final boolean found = countRemoved > 0;
			return found;
		}
		finally
		{
			cacheInstancesLock.unlock();
		}
	}	// unregister

	/**
	 * Extracts the TableName from given cache instance.
	 * 
	 * @param instance
	 * @return table name or <code>null</code> if table name could not be extracted
	 */
	private static final String getTableNameOrNull(final CacheInterface instance)
	{
		if (instance instanceof ITableAwareCacheInterface)
		{
			final ITableAwareCacheInterface recordsCache = (ITableAwareCacheInterface)instance;

			// Try cache TableName
			final String tableName = recordsCache.getTableName();
			if (tableName != null && !tableName.isEmpty())
			{
				return tableName;
			}

			// Try cache Name
			final String cacheName = recordsCache.getName();
			if (cacheName != null && !cacheName.isEmpty())
			{
				return cacheName;
			}

			// Fallback: return null because there is no table, no cache name
			return null;
		}

		// Fallback for any other cache interfaces: return null because TableName is not available
		return null;
	}

	public Set<String> getTableNames()
	{
		return ImmutableSet.copyOf(tableNames.keySet());
	}

	public Set<String> getTableNamesToBroadcast()
	{
		return RemoteCacheInvalidationHandler.instance.getTableNamesToBroadcast();
	}

	/**
	 * Invalidate ALL cached entries of all registered {@link CacheInterface}s.
	 * 
	 * @return how many cache entries were invalidated
	 */
	public int reset()
	{
		// Do nothing if already running (i.e. avoid recursion)
		if (cacheResetRunning.getAndSet(true))
		{
			return 0;
		}

		cacheInstancesLock.lock();
		int counter = 0;
		int total = 0;
		try
		{

			for (final CacheInterface cacheInstance : cacheInstances)
			{
				if (cacheInstance != null && cacheInstance.size() > 0)
				{
					total += resetNoFail(cacheInstance);
					counter++;
				}
			}
		}
		finally
		{
			cacheInstancesLock.unlock();
			cacheResetRunning.set(false);
		}

		if (log.isLoggable(Level.INFO))
		{
			log.info("" + counter + " cache instances invalidated (" + total + " cached items invalidated)");
		}

		return total;
	}	// reset

	private final transient AtomicBoolean cacheResetRunning = new AtomicBoolean();

	/**
	 * Invalidate all cached entries for given TableName.
	 * 
	 * @param tableName table name
	 * @return how many cache entries were invalidated
	 */
	public int reset(final String tableName)
	{
		final int recordId = RECORD_ID_ALL;
		return reset(tableName, recordId);
	}	// reset

	/**
	 * Invalidate all cached entries for given TableName/Record_ID.
	 * 
	 * @param tableName table name
	 * @param recordId record if applicable or {@link #RECORD_ID_ALL} for all
	 * @return how many cache entries were invalidated
	 */
	public int reset(final String tableName, final int recordId)
	{
		final boolean broadcast = true;
		return reset(tableName, recordId, broadcast);
	}

	/**
	 * Reset cache for TableName/Record_ID when given transaction is committed.
	 * 
	 * If no transaction was given or given transaction was not found, the cache is reset right away.
	 * 
	 * @param trxName
	 * @param tableName
	 * @param recordId
	 */
	public void resetOnTrxCommit(final String trxName, final String tableName, final int recordId)
	{
		final ITrxManager trxManager = Services.get(ITrxManager.class);
		final ITrx trx = trxManager.get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
		if (trxManager.isNull(trx))
		{
			reset(tableName, recordId);
			return;
		}

		RecordsToResetOnTrxCommitCollector.getCreate(trx).addRecord(tableName, recordId);
	}

	/**
	 * Invalidate all cached entries for given TableName/Record_ID.
	 * 
	 * @param tableName
	 * @param recordId
	 * @param broadcast true if we shall also broadcast this remotely.
	 * @return how many cache entries were invalidated
	 */
	private final int reset(final String tableName, final int recordId, final boolean broadcast)
	{
		if (tableName == null)
		{
			return reset();
		}

		cacheInstancesLock.lock();
		try
		{
			int counter = 0;
			int total = 0;

			//
			// Invalidate local caches if we have at least one cache interface about our table
			if (tableNames.containsKey(tableName))
			{
				for (final CacheInterface cacheInstance : cacheInstances)
				{
					if (cacheInstance == null)
					{
						// nothing to reset
					}
					else if (cacheInstance instanceof ITableAwareCacheInterface)
					{
						final ITableAwareCacheInterface recordsCache = (ITableAwareCacheInterface)cacheInstance;
						final int itemsRemoved = recordsCache.resetForRecordId(tableName, recordId);
						if (itemsRemoved > 0)
						{
							log.log(Level.FINE, "Rest cache instance: {0}", cacheInstance);
							total += itemsRemoved;
							counter++;
						}
					}
				}
			}
			//
			if (log.isLoggable(Level.INFO))
			{
				log.info(tableName + ": " + counter + " cache interfaces checked (" + total + " records invalidated)");
			}


			//
			// Broadcast cache invalidation.
			// We do this, even if we don't have any cache interface registered locally, because there might be remotely.
			if (broadcast)
			{
				RemoteCacheInvalidationHandler.instance.postEvent(tableName, recordId);
			}

			return total;
		}
		finally
		{
			cacheInstancesLock.unlock();
		}
	}	// reset

	/**
	 * Total Cached Elements
	 *
	 * @return count
	 */
	public int getElementCount()
	{
		int total = 0;
		cacheInstancesLock.lock();
		try
		{
			for (final CacheInterface cacheInstance : cacheInstances)
			{
				if (cacheInstance == null)
				{
					// do nothing
				}
				else
				{
					total += cacheInstance.size();
				}
			}
		}
		finally
		{
			cacheInstancesLock.unlock();
		}
		return total;
	}	// getElementCount

	/**
	 * String Representation
	 *
	 * @return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("CacheMgt[");
		sb.append("Instances=")
				.append(cacheInstances.size())
				.append("]");
		return sb.toString();
	}	// toString

	/**
	 * Extended String Representation
	 *
	 * @return info
	 */
	public String toStringX()
	{
		StringBuilder sb = new StringBuilder("CacheMgt[");
		sb.append("Instances=")
				.append(cacheInstances.size())
				.append(", Elements=").append(getElementCount())
				.append("]");
		return sb.toString();
	}	// toString

	/**
	 * Reset cache and clear ALL registered {@link CacheInterface}s.
	 */
	public void clear()
	{
		cacheInstancesLock.lock();
		try
		{
			reset();

			// Make sure all cache instances are reset
			for (final CacheInterface cacheInstance : cacheInstances)
			{
				if (cacheInstance == null)
				{
					continue;
				}
				resetNoFail(cacheInstance);
			}

			cacheInstances.clear();
			tableNames.clear();
		}
		finally
		{
			cacheInstancesLock.unlock();
		}

	}

	private int resetNoFail(final CacheInterface cacheInstance)
	{
		try
		{
			return cacheInstance.reset();
		}
		catch (Exception e)
		{
			// log but don't fail
			log.log(Level.WARNING, "Error while reseting " + cacheInstance, e);
			return 0;
		}
	}

	/** Bidirectional binding between local cache system and remote cache systems */
	private static final class RemoteCacheInvalidationHandler implements IEventListener
	{
		public static final transient CacheMgt.RemoteCacheInvalidationHandler instance = new CacheMgt.RemoteCacheInvalidationHandler();

		private static final Topic TOPIC_CacheInvalidation = Topic.builder()
				.setName("org.compiere.util.CacheMgt.CacheInvalidation")
				.setType(Type.REMOTE)
				.build();
		private static final String EVENT_PROPERTY_TableName = "TableName";
		private static final String EVENT_PROPERTY_Record_ID = "Record_ID";

		private boolean _initalized = false;
		private final Set<String> tableNamesToBroadcast = Sets.newConcurrentHashSet();

		private RemoteCacheInvalidationHandler()
		{
			super();
		}

		public final synchronized void enable()
		{
			// Do nothing if already registered.
			if (_initalized)
			{
				return;
			}

			//
			// Globally register this listener.
			// We register it globally because we want to survive.
			final IEventBusFactory eventBusFactory = Services.get(IEventBusFactory.class);
			eventBusFactory.registerGlobalEventListener(TOPIC_CacheInvalidation, instance);

			_initalized = true;
		}

		private final boolean isEnabled()
		{
			return _initalized;
		}

		/**
		 * Enable cache invalidation broadcasting for given table name.
		 * 
		 * @param tableName
		 */
		public synchronized void enableForTableName(final String tableName)
		{
			Check.assumeNotEmpty(tableName, "tableName not empty");

			enable();
			tableNamesToBroadcast.add(tableName);
		}

		public synchronized Set<String> getTableNamesToBroadcast()
		{
			return ImmutableSet.copyOf(tableNamesToBroadcast);
		}

		/**
		 * Broadcast a cache invalidation request.
		 * 
		 * @param tableName
		 * @param recordId
		 */
		public void postEvent(final String tableName, final int recordId)
		{
			// Do nothing if cache invalidation broadcasting is not enabled
			if (!isEnabled())
			{
				return;
			}

			// Do nothing if given table name is not in our table names to broadcast list
			if (!tableNamesToBroadcast.contains(tableName))
			{
				return;
			}

			// Broadcast the event.
			final Event event = Event.builder()
					.putProperty(EVENT_PROPERTY_TableName, tableName)
					.putProperty(EVENT_PROPERTY_Record_ID, recordId < 0 ? RECORD_ID_ALL : recordId)
					.build();
			Services.get(IEventBusFactory.class)
					.getEventBus(TOPIC_CacheInvalidation)
					.postEvent(event);
			if (log.isLoggable(Level.FINE))
			{
				log.fine("Broadcasting cache invalidation of " + tableName + "/" + recordId + ", event=" + event);
			}
		}

		/**
		 * Called when we got a remote cache invalidation request. It tries to invalidate local caches.
		 */
		@Override
		public void onEvent(final IEventBus eventBus, final Event event)
		{
			// Ignore local events because they were fired from CacheMgt.reset methods.
			// If we would not do so, we would have an infinite loop here.
			if (event.isLocalEvent())
			{
				return;
			}

			//
			// TableName
			final String tableName = event.getProperty(EVENT_PROPERTY_TableName);
			if (Check.isEmpty(tableName, true))
			{
				log.log(Level.INFO, "Ignored event event without tableName set: {0}", event);
				return;
			}
			// NOTE: we try to invalidate the local cache even if the tableName is not in our tableNames to broadcast list.

			//
			// Record_ID
			Integer recordId = event.getProperty(EVENT_PROPERTY_Record_ID);
			if (recordId == null || recordId < 0)
			{
				recordId = RECORD_ID_ALL;
			}

			//
			// Reset cache for TableName/Record_ID
			if (log.isLoggable(Level.FINE))
			{
				log.log(Level.FINE, "Reseting cache for " + tableName + "/" + recordId + " because we got remote event: " + event);
			}
			final boolean broadcast = false; // don't broadcast it anymore because else we would introduce recursion
			CacheMgt.get().reset(tableName, recordId, broadcast);
		}
	}

	/** Collects records that needs to be removed from cache when a given transaction is committed */
	private static final class RecordsToResetOnTrxCommitCollector extends TrxListenerAdapter
	{
		/** Gets/creates the records collector which needs to be reset when transaction is committed */
		public static final RecordsToResetOnTrxCommitCollector getCreate(final ITrx trx)
		{
			return trx.getProperty(TRX_PROPERTY, new Supplier<RecordsToResetOnTrxCommitCollector>()
			{

				@Override
				public RecordsToResetOnTrxCommitCollector get()
				{
					final RecordsToResetOnTrxCommitCollector collector = new RecordsToResetOnTrxCommitCollector();
					trx.getTrxListenerManager().registerListener(ResetCacheOnCommitTrxListener);
					return collector;

				}
			});
		}

		private static final String TRX_PROPERTY = RecordsToResetOnTrxCommitCollector.class.getName();

		/** Listens {@link ITrx}'s after-commit and fires enqueued cache invalidation requests */
		private static final ITrxListener ResetCacheOnCommitTrxListener = new TrxListenerAdapter()
		{
			@Override
			public void afterCommit(final ITrx trx)
			{
				final RecordsToResetOnTrxCommitCollector collector = trx.getProperty(TRX_PROPERTY);
				if (collector == null)
				{
					return;
				}

				collector.run();
			}
		};

		private final Set<ITableRecordReference> records = Sets.newConcurrentHashSet();

		/** Enqueues a record */
		public final void addRecord(final String tableName, final int recordId)
		{
			if (Check.isEmpty(tableName, true))
			{
				return;
			}
			if (recordId <= 0)
			{
				return;
			}
			final TableRecordReference record = new TableRecordReference(tableName, recordId);
			records.add(record);
			
			log.log(Level.FINE, "Scheduled cache invalidation on transaction commit: {0}", record);
		}

		/** Reset the cache for all enqueued records */
		private void run()
		{
			if (records.isEmpty())
			{
				return;
			}

			final CacheMgt cacheMgt = CacheMgt.get();
			for (final ITableRecordReference record : records)
			{
				cacheMgt.reset(record.getTableName(), record.getRecord_ID());
			}

			records.clear();
		}
	}
}