package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobStatus
import com.ashaf.instanz.data.models.Template
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(
    private val jobRepository: JobRepository,
    private val templateRepository: TemplateRepository,
    private val jobId: Long
) : ViewModel() {
    
    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()
    
    private val _template = MutableStateFlow<Template?>(null)
    val template: StateFlow<Template?> = _template.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Dynamic categories with hierarchical findings
    private val _categories = MutableStateFlow<List<FindingCategory>>(emptyList())
    val categories: StateFlow<List<FindingCategory>> = _categories.asStateFlow()
    
    // Backward compatibility - flat findings list (for migration)
    private val _findings = MutableStateFlow<List<String>>(emptyList())
    val findings: StateFlow<List<String>> = _findings.asStateFlow()
    
    private val gson = Gson()
    
    init {
        loadJobAndTemplate()
    }
    
    fun loadJobAndTemplate() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val job = jobRepository.getJobById(jobId)
                _job.value = job
                
                job?.let {
                    val template = templateRepository.getTemplateById(it.templateId)
                    _template.value = template
                    loadCategories() // Load hierarchical structure first
                    loadFindings() // Backward compatibility for old structure
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadFindings() {
        val job = _job.value ?: return
        if (job.dataJson.isBlank() || job.dataJson == "{}") {
            _findings.value = emptyList()
            return
        }
        
        try {
            val dataJson = gson.fromJson(job.dataJson, JsonObject::class.java)
            if (dataJson.has("findings")) {
                val findingsArray = dataJson.getAsJsonArray("findings")
                val findingsList = mutableListOf<String>()
                findingsArray.forEach { element ->
                    findingsList.add(element.asString)
                }
                _findings.value = findingsList
            } else {
                _findings.value = emptyList()
            }
        } catch (e: Exception) {
            _findings.value = emptyList()
        }
    }
    
    fun updateFieldValue(sectionId: String, fieldId: String, value: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                android.util.Log.d("EditorViewModel", "Updating field: section=$sectionId, field=$fieldId, value=$value")
                
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                // Create nested structure: sectionId -> fieldId -> value
                if (!dataJson.has(sectionId)) {
                    dataJson.add(sectionId, JsonObject())
                }
                val sectionObj = dataJson.getAsJsonObject(sectionId)
                sectionObj.addProperty(fieldId, value)
                
                // CRITICAL: Convert JsonObject to String ONCE
                val dataJsonString = gson.toJson(dataJson)
                android.util.Log.d("EditorViewModel", "New dataJson: $dataJsonString")
                
                // CRITICAL FIX: Update Job fields for client details
                var updatedJob = currentJob.copy(
                    dataJson = dataJsonString,
                    dateModified = System.currentTimeMillis()
                )
                
                // Sync client details from dataJson to Job fields
                if (sectionId == "client_details") {
                    when (fieldId) {
                        "client_first_name" -> updatedJob = updatedJob.copy(clientFirstName = value)
                        "client_last_name" -> updatedJob = updatedJob.copy(
                            clientLastName = value,
                            title = "${updatedJob.clientFirstName} $value"
                        )
                        "client_phone" -> updatedJob = updatedJob.copy(clientPhone = value)
                        "client_city", "client_street" -> {
                            // Update address
                            val city = sectionObj.get("client_city")?.asString ?: ""
                            val street = sectionObj.get("client_street")?.asString ?: ""
                            updatedJob = updatedJob.copy(clientAddress = "$city, $street")
                        }
                        "client_company" -> updatedJob = updatedJob.copy(clientCompany = value)
                    }
                }
                
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
                
                android.util.Log.d("EditorViewModel", "Job updated successfully: ${updatedJob.clientFirstName} ${updatedJob.clientLastName}")
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error updating field", e)
                _error.value = e.message
            }
        }
    }
    
    fun getFieldValue(sectionId: String, fieldId: String): String {
        val job = _job.value ?: return ""
        if (job.dataJson.isBlank() || job.dataJson == "{}") return ""
        
        return try {
            val dataJson = gson.fromJson(job.dataJson, JsonObject::class.java)
            if (dataJson.has(sectionId)) {
                val sectionObj = dataJson.getAsJsonObject(sectionId)
                if (sectionObj.has(fieldId)) {
                    sectionObj.get(fieldId).asString
                } else ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
    
    fun saveChanges() {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            try {
                // The job already has the updated dataJson from updateFieldValue calls
                val updatedJob = currentJob.copy(
                    dateModified = System.currentTimeMillis()
                )
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun addFinding() {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            val findingId = "finding_${System.currentTimeMillis()}"
            
            try {
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                // Add finding ID to findings array
                val findingsArray = if (dataJson.has("findings")) {
                    dataJson.getAsJsonArray("findings")
                } else {
                    com.google.gson.JsonArray()
                }
                findingsArray.add(findingId)
                dataJson.add("findings", findingsArray)
                
                // Initialize finding data
                val findingObj = JsonObject()
                dataJson.add(findingId, findingObj)
                
                val updatedJob = currentJob.copy(
                    dataJson = gson.toJson(dataJson),
                    dateModified = System.currentTimeMillis()
                )
                
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
                loadFindings()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun moveFindingUp(findingId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                if (dataJson.has("findings")) {
                    val findingsArray = dataJson.getAsJsonArray("findings")
                    val findingsList = mutableListOf<String>()
                    findingsArray.forEach { findingsList.add(it.asString) }
                    
                    val index = findingsList.indexOf(findingId)
                    if (index > 0) {
                        // Swap with previous
                        val temp = findingsList[index - 1]
                        findingsList[index - 1] = findingsList[index]
                        findingsList[index] = temp
                        
                        // Update array
                        val newArray = com.google.gson.JsonArray()
                        findingsList.forEach { newArray.add(it) }
                        dataJson.add("findings", newArray)
                        
                        val updatedJob = currentJob.copy(
                            dataJson = gson.toJson(dataJson),
                            dateModified = System.currentTimeMillis()
                        )
                        
                        jobRepository.updateJob(updatedJob)
                        _job.value = updatedJob
                        loadFindings()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun moveFindingDown(findingId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                if (dataJson.has("findings")) {
                    val findingsArray = dataJson.getAsJsonArray("findings")
                    val findingsList = mutableListOf<String>()
                    findingsArray.forEach { findingsList.add(it.asString) }
                    
                    val index = findingsList.indexOf(findingId)
                    if (index >= 0 && index < findingsList.size - 1) {
                        // Swap with next
                        val temp = findingsList[index + 1]
                        findingsList[index + 1] = findingsList[index]
                        findingsList[index] = temp
                        
                        // Update array
                        val newArray = com.google.gson.JsonArray()
                        findingsList.forEach { newArray.add(it) }
                        dataJson.add("findings", newArray)
                        
                        val updatedJob = currentJob.copy(
                            dataJson = gson.toJson(dataJson),
                            dateModified = System.currentTimeMillis()
                        )
                        
                        jobRepository.updateJob(updatedJob)
                        _job.value = updatedJob
                        loadFindings()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun deleteFinding(findingId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                // Remove from findings array
                if (dataJson.has("findings")) {
                    val findingsArray = dataJson.getAsJsonArray("findings")
                    val newArray = com.google.gson.JsonArray()
                    findingsArray.forEach { element ->
                        if (element.asString != findingId) {
                            newArray.add(element)
                        }
                    }
                    dataJson.add("findings", newArray)
                }
                
                // Remove finding data
                if (dataJson.has(findingId)) {
                    dataJson.remove(findingId)
                }
                
                val updatedJob = currentJob.copy(
                    dataJson = gson.toJson(dataJson),
                    dateModified = System.currentTimeMillis()
                )
                
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
                loadFindings()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            try {
                jobRepository.deleteJobById(jobId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun duplicateJob(jobId: Long) {
        viewModelScope.launch {
            try {
                val originalJob = jobRepository.getJobById(jobId) ?: return@launch
                
                val duplicatedJob = originalJob.copy(
                    id = 0, // Room will auto-generate new ID
                    dateCreated = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis(),
                    clientFirstName = "${originalJob.clientFirstName} (עותק)",
                    status = JobStatus.DRAFT
                )
                
                jobRepository.insertJob(duplicatedJob)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun archiveJob(jobId: Long) {
        viewModelScope.launch {
            try {
                val job = jobRepository.getJobById(jobId) ?: return@launch
                val archivedJob = job.copy(
                    status = JobStatus.ARCHIVED,
                    dateModified = System.currentTimeMillis()
                )
                jobRepository.updateJob(archivedJob)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun saveJobSettings(settings: com.ashaf.instanz.data.models.JobSettings) {
        viewModelScope.launch {
            try {
                val currentJob = _job.value ?: return@launch
                val settingsJson = gson.toJson(settings)
                val updatedJob = currentJob.copy(
                    jobSettingsJson = settingsJson,
                    dateModified = System.currentTimeMillis()
                )
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    // =====================================================
    // HIERARCHICAL FINDINGS - NEW STRUCTURE
    // =====================================================
    
    /**
     * Load categories and findings from job.dataJson
     * Supports both new hierarchical structure and old flat structure (migration)
     */
    private fun loadCategories() {
        val job = _job.value ?: return
        if (job.dataJson.isBlank() || job.dataJson == "{}") {
            _categories.value = emptyList()
            return
        }
        
        try {
            val dataJson = gson.fromJson(job.dataJson, JsonObject::class.java)
            
            // Check if new hierarchical structure exists
            if (dataJson.has("categories")) {
                val categoriesArray = dataJson.getAsJsonArray("categories")
                val categoriesList = mutableListOf<FindingCategory>()
                
                categoriesArray.forEach { categoryElement ->
                    val categoryObj = categoryElement.asJsonObject
                    val categoryId = categoryObj.get("id")?.asString ?: return@forEach
                    val title = categoryObj.get("title")?.asString ?: ""
                    val order = categoryObj.get("order")?.asInt ?: 0
                    
                    val findingsList = mutableListOf<FindingItem>()
                    if (categoryObj.has("findings")) {
                        val findingsArray = categoryObj.getAsJsonArray("findings")
                        findingsArray.forEach { findingElement ->
                            val findingObj = findingElement.asJsonObject
                            val findingId = findingObj.get("id")?.asString ?: return@forEach
                            val subject = findingObj.get("subject")?.asString ?: ""
                            val description = findingObj.get("description")?.asString ?: ""
                            val note = findingObj.get("note")?.asString ?: ""
                            
                            findingsList.add(FindingItem(findingId, subject, description, note))
                        }
                    }
                    
                    categoriesList.add(FindingCategory(categoryId, title, order, findingsList))
                }
                
                _categories.value = categoriesList.sortedBy { it.order }
            } else {
                // Backward compatibility - convert old flat structure to hierarchical
                loadFindings() // Load old findings
                _categories.value = emptyList() // No categories yet
            }
        } catch (e: Exception) {
            android.util.Log.e("EditorViewModel", "Error loading categories: ${e.message}", e)
            _categories.value = emptyList()
        }
    }
    
    /**
     * Add a new category
     */
    fun addCategory(title: String = "קטגוריה חדשה") {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            val categoryId = "category_${System.currentTimeMillis()}"
            
            try {
                val dataJson = if (currentJob.dataJson.isBlank() || currentJob.dataJson == "{}") {
                    JsonObject()
                } else {
                    gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                }
                
                // Get or create categories array
                val categoriesArray = if (dataJson.has("categories")) {
                    dataJson.getAsJsonArray("categories")
                } else {
                    com.google.gson.JsonArray()
                }
                
                // Create new category object
                val categoryObj = JsonObject()
                categoryObj.addProperty("id", categoryId)
                categoryObj.addProperty("title", title)
                categoryObj.addProperty("order", categoriesArray.size())
                categoryObj.add("findings", com.google.gson.JsonArray())
                
                categoriesArray.add(categoryObj)
                dataJson.add("categories", categoriesArray)
                
                val updatedJob = currentJob.copy(
                    dataJson = gson.toJson(dataJson),
                    dateModified = System.currentTimeMillis()
                )
                
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
                loadCategories()
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error adding category: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Update category title
     */
    fun updateCategoryTitle(categoryId: String, newTitle: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    categoriesArray.forEachIndexed { index, categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString == categoryId) {
                            categoryObj.addProperty("title", newTitle)
                        }
                    }
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                    loadCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error updating category: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Delete a category and all its findings
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    val newArray = com.google.gson.JsonArray()
                    categoriesArray.forEach { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString != categoryId) {
                            newArray.add(categoryElement)
                        }
                    }
                    dataJson.add("categories", newArray)
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                    loadCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error deleting category: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Move category up in order
     */
    fun moveCategoryUp(categoryId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    val categoriesList = mutableListOf<JsonObject>()
                    categoriesArray.forEach { categoriesList.add(it.asJsonObject) }
                    
                    val index = categoriesList.indexOfFirst { it.get("id")?.asString == categoryId }
                    if (index > 0) {
                        // Swap with previous
                        val temp = categoriesList[index - 1]
                        categoriesList[index - 1] = categoriesList[index]
                        categoriesList[index] = temp
                        
                        // Update order property
                        categoriesList.forEachIndexed { i, cat -> cat.addProperty("order", i) }
                        
                        // Rebuild array
                        val newArray = com.google.gson.JsonArray()
                        categoriesList.forEach { newArray.add(it) }
                        dataJson.add("categories", newArray)
                        
                        val updatedJob = currentJob.copy(
                            dataJson = gson.toJson(dataJson),
                            dateModified = System.currentTimeMillis()
                        )
                        
                        jobRepository.updateJob(updatedJob)
                        _job.value = updatedJob
                        loadCategories()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error moving category up: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Move category down in order
     */
    fun moveCategoryDown(categoryId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    val categoriesList = mutableListOf<JsonObject>()
                    categoriesArray.forEach { categoriesList.add(it.asJsonObject) }
                    
                    val index = categoriesList.indexOfFirst { it.get("id")?.asString == categoryId }
                    if (index >= 0 && index < categoriesList.size - 1) {
                        // Swap with next
                        val temp = categoriesList[index + 1]
                        categoriesList[index + 1] = categoriesList[index]
                        categoriesList[index] = temp
                        
                        // Update order property
                        categoriesList.forEachIndexed { i, cat -> cat.addProperty("order", i) }
                        
                        // Rebuild array
                        val newArray = com.google.gson.JsonArray()
                        categoriesList.forEach { newArray.add(it) }
                        dataJson.add("categories", newArray)
                        
                        val updatedJob = currentJob.copy(
                            dataJson = gson.toJson(dataJson),
                            dateModified = System.currentTimeMillis()
                        )
                        
                        jobRepository.updateJob(updatedJob)
                        _job.value = updatedJob
                        loadCategories()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error moving category down: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Add a new finding to a specific category
     */
    fun addFindingToCategory(categoryId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            val findingId = "finding_${System.currentTimeMillis()}"
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    categoriesArray.forEach { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString == categoryId) {
                            val findingsArray = if (categoryObj.has("findings")) {
                                categoryObj.getAsJsonArray("findings")
                            } else {
                                com.google.gson.JsonArray()
                            }
                            
                            val findingObj = JsonObject()
                            findingObj.addProperty("id", findingId)
                            findingObj.addProperty("subject", "")
                            findingObj.addProperty("description", "")
                            findingObj.addProperty("note", "")
                            findingObj.add("recommendations", com.google.gson.JsonArray())
                            
                            findingsArray.add(findingObj)
                            categoryObj.add("findings", findingsArray)
                        }
                    }
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                    loadCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error adding finding: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Update finding field in a specific category
     */
    fun updateFindingInCategory(categoryId: String, findingId: String, fieldName: String, value: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    categoriesArray.forEach { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString == categoryId) {
                            if (categoryObj.has("findings")) {
                                val findingsArray = categoryObj.getAsJsonArray("findings")
                                findingsArray.forEach { findingElement ->
                                    val findingObj = findingElement.asJsonObject
                                    if (findingObj.get("id")?.asString == findingId) {
                                        findingObj.addProperty(fieldName, value)
                                    }
                                }
                            }
                        }
                    }
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error updating finding: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Delete a finding from a category
     */
    fun deleteFindingFromCategory(categoryId: String, findingId: String) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    categoriesArray.forEach { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString == categoryId) {
                            if (categoryObj.has("findings")) {
                                val findingsArray = categoryObj.getAsJsonArray("findings")
                                val newArray = com.google.gson.JsonArray()
                                findingsArray.forEach { findingElement ->
                                    val findingObj = findingElement.asJsonObject
                                    if (findingObj.get("id")?.asString != findingId) {
                                        newArray.add(findingElement)
                                    }
                                }
                                categoryObj.add("findings", newArray)
                            }
                        }
                    }
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                    loadCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error deleting finding: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Move finding within the same category (up/down)
     */
    fun moveFindingInCategory(categoryId: String, findingId: String, direction: Int) {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                if (dataJson.has("categories")) {
                    val categoriesArray = dataJson.getAsJsonArray("categories")
                    categoriesArray.forEach { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        if (categoryObj.get("id")?.asString == categoryId) {
                            if (categoryObj.has("findings")) {
                                val findingsArray = categoryObj.getAsJsonArray("findings")
                                val findingsList = mutableListOf<JsonObject>()
                                findingsArray.forEach { findingsList.add(it.asJsonObject) }
                                
                                val index = findingsList.indexOfFirst { it.get("id")?.asString == findingId }
                                if (direction == -1 && index > 0) {
                                    // Move up
                                    val temp = findingsList[index - 1]
                                    findingsList[index - 1] = findingsList[index]
                                    findingsList[index] = temp
                                    
                                    val newArray = com.google.gson.JsonArray()
                                    findingsList.forEach { newArray.add(it) }
                                    categoryObj.add("findings", newArray)
                                } else if (direction == 1 && index >= 0 && index < findingsList.size - 1) {
                                    // Move down
                                    val temp = findingsList[index + 1]
                                    findingsList[index + 1] = findingsList[index]
                                    findingsList[index] = temp
                                    
                                    val newArray = com.google.gson.JsonArray()
                                    findingsList.forEach { newArray.add(it) }
                                    categoryObj.add("findings", newArray)
                                }
                            }
                        }
                    }
                    
                    val updatedJob = currentJob.copy(
                        dataJson = gson.toJson(dataJson),
                        dateModified = System.currentTimeMillis()
                    )
                    
                    jobRepository.updateJob(updatedJob)
                    _job.value = updatedJob
                    loadCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error moving finding: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Migrate old flat findings structure to new hierarchical structure
     * Creates a default category "ממצאים" and moves all findings there
     */
    fun migrateToHierarchicalStructure() {
        viewModelScope.launch {
            val currentJob = _job.value ?: return@launch
            
            try {
                val dataJson = gson.fromJson(currentJob.dataJson, JsonObject::class.java)
                
                // Check if already using new structure
                if (dataJson.has("categories")) {
                    android.util.Log.d("EditorViewModel", "Already using hierarchical structure")
                    return@launch
                }
                
                // Check if there are old findings to migrate
                if (!dataJson.has("findings")) {
                    android.util.Log.d("EditorViewModel", "No findings to migrate")
                    return@launch
                }
                
                val oldFindingsArray = dataJson.getAsJsonArray("findings")
                if (oldFindingsArray.size() == 0) {
                    android.util.Log.d("EditorViewModel", "No findings to migrate (empty array)")
                    return@launch
                }
                
                // Create default category
                val defaultCategory = JsonObject()
                defaultCategory.addProperty("id", "category_${System.currentTimeMillis()}")
                defaultCategory.addProperty("title", "ממצאים")
                defaultCategory.addProperty("order", 0)
                
                val newFindingsArray = com.google.gson.JsonArray()
                
                // Migrate each old finding
                oldFindingsArray.forEach { findingIdElement ->
                    val findingId = findingIdElement.asString
                    if (dataJson.has(findingId)) {
                        val oldFindingData = dataJson.getAsJsonObject(findingId)
                        
                        val newFinding = JsonObject()
                        newFinding.addProperty("id", findingId)
                        newFinding.addProperty("subject", oldFindingData.get("finding_subject")?.asString ?: "")
                        newFinding.addProperty("description", oldFindingData.get("finding_description")?.asString ?: "")
                        newFinding.addProperty("note", oldFindingData.get("finding_note")?.asString ?: "")
                        
                        // Copy recommendations if exists
                        if (oldFindingData.has("recommendations")) {
                            newFinding.add("recommendations", oldFindingData.getAsJsonArray("recommendations"))
                        } else {
                            newFinding.add("recommendations", com.google.gson.JsonArray())
                        }
                        
                        newFindingsArray.add(newFinding)
                        
                        // Remove old finding data
                        dataJson.remove(findingId)
                    }
                }
                
                defaultCategory.add("findings", newFindingsArray)
                
                // Create categories array
                val categoriesArray = com.google.gson.JsonArray()
                categoriesArray.add(defaultCategory)
                dataJson.add("categories", categoriesArray)
                
                // Remove old findings array
                dataJson.remove("findings")
                
                val updatedJob = currentJob.copy(
                    dataJson = gson.toJson(dataJson),
                    dateModified = System.currentTimeMillis()
                )
                
                jobRepository.updateJob(updatedJob)
                _job.value = updatedJob
                loadCategories()
                
                android.util.Log.d("EditorViewModel", "✅ Successfully migrated ${newFindingsArray.size()} findings to hierarchical structure")
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "❌ Error migrating to hierarchical structure: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
}

// Data classes for hierarchical findings
data class FindingCategory(
    val id: String,
    val title: String,
    val order: Int,
    val findings: List<FindingItem>
)

data class FindingItem(
    val id: String,
    val subject: String,
    val description: String,
    val note: String
)

