package de.metas.async.api.impl;

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

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.ILoggable;

import de.metas.async.api.IWorkPackageBL;
import de.metas.async.model.I_C_Queue_WorkPackage;
import de.metas.async.model.I_C_Queue_WorkPackage_Log;

public class WorkPackageBL implements IWorkPackageBL
{
	@Override
	public ILoggable createLoggable(final I_C_Queue_WorkPackage workPackage)
	{
		return new ILoggable()
		{
			@Override
			public void addLog(final String msg)
			{
				// NOTE: always create the logs out of transaction because we want them to be persisted even if the workpackage processing fails
				final Properties ctx = InterfaceWrapperHelper.getCtx(workPackage);
				final I_C_Queue_WorkPackage_Log logRecord = InterfaceWrapperHelper.create(ctx, I_C_Queue_WorkPackage_Log.class, ITrx.TRXNAME_None);
				logRecord.setC_Queue_WorkPackage(workPackage);
				logRecord.setMsgText(msg);
				InterfaceWrapperHelper.save(logRecord);
			}
		};
	}
}