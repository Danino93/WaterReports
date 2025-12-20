package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashaf.instanz.data.repositories.ImageRepository

class ImageViewModelFactory(
    private val imageRepository: ImageRepository,
    private val jobId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImageViewModel(imageRepository, jobId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

