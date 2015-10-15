package de.metas.materialtracking.ch.lagerkonf.invoicing.impl;

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
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.pricing.api.IEditablePricingContext;
import org.adempiere.pricing.api.IPricingBL;
import org.adempiere.pricing.api.IPricingContext;
import org.adempiere.pricing.api.IPricingResult;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

import com.google.common.annotations.VisibleForTesting;

import de.metas.materialtracking.IHandlingUnitsInfo;
import de.metas.materialtracking.IHandlingUnitsInfoWritableQty;
import de.metas.materialtracking.ch.lagerkonf.ILagerKonfQualityBasedConfig;
import de.metas.materialtracking.qualityBasedInvoicing.IInvoicingItem;
import de.metas.materialtracking.qualityBasedInvoicing.IProductionMaterial;
import de.metas.materialtracking.qualityBasedInvoicing.IQualityBasedInvoicingBL;
import de.metas.materialtracking.qualityBasedInvoicing.IQualityInspectionLine;
import de.metas.materialtracking.qualityBasedInvoicing.IQualityInspectionLinesCollection;
import de.metas.materialtracking.qualityBasedInvoicing.IQualityInspectionOrder;
import de.metas.materialtracking.qualityBasedInvoicing.IVendorReceipt;
import de.metas.materialtracking.qualityBasedInvoicing.QualityInspectionLineType;
import de.metas.materialtracking.qualityBasedInvoicing.impl.QualityInspectionLinesBuilder;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.IQualityInvoiceLine;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.IQualityInvoiceLineGroup;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.QualityInvoiceLineGroupType;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.impl.QualityInvoiceLine;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.impl.QualityInvoiceLineGroup;
import de.metas.materialtracking.qualityBasedInvoicing.spi.IQualityInvoiceLineGroupsBuilder;
import de.metas.materialtracking.spi.IHandlingUnitsInfoFactory;

/**
 * Takes an {@link IQualityInspectionOrder} and creates {@link IQualityInvoiceLineGroup}s.
 *
 * Before you start using it you need to configure this builder:
 * <ul>
 * <li>{@link #setPricingContext(IPricingContext)} - set pricing context to be used when calculating the prices
 * <li>{@link #setQtyReceivedFromVendor(BigDecimal, I_C_UOM)} - set Vendor's received Qty/UOM
 * </ul>
 *
 * To generate the {@link IQualityInvoiceLineGroup}s you need to call: {@link #create()}.
 *
 * To get the results you need to call: {@link #getCreatedInvoiceLineGroups()}.
 *
 * See https://drive.google.com/file/d/0B-AaY-YNDnR5b045VGJsdVhRUGc/view
 *
 * @author tsa
 *
 */
public class QualityInvoiceLineGroupsBuilder implements IQualityInvoiceLineGroupsBuilder
{
	// Services
	private final transient IPricingBL pricingBL = Services.get(IPricingBL.class);
	private final transient IQualityBasedInvoicingBL qualityBasedInvoicingBL = Services.get(IQualityBasedInvoicingBL.class);
	private final transient IHandlingUnitsInfoFactory handlingUnitsInfoFactory = Services.get(IHandlingUnitsInfoFactory.class);

	// NOTE: keep services used to minimum

	// Parameters
	private final IQualityInspectionOrder _qiOrder;
	private IQualityInspectionLinesCollection _qiLines;
	private IVendorReceipt _receiptFromVendor;
	private IPricingContext _pricingContext;

	private static final transient CLogger logger = CLogger.getCLogger(QualityInvoiceLineGroupsBuilder.class);

	// Result
	private final List<IQualityInvoiceLineGroup> _createdInvoiceLineGroups = new ArrayList<IQualityInvoiceLineGroup>();

