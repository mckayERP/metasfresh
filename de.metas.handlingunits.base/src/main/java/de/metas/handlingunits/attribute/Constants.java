package de.metas.handlingunits.attribute;

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

import org.compiere.model.I_M_Attribute;

/**
 * HU Attributes constants
 * 
 * @author tsa
 *
 */
public final class Constants
{
	/**
	 * Context name used to identify the map of initial default values to be used when creating HU attributes.
	 *
	 * Type: Map of {@link I_M_Attribute} to value({@link Object}).
	 */
	public static String CTXATTR_DefaultAttributesValue = Constants.class.getName() + "#DefaultAttributesValue";

	public static final String ATTR_QualityDiscountPercent_Value = "QualityDiscountPercent";
	public static final String ATTR_QualityNotice_Value = "QualityNotice";
	public static final String ATTR_SSCC18_Value = "SSCC18";
	public static final String ATTR_SubProducerBPartner_Value = "SubProducerBPartner";

	/**
	 * @see http://dewiki908/mediawiki/index.php/07759_Stockvalue_by_FiFo_%28100951729256%29
	 */
	public static final String ATTR_CostPrice = "HU_CostPrice";

	/**
	 * HU's attribute which stores the best before date.
	 * 
	 * @see http://dewiki908/mediawiki/index.php/09363_Best-before_management_%28Mindesthaltbarkeit%29_%28108375354495%29
	 */
	public static final String ATTR_BestBeforeDate = "HU_BestBeforeDate";

	private Constants()
	{
		super();
	};
}