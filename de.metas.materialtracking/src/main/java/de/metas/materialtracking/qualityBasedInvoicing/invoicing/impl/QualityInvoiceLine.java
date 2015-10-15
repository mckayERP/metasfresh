package de.metas.materialtracking.qualityBasedInvoicing.invoicing.impl;

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


import java.math.BigDecimal;

import org.adempiere.pricing.api.IPricingResult;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;

import de.metas.materialtracking.IHandlingUnitsInfo;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.IQualityInvoiceLine;

public class QualityInvoiceLine implements IQualityInvoiceLine
{
	private I_M_Product product;
	private String productName;
	private BigDecimal percentage;
	private BigDecimal qty;
	private I_C_UOM uom;
	private IPricingResult pricingResult;
	private boolean displayed = true;
	private String description = null;
	private IHandlingUnitsInfo handlingUnitsInfo;

	@Override
	public String toString()
	{
		final BigDecimal price = pricingResult == null ? null : pricingResult.getPriceStd();
		return "QualityInvoiceLine ["
		+ "product=" + (product == null ? null : product.getName())
		+ ", productName=" + productName
		+ ", percentage=" + percentage
		+ ", qty=" + qty
		+ ", uom=" + (uom == null ? null : uom.getUOMSymbol())
		+ ", price=" + price
		+ ", displayed=" + displayed
		+ ", description=" + description
		+ ", handlingUnitsInfo=" + handlingUnitsInfo
		+ "]";
	}

	@Override
	public I_M_Product getM_Product()
	{
		return product;
	}

	public void setM_Product(final I_M_Product product)
	{
		this.product = product;
	}

	@Override
	public String getProductName()
	{
		return productName;
	}

	public void setProductName(final String productName)
	{
		this.productName = productName;
	}

	@Override
	public BigDecimal getPercentage()
	{
		return percentage;
	}

	public void setPercentage(final BigDecimal percentage)
	{
		this.percentage = percentage;
	}

	@Override
	public BigDecimal getQty()
	{
		return qty;
	}

	public void setQty(final BigDecimal qty)
	{
		this.qty = qty;
	}

	@Override
	public I_C_UOM getC_UOM()
	{
		return uom;
	}

	public void setC_UOM(final I_C_UOM uom)
	{
		this.uom = uom;
	}

	@Override
	public IPricingResult getPrice()
	{
		return pricingResult;
	}

	public void setPrice(final IPricingResult price)
	{
		pricingResult = price;
	}

	@Override
	public boolean isDisplayed()
	{
		return displayed;
	}

	public void setDisplayed(final boolean displayed)
	{
		this.displayed = displayed;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(final String description)
	{
		this.description = description;
	}

	@Override
	public IHandlingUnitsInfo getHandlingUnitsInfo()
	{
		return handlingUnitsInfo;
	}

	public void setHandlingUnitsInfo(final IHandlingUnitsInfo handlingUnitsInfo)
	{
		this.handlingUnitsInfo = handlingUnitsInfo;
	}
}