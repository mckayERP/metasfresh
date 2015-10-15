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


/** Generated Interface for C_Flatrate_Term
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public interface I_C_Flatrate_Term 
{

    /** TableName=C_Flatrate_Term */
    public static final String Table_Name = "C_Flatrate_Term";

    /** AD_Table_ID=540320 */
//    public static final int Table_ID = org.compiere.model.MTable.getTable_ID(Table_Name);

//    org.compiere.util.KeyNamePair Model = new org.compiere.util.KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
//    java.math.BigDecimal accessLevel = java.math.BigDecimal.valueOf(1);

    /** Load Meta Data */

	/** Get Mandant.
	  * Mandant für diese Installation.
	  */
	public int getAD_Client_ID();

	public org.compiere.model.I_AD_Client getAD_Client() throws RuntimeException;

    /** Column definition for AD_Client_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_Client> COLUMN_AD_Client_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_Client>(I_C_Flatrate_Term.class, "AD_Client_ID", org.compiere.model.I_AD_Client.class);
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
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_Org> COLUMN_AD_Org_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_Org>(I_C_Flatrate_Term.class, "AD_Org_ID", org.compiere.model.I_AD_Org.class);
    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Verarbeitung zum Laufzeitende	  */
	public void setAD_PInstance_EndOfTerm_ID (int AD_PInstance_EndOfTerm_ID);

	/** Get Verarbeitung zum Laufzeitende	  */
	public int getAD_PInstance_EndOfTerm_ID();

	public org.compiere.model.I_AD_PInstance getAD_PInstance_EndOfTerm() throws RuntimeException;

	public void setAD_PInstance_EndOfTerm(org.compiere.model.I_AD_PInstance AD_PInstance_EndOfTerm);

    /** Column definition for AD_PInstance_EndOfTerm_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_PInstance> COLUMN_AD_PInstance_EndOfTerm_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_PInstance>(I_C_Flatrate_Term.class, "AD_PInstance_EndOfTerm_ID", org.compiere.model.I_AD_PInstance.class);
    /** Column name AD_PInstance_EndOfTerm_ID */
    public static final String COLUMNNAME_AD_PInstance_EndOfTerm_ID = "AD_PInstance_EndOfTerm_ID";

	/** Set Betreuer.
	  * Person, die bei einem fachlichen Problem vom System informiert wird.
	  */
	public void setAD_User_InCharge_ID (int AD_User_InCharge_ID);

	/** Get Betreuer.
	  * Person, die bei einem fachlichen Problem vom System informiert wird.
	  */
	public int getAD_User_InCharge_ID();

	public org.compiere.model.I_AD_User getAD_User_InCharge() throws RuntimeException;

	public void setAD_User_InCharge(org.compiere.model.I_AD_User AD_User_InCharge);

    /** Column definition for AD_User_InCharge_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User> COLUMN_AD_User_InCharge_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User>(I_C_Flatrate_Term.class, "AD_User_InCharge_ID", org.compiere.model.I_AD_User.class);
    /** Column name AD_User_InCharge_ID */
    public static final String COLUMNNAME_AD_User_InCharge_ID = "AD_User_InCharge_ID";

	/** Set Rechnungspartner.
	  * Geschäftspartners für die Rechnungsstellung
	  */
	public void setBill_BPartner_ID (int Bill_BPartner_ID);

	/** Get Rechnungspartner.
	  * Geschäftspartners für die Rechnungsstellung
	  */
	public int getBill_BPartner_ID();

	public org.compiere.model.I_C_BPartner getBill_BPartner() throws RuntimeException;

	public void setBill_BPartner(org.compiere.model.I_C_BPartner Bill_BPartner);

    /** Column definition for Bill_BPartner_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner> COLUMN_Bill_BPartner_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner>(I_C_Flatrate_Term.class, "Bill_BPartner_ID", org.compiere.model.I_C_BPartner.class);
    /** Column name Bill_BPartner_ID */
    public static final String COLUMNNAME_Bill_BPartner_ID = "Bill_BPartner_ID";

	/** Set Rechnungsstandort.
	  * Standort des Geschäftspartners für die Rechnungsstellung
	  */
	public void setBill_Location_ID (int Bill_Location_ID);

	/** Get Rechnungsstandort.
	  * Standort des Geschäftspartners für die Rechnungsstellung
	  */
	public int getBill_Location_ID();

	public org.compiere.model.I_C_BPartner_Location getBill_Location() throws RuntimeException;

	public void setBill_Location(org.compiere.model.I_C_BPartner_Location Bill_Location);

    /** Column definition for Bill_Location_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner_Location> COLUMN_Bill_Location_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner_Location>(I_C_Flatrate_Term.class, "Bill_Location_ID", org.compiere.model.I_C_BPartner_Location.class);
    /** Column name Bill_Location_ID */
    public static final String COLUMNNAME_Bill_Location_ID = "Bill_Location_ID";

	/** Set Rechnungskontakt.
	  * Ansprechpartner des Geschäftspartners für die Rechnungsstellung
	  */
	public void setBill_User_ID (int Bill_User_ID);

	/** Get Rechnungskontakt.
	  * Ansprechpartner des Geschäftspartners für die Rechnungsstellung
	  */
	public int getBill_User_ID();

	public org.compiere.model.I_AD_User getBill_User() throws RuntimeException;

	public void setBill_User(org.compiere.model.I_AD_User Bill_User);

    /** Column definition for Bill_User_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User> COLUMN_Bill_User_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User>(I_C_Flatrate_Term.class, "Bill_User_ID", org.compiere.model.I_AD_User.class);
    /** Column name Bill_User_ID */
    public static final String COLUMNNAME_Bill_User_ID = "Bill_User_ID";

	/** Set Währung.
	  * Die Währung für diesen Eintrag
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Währung.
	  * Die Währung für diesen Eintrag
	  */
	public int getC_Currency_ID();

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException;

	public void setC_Currency(org.compiere.model.I_C_Currency C_Currency);

    /** Column definition for C_Currency_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Currency> COLUMN_C_Currency_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Currency>(I_C_Flatrate_Term.class, "C_Currency_ID", org.compiere.model.I_C_Currency.class);
    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Vertragsbedingungen	  */
	public void setC_Flatrate_Conditions_ID (int C_Flatrate_Conditions_ID);

	/** Get Vertragsbedingungen	  */
	public int getC_Flatrate_Conditions_ID();

	public de.metas.flatrate.model.I_C_Flatrate_Conditions getC_Flatrate_Conditions() throws RuntimeException;

	public void setC_Flatrate_Conditions(de.metas.flatrate.model.I_C_Flatrate_Conditions C_Flatrate_Conditions);

    /** Column definition for C_Flatrate_Conditions_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Conditions> COLUMN_C_Flatrate_Conditions_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Conditions>(I_C_Flatrate_Term.class, "C_Flatrate_Conditions_ID", de.metas.flatrate.model.I_C_Flatrate_Conditions.class);
    /** Column name C_Flatrate_Conditions_ID */
    public static final String COLUMNNAME_C_Flatrate_Conditions_ID = "C_Flatrate_Conditions_ID";

	/** Set Datenerfassung	  */
	public void setC_Flatrate_Data_ID (int C_Flatrate_Data_ID);

	/** Get Datenerfassung	  */
	public int getC_Flatrate_Data_ID();

	public de.metas.flatrate.model.I_C_Flatrate_Data getC_Flatrate_Data() throws RuntimeException;

	public void setC_Flatrate_Data(de.metas.flatrate.model.I_C_Flatrate_Data C_Flatrate_Data);

    /** Column definition for C_Flatrate_Data_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Data> COLUMN_C_Flatrate_Data_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Data>(I_C_Flatrate_Term.class, "C_Flatrate_Data_ID", de.metas.flatrate.model.I_C_Flatrate_Data.class);
    /** Column name C_Flatrate_Data_ID */
    public static final String COLUMNNAME_C_Flatrate_Data_ID = "C_Flatrate_Data_ID";

	/** Set Pauschale - Vertragsperiode	  */
	public void setC_Flatrate_Term_ID (int C_Flatrate_Term_ID);

	/** Get Pauschale - Vertragsperiode	  */
	public int getC_Flatrate_Term_ID();

    /** Column definition for C_Flatrate_Term_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_C_Flatrate_Term_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "C_Flatrate_Term_ID", null);
    /** Column name C_Flatrate_Term_ID */
    public static final String COLUMNNAME_C_Flatrate_Term_ID = "C_Flatrate_Term_ID";

	/** Set Nachfolgende Vertragsperiode	  */
	public void setC_FlatrateTerm_Next_ID (int C_FlatrateTerm_Next_ID);

	/** Get Nachfolgende Vertragsperiode	  */
	public int getC_FlatrateTerm_Next_ID();

	public de.metas.flatrate.model.I_C_Flatrate_Term getC_FlatrateTerm_Next() throws RuntimeException;

	public void setC_FlatrateTerm_Next(de.metas.flatrate.model.I_C_Flatrate_Term C_FlatrateTerm_Next);

    /** Column definition for C_FlatrateTerm_Next_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Term> COLUMN_C_FlatrateTerm_Next_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Term>(I_C_Flatrate_Term.class, "C_FlatrateTerm_Next_ID", de.metas.flatrate.model.I_C_Flatrate_Term.class);
    /** Column name C_FlatrateTerm_Next_ID */
    public static final String COLUMNNAME_C_FlatrateTerm_Next_ID = "C_FlatrateTerm_Next_ID";

	/** Set Vertragsverlängerung/-übergang.
	  * Regelt z.B. die Vertragslaufzeit, Kündigungsfristen, autmatische Verlängerung usw.
	  */
	public void setC_Flatrate_Transition_ID (int C_Flatrate_Transition_ID);

	/** Get Vertragsverlängerung/-übergang.
	  * Regelt z.B. die Vertragslaufzeit, Kündigungsfristen, autmatische Verlängerung usw.
	  */
	public int getC_Flatrate_Transition_ID();

	public de.metas.flatrate.model.I_C_Flatrate_Transition getC_Flatrate_Transition() throws RuntimeException;

	public void setC_Flatrate_Transition(de.metas.flatrate.model.I_C_Flatrate_Transition C_Flatrate_Transition);

    /** Column definition for C_Flatrate_Transition_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Transition> COLUMN_C_Flatrate_Transition_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, de.metas.flatrate.model.I_C_Flatrate_Transition>(I_C_Flatrate_Term.class, "C_Flatrate_Transition_ID", de.metas.flatrate.model.I_C_Flatrate_Transition.class);
    /** Column name C_Flatrate_Transition_ID */
    public static final String COLUMNNAME_C_Flatrate_Transition_ID = "C_Flatrate_Transition_ID";

	/** Set Ändern oder Kündigen	  */
	public void setChangeOrCancelTerm (java.lang.String ChangeOrCancelTerm);

	/** Get Ändern oder Kündigen	  */
	public java.lang.String getChangeOrCancelTerm();

    /** Column definition for ChangeOrCancelTerm */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_ChangeOrCancelTerm = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "ChangeOrCancelTerm", null);
    /** Column name ChangeOrCancelTerm */
    public static final String COLUMNNAME_ChangeOrCancelTerm = "ChangeOrCancelTerm";

	/** Set Vertrags-Status	  */
	public void setContractStatus (java.lang.String ContractStatus);

	/** Get Vertrags-Status	  */
	public java.lang.String getContractStatus();

    /** Column definition for ContractStatus */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_ContractStatus = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "ContractStatus", null);
    /** Column name ContractStatus */
    public static final String COLUMNNAME_ContractStatus = "ContractStatus";

	/** Set Änderungs-Auftragszeile.
	  * Auftragszeile, mit der der Vertrag vor dem regulären Ende gekündigt oder umgewandelt wurde
	  */
	public void setC_OrderLine_TermChange_ID (int C_OrderLine_TermChange_ID);

	/** Get Änderungs-Auftragszeile.
	  * Auftragszeile, mit der der Vertrag vor dem regulären Ende gekündigt oder umgewandelt wurde
	  */
	public int getC_OrderLine_TermChange_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine_TermChange() throws RuntimeException;

	public void setC_OrderLine_TermChange(org.compiere.model.I_C_OrderLine C_OrderLine_TermChange);

    /** Column definition for C_OrderLine_TermChange_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_OrderLine> COLUMN_C_OrderLine_TermChange_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_OrderLine>(I_C_Flatrate_Term.class, "C_OrderLine_TermChange_ID", org.compiere.model.I_C_OrderLine.class);
    /** Column name C_OrderLine_TermChange_ID */
    public static final String COLUMNNAME_C_OrderLine_TermChange_ID = "C_OrderLine_TermChange_ID";

	/** Set Vertrags-Auftragszeile.
	  * Auftragszeile, mit der der Vertrag abgeschlossen wurde
	  */
	public void setC_OrderLine_Term_ID (int C_OrderLine_Term_ID);

	/** Get Vertrags-Auftragszeile.
	  * Auftragszeile, mit der der Vertrag abgeschlossen wurde
	  */
	public int getC_OrderLine_Term_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine_Term() throws RuntimeException;

	public void setC_OrderLine_Term(org.compiere.model.I_C_OrderLine C_OrderLine_Term);

    /** Column definition for C_OrderLine_Term_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_OrderLine> COLUMN_C_OrderLine_Term_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_OrderLine>(I_C_Flatrate_Term.class, "C_OrderLine_Term_ID", org.compiere.model.I_C_OrderLine.class);
    /** Column name C_OrderLine_Term_ID */
    public static final String COLUMNNAME_C_OrderLine_Term_ID = "C_OrderLine_Term_ID";

	/** Set Änderungs-Auftrag.
	  * Auftrag, mit der der Vertrag vor dem regulären Ende gekündigt oder umgewandelt wurde
	  */
	public void setC_Order_TermChange_ID (int C_Order_TermChange_ID);

	/** Get Änderungs-Auftrag.
	  * Auftrag, mit der der Vertrag vor dem regulären Ende gekündigt oder umgewandelt wurde
	  */
	public int getC_Order_TermChange_ID();

	public org.compiere.model.I_C_Order getC_Order_TermChange() throws RuntimeException;

	public void setC_Order_TermChange(org.compiere.model.I_C_Order C_Order_TermChange);

    /** Column definition for C_Order_TermChange_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Order> COLUMN_C_Order_TermChange_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Order>(I_C_Flatrate_Term.class, "C_Order_TermChange_ID", org.compiere.model.I_C_Order.class);
    /** Column name C_Order_TermChange_ID */
    public static final String COLUMNNAME_C_Order_TermChange_ID = "C_Order_TermChange_ID";

	/** Set Vertrags-Auftrag.
	  * Auftrag, mit der der Vertrag abgeschlossen wurde
	  */
	public void setC_Order_Term_ID (int C_Order_Term_ID);

	/** Get Vertrags-Auftrag.
	  * Auftrag, mit der der Vertrag abgeschlossen wurde
	  */
	public int getC_Order_Term_ID();

	public org.compiere.model.I_C_Order getC_Order_Term() throws RuntimeException;

	public void setC_Order_Term(org.compiere.model.I_C_Order C_Order_Term);

    /** Column definition for C_Order_Term_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Order> COLUMN_C_Order_Term_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_Order>(I_C_Flatrate_Term.class, "C_Order_Term_ID", org.compiere.model.I_C_Order.class);
    /** Column name C_Order_Term_ID */
    public static final String COLUMNNAME_C_Order_Term_ID = "C_Order_Term_ID";

	/** Get Erstellt.
	  * Datum, an dem dieser Eintrag erstellt wurde
	  */
	public java.sql.Timestamp getCreated();

    /** Column definition for Created */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Created = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Created", null);
    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Erstellt durch.
	  * Nutzer, der diesen Eintrag erstellt hat
	  */
	public int getCreatedBy();

    /** Column definition for CreatedBy */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User> COLUMN_CreatedBy = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User>(I_C_Flatrate_Term.class, "CreatedBy", org.compiere.model.I_AD_User.class);
    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Set Maßeinheit.
	  * Maßeinheit
	  */
	public void setC_UOM_ID (int C_UOM_ID);

	/** Get Maßeinheit.
	  * Maßeinheit
	  */
	public int getC_UOM_ID();

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException;

	public void setC_UOM(org.compiere.model.I_C_UOM C_UOM);

    /** Column definition for C_UOM_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_UOM> COLUMN_C_UOM_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_UOM>(I_C_Flatrate_Term.class, "C_UOM_ID", org.compiere.model.I_C_UOM.class);
    /** Column name C_UOM_ID */
    public static final String COLUMNNAME_C_UOM_ID = "C_UOM_ID";

	/** Set Lieferart.
	  * Definiert die zeitliche Steuerung von Lieferungen
	  */
	public void setDeliveryRule (java.lang.String DeliveryRule);

	/** Get Lieferart.
	  * Definiert die zeitliche Steuerung von Lieferungen
	  */
	public java.lang.String getDeliveryRule();

    /** Column definition for DeliveryRule */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_DeliveryRule = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "DeliveryRule", null);
    /** Column name DeliveryRule */
    public static final String COLUMNNAME_DeliveryRule = "DeliveryRule";

	/** Set Lieferung.
	  * Wie der Auftrag geliefert wird
	  */
	public void setDeliveryViaRule (java.lang.String DeliveryViaRule);

	/** Get Lieferung.
	  * Wie der Auftrag geliefert wird
	  */
	public java.lang.String getDeliveryViaRule();

    /** Column definition for DeliveryViaRule */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_DeliveryViaRule = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "DeliveryViaRule", null);
    /** Column name DeliveryViaRule */
    public static final String COLUMNNAME_DeliveryViaRule = "DeliveryViaRule";

	/** Set Belegverarbeitung.
	  * Der zukünftige Status des Belegs
	  */
	public void setDocAction (java.lang.String DocAction);

	/** Get Belegverarbeitung.
	  * Der zukünftige Status des Belegs
	  */
	public java.lang.String getDocAction();

    /** Column definition for DocAction */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_DocAction = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "DocAction", null);
    /** Column name DocAction */
    public static final String COLUMNNAME_DocAction = "DocAction";

	/** Set Belegstatus.
	  * Derzeitiger Status des Belegs
	  */
	public void setDocStatus (java.lang.String DocStatus);

	/** Get Belegstatus.
	  * Derzeitiger Status des Belegs
	  */
	public java.lang.String getDocStatus();

    /** Column definition for DocStatus */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_DocStatus = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "DocStatus", null);
    /** Column name DocStatus */
    public static final String COLUMNNAME_DocStatus = "DocStatus";

	/** Set Streckengeschäft-Kunde.
	  * Business Partner to ship to
	  */
	public void setDropShip_BPartner_ID (int DropShip_BPartner_ID);

	/** Get Streckengeschäft-Kunde.
	  * Business Partner to ship to
	  */
	public int getDropShip_BPartner_ID();

	public org.compiere.model.I_C_BPartner getDropShip_BPartner() throws RuntimeException;

	public void setDropShip_BPartner(org.compiere.model.I_C_BPartner DropShip_BPartner);

    /** Column definition for DropShip_BPartner_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner> COLUMN_DropShip_BPartner_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner>(I_C_Flatrate_Term.class, "DropShip_BPartner_ID", org.compiere.model.I_C_BPartner.class);
    /** Column name DropShip_BPartner_ID */
    public static final String COLUMNNAME_DropShip_BPartner_ID = "DropShip_BPartner_ID";

	/** Set Streckengeschäft-Ort.
	  * Business Partner Location for shipping to
	  */
	public void setDropShip_Location_ID (int DropShip_Location_ID);

	/** Get Streckengeschäft-Ort.
	  * Business Partner Location for shipping to
	  */
	public int getDropShip_Location_ID();

	public org.compiere.model.I_C_BPartner_Location getDropShip_Location() throws RuntimeException;

	public void setDropShip_Location(org.compiere.model.I_C_BPartner_Location DropShip_Location);

    /** Column definition for DropShip_Location_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner_Location> COLUMN_DropShip_Location_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_C_BPartner_Location>(I_C_Flatrate_Term.class, "DropShip_Location_ID", org.compiere.model.I_C_BPartner_Location.class);
    /** Column name DropShip_Location_ID */
    public static final String COLUMNNAME_DropShip_Location_ID = "DropShip_Location_ID";

	/** Set Streckengeschäft-Ansprechpartner.
	  * Business Partner Contact for drop shipment
	  */
	public void setDropShip_User_ID (int DropShip_User_ID);

	/** Get Streckengeschäft-Ansprechpartner.
	  * Business Partner Contact for drop shipment
	  */
	public int getDropShip_User_ID();

	public org.compiere.model.I_AD_User getDropShip_User() throws RuntimeException;

	public void setDropShip_User(org.compiere.model.I_AD_User DropShip_User);

    /** Column definition for DropShip_User_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User> COLUMN_DropShip_User_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User>(I_C_Flatrate_Term.class, "DropShip_User_ID", org.compiere.model.I_AD_User.class);
    /** Column name DropShip_User_ID */
    public static final String COLUMNNAME_DropShip_User_ID = "DropShip_User_ID";

	/** Set Enddatum.
	  * Last effective date (inclusive)
	  */
	public void setEndDate (java.sql.Timestamp EndDate);

	/** Get Enddatum.
	  * Last effective date (inclusive)
	  */
	public java.sql.Timestamp getEndDate();

    /** Column definition for EndDate */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_EndDate = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "EndDate", null);
    /** Column name EndDate */
    public static final String COLUMNNAME_EndDate = "EndDate";

	/** Set Vertrag jetzt verlängern	  */
	public void setExtendTerm (java.lang.String ExtendTerm);

	/** Get Vertrag jetzt verlängern	  */
	public java.lang.String getExtendTerm();

    /** Column definition for ExtendTerm */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_ExtendTerm = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "ExtendTerm", null);
    /** Column name ExtendTerm */
    public static final String COLUMNNAME_ExtendTerm = "ExtendTerm";

	/** Set Aktiv.
	  * Der Eintrag ist im System aktiv
	  */
	public void setIsActive (boolean IsActive);

	/** Get Aktiv.
	  * Der Eintrag ist im System aktiv
	  */
	public boolean isActive();

    /** Column definition for IsActive */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsActive = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsActive", null);
    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Vertrag autom. verlängern.
	  * Wenn dieser Haken gesetzt ist, werden laufende Verträge automatisch verlängert
	  */
	public void setIsAutoRenew (boolean IsAutoRenew);

	/** Get Vertrag autom. verlängern.
	  * Wenn dieser Haken gesetzt ist, werden laufende Verträge automatisch verlängert
	  */
	public boolean isAutoRenew();

    /** Column definition for IsAutoRenew */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsAutoRenew = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsAutoRenew", null);
    /** Column name IsAutoRenew */
    public static final String COLUMNNAME_IsAutoRenew = "IsAutoRenew";

	/** Set Gegenüberstellung mit erbr. Leist..
	  * Legt fest, ob die pauschal abgerechenten Beträge den tatsächlich erbrachten Leistungen gegenüber gestellt werden sollen
	  */
	public void setIsClosingWithActualSum (boolean IsClosingWithActualSum);

	/** Get Gegenüberstellung mit erbr. Leist..
	  * Legt fest, ob die pauschal abgerechenten Beträge den tatsächlich erbrachten Leistungen gegenüber gestellt werden sollen
	  */
	public boolean isClosingWithActualSum();

    /** Column definition for IsClosingWithActualSum */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsClosingWithActualSum = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsClosingWithActualSum", null);
    /** Column name IsClosingWithActualSum */
    public static final String COLUMNNAME_IsClosingWithActualSum = "IsClosingWithActualSum";

	/** Set Abschlusskorrektur vorsehen.
	  * Legt fest, ob innerhalb der Vertragslaufzeit (in der Regel zu deren Ende) noch korrigierte Pauschalen-Mengen erfasst werden können
	  */
	public void setIsClosingWithCorrectionSum (boolean IsClosingWithCorrectionSum);

	/** Get Abschlusskorrektur vorsehen.
	  * Legt fest, ob innerhalb der Vertragslaufzeit (in der Regel zu deren Ende) noch korrigierte Pauschalen-Mengen erfasst werden können
	  */
	public boolean isClosingWithCorrectionSum();

    /** Column definition for IsClosingWithCorrectionSum */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsClosingWithCorrectionSum = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsClosingWithCorrectionSum", null);
    /** Column name IsClosingWithCorrectionSum */
    public static final String COLUMNNAME_IsClosingWithCorrectionSum = "IsClosingWithCorrectionSum";

	/** Set AB bei neuer Vertragslaufzeit.
	  * Entscheidet, ob das System beim Fertigstellen einer neuen Vertragslaufzeit (z.B. bei automatischer Verlängerung) eine Auftragsbestätigung erzeugt.
	  */
	public void setIsNewTermCreatesOrder (boolean IsNewTermCreatesOrder);

	/** Get AB bei neuer Vertragslaufzeit.
	  * Entscheidet, ob das System beim Fertigstellen einer neuen Vertragslaufzeit (z.B. bei automatischer Verlängerung) eine Auftragsbestätigung erzeugt.
	  */
	public boolean isNewTermCreatesOrder();

    /** Column definition for IsNewTermCreatesOrder */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsNewTermCreatesOrder = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsNewTermCreatesOrder", null);
    /** Column name IsNewTermCreatesOrder */
    public static final String COLUMNNAME_IsNewTermCreatesOrder = "IsNewTermCreatesOrder";

	/** Set Portofrei	  */
	public void setIsPostageFree (boolean IsPostageFree);

	/** Get Portofrei	  */
	public boolean isPostageFree();

    /** Column definition for IsPostageFree */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsPostageFree = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsPostageFree", null);
    /** Column name IsPostageFree */
    public static final String COLUMNNAME_IsPostageFree = "IsPostageFree";

	/** Set Planspiel	  */
	public void setIsSimulation (boolean IsSimulation);

	/** Get Planspiel	  */
	public boolean isSimulation();

    /** Column definition for IsSimulation */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_IsSimulation = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "IsSimulation", null);
    /** Column name IsSimulation */
    public static final String COLUMNNAME_IsSimulation = "IsSimulation";

	/** Set Preissystem.
	  * Ein Preissystem enthält beliebig viele, Länder-abhängige Preislisten.
	  */
	public void setM_PricingSystem_ID (int M_PricingSystem_ID);

	/** Get Preissystem.
	  * Ein Preissystem enthält beliebig viele, Länder-abhängige Preislisten.
	  */
	public int getM_PricingSystem_ID();

	public org.compiere.model.I_M_PricingSystem getM_PricingSystem() throws RuntimeException;

	public void setM_PricingSystem(org.compiere.model.I_M_PricingSystem M_PricingSystem);

    /** Column definition for M_PricingSystem_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_M_PricingSystem> COLUMN_M_PricingSystem_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_M_PricingSystem>(I_C_Flatrate_Term.class, "M_PricingSystem_ID", org.compiere.model.I_M_PricingSystem.class);
    /** Column name M_PricingSystem_ID */
    public static final String COLUMNNAME_M_PricingSystem_ID = "M_PricingSystem_ID";

	/** Set Produkt.
	  * Produkt, Leistung, Artikel
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Produkt.
	  * Produkt, Leistung, Artikel
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

	public void setM_Product(org.compiere.model.I_M_Product M_Product);

    /** Column definition for M_Product_ID */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_M_Product> COLUMN_M_Product_ID = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_M_Product>(I_C_Flatrate_Term.class, "M_Product_ID", org.compiere.model.I_M_Product.class);
    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Notiz.
	  * Optional weitere Information für ein Dokument
	  */
	public void setNote (java.lang.String Note);

	/** Get Notiz.
	  * Optional weitere Information für ein Dokument
	  */
	public java.lang.String getNote();

    /** Column definition for Note */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Note = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Note", null);
    /** Column name Note */
    public static final String COLUMNNAME_Note = "Note";

	/** Set Kündigungs/Benachrichtigungsfrist.
	  * Datum vor Ende der Vertragslaufzeit, an dem der laufende Vertrag automatisch verlängert oder aber der Betreuer informiert wird.
	  */
	public void setNoticeDate (java.sql.Timestamp NoticeDate);

	/** Get Kündigungs/Benachrichtigungsfrist.
	  * Datum vor Ende der Vertragslaufzeit, an dem der laufende Vertrag automatisch verlängert oder aber der Betreuer informiert wird.
	  */
	public java.sql.Timestamp getNoticeDate();

    /** Column definition for NoticeDate */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_NoticeDate = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "NoticeDate", null);
    /** Column name NoticeDate */
    public static final String COLUMNNAME_NoticeDate = "NoticeDate";

	/** Set Planmenge pro Maßeinheit.
	  * Geplante Menge der zu erbringenden Leistung (z.B. zu liefernde Teile), pro pauschal abzurechnender Einheit (z.B. Pflegetag).
	  */
	public void setPlannedQtyPerUnit (java.math.BigDecimal PlannedQtyPerUnit);

	/** Get Planmenge pro Maßeinheit.
	  * Geplante Menge der zu erbringenden Leistung (z.B. zu liefernde Teile), pro pauschal abzurechnender Einheit (z.B. Pflegetag).
	  */
	public java.math.BigDecimal getPlannedQtyPerUnit();

    /** Column definition for PlannedQtyPerUnit */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_PlannedQtyPerUnit = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "PlannedQtyPerUnit", null);
    /** Column name PlannedQtyPerUnit */
    public static final String COLUMNNAME_PlannedQtyPerUnit = "PlannedQtyPerUnit";

	/** Set Abschlusskorrektur vorbereiten.
	  * Prozess zum erstellen eines Abrechnungs-Korrektur-Datensatzes und/oder eines Abrechnungs-Verrechnungs-Datensatzes
	  */
	public void setPrepareClosing (java.lang.String PrepareClosing);

	/** Get Abschlusskorrektur vorbereiten.
	  * Prozess zum erstellen eines Abrechnungs-Korrektur-Datensatzes und/oder eines Abrechnungs-Verrechnungs-Datensatzes
	  */
	public java.lang.String getPrepareClosing();

    /** Column definition for PrepareClosing */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_PrepareClosing = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "PrepareClosing", null);
    /** Column name PrepareClosing */
    public static final String COLUMNNAME_PrepareClosing = "PrepareClosing";

	/** Set Einzelpreis.
	  * Effektiver Preis
	  */
	public void setPriceActual (java.math.BigDecimal PriceActual);

	/** Get Einzelpreis.
	  * Effektiver Preis
	  */
	public java.math.BigDecimal getPriceActual();

    /** Column definition for PriceActual */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_PriceActual = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "PriceActual", null);
    /** Column name PriceActual */
    public static final String COLUMNNAME_PriceActual = "PriceActual";

	/** Set Verarbeitet.
	  * Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	public void setProcessed (boolean Processed);

	/** Get Verarbeitet.
	  * Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	public boolean isProcessed();

    /** Column definition for Processed */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Processed = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Processed", null);
    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column definition for Processing */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Processing = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Processing", null);
    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Anfangsdatum.
	  * First effective day (inclusive)
	  */
	public void setStartDate (java.sql.Timestamp StartDate);

	/** Get Anfangsdatum.
	  * First effective day (inclusive)
	  */
	public java.sql.Timestamp getStartDate();

    /** Column definition for StartDate */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_StartDate = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "StartDate", null);
    /** Column name StartDate */
    public static final String COLUMNNAME_StartDate = "StartDate";

	/** Set Vertragsart	  */
	public void setType_Conditions (java.lang.String Type_Conditions);

	/** Get Vertragsart	  */
	public java.lang.String getType_Conditions();

    /** Column definition for Type_Conditions */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Type_Conditions = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Type_Conditions", null);
    /** Column name Type_Conditions */
    public static final String COLUMNNAME_Type_Conditions = "Type_Conditions";

	/** Set Verrechnungsart.
	  * Art der Verrechnung bei der Gegenüberstellung mit tatsächliche erbrachten Leistungen
	  */
	public void setType_Flatrate (java.lang.String Type_Flatrate);

	/** Get Verrechnungsart.
	  * Art der Verrechnung bei der Gegenüberstellung mit tatsächliche erbrachten Leistungen
	  */
	public java.lang.String getType_Flatrate();

    /** Column definition for Type_Flatrate */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Type_Flatrate = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Type_Flatrate", null);
    /** Column name Type_Flatrate */
    public static final String COLUMNNAME_Type_Flatrate = "Type_Flatrate";

	/** Set Einheiten-Typ.
	  * Dient der Zusammenfassung ähnlicher Maßeinheiten
	  */
	public void setUOMType (java.lang.String UOMType);

	/** Get Einheiten-Typ.
	  * Dient der Zusammenfassung ähnlicher Maßeinheiten
	  */
	public java.lang.String getUOMType();

    /** Column definition for UOMType */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_UOMType = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "UOMType", null);
    /** Column name UOMType */
    public static final String COLUMNNAME_UOMType = "UOMType";

	/** Get Aktualisiert.
	  * Datum, an dem dieser Eintrag aktualisiert wurde
	  */
	public java.sql.Timestamp getUpdated();

    /** Column definition for Updated */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object> COLUMN_Updated = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, Object>(I_C_Flatrate_Term.class, "Updated", null);
    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Aktualisiert durch.
	  * Nutzer, der diesen Eintrag aktualisiert hat
	  */
	public int getUpdatedBy();

    /** Column definition for UpdatedBy */
    public static final org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User> COLUMN_UpdatedBy = new org.adempiere.model.ModelColumn<I_C_Flatrate_Term, org.compiere.model.I_AD_User>(I_C_Flatrate_Term.class, "UpdatedBy", org.compiere.model.I_AD_User.class);
    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";
}