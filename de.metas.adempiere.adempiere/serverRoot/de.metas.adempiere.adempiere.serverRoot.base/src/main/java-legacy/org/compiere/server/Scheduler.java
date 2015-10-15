/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                        *
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
package org.compiere.server;

import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.SchedulingPattern;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.ad.security.IUserRolePermissions;
import org.adempiere.ad.security.IUserRolePermissionsDAO;
import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.IClientDAO;
import org.adempiere.service.IOrgDAO;
import org.adempiere.user.api.IUserDAO;
import org.adempiere.util.Services;
import org.adempiere.util.lang.IAutoCloseable;
import org.adempiere.util.time.SystemTime;
import org.compiere.model.I_AD_Client;
import org.compiere.model.I_AD_Note;
import org.compiere.model.I_AD_OrgInfo;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.I_AD_PInstance_Para;
import org.compiere.model.I_AD_Scheduler;
import org.compiere.model.I_AD_SchedulerLog;
import org.compiere.model.I_AD_Task;
import org.compiere.model.MAttachment;
import org.compiere.model.MClient;
import org.compiere.model.MNote;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MScheduler;
import org.compiere.model.MSchedulerPara;
import org.compiere.model.MTask;
import org.compiere.model.MUser;
import org.compiere.model.X_AD_Scheduler;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnableAdapter;

/**
 * Scheduler
 *
 * @author Jorg Janke
 * @version $Id: Scheduler.java,v 1.5 2006/07/30 00:53:33 jjanke Exp $
 */
public class Scheduler extends AdempiereServer
{
	/**
	 * Scheduler
	 *
	 * @param model model
	 */
	public Scheduler(final MScheduler model)
	{
		super(model, 240);		// nap
		m_model = model;

		// metas us1030 updating status
		setSchedulerStatus(X_AD_Scheduler.STATUS_Started, false); // saveLogs=false
	}	// Scheduler

	/** The Concrete Model */
	private final MScheduler m_model;
	/** Last Summary; stored in the scheduler log. */
	private StringBuffer m_summary = new StringBuffer();

	/** last outcome; stored in the scheduler log. */
	private boolean m_success = true;

	/** Transaction */
	private Trx m_trx = null;

	//
	// Cron4J scheduling
	private it.sauronsoftware.cron4j.Scheduler cronScheduler;
	private Predictor predictor;

	/**
	 * Sets AD_Scheduler.Status and save the record
	 *
	 * @param status
	 */
	private void setSchedulerStatus(final String status, final boolean saveLogs)
	{
		Services.get(ITrxManager.class).run(new TrxRunnableAdapter()
		{
			@Override
			public void run(final String localTrxName) throws Exception
			{
				if (saveLogs)
				{
					saveLogs(localTrxName);
				}

				m_model.setStatus(status);
				InterfaceWrapperHelper.save(m_model, localTrxName);
			}
		});
	}

	/**
	 * <ul>
	 * <li>Delete old logs
	 * <li>create a new AD_SchedulerLog record based on {@link #m_summary}.
	 * </ul>
	 *
	 * @param trxName
	 */
	private void saveLogs(final String trxName)
	{
		final int no = m_model.deleteLog(trxName);
		m_summary.append("Logs deleted=").append(no);
		//
		final Properties ctx = InterfaceWrapperHelper.getCtx(m_model);
		final I_AD_SchedulerLog pLog = InterfaceWrapperHelper.create(ctx, I_AD_SchedulerLog.class, trxName);

		pLog.setAD_Org_ID(m_model.getAD_Org_ID());
		pLog.setAD_Scheduler_ID(m_model.getAD_Scheduler_ID());
		pLog.setSummary(m_summary.toString());
		pLog.setIsError(!m_success);
		pLog.setReference("#" + String.valueOf(p_runCount) + " - " + TimeUtil.formatElapsed(new Timestamp(p_startWork)));
		InterfaceWrapperHelper.save(pLog);

		m_summary = new StringBuffer();
	}

