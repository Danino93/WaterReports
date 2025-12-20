package com.ashaf.instanz.data.database

import androidx.room.TypeConverter
import com.ashaf.instanz.data.models.JobStatus

class Converters {
    @TypeConverter
    fun fromJobStatus(status: JobStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toJobStatus(status: String): JobStatus {
        return JobStatus.valueOf(status)
    }
}
