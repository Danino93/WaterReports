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
    
    // Dynamic findings list
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
                    loadFindings()
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
}

