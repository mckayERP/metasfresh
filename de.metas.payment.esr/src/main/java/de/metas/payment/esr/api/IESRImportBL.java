package de.metas.payment.esr.api;

/*
 * #%L
 * de.metas.payment.esr
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


import org.adempiere.ad.trx.api.ITrxRunConfig;
import org.adempiere.util.ISingletonService;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Payment;

import de.metas.async.model.I_C_Async_Batch;
import de.metas.payment.esr.model.I_ESR_Import;
import de.metas.payment.esr.model.I_ESR_ImportLine;
import de.metas.payment.esr.spi.IESRActionHandler;

public interface IESRImportBL extends ISingletonService
{
	/**
	 * Loads the given V11 file by create an entry in attachment and enqueing the esr import for importing in async mode
	 * 
	 * @param esrImport
	 * @param filename
	 * @param asyncBatch
	 */
	void loadESRImportFile(I_ESR_Import esrImport, String filename, I_C_Async_Batch asyncBatch);

	/**
	 * Loads the given V11 file by creating one {@link I_ESR_ImportLine} for each line of the file, not that this method also matches the line's references against the system by calling
	 * {@link #matchESRImportLine(I_ESR_ImportLine, ITrxRunConfig)}.
	 * 
	 * @param esrImport
	 * @param filename
	 * @param trxRunConfig
	 */
	void loadESRImportFile(I_ESR_Import esrImport, ITrxRunConfig trxRunConfig);

	/**
	 * Process ESR Import by creating payment documents for its lines. Lines that are processed or that already have a payment are skipped.
	 * 
	 * @param esrImport
	 * @param trxRunConfig
	 * @return the number of lines that the method created payments for
	 */
	int process(I_ESR_Import esrImport, ITrxRunConfig trxRunConfig);

	/**
	 * Method for calculating the check digit.
	 * 
	 * @param text
	 * @return checksum digit (between 0 and 9)
	 * @see <a href="http://w2.syronex.com/jmr/programming/mod11chk">ISBNs & The Modulo 11 Checksum Algorithm</a>
	 */
	int calculateESRCheckDigit(String text);

	/**
	 * Fertigstellen => Complete the import. Shall only be possible, when all rows have an ESR_Payment_Action. After pressing Button, the Header is set to Processed. And will only be shown again after
	 * explicit search.
	 * 
	 * @param esrImport
	 * @param message a status message or description to be passed to new documents (e.g. Payments or Allocations) that might be created by this method.
	 * @param trxRunConfig
	 */
	void complete(I_ESR_Import esrImport, String message, ITrxRunConfig trxRunConfig);

	void addErrorMsg(I_ESR_ImportLine importLine, String msg);

	/**
	 * Updates the given <code>importLine</code>'s <code>C_Invoice_ID</code> together with its related columns:
	 * <ul>
	 * <li>ESR_Invoice_Grandtotal</li>
	 * <li>ESR_Invoice_Openamt</li>
	 * <li>ESR_Document_Status: always updating to "partially matched"</li>
	 * </ul>
	 * 
	 * Also important
	 * <ul>
	 * <li>Doesn't actually save the given <code>importLine</code></li>
	 * <li>Starts by calling {@link #updateLinesOpenAmt(I_ESR_ImportLine, I_C_Invoice)}, i.e. it might also update other import line of the same import.
	 * </ul>
	 * 
	 * @param importLine
	 * @param invoice
	 */
	void setInvoice(final I_ESR_ImportLine importLine, final I_C_Invoice invoice);

	/**
	 * Registers a handler for a payment action.
	 * 
	 * @param actionName
	 * @param handler
	 */
	void registerActionHandler(String actionName, IESRActionHandler handler);

	/**
	 * @param payment Reverse all allocations of the payment; Unset invoice in the payment
	 */
	void reverseAllocationForPayment(I_C_Payment payment);

	/**
	 * 
	 * An approach for creating checksums for imported files, for the system to be able to prevent duplicates
	 * 
	 * @see <a href="http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java">http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java</a>
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	String getMD5Checksum(String filename) throws Exception;

	/**
	 * enqueue the esr import in order to be imported later
	 * 
	 * @param esrImport
	 * @param asyncBatch
	 */
	void enqueESRImport(I_ESR_Import esrImport, I_C_Async_Batch asyncBatch);

	/**
	 * Link an unlinked payment to the importLine invoice (i.e. create allocation).
	 * 
	 * @param importLine
	 */
	void linkInvoiceToPayment(I_ESR_ImportLine importLine);

	/**
	 * Check is the import is processed by checking if all lines are processed
	 * 
	 * @param esrImport
	 * @return
	 */
	boolean isProcessed(I_ESR_Import esrImport);

}