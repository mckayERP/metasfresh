package de.metas.invoicecandidate.api;

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


import java.util.List;
import java.util.Properties;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.ISingletonService;

import de.metas.invoicecandidate.model.I_C_ILCandHandler;
import de.metas.invoicecandidate.model.I_C_Invoice_Candidate;
import de.metas.invoicecandidate.spi.IInvoiceCandidateHandler;

/**
 * This API identifies and invokes {@link IInvoiceCandidateHandler}s for specific invoice candidates
 */
public interface IInvoiceCandidateHandlerBL extends ISingletonService
{
	List<IInvoiceCandidateHandler> retrieveImplementationsForTable(Properties ctx, String tableName);

	/**
	 * Calls {@link #mkInstance(I_C_ILCandHandler)} with the given handlerRecord and sets the record's SourceTable and Is_AD_User_InCharge_UI_Setting fields.
	 *
	 * @param ilCandGenerator
	 *
	 * @see IInvoiceCandidateHandler
	 */
	void evalClassName(I_C_ILCandHandler handlerRecord);

	/**
	 * Creates and returns a new instance of the ICreator SPI implementation defined in the classname column of the given creatorRecord.
	 *
	 * @param creatorRecord
	 * @return
	 */
	IInvoiceCandidateHandler mkInstance(I_C_ILCandHandler creatorRecord);

	/**
	 * Invokes all available {@link I_C_ILCandGenerator} records to create missing invoice candidates.
	 *
	 * Each created invoice candidate has a reference to the {@link I_C_ILCandGenerator} from whose {@link IInvoiceCandidateHandler} implementation it has been created.
	 *
	 * Note: this method does not return the created candidates because there might be too many of them.
	 *
	 * @param ctx
	 */
	void createMissingCandidates(Properties ctx);

	/**
	 * Creates missing invoice candidates for the given table name and model. It is assumes that 'model' can be handled by {@link InterfaceWrapperHelper}.
	 *
	 * Each created invoice candidate has a reference to the {@link I_C_ILCandHandler} from whose {@link IInvoiceCandidateHandler} implementation it has been created.
	 *
	 * @param model
	 * @return generated invoice candidates
	 */
	List<I_C_Invoice_Candidate> createMissingCandidatesFor(Object model);

	void setNetAmtToInvoice(I_C_Invoice_Candidate ic);

	/**
	 * Retrieve the {@link IInvoiceCandidateHandler} of the given <code>ic</code> and calls its {@link IInvoiceCandidateHandler#setOrderedData(I_C_Invoice_Candidate) setOrderedData()} method.
	 *
	 * @param ic
	 */
	void setOrderedData(I_C_Invoice_Candidate ic);

	/**
	 * Retrieve the {@link IInvoiceCandidateHandler} of the given <code>ic</code> and calls its {@link IInvoiceCandidateHandler#setDeliveredData(I_C_Invoice_Candidate) setDeliveredData()} method.
	 *
	 * @param ic
	 */
	void setDeliveredData(I_C_Invoice_Candidate ic);

	/**
	 * Sets: <code>PriceActual</code>, <code>Price_UOM_ID</code>, <code>IsTaxIncluded</code> for the given <code>ic</code>.
	 *
	 * @param ic
	 */
	void setPriceActual(I_C_Invoice_Candidate ic);

	void invalidateCandidatesFor(Object model);

	void setBPartnerData(I_C_Invoice_Candidate ic);

	void setC_UOM_ID(I_C_Invoice_Candidate ic);

	void setPriceEntered(I_C_Invoice_Candidate ic);
}