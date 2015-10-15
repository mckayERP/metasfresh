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
package de.metas.flatrate.model;


/** Generated Interface for C_Contract_Term_Alloc
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public interface I_C_Contract_Term_Alloc 
{

    /** TableName=C_Contract_Term_Alloc */
    public static final String Table_Name = "C_Contract_Term_Alloc";

    /** AD_Table_ID=540419 */
//    public static final int Table_ID = org.compiere.model.MTable.getTable_ID(Table_Name);

//    org.compiere.util.KeyNamePair Model = new org.compiere.util.KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
//    java.math.BigDecimal accessLevel = java.math.BigDecimal.valueOf(3);

    /** Load Meta Data */

	/** Get Mandant.
	  * Mandant für diese Installation.
	  */
	public int getAD_Client_ID();

	public org.compiere.model.I_AD_Client getAD_Client() throws RuntimeException;

    /** Column definition for AD_Client_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_Client> COLUMN_AD_Client_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_Client>(I_C_Contract_Term_Alloc.class, "AD_Client_ID", org.compiere.model.I_AD_Client.class);
    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Set Sektion.
	  * Organisatorische Einheit des Mandanten
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Sektion.
	  * Organisatorische Einheit des Mandanten
	  */
	public int getAD_Org_ID();

	public org.compiere.model.I_AD_Org getAD_Org() throws RuntimeException;

	public void setAD_Org(org.compiere.model.I_AD_Org AD_Org);

    /** Column definition for AD_Org_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_Org> COLUMN_AD_Org_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_Org>(I_C_Contract_Term_Alloc.class, "AD_Org_ID", org.compiere.model.I_AD_Org.class);
    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Prozess-Instanz.
	  * Instanz eines Prozesses
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID);

	/** Get Prozess-Instanz.
	  * Instanz eines Prozesses
	  */
	public int getAD_PInstance_ID();

	public org.compiere.model.I_AD_PInstance getAD_PInstance() throws RuntimeException;

	public void setAD_PInstance(org.compiere.model.I_AD_PInstance AD_PInstance);

    /** Column definition for AD_PInstance_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_PInstance> COLUMN_AD_PInstance_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_PInstance>(I_C_Contract_Term_Alloc.class, "AD_PInstance_ID", org.compiere.model.I_AD_PInstance.class);
    /** Column name AD_PInstance_ID */
    public static final String COLUMNNAME_AD_PInstance_ID = "AD_PInstance_ID";

	/** Set Auftragskandidat - Laufender Vertrag	  */
	public void setC_Contract_Term_Alloc_ID (int C_Contract_Term_Alloc_ID);

	/** Get Auftragskandidat - Laufender Vertrag	  */
	public int getC_Contract_Term_Alloc_ID();

    /** Column definition for C_Contract_Term_Alloc_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object> COLUMN_C_Contract_Term_Alloc_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object>(I_C_Contract_Term_Alloc.class, "C_Contract_Term_Alloc_ID", null);
    /** Column name C_Contract_Term_Alloc_ID */
    public static final String COLUMNNAME_C_Contract_Term_Alloc_ID = "C_Contract_Term_Alloc_ID";

	/** Set Pauschale - Vertragsperiode	  */
	public void setC_Flatrate_Term_ID (int C_Flatrate_Term_ID);

	/** Get Pauschale - Vertragsperiode	  */
	public int getC_Flatrate_Term_ID();

	public de.metas.flatrate.model.I_C_Flatrate_Term getC_Flatrate_Term() throws RuntimeException;

	public void setC_Flatrate_Term(de.metas.flatrate.model.I_C_Flatrate_Term C_Flatrate_Term);

    /** Column definition for C_Flatrate_Term_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, de.metas.flatrate.model.I_C_Flatrate_Term> COLUMN_C_Flatrate_Term_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, de.metas.flatrate.model.I_C_Flatrate_Term>(I_C_Contract_Term_Alloc.class, "C_Flatrate_Term_ID", de.metas.flatrate.model.I_C_Flatrate_Term.class);
    /** Column name C_Flatrate_Term_ID */
    public static final String COLUMNNAME_C_Flatrate_Term_ID = "C_Flatrate_Term_ID";

	/** Set Auftragskandidat	  */
	public void setC_OLCand_ID (int C_OLCand_ID);

	/** Get Auftragskandidat	  */
	public int getC_OLCand_ID();

	public de.metas.ordercandidate.model.I_C_OLCand getC_OLCand() throws RuntimeException;

	public void setC_OLCand(de.metas.ordercandidate.model.I_C_OLCand C_OLCand);

    /** Column definition for C_OLCand_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, de.metas.ordercandidate.model.I_C_OLCand> COLUMN_C_OLCand_ID = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, de.metas.ordercandidate.model.I_C_OLCand>(I_C_Contract_Term_Alloc.class, "C_OLCand_ID", de.metas.ordercandidate.model.I_C_OLCand.class);
    /** Column name C_OLCand_ID */
    public static final String COLUMNNAME_C_OLCand_ID = "C_OLCand_ID";

	/** Get Erstellt.
	  * Datum, an dem dieser Eintrag erstellt wurde
	  */
	public java.sql.Timestamp getCreated();

    /** Column definition for Created */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object> COLUMN_Created = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object>(I_C_Contract_Term_Alloc.class, "Created", null);
    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Erstellt durch.
	  * Nutzer, der diesen Eintrag erstellt hat
	  */
	public int getCreatedBy();

    /** Column definition for CreatedBy */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_User> COLUMN_CreatedBy = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_User>(I_C_Contract_Term_Alloc.class, "CreatedBy", org.compiere.model.I_AD_User.class);
    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Set Belegstatus.
	  * The current status of the document
	  */
	public void setDocStatus (java.lang.String DocStatus);

	/** Get Belegstatus.
	  * The current status of the document
	  */
	public java.lang.String getDocStatus();

    /** Column definition for DocStatus */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object> COLUMN_DocStatus = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object>(I_C_Contract_Term_Alloc.class, "DocStatus", null);
    /** Column name DocStatus */
    public static final String COLUMNNAME_DocStatus = "DocStatus";

	/** Set Aktiv.
	  * Der Eintrag ist im System aktiv
	  */
	public void setIsActive (boolean IsActive);

	/** Get Aktiv.
	  * Der Eintrag ist im System aktiv
	  */
	public boolean isActive();

    /** Column definition for IsActive */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object> COLUMN_IsActive = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object>(I_C_Contract_Term_Alloc.class, "IsActive", null);
    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Get Aktualisiert.
	  * Datum, an dem dieser Eintrag aktualisiert wurde
	  */
	public java.sql.Timestamp getUpdated();

    /** Column definition for Updated */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object> COLUMN_Updated = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, Object>(I_C_Contract_Term_Alloc.class, "Updated", null);
    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Aktualisiert durch.
	  * Nutzer, der diesen Eintrag aktualisiert hat
	  */
	public int getUpdatedBy();

    /** Column definition for UpdatedBy */
    public static final org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_User> COLUMN_UpdatedBy = new org.adempiere.model.ModelColumn<I_C_Contract_Term_Alloc, org.compiere.model.I_AD_User>(I_C_Contract_Term_Alloc.class, "UpdatedBy", org.compiere.model.I_AD_User.class);
    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";
}