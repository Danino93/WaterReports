package com.ashaf.instanz.data.repositories

import com.ashaf.instanz.data.database.JobDao
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JobRepository(private val jobDao: JobDao) {
    
    fun getAllJobs(): Flow<List<Job>> = jobDao.getAllJobs()
    
    suspend fun getJobById(jobId: Long): Job? = jobDao.getJobById(jobId)
    
    fun getJobsByStatus(status: JobStatus): Flow<List<Job>> = jobDao.getJobsByStatus(status)
    
    fun searchJobs(query: String): Flow<List<Job>> {
        val searchQuery = "%$query%"
        return jobDao.searchJobs(searchQuery)
    }
    
    suspend fun getTotalJobsCount(): Int = jobDao.getTotalJobsCount()
    
    suspend fun getMonthlyJobsCount(startOfMonth: Long, endOfMonth: Long): Int =
        jobDao.getMonthlyJobsCount(startOfMonth, endOfMonth)
    
    suspend fun getDraftJobsCount(): Int =
        jobDao.getJobsCountByStatus(JobStatus.DRAFT)
    
    suspend fun insertJob(job: Job): Long = jobDao.insertJob(job)
    
    suspend fun updateJob(job: Job) = jobDao.updateJob(job)
    
    suspend fun deleteJob(job: Job) = jobDao.deleteJob(job)
    
    suspend fun deleteJobById(jobId: Long) = jobDao.deleteJobById(jobId)
}

