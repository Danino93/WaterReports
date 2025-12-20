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
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
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
    
    fun generateJobReport(
        job: Job,
        template: Template,
        images: List<JobImage>,
        dataJson: Map<String, Map<String, String>>
    ): File {
        // Create PDF file
        val fileName = "Report_${job.jobNumber}_${System.currentTimeMillis()}.pdf"
        val pdfDir = File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val pdfFile = File(pdfDir, fileName)
        
        // Create PDF document
        val writer = PdfWriter(FileOutputStream(pdfFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        
        // Set RTL support
        document.setTextAlignment(TextAlignment.RIGHT)
        
        // Load custom content - prioritize job-specific content, fallback to template defaults
        val customContent = try {
            val gson = Gson()
            val jobDataJson = if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
                gson.fromJson(job.dataJson, JsonObject::class.java)
            } else null
            
            if (jobDataJson?.has("customContent") == true) {
                // Use job-specific custom content
                gson.fromJson(
                    jobDataJson.get("customContent").asString, 
                    com.ashaf.instanz.data.models.TemplateCustomContent::class.java
                )
            } else {
                // Fallback to template defaults
                template.parseCustomContent()
            }
        } catch (e: Exception) {
            // Fallback to template defaults
            template.parseCustomContent()
        }
        
        // Add content
        addHeader(document, job, template.name)
        
        // Add intro report section from custom template
        customContent?.let {
            addIntroReport(document, it)
        }
        
        addClientInfo(document, job)
        
        // Add intro work/activities/recommendations from custom template
        customContent?.let {
            addCustomSection(document, "הקדמה - עבודה", it.sections["intro_work"])
            addCustomSection(document, "פעילות הקדמה", it.sections["intro_activities"])
            addCustomSection(document, "המלצת הקדמה", it.sections["intro_recommendations"])
        }
        
        addSections(document, template, dataJson, images)
        
        // Add summary sections from custom template
        customContent?.let {
            addCustomSection(document, "סיכום - עבודה", it.sections["work_summary"])
            addCustomSection(document, "סיכום - דו\"ח", it.sections["report_summary"])
            addCustomSection(document, "פעילות סיכום", it.sections["summary_activities"])
            addCustomSection(document, "המלצת סיכום", it.sections["summary_recommendations"])
            addCustomSection(document, "בסיום", it.sections["conclusion"])
        }
        
        addFooter(document, job)
        
        document.close()
        
        return pdfFile
    }
    
    private fun addHeader(document: Document, job: Job, templateName: String) {
        // Title
        val title = Paragraph("$templateName - דוח ${job.jobNumber}")
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
            .setFontColor(DeviceRgb(25, 118, 210))
        document.add(title)
        
        // Subtitle with date
        val subtitle = Paragraph("תאריך: ${dateFormat.format(Date(job.dateCreated))}")
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
            .setFontColor(ColorConstants.GRAY)
        document.add(subtitle)
        
        // Line separator
        document.add(Paragraph("\n"))
    }
    
    private fun addClientInfo(document: Document, job: Job) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)
        
        // Client Name
        table.addCell(createLabelCell("שם לקוח:"))
        table.addCell(createValueCell("${job.clientFirstName} ${job.clientLastName}"))
        
        // Phone
        table.addCell(createLabelCell("טלפון:"))
        table.addCell(createValueCell(job.clientPhone))
        
        // Address
        table.addCell(createLabelCell("כתובת:"))
        table.addCell(createValueCell(job.clientAddress))
        
        // Created Date
        table.addCell(createLabelCell("תאריך יצירה:"))
        table.addCell(createValueCell(dateFormat.format(Date(job.dateCreated))))
        
        // Updated Date
        table.addCell(createLabelCell("תאריך עדכון:"))
        table.addCell(createValueCell(dateFormat.format(Date(job.dateModified))))
        
        document.add(table)
        document.add(Paragraph("\n"))
    }
    
    private fun addSections(
        document: Document,
        template: Template,
        dataJson: Map<String, Map<String, String>>,
        images: List<JobImage>
    ) {
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
                addSection(document, sectionJson, dataJson, images)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun addSection(
        document: Document,
        sectionJson: JsonObject,
        dataJson: Map<String, Map<String, String>>,
        images: List<JobImage>
    ) {
        val sectionId = sectionJson.get("id")?.asString ?: return
        val sectionTitle = sectionJson.get("title")?.asString ?: ""
        val fieldsJson = sectionJson.getAsJsonArray("fields") ?: return
        
        // Section Title
        val sectionTitlePara = Paragraph(sectionTitle)
            .setFontSize(18f)
            .setBold()
            .setFontColor(DeviceRgb(25, 118, 210))
            .setMarginTop(15f)
            .setMarginBottom(10f)
        document.add(sectionTitlePara)
        
        // Section Fields
        val sectionData = dataJson[sectionId] ?: emptyMap()
        
        fieldsJson.forEach { fieldJsonElement ->
            val fieldObj = fieldJsonElement.asJsonObject
            val fieldType = fieldObj.get("type")?.asString ?: ""
            val fieldId = fieldObj.get("id")?.asString ?: ""
            val fieldLabel = fieldObj.get("label")?.asString ?: ""
            
            when (fieldType) {
                "text", "textarea", "number" -> {
                    val value = sectionData[fieldId] ?: ""
                    if (value.isNotBlank()) {
                        addTextField(document, fieldLabel, value)
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
    
    private fun addTextField(document: Document, label: String, value: String) {
        val labelPara = Paragraph(label)
            .setFontSize(12f)
            .setBold()
            .setMarginTop(5f)
        document.add(labelPara)
        
        val valuePara = Paragraph(value)
            .setFontSize(11f)
            .setMarginBottom(10f)
        document.add(valuePara)
    }
    
    private fun addCheckboxField(document: Document, label: String, checked: Boolean) {
        val checkSymbol = if (checked) "☑" else "☐"
        val para = Paragraph("$checkSymbol $label")
            .setFontSize(11f)
            .setMarginBottom(5f)
        document.add(para)
    }
    
    private fun addImageField(document: Document, label: String, images: List<JobImage>) {
        val labelPara = Paragraph(label)
            .setFontSize(12f)
            .setBold()
            .setMarginTop(10f)
            .setMarginBottom(5f)
        document.add(labelPara)
        
        images.forEach { jobImage ->
            try {
                val imageFile = File(jobImage.filePath)
                if (imageFile.exists()) {
                    // Load and compress image
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    val compressedImage = compressBitmap(bitmap, 800, 600)
                    
                    val imageData = ImageDataFactory.create(compressedImage)
                    val image = Image(imageData)
                        .setMaxWidth(UnitValue.createPercentValue(80f))
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginBottom(10f)
                    
                    document.add(image)
                    
                    // Add caption if exists
                    if (jobImage.caption?.isNotEmpty() == true) {
                        val captionPara = Paragraph(jobImage.caption ?: "")
                            .setFontSize(10f)
                            .setItalic()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(15f)
                        document.add(captionPara)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun addIntroReport(document: Document, customContent: com.ashaf.instanz.data.models.TemplateCustomContent) {
        // Add inspector information if provided
        if (customContent.inspectorName.isNotEmpty() || customContent.experienceTitle.isNotEmpty()) {
            val sectionTitle = Paragraph("הקדמה - מומחה")
                .setFontSize(18f)
                .setBold()
                .setFontColor(DeviceRgb(25, 118, 210))
                .setMarginTop(15f)
                .setMarginBottom(10f)
            document.add(sectionTitle)
            
            if (customContent.inspectorName.isNotEmpty()) {
                addTextField(document, "שם המומחה", customContent.inspectorName)
            }
            
            if (customContent.experienceTitle.isNotEmpty()) {
                addTextField(document, "כותרת נסיון", customContent.experienceTitle)
            }
            
            if (customContent.experienceText.isNotEmpty()) {
                addTextField(document, "תיאור נסיון", customContent.experienceText)
            }
            
            // Add certificate image if exists
            customContent.certificateImagePath?.let { imagePath ->
                try {
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                        val compressedImage = compressBitmap(bitmap, 400, 300)
                        
                        val imageData = com.itextpdf.io.image.ImageDataFactory.create(compressedImage)
                        val image = com.itextpdf.layout.element.Image(imageData)
                            .setMaxWidth(UnitValue.createPercentValue(60f))
                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                            .setMarginBottom(15f)
                        
                        document.add(image)
                    }
                } catch (e: Exception) {
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
    
    private fun addCustomSection(document: Document, title: String, items: List<com.ashaf.instanz.data.models.TemplateSectionItem>?) {
        if (items.isNullOrEmpty()) return
        
        val sectionTitle = Paragraph(title)
            .setFontSize(18f)
            .setBold()
            .setFontColor(DeviceRgb(25, 118, 210))
            .setMarginTop(15f)
            .setMarginBottom(10f)
        document.add(sectionTitle)
        
        items.sortedBy { it.order }.forEach { item ->
            val para = Paragraph(item.text)
                .setFontSize(11f)
                .setMarginBottom(8f)
                .setTextAlignment(TextAlignment.RIGHT)
            document.add(para)
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addFooter(document: Document, job: Job) {
        val footer = Paragraph("דוח זה נוצר באמצעות אפליקציית דוחות נזקי מים")
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20f)
            .setFontColor(ColorConstants.GRAY)
        document.add(footer)
        
        val timestamp = Paragraph("נוצר בתאריך: ${dateFormat.format(Date())}")
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY)
        document.add(timestamp)
    }
    
    private fun createLabelCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(8f)
            .setBackgroundColor(DeviceRgb(240, 240, 240))
    }
    
    private fun createValueCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text))
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(8f)
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
        
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        
        return outputStream.toByteArray()
    }
}

