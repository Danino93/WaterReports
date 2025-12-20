package com.ashaf.instanz.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashaf.instanz.data.datastore.SettingsDataStore
import com.ashaf.instanz.data.repositories.ImageRepository
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository

class PdfViewModelFactory(
    private val context: Context,
    private val jobRepository: JobRepository,
    private val templateRepository: TemplateRepository,
    private val imageRepository: ImageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfViewModel(context, jobRepository, templateRepository, imageRepository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

