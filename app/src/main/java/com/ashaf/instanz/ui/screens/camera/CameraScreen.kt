package com.ashaf.instanz.ui.screens.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.viewmodel.ImageViewModel
import com.ashaf.instanz.ui.viewmodel.ImageViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    jobId: Long,
    sectionId: String,
    fieldId: String,
    onImageCaptured: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val imageViewModel: ImageViewModel = viewModel(
        factory = ImageViewModelFactory(
            imageRepository = appContainer.imageRepository,
            jobId = jobId
        )
    )
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    when {
        cameraPermissionState.status.isGranted -> {
            CameraContent(
                jobId = jobId,
                sectionId = sectionId,
                fieldId = fieldId,
                imageViewModel = imageViewModel,
                onImageCaptured = onImageCaptured,
                onBackClick = onBackClick
            )
        }
        else -> {
            PermissionDeniedContent(
                permissionState = cameraPermissionState,
                onBackClick = onBackClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun PermissionDeniedContent(
    permissionState: PermissionState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("צילום תמונה") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "חזרה")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "נדרשת הרשאה למצלמה",
                    style = MaterialTheme.typography.headlineSmall
                )
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("אשר הרשאה")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraContent(
    jobId: Long,
    sectionId: String,
    fieldId: String,
    imageViewModel: ImageViewModel,
    onImageCaptured: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    val cameraSelector = remember(lensFacing) {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("צילום תמונה") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "חזרה")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageCaptureBuilder = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        imageCapture = imageCaptureBuilder.build()
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Camera Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Flip Camera Button
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "הפוך מצלמה",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Capture Button
                IconButton(
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            captureImage(
                                context = context,
                                imageCapture = imageCapture,
                                jobId = jobId,
                                sectionId = sectionId,
                                fieldId = fieldId,
                                onSuccess = { filePath ->
                                    // Save to database
                                    imageViewModel.addImage(
                                        filePath = filePath,
                                        sectionId = sectionId,
                                        fieldId = fieldId,
                                        caption = ""
                                    )
                                    isCapturing = false
                                    onImageCaptured(filePath)
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        }
                    },
                    enabled = !isCapturing,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "צלם תמונה",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture?,
    jobId: Long,
    sectionId: String,
    fieldId: String,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    val imageCapture = imageCapture ?: run {
        onError()
        return
    }
    
    // Create file name
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_${jobId}_${sectionId}_${fieldId}_${timestamp}.jpg"
    
    // Create images directory
    val imagesDir = File(context.filesDir, "job_images").apply {
        if (!exists()) mkdirs()
    }
    
    val photoFile = File(imagesDir, fileName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSuccess(photoFile.absolutePath)
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Image capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}

