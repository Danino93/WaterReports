package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class JobWithTemplate(
    val job: Job,
    val templateName: String
)

class HomeViewModel(
    private val jobRepository: JobRepository,
    private val templateRepository: TemplateRepository
) : ViewModel() {
    
    val jobs: StateFlow<List<JobWithTemplate>> = jobRepository.getAllJobs()
        .map { jobsList ->
            jobsList.map { job ->
                val template = templateRepository.getTemplateById(job.templateId)
                JobWithTemplate(
                    job = job,
                    templateName = template?.name ?: "לא ידוע"
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _totalJobsCount = MutableStateFlow(0)
    val totalJobsCount: StateFlow<Int> = _totalJobsCount.asStateFlow()
    
    private val _monthlyJobsCount = MutableStateFlow(0)
    val monthlyJobsCount: StateFlow<Int> = _monthlyJobsCount.asStateFlow()
    
    private val _draftJobsCount = MutableStateFlow(0)
    val draftJobsCount: StateFlow<Int> = _draftJobsCount.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            _totalJobsCount.value = jobRepository.getTotalJobsCount()
            _draftJobsCount.value = jobRepository.getDraftJobsCount()
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis
            
            _monthlyJobsCount.value = jobRepository.getMonthlyJobsCount(startOfMonth, endOfMonth)
        }
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val filteredJobs: StateFlow<List<JobWithTemplate>> = combine(jobs, searchQuery) { jobsList, query ->
        if (query.isBlank()) {
            jobsList
        } else {
            jobsList.filter { jobWithTemplate ->
                val job = jobWithTemplate.job
                job.clientFirstName.contains(query, ignoreCase = true) ||
                job.clientLastName.contains(query, ignoreCase = true) ||
                job.clientAddress.contains(query, ignoreCase = true) ||
                jobWithTemplate.templateName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun deleteJob(job: Job) {
        viewModelScope.launch {
            jobRepository.deleteJob(job)
        }
    }
}

