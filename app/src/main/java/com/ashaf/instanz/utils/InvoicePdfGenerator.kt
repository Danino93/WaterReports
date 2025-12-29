package com.ashaf.instanz.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.Template
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.BaseDirection
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class InvoicePdfGenerator(
    private val context: Context,
    private val vatPercent: Int = 18,
    private val showPrices: Boolean = true,
    private val showVat: Boolean = true,
    private val jobSettings: com.ashaf.instanz.data.models.JobSettings = com.ashaf.instanz.data.models.JobSettings.default()
) {
    
    private val primaryColor = DeviceRgb(25, 118, 210)
    private val lightGray = DeviceRgb(245, 245, 245)
    private val darkGray = DeviceRgb(50, 50, 50)
    private val borderGray = DeviceRgb(200, 200, 200)
    private val accentColor = DeviceRgb(33, 150, 243)
    
    // Hebrew font for PDF
    private val hebrewFont: PdfFont by lazy {
        try {
            val fontFiles = listOf(
                "fonts/NotoSansHebrew-Regular.ttf",
                "fonts/Assistant-Regular.ttf",
                "fonts/Heebo-Regular.ttf",
                "fonts/Rubik-Regular.ttf"
            )
            
            for (fontFile in fontFiles) {
                try {
                    val fontStream = context.assets.open(fontFile)
                    val fontBytes = fontStream.readBytes()
                    fontStream.close()
                    
                    return@lazy PdfFontFactory.createFont(
                        fontBytes,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                    )
                } catch (e: Exception) {
                    continue
                }
            }
            
            PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA,
                PdfEncodings.IDENTITY_H
            )
        } catch (e: Exception) {
            throw RuntimeException("Cannot create PDF without Hebrew font!", e)
        }
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("he", "IL"))
    
    fun generateInvoicePdf(
        job: Job,
        template: Template,
        dataJson: Map<String, Map<String, String>>
    ): File {
        // Create PDF file
        val fileName = "Invoice_${job.jobNumber}_${System.currentTimeMillis()}.pdf"
        val pdfDir = File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val pdfFile = File(pdfDir, fileName)
        
        val writer = PdfWriter(FileOutputStream(pdfFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        
        // Set margins
        document.setMargins(80f, 50f, 60f, 50f)
        document.setTextAlignment(TextAlignment.RIGHT)
        
        // Load custom content
        val customContent = template.parseCustomContent()
        
        // Add header/footer event handler
        pdfDoc.addEventHandler(
            PdfDocumentEvent.END_PAGE,
            InvoiceHeaderFooterEventHandler(customContent, template.name)
        )
        
        // Get invoice header data
        val invoiceHeader = dataJson["invoice_header"] ?: emptyMap()
        val invoiceType = invoiceHeader["invoice_type"] ?: "חשבונית"
        val invoiceNumber = invoiceHeader["invoice_number"] ?: ""
        val invoiceDate = invoiceHeader["invoice_date"] ?: dateFormat.format(Date())
        
        // Add invoice header
        addInvoiceHeader(document, invoiceType, invoiceNumber, invoiceDate)
        
        // Add company info (from customContent)
        addCompanyInfo(document, customContent)
        
        // Add client info
        addClientInfo(document, dataJson)
        
        // Add invoice items table
        addInvoiceItems(document, dataJson)
        
        // Add pricing summary
        addPricingSummary(document, dataJson)
        
        // Add payment terms
        addPaymentTerms(document, dataJson)
        
        // Add signature
        addSignature(document, customContent)
        
        document.close()
        
        return pdfFile
    }
    
    private fun addInvoiceHeader(
        document: Document,
        invoiceType: String,
        invoiceNumber: String,
        invoiceDate: String
    ) {
        // Main title - professional design
        val title = createHebrewParagraph(invoiceType)
            .setFontSize(36f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(15f)
            .setMarginBottom(20f)
            .setFontColor(darkGray)
        document.add(title)
        
        // Decorative line
        val lineTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        lineTable.addCell(
            Cell()
                .setHeight(3f)
                .setBackgroundColor(darkGray)
                .setBorder(Border.NO_BORDER)
        )
        document.add(lineTable)
        
        // Invoice number and date table - professional design
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .useAllAvailableWidth()
            .setMarginBottom(30f)
        
        val numberCell = Cell()
            .add(createHebrewParagraph("מספר $invoiceType: $invoiceNumber")
                .setFontSize(18f)
                .setBold()
                .setFontColor(DeviceRgb(255, 255, 255)))
            .setPadding(14f)
            .setBackgroundColor(darkGray)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(borderGray, 1.5f))
        
        val dateCell = Cell()
            .add(createHebrewParagraph("תאריך: $invoiceDate")
                .setFontSize(18f)
                .setBold()
                .setFontColor(DeviceRgb(255, 255, 255)))
            .setPadding(14f)
            .setBackgroundColor(darkGray)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(borderGray, 1.5f))
        
        headerTable.addCell(numberCell)
        headerTable.addCell(dateCell)
        document.add(headerTable)
        
        document.add(Paragraph("\n"))
    }
    
    private fun addCompanyInfo(
        document: Document,
        customContent: com.ashaf.instanz.data.models.TemplateCustomContent?
    ) {
        customContent ?: return
        
        val companyTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        val titleCell = Cell()
            .add(createHebrewParagraph("פרטי החברה (מוכר)").setBold().setFontSize(15f))
            .setBackgroundColor(darkGray)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(borderGray, 1f))
        companyTable.addCell(titleCell)
        
        // Company name
        if (customContent.company.isNotEmpty()) {
            addTableRow(companyTable, "שם החברה:", customContent.company)
        }
        
        // Business number
        if (customContent.businessNumber.isNotEmpty()) {
            addTableRow(companyTable, "ח.פ:", customContent.businessNumber)
        }
        
        // Contact info
        val contactInfo = mutableListOf<String>()
        if (customContent.phone.isNotEmpty()) contactInfo.add("טלפון: ${customContent.phone}")
        if (customContent.email.isNotEmpty()) contactInfo.add("אימייל: ${customContent.email}")
        if (customContent.website.isNotEmpty()) contactInfo.add("אתר: ${customContent.website}")
        
        if (contactInfo.isNotEmpty()) {
            addTableRow(companyTable, "פרטי התקשרות:", contactInfo.joinToString(" | "))
        }
        
        document.add(companyTable)
        document.add(Paragraph("\n"))
    }
    
    private fun addClientInfo(
        document: Document,
        dataJson: Map<String, Map<String, String>>
    ) {
        val clientData = dataJson["client_details"] ?: return
        
        val clientTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        val titleCell = Cell()
            .add(createHebrewParagraph("פרטי הלקוח (קונה)").setBold().setFontSize(15f))
            .setBackgroundColor(darkGray)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(borderGray, 1f))
        clientTable.addCell(titleCell)
        
        val firstName = clientData["client_first_name"] ?: ""
        val lastName = clientData["client_last_name"] ?: ""
        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            addTableRow(clientTable, "שם:", "$firstName $lastName")
        }
        
        if (clientData["client_company"]?.isNotEmpty() == true) {
            addTableRow(clientTable, "חברה:", clientData["client_company"] ?: "")
        }
        
        if (clientData["client_id_number"]?.isNotEmpty() == true) {
            addTableRow(clientTable, "ח.פ / ת.ז:", clientData["client_id_number"] ?: "")
        }
        
        if (clientData["client_phone"]?.isNotEmpty() == true) {
            addTableRow(clientTable, "טלפון:", clientData["client_phone"] ?: "")
        }
        
        if (clientData["client_email"]?.isNotEmpty() == true) {
            addTableRow(clientTable, "אימייל:", clientData["client_email"] ?: "")
        }
        
        // Build address
        val addressParts = mutableListOf<String>()
        clientData["client_street"]?.let { if (it.isNotEmpty()) addressParts.add(it) }
        clientData["client_building_number"]?.let { if (it.isNotEmpty()) addressParts.add(it) }
        clientData["client_apartment"]?.let { if (it.isNotEmpty()) addressParts.add("דירה $it") }
        clientData["client_city"]?.let { if (it.isNotEmpty()) addressParts.add(it) }
        
        if (addressParts.isNotEmpty()) {
            addTableRow(clientTable, "כתובת:", addressParts.joinToString(", "))
        }
        
        document.add(clientTable)
        document.add(Paragraph("\n"))
    }
    
    private fun addInvoiceItems(
        document: Document,
        dataJson: Map<String, Map<String, String>>
    ) {
        // Get items from invoice_items section or from recommendations
        val itemsData = dataJson["invoice_items"] ?: emptyMap()
        
        // Try to get items from recommendations (similar to quote template)
        val allItems = mutableListOf<InvoiceItem>()
        
        // Check for items in invoice_items section
        // For now, we'll use a simple structure - can be enhanced later
        // Items should be stored as: item_1_description, item_1_quantity, etc.
        
        for (i in 1..20) { // Support up to 20 items
            val description = itemsData["item_${i}_description"] ?: ""
            if (description.isEmpty()) continue
            
            val quantity = itemsData["item_${i}_quantity"]?.toDoubleOrNull() ?: 1.0
            val unit = itemsData["item_${i}_unit"] ?: ""
            val unitPrice = itemsData["item_${i}_unit_price"]?.toDoubleOrNull() ?: 0.0
            val total = quantity * unitPrice
            
            allItems.add(InvoiceItem(description, quantity, unit, unitPrice, total))
        }
        
        if (allItems.isEmpty()) {
            // If no items found, add placeholder
            document.add(createHebrewParagraph("פירוט פריטים/שירותים")
                .setFontSize(16f)
                .setBold()
                .setFontColor(primaryColor)
                .setMarginTop(15f))
            document.add(createHebrewParagraph("לא נוספו פריטים")
                .setFontSize(12f)
                .setMarginBottom(15f))
            return
        }
        
        // Items table
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 35f, 15f, 15f, 15f, 15f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        // Header row - professional design
        val headers = listOf("#", "תיאור", "כמות", "יחידה", "מחיר יחידה", "סה\"כ")
        headers.forEach { headerText ->
            val cell = Cell()
                .add(createHebrewParagraph(headerText)
                    .setBold()
                    .setFontSize(12f)
                    .setFontColor(DeviceRgb(255, 255, 255)))
                .setBackgroundColor(darkGray)
                .setPadding(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(SolidBorder(DeviceRgb(100, 100, 100), 1f))
            itemsTable.addHeaderCell(cell)
        }
        
        // Items rows
        allItems.forEachIndexed { index, item ->
            val rowColor = if (index % 2 == 0) DeviceRgb(255, 255, 255) else lightGray
            
            itemsTable.addCell(createItemCell("${index + 1}", rowColor))
            itemsTable.addCell(createItemCell(item.description, rowColor))
            itemsTable.addCell(createItemCell(item.quantity.toString(), rowColor))
            itemsTable.addCell(createItemCell(item.unit, rowColor))
            itemsTable.addCell(createItemCell(QuoteCalculator.formatPrice(item.unitPrice), rowColor))
            itemsTable.addCell(createItemCell(QuoteCalculator.formatPrice(item.total), rowColor))
        }
        
        document.add(itemsTable)
    }
    
    private fun addPricingSummary(
        document: Document,
        dataJson: Map<String, Map<String, String>>
    ) {
        val pricingData = dataJson["pricing_summary"] ?: return
        
        document.add(Paragraph("\n"))
        
        val summaryTitle = createHebrewParagraph("סיכום מחירים")
            .setFontSize(20f)
            .setBold()
            .setFontColor(darkGray)
            .setMarginTop(25f)
            .setMarginBottom(10f)
        document.add(summaryTitle)
        document.add(Paragraph("\n"))
        
        val subtotal = pricingData["subtotal"]?.toDoubleOrNull() ?: 0.0
        val vatRate = pricingData["vat_rate"]?.toDoubleOrNull() ?: vatPercent.toDouble()
        val vatAmount = pricingData["vat_amount"]?.toDoubleOrNull() ?: (subtotal * vatRate / 100.0)
        val totalWithVat = pricingData["total_with_vat"]?.toDoubleOrNull() ?: (subtotal + vatAmount)
        
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .useAllAvailableWidth()
            .setMarginBottom(25f)
        
        // Subtotal row
        val subtotalLabelCell = Cell()
            .add(createHebrewParagraph("סיכום לפני מע\"מ:").setBold().setFontSize(12f))
            .setBackgroundColor(lightGray)
            .setPadding(12f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1f))
        summaryTable.addCell(subtotalLabelCell)
        
        val subtotalValueCell = Cell()
            .add(createHebrewParagraph(QuoteCalculator.formatPrice(subtotal)).setBold().setFontSize(12f))
            .setBackgroundColor(DeviceRgb(255, 255, 255))
            .setPadding(12f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1f))
        summaryTable.addCell(subtotalValueCell)
        
        // VAT row
        val vatLabelCell = Cell()
            .add(createHebrewParagraph("מע\"מ (${vatRate.toInt()}%):").setBold().setFontSize(12f))
            .setBackgroundColor(lightGray)
            .setPadding(12f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1f))
        summaryTable.addCell(vatLabelCell)
        
        val vatValueCell = Cell()
            .add(createHebrewParagraph(QuoteCalculator.formatPrice(vatAmount)).setBold().setFontSize(12f))
            .setBackgroundColor(DeviceRgb(255, 255, 255))
            .setPadding(12f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1f))
        summaryTable.addCell(vatValueCell)
        
        // Total with VAT - highlighted with professional design
        val totalLabelCell = Cell()
            .add(createHebrewParagraph("סה\"כ לתשלום כולל מע\"מ:").setBold().setFontSize(16f))
            .setBackgroundColor(darkGray)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(14f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1.5f))
        summaryTable.addCell(totalLabelCell)
        
        val totalValueCell = Cell()
            .add(createHebrewParagraph(QuoteCalculator.formatPrice(totalWithVat)).setBold().setFontSize(16f))
            .setBackgroundColor(darkGray)
            .setFontColor(DeviceRgb(255, 255, 255))
            .setPadding(14f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 1.5f))
        summaryTable.addCell(totalValueCell)
        
        document.add(summaryTable)
    }
    
    private fun addPaymentTerms(
        document: Document,
        dataJson: Map<String, Map<String, String>>
    ) {
        val paymentData = dataJson["payment_terms"] ?: return
        
        val paymentMethod = paymentData["payment_method"] ?: ""
        val paymentNotes = paymentData["payment_notes"] ?: ""
        val generalNotes = paymentData["general_notes"] ?: ""
        
        if (paymentMethod.isEmpty() && paymentNotes.isEmpty() && generalNotes.isEmpty()) return
        
        document.add(Paragraph("\n"))
        
        val termsTitle = createHebrewParagraph("תנאי תשלום והערות")
            .setFontSize(18f)
            .setBold()
            .setFontColor(darkGray)
            .setMarginTop(20f)
            .setMarginBottom(10f)
        document.add(termsTitle)
        document.add(Paragraph("\n"))
        
        if (paymentMethod.isNotEmpty()) {
            document.add(createHebrewParagraph("אופן תשלום: $paymentMethod")
                .setFontSize(12f)
                .setMarginBottom(5f))
        }
        
        if (paymentNotes.isNotEmpty()) {
            document.add(createHebrewParagraph("הערות תשלום:")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(10f)
                .setMarginBottom(5f))
            document.add(createHebrewParagraph(paymentNotes)
                .setFontSize(11f)
                .setMarginBottom(10f))
        }
        
        if (generalNotes.isNotEmpty()) {
            document.add(createHebrewParagraph("הערות כלליות:")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(10f)
                .setMarginBottom(5f))
            document.add(createHebrewParagraph(generalNotes)
                .setFontSize(11f)
                .setMarginBottom(10f))
        }
    }
    
    private fun addSignature(
        document: Document,
        customContent: com.ashaf.instanz.data.models.TemplateCustomContent?
    ) {
        document.add(Paragraph("\n\n"))
        
        val closingText = createHebrewParagraph("בכבוד רב,")
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.RIGHT)
        document.add(closingText)
        
        if (customContent?.company?.isNotEmpty() == true) {
            val companyName = createHebrewParagraph(customContent.company)
                .setFontSize(12f)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(5f)
            document.add(companyName)
        }
    }
    
    // Helper functions
    private fun createHebrewParagraph(text: String): Paragraph {
        val paragraph = Paragraph()
            .setFont(hebrewFont)
            .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
            .setTextAlignment(TextAlignment.RIGHT)
        
        if (containsHebrew(text)) {
            val lines = text.lines()
            lines.forEachIndexed { index, line ->
                val processedLine = reverseHebrewText(line)
                paragraph.add(com.itextpdf.layout.element.Text(processedLine))
                if (index < lines.size - 1) {
                    paragraph.add("\n")
                }
            }
        } else {
            paragraph.add(text)
        }
        
        return paragraph
    }
    
    private fun containsHebrew(text: String): Boolean {
        return text.any { it in '\u0590'..'\u05FF' }
    }
    
    private fun reverseHebrewText(text: String): String {
        if (text.isBlank()) return text
        
        val segments = mutableListOf<TextSegment>()
        var i = 0
        
        while (i < text.length) {
            val char = text[i]
            when {
                char in '\u0590'..'\u05FF' -> {
                    val start = i
                    while (i < text.length && text[i] in '\u0590'..'\u05FF') {
                        i++
                    }
                    segments.add(TextSegment(text.substring(start, i), SegmentType.HEBREW))
                }
                char.isDigit() || (char in 'a'..'z') || (char in 'A'..'Z') || char == '@' || char == '.' || char == ':' || char == '/' || char == '-' -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] in 'a'..'z' || text[i] in 'A'..'Z' || text[i] == '@' || text[i] == '.' || text[i] == ':' || text[i] == '/' || text[i] == '-')) {
                        i++
                    }
                    segments.add(TextSegment(text.substring(start, i), SegmentType.ENGLISH))
                }
                char.isWhitespace() -> {
                    segments.add(TextSegment(char.toString(), SegmentType.SPACE))
                    i++
                }
                else -> {
                    segments.add(TextSegment(char.toString(), SegmentType.PUNCTUATION))
                    i++
                }
            }
        }
        
        val result = StringBuilder()
        segments.reversed().forEach { segment ->
            when (segment.type) {
                SegmentType.HEBREW -> result.append(segment.text.reversed())
                SegmentType.ENGLISH -> result.append(segment.text)
                SegmentType.SPACE, SegmentType.PUNCTUATION -> result.append(segment.text)
            }
        }
        
        return result.toString()
    }
    
    private fun addTableRow(table: Table, label: String, value: String) {
        val labelCell = Cell()
            .add(createHebrewParagraph(label).setBold().setFontSize(11.5f))
            .setBackgroundColor(lightGray)
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 0.5f))
        table.addCell(labelCell)
        
        val valueCell = Cell()
            .add(createHebrewParagraph(value).setFontSize(11.5f))
            .setBackgroundColor(DeviceRgb(255, 255, 255))
            .setPadding(10f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(SolidBorder(borderGray, 0.5f))
        table.addCell(valueCell)
    }
    
    private fun addSummaryRow(table: Table, label: String, value: String) {
        // This function is no longer used - we use individual cells for better control
        // Keeping it for compatibility but it won't be called
    }
    
    private fun createItemCell(text: String, backgroundColor: DeviceRgb): Cell {
        return Cell()
            .add(createHebrewParagraph(text).setFontSize(10.5f))
            .setBackgroundColor(backgroundColor)
            .setPadding(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(SolidBorder(borderGray, 0.5f))
    }
    
    // Helper data classes
    private data class InvoiceItem(
        val description: String,
        val quantity: Double,
        val unit: String,
        val unitPrice: Double,
        val total: Double
    )
    
    private data class TextSegment(val text: String, val type: SegmentType)
    private enum class SegmentType { HEBREW, ENGLISH, SPACE, PUNCTUATION }
    
    // Header/Footer event handler
    inner class InvoiceHeaderFooterEventHandler(
        private val customContent: com.ashaf.instanz.data.models.TemplateCustomContent?,
        private val templateName: String
    ) : IEventHandler {
        override fun handleEvent(event: Event) {
            val docEvent = event as PdfDocumentEvent
            val pdfDoc = docEvent.document
            val page = docEvent.page
            val pageNumber = pdfDoc.getPageNumber(page)
            val totalPages = pdfDoc.numberOfPages
            
            val pageSize = page.pageSize
            val pdfCanvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
            
            Canvas(pdfCanvas, pageSize).use { canvas ->
                // Header - Logo
                customContent?.logoImagePath?.let { imagePath ->
                    try {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            bitmap?.let { bmp ->
                                val compressedImage = compressBitmap(bmp, 500, 150)
                                val imageData = ImageDataFactory.create(compressedImage)
                                val image = Image(imageData)
                                    .setWidth(200f)
                                    .setHeight(60f)
                                
                                val xPosition = (pageSize.width - 200f) / 2
                                val yPosition = pageSize.top - 80f
                                
                                val logoParagraph = Paragraph()
                                    .add(image)
                                    .setFixedPosition(xPosition, yPosition, 200f)
                                
                                canvas.add(logoParagraph)
                            }
                        } else {
                            // File doesn't exist, do nothing
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("InvoicePdfGenerator", "Failed to add logo: ${e.message}")
                    }
                }
                
                // Footer - Contact info
                customContent?.let { content ->
                    val footerText = "טלפון: ${content.phone} | אימייל: ${content.email} | ח.פ: ${content.businessNumber} | אתר: ${content.website}"
                    canvas.showTextAligned(
                        createMixedDirectionParagraph(footerText)
                            .setFontSize(9f)
                            .setTextAlignment(TextAlignment.CENTER),
                        pageSize.width / 2,
                        30f,
                        TextAlignment.CENTER
                    )
                }
                
                // Page number
                val pageNumberText = "עמוד $pageNumber מתוך $totalPages"
                canvas.showTextAligned(
                    createHebrewParagraph(pageNumberText)
                        .setFontSize(11f)
                        .setTextAlignment(TextAlignment.CENTER),
                    pageSize.width / 2,
                    15f,
                    TextAlignment.CENTER
                )
            }
        }
    }
    
    private fun createMixedDirectionParagraph(text: String): Paragraph {
        val paragraph = Paragraph()
            .setFont(hebrewFont)
            .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
            .setTextAlignment(TextAlignment.CENTER)
        
        val LRM = '\u200E'
        val segments = splitIntoBiDiSegments(text)
        val reversedSegments = segments.reversed()
        val finalText = StringBuilder()
        
        reversedSegments.forEach { segment ->
            when (segment.type) {
                SegmentType.HEBREW -> {
                    finalText.append(segment.text.reversed())
                }
                SegmentType.ENGLISH -> {
                    finalText.append("$LRM${segment.text}$LRM")
                }
                SegmentType.SPACE, SegmentType.PUNCTUATION -> {
                    finalText.append(segment.text)
                }
            }
        }
        
        paragraph.add(com.itextpdf.layout.element.Text(finalText.toString()))
        return paragraph
    }
    
    private fun splitIntoBiDiSegments(text: String): List<TextSegment> {
        if (text.isBlank()) return emptyList()
        
        val segments = mutableListOf<TextSegment>()
        var i = 0
        
        while (i < text.length) {
            val char = text[i]
            when {
                char in '\u0590'..'\u05FF' -> {
                    val start = i
                    while (i < text.length && text[i] in '\u0590'..'\u05FF') {
                        i++
                    }
                    segments.add(TextSegment(text.substring(start, i), SegmentType.HEBREW))
                }
                char.isDigit() || (char in 'a'..'z') || (char in 'A'..'Z') || char == '@' || char == '.' || char == ':' || char == '/' || char == '-' -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] in 'a'..'z' || text[i] in 'A'..'Z' || text[i] == '@' || text[i] == '.' || text[i] == ':' || text[i] == '/' || text[i] == '-')) {
                        i++
                    }
                    segments.add(TextSegment(text.substring(start, i), SegmentType.ENGLISH))
                }
                char.isWhitespace() -> {
                    segments.add(TextSegment(char.toString(), SegmentType.SPACE))
                    i++
                }
                else -> {
                    segments.add(TextSegment(char.toString(), SegmentType.PUNCTUATION))
                    i++
                }
            }
        }
        
        return segments
    }
    
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): ByteArray {
        val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }
}

