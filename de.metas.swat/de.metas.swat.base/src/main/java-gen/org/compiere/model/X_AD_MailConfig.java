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
package org.compiere.model;

/*
 * #%L
 * de.metas.swat.base
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

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.KeyNamePair;

/** Generated Model for AD_MailConfig
 *  @author Adempiere (generated) 
 *  @version Release 3.5.4a - $Id$ */
public class X_AD_MailConfig extends PO implements I_AD_MailConfig, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20110117L;

    /** Standard Constructor */
    public X_AD_MailConfig (Properties ctx, int AD_MailConfig_ID, String trxName)
    {
      super (ctx, AD_MailConfig_ID, trxName);
      /** if (AD_MailConfig_ID == 0)
        {
			setAD_MailBox_ID (0);
			setAD_MailConfig_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_MailConfig (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_AD_MailConfig[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_MailBox getAD_MailBox() throws RuntimeException
    {
		return (I_AD_MailBox)MTable.get(getCtx(), I_AD_MailBox.Table_Name)
			.getPO(getAD_MailBox_ID(), get_TrxName());	}

	/** Set Mail Box.
		@param AD_MailBox_ID Mail Box	  */
	public void setAD_MailBox_ID (int AD_MailBox_ID)
	{
		if (AD_MailBox_ID < 1) 
			set_Value (COLUMNNAME_AD_MailBox_ID, null);
		else 
			set_Value (COLUMNNAME_AD_MailBox_ID, Integer.valueOf(AD_MailBox_ID));
	}

	/** Get Mail Box.
		@return Mail Box	  */
	public int getAD_MailBox_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_MailBox_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Mail Configuration.
		@param AD_MailConfig_ID Mail Configuration	  */
	public void setAD_MailConfig_ID (int AD_MailConfig_ID)
	{
		if (AD_MailConfig_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_MailConfig_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_MailConfig_ID, Integer.valueOf(AD_MailConfig_ID));
	}

	/** Get Mail Configuration.
		@return Mail Configuration	  */
	public int getAD_MailConfig_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_MailConfig_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_Process getAD_Process() throws RuntimeException
    {
		return (org.compiere.model.I_AD_Process)MTable.get(getCtx(), org.compiere.model.I_AD_Process.Table_Name)
			.getPO(getAD_Process_ID(), get_TrxName());	}

	/** Set Prozess.
		@param AD_Process_ID 
		Prozess oder Bericht
	  */
	public void setAD_Process_ID (int AD_Process_ID)
	{
		if (AD_Process_ID < 1) 
			set_Value (COLUMNNAME_AD_Process_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Process_ID, Integer.valueOf(AD_Process_ID));
	}

	/** Get Prozess.
		@return Prozess oder Bericht
	  */
	public int getAD_Process_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Process_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** CustomType AD_Reference_ID=540142 */
	public static final int CUSTOMTYPE_AD_Reference_ID=540142;
	/** org.compiere.util.Login = L */
	public static final String CUSTOMTYPE_OrgCompiereUtilLogin = "L";
	/** Set Custom Type.
		@param CustomType Custom Type	  */
	public void setCustomType (String CustomType)
	{

		set_Value (COLUMNNAME_CustomType, CustomType);
	}

	/** Get Custom Type.
		@return Custom Type	  */
	public String getCustomType () 
	{
		return (String)get_Value(COLUMNNAME_CustomType);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getCustomType()));
    }
}