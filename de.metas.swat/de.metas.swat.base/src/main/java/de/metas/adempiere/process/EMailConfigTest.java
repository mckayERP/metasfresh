/**
 * 
 */
package de.metas.adempiere.process;

/*
 * #%L
 * de.metas.swat.base
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


import org.adempiere.util.Services;
import org.compiere.model.I_AD_MailConfig;
import org.compiere.model.MClient;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.EMail;

import de.metas.adempiere.service.IMailBL;
import de.metas.adempiere.service.IMailBL.IMailbox;

/**
 * @author tsa
 * 
 */
public class EMailConfigTest extends SvrProcess
{
	public static final String PARA_AD_Client_ID = I_AD_MailConfig.COLUMNNAME_AD_Client_ID;
	public static final String PARA_AD_Org_ID = I_AD_MailConfig.COLUMNNAME_AD_Org_ID;
	public static final String PARA_AD_Process_ID = I_AD_MailConfig.COLUMNNAME_AD_Process_ID;
	public static final String PARA_CustomType = I_AD_MailConfig.COLUMNNAME_CustomType;
	public static final String PARA_From_User_ID = "From_User_ID";
	public static final String PARA_EMail_To = "EMail_To";
	public static final String PARA_Subject = "Subject";
	public static final String PARA_Message = "Message";
	public static final String PARA_IsHtml = "IsHtml";

	private int p_AD_Client_ID;
	private int p_AD_Org_ID;
	private int p_AD_Process_ID;
	private String p_CustomType;
	private int p_From_User_ID;
	private String p_EMail_To;
	private String p_Subject;
	private String p_Message;
	private boolean p_IsHtml;

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals(PARA_AD_Client_ID))
				p_AD_Client_ID = para.getParameterAsInt();
			else if (name.equals(PARA_AD_Org_ID))
				p_AD_Org_ID = para.getParameterAsInt();
			else if (name.equals(PARA_AD_Process_ID))
				p_AD_Process_ID = para.getParameterAsInt();
			else if (name.equals(PARA_CustomType))
				p_CustomType = (String)para.getParameter();
			else if (name.equals(PARA_From_User_ID))
				p_From_User_ID = para.getParameterAsInt();
			else if (name.equals(PARA_EMail_To))
				p_EMail_To = (String)para.getParameter();
			else if (name.equals(PARA_Subject))
				p_Subject = (String)para.getParameter();
			else if (name.equals(PARA_Message))
				p_Message = (String)para.getParameter();
			else if (name.equals(PARA_IsHtml))
				p_IsHtml = para.getParameterAsBoolean();
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		testSend();

		// final MClient client = MClient.get(getCtx(), p_AD_Client_ID);
		// client.sendEMail(p_EMail_To, p_Subject, p_Message, null, p_IsHtml);

		return "Done";
	}

	public MUser getFrom_User()
	{
		if (p_From_User_ID > 0)
			return MUser.get(getCtx(), p_From_User_ID);
		else
			return null;
	}

	private void testSend()
	{
		final IMailBL mailBL = Services.get(IMailBL.class);
		final MClient client = MClient.get(getCtx(), p_AD_Client_ID);
		final IMailbox mailbox = mailBL.findMailBox(client, p_AD_Org_ID, p_AD_Process_ID, p_CustomType, getFrom_User());
		addLog("Using configuration: " + mailbox);

		final EMail email = mailBL.createEMail(getCtx(), mailbox, p_EMail_To,
				p_Subject, p_Message, p_IsHtml);
		addLog("EMail: " + email);

		String msg = email.send();
		if (!EMail.SENT_OK.equals(msg))
		{
			addLog("Send error: " + msg);
		}
	}
}