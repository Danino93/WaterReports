package com.ashaf.instanz.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobImage
import com.ashaf.instanz.data.models.Template
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
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
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(
    private val context: Context,
    private val imagesPerRow: Int = 2,
    private val showHeader: Boolean = true,
    private val jobSettings: com.ashaf.instanz.data.models.JobSettings = com.ashaf.instanz.data.models.JobSettings.default()
) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("he", "IL"))
    private var sectionCounter = 0
    private var subsectionCounter = 0
    
    // Hebrew font for PDF - MUST use embedded font from assets!
    private val hebrewFont: PdfFont by lazy {
        try {
            // First try: Load from assets (BEST OPTION!)
            android.util.Log.d("PdfGenerator", "🔍 Trying to load font from assets...")
            
            // Try multiple font files in order of preference
            val fontFiles = listOf(
                "fonts/NotoSansHebrew-Regular.ttf",  // Best for Hebrew RTL!
                "fonts/Assistant-Regular.ttf",        // Alternative
                "fonts/Heebo-Regular.ttf",            // Alternative
                "fonts/Rubik-Regular.ttf"             // Fallback
            )
            
            for (fontFile in fontFiles) {
                try {
                    val fontStream = context.assets.open(fontFile)
                    val fontBytes = fontStream.readBytes()
                    fontStream.close()
                    
                    android.util.Log.d("PdfGenerator", "✅ Successfully loaded $fontFile from assets!")
                    
                    return@lazy PdfFontFactory.createFont(
                        fontBytes,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                    )
                } catch (e: Exception) {
                    android.util.Log.d("PdfGenerator", "⚠️ Font not found: $fontFile")
                    continue
                }
            }
            
            // Second try: System fonts with better Hebrew support (FALLBACK)
            android.util.Log.d("PdfGenerator", "🔍 Trying system fonts as fallback...")
            val fontPaths = listOf(
                // Hebrew-specific fonts
                "/system/fonts/NotoSansHebrew-Regular.ttf",
                "/system/fonts/NotoSansHebrew-Bold.ttf",
                "/system/fonts/DroidSansHebrew-Regular.ttf",
                "/system/fonts/DroidSansHebrew.ttf",
                // General fonts with Hebrew support
                "/system/fonts/NotoSans-Regular.ttf",
                "/system/fonts/NotoSans-Bold.ttf",
                "/system/fonts/Roboto-Medium.ttf",
                "/system/fonts/Roboto-Bold.ttf",
                "/system/fonts/Roboto-Regular.ttf",
                "/system/fonts/DroidSans-Bold.ttf",
                "/system/fonts/DroidSans.ttf"
            )
            
            for (fontPath in fontPaths) {
                try {
                    val fontFile = File(fontPath)
                    if (fontFile.exists()) {
                        android.util.Log.d("PdfGenerator", "✅ Found system font: $fontPath")
                        
                        // Try to create the font
                        val font = PdfFontFactory.createFont(
                            fontPath,
                            PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                        )
                        
                        android.util.Log.d("PdfGenerator", "✅ Successfully loaded system font: $fontPath")
                        return@lazy font
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PdfGenerator", "⚠️ Failed to load $fontPath: ${e.message}")
                    continue
                }
            }
            
            // Last resort: Helvetica (WON'T WORK FOR HEBREW!)
            android.util.Log.e("PdfGenerator", "❌❌❌ NO HEBREW FONT FOUND! PDF WILL BE BROKEN! ❌❌❌")
            android.util.Log.e("PdfGenerator", ">>> PLEASE ADD Rubik-Regular.ttf TO assets/fonts/ <<<")
            
            PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA,
                PdfEncodings.IDENTITY_H
            )
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "❌ Critical font error: ${e.message}", e)
            throw RuntimeException("Cannot create PDF without Hebrew font!", e)
        }
    }
    
    // Get next section number
    private fun nextSection(): String {
        sectionCounter++
        subsectionCounter = 0
        return "$sectionCounter."
    }
    
    // Get next subsection number  
    private fun nextSubsection(): String {
        subsectionCounter++
        return "$sectionCounter.$subsectionCounter"
    }
    
    // Reset subsection counter (when starting new main section)
    private fun resetSubsection() {
        subsectionCounter = 0
    }
    
    // Helper function to create Hebrew-enabled Paragraph with RTL support
    private fun createHebrewParagraph(text: String): Paragraph {
        // Reverse the text manually for better RTL support with some fonts
        // This is a workaround for fonts that don't handle RTL well
        val processedText = if (containsHebrew(text)) {
            reverseHebrewText(text)
        } else {
            text
        }
        
        return Paragraph(processedText)
            .setFont(hebrewFont)
            .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
            .setTextAlignment(TextAlignment.RIGHT)
    }
    
    // Check if text contains Hebrew characters
    private fun containsHebrew(text: String): Boolean {
        return text.any { it in '\u0590'..'\u05FF' }
    }
    
    // Reverse Hebrew text for fonts that don't handle RTL properly
    private fun reverseHebrewText(text: String): String {
        val result = StringBuilder()
        var i = 0
        
        while (i < text.length) {
            val char = text[i]
            
            // If it's a Hebrew character, collect the Hebrew sequence
            if (char in '\u0590'..'\u05FF') {
                val hebrewSequence = StringBuilder()
                var j = i
                
                // Collect consecutive Hebrew characters and spaces
                while (j < text.length && (text[j] in '\u0590'..'\u05FF' || text[j] == ' ')) {
                    hebrewSequence.append(text[j])
                    j++
                }
                
                // Reverse the Hebrew sequence
                result.append(hebrewSequence.toString().reversed())
                i = j
            } else {
                // Non-Hebrew character, keep as is
                result.append(char)
                i++
            }
        }
        
        return result.toString()
    }
    
    // Helper functions for cells with RTL support
    private fun createLabelCell(text: String): Cell {
        return Cell().add(
            createHebrewParagraph(text)
                .setBold()
                .setFontSize(11f)
        )
            .setTextAlignment(TextAlignment.RIGHT)
            .setBackgroundColor(DeviceRgb(240, 240, 240))
            .setPadding(5f)
    }
    
    private fun createValueCell(text: String): Cell {
        return Cell().add(
            createHebrewParagraph(text)
                .setFontSize(11f)
        )
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(5f)
    }
    
    // Page event handler for headers, footers, and page numbers with RTL support
    inner class HeaderFooterEventHandler(
        private val customContent: com.ashaf.instanz.data.models.TemplateCustomContent?,
        private val templateName: String,
        private val jobNumber: String
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
                // Header - Logo on every page
                customContent?.logoImagePath?.let { imagePath ->
                    try {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            val compressedImage = compressBitmap(bitmap, 500, 150)
                            val imageData = ImageDataFactory.create(compressedImage)
                            val image = Image(imageData)
                                .setWidth(250f)  // הגדלתי מ-120 ל-250
                                .setHeight(80f)   // הגדלתי מ-40 ל-80
                            
                            // Position at top center
                            val xPosition = (pageSize.width - 250f) / 2
                            val yPosition = pageSize.top - 90f
                            
                            val logoParagraph = Paragraph()
                                .add(image)
                                .setFixedPosition(xPosition, yPosition, 250f)
                            
                            canvas.add(logoParagraph)
                        } else {
                            android.util.Log.w("PdfGenerator", "Logo file not found in header: $imagePath")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PdfGenerator", "Failed to add logo to header: ${e.message}")
                    }
                }
                
                // Footer - contact image OR contact info
                customContent?.let { content ->
                    if (content.contactImagePath != null) {
                        // If footer image exists, show it
                        try {
                            val imageFile = File(content.contactImagePath!!)
                            if (imageFile.exists()) {
                                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                                val compressedImage = compressBitmap(bitmap, 400, 100)
                                val imageData = ImageDataFactory.create(compressedImage)
                                val image = Image(imageData)
                                    .setHeight(30f)
                                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                
                                // Position it at bottom center
                                val footerParagraph = Paragraph()
                                    .add(image)
                                    .setFixedPosition(
                                        (pageSize.width - 200f) / 2,
                                        20f,
                                        200f
                                    )
                                canvas.add(footerParagraph)
                            } else {
                                // File doesn't exist, show text footer instead
                                val footerText = "טלפון: ${content.phone} | אימייל: ${content.email} | ח.פ: ${content.businessNumber} | אתר: ${content.website}"
                                canvas.showTextAligned(
                                    createHebrewParagraph(footerText)
                                        .setFontSize(8f)
                                        .setTextAlignment(TextAlignment.CENTER),
                                    pageSize.width / 2,
                                    30f,
                                    TextAlignment.CENTER
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfGenerator", "Failed to load footer image: ${e.message}")
                            // On error, show text footer
                            val footerText = "טלפון: ${content.phone} | אימייל: ${content.email} | ח.פ: ${content.businessNumber} | אתר: ${content.website}"
                            canvas.showTextAligned(
                                createHebrewParagraph(footerText)
                                    .setFontSize(8f)
                                    .setTextAlignment(TextAlignment.CENTER),
                                pageSize.width / 2,
                                30f,
                                TextAlignment.CENTER
                            )
                        }
                    } else {
                        // No footer image, show text footer
                        val footerText = "טלפון: ${content.phone} | אימייל: ${content.email} | ח.פ: ${content.businessNumber} | אתר: ${content.website}"
                        canvas.showTextAligned(
                            createHebrewParagraph(footerText)
                                .setFontSize(8f)
                                .setTextAlignment(TextAlignment.CENTER),
                            pageSize.width / 2,
                            30f,
                            TextAlignment.CENTER
                        )
                    }
                }
                
                // Page number
                canvas.showTextAligned(
                    createHebrewParagraph("עמוד $pageNumber מתוך $totalPages")
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER),
                    pageSize.width / 2,
                    15f,
                    TextAlignment.CENTER
                )
            }
        }
    }
    
    fun generateJobReport(
        job: Job,
        template: Template,
        images: List<JobImage>,
        dataJson: Map<String, Map<String, String>>
    ): File {
        android.util.Log.d("PdfGenerator", "=====================================")
        android.util.Log.d("PdfGenerator", "🚀 generateJobReport STARTED")
        android.util.Log.d("PdfGenerator", "Job Number: ${job.jobNumber}")
        android.util.Log.d("PdfGenerator", "Job dataJson length: ${job.dataJson.length}")
        android.util.Log.d("PdfGenerator", "Job dataJson content: ${job.dataJson.take(500)}")
        android.util.Log.d("PdfGenerator", "Received dataJson parameter: $dataJson")
        android.util.Log.d("PdfGenerator", "Received dataJson size: ${dataJson.size}")
        android.util.Log.d("PdfGenerator", "Received dataJson keys: ${dataJson.keys}")
        android.util.Log.d("PdfGenerator", "=====================================")
        
        // Create PDF file
        val fileName = "Report_${job.jobNumber}_${System.currentTimeMillis()}.pdf"
        val pdfDir = File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val pdfFile = File(pdfDir, fileName)
        
        // Create PDF document
        val writer = PdfWriter(FileOutputStream(pdfFile))
        val pdfDoc = PdfDocument(writer)
        
        // Initialize section counters
        sectionCounter = 0
        subsectionCounter = 0
        
        val document = Document(pdfDoc, PageSize.A4)
        
        // Set margins to accommodate header/footer (top margin increased for logo space)
        document.setMargins(120f, 36f, 50f, 36f)  // Top: 120px for logo + spacing
        
        // Set RTL support
        document.setTextAlignment(TextAlignment.RIGHT)
        
        // Load custom content - prioritize job-specific content, fallback to template defaults
        val customContent = try {
            val gson = Gson()
            val jobDataJson = if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
                gson.fromJson(job.dataJson, JsonObject::class.java)
            } else null
            
            // ALWAYS start with master template as base (for inheritance)
            val masterContent = template.parseCustomContent()
            android.util.Log.d("PdfGenerator", "📋 Loaded master template customContent as base")
            
            if (jobDataJson?.has("customContent") == true) {
                // Smart merge: inherit from master + job-specific overrides
                val customContentElement = jobDataJson.get("customContent")
                
                android.util.Log.d("PdfGenerator", "==== Loading job-specific customContent ====")
                android.util.Log.d("PdfGenerator", "customContent type: ${if (customContentElement.isJsonObject) "JsonObject" else "String"}")
                
                val jobSpecificContent = if (customContentElement.isJsonObject) {
                    android.util.Log.d("PdfGenerator", "✅ Found job-specific overrides (JsonObject)")
                    gson.fromJson(customContentElement, com.ashaf.instanz.data.models.TemplateCustomContent::class.java)
                } else {
                    android.util.Log.d("PdfGenerator", "⚠️ Found job-specific overrides (String - old format)")
                    gson.fromJson(
                        customContentElement.asString,
                        com.ashaf.instanz.data.models.TemplateCustomContent::class.java
                    )
                }
                
                // Smart merge: override only non-null/non-empty fields
                val merged = com.ashaf.instanz.data.models.TemplateCustomContent(
                    logoImagePath = jobSpecificContent?.logoImagePath?.takeIf { it.isNotBlank() } ?: masterContent?.logoImagePath,
                    contactImagePath = jobSpecificContent?.contactImagePath?.takeIf { it.isNotBlank() } ?: masterContent?.contactImagePath,
                    phone = jobSpecificContent?.phone?.takeIf { it.isNotBlank() } ?: masterContent?.phone ?: "",
                    email = jobSpecificContent?.email?.takeIf { it.isNotBlank() } ?: masterContent?.email ?: "",
                    businessNumber = jobSpecificContent?.businessNumber?.takeIf { it.isNotBlank() } ?: masterContent?.businessNumber ?: "",
                    website = jobSpecificContent?.website?.takeIf { it.isNotBlank() } ?: masterContent?.website ?: "",
                    visitReason = jobSpecificContent?.visitReason?.takeIf { it.isNotBlank() } ?: masterContent?.visitReason ?: "",
                    company = jobSpecificContent?.company?.takeIf { it.isNotBlank() } ?: masterContent?.company ?: "",
                    inspectorName = jobSpecificContent?.inspectorName?.takeIf { it.isNotBlank() } ?: masterContent?.inspectorName ?: "",
                    experienceTitle = jobSpecificContent?.experienceTitle?.takeIf { it.isNotBlank() } ?: masterContent?.experienceTitle ?: "",
                    experienceText = jobSpecificContent?.experienceText?.takeIf { it.isNotBlank() } ?: masterContent?.experienceText ?: "",
                    certificateImagePath = jobSpecificContent?.certificateImagePath?.takeIf { it.isNotBlank() } ?: masterContent?.certificateImagePath,
                    disclaimerText = jobSpecificContent?.disclaimerText?.takeIf { it.isNotBlank() } ?: masterContent?.disclaimerText ?: "",
                    sections = if (jobSpecificContent?.sections?.isNotEmpty() == true) jobSpecificContent.sections else masterContent?.sections ?: emptyMap()
                )
                
                android.util.Log.d("PdfGenerator", "🔀 Merged customContent:")
                android.util.Log.d("PdfGenerator", "  - logoImagePath: ${merged.logoImagePath}")
                android.util.Log.d("PdfGenerator", "  - certificateImagePath: ${merged.certificateImagePath}")
                android.util.Log.d("PdfGenerator", "  - phone: ${merged.phone}")
                android.util.Log.d("PdfGenerator", "  - email: ${merged.email}")
                android.util.Log.d("PdfGenerator", "  - inspectorName: ${merged.inspectorName}")
                
                merged
            } else {
                // No job-specific content, use master template
                android.util.Log.d("PdfGenerator", "⚠️ No job-specific customContent, using master template")
                masterContent
            }
        } catch (e: Exception) {
            // Fallback to template defaults
            android.util.Log.e("PdfGenerator", "❌ Error loading customContent: ${e.message}", e)
            template.parseCustomContent()
        }
        
        // Add header/footer event handler for all pages
        pdfDoc.addEventHandler(
            PdfDocumentEvent.END_PAGE,
            HeaderFooterEventHandler(customContent, template.name, job.jobNumber)
        )
        
        // Add content
        addHeader(document, job, template.name, customContent)
        
        // Add top fields (visit reason, company)
        customContent?.let {
            if (it.visitReason.isNotEmpty() || it.company.isNotEmpty()) {
                addTopFields(document, it)
            }
        }
        
        // NEW ORDER: First show client details and general info in a combined table
        android.util.Log.d("PdfGenerator", "📋 Adding combined first page table")
        addCombinedFirstPageTable(
            document,
            template,
            dataJson,
            images,
            job
        )
        
        // THEN show intro report (expert details)
        android.util.Log.d("PdfGenerator", "👨‍💼 Adding intro report (expert details)")
        customContent?.let {
            addIntroReport(document, it)
        }
        
        // FINALLY add all other sections (excluding the ones we already added)
        android.util.Log.d("PdfGenerator", "📄 Adding remaining sections")
        addSections(
            document, 
            template, 
            dataJson, 
            images, 
            job,
            exclude = listOf("client_details", "general_info")  // Exclude what we already added
        )
        
        // Add intro work/activities/recommendations from custom template
        customContent?.let {
            addCustomSection(document, "הקדמה - עבודה", it.sections["intro_work"])
            addCustomSection(document, "פעילות הקדמה", it.sections["intro_activities"])
            addCustomSection(document, "המלצת הקדמה", it.sections["intro_recommendations"])
        }
        
        // Add findings and recommendations
        addFindings(document, job, images)
        
        // Add recommendations summary with price calculation
        addRecommendationsSummary(document, job)
        
        // Add summary sections from custom template
        customContent?.let {
            addCustomSection(document, "סיכום - עבודה", it.sections["work_summary"])
            addCustomSection(document, "סיכום - דו\"ח", it.sections["report_summary"])
            addCustomSection(document, "פעילות סיכום", it.sections["summary_activities"])
            addCustomSection(document, "המלצת סיכום", it.sections["summary_recommendations"])
            addCustomSection(document, "בסיום", it.sections["conclusion"])
        }
        
        addFooter(document, job, customContent)
        
        document.close()
        
        android.util.Log.d("PdfGenerator", "=====================================")
        android.util.Log.d("PdfGenerator", "✅ PDF GENERATION COMPLETED!")
        android.util.Log.d("PdfGenerator", "File path: ${pdfFile.absolutePath}")
        android.util.Log.d("PdfGenerator", "File size: ${pdfFile.length()} bytes")
        android.util.Log.d("PdfGenerator", "File exists: ${pdfFile.exists()}")
        android.util.Log.d("PdfGenerator", "=====================================")
        
        return pdfFile
    }
    
    private fun addHeader(
        document: Document, 
        job: Job, 
        templateName: String,
        customContent: com.ashaf.instanz.data.models.TemplateCustomContent? = null
    ) {
        // Logo is now added to every page via HeaderFooterEventHandler
        // Minimal spacing - logo already provides top margin
        
        // Title with "דוח" prefix - compact version without date
        val title = createHebrewParagraph("דוח $templateName")
            .setFontSize(22f)  // Slightly smaller for more compact look
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(5f)   // Small top margin
            .setMarginBottom(10f)  // Reduced bottom margin
            .setFontColor(DeviceRgb(25, 118, 210))
        document.add(title)
    }
    
    private fun addTopFields(document: Document, customContent: com.ashaf.instanz.data.models.TemplateCustomContent) {
        // Removed visit reason and company display to keep the report clean and compact
        // These details are already in the footer contact info
    }
    
    // Special function for first page: combined table with client_details + general_info side by side
    private fun addCombinedFirstPageTable(
        document: Document,
        template: Template,
        dataJson: Map<String, Map<String, String>>,
        images: List<JobImage>,
        job: Job
    ) {
        try {
            val gson = Gson()
            val templateObj = gson.fromJson(template.jsonData, JsonObject::class.java)
            val sectionsArray = templateObj.getAsJsonArray("sections") ?: return
            
            var clientSection: JsonObject? = null
            var generalSection: JsonObject? = null
            
            // Find the two sections
            sectionsArray.forEach { element ->
                val sectionObj = element.asJsonObject
                val sectionId = sectionObj.get("id")?.asString
                when (sectionId) {
                    "client_details" -> clientSection = sectionObj
                    "general_info" -> generalSection = sectionObj
                }
            }
            
            if (clientSection == null || generalSection == null) {
                android.util.Log.w("PdfGenerator", "Could not find client_details or general_info sections")
                return
            }
            
            // Create a professional table like in the old report
            val combinedTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 30f, 20f, 30f)))
                .useAllAvailableWidth()
                .setMarginBottom(20f)
                .setBorder(SolidBorder(DeviceRgb(100, 100, 100), 2f))
            
            // Get data
            val clientData = dataJson["client_details"] ?: emptyMap()
            val generalData = dataJson["general_info"] ?: emptyMap()
            val clientFields = clientSection!!.getAsJsonArray("fields")
            val generalFields = generalSection!!.getAsJsonArray("fields")
            
            // Header row with section titles
            val clientHeaderCell = Cell(1, 2)
                .add(createHebrewParagraph("מזמין הבדיקה").setBold().setFontSize(12f))
                .setBackgroundColor(DeviceRgb(220, 220, 220))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8f)
                .setBorder(SolidBorder(DeviceRgb(100, 100, 100), 1f))
            
            val generalHeaderCell = Cell(1, 2)
                .add(createHebrewParagraph("מידע כללי").setBold().setFontSize(12f))
                .setBackgroundColor(DeviceRgb(220, 220, 220))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8f)
                .setBorder(SolidBorder(DeviceRgb(100, 100, 100), 1f))
            
            combinedTable.addCell(generalHeaderCell)  // Right side first (RTL)
            combinedTable.addCell(clientHeaderCell)   // Left side
            
            // Collect all fields
            val clientFieldsList = mutableListOf<Pair<String, String>>()
            clientFields?.forEach { fieldElement ->
                val fieldObj = fieldElement.asJsonObject
                val fieldId = fieldObj.get("id")?.asString ?: ""
                val fieldLabel = fieldObj.get("label")?.asString ?: ""
                val fieldType = fieldObj.get("type")?.asString ?: ""
                
                if (fieldType in listOf("text", "textarea", "number")) {
                    val value = clientData[fieldId] ?: ""
                    if (value.isNotBlank()) {
                        clientFieldsList.add(Pair(fieldLabel, value))
                    }
                }
            }
            
            // Collect general fields (including date)
            val generalFieldsList = mutableListOf<Pair<String, String>>()
            val formattedDate = dateFormat.format(Date(job.dateCreated))
            generalFieldsList.add(Pair("תאריך", formattedDate))
            
            generalFields?.forEach { fieldElement ->
                val fieldObj = fieldElement.asJsonObject
                val fieldId = fieldObj.get("id")?.asString ?: ""
                val fieldLabel = fieldObj.get("label")?.asString ?: ""
                val fieldType = fieldObj.get("type")?.asString ?: ""
                
                if (fieldType in listOf("text", "textarea", "number")) {
                    val value = generalData[fieldId] ?: ""
                    if (value.isNotBlank()) {
                        generalFieldsList.add(Pair(fieldLabel, value))
                    }
                }
            }
            
            // Fill table rows (side by side, alternating)
            val maxRows = maxOf(generalFieldsList.size, clientFieldsList.size)
            for (i in 0 until maxRows) {
                val generalPair = generalFieldsList.getOrNull(i)
                val clientPair = clientFieldsList.getOrNull(i)
                
                // General Info cells (right side - RTL)
                if (generalPair != null) {
                    val labelCell = Cell()
                        .add(createHebrewParagraph(generalPair.first).setFontSize(10f).setBold())
                        .setBackgroundColor(DeviceRgb(240, 240, 240))
                        .setPadding(6f)
                        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f))
                        .setTextAlignment(TextAlignment.RIGHT)
                    
                    val valueCell = Cell()
                        .add(createHebrewParagraph(generalPair.second).setFontSize(10f))
                        .setPadding(6f)
                        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f))
                        .setTextAlignment(TextAlignment.RIGHT)
                    
                    combinedTable.addCell(labelCell)
                    combinedTable.addCell(valueCell)
                } else {
                    combinedTable.addCell(Cell().setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f)))
                    combinedTable.addCell(Cell().setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f)))
                }
                
                // Client Details cells (left side)
                if (clientPair != null) {
                    val labelCell = Cell()
                        .add(createHebrewParagraph(clientPair.first).setFontSize(10f).setBold())
                        .setBackgroundColor(DeviceRgb(240, 240, 240))
                        .setPadding(6f)
                        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f))
                        .setTextAlignment(TextAlignment.RIGHT)
                    
                    val valueCell = Cell()
                        .add(createHebrewParagraph(clientPair.second).setFontSize(10f))
                        .setPadding(6f)
                        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f))
                        .setTextAlignment(TextAlignment.RIGHT)
                    
                    combinedTable.addCell(labelCell)
                    combinedTable.addCell(valueCell)
                } else {
                    combinedTable.addCell(Cell().setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f)))
                    combinedTable.addCell(Cell().setBorder(SolidBorder(DeviceRgb(150, 150, 150), 0.5f)))
                }
            }
            
            document.add(combinedTable)
            
            // Add general_image below the table if exists (limited to fit first page)
            val generalImages = images.filter { it.sectionId == "general_info" }
            if (generalImages.isNotEmpty()) {
                addCoverImageField(document, "תמונת שער", generalImages)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error creating combined first page table: ${e.message}", e)
        }
    }
    
    // Removed addClientInfo - now handled by addSections which reads from dataJson
    
    private fun addSections(
        document: Document,
        template: Template,
        dataJson: Map<String, Map<String, String>>,
        images: List<JobImage>,
        job: Job,
        onlyInclude: List<String>? = null,  // If specified, only these section IDs
        exclude: List<String>? = null        // If specified, exclude these section IDs
    ) {
        android.util.Log.d("PdfGenerator", "========== addSections START ==========")
        android.util.Log.d("PdfGenerator", "dataJson received: $dataJson")
        android.util.Log.d("PdfGenerator", "dataJson size: ${dataJson.size}")
        android.util.Log.d("PdfGenerator", "onlyInclude: $onlyInclude, exclude: $exclude")
        try {
            val gson = Gson()
            val templateObj = gson.fromJson(template.jsonData, JsonObject::class.java)
            val sectionsArray = templateObj.getAsJsonArray("sections") ?: return
            
            val sections = mutableListOf<JsonObject>()
            sectionsArray.forEach { element ->
                sections.add(element.asJsonObject)
            }
            sections.sortBy { it.get("order")?.asInt ?: 0 }
            
            sections.forEach { sectionJson ->
                val sectionId = sectionJson.get("id")?.asString ?: return@forEach
                
                // Filter by onlyInclude or exclude
                if (onlyInclude != null && sectionId !in onlyInclude) {
                    android.util.Log.d("PdfGenerator", "⏭️ Skipping section '$sectionId' (not in onlyInclude)")
                    return@forEach
                }
                if (exclude != null && sectionId in exclude) {
                    android.util.Log.d("PdfGenerator", "⏭️ Skipping section '$sectionId' (in exclude list)")
                    return@forEach
                }
                
                addSection(document, sectionJson, dataJson, images, job)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun addSection(
        document: Document,
        sectionJson: JsonObject,
        dataJson: Map<String, Map<String, String>>,
        images: List<JobImage>,
        job: Job
    ) {
        val sectionId = sectionJson.get("id")?.asString ?: return
        val sectionTitle = sectionJson.get("title")?.asString ?: ""
        val fieldsJson = sectionJson.getAsJsonArray("fields") ?: return
        
        android.util.Log.d("PdfGenerator", "==== Processing Section ====")
        android.util.Log.d("PdfGenerator", "Section ID: $sectionId")
        android.util.Log.d("PdfGenerator", "Section Title: $sectionTitle")
        android.util.Log.d("PdfGenerator", "Available sections in dataJson: ${dataJson.keys}")
        
        // Add page break before each section (except first one) to avoid splitting
        if (sectionCounter > 0) {
            document.add(com.itextpdf.layout.element.AreaBreak(com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE))
            android.util.Log.d("PdfGenerator", "📄 Added page break before section: $sectionTitle")
        }
        
        // Section Title with colored background - like in the example
        val sectionNumber = nextSection()
        
        // Create a table for the section title with colored background
        val titleTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .useAllAvailableWidth()
            .setMarginTop(15f)
            .setMarginBottom(10f)
        
        val titleCell = Cell().add(
            createHebrewParagraph("$sectionNumber $sectionTitle")
                .setFontSize(16f)
                .setBold()
                .setFontColor(ColorConstants.BLACK)
        )
        .setBackgroundColor(DeviceRgb(178, 223, 219)) // Light teal like in example
        .setPadding(10f)
        .setTextAlignment(TextAlignment.RIGHT)
        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 1f))
        
        titleTable.addCell(titleCell)
        document.add(titleTable)
        
        // Section Fields
        val sectionData = dataJson[sectionId] ?: emptyMap()
        android.util.Log.d("PdfGenerator", "Section data for '$sectionId': $sectionData")
        
        // Add date as first field in "מידע כללי" section
        if (sectionId == "general_info") {
            val formattedDate = dateFormat.format(Date(job.dateCreated))
            addTextField(document, "תאריך הבדיקה", formattedDate)
            android.util.Log.d("PdfGenerator", "  📅 Added date field: $formattedDate")
        }
        
        fieldsJson.forEach { fieldJsonElement ->
            val fieldObj = fieldJsonElement.asJsonObject
            val fieldType = fieldObj.get("type")?.asString ?: ""
            val fieldId = fieldObj.get("id")?.asString ?: ""
            val fieldLabel = fieldObj.get("label")?.asString ?: ""
            
            android.util.Log.d("PdfGenerator", "  Field: $fieldId | Label: $fieldLabel | Type: $fieldType")
            
            when (fieldType) {
                "text", "textarea", "number" -> {
                    val value = sectionData[fieldId] ?: ""
                    android.util.Log.d("PdfGenerator", "    Value for '$fieldId': '$value' | isBlank: ${value.isBlank()}")
                    if (value.isNotBlank()) {
                        addTextField(document, fieldLabel, value)
                        android.util.Log.d("PdfGenerator", "    ✓ Added text field")
                    } else {
                        android.util.Log.w("PdfGenerator", "    ✗ Skipped (blank value)")
                    }
                }
                "checkbox" -> {
                    val value = sectionData[fieldId] ?: "false"
                    val isChecked = value.toBooleanStrictOrNull() ?: false
                    addCheckboxField(document, fieldLabel, isChecked)
                }
                "image" -> {
                    val fieldImages = images.filter { 
                        it.sectionId == sectionId
                    }
                    if (fieldImages.isNotEmpty()) {
                        addImageField(document, fieldLabel, fieldImages)
                    }
                }
            }
        }
        
        document.add(Paragraph("\n"))
    }
    
    // Helper functions for rendering fields with beautiful tables
    private fun addTextField(document: Document, label: String, value: String) {
        android.util.Log.d("PdfGenerator", "  📝 addTextField: '$label' = '$value'")
        
        // Use table for better formatting
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .useAllAvailableWidth()
            .setMarginBottom(5f)
            .setKeepTogether(true)  // Prevent table from breaking across pages
        
        table.addCell(createLabelCell(label))
        table.addCell(createValueCell(value))
        
        document.add(table)
        android.util.Log.d("PdfGenerator", "  ✅ addTextField complete")
    }
    
    private fun addCheckboxField(document: Document, label: String, checked: Boolean) {
        val checkSymbol = if (checked) "☑" else "☐"
        val para = createHebrewParagraph("$checkSymbol $label")
            .setFontSize(11f)
            .setMarginBottom(5f)
        document.add(para)
    }
    
    private fun addCoverImageField(document: Document, label: String, images: List<JobImage>) {
        // Special handling for cover image - limit to first page with border
        images.forEach { jobImage ->
            try {
                val imageFile = File(jobImage.filePath)
                if (imageFile.exists()) {
                    // Load and compress image with proper rotation
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    val rotatedBitmap = rotateImageIfNeeded(bitmap, imageFile.absolutePath)
                    val compressedImage = compressBitmap(rotatedBitmap, 800, 600)
                    
                    val imageData = ImageDataFactory.create(compressedImage)
                    val image = Image(imageData)
                        .setMaxWidth(UnitValue.createPercentValue(80f))
                        .setMaxHeight(300f) // Limit height to fit first page
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    
                    // Create a table to add border around the image
                    val imageTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                        .useAllAvailableWidth()
                        .setWidth(UnitValue.createPercentValue(85f))
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginTop(10f)
                        .setMarginBottom(15f)
                    
                    val imageCell = Cell()
                        .add(image)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(10f)
                        .setBorder(com.itextpdf.layout.borders.SolidBorder(DeviceRgb(200, 200, 200), 2f))
                        .setBackgroundColor(DeviceRgb(250, 250, 250))
                    
                    imageTable.addCell(imageCell)
                    document.add(imageTable)
                    
                    // Add caption if exists
                    jobImage.caption?.let { caption ->
                        if (caption.isNotBlank()) {
                            val captionPara = createHebrewParagraph(caption)
                                .setFontSize(10f)
                                .setItalic()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(10f)
                            document.add(captionPara)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfGenerator", "Error adding cover image: ${e.message}", e)
            }
        }
    }
    
    private fun addImageField(document: Document, label: String, images: List<JobImage>) {
        val labelPara = createHebrewParagraph(label)
            .setFontSize(12f)
            .setBold()
            .setMarginTop(10f)
            .setMarginBottom(5f)
        document.add(labelPara)
        
        images.forEach { jobImage ->
            try {
                val imageFile = File(jobImage.filePath)
                if (imageFile.exists()) {
                    // Load and compress image with proper rotation
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    val rotatedBitmap = rotateImageIfNeeded(bitmap, imageFile.absolutePath)
                    val compressedImage = compressBitmap(rotatedBitmap, 800, 600)
                    
                    val imageData = ImageDataFactory.create(compressedImage)
                    val image = Image(imageData)
                        .setMaxWidth(UnitValue.createPercentValue(80f))
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginBottom(10f)
                    
                    document.add(image)
                    
                    // Add caption if exists
                    jobImage.caption?.let { caption ->
                        if (caption.isNotBlank()) {
                            val captionPara = createHebrewParagraph(caption)
                                .setFontSize(10f)
                                .setItalic()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(10f)
                            document.add(captionPara)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfGenerator", "Error adding image: ${e.message}", e)
            }
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addIntroReport(document: Document, customContent: com.ashaf.instanz.data.models.TemplateCustomContent) {
        // Add company information if provided (only show if experienceText exists)
        if (customContent.experienceText.isNotEmpty()) {
            val sectionNumber = nextSection()
            
            // Modern section header with gradient-like effect
            val sectionTitle = createHebrewParagraph("$sectionNumber אודות החברה")
                .setFontSize(20f)
                .setBold()
                .setFontColor(DeviceRgb(0, 102, 204))  // Modern blue
                .setMarginTop(20f)
                .setMarginBottom(15f)
            document.add(sectionTitle)
            
            // Add a subtle separator line
            val separator = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .useAllAvailableWidth()
                .setMarginBottom(15f)
            separator.addCell(
                Cell()
                    .setHeight(3f)
                    .setBackgroundColor(DeviceRgb(0, 102, 204))
                    .setBorder(Border.NO_BORDER)
            )
            document.add(separator)
            
            android.util.Log.d("PdfGenerator", "📝 Adding company info (experienceText):")
            android.util.Log.d("PdfGenerator", "  - experienceText length: ${customContent.experienceText.length}")
            android.util.Log.d("PdfGenerator", "  - experienceText preview: ${customContent.experienceText.take(100)}")
            
            // Company info in a nice card with double-border shadow effect
            val shadowTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .useAllAvailableWidth()
                .setMarginBottom(20f)
            
            // Outer shadow layer
            val shadowCell = Cell()
                .setPadding(3f)
                .setBackgroundColor(DeviceRgb(230, 230, 230))
                .setBorder(Border.NO_BORDER)
            
            // Inner content card
            val infoCard = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .useAllAvailableWidth()
            
            val cardCell = Cell()
                .add(createHebrewParagraph(customContent.experienceText)
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMultipliedLeading(1.5f))  // Line height = 1.5x
                .setPadding(18f)  // Extra padding for better look
                .setBackgroundColor(DeviceRgb(248, 249, 250))  // Light gray background
                .setBorder(SolidBorder(DeviceRgb(0, 102, 204), 1f))  // Blue border
            
            infoCard.addCell(cardCell)
            shadowCell.add(infoCard)
            shadowTable.addCell(shadowCell)
            document.add(shadowTable)
            
            // Add certificate image if exists
            customContent.certificateImagePath?.let { imagePath ->
                try {
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        android.util.Log.d("PdfGenerator", "📷 Adding certificate image: $imagePath")
                        val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                        val compressedImage = compressBitmap(bitmap, 400, 300)
                        
                        val imageData = com.itextpdf.io.image.ImageDataFactory.create(compressedImage)
                        val image = com.itextpdf.layout.element.Image(imageData)
                            .setMaxWidth(UnitValue.createPercentValue(60f))
                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                            .setMarginBottom(15f)
                        
                        document.add(image)
                    } else {
                        android.util.Log.w("PdfGenerator", "⚠️ Certificate image file not found: $imagePath")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfGenerator", "❌ Error adding certificate image", e)
                    e.printStackTrace()
                }
            }
            
            document.add(Paragraph("\n"))
        }
        
        // Add intro report items
        customContent.sections["intro_report"]?.let { items ->
            if (items.isNotEmpty()) {
                addCustomSection(document, "הקדמה - דו\"ח", items)
            }
        }
    }
    
    private fun addCustomSection(
        document: Document, 
        title: String, 
        items: List<com.ashaf.instanz.data.models.TemplateSectionItem>?,
        addSectionNumber: Boolean = true,
        isSubsection: Boolean = false
    ) {
        if (items.isNullOrEmpty()) return
        
        // Get section number
        val sectionNumber = if (addSectionNumber) {
            if (isSubsection) nextSubsection() else nextSection()
        } else ""
        
        val fullTitle = if (sectionNumber.isNotEmpty()) "$sectionNumber $title" else title
        
        val sectionTitle = createHebrewParagraph(fullTitle)
            .setFontSize(18f)
            .setBold()
            .setFontColor(DeviceRgb(25, 118, 210))
            .setMarginTop(15f)
            .setMarginBottom(10f)
        document.add(sectionTitle)
        
        items.sortedBy { it.order }.forEach { item ->
            val para = createHebrewParagraph(item.text)
                .setFontSize(11f)
                .setMarginBottom(8f)
                .setTextAlignment(TextAlignment.RIGHT)
            document.add(para)
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addFindings(document: Document, job: Job, images: List<JobImage>) {
        try {
            val gson = Gson()
            val jobDataJson = if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
                gson.fromJson(job.dataJson, JsonObject::class.java)
            } else {
                return
            }
            
            // Check if there are findings
            if (!jobDataJson.has("findings")) {
                return
            }
            
            val findingsArray = jobDataJson.getAsJsonArray("findings")
            if (findingsArray.size() == 0) {
                return
            }
            
            // Add findings section title
            val sectionNumber = nextSection()
            val sectionTitle = createHebrewParagraph("$sectionNumber ממצאים")
                .setFontSize(22f)
                .setBold()
                .setFontColor(DeviceRgb(25, 118, 210))
                .setMarginTop(20f)
                .setMarginBottom(15f)
            document.add(sectionTitle)
            
            android.util.Log.d("PdfGenerator", "📋 Adding findings, total images available: ${images.size}")
            
            // Iterate through findings
            findingsArray.forEachIndexed { index, findingElement ->
                val findingId = findingElement.asString
                val findingObj = jobDataJson.getAsJsonObject(findingId) ?: return@forEachIndexed
                
                // Get subject for header
                val subject = findingObj.get("finding_subject")?.asString ?: ""
                val findingNumber = index + 1
                
                // Use subject as header (or fallback to "ממצא #X" if no subject)
                val headerText = if (subject.isNotBlank()) {
                    "$findingNumber. $subject"  // e.g., "1. רטיבות בתקרה"
                } else {
                    "ממצא #$findingNumber"
                }
                
                // Modern finding header with colored background
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                    .useAllAvailableWidth()
                    .setMarginTop(15f)
                    .setMarginBottom(10f)
                
                val headerCell = Cell()
                    .add(createHebrewParagraph(headerText)
                        .setFontSize(16f)
                        .setBold()
                        .setFontColor(DeviceRgb(255, 255, 255)))  // White text
                    .setBackgroundColor(DeviceRgb(0, 150, 136))  // Teal background
                    .setPadding(12f)
                    .setBorder(Border.NO_BORDER)
                
                headerTable.addCell(headerCell)
                document.add(headerTable)
                
                // Modern finding details card
                val category = findingObj.get("finding_category")?.asString ?: ""
                if (category.isNotBlank()) {
                    val detailsCard = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                        .useAllAvailableWidth()
                        .setMarginBottom(12f)
                    
                    val detailsCell = Cell()
                        .add(createHebrewParagraph("תת נושא: $category")
                            .setFontSize(11f)
                            .setBold()
                            .setFontColor(DeviceRgb(100, 100, 100)))
                        .setPadding(10f)  // More padding
                        .setBackgroundColor(DeviceRgb(240, 248, 255))  // Light blue
                        .setBorder(SolidBorder(DeviceRgb(0, 150, 136), 2f))  // Thicker border
                    
                    detailsCard.addCell(detailsCell)
                    document.add(detailsCard)
                }
                
                // Description in a modern card
                val description = findingObj.get("finding_description")?.asString ?: ""
                if (description.isNotBlank()) {
                    val descCard = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                        .useAllAvailableWidth()
                        .setMarginBottom(12f)
                    
                    val descCell = Cell()
                        .add(createHebrewParagraph("📋 תיאור הבעיה")
                            .setBold()
                            .setFontSize(12f)
                            .setFontColor(DeviceRgb(50, 50, 50))
                            .setMarginBottom(10f))
                        .add(createHebrewParagraph(description)
                            .setFontSize(11f)
                            .setMultipliedLeading(1.4f))  // Line height = 1.4x
                        .setPadding(15f)  // More padding for depth
                        .setBackgroundColor(DeviceRgb(255, 255, 255))
                        .setBorder(SolidBorder(DeviceRgb(180, 180, 180), 2f))  // Thicker border
                    
                    descCard.addCell(descCell)
                    document.add(descCard)
                }
                
                // Note in a highlighted card
                val note = findingObj.get("finding_note")?.asString ?: ""
                if (note.isNotBlank()) {
                    val noteCard = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                        .useAllAvailableWidth()
                        .setMarginBottom(12f)
                    
                    val noteCell = Cell()
                        .add(createHebrewParagraph("⚠️ הערה חשובה")
                            .setBold()
                            .setFontSize(12f)
                            .setFontColor(DeviceRgb(255, 152, 0))  // Orange
                            .setMarginBottom(10f))
                        .add(createHebrewParagraph(note)
                            .setFontSize(11f)
                            .setMultipliedLeading(1.4f))  // Line height = 1.4x
                        .setPadding(15f)  // More padding
                        .setBackgroundColor(DeviceRgb(255, 248, 225))  // Light orange
                        .setBorder(SolidBorder(DeviceRgb(255, 152, 0), 3f))  // Thicker border for emphasis!
                    
                    noteCard.addCell(noteCell)
                    document.add(noteCard)
                }
                
                // Recommendations for this finding
                val recommendationsJson = findingObj.get("recommendations")?.asString ?: ""
                if (recommendationsJson.isNotBlank()) {
                    try {
                        val recommendations = gson.fromJson(recommendationsJson, com.google.gson.JsonArray::class.java)
                        if (recommendations != null && recommendations.size() > 0) {
                            // Modern recommendations header
                            val recHeaderTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                                .useAllAvailableWidth()
                                .setMarginTop(12f)
                                .setMarginBottom(10f)
                            
                            val recHeaderCell = Cell()
                                .add(createHebrewParagraph("💡 המלצות לטיפול")
                                    .setBold()
                                    .setFontSize(13f)
                                    .setFontColor(DeviceRgb(255, 255, 255)))
                                .setBackgroundColor(DeviceRgb(255, 152, 0))  // Orange
                                .setPadding(10f)
                                .setBorder(Border.NO_BORDER)
                            
                            recHeaderTable.addCell(recHeaderCell)
                            document.add(recHeaderTable)
                            
                            // Modern recommendations table with styled headers
                            val recTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 35f, 10f, 15f, 15f, 20f)))
                                .useAllAvailableWidth()
                                .setMarginBottom(15f)
                            
                            // Styled table headers
                            val headerColor = DeviceRgb(70, 130, 180)  // Steel blue
                            val headerCells = listOf("#", "תיאור", "כמות", "יחידה", "מחיר יחידה", "מחיר כולל")
                            headerCells.forEach { headerText ->
                                val headerCell = Cell()
                                    .add(createHebrewParagraph(headerText)
                                        .setBold()
                                        .setFontSize(10f)
                                        .setFontColor(DeviceRgb(255, 255, 255)))
                                    .setBackgroundColor(headerColor)
                                    .setPadding(8f)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setBorder(SolidBorder(DeviceRgb(255, 255, 255), 1f))
                                recTable.addHeaderCell(headerCell)
                            }
                            
                            // Add recommendations with alternating row colors
                            recommendations.forEachIndexed { recIndex, recElement ->
                                val recObj = recElement.asJsonObject
                                val recDescription = recObj.get("description")?.asString ?: ""
                                val recQuantity = recObj.get("quantity")?.asString ?: ""
                                val recUnit = recObj.get("unit")?.asString ?: ""
                                val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                                val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                                
                                // Alternating row colors for better readability
                                val rowColor = if (recIndex % 2 == 0) DeviceRgb(248, 249, 250) else DeviceRgb(255, 255, 255)
                                
                                val cells = listOf(
                                    "${recIndex + 1}",
                                    recDescription,
                                    recQuantity,
                                    recUnit,
                                    if (recPricePerUnit.isNotBlank()) "₪$recPricePerUnit" else "",
                                    if (recTotalPrice.isNotBlank()) "₪$recTotalPrice" else ""
                                )
                                
                                cells.forEach { cellText ->
                                    val cell = Cell()
                                        .add(createHebrewParagraph(cellText).setFontSize(10f))
                                        .setBackgroundColor(rowColor)
                                        .setPadding(6f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setBorder(SolidBorder(DeviceRgb(220, 220, 220), 0.5f))
                                    recTable.addCell(cell)
                                }
                            }
                            
                            document.add(recTable)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Add images for this finding
                val findingImages = images.filter { it.sectionId == findingId }
                android.util.Log.d("PdfGenerator", "📸 Finding $findingId has ${findingImages.size} images")
                
                if (findingImages.isNotEmpty()) {
                    val imagesLabel = createHebrewParagraph("תמונות:")
                        .setBold()
                        .setMarginTop(10f)
                        .setMarginBottom(8f)
                    document.add(imagesLabel)
                    
                    findingImages.forEach { jobImage ->
                        try {
                            val imageFile = File(jobImage.filePath)
                            if (imageFile.exists()) {
                                android.util.Log.d("PdfGenerator", "✅ Adding image: ${jobImage.filePath}")
                                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                                val rotatedBitmap = rotateImageIfNeeded(bitmap, imageFile.absolutePath)
                                val compressedImage = compressBitmap(rotatedBitmap, 800, 600)
                                
                                val imageData = ImageDataFactory.create(compressedImage)
                                val image = Image(imageData)
                                    .setMaxWidth(UnitValue.createPercentValue(70f))
                                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                    .setMarginBottom(10f)
                                
                                document.add(image)
                                
                                // Add caption if exists
                                jobImage.caption?.let { caption ->
                                    if (caption.isNotBlank()) {
                                        val captionPara = createHebrewParagraph(caption)
                                            .setFontSize(10f)
                                            .setItalic()
                                            .setTextAlignment(TextAlignment.CENTER)
                                            .setMarginBottom(10f)
                                        document.add(captionPara)
                                    }
                                }
                            } else {
                                android.util.Log.w("PdfGenerator", "⚠️ Image file not found: ${jobImage.filePath}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfGenerator", "❌ Error adding finding image", e)
                        }
                    }
                }
                
                // Add separator between findings
                if (index < findingsArray.size() - 1) {
                    document.add(Paragraph("\n"))
                }
            }
            
            document.add(Paragraph("\n"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun addRecommendationsSummary(document: Document, job: Job) {
        try {
            val gson = Gson()
            val jobDataJson = if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
                gson.fromJson(job.dataJson, JsonObject::class.java)
            } else {
                return
            }
            
            // Check if there are findings
            if (!jobDataJson.has("findings")) {
                return
            }
            
            val findingsArray = jobDataJson.getAsJsonArray("findings")
            if (findingsArray.size() == 0) {
                return
            }
            
            // Collect all recommendations from all findings
            val allRecommendations = mutableListOf<Triple<Int, String, JsonObject>>() // <findingNumber, findingSubject, recObj>
            
            findingsArray.forEachIndexed { index, findingElement ->
                val findingId = findingElement.asString
                val findingObj = jobDataJson.getAsJsonObject(findingId) ?: return@forEachIndexed
                val findingSubject = findingObj.get("finding_subject")?.asString ?: "ממצא ${index + 1}"
                
                val recommendationsJson = findingObj.get("recommendations")?.asString ?: ""
                if (recommendationsJson.isNotBlank()) {
                    try {
                        val recommendations = gson.fromJson(recommendationsJson, com.google.gson.JsonArray::class.java)
                        recommendations?.forEach { recElement ->
                            val recObj = recElement.asJsonObject
                            allRecommendations.add(Triple(index + 1, findingSubject, recObj))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // If no recommendations, return
            if (allRecommendations.isEmpty()) {
                return
            }
            
            // Modern section header
            val sectionNumber = nextSection()
            val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .useAllAvailableWidth()
                .setMarginTop(25f)
                .setMarginBottom(15f)
            
            val headerCell = Cell()
                .add(createHebrewParagraph("$sectionNumber 💰 סיכום המלצות וריכוז מחירים")
                    .setFontSize(18f)
                    .setBold()
                    .setFontColor(DeviceRgb(255, 255, 255)))
                .setBackgroundColor(DeviceRgb(255, 87, 34))  // Deep orange
                .setPadding(15f)
                .setBorder(Border.NO_BORDER)
            
            headerTable.addCell(headerCell)
            document.add(headerTable)
            
            // Modern summary table with styled headers
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 20f, 25f, 10f, 12f, 13f, 15f)))
                .useAllAvailableWidth()
                .setMarginBottom(15f)
            
            // Styled table headers
            val headerColor = DeviceRgb(52, 73, 94)  // Dark blue-gray
            val headers = listOf("#", "ממצא", "תיאור", "כמות", "יחידה", "מחיר יחידה", "מחיר כולל")
            headers.forEach { headerText ->
                val cell = Cell()
                    .add(createHebrewParagraph(headerText)
                        .setBold()
                        .setFontSize(10f)
                        .setFontColor(DeviceRgb(255, 255, 255)))
                    .setBackgroundColor(headerColor)
                    .setPadding(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(SolidBorder(DeviceRgb(255, 255, 255), 1f))
                summaryTable.addHeaderCell(cell)
            }
            
            // Add all recommendations with alternating row colors
            var subtotal = 0.0
            allRecommendations.forEachIndexed { index, (findingNum, findingSubject, recObj) ->
                val recDescription = recObj.get("description")?.asString ?: ""
                val recQuantity = recObj.get("quantity")?.asString ?: ""
                val recUnit = recObj.get("unit")?.asString ?: ""
                val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                
                // Calculate subtotal
                val totalPrice = recTotalPrice.toDoubleOrNull() ?: 0.0
                subtotal += totalPrice
                
                // Alternating row colors
                val rowColor = if (index % 2 == 0) DeviceRgb(245, 245, 245) else DeviceRgb(255, 255, 255)
                
                val cells = listOf(
                    "${index + 1}",
                    findingSubject,
                    recDescription,
                    recQuantity,
                    recUnit,
                    if (recPricePerUnit.isNotBlank()) "₪$recPricePerUnit" else "",
                    if (recTotalPrice.isNotBlank()) "₪$recTotalPrice" else ""
                )
                
                cells.forEach { cellText ->
                    val cell = Cell()
                        .add(createHebrewParagraph(cellText).setFontSize(9f))
                        .setBackgroundColor(rowColor)
                        .setPadding(7f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(SolidBorder(DeviceRgb(220, 220, 220), 0.5f))
                    summaryTable.addCell(cell)
                }
            }
            
            document.add(summaryTable)
            
            // Price summary
            val vatPercent = jobSettings.vatPercent
            val vatRate = vatPercent.toDouble() / 100.0
            val vat = subtotal * vatRate
            val total = subtotal + vat
            
            val summaryCard = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .useAllAvailableWidth()
                .setMarginTop(10f)
                .setMarginBottom(20f)
            
            // Subtotal
            summaryCard.addCell(createLabelCell("סה\"כ לפני מע\"מ:"))
            summaryCard.addCell(createValueCell(String.format("₪%.2f", subtotal)))
            
            // VAT
            summaryCard.addCell(createLabelCell("מע\"מ ($vatPercent%):"))
            summaryCard.addCell(createValueCell(String.format("₪%.2f", vat)))
            
            // Total
            val totalCell1 = createLabelCell("סה\"כ כולל מע\"מ:")
                .setBackgroundColor(DeviceRgb(25, 118, 210))
                .setFontColor(ColorConstants.WHITE)
            val totalCell2 = createValueCell(String.format("₪%.2f", total))
                .setBackgroundColor(DeviceRgb(25, 118, 210))
                .setFontColor(ColorConstants.WHITE)
                .setBold()
            
            summaryCard.addCell(totalCell1)
            summaryCard.addCell(totalCell2)
            
            document.add(summaryCard)
            
            document.add(Paragraph("\n"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun addFooter(document: Document, job: Job, customContent: com.ashaf.instanz.data.models.TemplateCustomContent?) {
        // Add professional disclaimer section - now editable!
        document.add(Paragraph("\n"))
        
        // Title with colored background
        val disclaimerTitleTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .useAllAvailableWidth()
            .setMarginTop(20f)
            .setMarginBottom(10f)
        
        val disclaimerTitleCell = Cell().add(
            createHebrewParagraph("תצהיר")
                .setFontSize(14f)
                .setBold()
        )
        .setBackgroundColor(DeviceRgb(178, 223, 219))
        .setPadding(8f)
        .setTextAlignment(TextAlignment.CENTER)
        .setBorder(SolidBorder(DeviceRgb(150, 150, 150), 1f))
        
        disclaimerTitleTable.addCell(disclaimerTitleCell)
        document.add(disclaimerTitleTable)
        
        // Get disclaimer text from customContent (editable!) or use default
        val disclaimerText = customContent?.disclaimerText ?: """
המלצות הדוח לפי הממצאים בשטח ונתונים שנמסרו ע"י המזמין.

הבדיקה בוצעה בשיטת אל-הרס ועל כן יתכן שבמהלך העבודות יתגלו רטיבויות/נזילות/כשלים נסתרים שלא נראו בעין המצלמה וידרשו טיפול נוסף.

יתכנו כתמי רטיבות גם לאחר סיום העבודות - יש אפשרות לבצע בדיקת לחות של החול במעבדה ולקבוע האם קיים צורך לבצע ייבוש חול בעזרת משאבת לחות.

חוות הדעת נכונה ליום הבדיקה.

עקב ריכוז גבוהה של מים בדירה ייתכנו סטיות במטר מהמצלמה.

חוות הדעת ניתנה למיטב ידיעתי וניסיוני המקצועי וללא אינטרס אישי ועניין כלשהו בנכס.
        """.trimIndent()
        
        android.util.Log.d("PdfGenerator", "Disclaimer text length: ${disclaimerText.length}")
        
        val disclaimerPara = createHebrewParagraph(disclaimerText)
            .setFontSize(10f)
            .setMarginBottom(20f)
        document.add(disclaimerPara)
        
        // Signature line
        val signatureLine = createHebrewParagraph("בכבוד רב,")
            .setFontSize(11f)
            .setBold()
            .setMarginTop(10f)
        document.add(signatureLine)
        
        val companyName = createHebrewParagraph("אשף האינסטלציה")
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(20f)
        document.add(companyName)
    }
    
    private fun rotateImageIfNeeded(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = android.media.ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            
            if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL && 
                orientation != android.media.ExifInterface.ORIENTATION_UNDEFINED) {
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                bitmap.recycle()  // Free memory
                android.util.Log.d("PdfGenerator", "✅ Rotated image from orientation $orientation")
                rotatedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfGenerator", "Failed to read EXIF orientation: ${e.message}")
            bitmap
        }
    }
    
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): ByteArray {
        var width = bitmap.width
        var height = bitmap.height
        
        // Calculate scaling factor
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        if (scale < 1) {
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        
        // Fix PNG transparency issue: Convert ARGB to RGB with white background
        val finalBitmap = if (scaledBitmap.hasAlpha()) {
            android.util.Log.d("PdfGenerator", "✅ Image has transparency - converting to white background")
            // Create a new bitmap with RGB_565 or ARGB_8888 but with white background
            val rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(rgbBitmap)
            
            // Fill with white background
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Draw the original image on top
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            
            rgbBitmap
        } else {
            scaledBitmap
        }
        
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        
        // Clean up
        if (finalBitmap != scaledBitmap) {
            finalBitmap.recycle()
        }
        
        return outputStream.toByteArray()
    }
}