	public QualityInvoiceLineGroupsBuilder(final IQualityInspectionOrder qiOrder)
	{
		super();
		Check.assumeNotNull(qiOrder, "qiOrder not null");
		_qiOrder = qiOrder;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("[");
		sb.append("\nOrder: ").append(_qiOrder);
		sb.append("\nReceived from Vendor: ").append(_receiptFromVendor);
		sb.append("\nPricing context: ").append(_pricingContext);

		if (_qiLines != null)
		{
			sb.append("\n").append(_qiLines);
		}

		sb.append("\nCreated invoice line groups:");
		sb.append(_createdInvoiceLineGroups);

		sb.append("\n]");

		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.metas.materialtracking.ch.lagerkonf.invoicing.impl.IqualityInvoiceLineGroupsBuilder#getCreatedInvoiceLineGroups()
	 */
	@Override
	public List<IQualityInvoiceLineGroup> getCreatedInvoiceLineGroups()
	{
		return new ArrayList<>(_createdInvoiceLineGroups);
	}

	private void addCreatedInvoiceLineGroup(final IQualityInvoiceLineGroup invoiceLineGroup)
	{
		//
		// Validate the invoice line group (before adding)
		Check.assumeNotNull(invoiceLineGroup, "invoiceLineGroup not null");

		//
		// Validate the invoiceable line
		final IQualityInvoiceLine invoiceableLine = invoiceLineGroup.getInvoiceableLine();
		Check.assumeNotNull(invoiceableLine, "invoiceLineGroup shall have invoiceable line set: {0}", invoiceLineGroup);
		Check.assumeNotNull(invoiceableLine.getM_Product(), "invoiceable line's product not null: {0}", invoiceLineGroup);
		Check.assumeNotNull(invoiceableLine.getC_UOM(), "invoiceable line's uom not null: {0}", invoiceLineGroup);
		Check.assumeNotNull(invoiceableLine.getQty(), "invoiceable line's quantity not null: {0}", invoiceLineGroup);

		//
		// Validate the invoiceable line pricing
		final IPricingResult pricingResult = invoiceableLine.getPrice();
		Check.assumeNotNull(pricingResult, "invoiceable line's price not null: {0}", invoiceLineGroup);
		Check.assumeNotNull(pricingResult.isCalculated(), "invoiceable line's price shall be calculated: {0}", invoiceLineGroup);

		//
		// Add it to the list
		_createdInvoiceLineGroups.add(invoiceLineGroup);
	}

	@Override
	public void setReceiptFromVendor(final IVendorReceipt receiptFromVendor)
	{
		_receiptFromVendor = receiptFromVendor;
	}

	private IVendorReceipt getReceiptFromVendor()
	{
		Check.assumeNotNull(_receiptFromVendor, "_receiptFromVendor not null");
		return _receiptFromVendor;
	}

	@Override
	public void setPricingContext(final IPricingContext pricingContext)
	{
		Check.assumeNotNull(pricingContext, "pricingContext not null");
		_pricingContext = pricingContext.copy();
	}

	private IPricingContext getPricingContext()
	{
		Check.assumeNotNull(_pricingContext, "_pricingContext not null");
		return _pricingContext;
	}

	private final IQualityInspectionLinesCollection getQualityInspectionLinesCollection()
	{
		if (_qiLines == null)
		{
			final boolean buildWithAveragedValues = true; // when we invoice we need the average values
			final QualityInspectionLinesBuilder linesBuilder = new QualityInspectionLinesBuilder(_qiOrder, buildWithAveragedValues);
			linesBuilder.setReceiptFromVendor(getReceiptFromVendor());
			linesBuilder.create();
			_qiLines = linesBuilder.getCreatedQualityInspectionLinesCollection();
		}

		return _qiLines;
	}

	private ILagerKonfQualityBasedConfig getQualityBasedConfig()
	{
		return (ILagerKonfQualityBasedConfig)_qiOrder.getQualityBasedConfig();
	}

	private IQualityInspectionLine getScrapQualityInspectionLine()
	{
		return getQualityInspectionLinesCollection().getByType(QualityInspectionLineType.Scrap);
	}

	private IQualityInspectionLine getRawQualityInspectionLine()
	{
		return getQualityInspectionLinesCollection().getByType(QualityInspectionLineType.Raw);
	}

	@Override
	public void create()
	{
		//
		// Scrap
		// i.e. Erdbesatz
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();
		if (config.isFeeForScrap())
		{
			createQualityInvoiceLineGroup_Scrap();
		}

		//
		// By-Product(s)
		// e.g. Futterkarotten
		for (final IQualityInspectionLine line : getQualityInspectionLinesCollection().getAllByType(QualityInspectionLineType.ProducedByProducts))
		{
			createQualityInvoiceLineGroup_ProducedMaterial(line, QualityInvoiceLineGroupType.ProducedByProducts);
		}

		// 08848: for now we create a PreceedingRegularOrder record for all PP_Orders
		// TODO: create the RegularOrders' ICs independently
		if (
				// task 09117: we also want/need the regular orders if there is just one invoicing
				// config.getOverallNumberOfInvoicings() > 1  && 08092: we only deal with regular orders if it is the last of > 1 invoicings */ 
				qualityBasedInvoicingBL.isLastInspection(_qiOrder))
		{
			createQualityInvoiceLineGroups_RegularOrders();
		}

		//
		// Additional fees
		// e.g.
		// Abzug für Beitrag Basic-Linie 9831.2 kg -0.06 -589.88
		// Abzug für Beitrag Verkaufsförderung
		boolean firstItem = true; //
		for (final IInvoicingItem feeItem : getQualityBasedConfig().getAdditionalFeeProducts())
		{
			createQualityInvoiceLineGroup_AditionalFees(feeItem, firstItem); // is called with firstItem==true only one time
			firstItem = false;
		}

		//
		// Main Produced product
		// i.e. Karotten mittel
		{
			final IQualityInspectionLine line = getQualityInspectionLinesCollection().getByType(QualityInspectionLineType.ProducedMain);
			createQualityInvoiceLineGroup_ProducedMaterial(line, QualityInvoiceLineGroupType.ProducedMainProduct);
		}

		// ok, at this point we can be sure that there is already a invoicing group set!
		// add one detail line for the raw material and one for the scrap-information
		{
			// TODO: verify
			final IQualityInvoiceLineGroup firstGroup = getCreatedInvoiceLineGroups().get(0);

			final IQualityInspectionLine scrapLine = getScrapQualityInspectionLine();

			//
			// Before detail: raw product (as reference)
			// e.g. Karotten mittel ungewaschen
			{
				final IQualityInspectionLine rawLine = getRawQualityInspectionLine();
				final QualityInvoiceLine detail = createQualityInvoiceLine(rawLine);
				firstGroup.addDetailBefore(detail);
			}

			//
			// Before detail: scrap product
			// e.g. Erdbesatz
			{
				final QualityInvoiceLine detail = createQualityInvoiceLine(scrapLine);
				firstGroup.addDetailBefore(detail);

				detail.setQty(detail.getQty().negate()); // NOTE: in report is with "-"
			}
		}

		//
		// Co-Products
		// i.e. Karotten gross
		for (final IQualityInspectionLine line : getQualityInspectionLinesCollection().getAllByType(QualityInspectionLineType.ProducedCoProducts))
		{
			createQualityInvoiceLineGroup_ProducedMaterial(line, QualityInvoiceLineGroupType.ProducedCoProduct);
		}

		//
		// Withholding
		// e.g. Akontozahlung 50 %
		createQualityInvoiceLineGroup_WithholdingAmount();

		// //
		// // Preceeding Regular Orders
		// for (final IQualityInspectionOrder regularOrder : getPreceedingRegularOrders())
		// {
		// createQualityInvoiceLineGroup_PreceedingRegularOrder(regularOrder);
		// }

		// TODO: Auslagerung nach 01.06.2015
		// if it's the second/last invoice and if there are still unwashed carrots on storage
		// ... which means : SUM of (unwashed carrots from all preceding manufacturing orders) != qty received from vendor

		//
		// TODO: Transport Units(TU) received from vendor
		// i.e. Gebinde
		{
		}
	}

	private void createQualityInvoiceLineGroups_RegularOrders()
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();

		final List<IQualityInspectionOrder> allRegularOrders = _qiOrder.getAllOrders();

		final IQualityInspectionLinesCollection qualityInspectionLines = getQualityInspectionLinesCollection();
		final IQualityInspectionLine producedWithoutByProducts = qualityInspectionLines.getByType(QualityInspectionLineType.ProducedTotalWithoutByProducts);
		final IQualityInspectionLine overallRaw = qualityInspectionLines.getByType(QualityInspectionLineType.Raw);
		final IHandlingUnitsInfo overallRawHUInfo = overallRaw.getHandlingUnitsInfoProjected();
		final BigDecimal overallQtyTU = new BigDecimal(overallRawHUInfo.getQtyTU());
		final BigDecimal overallAvgProducedQtyPerTU = producedWithoutByProducts.getQtyProjected().divide(overallQtyTU, RoundingMode.HALF_UP);
		final I_C_UOM overallRawUOM = overallRaw.getC_UOM();

		//
		// iterate allRegularOrders and create and add one group per date
		final Map<Timestamp, QualityInvoiceLineGroup> date2InvoiceLineGroup = new HashMap<Timestamp, QualityInvoiceLineGroup>();
		for (final IQualityInspectionOrder regularOrder : allRegularOrders)
		{
			final Timestamp dateOfProduction = TimeUtil.getDay(regularOrder.getDateOfProduction());

			if (!isProcessRegularOrderForDate(dateOfProduction))
			{
				continue;
			}

			QualityInvoiceLineGroup qualityInvoiceLineGroup = date2InvoiceLineGroup.get(dateOfProduction);
			if (qualityInvoiceLineGroup != null)
			{
				continue; // already created one for this date
			}
			qualityInvoiceLineGroup = new QualityInvoiceLineGroup();
			qualityInvoiceLineGroup.setQualityInvoiceLineGroupType(QualityInvoiceLineGroupType.PreceeedingRegularOrderDeduction);

			final QualityInvoiceLine invoiceableLine = new QualityInvoiceLine();
			qualityInvoiceLineGroup.setInvoiceableLine(invoiceableLine);

			// initial HUInfo
			final IHandlingUnitsInfoWritableQty huInfo = handlingUnitsInfoFactory.createHUInfoWritableQty(overallRawHUInfo);
			huInfo.setQtyTU(0);
			invoiceableLine.setHandlingUnitsInfo(huInfo);

			// initial Qty
			invoiceableLine.setQty(BigDecimal.ZERO); // make sure it's not null, we want to iterate and add to it later

			//
			// static stuff like product, uom
			final String labelPrefix = "Auslagerung per ";
			final String labelToUse = createRegularOrderLabelToUse(labelPrefix, dateOfProduction);
			invoiceableLine.setDescription(labelToUse);

			// 08702: don't use the raw product itself, but a dedicated product.
			// That's because the raw product is the product-under-contract and therefore the IC would have IsToClear='Y' if we used the raw-product here.
			// Note that we only use the product, but the raw product's uom and qty.
			invoiceableLine.setM_Product(config.getRegularPPOrderProduct());

			invoiceableLine.setC_UOM(overallRawUOM);

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(invoiceableLine);
			final BigDecimal priceActual = config.getQualityAdjustmentForDateOrNull(dateOfProduction);

			final IPricingResult pricingResult = createPricingResult(pricingCtx, invoiceableLine, priceActual, invoiceableLine.getC_UOM());
			invoiceableLine.setPrice(pricingResult);

			// the detail that shall override the invoiceable line's displayed infos
			final QualityInvoiceLine detail = createDetailForSingleRegularOrder(overallRawUOM, huInfo, invoiceableLine.getQty(), labelToUse);
			detail.setDisplayed(true);

			qualityInvoiceLineGroup.setInvoiceableLineOverride(detail);

			date2InvoiceLineGroup.put(dateOfProduction, qualityInvoiceLineGroup);

			addCreatedInvoiceLineGroup(qualityInvoiceLineGroup);
		}

		QualityInvoiceLineGroup lastInvoiceLineGroup = null;
		BigDecimal netAmtSum = BigDecimal.ZERO; // needed for the final totals detail line
		int qtyTUSum = 0; // needed to decide if we were able to account for all HUs that were received

		//
		// now iterate allRegularOrders again, create one non-displayed detail for each PP_Order and aggregated them on the per-date-groups
		for (final IQualityInspectionOrder regularOrder : allRegularOrders)
		{
			final Timestamp dateOfProduction = TimeUtil.getDay(regularOrder.getDateOfProduction());

			if (!isProcessRegularOrderForDate(dateOfProduction))
			{
				continue;
			}

			final QualityInvoiceLineGroup invoiceLineGroupForDate = date2InvoiceLineGroup.get(dateOfProduction); // not null because we just created them
			Check.errorIf(invoiceLineGroupForDate == null, "Missing invoiceLineGroup for date {0}", dateOfProduction);

			final boolean firstItem = lastInvoiceLineGroup == null;
			if (firstItem)
			{
				final IProductionMaterial currentRawProductionMaterial = regularOrder.getRawProductionMaterial();
				final I_C_UOM uom = currentRawProductionMaterial.getC_UOM();

				final QualityInvoiceLine detailBefore = new QualityInvoiceLine();
				invoiceLineGroupForDate.addDetailBefore(detailBefore);

				detailBefore.setProductName("Anzahl kg pro Paloxe im Durchschnitt");
				detailBefore.setDisplayed(true);
				detailBefore.setC_UOM(uom);
				detailBefore.setQty(overallAvgProducedQtyPerTU);
				detailBefore.setM_Product(config.getRegularPPOrderProduct());
			}

			final QualityInvoiceLine regularInvoiceDetailLine = createQualityInvoiceLineDetail_RegularOrder(regularOrder,
					overallAvgProducedQtyPerTU,
					invoiceLineGroupForDate.getInvoiceableLine().getProductName());
			regularInvoiceDetailLine.setDisplayed(false); // it's not displayed, we just add it so that we can do some QA later on
			// invoiceLineGroup.setInvoiceableLineOverride(detail);
			invoiceLineGroupForDate.addDetailBefore(regularInvoiceDetailLine);

			// update the invoiceable line's qty and HU-Info
			// the casts are safe, because we created the QualityInvoiceLines in this very class
			final QualityInvoiceLine invoicableLineForDate = (QualityInvoiceLine)invoiceLineGroupForDate.getInvoiceableLine();
			final QualityInvoiceLine invoicableLineOverrideForDate = (QualityInvoiceLine)invoiceLineGroupForDate.getInvoiceableLineOverride();

			invoicableLineForDate.setQty(
					invoicableLineForDate.getQty().add(regularInvoiceDetailLine.getQty())
					);
			invoicableLineOverrideForDate.setQty(
					invoicableLineOverrideForDate.getQty().add(regularInvoiceDetailLine.getQty())
					);

			final int qtyTU;
			final IHandlingUnitsInfo regularInvoiceDetailHUInfo = regularInvoiceDetailLine.getHandlingUnitsInfo();
			if (regularInvoiceDetailHUInfo != null)
			{
				qtyTU = regularInvoiceDetailHUInfo.getQtyTU();

				final IHandlingUnitsInfo updatedHUInfo = invoicableLineForDate.getHandlingUnitsInfo().add(regularInvoiceDetailHUInfo);
				invoicableLineForDate.setHandlingUnitsInfo(updatedHUInfo);
				invoicableLineOverrideForDate.setHandlingUnitsInfo(updatedHUInfo);
			}
			else
			{
				qtyTU = 0;
			}

			regularInvoiceDetailLine.setPrice(invoicableLineForDate.getPrice()); // we augment the current PP_Order's detail line with the price info because we want its net amount.
			qtyTUSum += qtyTU;
			netAmtSum = netAmtSum.add(getNetAmount(regularInvoiceDetailLine));

			lastInvoiceLineGroup = invoiceLineGroupForDate;
		}

		// if there is not a PP_Order for each received HU *and* if where have a quality adjustment, then we need to explicitly deal with the rest
		final int missingQtyTU = overallRawHUInfo.getQtyTU() - qtyTUSum;
		final Timestamp dateForMissingQtyTUs = TimeUtil.addDays(config.getValidToDate(), 1);
		if (missingQtyTU > 0 && config.getQualityAdjustmentForDateOrNull(dateForMissingQtyTUs) != null)
		{
			final QualityInvoiceLineGroup invoiceLineGroup = new QualityInvoiceLineGroup();
			invoiceLineGroup.setQualityInvoiceLineGroupType(QualityInvoiceLineGroupType.PreceeedingRegularOrderDeduction);

			final String labelPrefix = "Auslagerung nach ";
			final String labelToUse = createRegularOrderLabelToUse(labelPrefix, dateForMissingQtyTUs);

			final IHandlingUnitsInfoWritableQty huInfo = handlingUnitsInfoFactory.createHUInfoWritableQty(overallRawHUInfo);
			((IHandlingUnitsInfoWritableQty)huInfo).setQtyTU(missingQtyTU);
			{
				final QualityInvoiceLine detailLineOverride = createDetailLineForRegularOrder(dateForMissingQtyTUs,
						overallRaw.getC_UOM(),
						huInfo,
						new BigDecimal(missingQtyTU).multiply(overallAvgProducedQtyPerTU),
						labelToUse);
				detailLineOverride.setDisplayed(true);
				invoiceLineGroup.setInvoiceableLineOverride(detailLineOverride);
			}
			{
				final QualityInvoiceLine detailLineInvcoicable = createDetailLineForRegularOrder(dateForMissingQtyTUs,
						overallRaw.getC_UOM(),
						huInfo,
						new BigDecimal(missingQtyTU).multiply(overallAvgProducedQtyPerTU),
						labelToUse);
				detailLineInvcoicable.setDisplayed(true);

				// Pricing
				final IPricingContext pricingCtx = createPricingContext(detailLineInvcoicable);
				final BigDecimal priceActual = config.getQualityAdjustmentForDateOrNull(dateForMissingQtyTUs);

				final IPricingResult pricingResult = createPricingResult(pricingCtx, detailLineInvcoicable, priceActual, detailLineInvcoicable.getC_UOM());
				detailLineInvcoicable.setPrice(pricingResult);

				invoiceLineGroup.setInvoiceableLine(detailLineInvcoicable);
			}
			addCreatedInvoiceLineGroup(invoiceLineGroup);
			lastInvoiceLineGroup = invoiceLineGroup;
		}

		if (lastInvoiceLineGroup != null)
		{
			// Total Qualitätslagerausgleich
			final QualityInvoiceLine detailAfter = new QualityInvoiceLine();
			final IQualityInvoiceLine invoiceableLine = lastInvoiceLineGroup.getInvoiceableLine();

			lastInvoiceLineGroup.addDetailAfter(detailAfter);

			detailAfter.setDisplayed(true);
			detailAfter.setProductName("Total Qualitätslagerausgleich"); // TRL
			detailAfter.setM_Product(invoiceableLine.getM_Product());
			detailAfter.setQty(BigDecimal.ONE);

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(detailAfter);
			final IPricingResult pricingResult = createPricingResult(pricingCtx, detailAfter, netAmtSum, lastInvoiceLineGroup.getInvoiceableLine().getC_UOM());
			detailAfter.setPrice(pricingResult);
		}
	}

