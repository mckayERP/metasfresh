package de.metas.commission.service.impl;

/*
 * #%L
 * de.metas.commission.base
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


import java.util.Date;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.impl.CompareQueryFilter.Operator;
import org.adempiere.util.Services;
import org.compiere.util.CLogger;

import de.metas.commission.model.I_C_AdvCommissionLock;
import de.metas.commission.service.ICommissionLockDAO;

public class CommissionLockDAO implements ICommissionLockDAO
{
	private static final transient CLogger logger = CLogger.getCLogger(CommissionLockDAO.class);

	@Override
	public boolean isLocked(final org.compiere.model.I_C_BPartner bPartner, final Date date)
	{
		// @formatter:off
		final boolean locked = Services.get(IQueryBL.class).createQueryBuilder(I_C_AdvCommissionLock.class)
				.setContext(bPartner)
				.addEqualsFilter(I_C_AdvCommissionLock.COLUMNNAME_C_BPartner_ID, bPartner.getC_BPartner_ID())
				.addCompareFilter(I_C_AdvCommissionLock.COLUMNNAME_DateFrom, Operator.LessOrEqual, date)
				.addCompareFilter(I_C_AdvCommissionLock.COLUMNNAME_DateTo, Operator.GreatherOrEqual, date)
				.addOnlyActiveRecordsFilter()
				.filterByClientId()
				.create()
				.match();
		// @formatter:on
		if (!locked)
		{
			return false;
		}

		CommissionLockDAO.logger.config("BPartner " + bPartner + " is locked");
		return true;
	}

}