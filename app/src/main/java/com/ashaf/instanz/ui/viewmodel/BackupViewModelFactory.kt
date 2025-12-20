package com.ashaf.instanz.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashaf.instanz.data.database.AppDatabase
import com.ashaf.instanz.data.datastore.PreferencesManager

class BackupViewModelFactory(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(context, database, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

