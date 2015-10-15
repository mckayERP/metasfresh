package de.metas.handlingunits.materialtracking.spi.impl;

/*
 * #%L
 * de.metas.handlingunits.base
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
import java.util.logging.Level;

import org.adempiere.mm.attributes.api.IAttributeDAO;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.ILoggable;
import org.adempiere.util.Services;
import org.adempiere.util.lang.ObjectUtils;
import org.compiere.model.I_M_Attribute;
import org.compiere.util.CLogger;
import org.eevolution.model.I_PP_Order;

import de.metas.handlingunits.HUConstants;
import de.metas.handlingunits.IHUAssignmentDAO;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactoryService;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Assignment;
import de.metas.handlingunits.model.I_PP_Cost_Collector;
import de.metas.handlingunits.storage.IHUStorageFactory;
import de.metas.materialtracking.IMaterialTrackingBL;
import de.metas.materialtracking.MTLinkRequest;
import de.metas.materialtracking.MaterialTrackingListenerAdapter;
import de.metas.materialtracking.model.I_M_Material_Tracking;

/**
 * 
 * @author ts
 * @task http://dewiki908/mediawiki/index.php/09106_Material-Vorgangs-ID_nachtr%C3%A4glich_erfassen_%28101556035702%29
 */
public class HUOrderLineMaterialTrackingListener extends MaterialTrackingListenerAdapter
{
	private static final transient CLogger logger = CLogger.getCLogger(HUOrderLineMaterialTrackingListener.class);

	public static HUOrderLineMaterialTrackingListener INSTANCE = new HUOrderLineMaterialTrackingListener();

	private HUOrderLineMaterialTrackingListener()
	{
		// nothing
	}

	@Override
	public void afterModelLinked(final MTLinkRequest request)
	{
		if (!request.getParams().getParameterAsBool(HUConstants.PARAM_CHANGE_HU_MAterial_Tracking_ID))
		{
			logger.log(Level.FINE,
					"request {0} has no Params or has {1} == false; nothing to do",
					new Object[] { request, HUConstants.PARAM_CHANGE_HU_MAterial_Tracking_ID });
			return;
		}

		final IHUAssignmentDAO huAssignmentDAO = Services.get(IHUAssignmentDAO.class);

		final List<I_M_HU_Assignment> huAssignmentsForModel = huAssignmentDAO.retrieveHUAssignmentsForModel(request.getModel());
		for (final I_M_HU_Assignment huAssignment : huAssignmentsForModel)
		{
			if (huAssignment.getM_HU_ID() > 0)
			{
				processLinkRequestForHU(huAssignment.getM_HU(), request);
			}
			if (huAssignment.getM_LU_HU_ID() > 0)
			{
				processLinkRequestForHU(huAssignment.getM_LU_HU(), request);
			}
			if (huAssignment.getM_TU_HU_ID() > 0)
			{
				processLinkRequestForHU(huAssignment.getM_TU_HU(), request);
			}
			if (huAssignment.getVHU_ID() > 0)
			{
				processLinkRequestForHU(huAssignment.getVHU(), request);
			}
		}
	}

	/**
	 * This method:
	 * <ul>
	 * <li>updates the given HU's M_Matrial_Tracking_ID HU_Attribute
	 * <li>checks if the HU was issued to a PP_Order and updates the PP_Order's M_Material_Tracking_Refs
	 * </ul>
	 * 
	 * @param hu
	 * @param request
	 */
	private void processLinkRequestForHU(final I_M_HU hu, final MTLinkRequest request)
	{
		final I_M_Material_Tracking materialTracking = request.getMaterialTracking();
		final ILoggable loggable = request.getLoggable();

		//
		// update the HU itself
		{
			final IAttributeDAO attributeDAO = Services.get(IAttributeDAO.class);
			final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
			final IAttributeStorageFactoryService attributeStorageFactoryService = Services.get(IAttributeStorageFactoryService.class);

			final Properties ctx = InterfaceWrapperHelper.getCtx(hu);

			final I_M_Attribute materialTrackingAttribute = attributeDAO.retrieveAttributeByValue(
					ctx,
					I_M_Material_Tracking.COLUMNNAME_M_Material_Tracking_ID,
					I_M_Attribute.class);

			final IHUStorageFactory storageFactory = handlingUnitsBL.getStorageFactory();
			final IAttributeStorageFactory huAttributeStorageFactory = attributeStorageFactoryService.createHUAttributeStorageFactory();
			huAttributeStorageFactory.setHUStorageFactory(storageFactory);

			final IAttributeStorage attributeStorage = huAttributeStorageFactory.getAttributeStorage(hu);

			attributeStorage.setValueNoPropagate(materialTrackingAttribute, materialTracking.getM_Material_Tracking_ID());
			attributeStorage.saveChangesIfNeeded();

			final String msg = "Updated IAttributeStorage " + attributeStorage + " of M_HU " + hu;
			logger.log(Level.FINE, msg);
			loggable.addLog(msg);
		}

		//
		// check if the HU is assigned to a PP_Order and also update that PP_Order's material tracking reference
		{
			final IHUAssignmentDAO huAssignmentDAO = Services.get(IHUAssignmentDAO.class);
			final IMaterialTrackingBL materialTrackingBL = Services.get(IMaterialTrackingBL.class);

			final List<I_PP_Cost_Collector> costCollectors = huAssignmentDAO.retrieveModelsForHU(hu, I_PP_Cost_Collector.class);
			for (final I_PP_Cost_Collector costCollector : costCollectors)
			{
				if (costCollector.getPP_Order_ID() <= 0)
				{
					continue; // this might never be the case but I'm not well versed with such stuff
				}

				final I_PP_Order ppOrder = costCollector.getPP_Order();

				materialTrackingBL.unlinkModelFromMaterialTracking(ppOrder);
				materialTrackingBL.linkModelToMaterialTracking(
						MTLinkRequest.builder(request)
								.setModel(ppOrder)
								.build());

				final String msg = "Updated M_Material_Tracking_Ref for PP_Order " + ppOrder + " of M_HU " + hu;
				logger.log(Level.FINE, msg);
				loggable.addLog(msg);
			}
		}
	}

	@Override
	public String toString()
	{
		return ObjectUtils.toString(this);
	}
}