package de.metas.banking.service.impl;

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


import java.util.List;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BankStatement;
import org.compiere.model.I_C_BankStatementLine;
import org.compiere.model.I_C_Payment;

import de.metas.banking.interfaces.I_C_BankStatementLine_Ref;
import de.metas.banking.service.IBankStatementDAO;

public class BankStatementDAO implements IBankStatementDAO
{

	@Override
	public <T extends I_C_BankStatementLine> List<T> retrieveLines(final I_C_BankStatement bankStatement, final Class<T> clazz)
	{
		return retrieveLinesQuery(bankStatement)
				.create()
				.list(clazz);
	}

	private IQueryBuilder<I_C_BankStatementLine> retrieveLinesQuery(final I_C_BankStatement bankStatement)
	{
		return Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_BankStatementLine.class)
				.setContext(bankStatement)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_BankStatementLine.COLUMNNAME_C_BankStatement_ID, bankStatement.getC_BankStatement_ID())
				//
				.orderBy()
				.addColumn(I_C_BankStatementLine.COLUMNNAME_Line)
				.addColumn(I_C_BankStatementLine.COLUMNNAME_C_BankStatementLine_ID)
				.endOrderBy();
	}

	@Override
	public List<I_C_BankStatementLine_Ref> retrieveLineReferences(final org.compiere.model.I_C_BankStatementLine bankStatementLine)
	{
		return retrieveLineReferencesQuery(bankStatementLine)
				.create()
				.list(I_C_BankStatementLine_Ref.class);
	}

	public IQueryBuilder<I_C_BankStatementLine_Ref> retrieveLineReferencesQuery(final org.compiere.model.I_C_BankStatementLine bankStatementLine)
	{
		return Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_BankStatementLine_Ref.class)
				.setContext(bankStatementLine)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_BankStatementLine_Ref.COLUMNNAME_C_BankStatementLine_ID, bankStatementLine.getC_BankStatementLine_ID())
				.orderBy()
				.addColumn(I_C_BankStatementLine_Ref.COLUMNNAME_Line)
				.addColumn(I_C_BankStatementLine_Ref.COLUMNNAME_C_BankStatementLine_Ref_ID)
				.endOrderBy();
	}

	@Override
	public boolean isPaymentOnBankStatement(final I_C_Payment payment)
	{
		final int paymentId = payment.getC_Payment_ID();
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		//
		// Check if payment is on any bank statement line reference, processed or not
		final boolean hasBankStatementLineRefs = queryBL.createQueryBuilder(I_C_BankStatementLine_Ref.class)
				.setContext(payment)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_BankStatementLine_Ref.COLUMNNAME_C_Payment_ID, paymentId)
				.create()
				.match();
		if (hasBankStatementLineRefs)
		{
			return true;
		}

		//
		// Check if payment is on any bank statement line, processed or not
		final boolean hasBankStatementLines = queryBL.createQueryBuilder(I_C_BankStatementLine.class)
				.setContext(payment)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_BankStatementLine.COLUMN_C_Payment_ID, paymentId)
				.create()
				.match();
		if (hasBankStatementLines)
		{
			return true;
		}

		return false;
	}
}