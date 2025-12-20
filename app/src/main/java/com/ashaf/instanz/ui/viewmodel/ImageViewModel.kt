package com.ashaf.instanz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashaf.instanz.data.models.JobImage
import com.ashaf.instanz.data.repositories.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ImageViewModel(
    private val imageRepository: ImageRepository,
    private val jobId: Long
) : ViewModel() {
    
    private val _images = MutableStateFlow<List<JobImage>>(emptyList())
    val images: StateFlow<List<JobImage>> = _images.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadImages()
    }
    
    private fun loadImages() {
        viewModelScope.launch {
            imageRepository.getImagesForJob(jobId).collect { imageList ->
                _images.value = imageList
            }
        }
    }
    
    fun getImagesForSection(sectionId: String, fieldId: String = ""): List<JobImage> {
        return _images.value.filter { it.sectionId == sectionId }
    }
    
    fun addImage(
        filePath: String,
        sectionId: String,
        fieldId: String,
        caption: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = File(filePath)
                if (file.exists()) {
                    // Get current max order for this section
                    val currentImages = _images.value.filter { it.sectionId == sectionId }
                    val nextOrder = (currentImages.maxOfOrNull { it.order } ?: -1) + 1
                    
                    val image = JobImage(
                        jobId = jobId,
                        sectionId = sectionId,
                        filePath = filePath,
                        caption = caption,
                        order = nextOrder
                    )
                    imageRepository.insertImage(image)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateImageCaption(imageId: Long, newCaption: String) {
        viewModelScope.launch {
            val image = _images.value.find { it.id == imageId }
            if (image != null) {
                imageRepository.updateImage(image.copy(caption = newCaption))
            }
        }
    }
    
    fun deleteImage(image: JobImage) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Delete file
                val file = File(image.filePath)
                if (file.exists()) {
                    file.delete()
                }
                // Delete from database
                imageRepository.deleteImage(image)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteImagesForSection(sectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sectionImages = _images.value.filter { it.sectionId == sectionId }
                sectionImages.forEach { image ->
                    val file = File(image.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                imageRepository.deleteImagesForSection(jobId, sectionId)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

