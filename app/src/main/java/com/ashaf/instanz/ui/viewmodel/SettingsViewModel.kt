package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // All settings as StateFlows
    val versionNumber: StateFlow<String> = settingsDataStore.versionNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "389")
    
    val language: StateFlow<String> = settingsDataStore.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Hebrew/עברית")
    
    val reportColorScheme: StateFlow<String> = settingsDataStore.reportColorScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "בהיר")
    
    val maxJobsPerScreen: StateFlow<Float> = settingsDataStore.maxJobsPerScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100f)
    
    val imagesPerRow: StateFlow<Float> = settingsDataStore.imagesPerRow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2f)
    
    val specialImagesPerRow: StateFlow<Float> = settingsDataStore.specialImagesPerRow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2f)
    
    val vatPercent: StateFlow<Float> = settingsDataStore.vatPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)
    
    val showHeaderInReport: StateFlow<Boolean> = settingsDataStore.showHeaderInReport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val textFormattingSupport: StateFlow<Boolean> = settingsDataStore.textFormattingSupport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val quickPhoto: StateFlow<Boolean> = settingsDataStore.quickPhoto
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val showChartInReport: StateFlow<Boolean> = settingsDataStore.showChartInReport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val showOnlyMultipleStatuses: StateFlow<Boolean> = settingsDataStore.showOnlyMultipleStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val unlimitedJobs: StateFlow<Boolean> = settingsDataStore.unlimitedJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val accountStatus: StateFlow<String> = settingsDataStore.accountStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "חשבון פה תוקף")
    
    val lastBackupDate: StateFlow<Long> = settingsDataStore.lastBackupDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Save functions
    fun saveLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.saveLanguage(language)
        }
    }
    
    fun saveReportColorScheme(scheme: String) {
        viewModelScope.launch {
            settingsDataStore.saveReportColorScheme(scheme)
        }
    }
    
    fun saveMaxJobsPerScreen(max: Float) {
        viewModelScope.launch {
            settingsDataStore.saveMaxJobsPerScreen(max)
        }
    }
    
    fun saveImagesPerRow(count: Float) {
        viewModelScope.launch {
            settingsDataStore.saveImagesPerRow(count)
        }
    }
    
    fun saveSpecialImagesPerRow(count: Float) {
        viewModelScope.launch {
            settingsDataStore.saveSpecialImagesPerRow(count)
        }
    }
    
    fun saveVatPercent(percent: Float) {
        viewModelScope.launch {
            settingsDataStore.saveVatPercent(percent)
        }
    }
    
    fun saveShowHeaderInReport(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveShowHeaderInReport(show)
        }
    }
    
    fun saveTextFormattingSupport(support: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveTextFormattingSupport(support)
        }
    }
    
    fun saveQuickPhoto(quick: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveQuickPhoto(quick)
        }
    }
    
    fun saveShowChartInReport(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveShowChartInReport(show)
        }
    }
    
    fun saveShowOnlyMultipleStatuses(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveShowOnlyMultipleStatuses(show)
        }
    }
    
    fun saveUnlimitedJobs(unlimited: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveUnlimitedJobs(unlimited)
        }
    }
    
    fun updateLastBackupDate() {
        viewModelScope.launch {
            settingsDataStore.saveLastBackupDate(System.currentTimeMillis())
        }
    }
}

