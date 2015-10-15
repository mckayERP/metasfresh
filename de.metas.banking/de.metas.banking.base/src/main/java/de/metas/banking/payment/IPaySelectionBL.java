package de.metas.banking.payment;

/*
 * #%L
 * de.metas.banking.base
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


import org.adempiere.util.ISingletonService;
import org.compiere.model.I_C_PaySelection;
import org.compiere.model.I_C_PaySelectionLine;

import de.metas.banking.interfaces.I_C_BankStatementLine_Ref;
import de.metas.banking.model.I_C_BankStatement;
import de.metas.banking.model.I_C_BankStatementLine;
import de.metas.banking.model.I_C_Payment;

public interface IPaySelectionBL extends ISingletonService
{
	/**
	 * Creates a new pay selection updater which will create/updated/delete the {@link I_C_PaySelectionLine}s.
	 * 
	 * @return updater
	 */
	IPaySelectionUpdater newPaySelectionUpdater();

	/**
	 * Create bank statement lines for the given <code>bankStatement</code>, using the pay selection lines of the given <code>paySelection</code>.
	 * 
	 * @param bankStatement
	 * @param paySelection
	 */
	void createBankStatementLines(I_C_BankStatement bankStatement, I_C_PaySelection paySelection);

	/**
	 * Create payments for each line of given pay selection. The payments are created only if they where not created before. For more informations, see
	 * {@link #createPaymentIfNeeded(de.metas.banking.model.I_C_PaySelectionLine)}.
	 * 
	 * @param paySelection
	 */
	void createPayments(I_C_PaySelection paySelection);

	/**
	 * Creates a payment for given pay selection line, links the payment to line and saves the line.
	 * 
	 * A payment will be created only if
	 * <ul>
	 * <li>was not created before
	 * <li>line is not linked to a {@link I_C_BankStatementLine} or {@link I_C_BankStatementLine_Ref}.
	 * </ul>
	 * 
	 * @param line
	 * @return newly created payment or <code>null</code> if no payment was generated.
	 */
	I_C_Payment createPaymentIfNeeded(de.metas.banking.model.I_C_PaySelectionLine line);

}