package com.ashaf.instanz.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: String,
    val jobNumber: String,
    val title: String,
    val clientFirstName: String,
    val clientLastName: String,
    val clientPhone: String,
    val clientAddress: String,
    val clientCompany: String? = null,
    val dateCreated: Long,
    val dateModified: Long,
    val status: JobStatus,
    val dataJson: String, // JSON של כל הנתונים
    val buildingImagePath: String? = null,
    val jobSettingsJson: String? = null // JSON של הגדרות עבודה
) {
    fun getJobSettings(): JobSettings {
        return if (jobSettingsJson != null) {
            try {
                com.google.gson.Gson().fromJson(jobSettingsJson, JobSettings::class.java)
            } catch (e: Exception) {
                JobSettings.default()
            }
        } else {
            JobSettings.default()
        }
    }
}

enum class JobStatus {
    DRAFT,       // טיוטה
    IN_PROGRESS, // בתהליך
    COMPLETED,   // הושלם
    SENT,        // נשלח
    ARCHIVED     // ארכיון
}

// Client data class for easier handling
data class Client(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val address: String,
    val company: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"
}