	private String createRegularOrderLabelToUse(final String labelPrefix, final Timestamp date)
	{
		final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		final String labelToUse = labelPrefix + dateFormat.format(date);
		return labelToUse;
	}

	/**
	 *
	 * @param feeItem
	 * @param firstItem indicates if this is the first fee-item. If it is, then the method will prepend a details line that is about the total produced goods (without By-Products).
	 * @return
	 */
	private IQualityInvoiceLineGroup createQualityInvoiceLineGroup_AditionalFees(
			final IInvoicingItem feeItem,
			final boolean firstItem)
	{
		//
		// Invoice Line Group
		final QualityInvoiceLineGroup invoiceLineGroup = new QualityInvoiceLineGroup();
		invoiceLineGroup.setQualityInvoiceLineGroupType(QualityInvoiceLineGroupType.AdditionalFee);

		//
		// Detail: our reference line which we will use to calculate the fee invoiceable line
		// (i.e. Ausbeute (Marktfähige Ware))
		// Create the detail only if it's first line
		final IQualityInspectionLine producedTotalWithoutByProductsLine = getQualityInspectionLinesCollection().getByType(QualityInspectionLineType.ProducedTotalWithoutByProducts);
		if (firstItem)
		{
			final QualityInvoiceLine detail = createQualityInvoiceLine(producedTotalWithoutByProductsLine);
			invoiceLineGroup.addDetailBefore(detail);
		}

		//
		// Invoiceable line
		{
			final QualityInvoiceLine invoiceableLine = new QualityInvoiceLine();
			invoiceLineGroup.setInvoiceableLine(invoiceableLine);

			invoiceableLine.setM_Product(feeItem.getM_Product());
			invoiceableLine.setQty(producedTotalWithoutByProductsLine.getQtyProjected());
			invoiceableLine.setC_UOM(producedTotalWithoutByProductsLine.getC_UOM());

			// Pricing
			final IEditablePricingContext pricingCtx = createPricingContext(invoiceableLine);
			final IPricingResult pricingResult = pricingBL.calculatePrice(pricingCtx);
			pricingResult.setPriceList(pricingResult.getPriceList().negate());
			pricingResult.setPriceStd(pricingResult.getPriceStd().negate());
			pricingResult.setPriceLimit(pricingResult.getPriceLimit().negate());
			// NOTE: we need to set the Price UOM to same UOM as Qty to avoid conversion errors like (cannot convert from Kg to Stuck)
			pricingResult.setPrice_UOM_ID(producedTotalWithoutByProductsLine.getC_UOM().getC_UOM_ID());
			invoiceableLine.setPrice(pricingResult);
		}

		//
		addCreatedInvoiceLineGroup(invoiceLineGroup);
		return invoiceLineGroup;
	}

