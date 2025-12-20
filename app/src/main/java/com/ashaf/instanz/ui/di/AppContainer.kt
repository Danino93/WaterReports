package com.ashaf.instanz.ui.di

import android.content.Context
import com.ashaf.instanz.data.database.AppDatabase
import com.ashaf.instanz.data.datastore.SettingsDataStore
import com.ashaf.instanz.data.repositories.JobRepository
import com.ashaf.instanz.data.repositories.TemplateRepository
import com.ashaf.instanz.data.repositories.ImageRepository

data class AppContainer(
    val jobRepository: JobRepository,
    val templateRepository: TemplateRepository,
    val imageRepository: ImageRepository,
    val settingsDataStore: SettingsDataStore
) {
    companion object {
        fun create(context: Context): AppContainer {
            val database = AppDatabase.getDatabase(context)
            return AppContainer(
                jobRepository = JobRepository(database.jobDao()),
                templateRepository = TemplateRepository(database.templateDao()),
                imageRepository = ImageRepository(database.jobImageDao()),
                settingsDataStore = SettingsDataStore(context)
            )
        }
    }
}

