/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package de.metas.flatrate.model;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_Flatrate_Data
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_C_Flatrate_Data extends org.compiere.model.PO implements I_C_Flatrate_Data, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 538342001L;

    /** Standard Constructor */
    public X_C_Flatrate_Data (Properties ctx, int C_Flatrate_Data_ID, String trxName)
    {
      super (ctx, C_Flatrate_Data_ID, trxName);
      /** if (C_Flatrate_Data_ID == 0)
        {
			setC_BPartner_ID (0);
			setC_Flatrate_Data_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Load Constructor */
    public X_C_Flatrate_Data (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }


    /** Load Meta Data */
    @Override
    protected org.compiere.model.POInfo initPO (Properties ctx)
    {
      org.compiere.model.POInfo poi = org.compiere.model.POInfo.getPOInfo (ctx, Table_Name, get_TrxName());
      return poi;
    }

    @Override
    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_C_Flatrate_Data[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	@Override
	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_C_BPartner_ID, org.compiere.model.I_C_BPartner.class);
	}

	@Override
	public void setC_BPartner(org.compiere.model.I_C_BPartner C_BPartner)
	{
		set_ValueFromPO(COLUMNNAME_C_BPartner_ID, org.compiere.model.I_C_BPartner.class, C_BPartner);
	}

	/** Set Geschäftspartner.
		@param C_BPartner_ID 
		Bezeichnet einen Geschäftspartner
	  */
	@Override
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Geschäftspartner.
		@return Bezeichnet einen Geschäftspartner
	  */
	@Override
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_Flatrate_DataEntry_IncludedTab.
		@param C_Flatrate_DataEntry_IncludedT C_Flatrate_DataEntry_IncludedTab	  */
	@Override
	public void setC_Flatrate_DataEntry_IncludedT (java.lang.String C_Flatrate_DataEntry_IncludedT)
	{
		set_Value (COLUMNNAME_C_Flatrate_DataEntry_IncludedT, C_Flatrate_DataEntry_IncludedT);
	}

	/** Get C_Flatrate_DataEntry_IncludedTab.
		@return C_Flatrate_DataEntry_IncludedTab	  */
	@Override
	public java.lang.String getC_Flatrate_DataEntry_IncludedT () 
	{
		return (java.lang.String)get_Value(COLUMNNAME_C_Flatrate_DataEntry_IncludedT);
	}

	/** Set Datenerfassung.
		@param C_Flatrate_Data_ID Datenerfassung	  */
	@Override
	public void setC_Flatrate_Data_ID (int C_Flatrate_Data_ID)
	{
		if (C_Flatrate_Data_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_Flatrate_Data_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Flatrate_Data_ID, Integer.valueOf(C_Flatrate_Data_ID));
	}

	/** Get Datenerfassung.
		@return Datenerfassung	  */
	@Override
	public int getC_Flatrate_Data_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Flatrate_Data_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Verarbeitet.
		@param Processed 
		Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	@Override
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Verarbeitet.
		@return Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	@Override
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}
}