package com.ashaf.instanz.data.repositories

import com.ashaf.instanz.data.database.JobImageDao
import com.ashaf.instanz.data.models.JobImage
import kotlinx.coroutines.flow.Flow

class ImageRepository(private val jobImageDao: JobImageDao) {
    
    fun getImagesForJob(jobId: Long): Flow<List<JobImage>> = jobImageDao.getImagesForJob(jobId)
    
    fun getImagesForSection(jobId: Long, sectionId: String): Flow<List<JobImage>> =
        jobImageDao.getImagesForSection(jobId, sectionId)
    
    suspend fun insertImage(image: JobImage): Long = jobImageDao.insertImage(image)
    
    suspend fun insertImages(images: List<JobImage>) = jobImageDao.insertImages(images)
    
    suspend fun updateImage(image: JobImage) = jobImageDao.updateImage(image)
    
    suspend fun deleteImage(image: JobImage) = jobImageDao.deleteImage(image)
    
    suspend fun deleteImagesForJob(jobId: Long) = jobImageDao.deleteImagesForJob(jobId)
    
    suspend fun deleteImagesForSection(jobId: Long, sectionId: String) =
        jobImageDao.deleteImagesForSection(jobId, sectionId)
}

