package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository

class EditorViewModelFactory(
    private val jobRepository: JobRepository,
    private val templateRepository: TemplateRepository,
    private val jobId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            return EditorViewModel(jobRepository, templateRepository, jobId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

