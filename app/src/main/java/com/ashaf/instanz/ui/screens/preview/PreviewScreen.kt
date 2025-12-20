package com.ashaf.instanz.ui.screens.preview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ashaf.instanz.data.models.JobImage
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    jobId: Long,
    onBackClick: () -> Unit,
    onExportPdfClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    
    val editorViewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory(
            jobRepository = appContainer.jobRepository,
            templateRepository = appContainer.templateRepository,
            jobId = jobId
        )
    )
    
    val imageViewModel: ImageViewModel = viewModel(
        factory = ImageViewModelFactory(
            imageRepository = appContainer.imageRepository,
            jobId = jobId
        )
    )
    
    val pdfViewModel: PdfViewModel = viewModel(
        factory = PdfViewModelFactory(
            context = context,
            jobRepository = appContainer.jobRepository,
            templateRepository = appContainer.templateRepository,
            imageRepository = appContainer.imageRepository,
            settingsDataStore = appContainer.settingsDataStore
        )
    )
    
    val job by editorViewModel.job.collectAsState()
    val template by editorViewModel.template.collectAsState()
    val images by imageViewModel.images.collectAsState()
    val isLoading by editorViewModel.isLoading.collectAsState()
    
    val isGeneratingPdf by pdfViewModel.isGenerating.collectAsState()
    val generatedPdfFile by pdfViewModel.generatedPdfFile.collectAsState()
    val pdfError by pdfViewModel.error.collectAsState()
    
    // Handle PDF generation result
    LaunchedEffect(generatedPdfFile) {
        generatedPdfFile?.let { pdfFile ->
            // Share PDF
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "שתף דוח PDF"))
            pdfViewModel.clearGeneratedPdf()
        }
    }
    
    // Show error dialog
    pdfError?.let { error ->
        AlertDialog(
            onDismissRequest = { pdfViewModel.clearError() },
            title = { Text("שגיאה") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { pdfViewModel.clearError() }) {
                    Text("אישור")
                }
            }
        )
    }
    
    val sectionsJson = remember(template) {
        template?.jsonData?.let { jsonString ->
            try {
                val gson = Gson()
                val templateObj = gson.fromJson(jsonString, JsonObject::class.java)
                val sectionsArray = templateObj.getAsJsonArray("sections")
                if (sectionsArray != null) {
                    val sectionsList = mutableListOf<JsonObject>()
                    sectionsArray.forEach { element ->
                        sectionsList.add(element.asJsonObject)
                    }
                    sectionsList.sortedBy { it.get("order")?.asInt ?: 0 }.toList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "תצוגה מקדימה",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "חזרה")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { pdfViewModel.generatePdf(jobId) },
                        enabled = !isGeneratingPdf
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.PictureAsPdf, "ייצא PDF")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
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
                .background(Background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                job != null && template != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        PreviewHeaderCard(
                            jobTitle = "${template!!.name} - דוח ${job!!.jobNumber}",
                            clientFirstName = job!!.clientFirstName,
                            clientLastName = job!!.clientLastName,
                            clientPhone = job!!.clientPhone,
                            clientAddress = job!!.clientAddress,
                            dateCreated = job!!.dateCreated,
                            dateModified = job!!.dateModified
                        )
                        
                        // Sections
                        sectionsJson.forEach { sectionJson ->
                            PreviewSectionCard(
                                sectionJson = sectionJson,
                                viewModel = editorViewModel,
                                images = images
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        "שגיאה בטעינת הדוח",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewHeaderCard(
    jobTitle: String,
    clientFirstName: String,
    clientLastName: String,
    clientPhone: String,
    clientAddress: String,
    dateCreated: Long,
    dateModified: Long
) {
    val clientName = "$clientFirstName $clientLastName"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = jobTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            // Client Info
            InfoRow(label = "שם לקוח", value = clientName)
            InfoRow(label = "טלפון", value = clientPhone)
            InfoRow(label = "כתובת", value = clientAddress)
            
            Divider()
            
            // Dates
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("he", "IL"))
            InfoRow(label = "נוצר בתאריך", value = dateFormat.format(Date(dateCreated)))
            InfoRow(label = "עודכן בתאריך", value = dateFormat.format(Date(dateModified)))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun PreviewSectionCard(
    sectionJson: JsonObject,
    viewModel: EditorViewModel,
    images: List<JobImage>
) {
    val sectionId = sectionJson.get("id")?.asString ?: ""
    val sectionTitle = sectionJson.get("title")?.asString ?: ""
    val fieldsJson = sectionJson.getAsJsonArray("fields")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            fieldsJson?.forEach { fieldJsonElement ->
                val fieldObj = fieldJsonElement.asJsonObject
                val fieldType = fieldObj.get("type")?.asString ?: ""
                val fieldId = fieldObj.get("id")?.asString ?: ""
                val fieldLabel = fieldObj.get("label")?.asString ?: ""
                
                when (fieldType) {
                    "text", "textarea", "number" -> {
                        val value = viewModel.getFieldValue(sectionId, fieldId)
                        if (value.isNotBlank()) {
                            PreviewTextField(label = fieldLabel, value = value)
                        }
                    }
                    "checkbox" -> {
                        val value = viewModel.getFieldValue(sectionId, fieldId)
                        val isChecked = value.toBooleanStrictOrNull() ?: false
                        PreviewCheckboxField(label = fieldLabel, checked = isChecked)
                    }
                    "image" -> {
                        val fieldImages = images.filter { 
                            it.sectionId == sectionId
                        }
                        if (fieldImages.isNotEmpty()) {
                            PreviewImageField(label = fieldLabel, images = fieldImages)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTextField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun PreviewCheckboxField(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) Primary else TextSecondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun PreviewImageField(label: String, images: List<JobImage>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        images.forEach { image ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    AsyncImage(
                        model = File(image.filePath),
                        contentDescription = image.caption?.ifEmpty { "תמונה" } ?: "תמונה",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentScale = ContentScale.Crop
                    )
                    
            if (image.caption?.isNotEmpty() == true) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = image.caption ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        color = TextPrimary
                    )
                }
            }
                }
            }
        }
    }
}

