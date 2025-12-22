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
    
    // Template name (editable)
    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()
    
    // Header images and contact
    private val _logoImagePath = MutableStateFlow<String?>(null)
    val logoImagePath: StateFlow<String?> = _logoImagePath.asStateFlow()
    
    private val _contactImagePath = MutableStateFlow<String?>(null)
    val contactImagePath: StateFlow<String?> = _contactImagePath.asStateFlow()
    
    private val _phone = MutableStateFlow("052-451-6082")
    val phone: StateFlow<String> = _phone.asStateFlow()
    
    private val _email = MutableStateFlow("danino93@gmail.com")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _businessNumber = MutableStateFlow("208243708")
    val businessNumber: StateFlow<String> = _businessNumber.asStateFlow()
    
    private val _website = MutableStateFlow("https://ashaf-d.com")
    val website: StateFlow<String> = _website.asStateFlow()
    
    // Top fields
    private val _visitReason = MutableStateFlow("איתור נקלי מים")
    val visitReason: StateFlow<String> = _visitReason.asStateFlow()
    
    private val _company = MutableStateFlow("איתור - אשף האיינסטלציה")
    val company: StateFlow<String> = _company.asStateFlow()
    
    // Inspector details
    private val _inspectorName = MutableStateFlow("אליק דנינו")
    val inspectorName: StateFlow<String> = _inspectorName.asStateFlow()
    
    private val _experienceTitle = MutableStateFlow("אשף האיינסטלציה")
    val experienceTitle: StateFlow<String> = _experienceTitle.asStateFlow()
    
    private val _experienceText = MutableStateFlow("14 שנים בעבודות איינסטלציה, בענף הבניה, איטום ולויני פרוקטיםים של איטום חדירת מים מקירות")
    val experienceText: StateFlow<String> = _experienceText.asStateFlow()
    
    private val _certificateImagePath = MutableStateFlow<String?>(null)
    val certificateImagePath: StateFlow<String?> = _certificateImagePath.asStateFlow()
    
    // Disclaimer text - editable
    private val _disclaimerText = MutableStateFlow("""המלצות הדוח לפי הממצאים בשטח ונתונים שנמסרו ע"י המזמין.

הבדיקה בוצעה בשיטת אל-הרס ועל כן יתכן שבמהלך העבודות יתגלו רטיבויות/נזילות/כשלים נסתרים שלא נראו בעין המצלמה וידרשו טיפול נוסף.

יתכנו כתמי רטיבות גם לאחר סיום העבודות - יש אפשרות לבצע בדיקת לחות של החול במעבדה ולקבוע האם קיים צורך לבצע ייבוש חול בעזרת משאבת לחות.

חוות הדעת נכונה ליום הבדיקה.

עקב ריכוז גבוהה של מים בדירה ייתכנו סטיות במטר מהמצלמה.

חוות הדעת ניתנה למיטב ידיעתי וניסיוני המקצועי וללא אינטרס אישי ועניין כלשהו בנכס.""")
    val disclaimerText: StateFlow<String> = _disclaimerText.asStateFlow()
    
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
            _templateName.value = template?.name ?: ""
            
            // Determine where to load custom content from
            val customContent = if (jobId != null) {
                // Mode 2: Smart merge - inherit from master template + job-specific overrides
                val job = jobRepository.getJobById(jobId)
                job?.let {
                    try {
                        // ALWAYS start with master template as base
                        val masterContent = template?.parseCustomContent()
                        android.util.Log.d("TemplateEditor", "📋 Starting with master template as base")
                        
                        if (it.dataJson.isNotBlank() && it.dataJson != "{}") {
                            val dataJson = gson.fromJson(it.dataJson, com.google.gson.JsonObject::class.java)
                            if (dataJson.has("customContent")) {
                                val customContentElement = dataJson.get("customContent")
                                
                                // ✅ Support both new (JsonObject) and old (String) formats
                                val jobSpecificContent = if (customContentElement.isJsonObject) {
                                    android.util.Log.d("TemplateEditor", "✅ Found job-specific overrides (JsonObject)")
                                    gson.fromJson(customContentElement, TemplateCustomContent::class.java)
                                } else if (customContentElement.isJsonPrimitive && customContentElement.asJsonPrimitive.isString) {
                                    android.util.Log.d("TemplateEditor", "⚠️ Found job-specific overrides (String - old format)")
                                    gson.fromJson(customContentElement.asString, TemplateCustomContent::class.java)
                                } else {
                                    null
                                }
                                
                                // Smart merge: override only non-null/non-empty fields
                                if (jobSpecificContent != null && masterContent != null) {
                                    android.util.Log.d("TemplateEditor", "🔀 Merging job-specific overrides with master template")
                                    TemplateCustomContent(
                                        logoImagePath = jobSpecificContent.logoImagePath?.takeIf { it.isNotBlank() } ?: masterContent.logoImagePath,
                                        contactImagePath = jobSpecificContent.contactImagePath?.takeIf { it.isNotBlank() } ?: masterContent.contactImagePath,
                                        phone = jobSpecificContent.phone.takeIf { it.isNotBlank() } ?: masterContent.phone,
                                        email = jobSpecificContent.email.takeIf { it.isNotBlank() } ?: masterContent.email,
                                        businessNumber = jobSpecificContent.businessNumber.takeIf { it.isNotBlank() } ?: masterContent.businessNumber,
                                        website = jobSpecificContent.website.takeIf { it.isNotBlank() } ?: masterContent.website,
                                        visitReason = jobSpecificContent.visitReason.takeIf { it.isNotBlank() } ?: masterContent.visitReason,
                                        company = jobSpecificContent.company.takeIf { it.isNotBlank() } ?: masterContent.company,
                                        inspectorName = jobSpecificContent.inspectorName.takeIf { it.isNotBlank() } ?: masterContent.inspectorName,
                                        experienceTitle = jobSpecificContent.experienceTitle.takeIf { it.isNotBlank() } ?: masterContent.experienceTitle,
                                        experienceText = jobSpecificContent.experienceText.takeIf { it.isNotBlank() } ?: masterContent.experienceText,
                                        certificateImagePath = jobSpecificContent.certificateImagePath?.takeIf { it.isNotBlank() } ?: masterContent.certificateImagePath,
                                        disclaimerText = jobSpecificContent.disclaimerText.takeIf { it.isNotBlank() } ?: masterContent.disclaimerText,
                                        sections = if (jobSpecificContent.sections.isNotEmpty()) jobSpecificContent.sections else masterContent.sections
                                    )
                                } else {
                                    masterContent
                                }
                            } else {
                                android.util.Log.d("TemplateEditor", "No job-specific overrides, using master template")
                                masterContent
                            }
                        } else {
                            android.util.Log.d("TemplateEditor", "Empty dataJson, using master template")
                            masterContent
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TemplateEditor", "❌ Error merging customContent: ${e.message}", e)
                        template?.parseCustomContent()
                    }
                }
            } else {
                // Mode 1: Load from template defaults
                android.util.Log.d("TemplateEditor", "Loading master template customContent")
                template?.parseCustomContent()
            }
            
            // Apply loaded content
            customContent?.let { content ->
                android.util.Log.d("TemplateEditor", "Applying customContent:")
                android.util.Log.d("TemplateEditor", "  - logoImagePath: ${content.logoImagePath}")
                android.util.Log.d("TemplateEditor", "  - phone: ${content.phone}")
                android.util.Log.d("TemplateEditor", "  - email: ${content.email}")
                
                // Header images and contact
                _logoImagePath.value = content.logoImagePath
                _contactImagePath.value = content.contactImagePath
                _phone.value = content.phone
                _email.value = content.email
                _businessNumber.value = content.businessNumber
                _website.value = content.website
                
                // Top fields
                _visitReason.value = content.visitReason
                _company.value = content.company
                
                // Inspector details
                _inspectorName.value = content.inspectorName
                _experienceTitle.value = content.experienceTitle
                _experienceText.value = content.experienceText
                _certificateImagePath.value = content.certificateImagePath
                _disclaimerText.value = content.disclaimerText
                
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
    
    // Update function for template name
    fun updateTemplateName(name: String) {
        _templateName.value = name
    }
    
    // Update functions for header images and contact
    fun updateLogoImage(path: String?) {
        _logoImagePath.value = path
    }
    
    fun updateContactImage(path: String?) {
        _contactImagePath.value = path
    }
    
    fun updatePhone(phone: String) {
        _phone.value = phone
    }
    
    fun updateEmail(email: String) {
        _email.value = email
    }
    
    fun updateBusinessNumber(number: String) {
        _businessNumber.value = number
    }
    
    fun updateWebsite(website: String) {
        _website.value = website
    }
    
    // Update functions for top fields
    fun updateVisitReason(reason: String) {
        _visitReason.value = reason
    }
    
    fun updateCompany(company: String) {
        _company.value = company
    }
    
    // Update functions for inspector details
    fun updateInspectorName(name: String) {
        _inspectorName.value = name
    }
    
    fun updateExperienceTitle(title: String) {
        _experienceTitle.value = title
    }
    
    fun updateExperienceText(text: String) {
        _experienceText.value = text
        // Auto-save after a short delay (debounced)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Wait 1 second before saving (longer than disclaimer)
            saveTemplate()
        }
    }
    
    fun updateCertificateImage(path: String?) {
        _certificateImagePath.value = path
    }
    
    fun updateDisclaimerText(text: String) {
        _disclaimerText.value = text
        // Auto-save after a short delay (debounced)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Wait 500ms before saving
            saveTemplate()
        }
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
    
    fun resetToDefaults() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (jobId != null) {
                    // Remove customContent from job's dataJson
                    val job = jobRepository.getJobById(jobId) ?: return@launch
                    val dataJson = if (job.dataJson.isBlank() || job.dataJson == "{}") {
                        com.google.gson.JsonObject()
                    } else {
                        gson.fromJson(job.dataJson, com.google.gson.JsonObject::class.java)
                    }
                    
                    // Remove customContent
                    dataJson.remove("customContent")
                    
                    val updatedJob = job.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    jobRepository.updateJob(updatedJob)
                    
                    android.util.Log.d("TemplateEditor", "✅ Reset to defaults - customContent removed from job")
                    
                    // Reload from master template
                    loadTemplate()
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    fun saveTemplate() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val customContent = TemplateCustomContent(
                    // Header images and contact
                    logoImagePath = _logoImagePath.value,
                    contactImagePath = _contactImagePath.value,
                    phone = _phone.value,
                    email = _email.value,
                    businessNumber = _businessNumber.value,
                    website = _website.value,
                    
                    // Top fields
                    visitReason = _visitReason.value,
                    company = _company.value,
                    
                    // Inspector details
                    inspectorName = _inspectorName.value,
                    experienceTitle = _experienceTitle.value,
                    experienceText = _experienceText.value,
                    certificateImagePath = _certificateImagePath.value,
                    disclaimerText = _disclaimerText.value,
                    
                    // Sections
                    sections = _sections.value
                )
                
                if (jobId == null) {
                    // Mode 1: Edit template defaults
                    val currentTemplate = _template.value ?: return@launch
                    val updatedTemplate = currentTemplate.copy(
                        name = _templateName.value,
                        customContent = gson.toJson(customContent)
                    )
                    templateRepository.updateTemplate(updatedTemplate)
                    _template.value = updatedTemplate
                    
                    android.util.Log.d("TemplateEditor", "✅ Saved template customContent")
                } else {
                    // Mode 2: Edit job-specific content
                    val job = jobRepository.getJobById(jobId) ?: return@launch
                    val dataJson = if (job.dataJson.isBlank() || job.dataJson == "{}") {
                        com.google.gson.JsonObject()
                    } else {
                        gson.fromJson(job.dataJson, com.google.gson.JsonObject::class.java)
                    }
                    
                    // Save custom content to job's dataJson as JsonObject (not String!)
                    // This fixes the double JSON encoding issue
                    val customContentJson = gson.toJsonTree(customContent).asJsonObject
                    dataJson.add("customContent", customContentJson)
                    
                    val finalDataJson = gson.toJson(dataJson)
                    
                    // Debug logs
                    android.util.Log.d("TemplateEditor", "==== Saving customContent for job $jobId ====")
                    android.util.Log.d("TemplateEditor", "customContent fields:")
                    android.util.Log.d("TemplateEditor", "  - phone: ${customContent.phone}")
                    android.util.Log.d("TemplateEditor", "  - email: ${customContent.email}")
                    android.util.Log.d("TemplateEditor", "  - inspectorName: ${customContent.inspectorName}")
                    android.util.Log.d("TemplateEditor", "  - sections count: ${customContent.sections.size}")
                    android.util.Log.d("TemplateEditor", "Final dataJson length: ${finalDataJson.length} chars")
                    
                    val updatedJob = job.copy(
                        dataJson = finalDataJson,
                        dateModified = System.currentTimeMillis()
                    )
                    jobRepository.updateJob(updatedJob)
                    
                    android.util.Log.d("TemplateEditor", "✅ Job updated successfully")
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}