	/**
	 * Work
	 */
	@Override
	protected void doWork()
	{

		// metas us1030 updating staus
		setSchedulerStatus(X_AD_Scheduler.STATUS_Running, false); // saveLogs=false

		m_summary = new StringBuffer(m_model.toString()).append(" - ");

		// Prepare a ctx for the report/process - BF [1966880]
		final Properties schedulerCtx = createSchedulerCtx();

		try (final IAutoCloseable contextRestorer = Env.switchContext(schedulerCtx))
		{
			final String type = m_model.getSchedulerProcessType();
			if (X_AD_Scheduler.SCHEDULERPROCESSTYPE_Task.equals(type))
			{
				if (m_model.getAD_Task_ID() <= 0)
				{
					throw new AdempiereException("@NotFound@ @AD_Task_ID@");
				}
				final MTask task = new MTask(schedulerCtx, m_model.getAD_Task_ID(), ITrx.TRXNAME_None);
				m_summary.append(runTask(task));
			}
			else if (X_AD_Scheduler.SCHEDULERPROCESSTYPE_Process.equals(type)
					|| X_AD_Scheduler.SCHEDULERPROCESSTYPE_Report.equals(type))
			{
				if (m_model.getAD_Process_ID() <= 0)
				{
					throw new AdempiereException("@NotFound@ @AD_Process_ID@");
				}

				// metas-ts using process with scheduler-ctx
				final MProcess process = new MProcess(schedulerCtx, m_model.getAD_Process_ID(), ITrx.TRXNAME_None);

				DB.saveConstraints();
				try
				{
					// HOTFIX for 06520: we are actually disabling OpenTrx timeout. That's the main cause of the issue described in this task
					DB.getConstraints().setTrxTimeoutSecs(0, true); // secs=NoTimeout, logOnly=true

					String trxNamePrefix = "Scheduler_" + process.getValue();
					m_trx = Trx.get(Trx.createTrxName(trxNamePrefix), true);
					if (process.isReport())
					{
						m_summary.append(runReport(process));
					}
					else
					{
						m_summary.append(runProcess(process));
					}
					m_trx.commit(true);
				}
				catch (final Exception e)
				{
					if (m_trx != null)
					{
						m_trx.rollback();
					}
					log.log(Level.WARNING, "Failed processing: " + process.toString(), e);
					m_summary.append(e.toString());
				}
				finally
				{
					if (m_trx != null)
					{
						m_trx.close();
					}
					m_trx = null;

					DB.restoreConstraints();
				}
			}
			else
			{
				throw new AdempiereException("@NotSupported@ @" + I_AD_Scheduler.COLUMNNAME_SchedulerProcessType + "@ - " + type);
			}
		}

		// metas us1030 updating status: Running->Started
		setSchedulerStatus(X_AD_Scheduler.STATUS_Started, true); // saveLogs=true
		// metas end

	}	// doWork

	private final Properties createSchedulerCtx()
	{
		final Properties schedulerCtx = Env.newTemporaryCtx();

		final Properties ctx = getCtx(); // server context

		//
		// AD_Client, AD_Language
		final IClientDAO clientDAO = Services.get(IClientDAO.class);
		final int adClientId = m_model.getAD_Client_ID();
		final I_AD_Client schedClient = clientDAO.retriveClient(ctx, adClientId);
		Env.setContext(schedulerCtx, Env.CTXNAME_AD_Client_ID, schedClient.getAD_Client_ID());
		Env.setContext(schedulerCtx, Env.CTXNAME_AD_Language, schedClient.getAD_Language());

		//
		// AD_Org, M_Warehouse
		final int adOrgId = m_model.getAD_Org_ID();
		Env.setContext(schedulerCtx, Env.CTXNAME_AD_Org_ID, adOrgId);
		if (adOrgId > 0)
		{
			final I_AD_OrgInfo schedOrg = Services.get(IOrgDAO.class).retrieveOrgInfo(schedulerCtx, adOrgId, ITrx.TRXNAME_None);
			if (schedOrg.getM_Warehouse_ID() > 0)
			{
				Env.setContext(schedulerCtx, Env.CTXNAME_M_Warehouse_ID, schedOrg.getM_Warehouse_ID());
			}
		}

		//
		// AD_User_ID, SalesRep_ID
		final int adUserId = getAD_User_ID();
		Env.setContext(schedulerCtx, Env.CTXNAME_AD_User_ID, adUserId);
		Env.setContext(schedulerCtx, Env.CTXNAME_SalesRep_ID, adUserId);

		//
		// AD_Role
		final int adRoleId;
		if (!InterfaceWrapperHelper.isNull(m_model, I_AD_Scheduler.COLUMNNAME_AD_Role_ID))
		{
			adRoleId = m_model.getAD_Role_ID();
		}
		else
		{
			// Use the first user role, which has access to our organization.
			final IUserRolePermissions role = Services.get(IUserRolePermissionsDAO.class)
					.retrieveFirstUserRolesPermissionsForUserWithOrgAccess(schedulerCtx, adUserId, adOrgId)
					.orNull();
			adRoleId = role == null ? Env.CTXVALUE_AD_Role_ID_NONE : role.getAD_Role_ID();
		}
		Env.setContext(schedulerCtx, Env.CTXNAME_AD_Role_ID, adRoleId);

		//
		// Date
		final Timestamp date = SystemTime.asDayTimestamp();
		Env.setContext(schedulerCtx, Env.CTXNAME_Date, date);

		return schedulerCtx;
	}