	private IQualityInvoiceLineGroup createQualityInvoiceLineGroup_Scrap()
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();

		final IQualityInspectionLine scrapLine = getScrapQualityInspectionLine();
		final BigDecimal qtyProjected = scrapLine.getQtyProjected();
		final BigDecimal percentage = scrapLine.getPercentage();
		final int qtyPrecision = scrapLine.getC_UOM().getStdPrecision();

		final BigDecimal qtyProjectedToCharge;
		final BigDecimal percentageToCharge;
		final BigDecimal scrapPercentageTreshold = config.getScrapPercentageTreshold();
		if (percentage.compareTo(scrapPercentageTreshold) <= 0)
		{
			percentageToCharge = BigDecimal.ZERO;
			qtyProjectedToCharge = BigDecimal.ONE; // needs to be 1, if it was 0, then the IC wouldn't be invoiced
		}
		else
		{
			percentageToCharge = percentage
					.subtract(scrapPercentageTreshold)
					.setScale(2, RoundingMode.HALF_UP);

			// compute the qty that is above the treshold and therefore shall be charged
			qtyProjectedToCharge = qtyProjected
					.multiply(percentageToCharge)
					.divide(percentage, qtyPrecision, RoundingMode.HALF_UP);
		}

		final QualityInvoiceLineGroup invoiceLineGroup = new QualityInvoiceLineGroup();
		invoiceLineGroup.setQualityInvoiceLineGroupType(QualityInvoiceLineGroupType.Scrap);

