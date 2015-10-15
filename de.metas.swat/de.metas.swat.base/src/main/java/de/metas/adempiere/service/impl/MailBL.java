/**
 *
 */
package de.metas.adempiere.service.impl;

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


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.internet.InternetAddress;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Check;
import org.adempiere.util.ProcessUtil;
import org.adempiere.util.Services;
import org.compiere.db.CConnection;
import org.compiere.interfaces.Server;
import org.compiere.model.I_AD_Client;
import org.compiere.model.I_AD_MailBox;
import org.compiere.model.I_AD_MailConfig;
import org.compiere.model.I_AD_User;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.Ini;

import de.metas.adempiere.service.IMailBL;

/**
 * @author Cristina Ghita, Metas.RO
 * @see http://dewiki908/mediawiki/index.php/US901:_Postfächer_pro_Organisation_einstellen_können_%282010110510000031 %29
 */
public class MailBL implements IMailBL
{
	private final CLogger log = CLogger.getCLogger(getClass());

	private static final String SYSCONFIG_DebugMailTo = "org.adempiere.user.api.IUserBL.DebugMailTo";

	private static class Mailbox implements IMailbox
	{
		private final String smtpHost;
		private final String email;
		private final String username;
		private final String password;
		private final boolean isSmtpAuthorization;
		private final boolean isSendFromServer;
		private final int adClientId;
		private final int adUserId;

		public Mailbox(final String smtpHost, final String email,
				final String username, final String password,
				final boolean isSmtpAuthorization,
				final boolean isSendFromServer,
				final int AD_Client_ID, final int AD_User_ID)
		{
			this.smtpHost = smtpHost;
			this.email = email;
			this.username = username;
			this.password = password;
			this.isSmtpAuthorization = isSmtpAuthorization;
			this.isSendFromServer = isSendFromServer;
			adClientId = AD_Client_ID;
			adUserId = AD_User_ID;
		}

		@Override
		public String getSmtpHost()
		{
			return smtpHost;
		}

		@Override
		public String getEmail()
		{
			return email;
		}

		@Override
		public String getUsername()
		{
			return username;
		}

		@Override
		public String getPassword()
		{
			return password;
		}

		@Override
		public boolean isSmtpAuthorization()
		{
			return isSmtpAuthorization;
		}

		@Override
		public boolean isSendFromServer()
		{
			return isSendFromServer;
		}

		@Override
		public int getAD_Client_ID()
		{
			return adClientId;
		}

		@Override
		public int getAD_User_ID()
		{
			return adUserId;
		}

		@Override
		public String toString()
		{
			return "Mailbox [smtpHost=" + smtpHost
					+ ", email=" + email
					+ ", username=" + username
					+ ", password=" + (password == null ? "(empty)" : "********")
					+ ", isSmtpAuthorization=" + isSmtpAuthorization
					+ ", isSendFromServer=" + isSendFromServer
					+ ", clientId=" + adClientId
					+ ", userId=" + adUserId
					+ "]";
		}
	}

	// @Cached
	@Override
	public IMailbox findMailBox(final I_AD_Client client, final int AD_Org_ID, final int AD_Process_ID, final String customType, final I_AD_User user)
	{
		IMailbox mailbox = findMailBox(client, AD_Org_ID, AD_Process_ID, customType);
		if (user != null)
		{
			mailbox = new Mailbox(mailbox.getSmtpHost(),
					user.getEMail(),
					user.getEMailUser(),
					user.getEMailUserPW(),
					mailbox.isSmtpAuthorization(),
					mailbox.isSendFromServer(),
					mailbox.getAD_Client_ID(),
					user.getAD_User_ID()
					);
		}
		return mailbox;
	}

