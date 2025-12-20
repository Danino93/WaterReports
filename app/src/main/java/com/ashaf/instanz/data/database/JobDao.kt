package com.ashaf.instanz.data.database

import androidx.room.*
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY dateModified DESC")
    fun getAllJobs(): Flow<List<Job>>
    
    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: Long): Job?
    
    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY dateModified DESC")
    fun getJobsByStatus(status: JobStatus): Flow<List<Job>>
    
    @Query("SELECT * FROM jobs WHERE clientFirstName LIKE :searchQuery OR clientLastName LIKE :searchQuery OR clientAddress LIKE :searchQuery ORDER BY dateModified DESC")
    fun searchJobs(searchQuery: String): Flow<List<Job>>
    
    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun getTotalJobsCount(): Int
    
    @Query("SELECT COUNT(*) FROM jobs WHERE dateCreated >= :startOfMonth AND dateCreated <= :endOfMonth")
    suspend fun getMonthlyJobsCount(startOfMonth: Long, endOfMonth: Long): Int
    
    @Query("SELECT COUNT(*) FROM jobs WHERE status = :status")
    suspend fun getJobsCountByStatus(status: JobStatus): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job): Long
    
    @Update
    suspend fun updateJob(job: Job)
    
    @Delete
    suspend fun deleteJob(job: Job)
    
    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJobById(jobId: Long)
}
