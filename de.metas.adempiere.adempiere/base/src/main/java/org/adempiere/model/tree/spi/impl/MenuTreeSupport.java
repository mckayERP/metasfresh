/**
 *
 */
package org.adempiere.model.tree.spi.impl;

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


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.adempiere.ad.security.IRoleDAO;
import org.adempiere.ad.security.IUserRolePermissions;
import org.adempiere.ad.service.IDeveloperModeBL;
import org.adempiere.service.IClientDAO;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_Menu;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.X_AD_Menu;
import org.compiere.util.DB;
import org.compiere.util.Env;

import de.metas.adempiere.model.I_AD_Role;

/**
 * @author tsa
 *
 */
public class MenuTreeSupport extends DefaultPOTreeSupport
{
	@Override
	public String getNodeInfoSelectSQL(final MTree tree)
	{
		final Properties ctx = tree.getCtx();
		final boolean m_clientTree = tree.isClientTree();
		
		final StringBuilder sqlDeveloperMode = new StringBuilder();
		if (Services.get(IDeveloperModeBL.class).isEnabled())
		{
			sqlDeveloperMode.append("\n, (select min(t.TableName) from AD_Tab tt inner join AD_Table t on (t.AD_Table_ID=tt.AD_Table_ID) where tt.AD_Window_ID=AD_Menu.AD_Window_ID and tt.SeqNo=10 and tt.IsActive='Y') as AD_Window_TableName");
			sqlDeveloperMode.append("\n, (select process.Classname from AD_Process process where process.AD_Process_ID=AD_Menu.AD_Process_ID) as AD_Process_ClassName");
			sqlDeveloperMode.append("\n, (select form.Classname from AD_Form form where form.AD_Form_ID=AD_Menu.AD_Form_ID) as AD_Form_ClassName");
			sqlDeveloperMode.append("\n");
		}

		final StringBuffer sql = new StringBuffer();
		final boolean base = Env.isBaseLanguage(ctx, "AD_Menu");
		if (base)
		{
			sql.append("SELECT AD_Menu.AD_Menu_ID AS Node_ID, AD_Menu.Name, AD_Menu.Description, AD_Menu.IsSummary, AD_Menu.Action, "
					+ " NULL AS " + COLUMNNAME_PrintColor + ","
					+ " AD_Menu.AD_Window_ID, AD_Menu.AD_Process_ID, AD_Menu.AD_Form_ID, AD_Menu.AD_Workflow_ID, AD_Menu.AD_Task_ID, AD_Menu.AD_Workbench_ID, "
					+ " AD_Menu.InternalName "
					+ sqlDeveloperMode
					+ " FROM AD_Menu ");
		}
		else
		{
			sql.append("SELECT AD_Menu.AD_Menu_ID AS Node_ID,  t.Name,t.Description,AD_Menu.IsSummary,AD_Menu.Action, "
					+ " NULL AS " + COLUMNNAME_PrintColor + ","
					+ " AD_Menu.AD_Window_ID, AD_Menu.AD_Process_ID, AD_Menu.AD_Form_ID, AD_Menu.AD_Workflow_ID, AD_Menu.AD_Task_ID, AD_Menu.AD_Workbench_ID, "
					+ " AD_Menu.InternalName "
					+ sqlDeveloperMode
					+ "FROM AD_Menu, AD_Menu_Trl t");
		}
		sql.append(" WHERE 1=1 ");
		if (!base)
		{
			sql.append(" AND AD_Menu.AD_Menu_ID=t.AD_Menu_ID AND t.AD_Language=").append(DB.TO_STRING(Env.getAD_Language(ctx)));
		}
		if (!tree.isEditable())
		{
			sql.append(" AND AD_Menu.IsActive='Y' ");
		}

		// Do not show Beta
		final IClientDAO clientDAO = Services.get(IClientDAO.class);
		final I_AD_Role role = Services.get(IRoleDAO.class).retrieveRole(ctx);
		final boolean useBetaFunctions =
				clientDAO.retriveClient(ctx).isUseBetaFunctions() || role.isRoleAlwaysUseBetaFunctions();
		if (!useBetaFunctions)
		{
			// task 09088: the client doesn't "want" to use beta functions and the role doesn't override this,
			// so we filter out features that are not marked as beta
			sql.append(" AND ");
			sql.append(" (AD_Menu.AD_Window_ID IS NULL OR EXISTS (SELECT 1 FROM AD_Window w WHERE AD_Menu.AD_Window_ID=w.AD_Window_ID AND w.IsBetaFunctionality='N'))")
			.append(" AND (AD_Menu.AD_Process_ID IS NULL OR EXISTS (SELECT 1 FROM AD_Process p WHERE AD_Menu.AD_Process_ID=p.AD_Process_ID AND p.IsBetaFunctionality='N'))")
			.append(" AND (AD_Menu.AD_Workflow_ID IS NULL OR EXISTS (SELECT 1 FROM AD_Workflow wf WHERE AD_Menu.AD_Workflow_ID=wf.AD_Workflow_ID AND wf.IsBetaFunctionality='N'))")
			.append(" AND (AD_Menu.AD_Form_ID IS NULL OR EXISTS (SELECT 1 FROM AD_Form f WHERE AD_Menu.AD_Form_ID=f.AD_Form_ID AND f.IsBetaFunctionality='N'))");
		}
		// In R/O Menu - Show only defined Forms
		if (!tree.isEditable())
		{
			sql.append(" AND ");
			sql.append("(AD_Menu.AD_Form_ID IS NULL OR EXISTS (SELECT 1 FROM AD_Form f WHERE AD_Menu.AD_Form_ID=f.AD_Form_ID AND ");
			if (m_clientTree)
			{
				sql.append("f.Classname");
			}
			else
			{
				sql.append("f.JSPURL");
			}
			sql.append(" IS NOT NULL))");
		}

		return sql.toString();
	}