	public IMailbox findMailBox(final I_AD_Client client, final int AD_Org_ID, final int AD_Process_ID, final String customType)
	{
		log.fine("Looking for AD_Client_ID=" + client.getAD_Client_ID() + ", AD_Org_ID=" + AD_Org_ID + ", AD_Process_ID=" + AD_Process_ID + ", customType=" + customType);

		final ArrayList<Object> params = new ArrayList<Object>();
		final StringBuffer whereClause = new StringBuffer();

		whereClause.append(I_AD_MailConfig.COLUMNNAME_AD_Client_ID).append(" = ?");
		params.add(client.getAD_Client_ID());

		// + I_AD_MailConfig.COLUMNNAME_AD_Org_ID + " IN (0 , ?)";
		// params.add(AD_Org_ID);
		if (!Check.isEmpty(customType, true))
		{
			whereClause.append(" AND ").append(I_AD_MailConfig.COLUMNNAME_CustomType).append(" = ? ");
			params.add(customType);
		}
		else if (AD_Process_ID > 0)
		{
			whereClause.append(" AND ").append(I_AD_MailConfig.COLUMNNAME_AD_Process_ID).append(" = ? ");
			params.add(AD_Process_ID);
		}

		final List<I_AD_MailConfig> configs = new Query(Env.getCtx(), I_AD_MailConfig.Table_Name, whereClause.toString(), null)
				.setOnlyActiveRecords(true)
				.setParameters(params)
				.setOrderBy(I_AD_MailConfig.COLUMNNAME_AD_Org_ID + " DESC ")
				.list(I_AD_MailConfig.class);
		for (final I_AD_MailConfig config : configs)
		{
			if (config.getAD_Org_ID() == AD_Org_ID
					|| config.getAD_Org_ID() != AD_Org_ID && config.getAD_Org_ID() == 0)
			{
				final I_AD_MailBox adMailbox = config.getAD_MailBox();
				final Mailbox mailbox = new Mailbox(adMailbox.getSMTPHost(),
						adMailbox.getEMail(),
						adMailbox.getUserName(),
						adMailbox.getPassword(),
						adMailbox.isSmtpAuthorization(),
						client.isServerEMail(),
						client.getAD_Client_ID(),
						-1 // AD_User_ID
				);
				log.fine("Found: " + getSummary(config) + "=>" + mailbox);
				return mailbox;
			}
		}

		final Mailbox mailbox = new Mailbox(client.getSMTPHost(),
				client.getRequestEMail(),
				client.getRequestUser(),
				client.getRequestUserPW(),
				client.isSmtpAuthorization(),
				client.isServerEMail(),
				client.getAD_Client_ID(),
				-1 // AD_User_ID
		);
		log.fine("Fallback to AD_Client settings: " + mailbox);
		return mailbox;
	}

	@Override
	public EMail createEMail(final I_AD_Client client,
			final String mailCustomType,
			final String to,
			final String subject,
			final String message,
			final boolean html)
	{
		final I_AD_User from = null;
		return createEMail(client, mailCustomType, from, to, subject, message, html);
	}

	@Override
	public EMail createEMail(final I_AD_Client client,
			final String mailCustomType,
			final I_AD_User from,
			final String to,
			final String subject,
			final String message,
			final boolean html)
	{
		final Properties ctx = POWrapper.getCtx(client);
		final IMailbox mailbox = findMailBox(client, ProcessUtil.getCurrentOrgId(), ProcessUtil.getCurrentProcessId(), mailCustomType, from);
		return createEMail(ctx, mailbox, to, subject, message, html);
	}

