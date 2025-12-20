package com.ashaf.instanz.data.database

import androidx.room.*
import com.ashaf.instanz.data.models.JobImage
import kotlinx.coroutines.flow.Flow

@Dao
interface JobImageDao {
    @Query("SELECT * FROM job_images WHERE jobId = :jobId ORDER BY `order`")
    fun getImagesForJob(jobId: Long): Flow<List<JobImage>>
    
    @Query("SELECT * FROM job_images WHERE jobId = :jobId AND sectionId = :sectionId ORDER BY `order`")
    fun getImagesForSection(jobId: Long, sectionId: String): Flow<List<JobImage>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: JobImage): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<JobImage>)
    
    @Update
    suspend fun updateImage(image: JobImage)
    
    @Delete
    suspend fun deleteImage(image: JobImage)
    
    @Query("DELETE FROM job_images WHERE jobId = :jobId")
    suspend fun deleteImagesForJob(jobId: Long)
    
    @Query("DELETE FROM job_images WHERE jobId = :jobId AND sectionId = :sectionId")
    suspend fun deleteImagesForSection(jobId: Long, sectionId: String)
}
