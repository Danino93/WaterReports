package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.Template
import com.ashaf.instanz.data.models.TemplateCustomContent
import com.ashaf.instanz.data.models.TemplateSectionItem
import com.ashaf.instanz.data.repositories.TemplateRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TemplateEditorViewModel(
    private val templateRepository: TemplateRepository,
    private val jobRepository: com.ashaf.instanz.data.repositories.JobRepository,
    private val templateId: String,
    private val jobId: Long? = null // null = edit template, not null = edit job-specific content
) : ViewModel() {
    
    private val _template = MutableStateFlow<Template?>(null)
    val template: StateFlow<Template?> = _template.asStateFlow()
    
    private val _inspectorName = MutableStateFlow("")
    val inspectorName: StateFlow<String> = _inspectorName.asStateFlow()
    
    private val _experienceTitle = MutableStateFlow("")
    val experienceTitle: StateFlow<String> = _experienceTitle.asStateFlow()
    
    private val _experienceText = MutableStateFlow("")
    val experienceText: StateFlow<String> = _experienceText.asStateFlow()
    
    private val _certificateImagePath = MutableStateFlow<String?>(null)
    val certificateImagePath: StateFlow<String?> = _certificateImagePath.asStateFlow()
    
    private val _sections = MutableStateFlow<Map<String, MutableList<TemplateSectionItem>>>(emptyMap())
    val sections: StateFlow<Map<String, MutableList<TemplateSectionItem>>> = _sections.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val gson = Gson()
    
    init {
        loadTemplate()
    }
    
    private fun loadTemplate() {
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId)
            _template.value = template
            
            // Determine where to load custom content from
            val customContent = if (jobId != null) {
                // Mode 2: Load from job-specific content
                val job = jobRepository.getJobById(jobId)
                job?.let {
                    try {
                        if (it.dataJson.isNotBlank() && it.dataJson != "{}") {
                            val dataJson = gson.fromJson(it.dataJson, com.google.gson.JsonObject::class.java)
                            if (dataJson.has("customContent")) {
                                gson.fromJson(dataJson.get("customContent").asString, TemplateCustomContent::class.java)
                            } else {
                                // Fallback to template defaults
                                template?.parseCustomContent()
                            }
                        } else {
                            // Fallback to template defaults
                            template?.parseCustomContent()
                        }
                    } catch (e: Exception) {
                        template?.parseCustomContent()
                    }
                }
            } else {
                // Mode 1: Load from template defaults
                template?.parseCustomContent()
            }
            
            // Apply loaded content
            customContent?.let { content ->
                _inspectorName.value = content.inspectorName
                _experienceTitle.value = content.experienceTitle
                _experienceText.value = content.experienceText
                _certificateImagePath.value = content.certificateImagePath
                
                // Convert to mutable lists
                val mutableSections = mutableMapOf<String, MutableList<TemplateSectionItem>>()
                content.sections.forEach { (key, value) ->
                    mutableSections[key] = value.toMutableList()
                }
                _sections.value = mutableSections
            } ?: run {
                // Initialize empty sections
                _sections.value = mapOf(
                    "intro_report" to mutableListOf(),
                    "conclusion" to mutableListOf(),
                    "intro_work" to mutableListOf(),
                    "intro_activities" to mutableListOf(),
                    "intro_recommendations" to mutableListOf(),
                    "summary_recommendations" to mutableListOf(),
                    "summary_activities" to mutableListOf(),
                    "work_summary" to mutableListOf(),
                    "report_summary" to mutableListOf()
                )
            }
        }
    }
    
    fun updateInspectorName(name: String) {
        _inspectorName.value = name
    }
    
    fun updateExperienceTitle(title: String) {
        _experienceTitle.value = title
    }
    
    fun updateExperienceText(text: String) {
        _experienceText.value = text
    }
    
    fun updateCertificateImage(path: String?) {
        _certificateImagePath.value = path
    }
    
    fun addSectionItem(sectionId: String, text: String) {
        val currentSections = _sections.value.toMutableMap()
        val sectionItems = currentSections[sectionId] ?: mutableListOf()
        
        val newItem = TemplateSectionItem(
            text = text,
            order = sectionItems.size
        )
        
        sectionItems.add(newItem)
        currentSections[sectionId] = sectionItems
        _sections.value = currentSections
    }
    
    fun updateSectionItem(sectionId: String, index: Int, text: String) {
        val currentSections = _sections.value.toMutableMap()
        val sectionItems = currentSections[sectionId] ?: return
        
        if (index < sectionItems.size) {
            sectionItems[index] = sectionItems[index].copy(text = text)
            currentSections[sectionId] = sectionItems
            _sections.value = currentSections
        }
    }
    
    fun deleteSectionItem(sectionId: String, index: Int) {
        val currentSections = _sections.value.toMutableMap()
        val sectionItems = currentSections[sectionId] ?: return
        
        if (index < sectionItems.size) {
            sectionItems.removeAt(index)
            // Update order
            sectionItems.forEachIndexed { idx, item ->
                sectionItems[idx] = item.copy(order = idx)
            }
            currentSections[sectionId] = sectionItems
            _sections.value = currentSections
        }
    }
    
    fun saveTemplate() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val customContent = TemplateCustomContent(
                    inspectorName = _inspectorName.value,
                    experienceTitle = _experienceTitle.value,
                    experienceText = _experienceText.value,
                    certificateImagePath = _certificateImagePath.value,
                    sections = _sections.value
                )
                
                if (jobId == null) {
                    // Mode 1: Edit template defaults
                    val currentTemplate = _template.value ?: return@launch
                    val updatedTemplate = currentTemplate.copy(
                        customContent = gson.toJson(customContent)
                    )
                    templateRepository.updateTemplate(updatedTemplate)
                    _template.value = updatedTemplate
                } else {
                    // Mode 2: Edit job-specific content
                    val job = jobRepository.getJobById(jobId) ?: return@launch
                    val dataJson = if (job.dataJson.isBlank() || job.dataJson == "{}") {
                        com.google.gson.JsonObject()
                    } else {
                        gson.fromJson(job.dataJson, com.google.gson.JsonObject::class.java)
                    }
                    
                    // Save custom content to job's dataJson
                    dataJson.addProperty("customContent", gson.toJson(customContent))
                    
                    val updatedJob = job.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    jobRepository.updateJob(updatedJob)
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}

