package de.metas.adempiere.util.cache;

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


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.ad.trx.api.OnTrxMissingPolicy;
import org.adempiere.util.Services;
import org.adempiere.util.proxy.AroundInvoke;
import org.adempiere.util.proxy.Cached;
import org.adempiere.util.proxy.IInvocationContext;
import org.adempiere.util.proxy.impl.JavaAssistInterceptor;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.Util.ArrayKey;

import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public @Cached// @Interceptor
class CacheInterceptor implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6740693287832574641L;

	// services
	private static final transient CLogger logger = CLogger.getCLogger(CacheInterceptor.class);
	private final transient ITrxManager trxManager = Services.get(ITrxManager.class);

	private static final LoadingCache<Method, CachedMethodDescriptor> cachedMethodsDescriptor = CacheBuilder.newBuilder()
			.build(new CacheLoader<Method, CachedMethodDescriptor>()
			{

				@Override
				public CachedMethodDescriptor load(final Method method) throws Exception
				{
					try
					{
						return new CachedMethodDescriptor(method);
					}
					catch (Exception e)
					{
						throw CacheIntrospectionException.wrapIfNeeded(e)
								.setMethod(method);
					}
				}
			});

	private static final CacheBuilder<Object, Object> _cacheStorageBuilder = CacheBuilder.newBuilder();
	private final Cache<String, CCache<ArrayKey, Object>> _cacheStorage = _cacheStorageBuilder.build();

	private static final String TRX_PROPERTY_CacheStorage = CacheInterceptor.class.getName() + ".CacheStorage";
	private static final Supplier<Cache<String, CCache<ArrayKey, Object>>> TRX_PROPERTY_CacheStorageInitializer = new Supplier<Cache<String, CCache<ArrayKey, Object>>>()
	{
		@Override
		public Cache<String, CCache<ArrayKey, Object>> get()
		{
			return _cacheStorageBuilder.build();
		}
	};

	/**
	 * 
	 * @param invCtx
	 * @return
	 * @throws Throwable could throw throwable as the cached method could also throw it.
	 */
	@AroundInvoke
	public Object invokeCache(final IInvocationContext invCtx) throws Throwable
	{
		if (logger.isLoggable(Level.FINEST))
		{
			logger.finest("Entering - invCtx: " + invCtx);
		}

		//
		// Get method descriptor
		final Method method = invCtx.getMethod();
		final CachedMethodDescriptor methodDescriptor;
		try
		{
			methodDescriptor = cachedMethodsDescriptor.get(method);
		}
		catch (Exception e)
		{
			final CacheIntrospectionException cacheEx = CacheIntrospectionException.wrapIfNeeded(e);
			if (JavaAssistInterceptor.FAIL_ON_ERROR)
			{
				throw cacheEx;
			}
			else
			{
				logger.log(Level.WARNING, "Failed introspecting cached method: " + method, cacheEx);
			}

			// Invoke the cached method directly
			final Object result = invCtx.proceed();
			return result;
		}

		//
		// Build cache key
		final CacheKeyBuilder cacheKeyBuilder = methodDescriptor.createKeyBuilder(invCtx.getTarget(), invCtx.getParameters());
		if (cacheKeyBuilder.isSkipCaching())
		{
			// Invoke the cached method directly
			final Object result = invCtx.proceed();
			return result;
		}
		
		//
		// Get the Cache Storage.
		// In case the cache storage could not be retrieved, we are invoking the cached method directly (by-pass the cache).
		final Cache<String, CCache<ArrayKey, Object>> cacheStorage = getCacheStorage(cacheKeyBuilder.getTrxName());
		if (cacheStorage == null)
		{
			final CacheGetException ex = new CacheGetException("Could not get the cache storage, maybe because transaction was not found"
					+ "\n TrxName: " + cacheKeyBuilder.getTrxName()
					+ "\n Method: " + method
					+ "\n Method arguments: " + (invCtx == null ? "-" : Arrays.asList(invCtx.getParameters()))
					+ "\n Target Object: " + invCtx.getTarget()
					+ "\n Method descriptor: " + methodDescriptor);
			logger.log(Level.WARNING, "No cache storage found. Invoking cached method directly.", ex);

			// Invoke the cached method directly
			final Object result = invCtx.proceed();
			return result;
		}

		//
		// Get the method level cache container (Method's parameters key -> cached value) 
		final ArrayKey cacheKey = cacheKeyBuilder.buildKey();
		final CCache<ArrayKey, Object> methodCache = cacheStorage.get(methodDescriptor.getCacheName(), methodDescriptor.createCCacheCallable());

		//
		// Get method's cached value / update method's cached value
		final Object cacheResult;
		if (cacheKeyBuilder.isCacheReload())
		{
			cacheResult = invCtx.call();
			methodCache.put(cacheKey, cacheResult);
		}
		else
		{
			cacheResult = methodCache.get(cacheKey, invCtx);
		}

		// Unbox the NullResult and return the cached value
		return cacheResult == IInvocationContext.NullResult ? null : cacheResult;
	}

	/**
	 * @param trxName
	 * @return cache storage or null if not found
	 */
	private final Cache<String, CCache<ArrayKey, Object>> getCacheStorage(final String trxName)
	{
		//
		// If we have a transaction, we shall use transaction's cache
		if (!trxManager.isNull(trxName))
		{
			final ITrx trx = trxManager.get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
			if (trxManager.isNull(trx))
			{
				// If it was about a thread inherited trxName, and no trx was found
				// => we shall go with the local cache, for sure
				if (ITrx.TRXNAME_ThreadInherited == trxName)
				{
					return _cacheStorage;
				}

				// Else, return "null", i.e. cache storage not found
				return null;
			}

			return trx.getProperty(TRX_PROPERTY_CacheStorage, TRX_PROPERTY_CacheStorageInitializer);
		}

		//
		// Use our local cache
		return _cacheStorage;
	}
}