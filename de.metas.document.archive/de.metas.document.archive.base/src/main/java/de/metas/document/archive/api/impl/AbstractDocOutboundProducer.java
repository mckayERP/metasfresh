package de.metas.document.archive.api.impl;

/*
 * #%L
 * de.metas.document.archive.base
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
import java.util.logging.Logger;

import org.adempiere.document.service.IDocActionBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_DocType;
import org.compiere.util.CLogger;

import de.metas.async.api.IWorkPackageQueue;
import de.metas.async.processor.IWorkPackageQueueFactory;
import de.metas.document.archive.api.IDocOutboundProducer;
import de.metas.document.archive.api.IDocOutboundProducerService;
import de.metas.document.archive.async.spi.impl.DocOutboundWorkpackageProcessor;
import de.metas.document.archive.model.I_C_Doc_Outbound_Config;

/**
 * {@link IDocOutboundProducer} base implementation.
 * 
 * Mainly all business logic is implemented but the life-cycle management methods are left unimplemented (see {@link #init(IDocOutboundProducerService)}, {@link #destroy(IDocOutboundProducerService)}
 * ).
 * 
 * @author tsa
 * 
 */
public abstract class AbstractDocOutboundProducer implements IDocOutboundProducer
{
	protected final transient Logger logger = CLogger.getCLogger(getClass());

	private final I_C_Doc_Outbound_Config config;
	private final String tableName;
	private final boolean isDocument;

	@Override
	public abstract void init(IDocOutboundProducerService producerService);

	@Override
	public abstract void destroy(IDocOutboundProducerService producerService);

	public AbstractDocOutboundProducer(final I_C_Doc_Outbound_Config config)
	{
		super();

		Check.assumeNotNull(config, "config not null");

		this.config = config;
		tableName = config.getAD_Table().getTableName();

		if (Services.get(IDocActionBL.class).isDocumentTable(tableName))
		{
			isDocument = true;
		}
		else
		{
			isDocument = false;
		}
	}

	@Override
	public String toString()
	{
		return "DocOutboundProducer ["
				+ "tableName=" + tableName
				+ ", isDocument=" + isDocument
				+ ", config=" + config
				+ "]";
	}

	@Override
	public I_C_Doc_Outbound_Config getC_Doc_Outbound_Config()
	{
		return config;
	}

	public String getTableName()
	{
		return tableName;
	}

	public boolean isDocument()
	{
		return isDocument;
	}

	@Override
	public boolean accept(final Object model)
	{
		if (model == null)
		{
			return false;
		}

		// Check tableName match
		final String modelTableName = InterfaceWrapperHelper.getModelTableName(model);
		if (!tableName.equals(modelTableName))
		{
			return false;
		}

		if (isDocument)
		{
			return acceptDocument(model);
		}

		return true;
	}

	protected boolean acceptDocument(final Object model)
	{
		final String requiredDocBaseType = config.getDocBaseType();
		if (!Check.isEmpty(requiredDocBaseType, true))
		{
			final I_C_DocType docType = Services.get(IDocActionBL.class).getDocTypeOrNull(model);
			if (docType == null)
			{
				logger.log(Level.INFO, "No document type found for {0}. Ignore it.", model);
				return false;
			}

			final String actualDocBaseType = docType.getDocBaseType();
			if (!requiredDocBaseType.equals(actualDocBaseType))
			{
				logger.log(Level.FINE, "Skip {0} because it has DocBaseType={1} when {2} was expected", new Object[] { model, actualDocBaseType, requiredDocBaseType });
				return false;
			}
		}

		return true;
	}

	@Override
	public void createDocOutbound(final Object model)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(model);
		final IWorkPackageQueue packageQueue = Services.get(IWorkPackageQueueFactory.class).getQueueForEnqueuing(ctx, DocOutboundWorkpackageProcessor.class);
		packageQueue.enqueueElement(model);
	}
}