	/**
	 * Run Report
	 *
	 * @param process
	 * @return summary
	 * @throws Exception
	 */
	private String runReport(final MProcess process) throws Exception
	{
		log.info(process.toString());
		if (!process.isReport() || process.getAD_ReportView_ID() == 0)
		{
			return "Not a Report AD_Process_ID=" + process.getAD_Process_ID() + " - " + process.getName();
		}

		// Process
		final ProcessInfo pi = createProcessInfo(process);
		if (!process.processIt(pi, m_trx) && pi.getClassName() != null)
		{
			return "Process failed: (" + pi.getClassName() + ") " + pi.getSummary();
		}

		// Report
		final Properties ctx = pi.getCtx();
		final ReportEngine re = ReportEngine.get(ctx, pi);
		if (re == null)
		{
			return "Cannot create Report AD_Process_ID=" + process.getAD_Process_ID() + " - " + process.getName();
		}
		final File report = re.getPDF();
		// Notice
		final int AD_Message_ID = 884;		// HARDCODED SchedulerResult
		final Integer[] userIDs = m_model.getRecipientAD_User_IDs();
		for (int i = 0; i < userIDs.length; i++)
		{
			final MNote note = new MNote(ctx, AD_Message_ID, userIDs[i].intValue(), m_trx.getTrxName());
			note.setClientOrg(pi.getAD_Client_ID(), pi.getAD_Org_ID());
			note.setTextMsg(m_model.getName());
			note.setDescription(m_model.getDescription());
			note.setRecord(pi.getTable_ID(), pi.getRecord_ID());
			note.save();
			// Attachment
			final MAttachment attachment = new MAttachment(ctx, I_AD_Note.Table_ID, note.getAD_Note_ID(), m_trx.getTrxName());
			attachment.setClientOrg(pi.getAD_Client_ID(), pi.getAD_Org_ID());
			attachment.addEntry(report);
			attachment.setTextMsg(m_model.getName());
			attachment.save();
		}
		//
		return pi.getSummary();
	}	// runReport

	/**
	 * Run Process
	 *
	 * @param process process
	 * @return summary
	 * @throws Exception
	 */
	private String runProcess(final MProcess process) throws Exception
	{
		log.info(process.toString());

		final ProcessInfo pi = createProcessInfo(process);

		final boolean ok = process.processIt(pi, m_trx);
		ProcessInfoUtil.setLogFromDB(pi);

		// notify supervisor if error
		// metas: c.ghita@metas.ro: start
		final MUser from = new MUser(pi.getCtx(), pi.getAD_User_ID(), ITrx.TRXNAME_None);
		notify(ok, from, process.getName(), pi.getSummary(), pi.getLogInfo(),
				Services.get(IADTableDAO.class).retrieveTableId(I_AD_PInstance.Table_Name),
				pi.getAD_PInstance_ID());
		// metas: c.ghita@metas.ro: end

		m_success = ok; // stored it, so we can persist it in the scheduler log

		return pi.getSummary();
	}	// runProcess

	/**
	 * Creates and setup the {@link ProcessInfo}.
	 * 
	 * @param process
	 * @see org.compiere.wf.MWFActivity.performWork(Trx)
	 */
	private final ProcessInfo createProcessInfo(final MProcess process)
	{
		final Properties ctx = process.getCtx(); // We assume the right context was already used when the process was loaded
		final int AD_Table_ID = 0;
		final int Record_ID = 0;

		final MPInstance pInstance = new MPInstance(ctx, process, AD_Table_ID, Record_ID);
		fillParameter(pInstance);

		final ProcessInfo pi = new ProcessInfo(process.getName(), process.getAD_Process_ID(), AD_Table_ID, Record_ID);
		pi.setCtx(ctx);
		pi.setAD_User_ID(Env.getAD_User_ID(ctx));
		pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
		pi.setAD_Org_ID(Env.getAD_Org_ID(ctx));
		pi.setAD_PInstance_ID(pInstance.getAD_PInstance_ID());

		return pi;
	}

