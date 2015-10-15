package org.adempiere.impexp.spi.impl;

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


import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.event.Event;
import org.adempiere.event.IEventBusFactory;
import org.adempiere.event.Topic;
import org.adempiere.event.Type;
import org.adempiere.impexp.IImportProcess;
import org.adempiere.impexp.IImportProcessFactory;
import org.adempiere.impexp.ImportProcessResult;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import de.metas.async.model.I_C_Queue_WorkPackage;
import de.metas.async.spi.WorkpackageProcessorAdapter;

/**
 * Workpackage processor used to import records enqueued by {@link AsyncImportProcessBuilder}.
 * 
 * @author tsa
 *
 */
public class AsyncImportWorkpackageProcessor extends WorkpackageProcessorAdapter
{
	// services
	private static final transient CLogger logger = CLogger.getCLogger(AsyncImportWorkpackageProcessor.class);

	public static final String PARAM_ImportTableName = "ImportTableName";
	public static final String PARAM_Selection_ID = IImportProcess.PARAM_Selection_ID;

	private static final String MSG_Event_RecordsImported = "org.adempiere.impexp.async.Event_RecordsImported";
	public static final Topic TOPIC_RecordsImported = Topic.of("org.adempiere.impexp.async.RecordsImported", Type.REMOTE);

	/** @return false. IMPORTANT: let the {@link IImportProcess} manage the transactions */
	@Override
	public boolean isRunInTransaction()
	{
		return false;
	}

	private String getImportTableName()
	{
		return getParameters().getParameterAsString(PARAM_ImportTableName);
	}

	@Override
	public Result processWorkPackage(final I_C_Queue_WorkPackage workpackage, final String localTrxName)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(workpackage);

		final String importTableName = getImportTableName();
		Check.assumeNotNull(importTableName, "importTableName not null");

		// Make sure we have a selection defined. It will be used by the import processor.
		final int selectionId = getParameters().getParameterAsInt(PARAM_Selection_ID);
		Check.assume(selectionId > 0, "selectionId > 0");

		final IImportProcess<Object> importProcessor = Services.get(IImportProcessFactory.class).newImportProcessForTableName(importTableName);
		importProcessor.setCtx(ctx);
		importProcessor.setLoggable(getLoggable());
		importProcessor.setParameters(getParameters());
		final ImportProcessResult result = importProcessor.run();

		notifyImportDone(result, workpackage.getCreatedBy());

		return Result.SUCCESS;
	}

	private void notifyImportDone(final ImportProcessResult result, final int recipientUserId)
	{
		try
		{
			final String targetTableName = result.getTargetTableName();
			final String windowName = Services.get(IADTableDAO.class).retrieveWindowName(Env.getCtx(), targetTableName);

			Services.get(IEventBusFactory.class)
					.getEventBus(TOPIC_RecordsImported)
					.postEvent(Event.builder()
							.setDetailADMessage(MSG_Event_RecordsImported, result.getInsertCount(), result.getUpdateCount(), windowName)
							.addRecipient_User_ID(recipientUserId)
							.build());
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, "Failed creating and posting event. Ignored.", e);
		}
	}

}