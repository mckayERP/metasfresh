package org.adempiere.ad.trx.api.impl;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.adempiere.ad.service.IDeveloperModeBL;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxListenerManager;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.ad.trx.api.ITrxNameGenerator;
import org.adempiere.ad.trx.api.ITrxRunConfig;
import org.adempiere.ad.trx.api.ITrxRunConfig.OnRunnableFail;
import org.adempiere.ad.trx.api.ITrxRunConfig.OnRunnableSuccess;
import org.adempiere.ad.trx.api.ITrxRunConfig.TrxPropagation;
import org.adempiere.ad.trx.api.ITrxSavepoint;
import org.adempiere.ad.trx.api.OnTrxMissingPolicy;
import org.adempiere.ad.trx.exceptions.IllegalTrxRunStateException;
import org.adempiere.ad.trx.exceptions.OnTrxMissingPolicyNotSupportedException;
import org.adempiere.ad.trx.exceptions.TrxException;
import org.adempiere.ad.trx.exceptions.TrxNotFoundException;
import org.adempiere.ad.trx.jmx.JMXTrxManager;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.jmx.JMXRegistry;
import org.adempiere.util.jmx.JMXRegistry.OnJMXAlreadyExistsPolicy;
import org.adempiere.util.trxConstraints.api.IOpenTrxBL;
import org.adempiere.util.trxConstraints.api.ITrxConstraints;
import org.adempiere.util.trxConstraints.api.ITrxConstraintsBL;
import org.compiere.util.CLogger;
import org.compiere.util.TrxRunnable;
import org.compiere.util.TrxRunnable2;
import org.compiere.util.TrxRunnable2Wrapper;
import org.compiere.util.Util;

/**
 * Abstract {@link ITrxManager} implementation without any dependencies on a native stuff.
 *
 * @author tsa
 *
 */
public abstract class AbstractTrxManager implements ITrxManager
{
	protected final transient CLogger logger = CLogger.getCLogger(getClass());

	/**
	 * Active Transactions Map: trxName to {@link ITrx}
	 */
	private final Map<String, ITrx> trxName2trx = new HashMap<String, ITrx>();
	private final ReentrantLock trxName2trxLock = new ReentrantLock();

	private ITrxNameGenerator trxNameGenerator = DefaultTrxNameGenerator.instance;

	/**
	 * Thread level transaction name
	 */
	private final InheritableThreadLocal<String> threadLocalTrx = new InheritableThreadLocal<>();
	private final InheritableThreadLocal<OnRunnableFail> threadLocalOnRunnableFail = new InheritableThreadLocal<>();

	private boolean debugTrxCreateStacktrace = false;
	private boolean debugTrxCloseStacktrace = false;
	/**
	 * Debugging: closed transactions list.
	 *
	 * If not null it will collect transactions which were cloased and removed from {@link #getActiveTransactionsList()}.
	 */
	private List<ITrx> debugClosedTransactionsList = null;
	private boolean debugConnectionBackendId = false;

	public AbstractTrxManager()
	{
		super();

		final JMXTrxManager jmxBean = new JMXTrxManager(this);
		JMXRegistry.get().registerJMX(jmxBean, OnJMXAlreadyExistsPolicy.Replace);
	}

	/**
	 * Creates and returns a transaction for given name.
	 * 
	 * NOTE: implementations of this method shall not register or start the transaction. They only need to create the actual {@link ITrx} object.
	 *
	 * @param trxName transaction name; please keep in mind: this is the actual trxName that will be used and not a prefix
	 * @return create transaction; never return null
	 */
	protected abstract ITrx createTrx(final String trxName);

	/**
	 * Creates and registers {@link ITrx} for given transaction name.
	 * 
	 * NOTE: this method assumes that {@link #trxName2trxLock} is already locked.
	 * 
	 * @param trxName
	 * @return created transaction name
	 */
	private final ITrx createTrxAndRegister(final String trxName)
	{
		final ITrx trx = createTrx(trxName);
		Check.assumeNotNull(trx, "trx not null"); // shall never happen, but just to make sure the contract is respected

		trxName2trxLock.lock();
		try
		{
			Services.get(IOpenTrxBL.class).onNewTrx(trx); // metas 02367
			final ITrx trxOld = trxName2trx.put(trxName, trx);

			//
			// Handle the case when a transaction already exists with exactly the same name
			if (trxOld != null)
			{
				// Log the case
				final TrxException ex = new TrxException("Possible trx leak found: registering a trxName which is already existing"
						+ "\n trxName=" + trxName
						+ "\n New trx=" + trx
						+ "\n Old trx=" + trxOld);
				if (Services.get(IDeveloperModeBL.class).isEnabled())
				{
					throw ex;
				}
				logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);

				// Try closing the old transaction
				try
				{
					trxOld.close();
				}
				catch (Exception e)
				{
					throw new TrxException("Failed closing the old transaction: " + trxOld, e);
				}
			}
		}
		finally
		{
			trxName2trxLock.unlock();
		}

