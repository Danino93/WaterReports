package com.ashaf.instanz.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.datastore.SettingsDataStore
import com.ashaf.instanz.data.repositories.ImageRepository
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository
import com.ashaf.instanz.utils.PdfGenerator
import com.ashaf.instanz.utils.QuotePdfGenerator
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel(
    private val context: Context,
    private val jobRepository: JobRepository,
    private val templateRepository: TemplateRepository,
    private val imageRepository: ImageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun generatePdf(jobId: Long) {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _generatedPdfFile.value = null
            
            try {
                // Load job
                val job = jobRepository.getJobById(jobId)
                if (job == null) {
                    _error.value = "לא נמצא דוח"
                    return@launch
                }
                
                // Load template
                val template = templateRepository.getTemplateById(job.templateId)
                if (template == null) {
                    _error.value = "לא נמצאה תבנית"
                    return@launch
                }
                
                // Load images
                val images = imageRepository.getImagesForJob(jobId).first()
                
                // Parse dataJson
                val dataJson = parseDataJson(job.dataJson)
                
                // Load job-specific settings (if available), otherwise use global settings
                val jobSettings = job.getJobSettings()
                val imagesPerRow = settingsDataStore.imagesPerRow.first().toInt()
                val showHeader = jobSettings.showImagesInReport
                val vatPercent = jobSettings.vatPercent
                val showPrices = jobSettings.showPricesInReport
                val showVat = jobSettings.showVatInReport
                
                // Generate PDF - use QuotePdfGenerator for quote templates
                val pdfFile = if (template.id == "template_quote") {
                    val quotePdfGenerator = QuotePdfGenerator(
                        context = context,
                        vatPercent = vatPercent,
                        showPrices = showPrices,
                        showVat = showVat,
                        jobSettings = jobSettings
                    )
                    quotePdfGenerator.generateQuotePdf(
                        job = job,
                        template = template,
                        dataJson = dataJson
                    )
                } else {
                    val pdfGenerator = PdfGenerator(
                        context = context,
                        imagesPerRow = imagesPerRow,
                        showHeader = showHeader,
                        jobSettings = jobSettings
                    )
                    pdfGenerator.generateJobReport(
                        job = job,
                        template = template,
                        images = images,
                        dataJson = dataJson
                    )
                }
                
                _generatedPdfFile.value = pdfFile
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "שגיאה ביצירת PDF: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    private fun parseDataJson(jsonString: String): Map<String, Map<String, String>> {
        return try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            val result = mutableMapOf<String, Map<String, String>>()
            
            jsonObject.keySet().forEach { sectionId ->
                val sectionObj = jsonObject.getAsJsonObject(sectionId)
                val sectionMap = mutableMapOf<String, String>()
                
                sectionObj.keySet().forEach { fieldId ->
                    val value = sectionObj.get(fieldId)?.asString ?: ""
                    sectionMap[fieldId] = value
                }
                
                result[sectionId] = sectionMap
            }
            
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearGeneratedPdf() {
        _generatedPdfFile.value = null
    }
}

