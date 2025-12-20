package com.ashaf.instanz.utils

import android.content.Context
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobImage
import com.ashaf.instanz.data.models.Template
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.io.FileOutputStream

class QuotePdfGenerator(
    private val context: Context,
    private val vatPercent: Int = 18,
    private val showPrices: Boolean = true,
    private val showVat: Boolean = true,
    private val jobSettings: com.ashaf.instanz.data.models.JobSettings = com.ashaf.instanz.data.models.JobSettings.default()
) {
    
    private val primaryColor = DeviceRgb(25, 118, 210)
    private val lightGray = DeviceRgb(240, 240, 240)
    
    fun generateQuotePdf(
        job: Job,
        template: Template,
        dataJson: Map<String, Map<String, String>>
    ): File {
        // Create PDF file
        val fileName = "Quote_${job.id}_${System.currentTimeMillis()}.pdf"
        val pdfDir = File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val pdfFile = File(pdfDir, fileName)
        
        val writer = PdfWriter(FileOutputStream(pdfFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        
        document.setTextAlignment(TextAlignment.RIGHT)
        
        // Add content
        addQuoteHeader(document)
        addCompanyInfo(document, dataJson)
        addClientInfo(document, dataJson)
        addExperienceSection(document, dataJson)
        addWorkSections(document, dataJson)
        addPricingSummary(document, dataJson)
        addTermsAndNotes(document, dataJson)
        addSignature(document, dataJson)
        
        document.close()
        
        return pdfFile
    }
    
    private fun addQuoteHeader(document: Document) {
        val title = Paragraph("הצעת מחיר")
            .setFontSize(28f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30f)
            .setFontColor(primaryColor)
        document.add(title)
        
        // Line separator
        document.add(Paragraph("\n"))
        document.add(Paragraph("\n"))
    }
    
    private fun addCompanyInfo(document: Document, dataJson: Map<String, Map<String, String>>) {
        val companyData = dataJson["company_info"] ?: return
        
        // Company logo placeholder (if exists)
        val companyName = companyData["company_name"] ?: ""
        val companyOwner = companyData["company_owner"] ?: ""
        
        val companyTitle = Paragraph(companyName)
            .setFontSize(20f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(primaryColor)
        document.add(companyTitle)
        
        if (companyOwner.isNotEmpty()) {
            val ownerPara = Paragraph(companyOwner)
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(ownerPara)
        }
        
        // Contact info in one line
        val contactInfo = mutableListOf<String>()
        companyData["company_phone"]?.let { if (it.isNotEmpty()) contactInfo.add("טלפון: $it") }
        companyData["company_email"]?.let { if (it.isNotEmpty()) contactInfo.add("אימייל: $it") }
        companyData["company_website"]?.let { if (it.isNotEmpty()) contactInfo.add("אתר: $it") }
        companyData["company_id"]?.let { if (it.isNotEmpty()) contactInfo.add("ח.פ: $it") }
        
        if (contactInfo.isNotEmpty()) {
            val contactPara = Paragraph(contactInfo.joinToString(" | "))
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(contactPara)
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addClientInfo(document: Document, dataJson: Map<String, Map<String, String>>) {
        val clientData = dataJson["client_info"] ?: return
        val quoteData = dataJson["quote_details"] ?: emptyMap()
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        // Title row
        val titleCell = Cell(1, 2)
            .add(Paragraph("פרטי הלקוח והעבודה").setBold().setFontSize(14f))
            .setBackgroundColor(primaryColor)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(10f)
            .setTextAlignment(TextAlignment.CENTER)
        table.addCell(titleCell)
        
        // Client info
        val firstName = clientData["client_first_name"] ?: ""
        val lastName = clientData["client_last_name"] ?: ""
        addTableRow(table, "שם פרטי:", firstName)
        addTableRow(table, "שם משפחה:", lastName)
        addTableRow(table, "טלפון:", clientData["client_phone"] ?: "")
        addTableRow(table, "כתובת:", clientData["client_address"] ?: "")
        
        // Quote info
        addTableRow(table, "עבודה:", quoteData["work_type"] ?: "")
        addTableRow(table, "תאריך:", quoteData["quote_date"] ?: "")
        if (quoteData["quote_number"]?.isNotEmpty() == true) {
            addTableRow(table, "מספר הצעה:", quoteData["quote_number"] ?: "")
        }
        
        document.add(table)
        document.add(Paragraph("\n"))
    }
    
    private fun addExperienceSection(document: Document, dataJson: Map<String, Map<String, String>>) {
        val expData = dataJson["experience"] ?: return
        val experience = expData["experience_description"] ?: ""
        val education = expData["education"] ?: ""
        
        if (experience.isEmpty() && education.isEmpty()) return
        
        val sectionTitle = Paragraph("כללי ומבוא")
            .setFontSize(16f)
            .setBold()
            .setFontColor(primaryColor)
            .setMarginTop(10f)
        document.add(sectionTitle)
        document.add(Paragraph("\n"))
        
        if (experience.isNotEmpty()) {
            val expTitle = Paragraph("ניסיון:")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(5f)
            document.add(expTitle)
            
            val expPara = Paragraph(experience)
                .setFontSize(11f)
                .setMarginBottom(10f)
            document.add(expPara)
        }
        
        if (education.isNotEmpty()) {
            val eduTitle = Paragraph("השכלה והכשרות:")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(5f)
            document.add(eduTitle)
            
            val eduPara = Paragraph(education)
                .setFontSize(11f)
                .setMarginBottom(10f)
            document.add(eduPara)
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addWorkSections(document: Document, dataJson: Map<String, Map<String, String>>) {
        val workSectionTitle = Paragraph("פירוט העבודות")
            .setFontSize(16f)
            .setBold()
            .setFontColor(primaryColor)
            .setMarginTop(10f)
        document.add(workSectionTitle)
        document.add(Paragraph("\n"))
        
        // Iterate through work sections 1-5
        for (i in 1..5) {
            val sectionData = dataJson["work_section_$i"] ?: continue
            val title = sectionData["work_${i}_title"] ?: ""
            val description = sectionData["work_${i}_description"] ?: ""
            
            if (title.isEmpty() && description.isEmpty()) continue
            
            // Section number and title
            val sectionTitle = Paragraph("$i. $title")
                .setFontSize(13f)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(5f)
            document.add(sectionTitle)
            
            // Description
            if (description.isNotEmpty()) {
                val descPara = Paragraph(description)
                    .setFontSize(11f)
                    .setMarginBottom(10f)
                document.add(descPara)
            }
            
            // Price details
            val quantity = sectionData["work_${i}_quantity"]?.toDoubleOrNull() ?: 0.0
            val unit = sectionData["work_${i}_unit"] ?: ""
            val unitPrice = sectionData["work_${i}_unit_price"]?.toDoubleOrNull() ?: 0.0
            val totalPrice = sectionData["work_${i}_total_price"]?.toDoubleOrNull() ?: 0.0
            
            if (totalPrice > 0) {
                val priceTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(10f)
                
                priceTable.addCell(createPriceCell("כמות", true))
                priceTable.addCell(createPriceCell("יחידה", true))
                priceTable.addCell(createPriceCell("מחיר יחידה", true))
                priceTable.addCell(createPriceCell("מחיר כולל", true))
                
                priceTable.addCell(createPriceCell(quantity.toString(), false))
                priceTable.addCell(createPriceCell(unit, false))
                priceTable.addCell(createPriceCell(QuoteCalculator.formatPrice(unitPrice), false))
                priceTable.addCell(createPriceCell(QuoteCalculator.formatPrice(totalPrice), false))
                
                document.add(priceTable)
            }
        }
    }
    
    private fun addPricingSummary(document: Document, dataJson: Map<String, Map<String, String>>) {
        val pricingData = dataJson["pricing_summary"] ?: return
        
        document.add(Paragraph("\n"))
        
        val summaryTitle = Paragraph("ריכוז מחירים")
            .setFontSize(16f)
            .setBold()
            .setFontColor(primaryColor)
            .setMarginTop(20f)
        document.add(summaryTitle)
        document.add(Paragraph("\n"))
        
        val subtotal = pricingData["subtotal"]?.toDoubleOrNull() ?: 0.0
        val vatRate = pricingData["vat_rate"]?.toDoubleOrNull() ?: vatPercent.toDouble()
        val vatAmount = pricingData["vat_amount"]?.toDoubleOrNull() ?: (subtotal * vatRate / 100.0)
        val totalWithVat = pricingData["total_with_vat"]?.toDoubleOrNull() ?: (subtotal + vatAmount)
        
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        addSummaryRow(summaryTable, "סיכום מחירים לפני מע\"מ:", QuoteCalculator.formatPrice(subtotal))
        addSummaryRow(summaryTable, "סה\"כ סכום מע\"מ (${vatRate.toInt()}%):", QuoteCalculator.formatPrice(vatAmount))
        
        // Total with VAT - highlighted
        val totalLabelCell = Cell()
            .add(Paragraph("סיכום מחירים כולל מע\"מ:").setBold().setFontSize(14f))
            .setBackgroundColor(primaryColor)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
        summaryTable.addCell(totalLabelCell)
        
        val totalValueCell = Cell()
            .add(Paragraph(QuoteCalculator.formatPrice(totalWithVat)).setBold().setFontSize(14f))
            .setBackgroundColor(primaryColor)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
        summaryTable.addCell(totalValueCell)
        
        document.add(summaryTable)
    }
    
    private fun addTermsAndNotes(document: Document, dataJson: Map<String, Map<String, String>>) {
        val termsData = dataJson["terms_and_notes"] ?: return
        
        val hasNotes = (1..5).any { termsData["note_$it"]?.isNotEmpty() == true } ||
                       termsData["additional_notes"]?.isNotEmpty() == true
        
        if (!hasNotes) return
        
        document.add(Paragraph("\n"))
        
        val notesTitle = Paragraph("תצהיר והערות:")
            .setFontSize(14f)
            .setBold()
            .setMarginTop(10f)
        document.add(notesTitle)
        document.add(Paragraph("\n"))
        
        val notesList = com.itextpdf.layout.element.List()
        
        for (i in 1..5) {
            val note = termsData["note_$i"] ?: ""
            if (note.isNotEmpty()) {
                notesList.add(note)
            }
        }
        
        if (notesList.isEmpty.not()) {
            document.add(notesList)
        }
        
        val additionalNotes = termsData["additional_notes"] ?: ""
        if (additionalNotes.isNotEmpty()) {
            val notesPara = Paragraph(additionalNotes)
                .setFontSize(11f)
                .setMarginTop(10f)
            document.add(notesPara)
        }
    }
    
    private fun addSignature(document: Document, dataJson: Map<String, Map<String, String>>) {
        val signatureData = dataJson["signature"] ?: return
        val companyData = dataJson["company_info"] ?: emptyMap()
        
        document.add(Paragraph("\n\n"))
        
        val closingText = signatureData["closing_text"] ?: "בכבוד רב,"
        val ownerName = companyData["company_owner"] ?: ""
        
        val closingPara = Paragraph(closingText)
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.RIGHT)
        document.add(closingPara)
        
        if (ownerName.isNotEmpty()) {
            val namePara = Paragraph(ownerName)
                .setFontSize(12f)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
            document.add(namePara)
        }
    }
    
    private fun addTableRow(table: Table, label: String, value: String) {
        val labelCell = Cell()
            .add(Paragraph(label).setBold())
            .setBackgroundColor(lightGray)
            .setPadding(8f)
            .setTextAlignment(TextAlignment.RIGHT)
        table.addCell(labelCell)
        
        val valueCell = Cell()
            .add(Paragraph(value))
            .setPadding(8f)
            .setTextAlignment(TextAlignment.RIGHT)
        table.addCell(valueCell)
    }
    
    private fun addSummaryRow(table: Table, label: String, value: String) {
        val labelCell = Cell()
            .add(Paragraph(label).setBold())
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
        table.addCell(labelCell)
        
        val valueCell = Cell()
            .add(Paragraph(value).setBold())
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
        table.addCell(valueCell)
    }
    
    private fun createPriceCell(text: String, isHeader: Boolean): Cell {
        val para = Paragraph(text)
        if (isHeader) {
            para.setBold()
        }
        
        return Cell()
            .add(para)
            .setBackgroundColor(if (isHeader) lightGray else null)
            .setPadding(8f)
            .setTextAlignment(TextAlignment.CENTER)
    }
}