	@Override
	public MTreeNode loadNodeInfo(final MTree tree, final ResultSet rs) throws SQLException
	{
		final MTreeNode info = super.loadNodeInfo(tree, rs);
		
		// me16: also load the menu entry's internal name
		final String internalName = rs.getString(I_AD_Menu.COLUMNNAME_InternalName);
		info.setInternalName(internalName);
				
		final String action = rs.getString(I_AD_Menu.COLUMNNAME_Action);
		info.setImageIndicator(action);
		if (info.getAllowsChildren() || action == null)
		{
			return info;
		}

		final int AD_Window_ID = rs.getInt(I_AD_Menu.COLUMNNAME_AD_Window_ID);
		final int AD_Process_ID = rs.getInt(I_AD_Menu.COLUMNNAME_AD_Process_ID);
		final int AD_Form_ID = rs.getInt(I_AD_Menu.COLUMNNAME_AD_Form_ID);
		final int AD_Workflow_ID = rs.getInt(I_AD_Menu.COLUMNNAME_AD_Workflow_ID);
		final int AD_Task_ID = rs.getInt(I_AD_Menu.COLUMNNAME_AD_Task_ID);
		// int AD_Workbench_ID = m_nodeRowSet.getInt(I_AD_Menu.COLUMNNAME_AD_Workbench_ID);
		//
		final IUserRolePermissions role = Env.getUserRolePermissions(tree.getCtx());
		Boolean access = null;
		if (X_AD_Menu.ACTION_Window.equals(action))
		{
			access = role.checkWindowAccess(AD_Window_ID);
			
			if (Services.get(IDeveloperModeBL.class).isEnabled())
			{
				final String tableName = rs.getString("AD_Window_TableName"); // table name of first window tab 
				info.setName(info.getName() + " (" + tableName + ")");
			}
		}
		else if (X_AD_Menu.ACTION_Process.equals(action))
		{
			access = role.checkProcessAccess(AD_Process_ID);
			
			if (Services.get(IDeveloperModeBL.class).isEnabled())
			{
				final String classname = rs.getString("AD_Process_ClassName");
				info.setName(info.getName() + " (" + classname + ")");
			}
		}
		else if (X_AD_Menu.ACTION_Report.equals(action))
		{
			access = role.checkProcessAccess(AD_Process_ID);
		}
		else if (X_AD_Menu.ACTION_Form.equals(action))
		{
			access = role.checkFormAccess(AD_Form_ID);
			
			if (Services.get(IDeveloperModeBL.class).isEnabled())
			{
				final String classname = rs.getString("AD_Form_ClassName");
				info.setName(info.getName() + " (" + classname + ")");
			}
		}
		else if (X_AD_Menu.ACTION_WorkFlow.equals(action))
		{
			access = role.checkWorkflowAccess(AD_Workflow_ID);
		}
		else if (X_AD_Menu.ACTION_Task.equals(action))
		{
			access = role.checkTaskAccess(AD_Task_ID);
			// else if (X_AD_Menu.ACTION_Workbench.equals(action))
			// access = role.getWorkbenchAccess(AD_Window_ID);
			// log.fine("getNodeDetail - " + name + " - " + actionColor + " - " + access);
		}

		//
		if (access == null // rw or ro for Role
				&& !tree.isEditable())
		{
			return null;
		}
		return info;
	}
}