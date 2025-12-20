package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobStatus
import com.ashaf.instanz.data.repositories.JobRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JobViewModel(
    private val jobRepository: JobRepository,
    private val jobId: Long?
) : ViewModel() {
    
    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()
    
    init {
        if (jobId != null) {
            loadJob(jobId)
        }
    }
    
    private fun loadJob(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _job.value = jobRepository.getJobById(id)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _createdJobId = MutableStateFlow<Long?>(null)
    val createdJobId: StateFlow<Long?> = _createdJobId.asStateFlow()
    
    fun createJob(
        templateId: String,
        clientFirstName: String,
        clientLastName: String,
        clientPhone: String,
        clientAddress: String,
        clientCompany: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentTime = System.currentTimeMillis()
                val newJob = Job(
                    templateId = templateId,
                    jobNumber = generateJobNumber(),
                    title = "$clientFirstName $clientLastName",
                    clientFirstName = clientFirstName,
                    clientLastName = clientLastName,
                    clientPhone = clientPhone,
                    clientAddress = clientAddress,
                    clientCompany = clientCompany,
                    dateCreated = currentTime,
                    dateModified = currentTime,
                    status = JobStatus.DRAFT,
                    dataJson = "{}"
                )
                val newJobId = jobRepository.insertJob(newJob)
                _createdJobId.value = newJobId
                // Reload the job to get the full entity with ID
                _job.value = jobRepository.getJobById(newJobId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateJob(job: Job) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                jobRepository.updateJob(job)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveJob(
        clientFirstName: String,
        clientLastName: String,
        clientPhone: String,
        clientAddress: String,
        clientCompany: String? = null
    ) {
        viewModelScope.launch {
            val currentJob = job.value ?: return@launch
            val updatedJob = currentJob.copy(
                clientFirstName = clientFirstName,
                clientLastName = clientLastName,
                clientPhone = clientPhone,
                clientAddress = clientAddress,
                clientCompany = clientCompany,
                status = JobStatus.DRAFT,
                dateModified = System.currentTimeMillis()
            )
            updateJob(updatedJob)
        }
    }
    
    private fun generateJobNumber(): String {
        // Generate job number based on current date and random number
        val timestamp = System.currentTimeMillis()
        return "#${timestamp.toString().takeLast(6)}"
    }
}

