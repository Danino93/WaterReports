package com.ashaf.instanz.data.repositories

import com.ashaf.instanz.data.database.TemplateDao
import com.ashaf.instanz.data.models.Template
import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val templateDao: TemplateDao) {
    
    fun getAllTemplates(): Flow<List<Template>> = templateDao.getAllTemplates()
    
    suspend fun getTemplateById(templateId: String): Template? = templateDao.getTemplateById(templateId)
    
    suspend fun insertTemplate(template: Template) = templateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: Template) = templateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: Template) = templateDao.deleteTemplate(template)
    
    suspend fun insertDefaultTemplates() {
        // Check if templates already exist
        val existingTemplates = getAllTemplates()
        // This will be handled by the ViewModel using first() or similar
    }
}