		// e.g. Entsorgungskosten (Erdbesatz > 10 %)

		// TODO: AD_Message
		final String labelToUse = "Entsorgungskosten (Erdbesatz > " + scrapPercentageTreshold.setScale(2, RoundingMode.HALF_UP) + "%)";
		// Detail with the invoiceable line's "label"
		{
			final QualityInvoiceLine detail = new QualityInvoiceLine();
			invoiceLineGroup.setInvoiceableLineOverride(detail);

			detail.setDisplayed(true);

			detail.setM_Product(scrapLine.getM_Product());
			detail.setProductName(labelToUse);
			detail.setPercentage(percentageToCharge);
			detail.setQty(qtyProjectedToCharge);
			detail.setC_UOM(scrapLine.getC_UOM());
		}
		// Invoiceable Line
		{
			final QualityInvoiceLine invoiceableLine = new QualityInvoiceLine();
			invoiceLineGroup.setInvoiceableLine(invoiceableLine);

			invoiceableLine.setM_Product(scrapLine.getM_Product());
			invoiceableLine.setProductName(labelToUse);
			invoiceableLine.setPercentage(percentageToCharge);
			invoiceableLine.setQty(qtyProjectedToCharge);
			invoiceableLine.setC_UOM(scrapLine.getC_UOM());

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(invoiceableLine);
			final IPricingResult pricingResult = pricingBL.calculatePrice(pricingCtx);
			pricingResult.setPriceStd(config.getScrapProcessingFeeForPercentage(percentage).negate());
			invoiceableLine.setPrice(pricingResult);
		}

