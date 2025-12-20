package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository

class TemplateEditorViewModelFactory(
    private val templateRepository: TemplateRepository,
    private val jobRepository: JobRepository,
    private val templateId: String,
    private val jobId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateEditorViewModel::class.java)) {
            return TemplateEditorViewModel(templateRepository, jobRepository, templateId, jobId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