	/**
	 * metas: c.ghita@metas.ro
	 * method for run a task
	 * 
	 * @param task
	 * @return
	 */
	private String runTask(final MTask task)
	{
		final String summary = task.execute() + task.getTask().getErrorLog();
		final Integer exitValue = task.getTask().getExitValue();
		final boolean ok = exitValue != 0 ? false : true;
		notify(ok, null, task.getName(), summary, "",
				Services.get(IADTableDAO.class).retrieveTableId(I_AD_Task.Table_Name),
				task.get_ID());
		return summary;
	}

	private int getAD_User_ID()
	{
		// FIXME: i think we need to brainstorm and figure out how to get rid of checking UpdatedBy/CreatedBy,
		// because those are totally unpredictable!!!

		int AD_User_ID;
		if (m_model.getSupervisor_ID() > 0)
		{
			AD_User_ID = m_model.getSupervisor_ID();
		}
		// NOTE: for now i am turning off the UpdateBy checking because that is clearly not predictable
		// else if (m_model.getUpdatedBy() > 0)
		// {
		// AD_User_ID = m_model.getUpdatedBy();
		// }
		else if (m_model.getCreatedBy() > 0)
		{
			AD_User_ID = m_model.getCreatedBy();
		}
		else
		{
			AD_User_ID = IUserDAO.SUPERUSER_USER_ID; // fall back to SuperUser
		}
		return AD_User_ID;
	}

	/**
	 * Fill Parameter
	 *
	 * @param pInstance process instance
	 */
	private void fillParameter(final MPInstance pInstance)
	{
		// NOTE: we want to requrey the parameters each time, in case they were changed
		// This will allow the sysadm to do quick tweaks without restarting adempiere server
		final MSchedulerPara[] sParams = m_model.getParameters(true);

		for (final I_AD_PInstance_Para iPara : pInstance.getParameters())
		{
			for (int np = 0; np < sParams.length; np++)
			{
				final MSchedulerPara sPara = sParams[np];
				if (iPara.getParameterName().equals(sPara.getColumnName()))
				{
					final String variable = sPara.getParameterDefault();
					log.fine(sPara.getColumnName() + " = " + variable);
					// Value - Constant/Variable
					Object value = variable;
					if (variable == null
							|| variable != null && variable.length() == 0)
					{
						value = null;
					}
					else if (variable.indexOf('@') != -1)	// we have a variable
					{
						// Strip
						int index = variable.indexOf('@');
						String columnName = variable.substring(index + 1);
						index = columnName.indexOf('@');
						if (index == -1)
						{
							log.warning(sPara.getColumnName() + " - cannot evaluate=" + variable);
							break;
						}
						columnName = columnName.substring(0, index);
						// try Env
						final Properties ctx = getCtx();
						final String env = Env.getContext(ctx, columnName);
						if (env.length() == 0)
						{
							log.warning(sPara.getColumnName() + " - not in environment =" + columnName + "(" + variable + ")");
							break;
						}
						else
						{
							value = env;
						}
					}	// @variable@

					// No Value
					if (value == null)
					{
						log.fine(sPara.getColumnName() + " - empty");
						break;
					}

					// Convert to Type
					try
					{
						if (DisplayType.isNumeric(sPara.getDisplayType())
								|| DisplayType.isID(sPara.getDisplayType()))
						{
							BigDecimal bd = null;
							if (value instanceof BigDecimal)
							{
								bd = (BigDecimal)value;
							}
							else if (value instanceof Integer)
							{
								bd = new BigDecimal(((Integer)value).intValue());
							}
							else
							{
								bd = new BigDecimal(value.toString());
							}
							iPara.setP_Number(bd);
							log.fine(sPara.getColumnName() + " = " + variable + " (=" + bd + "=)");
						}
						else if (DisplayType.isDate(sPara.getDisplayType()))
						{
							Timestamp ts = null;
							if (value instanceof Timestamp)
							{
								ts = (Timestamp)value;
							}
							else
							{
								ts = Timestamp.valueOf(value.toString());
							}
							iPara.setP_Date(ts);
							log.fine(sPara.getColumnName() + " = " + variable + " (=" + ts + "=)");
						}
						else
						{
							iPara.setP_String(value.toString());
							log.fine(sPara.getColumnName() + " = " + variable + " (=" + value + "=) " + value.getClass().getName());
						}
						InterfaceWrapperHelper.save(iPara);
					}
					catch (final Exception e)
					{
						log.warning(sPara.getColumnName() + " = " + variable + " (" + value + ") " + value.getClass().getName() + " - " + e.getLocalizedMessage());
					}
					break;
				}	// parameter match
			}	// scheduler parameter loop
		}	// instance parameter loop
	}	// fillParameter

