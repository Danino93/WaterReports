package com.ashaf.instanz.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.database.AppDatabase
import com.ashaf.instanz.data.datastore.PreferencesManager
import com.ashaf.instanz.utils.BackupManager
import com.ashaf.instanz.utils.GoogleDriveManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val googleDriveManager = GoogleDriveManager(context)
    private val backupManager = BackupManager(context, database)
    
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()
    
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()
    
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun getSignInIntent() = googleDriveManager.getSignInIntent()
    
    fun handleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            if (account != null) {
                googleDriveManager.initializeDriveService(account)
                preferencesManager.setGoogleDriveConnected(true)
                _backupStatus.value = "התחברת בהצלחה ל-Google Drive"
            } else {
                _error.value = "ההתחברות נכשלה"
            }
        }
    }
    
    fun isSignedIn() = googleDriveManager.isSignedIn()
    
    fun signOut() {
        viewModelScope.launch {
            googleDriveManager.signOut()
            preferencesManager.setGoogleDriveConnected(false)
            _backupStatus.value = "התנתקת מ-Google Drive"
        }
    }
    
    fun backupToGoogleDrive() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _error.value = null
            _backupStatus.value = "יוצר גיבוי..."
            
            try {
                // Create backup file
                val backupFile = backupManager.createBackup()
                if (backupFile == null) {
                    _error.value = "שגיאה ביצירת קובץ הגיבוי"
                    return@launch
                }
                
                _backupStatus.value = "מעלה ל-Google Drive..."
                
                // Upload to Google Drive
                val fileId = googleDriveManager.uploadFile(
                    localFile = backupFile,
                    fileName = backupFile.name,
                    mimeType = "application/zip"
                )
                
                if (fileId != null) {
                    preferencesManager.updateLastBackupTime(System.currentTimeMillis())
                    _backupStatus.value = "הגיבוי הושלם בהצלחה"
                    
                    // Delete local backup file
                    backupFile.delete()
                } else {
                    _error.value = "שגיאה בהעלאה ל-Google Drive"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "שגיאה בגיבוי: ${e.message}"
            } finally {
                _isBackingUp.value = false
            }
        }
    }
    
    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _isRestoring.value = true
            _error.value = null
            _backupStatus.value = "מחפש גיבויים..."
            
            try {
                // List backup files
                val files = googleDriveManager.listFilesInAppFolder()
                val backupFiles = files.filter { it.name.startsWith("backup_") && it.name.endsWith(".zip") }
                
                if (backupFiles.isEmpty()) {
                    _error.value = "לא נמצאו קבצי גיבוי"
                    return@launch
                }
                
                // Get latest backup
                val latestBackup = backupFiles.maxByOrNull { it.createdTime?.value ?: 0L }
                if (latestBackup == null) {
                    _error.value = "לא נמצא גיבוי"
                    return@launch
                }
                
                _backupStatus.value = "מוריד גיבוי..."
                
                // Download backup file
                val tempBackupFile = java.io.File(context.cacheDir, "temp_backup.zip")
                val downloaded = googleDriveManager.downloadFile(latestBackup.id, tempBackupFile)
                
                if (!downloaded) {
                    _error.value = "שגיאה בהורדת הגיבוי"
                    return@launch
                }
                
                _backupStatus.value = "משחזר נתונים..."
                
                // Restore backup
                val restored = backupManager.restoreBackup(tempBackupFile)
                
                if (restored) {
                    _backupStatus.value = "השחזור הושלם בהצלחה. יש להפעיל מחדש את האפליקציה"
                } else {
                    _error.value = "שגיאה בשחזור הנתונים"
                }
                
                // Delete temp file
                tempBackupFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "שגיאה בשחזור: ${e.message}"
            } finally {
                _isRestoring.value = false
            }
        }
    }
    
    fun clearStatus() {
        _backupStatus.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}

