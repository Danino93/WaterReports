package com.ashaf.instanz.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val COMPANY_PHONE = stringPreferencesKey("company_phone")
        val COMPANY_EMAIL = stringPreferencesKey("company_email")
        val COMPANY_ADDRESS = stringPreferencesKey("company_address")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val GOOGLE_DRIVE_CONNECTED = booleanPreferencesKey("google_drive_connected")
    }
    
    // Company Info
    val companyName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COMPANY_NAME] ?: ""
    }
    
    val companyPhone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COMPANY_PHONE] ?: ""
    }
    
    val companyEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COMPANY_EMAIL] ?: ""
    }
    
    val companyAddress: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COMPANY_ADDRESS] ?: ""
    }
    
    // Backup Settings
    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_ENABLED] ?: false
    }
    
    val lastBackupTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_TIME] ?: 0L
    }
    
    val googleDriveConnected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[GOOGLE_DRIVE_CONNECTED] ?: false
    }
    
    // Save Company Info
    suspend fun saveCompanyInfo(name: String, phone: String, email: String, address: String) {
        context.dataStore.edit { prefs ->
            prefs[COMPANY_NAME] = name
            prefs[COMPANY_PHONE] = phone
            prefs[COMPANY_EMAIL] = email
            prefs[COMPANY_ADDRESS] = address
        }
    }
    
    // Backup Settings
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun updateLastBackupTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = time
        }
    }
    
    suspend fun setGoogleDriveConnected(connected: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[GOOGLE_DRIVE_CONNECTED] = connected
        }
    }
}