	/**
	 * Get Server Info
	 *
	 * @return info
	 */
	@Override
	public String getServerInfo()
	{
		return "#" + p_runCount + " - Last=" + m_summary.toString();
	}	// getServerInfo

	/**
	 * metas: c.ghita@metas.ro
	 * notify trough mail in case of abnormal termination
	 * 
	 * @param ok
	 * @param from
	 * @param subject
	 * @param summary
	 * @param logInfo
	 * @param AD_Table_ID
	 * @param Record_ID
	 */

	private void notify(final boolean ok, final MUser from, final String subject, final String summary, final String logInfo,
			final int AD_Table_ID, final int Record_ID)
	{
		final Properties ctx = getCtx();

		// notify supervisor if error
		if (!ok)
		{
			final int supervisorId = m_model.getSupervisor_ID();
			if (supervisorId > 0)
			{
				final MUser user = new MUser(ctx, supervisorId, ITrx.TRXNAME_None);
				final boolean email = user.isNotificationEMail();
				final boolean notice = user.isNotificationNote();

				// if (email || notice)
				// ProcessInfoUtil.setLogFromDB(pi);

				if (email)
				{
					final MClient client = MClient.get(m_model.getCtx(), m_model.getAD_Client_ID());
					final File attachment = null;
					client.sendEMail(from, user, subject, summary + " " + logInfo, attachment);
				}
				if (notice)
				{
					final int AD_Message_ID = 442; // TODO: HARDCODED: ProcessRunError
					final MNote note = new MNote(ctx, AD_Message_ID, supervisorId, ITrx.TRXNAME_None);
					note.setClientOrg(m_model.getAD_Client_ID(), m_model.getAD_Org_ID());
					note.setTextMsg(summary);
					// note.setDescription();
					note.setRecord(AD_Table_ID, Record_ID);
					note.save();
				}
			}
		}
		else
		{
			final Integer[] userIDs = m_model.getRecipientAD_User_IDs();
			if (userIDs.length > 0)
			{
				// ProcessInfoUtil.setLogFromDB(pi);
				for (int i = 0; i < userIDs.length; i++)
				{
					final MUser user = new MUser(ctx, userIDs[i].intValue(), null);
					final boolean email = user.isNotificationEMail();
					final boolean notice = user.isNotificationNote();

					if (email)
					{
						final MClient client = MClient.get(m_model.getCtx(), m_model.getAD_Client_ID());
						client.sendEMail(from, user, subject, summary + " " + logInfo, null);
					}
					if (notice)
					{
						final int AD_Message_ID = 441; // TODO: HARDCODED: ProcessOK
						final MNote note = new MNote(ctx,
								AD_Message_ID, userIDs[i].intValue(), null);
						note.setClientOrg(m_model.getAD_Client_ID(), m_model.getAD_Org_ID());
						note.setTextMsg(summary);
						// note.setDescription();
						note.setRecord(AD_Table_ID, Record_ID);
						note.save();
					}
				}
			}
		}
	}

	/**
	 * This implementation evaluated a cron pattern to do the scheduling. If the model's scheduling type is not "cron",
	 * then the super classe's scheduling is used instead.
	 */
	@Override
	public void run()
	{
		if (!X_AD_Scheduler.SCHEDULETYPE_CronSchedulingPattern.equals(m_model.getScheduleType()))
		{
			super.run();
			return;
		}

		final String cronPattern = m_model.getCronPattern();
		if (cronPattern != null && cronPattern.trim().length() > 0 && SchedulingPattern.validate(cronPattern))
		{
			cronScheduler = new it.sauronsoftware.cron4j.Scheduler();
			cronScheduler.schedule(cronPattern, new Runnable()
			{
				@Override
				public void run()
				{
					runNow();
					final long next = predictor.nextMatchingTime();
					setDateNextRun(new Timestamp(next));
				}
			});
			predictor = new Predictor(cronPattern);
			final long next = predictor.nextMatchingTime();
			setDateNextRun(new Timestamp(next));
			cronScheduler.start();
			while (true)
			{
				if (!sleep())
				{
					cronScheduler.stop();
					break;
				}
				else if (!cronScheduler.isStarted())
				{
					break;
				}
			}
		}
		else
		{
			super.run();
		}
	}
}	// Scheduler