		return trx;
	}

	@Override
	public ITrxRunConfig createTrxRunConfig(final TrxPropagation trxMode, final OnRunnableSuccess onRunnableSuccess, final OnRunnableFail onRunnableFail)
	{
		return new TrxRunConfig(trxMode, onRunnableSuccess, onRunnableFail);
	}

	@Override
	public ITrx getTrxOrNull(final String trxName)
	{
		if (isNull(trxName))
		{
			return ITrx.TRX_None;
		}

		return get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
	}

	@Override
	public ITrx getTrx(final String trxName)
	{
		if (isNull(trxName))
		{
			return ITrx.TRX_None;
		}

		final ITrx trx = get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
		if (isNull(trx))
		{
			throw new TrxNotFoundException(this, trxName);
		}
		return trx;
	}

	@Override
	public final ITrx get(final String trxName, final boolean createNew)
	{
		final OnTrxMissingPolicy onTrxMissingPolicy = createNew ?
				OnTrxMissingPolicy.CreateNew
				: OnTrxMissingPolicy.ReturnTrxNone // backward compatibility
		;

		return get(trxName, onTrxMissingPolicy);
	}

	@Override
	public final ITrx get(String trxName, final OnTrxMissingPolicy onTrxMissingPolicy)
	{
		Check.assumeNotNull(onTrxMissingPolicy, TrxException.class, "onTrxMissingPolicy not null");

		//
		// Avoid null trxName
		if (isNull(trxName))
		{
			// If we were asked to return None in case of missing transaction, we shall return none,
			// because the expectation of the caller is to not fail here.
			if (onTrxMissingPolicy == OnTrxMissingPolicy.ReturnTrxNone)
			{
				return ITrx.TRX_None;
			}

			throw new TrxNotFoundException("No Transaction Name");
		}

		//
		// If our trxName is the thread inherited marker, we need to resolve it to actual "trxName"
		if (ITrx.TRXNAME_ThreadInherited == trxName)
		{
			trxName = threadLocalTrx.get();
			if (trxName == null)
			{
				if (onTrxMissingPolicy == OnTrxMissingPolicy.CreateNew)
				{
					final String newTrxNamePrefix = null; // no prefix
					// NOTE: don't actual create&register the ITrx object (to avoid recursion).
					// The actual transaction will be explicitelly created later in this method.
					final boolean createTrx = false;
					trxName = createTrxName(newTrxNamePrefix, createTrx);
				}
				else if (onTrxMissingPolicy == OnTrxMissingPolicy.Fail)
				{
					throw new TrxNotFoundException("No thread inherited transaction was found (Thread: " + Thread.currentThread().getName() + ")");
				}
				else if (onTrxMissingPolicy == OnTrxMissingPolicy.ReturnTrxNone)
				{
					// No actual thread local transaction was found
					return ITrx.TRX_None;
				}
				else
				{
					throw new OnTrxMissingPolicyNotSupportedException(onTrxMissingPolicy);
				}
			}
		}

		//
		// Get/Create the ITrx from "trxName"
		trxName2trxLock.lock();
		try
		{
			ITrx trx = trxName2trx.get(trxName);
			if (trx != null)
			{
				// transaction was found => perfect
			}
			else if (onTrxMissingPolicy == OnTrxMissingPolicy.CreateNew)
			{
				trx = createTrxAndRegister(trxName);
			}
			else if (onTrxMissingPolicy == OnTrxMissingPolicy.Fail)
			{
				throw new TrxNotFoundException(this, trxName);
			}
			else if (onTrxMissingPolicy == OnTrxMissingPolicy.ReturnTrxNone)
			{
				return ITrx.TRX_None;
			}
			else
			{
				throw new OnTrxMissingPolicyNotSupportedException(onTrxMissingPolicy);
			}

			return trx;
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}	// get

	@Override
	public boolean remove(final ITrx trx)
	{
		final String trxName = trx.getTrxName();

		trxName2trxLock.lock();
		try
		{
			final ITrx trxOld = trxName2trx.remove(trxName);
			if (trxOld != null)
			{
				if (debugClosedTransactionsList != null)
				{
					debugClosedTransactionsList.add(trxOld);
				}
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}

	@Override
	public ITrx[] getActiveTransactions()
	{
		trxName2trxLock.lock();
		try
		{
			final Collection<ITrx> collections = trxName2trx.values();
			final ITrx[] trxs = new ITrx[collections.size()];
			collections.toArray(trxs);
			return trxs;
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}

	@Override
	public List<ITrx> getActiveTransactionsList()
	{
		trxName2trxLock.lock();
		try
		{
			return new ArrayList<ITrx>(trxName2trx.values());
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}

	@Override
	public String createTrxName(final String prefix)
	{
		// calling with createNew == true because that is the old (default) behavior of this method
		final boolean createTrx = true;
		return createTrxName(prefix, createTrx);
	}

	@Override
	public String createTrxName(final String prefix, final boolean createTrx)
	{
		// avoid common mistake: prefix shall not be ThreadInerited
		Check.assume(prefix != ITrx.TRXNAME_ThreadInherited, "prefix shall not be ThreadInherited transaction name marker");

		//
		// Build trxName
		final String trxName = trxNameGenerator.createTrxName(prefix);

		//
		// Actually create,register,activate transaction
		if (createTrx)
		{
			// create transaction entry
			get(trxName, OnTrxMissingPolicy.CreateNew);
		}

		return trxName;
	}	// createTrxName

	/**
	 * Create unique Transaction Name
	 *
	 * @return unique name
	 */
	public String createTrxName()
	{
		final String prefix = null;
		return createTrxName(prefix);
	}	// createTrxName

	/**
	 * Runs the given trx runnable in an internal transaction (the transaction name will start with <code>"TrxRun"</code>).
	 */
	@Override
	public void run(final TrxRunnable r)
	{
		run(ITrx.TRXNAME_None, r);
	}

	// metas: backward compatibility
	@Override
	public void run(final String trxName, final TrxRunnable r)
	{
		final boolean manageTrx = false;
		run(trxName, manageTrx, r);
	}

	/**
	 *
	 * <ul>
	 * <li>trxName=null, manageTrx in {true, false} => create a new trxName (and thus a new local trx) with prefix <code>"TrxRun"</code>
	 * <li>trxName!=null, manageTrx=true -> => create a new trxName (and thus a new local trx) with prefix being the given <code>trxName</code>
	 * <li>trxName!=null, manageTrx=false -> => use the trx with the the given trxName; create a savepoint and to roll back to in case of problems. don't commit in case of success.
	 * </ul>
	 */
	// metas: added manageTrx parameter
	@Override
	public void run(final String trxName, final boolean manageTrx, final TrxRunnable runnable)
	{
		final TrxPropagation trxMode;
		final OnRunnableSuccess onRunnableSuccess;
		final OnRunnableFail onRunnableFail = getThreadInheritedOnRunnableFail(OnRunnableFail.ASK_RUNNABLE);
		String trxNameToUse = trxName;
		if (isNull(trxName) || manageTrx)
		{
			// localTrx = true;
			trxMode = TrxPropagation.REQUIRES_NEW;
			onRunnableSuccess = OnRunnableSuccess.COMMIT;
		}
		else if (ITrx.TRXNAME_ThreadInherited.equals(trxName))
		{
			// NOTE: we are ignoring the "manageTrx" flag because in most of the cases is "false" because the trxName is not null.
			// On the other hand, if we were asked to run in an inherited transaction is does not exist,
			// in most of the cases it's expected to create one and execute using it.

			// Get the actual inherited transaction.
			// If that one does not exist, we will need to create one.
			// If exists, we will need to use it.
			final ITrx inheritedTrx = getThreadInheritedTrx(OnTrxMissingPolicy.ReturnTrxNone);
			if (isNull(inheritedTrx))
			{
				trxMode = TrxPropagation.REQUIRES_NEW;
				onRunnableSuccess = OnRunnableSuccess.COMMIT;
				trxNameToUse = ITrx.TRXNAME_None;
			}
			else
			{
				trxMode = TrxPropagation.NESTED;
				onRunnableSuccess = OnRunnableSuccess.DONT_COMMIT;
				trxNameToUse = inheritedTrx.getTrxName();
			}
		}
		else
		{
			// localTrx = false;
			trxMode = TrxPropagation.NESTED;
			onRunnableSuccess = OnRunnableSuccess.DONT_COMMIT;
		}

		final ITrxRunConfig trxRunConfig = new TrxRunConfig(trxMode, onRunnableSuccess, onRunnableFail);
		run(trxNameToUse, trxRunConfig, runnable);
	}

	@Override
	public void run(final String trxName, final ITrxRunConfig cfg, final TrxRunnable runnable)
	{
		//
		// Determine which trxName we shall use based on trx run configuration
		final String trxNameToUse;
		final boolean addTrxToAllowedTrxConstraints;
		final TrxPropagation trxPropagation = cfg.getTrxMode();
		if (trxPropagation == TrxPropagation.REQUIRES_NEW)
		{
			Check.assume(ITrx.TRXNAME_ThreadInherited != trxName, IllegalTrxRunStateException.class, "Inherited transaction not allowed when propagation is REQUIRES_NEW");
			final String trxPrefix = trxName == null ? "TrxRun" : trxName;

			// add transaction to allowed transactions only if is not a local transaction.
			// Local transactions needs to be added explicitly.
			addTrxToAllowedTrxConstraints = !ITrx.TRXNAME_PREFIX_LOCAL.equals(trxPrefix);

			trxNameToUse = createTrxName(trxPrefix, false); // 03824: only create the name, don't open the trx yet
		}
		else if (trxPropagation == TrxPropagation.NESTED)
		{
			addTrxToAllowedTrxConstraints = true;

			if (ITrx.TRXNAME_ThreadInherited == trxName)
			{
				trxNameToUse = threadLocalTrx.get();
				if (trxNameToUse == null)
				{
					throw new IllegalTrxRunStateException("Inherited transaction name was not found (Thread: " + Thread.currentThread().getName() + ")")
							.setTrxRunConfig(cfg)
							.setTrxName(trxName);
				}
			}
			else
			{
				trxNameToUse = trxName;
			}

			//
			// WARN about following possible illegal case: propagation=NESTED, onSuccess=COMMIT.
			// Because in most of the cases this is not a valid case which could lead to weird errors,
			// i.e. the caller thread would have the transaction commited and closed after this call which I am not sure it is desired or expected.
			if (cfg.getOnRunnableSuccess() == OnRunnableSuccess.COMMIT)
			{
				new IllegalTrxRunStateException("Possible wrong trx run configuration: Propagation=NESTED, OnSuccess=COMMIT. Ignored.")
						.setTrxRunConfig(cfg)
						.setTrxName(trxNameToUse)
						.throwOrLogWarning(Services.get(IDeveloperModeBL.class).isEnabled(), logger);
			}
		}
		else
		{
			throw new IllegalTrxRunStateException("TrxPropagation not supported: " + trxPropagation)
					.setTrxRunConfig(cfg)
					.setTrxName(trxName);
		}

		//
		// Execute the runnable
		final ITrxConstraintsBL trxConstraintsBL = Services.get(ITrxConstraintsBL.class);
		trxConstraintsBL.saveConstraints();
		final String threadLocalTrxNameOld = threadLocalTrx.get();
		boolean restoreThreadLocalTrxName = false;
		try
		{
			if (trxPropagation == TrxPropagation.REQUIRES_NEW)
			{
				final ITrxConstraints constraints = trxConstraintsBL.getConstraints();

				//
				// Increase maximum allowed transactions count
				// Reason: if we are here, we were asked programatically to execute a code chunk in a separate transaction.
				constraints.incMaxTrx(1);

				// 03824: now allow 'trxName' to be opened and create the trx
				// we need to allow the transaction name only if we created it here
				if (addTrxToAllowedTrxConstraints)
				{
					constraints.addAllowedTrxNamePrefix(trxNameToUse);
				}

				// Create and start the new transaction
				get(trxNameToUse, OnTrxMissingPolicy.CreateNew);
			}

			// Set our transaction as currently active thread local transaction
			restoreThreadLocalTrxName = true;
			threadLocalTrx.set(trxNameToUse);

			final TrxRunnable2 runnable2 = runnable instanceof TrxRunnable2 ? (TrxRunnable2)runnable : new TrxRunnable2Wrapper(runnable);
			run0(runnable2, cfg, trxNameToUse);

		}
		finally
		{
			// Restore the thread local transaction, in case we changed it.
			if (restoreThreadLocalTrxName)
			{
				threadLocalTrx.set(threadLocalTrxNameOld);
				restoreThreadLocalTrxName = false;
			}

			// Restore trx constraints
			trxConstraintsBL.restoreConstraints();
		}
	}

	private final void run0(final TrxRunnable2 runnable, final ITrxRunConfig cfg, final String trxName)
	{
		Check.assumeNotNull(runnable, IllegalTrxRunStateException.class, "Param 'runnable' is not null");
		Check.assumeNotNull(cfg, IllegalTrxRunStateException.class, "Param 'cfg' is not null");

		// Validate trxName
		if (isNull(trxName))
		{
			throw new IllegalTrxRunStateException("A concrete transaction name is expected")
					.setTrxRunConfig(cfg)
					.setTrxName(trxName);
		}
		else if (trxName == ITrx.TRXNAME_ThreadInherited)
		{
			throw new IllegalTrxRunStateException("No thread inherited transaction placeholder is allowed at this point")
					.setTrxRunConfig(cfg)
					.setTrxName(trxName);
		}

		//
		// Get/create the actual transaction to use.
		ITrx trx;
		final TrxPropagation trxPropagation = cfg.getTrxMode();
		if (TrxPropagation.REQUIRES_NEW == trxPropagation)
		{
			trx = get(trxName, OnTrxMissingPolicy.CreateNew);
		}
		else if (TrxPropagation.NESTED == trxPropagation)
		{
			trx = get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
			if (isNull(trx))
			{
				trx = get(trxName, OnTrxMissingPolicy.CreateNew);
				new IllegalTrxRunStateException("New transaction was created even it was expected to already exist")
						.setTrxRunConfig(cfg)
						.setTrxName(trxName)
						.throwOrLogWarning(Services.get(IDeveloperModeBL.class).isEnabled(), logger);
			}
		}
		else
		{
			throw new IllegalTrxRunStateException("Invalid TrxPropagation: " + trxPropagation)
					.setTrxRunConfig(cfg)
					.setTrxName(trxName);
		}
		// Start the transaction if not already started.
		if (!trx.isActive())
		{
			trx.start();
		}

		//
		// Actually run the runnable, i.e.
		// * create the savepoint if needed
		// * run the runnable and handle the runnable's exceptions
		// * commit/rollback the transaction if needed
		final OnRunnableFail onRunnableFail = cfg.getOnRunnableFail();
		ITrxSavepoint savepoint = null;
		Throwable exceptionToThrow = null; // set in "catch" block; used in finally block to add more suppressed exceptions if needed.
		try
		{
			// Create the transaction savepoint if needed
			if (trxPropagation == TrxPropagation.NESTED
					&& (onRunnableFail == OnRunnableFail.ROLLBACK || onRunnableFail == OnRunnableFail.ASK_RUNNABLE))
			{
				// creating a savepoint so that we'll be able to rollback in case of a failure
				savepoint = trx.createTrxSavepoint(null);
			}

			// Actually execute the runnable
			runnable.run(trxName);

			// Commit the transaction if we were asked to do it
			OnRunnableSuccess onRunnableSuccess = cfg.getOnRunnableSuccess();
			if (onRunnableSuccess == OnRunnableSuccess.COMMIT)
			{
				trx.commit(true); // throwException=true

				// unsetting the savepoint because after the commit an attempt to release if would cause
				// "org.postgresql.util.PSQLException: ERROR: RELEASE SAVEPOINT can only be used in transaction blocks; State=25P01; ErrorCode=0"
				savepoint = null;
			}
		}
		// we catch Throwable and not only Exceptions because java.lang.AssertionError is not an Exception
		catch (final Throwable runException)
		{
			//
			// Call custom exception handler to advice us what to do
			exceptionToThrow = runException;
			boolean rollback = true;
			try
			{
				rollback = runnable.doCatch(runException);
				exceptionToThrow = null;
			}
			catch (final Throwable doCatchException)
			{
				exceptionToThrow = doCatchException;
				rollback = true;
			}

			//
			// Rollback transaction if needed
			if (onRunnableFail == OnRunnableFail.ROLLBACK
					|| (onRunnableFail == OnRunnableFail.ASK_RUNNABLE && rollback))
			{
				if (trxPropagation == TrxPropagation.REQUIRES_NEW)
				{
					// rolling back to the start of this little internal trx
					// NOTE: we assume this method NEVER EVER throws exception
					trx.rollback();
				}
				else if (trxPropagation == TrxPropagation.NESTED)
				{
					Check.assume(savepoint != null, IllegalTrxRunStateException.class, "A savepoint was created ({0})", cfg); // shall not happen

					try
					{
						trx.rollback(savepoint);
						savepoint = null;
					}
					catch (final Exception e2)
					{
						final TrxException rollbackEx = new TrxException("Failed to rollback to savepoint"
								+ "\nSavepoint: " + savepoint
								+ "\nTrx: " + trx);
						logger.log(Level.WARNING, "Failed to rollback to savepoint. Going forward...", rollbackEx);
					}

					// metas-ts: setting 'trx' to null only if we have a non-local trx.
					// Otherwise we could not close the trx and it would be left dangling
					trx = null;
					savepoint = null; // get rid of savepoint, because even if it failed there is not match we can do
				}
				else
				{
					throw new IllegalTrxRunStateException("Not supported TrxPropagation: " + trxPropagation)
							.setTrxRunConfig(cfg)
							.setTrxName(trxName);
				}
			} // end rollback

			//
			// Propagate the catched exception, no matter what, even if we were called with OnRunnableFail.DONT_ROLLBACK
			if (exceptionToThrow != null)
			{
				throw AdempiereException.wrapIfNeeded(exceptionToThrow);
			}

		}
		finally
		{
			//
			// Close the transaction if needed
			if (trx == null)
			{
				// nothing to close because transaction was already closed (i.e. when it was commited)
			}
			else if (trxPropagation == TrxPropagation.REQUIRES_NEW)
			{
				// we created this trx, so we make sure it is also closed at the end
				trx.close();
				trx = null;
				savepoint = null; // get rid of savepoint because it was released
			}

			//
			// Call custom finally handler (if any):
			try
			{
				runnable.doFinally();
			}
			catch (final Throwable doFinallyException)
			{
				// Propagate the doFinallyException only if we are not currently throwing another exception.
				// If we are currently throwing another exception, we suppress this one.
				AdempiereException.suppressOrThrow(doFinallyException, exceptionToThrow);
			}
			finally
			{
				// make sure that our savepoint is released if there was no commit or rollback
				if (savepoint != null && trx != null)
				{
					try
					{
						trx.releaseSavepoint(savepoint);
					}
					catch (Exception e)
					{
						final String errmsg = "There was an exception while rolling back to savepoint. Going forward."
								+ "\n Trx: " + trx
								+ "\n Savepoint: " + savepoint;
						logger.log(Level.WARNING, errmsg, e);
					}
				}
				savepoint = null;
			}
		}
	}

	@Override
	public void commit(final String trxName)
	{
		final ITrx trx = get(trxName, OnTrxMissingPolicy.Fail);
		try
		{
			trx.commit(true);
		}
		catch (final Exception e)
		{
			throw new DBException(e);
		}
	}

	@Override
	public ITrxListenerManager getTrxListenerManager(final String trxName)
	{
		final ITrx trx = get(trxName, OnTrxMissingPolicy.Fail);
		return trx.getTrxListenerManager();
	}

	@Override
	public ITrxListenerManager getTrxListenerManagerOrAutoCommit(final String trxName)
	{
		final ITrx trx = get(trxName, OnTrxMissingPolicy.ReturnTrxNone);
		if (isNull(trx) || !trx.isActive())
		{
			return AutoCommitTrxListenerManager.instance;
		}
		return trx.getTrxListenerManager();

	}

	@Override
	public final boolean isNull(final ITrx trx)
	{
		return trx == null;
	}

	@Override
	public final boolean isNull(final String trxName)
	{
		return trxName == null
				|| trxName == ITrx.TRXNAME_None
				|| trxName == ITrx.TRXNAME_NoneNotNull
				|| trxName.length() == 0;
	}

	@Override
	public void setTrxNameGenerator(final ITrxNameGenerator trxNameGenerator)
	{
		Check.assumeNotNull(trxNameGenerator, "trxNameGenerator not null");
		this.trxNameGenerator = trxNameGenerator;
	}

	@Override
	public boolean isValidTrxName(final String trxName)
	{
		if (isNull(trxName))
		{
			return false;
		}
		if (ITrx.TRXNAME_ThreadInherited == trxName)
		{
			return false;
		}
		return true;
	}

	@Override
	public boolean isSameTrxName(final String trxName1, final String trxName2)
	{
		if (trxName1 == trxName2)
		{
			return true;
		}

		//
		// Get actual trxName1 (handle the case when we are dealing with thread inherited transactions)
		String actualTrxName1 = trxName1;
		if (Util.same(trxName1, ITrx.TRXNAME_ThreadInherited))
		{
			actualTrxName1 = threadLocalTrx.get();
		}

		//
		// Get actual trxName2 (handle the case when we are dealing with thread inherited transactions)
		String actualTrxName2 = trxName2;
		if (Util.same(trxName2, ITrx.TRXNAME_ThreadInherited))
		{
			actualTrxName2 = threadLocalTrx.get();
		}

		// Case: actual trxName1 is null
		if (isNull(actualTrxName1))
		{
			return isNull(actualTrxName2);
		}
		// Case: actual trxName1 is NOT null
		else
		{
			// trxName1 is null, but trxName2 is not null
			if (isNull(actualTrxName2))
			{
				return false;
			}

			//
			// Compare actual transactions
			return actualTrxName1.equals(actualTrxName2);
		}
	}

	@Override
	public boolean isSameTrxName(final ITrx trx1, final String trxName2)
	{
		final String trxName1 = trx1 == null ? ITrx.TRXNAME_None : trx1.getTrxName();
		return isSameTrxName(trxName1, trxName2);
	}

	@Override
	public void assertTrxNameNotNull(final String trxName)
	{
		Check.assume(!isNull(trxName), "trxName shall not be null");
	}

	@Override
	public void assertTrxNameNull(final String trxName)
	{
		Check.assume(isNull(trxName), "trxName shall be null but it was {0}", trxName);
	}

	@Override
	public void assertTrxNotNull(final IContextAware contextProvider)
	{
		Check.assumeNotNull(contextProvider, "contextProvider not null");

		final String trxName = contextProvider.getTrxName();
		Check.assume(!isNull(trxName), TrxException.class, "trxName shall not be null in {0}", contextProvider);
	}

	@Override
	public void assertTrxNull(final IContextAware contextProvider)
	{
		Check.assumeNotNull(contextProvider, "contextProvider not null");

		final String trxName = contextProvider.getTrxName();
		Check.assume(isNull(trxName), "trxName shall be null in {0}", contextProvider);
	}

	@Override
	public <T> void assertModelsTrxName(final String trxNameExpected, final Collection<T> models)
	{
		if (models == null || models.isEmpty())
		{
			return;
		}

		for (final T model : models)
		{
			assertModelTrxName(trxNameExpected, model);
		}
	}

	@Override
	public <T> void assertModelTrxName(final String trxNameExpected, final T model)
	{
		if (model == null)
		{
			return;
		}

		final String trxNameActual = InterfaceWrapperHelper.getTrxName(model);
		if (!isSameTrxName(trxNameExpected, trxNameActual))
		{
			throw new TrxException("Model does not have expected transaction"
					+ "\n Expected trxName: " + trxNameExpected
					+ "\n Model trxName: " + trxNameActual
					+ "\n Model: " + model);
		}
	}

	@Override
	public <T> void assertModelTrxNameNotNull(final T model)
	{
		Check.assumeNotNull(model, "model not null");
		final String trxName = InterfaceWrapperHelper.getTrxName(model);
		Check.assume(!isNull(trxName), "trxName shall not be null for {0}", model);
	}

	@Override
	public void assertThreadInheritedTrxExists()
	{
		final String trxName = getThreadInheritedTrxName();
		Check.assume(!isNull(trxName), "ThreadInherited transaction shall be set at this point");
	}
	
	@Override
	public void assertThreadInheritedTrxNotExists()
	{
		final String trxName = getThreadInheritedTrxName();
		Check.assume(isNull(trxName), "ThreadInherited transaction shall NOT be set at this point");
	}


	@Override
	public String setThreadInheritedTrxName(final String trxName)
	{
		final String trxNameOld = threadLocalTrx.get();
		threadLocalTrx.set(trxName);

		return trxNameOld;
	}

	@Override
	public String getThreadInheritedTrxName()
	{
		return threadLocalTrx.get();
	}

	@Override
	public String getThreadInheritedTrxName(final OnTrxMissingPolicy onTrxMissingPolicy)
	{
		final ITrx trx = getThreadInheritedTrx(onTrxMissingPolicy);
		if (isNull(trx))
		{
			return ITrx.TRXNAME_None;
		}
		return trx.getTrxName();
	}

	@Override
	public ITrx getThreadInheritedTrx(final OnTrxMissingPolicy onTrxMissingPolicy)
	{
		Check.assumeNotNull(onTrxMissingPolicy, "onTrxMissingPolicy not null");

		final String trxName = threadLocalTrx.get();

		//
		// Case: thread inerited trxName was NOT set
		// Case: thread inerited trxName was set but it's name is considered null/none.
		// => throw exception
		if (isNull(trxName))
		{
			if (onTrxMissingPolicy == OnTrxMissingPolicy.Fail)
			{
				throw new TrxNotFoundException("No thread inherited transaction exist or it's considered null (trxName=" + trxName + ")");
			}
			else if (onTrxMissingPolicy == OnTrxMissingPolicy.ReturnTrxNone)
			{
				return ITrx.TRX_None;
			}
			else
			{
				throw new OnTrxMissingPolicyNotSupportedException(onTrxMissingPolicy);
			}
		}

		// NOTE: at this point trxName is not null and we relly on "getTrx" method to make sure the transaction really exist
		OnTrxMissingPolicyNotSupportedException.throwIf(onTrxMissingPolicy, OnTrxMissingPolicy.CreateNew); // createNew is not supported
		final ITrx trx = get(trxName, onTrxMissingPolicy);
		return trx;
	}

	@Override
	public IContextAware createThreadContextAware(final Object contextProvider)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(contextProvider);
		return createThreadContextAware(ctx);
	}

	@Override
	public IContextAware createThreadContextAware(final Properties ctx)
	{
		Check.assumeNotNull(ctx, "ctx not null");

		return new IContextAware()
		{
			@Override
			public Properties getCtx()
			{
				return ctx;
			}

			@Override
			public String getTrxName()
			{
				final String trxName = getThreadInheritedTrxName();
				if (isNull(trxName))
				{
					// task 07752: if there is no inherited trx (yet!), we fall back to <<ThreadInherited>>
					return ITrx.TRXNAME_ThreadInherited;
				}
				return trxName;
			}
		};
	}
	
	@Override
	public void setThreadInheritedOnRunnableFail(final OnRunnableFail onRunnableFail)
	{
		threadLocalOnRunnableFail.set(onRunnableFail);
	}
	
	private final OnRunnableFail getThreadInheritedOnRunnableFail(final OnRunnableFail onRunnableFailDefault)
	{
		final OnRunnableFail onRunnableFail = threadLocalOnRunnableFail.get();
		return onRunnableFail == null ? onRunnableFailDefault : onRunnableFail;
	}

	@Override
	public final boolean isDebugTrxCreateStacktrace()
	{
		return debugTrxCreateStacktrace;
	}

	@Override
	public final void setDebugTrxCreateStacktrace(final boolean debugTrxCreateStacktrace)
	{
		this.debugTrxCreateStacktrace = debugTrxCreateStacktrace;
	}

	@Override
	public final boolean isDebugTrxCloseStacktrace()
	{
		return debugTrxCloseStacktrace;
	}

	@Override
	public final void setDebugTrxCloseStacktrace(final boolean debugTrxCloseStacktrace)
	{
		this.debugTrxCloseStacktrace = debugTrxCloseStacktrace;
	}

	@Override
	public final void setDebugClosedTransactions(final boolean enabled)
	{
		trxName2trxLock.lock();
		try
		{
			if (enabled)
			{
				if (debugClosedTransactionsList == null)
				{
					debugClosedTransactionsList = new ArrayList<ITrx>();
				}
			}
			else
			{
				debugClosedTransactionsList = null;
			}
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}

	@Override
	public final boolean isDebugClosedTransactions()
	{
		return debugClosedTransactionsList != null;
	}

	@Override
	public final List<ITrx> getDebugClosedTransactions()
	{
		trxName2trxLock.lock();
		try
		{
			if (debugClosedTransactionsList == null)
			{
				return Collections.emptyList();
			}
			return new ArrayList<ITrx>(debugClosedTransactionsList);
		}
		finally
		{
			trxName2trxLock.unlock();
		}
	}

	@Override
	public void setDebugConnectionBackendId(boolean debugConnectionBackendId)
	{
		this.debugConnectionBackendId = debugConnectionBackendId;
	}

	@Override
	public boolean isDebugConnectionBackendId()
	{
		return debugConnectionBackendId;
	}

}