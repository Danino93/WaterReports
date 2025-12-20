package com.ashaf.instanz.data.database

import androidx.room.*
import com.ashaf.instanz.data.models.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY dateCreated DESC")
    fun getAllTemplates(): Flow<List<Template>>
    
    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: String): Template?
    
    @Query("SELECT * FROM templates WHERE isCustom = :isCustom")
    fun getTemplatesByType(isCustom: Boolean): Flow<List<Template>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<Template>)
    
    @Update
    suspend fun updateTemplate(template: Template)
    
    @Delete
    suspend fun deleteTemplate(template: Template)
    
    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)
}
