package de.metas.materialtracking.qualityBasedInvoicing.impl;

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


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_M_Product;
import org.compiere.util.CLogger;
import org.eevolution.api.IPPOrderBOMBL;
import org.eevolution.api.IPPOrderBOMDAO;
import org.eevolution.model.I_PP_Order;
import org.eevolution.model.I_PP_Order_BOMLine;

import de.metas.materialtracking.qualityBasedInvoicing.IProductionMaterial;
import de.metas.materialtracking.qualityBasedInvoicing.IProductionMaterialQuery;
import de.metas.materialtracking.qualityBasedInvoicing.ProductionMaterialType;

/* package */class ProductionMaterialQueryExecutor
{

	private static final transient CLogger logger = CLogger.getCLogger(ProductionMaterialQueryExecutor.class);

	// Services
	private final transient IPPOrderBOMDAO ppOrderBOMDAO = Services.get(IPPOrderBOMDAO.class);
	private final transient IPPOrderBOMBL ppOrderBOMBL = Services.get(IPPOrderBOMBL.class);

	private final IProductionMaterialQuery query;
	private final I_PP_Order ppOrder;
	private final List<ProductionMaterialType> types;

	private List<I_PP_Order_BOMLine> _ppOrderBOMLines;

	public ProductionMaterialQueryExecutor(final IProductionMaterialQuery query)
	{
		super();

		Check.assumeNotNull(query, "query not null");
		this.query = query;

		final I_PP_Order ppOrder = query.getPP_Order();
		Check.assumeNotNull(ppOrder, "ppOrder not null");
		this.ppOrder = ppOrder;

		types = query.getTypes();
		Check.assumeNotEmpty(types, "Types not not empty for query: {0}", query);
	}

	public IProductionMaterial retriveSingleProductionMaterial()
	{
		final List<IProductionMaterial> productionMaterials = retriveProductionMaterials();
		if (productionMaterials.isEmpty())
		{
			return null;
		}
		else if (productionMaterials.size() == 1)
		{
			return productionMaterials.get(0);
		}
		else
		{
			throw new AdempiereException("More then one Production Material was found for query."
					+ "\n Query: " + query
					+ "\n Production Materials: " + productionMaterials);
		}
	}

	public List<IProductionMaterial> retriveProductionMaterials()
	{
		final List<IProductionMaterial> result = new ArrayList<>();

		//
		// Create Production Material from manufacturing order header (if eligible)
		{
			final IProductionMaterial productionMaterial = createProductionMaterialIfEligible(ppOrder);
			if (productionMaterial != null)
			{
				result.add(productionMaterial);
			}
		}

		//
		// Iterate BOM Lines
		final List<I_PP_Order_BOMLine> ppOrderBOMLines = getAllPPOrderBOMLines();
		for (final I_PP_Order_BOMLine ppOrderBOMLine : ppOrderBOMLines)
		{
			final IProductionMaterial productionMaterial = createProductionMaterialIfEligible(ppOrderBOMLine);
			if (productionMaterial == null)
			{
				continue;
			}
			result.add(productionMaterial);
		}

		return result;
	}

	private IProductionMaterial createProductionMaterialIfEligible(final I_PP_Order ppOrder)
	{
		//
		// Check is Product is matching our query
		final I_M_Product queryProduct = query.getM_Product();
		if (queryProduct != null && queryProduct.getM_Product_ID() != ppOrder.getM_Product_ID())
		{
			// Query Product does not match PP Order Product
			return null;
		}

		//
		// Create Production Material
		final IProductionMaterial productionMaterial = new PPOrderProductionMaterial(ppOrder);

		//
		// Check is Type is matching our query
		if (!isTypeRequested(productionMaterial.getType()))
		{
			return null;
		}

		return productionMaterial;
	}

	private IProductionMaterial createProductionMaterialIfEligible(final I_PP_Order_BOMLine ppOrderBOMLine)
	{
		//
		// Check if Product is matching the query
		final I_M_Product queryProduct = query.getM_Product();
		if (queryProduct != null && queryProduct.getM_Product_ID() != ppOrderBOMLine.getM_Product_ID())
		{
			logger.log(Level.FINE, "Query Product {0} does not match BOM Line M_Product_ID {1}", new Object[] { queryProduct, ppOrderBOMLine.getM_Product_ID() });
			return null;
		}

		//
		// In case our BOM Line is a Variant of a main component line,
		// find out which is the main component line and get the main product from it
		final I_M_Product mainComponentProduct;
		if (ppOrderBOMBL.isVariant(ppOrderBOMLine))
		{
			final I_PP_Order_BOMLine mainComponentBOMLine = getMainComponentOrderBOMLine(ppOrderBOMLine);
			mainComponentProduct = mainComponentBOMLine.getM_Product();
		}
		else
		{
			mainComponentProduct = null;
		}

		//
		// Create Production Material
		final IProductionMaterial productionMaterial = new PPOrderBOMLineProductionMaterial(ppOrderBOMLine, mainComponentProduct);

		//
		// Check if Type is matching the query
		if (!isTypeRequested(productionMaterial.getType()))
		{
			logger.log(Level.FINE, "IProductionMaterial {0} does not match the any of the requested types {1}", new Object[] { productionMaterial, types });
			return null;
		}

		return productionMaterial;
	}

	private boolean isTypeRequested(final ProductionMaterialType type)
	{
		return types.contains(type);
	}

	private List<I_PP_Order_BOMLine> getAllPPOrderBOMLines()
	{
		if (_ppOrderBOMLines == null)
		{
			_ppOrderBOMLines = ppOrderBOMDAO.retrieveOrderBOMLines(ppOrder);
		}
		return _ppOrderBOMLines;

	}

	/**
	 * Gets the main component BOM Line for a given Variant BOM Line.
	 *
	 * @param variantComponentBOMLine
	 * @return main component BOM Line; never return null
	 */
	private I_PP_Order_BOMLine getMainComponentOrderBOMLine(final I_PP_Order_BOMLine variantComponentBOMLine)
	{
		Check.assumeNotNull(variantComponentBOMLine, "variantComponentBOMLine not null");

		final String variantGroup = variantComponentBOMLine.getVariantGroup();
		Check.assumeNotEmpty(variantGroup, "variantGroup not empty");

		//
		// Iterate through order BOM Lines and find out which is our main component line which have the same VariantGroup
		I_PP_Order_BOMLine componentBOMLine = null;
		final List<I_PP_Order_BOMLine> ppOrderBOMLines = getAllPPOrderBOMLines();
		for (final I_PP_Order_BOMLine ppOrderBOMLine : ppOrderBOMLines)
		{
			// lines which are not components are not interesting for us
			if (!ppOrderBOMBL.isComponent(ppOrderBOMLine))
			{
				continue;
			}

			// lines which does not have our variant group are not interesting for us
			if (!Check.equals(variantGroup, ppOrderBOMLine.getVariantGroup()))
			{
				continue;
			}

			// We found our main component line.
			// Make sure is the only one that we found.
			Check.assumeNull(componentBOMLine, "Only one main component shall be found for variant group {0}: {1}, {2}", variantGroup, componentBOMLine, ppOrderBOMLine);
			componentBOMLine = ppOrderBOMLine;
		}

		// Make sure we found a main component line
		Check.assumeNotNull(componentBOMLine, "No main component line found for variant group: {0}", variantGroup);

		// Return the main component line
		return componentBOMLine;
	}
}