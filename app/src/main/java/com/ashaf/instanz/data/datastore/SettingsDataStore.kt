package com.ashaf.instanz.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    
    companion object {
        // Keys for all settings
        private val VERSION_NUMBER = stringPreferencesKey("version_number")
        private val LANGUAGE = stringPreferencesKey("language")
        private val REPORT_COLOR_SCHEME = stringPreferencesKey("report_color_scheme")
        private val MAX_JOBS_PER_SCREEN = floatPreferencesKey("max_jobs_per_screen")
        private val IMAGES_PER_ROW = floatPreferencesKey("images_per_row")
        private val SPECIAL_IMAGES_PER_ROW = floatPreferencesKey("special_images_per_row")
        private val VAT_PERCENT = floatPreferencesKey("vat_percent")
        private val SHOW_HEADER_IN_REPORT = booleanPreferencesKey("show_header_in_report")
        private val TEXT_FORMATTING_SUPPORT = booleanPreferencesKey("text_formatting_support")
        private val QUICK_PHOTO = booleanPreferencesKey("quick_photo")
        private val SHOW_CHART_IN_REPORT = booleanPreferencesKey("show_chart_in_report")
        private val SHOW_ONLY_MULTIPLE_STATUSES = booleanPreferencesKey("show_only_multiple_statuses")
        private val UNLIMITED_JOBS = booleanPreferencesKey("unlimited_jobs")
        private val ACCOUNT_STATUS = stringPreferencesKey("account_status")
        private val LAST_BACKUP_DATE = longPreferencesKey("last_backup_date")
        private val LAST_INVOICE_NUMBER = intPreferencesKey("last_invoice_number")
    }
    
    // Version Number
    val versionNumber: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[VERSION_NUMBER] ?: "389"
    }
    
    suspend fun saveVersionNumber(version: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[VERSION_NUMBER] = version
        }
    }
    
    // Language
    val language: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "Hebrew/עברית"
    }
    
    suspend fun saveLanguage(lang: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }
    
    // Report Color Scheme
    val reportColorScheme: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[REPORT_COLOR_SCHEME] ?: "בהיר"
    }
    
    suspend fun saveReportColorScheme(scheme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[REPORT_COLOR_SCHEME] = scheme
        }
    }
    
    // Max Jobs Per Screen
    val maxJobsPerScreen: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[MAX_JOBS_PER_SCREEN] ?: 100f
    }
    
    suspend fun saveMaxJobsPerScreen(max: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[MAX_JOBS_PER_SCREEN] = max
        }
    }
    
    // Images Per Row
    val imagesPerRow: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[IMAGES_PER_ROW] ?: 2f
    }
    
    suspend fun saveImagesPerRow(count: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[IMAGES_PER_ROW] = count
        }
    }
    
    // Special Images Per Row
    val specialImagesPerRow: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[SPECIAL_IMAGES_PER_ROW] ?: 2f
    }
    
    suspend fun saveSpecialImagesPerRow(count: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[SPECIAL_IMAGES_PER_ROW] = count
        }
    }
    
    // VAT Percent
    val vatPercent: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[VAT_PERCENT] ?: 18f
    }
    
    suspend fun saveVatPercent(percent: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[VAT_PERCENT] = percent
        }
    }
    
    // Show Header In Report
    val showHeaderInReport: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SHOW_HEADER_IN_REPORT] ?: true
    }
    
    suspend fun saveShowHeaderInReport(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_HEADER_IN_REPORT] = show
        }
    }
    
    // Text Formatting Support
    val textFormattingSupport: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[TEXT_FORMATTING_SUPPORT] ?: false
    }
    
    suspend fun saveTextFormattingSupport(support: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[TEXT_FORMATTING_SUPPORT] = support
        }
    }
    
    // Quick Photo
    val quickPhoto: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[QUICK_PHOTO] ?: true
    }
    
    suspend fun saveQuickPhoto(quick: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[QUICK_PHOTO] = quick
        }
    }
    
    // Show Chart In Report
    val showChartInReport: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SHOW_CHART_IN_REPORT] ?: true
    }
    
    suspend fun saveShowChartInReport(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_CHART_IN_REPORT] = show
        }
    }
    
    // Show Only Multiple Statuses
    val showOnlyMultipleStatuses: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SHOW_ONLY_MULTIPLE_STATUSES] ?: false
    }
    
    suspend fun saveShowOnlyMultipleStatuses(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_ONLY_MULTIPLE_STATUSES] = show
        }
    }
    
    // Unlimited Jobs
    val unlimitedJobs: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[UNLIMITED_JOBS] ?: true // ברירת מחדל: ללא הגבלה
    }
    
    suspend fun saveUnlimitedJobs(unlimited: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[UNLIMITED_JOBS] = unlimited
        }
    }
    
    // Account Status
    val accountStatus: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[ACCOUNT_STATUS] ?: "חשבון פה תוקף"
    }
    
    suspend fun saveAccountStatus(status: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[ACCOUNT_STATUS] = status
        }
    }
    
    // Last Backup Date
    val lastBackupDate: Flow<Long> = context.settingsDataStore.data.map { preferences ->
        preferences[LAST_BACKUP_DATE] ?: 0L
    }
    
    suspend fun saveLastBackupDate(date: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_BACKUP_DATE] = date
        }
    }
    
    // Last Invoice Number - for sequential invoice numbering (legally required)
    val lastInvoiceNumber: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[LAST_INVOICE_NUMBER] ?: 0
    }
    
    suspend fun getNextInvoiceNumber(): Int {
        var nextNumber = 0
        context.settingsDataStore.edit { preferences ->
            val current = preferences[LAST_INVOICE_NUMBER] ?: 0
            nextNumber = current + 1
            preferences[LAST_INVOICE_NUMBER] = nextNumber
        }
        return nextNumber
    }
    
    suspend fun saveLastInvoiceNumber(number: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_INVOICE_NUMBER] = number
        }
    }
}