		addCreatedInvoiceLineGroup(invoiceLineGroup);
		return invoiceLineGroup;
	}

	/**
	 * Applies for Main Product, Co-Product and By-Product
	 *
	 * @param producedMaterial
	 * @return
	 */
	private IQualityInvoiceLineGroup createQualityInvoiceLineGroup_ProducedMaterial(
			final IQualityInspectionLine producedMaterial,
			final QualityInvoiceLineGroupType qualityInvoiceLineGroupType)
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();

		final QualityInvoiceLineGroup invoiceLineGroup = new QualityInvoiceLineGroup();
		invoiceLineGroup.setQualityInvoiceLineGroupType(qualityInvoiceLineGroupType);

		//
		// Case: produced material with Fee (by-products)
		// e.g. this produced material (usually a by-product) needs to be sorted out, so we'll charge a fee on it.
		final I_M_Product product = producedMaterial.getM_Product();
		if (config.isFeeForProducedMaterial(product))
		{
			//
			// Total produced line (this is our reference for calculations)
			// e.g. Karotten netto gewaschen
			{
				final IQualityInspectionLine producedTotal = getQualityInspectionLinesCollection().getByType(QualityInspectionLineType.ProducedTotal);
				final QualityInvoiceLine detail = createQualityInvoiceLine(producedTotal);
				invoiceLineGroup.addDetailBefore(detail);
			}
			//
			// By-Products line (qty negated)
			// e.g. Ausfall (Futterkarotten)
			{
				final QualityInvoiceLine detail = createQualityInvoiceLine(producedMaterial);
				invoiceLineGroup.addDetailBefore(detail);

				detail.setQty(producedMaterial.getQtyProjected().negate());
			}
			//
			// Processing fee for By-Products
			// e.g. Zusaetzliche Sortierkosten
			// Detail with the invoiceable line's "label"
			{
				final QualityInvoiceLine detail = createQualityInvoiceLine(producedMaterial);

				invoiceLineGroup.setInvoiceableLineOverride(detail);
				detail.setDisplayed(true);
				detail.setProductName(config.getFeeNameForProducedProduct(product));
			}
			// actual invoiceable line
			{
				final QualityInvoiceLine invoiceableLine = createQualityInvoiceLine(producedMaterial);
				invoiceLineGroup.setInvoiceableLine(invoiceableLine);

				invoiceableLine.setProductName(config.getFeeNameForProducedProduct(product));

				// Pricing
				final BigDecimal feeProductPercent = producedMaterial.getPercentage();
				final BigDecimal feeAmt = config.getFeeForProducedMaterial(product, feeProductPercent);

				final IPricingContext pricingContext = createPricingContext(invoiceableLine);
				final IPricingResult pricingResult = createPricingResult(pricingContext, invoiceableLine, feeAmt.negate(), invoiceableLine.getC_UOM());
				invoiceableLine.setPrice(pricingResult);
			}
		}
		//
		// Case: produced material (without Free)
		else
		{
			final QualityInvoiceLine invoiceableLine = createQualityInvoiceLine(producedMaterial);
			invoiceLineGroup.setInvoiceableLine(invoiceableLine);

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(invoiceableLine);
			final IPricingResult pricingResult = pricingBL.calculatePrice(pricingCtx);
			invoiceableLine.setPrice(pricingResult);
		}

		addCreatedInvoiceLineGroup(invoiceLineGroup);
		return invoiceLineGroup;
	}

	private IQualityInvoiceLineGroup createQualityInvoiceLineGroup_WithholdingAmount()
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();
		if(config.getOverallNumberOfInvoicings() < 2)
		{
			logger.log(Level.FINE, "Nothing to do as ILagerKonfQualityBasedConfig {0} has OverallNumberOfInvoicings = {1}", 
					new Object[] { config, config.getOverallNumberOfInvoicings() });
			return null; // nothing to do
		}
		
		final IInvoicingItem withholdingItem = config.getWithholdingProduct();
		final BigDecimal withholdingBaseAmount = getTotalInvoiceableAmount();
		final I_M_Product withholdingProduct = withholdingItem.getM_Product();
		final I_C_UOM withholdingPriceUOM = withholdingItem.getC_UOM();

		final BigDecimal withholdingAmount;
		final String labelToUse;

		if (qualityBasedInvoicingBL.isLastInspection(_qiOrder))
		{
			withholdingAmount = _qiOrder.getAlreadyInvoicedNetSum();
			labelToUse = "Akonto-Netto"; // task 08947: the amount was already correct, just use this label
		}
		else
		{
			final BigDecimal withholdingPercent = config.getWithholdingPercent();
			withholdingAmount = withholdingBaseAmount
					.multiply(withholdingPercent)
					.divide(Env.ONEHUNDRED, 12, RoundingMode.HALF_UP); // will be fixed when pricing result is created
			labelToUse = "Einbehalt " + withholdingPercent.setScale(0, RoundingMode.HALF_UP) + "%";
		}

		// Invoice Line Group
		final QualityInvoiceLineGroup invoiceLineGroup = new QualityInvoiceLineGroup();
		invoiceLineGroup.setQualityInvoiceLineGroupType(QualityInvoiceLineGroupType.WithholdingAmount);

		//
		// Detail: Withholding base
		{
			final QualityInvoiceLine detail = new QualityInvoiceLine();
			invoiceLineGroup.addDetailBefore(detail);

			detail.setDisplayed(false);
			detail.setProductName("Withholding base"); // TRL
			detail.setQty(BigDecimal.ONE);
			detail.setC_UOM(withholdingPriceUOM);
			detail.setM_Product(withholdingProduct);

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(detail);
			final IPricingResult pricingResult = createPricingResult(pricingCtx, detail, withholdingBaseAmount, withholdingPriceUOM);
			detail.setPrice(pricingResult);
		}

		//
		// Withholding
		{
			final QualityInvoiceLine overridingDetail = new QualityInvoiceLine();
			invoiceLineGroup.setInvoiceableLineOverride(overridingDetail);

			overridingDetail.setProductName(labelToUse);
			overridingDetail.setM_Product(withholdingProduct);
			overridingDetail.setQty(BigDecimal.ONE);
			overridingDetail.setC_UOM(withholdingPriceUOM);
		}
		{
			final QualityInvoiceLine invoiceableLine = new QualityInvoiceLine();
			invoiceLineGroup.setInvoiceableLine(invoiceableLine);

			invoiceableLine.setM_Product(withholdingProduct);
			invoiceableLine.setQty(BigDecimal.ONE);
			invoiceableLine.setC_UOM(withholdingPriceUOM);

			// Pricing
			final IPricingContext pricingCtx = createPricingContext(invoiceableLine);
			final IPricingResult pricingResult = createPricingResult(pricingCtx, invoiceableLine, withholdingAmount.negate(), withholdingPriceUOM);
			invoiceableLine.setPrice(pricingResult);
		}

		addCreatedInvoiceLineGroup(invoiceLineGroup);
		return invoiceLineGroup;
	}

	// TODO: cover by our tests in QualityOrderReportBuilder_StandardUseCase_Test
	@VisibleForTesting
	private QualityInvoiceLine createQualityInvoiceLineDetail_RegularOrder(
			final IQualityInspectionOrder regularOrder,
			final BigDecimal overallAvgProducedQtyPerTU,
			final String labelToUse)
	{
		//
		// Extract parameters from regular manufacturing norder

		final Timestamp dateOfProduction = TimeUtil.getDay(regularOrder.getDateOfProduction());

		if (!isProcessRegularOrderForDate(dateOfProduction))
		{
			return null; // nothing to do
		}

		final IQualityInspectionLinesCollection qualityInspectionLines = getQualityInspectionLinesCollection();

		final IProductionMaterial currentRawProductionMaterial = regularOrder.getRawProductionMaterial();
		final I_C_UOM uom = currentRawProductionMaterial.getC_UOM();

		final StringBuilder description = new StringBuilder("PP_Order[IsQualityInspection=" + regularOrder.isQualityInspection() + ", PP_Order.DocumentNo="
				+ regularOrder.getPP_Order().getDocumentNo() + "]");

		IHandlingUnitsInfo currentRawHUInfo = currentRawProductionMaterial.getHandlingUnitsInfo();
		if (currentRawHUInfo == null)
		{
			logger.log(Level.INFO, "IQualityInspectionOrder {0} has no IHandlingUnitsInfo; computing the probable TU-Qty", regularOrder);

			final IQualityInspectionLine overallRaw = qualityInspectionLines.getByType(QualityInspectionLineType.Raw);
			final IHandlingUnitsInfo overallRawHUInfo = overallRaw.getHandlingUnitsInfoProjected();
			final BigDecimal overallQtyTU = new BigDecimal(overallRawHUInfo.getQtyTU());

			final BigDecimal bdOverallqtyTU = overallQtyTU
					.multiply(currentRawProductionMaterial.getQty())
					.divide(overallRaw.getQtyProjected(), RoundingMode.HALF_UP)
					.setScale(0, RoundingMode.HALF_UP);
			final int currentQtyTUCalc = bdOverallqtyTU.signum() <= 0 ? 1 : bdOverallqtyTU.intValueExact();

			description.append("; Missing HandlingUnitsInfo!"
					+ " Calculated QtyTU=" + currentQtyTUCalc + " as ( overallQtyTU=" + overallQtyTU
					+ " * currentRawProductionMaterialQty=" + currentRawProductionMaterial.getQty()
					+ " / overallRawQtyProjected=" + overallRaw.getQtyProjected() + " )");

			currentRawHUInfo = handlingUnitsInfoFactory.createHUInfoWritableQty(overallRawHUInfo);
			((IHandlingUnitsInfoWritableQty)currentRawHUInfo).setQtyTU(currentQtyTUCalc);
		}
		final BigDecimal currentQty = overallAvgProducedQtyPerTU.multiply(new BigDecimal(currentRawHUInfo.getQtyTU()));

		final QualityInvoiceLine detailForCurrentRegularOrder = createDetailLineForRegularOrder(dateOfProduction, uom, currentRawHUInfo, currentQty, labelToUse);
		detailForCurrentRegularOrder.setDescription(description.toString());

		return detailForCurrentRegularOrder;
	}

	private boolean isProcessRegularOrderForDate(final Timestamp dateOfProduction)
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();

		final BigDecimal priceActual = config.getQualityAdjustmentForDateOrNull(dateOfProduction);
		if (priceActual == null)
		{
			logger.log(Level.FINE, "ILagerKonfQualityBasedConfig {0} has no price for dateOfProduction {1}. Assuming that there is nothing to do", new Object[] { config, dateOfProduction });
			return false;
		}
		return true;
	}

	private QualityInvoiceLine createDetailLineForRegularOrder(
			final Timestamp date,
			final I_C_UOM uom,
			final IHandlingUnitsInfo currentRawHUInfo,
			final BigDecimal qty,
			final String labelToUse)
	{
		// Detail

		final QualityInvoiceLine detail = createDetailForSingleRegularOrder(uom, currentRawHUInfo, qty, labelToUse);

		return detail;
	}

	private QualityInvoiceLine createDetailForSingleRegularOrder(final I_C_UOM uom,
			final IHandlingUnitsInfo huInfo,
			final BigDecimal qty,
			final String labelToUse)
	{
		final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();

		final QualityInvoiceLine detail = new QualityInvoiceLine();

		detail.setDisplayed(false); // we only want this detail for reference and QA
		detail.setM_Product(config.getRegularPPOrderProduct());
		detail.setC_UOM(uom);
		detail.setQty(qty);
		detail.setHandlingUnitsInfo(huInfo);
		detail.setProductName(labelToUse);
		return detail;
	}

	// private void createInvoicableLineForRegularOrder(
	// final Timestamp date,
	// final I_C_UOM uom,
	// final IHandlingUnitsInfo currentRawHUInfo,
	// final BigDecimal qty,
	// final QualityInvoiceLineGroup invoiceLineGroup,
	// final String labelPrefix)
	// {
	//
	// final ILagerKonfQualityBasedConfig config = getQualityBasedConfig();
	//
	// // Detail
	// // TODO: AD_Message
	// {
	// final QualityInvoiceLine detail = createDetailForSingleRegularOrder(uom, currentRawHUInfo, qty, labelToUse);
	//
	// // invoiceLineGroup.setInvoiceableLineOverride(detail);
	// invoiceLineGroup.setInvoiceableLineOverride(detail);
	// }
	//
	// }

	private IEditablePricingContext createPricingContext(final IQualityInvoiceLine line)
	{
		final IPricingContext pricingContextInitial = getPricingContext();
		final IEditablePricingContext pricingContext = pricingContextInitial.copy();

		final I_M_Product product = line.getM_Product();
		pricingContext.setM_Product_ID(product == null ? -1 : product.getM_Product_ID());
		pricingContext.setQty(line.getQty());

		final I_C_UOM uom = line.getC_UOM();
		if (uom == null)
		{
			pricingContext.setConvertPriceToContextUOM(false);
			pricingContext.setC_UOM_ID(-1);
		}
		else
		{
			pricingContext.setC_UOM_ID(uom.getC_UOM_ID());
		}
		// pricingContext.setPriceDate( ); // FIXME: shall we set it here?

		return pricingContext;
	}

	private IPricingResult createPricingResult(
			final IPricingContext pricingCtx,
			final QualityInvoiceLine line,
			final BigDecimal price,
			final I_C_UOM priceUOM)
	{
		Check.assumeNotNull(price, "price not null");
		Check.assumeNotNull(priceUOM, "priceUOM not null");

		final IPricingResult pricingResult = pricingBL.calculatePrice(pricingCtx);

		final int pricePrecision = pricingResult.getPrecision();
		final BigDecimal priceToSet = price.setScale(pricePrecision, RoundingMode.HALF_UP);

		pricingResult.setC_TaxCategory_ID(pricingResult.getC_TaxCategory_ID());
		pricingResult.setPriceStd(priceToSet);
		pricingResult.setPriceLimit(priceToSet);
		pricingResult.setPriceList(priceToSet);
		pricingResult.setPrice_UOM_ID(priceUOM.getC_UOM_ID());
		pricingResult.setDiscount(BigDecimal.ZERO);
		pricingResult.setCalculated(true);

		return pricingResult;
	}

	private BigDecimal getTotalInvoiceableAmount()
	{
		BigDecimal totalNetAmt = BigDecimal.ZERO;
		for (final IQualityInvoiceLineGroup invoiceLineGroup : _createdInvoiceLineGroups)
		{
			final IQualityInvoiceLine invoiceableLine = invoiceLineGroup.getInvoiceableLine();
			final BigDecimal netAmt = getNetAmount(invoiceableLine);
			totalNetAmt = totalNetAmt.add(netAmt);
		}

		return totalNetAmt;
	}

	private BigDecimal getNetAmount(final IQualityInvoiceLine line)
	{
		Check.assumeNotNull(line, "line not null");

		final IPricingResult pricingResult = line.getPrice();
		Check.assumeNotNull(pricingResult, "pricingResult not null");
		Check.assume(pricingResult.isCalculated(), "Price is calculated for {0}", line);

		final BigDecimal price = pricingResult.getPriceStd();
		final int pricePrecision = pricingResult.getPrecision();

		final BigDecimal qty = line.getQty();

		final BigDecimal netAmt = price.multiply(qty).setScale(pricePrecision, RoundingMode.HALF_UP);
		return netAmt;
	}

	/**
	 * Creates an {@link QualityInvoiceLine} instance.
	 *
	 * Product, Qty, UOM are copied from given {@link IQualityInspectionLine}.
	 *
	 * @param qiLine
	 * @return
	 */
	private QualityInvoiceLine createQualityInvoiceLine(final IQualityInspectionLine qiLine)
	{
		final QualityInvoiceLine invoiceLine = new QualityInvoiceLine();
		invoiceLine.setM_Product(qiLine.getM_Product());
		invoiceLine.setProductName(qiLine.getName());
		invoiceLine.setPercentage(qiLine.getPercentage());
		invoiceLine.setQty(qiLine.getQtyProjected());
		invoiceLine.setC_UOM(qiLine.getC_UOM());
		invoiceLine.setHandlingUnitsInfo(qiLine.getHandlingUnitsInfoProjected());
		return invoiceLine;
	}
}