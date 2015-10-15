package de.metas.materialtracking.model.validator;

/*
 * #%L
 * de.metas.materialtracking
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

import org.adempiere.ad.modelvalidator.annotations.Init;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.inout.service.IInOutDAO;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_InOutLine;

import de.metas.materialtracking.IMaterialTrackingAttributeBL;
import de.metas.materialtracking.IMaterialTrackingBL;
import de.metas.materialtracking.model.I_M_Material_Tracking;
import de.metas.materialtracking.spi.impl.listeners.InOutLineMaterialTrackingListener;

@Interceptor(I_M_InOut.class)
public class M_InOut extends MaterialTrackableDocumentByASIInterceptor<I_M_InOut, I_M_InOutLine>
{
	@Init
	public void init()
	{
		final IMaterialTrackingBL materialTrackingBL = Services.get(IMaterialTrackingBL.class);

		materialTrackingBL.addModelTrackingListener(
				InOutLineMaterialTrackingListener.LISTENER_TableName,
				InOutLineMaterialTrackingListener.instance);
	}

	@Override
	protected final boolean isEligibleForMaterialTracking(final I_M_InOut receipt)
	{
		// Shipments are not eligible
		if (receipt.isSOTrx())
		{
			return false;
		}

		return true;
	}

	@Override
	protected List<I_M_InOutLine> retrieveDocumentLines(final I_M_InOut document)
	{
		final IInOutDAO inoutDAO = Services.get(IInOutDAO.class);

		final List<I_M_InOutLine> documentLines = inoutDAO.retrieveLines(document);
		return documentLines;
	}

	@Override
	protected I_M_Material_Tracking getMaterialTrackingFromDocumentLineASI(final I_M_InOutLine documentLine)
	{
		final de.metas.materialtracking.model.I_M_InOutLine iolExt = InterfaceWrapperHelper.create(documentLine, de.metas.materialtracking.model.I_M_InOutLine.class);
		if (iolExt.getM_Material_Tracking_ID() > 0)
		{
			return iolExt.getM_Material_Tracking();
		}

		// fall-back in case the M_Material_Tracking_ID is not (yet) set
		final IMaterialTrackingAttributeBL materialTrackingAttributeBL = Services.get(IMaterialTrackingAttributeBL.class);

		final I_M_AttributeSetInstance asi = iolExt.getM_AttributeSetInstance();
		final I_M_Material_Tracking materialTracking = materialTrackingAttributeBL.getMaterialTracking(asi);
		return materialTracking;
	}

	@Override
	protected I_M_AttributeSetInstance getM_AttributeSetInstance(final I_M_InOutLine documentLine)
	{
		// shall not be called because we implement "getMaterialTrackingFromDocumentLineASI"
		throw new IllegalStateException("shall not be called");
	}
}