	@Override
	public EMail createEMail(final Properties ctx,
			final IMailbox mailbox,
			final String to,
			final String subject,
			String message,
			final boolean html)
	{
		if (Check.isEmpty(to, true))
		{
			throw new AdempiereException("No To address");
		}

		if (mailbox.getEmail() == null
				|| mailbox.getUsername() == null
				// is SMTP authorisation and password is null - teo_sarca [ 1723309 ]
				|| mailbox.isSmtpAuthorization() && mailbox.getPassword() == null)
		{
			throw new AdempiereException("Mailbox incomplete: " + mailbox);
		}

		EMail email = null;
		if (mailbox.isSendFromServer() && Ini.isClient())
		{
			log.fine("Creating on server");
			final Server server = CConnection.get().getServer();
			try
			{
				if (server != null)
				{ // See ServerBean
					if (html && message != null)
					{
						message = EMail.HTML_MAIL_MARKER + message;
					}

					if (mailbox.getAD_User_ID() < 0)
					{
						email = server.createEMail(Env.getRemoteCallCtx(ctx), mailbox.getAD_Client_ID(),
								to, subject, message);
					}
					else
					{
						email = server.createEMail(Env.getRemoteCallCtx(ctx), mailbox.getAD_Client_ID(),
								mailbox.getAD_User_ID(),
								to, subject, message);
					}
				}
				else
				{
					log.log(Level.WARNING, "No AppsServer");
				}
			}
			catch (final Exception ex)
			{
				log.log(Level.SEVERE, "" + mailbox + " - AppsServer error: " + ex.getLocalizedMessage(), ex);
			}
		}

		if (email == null)
		{
			log.fine("Creating on client");
			email = new EMail(ctx, mailbox.getSmtpHost(), mailbox.getEmail(), to, subject, message, html);
			if (mailbox.isSmtpAuthorization())
			{
				email.createAuthenticator(mailbox.getUsername(), mailbox.getPassword());
			}
		}
		return email;
	}

	public String getSummary(final I_AD_MailConfig config)
	{
		return config.getClass().getSimpleName() + "["
				+ "AD_Client_ID=" + config.getAD_Client_ID()
				+ ", AD_Org_ID=" + config.getAD_Org_ID()
				+ ", AD_Process_ID=" + config.getAD_Process_ID()
				+ ", CustomType=" + config.getCustomType()
				+ ", IsActive=" + config.isActive()
				+ ", AD_MailConfig_ID=" + config.getAD_MailConfig_ID()
				+ "]";
	}

	public String getSummary(final I_AD_MailBox mailbox)
	{
		return mailbox.getClass().getSimpleName() + "["
				+ "SMTPHost=" + mailbox.getSMTPHost()
				+ ", EMail=" + mailbox.getEMail()
				+ ", Password=" + (Check.isEmpty(mailbox.getPassword()) ? "(empty)" : "******")
				+ ", IsSMTPAuth=" + mailbox.isSmtpAuthorization()
				+ ", AD_MailBox_ID=" + mailbox.getAD_MailBox_ID()
				+ "]";
	}

	@Override
	public InternetAddress getDebugMailToAddressOrNull(final Properties ctx)
	{
		String emailStr = Services.get(ISysConfigBL.class).getValue(SYSCONFIG_DebugMailTo,
				null, // defaultValue
				Env.getAD_Client_ID(ctx),
				Env.getAD_Org_ID(ctx));
		if (Check.isEmpty(emailStr, true))
		{
			return null;
		}

		emailStr = emailStr.trim();

		if (emailStr.equals("-"))
		{
			return null;
		}

		final InternetAddress email;
		try
		{
			email = new InternetAddress(emailStr, true);
		}
		catch (final Exception e)
		{
			log.log(Level.WARNING, "Invalid email address: " + emailStr, e.getLocalizedMessage());
			return null;
		}

		return email;
	}

	@Override
	public void send(final EMail email)
	{
		final String msg = email.send();
		if (!msg.equals(EMail.SENT_OK))
		{
			throw new EMailSendException(msg, email.isSentConnectionError());
		}
	}

	@Override
	public boolean isConnectionError(final Exception e)
	{
		if (e instanceof EMailSendException)
		{
			return ((EMailSendException)e).isConnectionError();
		}
		else if (e instanceof java.net.ConnectException)
		{
			return true;
		}

		return false;
	}

	public static class EMailSendException extends AdempiereException
	{
		/**
		 *
		 */
		private static final long serialVersionUID = -4519372831111638967L;

		private final boolean connectionError;

		public EMailSendException(final String msg, final boolean connectionError)
		{
			super(msg);
			this.connectionError = connectionError;
		}

		public boolean isConnectionError()
		{
			return connectionError;
		}
	}
}