package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.Template
import com.ashaf.instanz.data.repositories.TemplateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TemplateViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {
    
    val templates: StateFlow<List<Template>> = templateRepository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _selectedTemplate = MutableStateFlow<Template?>(null)
    val selectedTemplate: StateFlow<Template?> = _selectedTemplate.asStateFlow()
    
    fun selectTemplate(template: Template) {
        _selectedTemplate.value = template
    }
    
    suspend fun getTemplateById(templateId: String): Template? {
        return templateRepository.getTemplateById(templateId)
    }
}

