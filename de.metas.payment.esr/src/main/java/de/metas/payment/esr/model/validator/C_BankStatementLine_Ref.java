package de.metas.payment.esr.model.validator;

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


import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.compiere.model.ModelValidator;

import de.metas.banking.interfaces.I_C_BankStatementLine_Ref;
import de.metas.payment.esr.api.IESRImportDAO;
import de.metas.payment.esr.model.I_ESR_ImportLine;

@Interceptor(I_C_BankStatementLine_Ref.class)
public class C_BankStatementLine_Ref
{
	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_DELETE })
	public void unlinkESRImportLines(final I_C_BankStatementLine_Ref bankStatementLineRef)
	{
		final IESRImportDAO esrImportDAO = Services.get(IESRImportDAO.class);
		for (final I_ESR_ImportLine esrImportLine : esrImportDAO.fetchLinesForBankStatementLineRef(bankStatementLineRef))
		{
			esrImportLine.setC_BankStatementLine(null);
			esrImportLine.setC_BankStatementLine_Ref(null);
			InterfaceWrapperHelper.save(esrImportLine);
		}
	}
}