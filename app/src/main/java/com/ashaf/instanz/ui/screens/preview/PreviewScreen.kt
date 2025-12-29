package com.ashaf.instanz.ui.screens.preview

import android.content.Intent
import android.os.Environment
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
import com.ashaf.instanz.data.models.TemplateCustomContent
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
    
    var showPdfOptionsDialog by remember { mutableStateOf(false) }
    var currentPdfFile by remember { mutableStateOf<File?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // Handle PDF generation result
    LaunchedEffect(generatedPdfFile) {
        generatedPdfFile?.let { pdfFile ->
            currentPdfFile = pdfFile
            showPdfOptionsDialog = true
        }
    }
    
    // PDF Options Dialog
    if (showPdfOptionsDialog && currentPdfFile != null) {
        AlertDialog(
            onDismissRequest = {
                showPdfOptionsDialog = false
                pdfViewModel.clearGeneratedPdf()
            },
            title = {
                Text(
                    "הדוח נוצר בהצלחה!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("מה תרצה לעשות עם הדוח?")
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save to device button
                    Button(
                        onClick = {
                            currentPdfFile?.let { pdfFile ->
                                try {
                                    // Copy to Downloads folder
                                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    if (!downloadsDir.exists()) {
                                        downloadsDir.mkdirs()
                                    }
                                    
                                    val destinationFile = File(downloadsDir, pdfFile.name)
                                    FileInputStream(pdfFile).use { input ->
                                        FileOutputStream(destinationFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    
                                    // Open the PDF file after saving
                                    try {
                                        // Use FileProvider to create URI for the saved file
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            destinationFile
                                        )
                                        
                                        val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        
                                        // Try to open the PDF
                                        context.startActivity(openIntent)
                                        successMessage = "הדוח נשמר ונפתח בהצלחה!"
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        // No PDF viewer app found
                                        android.util.Log.w("PreviewScreen", "No PDF viewer app found")
                                        successMessage = "הדוח נשמר בהצלחה!\nמיקום: Downloads/${pdfFile.name}\n\nאנא התקן אפליקציית PDF viewer כדי לפתוח את הקובץ."
                                    } catch (e: Exception) {
                                        // Other error (permissions, etc.)
                                        android.util.Log.w("PreviewScreen", "Could not open PDF: ${e.message}")
                                        successMessage = "הדוח נשמר בהצלחה!\nמיקום: Downloads/${pdfFile.name}"
                                    }
                                    showSuccessMessage = true
                                } catch (e: Exception) {
                                    pdfViewModel.setError("שגיאה בשמירת הקובץ: ${e.message}")
                                }
                            }
                            showPdfOptionsDialog = false
                            pdfViewModel.clearGeneratedPdf()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שמור על המכשיר")
                    }
                    
                    // Share button
                    OutlinedButton(
                        onClick = {
                            currentPdfFile?.let { pdfFile ->
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
                            }
                            showPdfOptionsDialog = false
                            pdfViewModel.clearGeneratedPdf()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שתף")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPdfOptionsDialog = false
                        pdfViewModel.clearGeneratedPdf()
                    }
                ) {
                    Text("ביטול")
                }
            }
        )
    }
    
    // Success message snackbar
    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("הצלחה!") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(onClick = { showSuccessMessage = false }) {
                    Text("אישור")
                }
            }
        )
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
    
    // Load custom content (same logic as PdfGenerator)
    val customContent = remember(job, template) {
        val currentJob = job // Local copy for smart cast
        try {
            val gson = Gson()
            val jobDataJson = if (currentJob?.dataJson?.isNotBlank() == true && currentJob.dataJson != "{}") {
                gson.fromJson(currentJob.dataJson, JsonObject::class.java)
            } else null

            if (jobDataJson?.has("customContent") == true) {
                // Use job-specific custom content
                val customContentElement = jobDataJson.get("customContent")
                
                android.util.Log.d("PreviewScreen", "==== Loading customContent for Preview ====")
                android.util.Log.d("PreviewScreen", "customContent type: ${if (customContentElement.isJsonObject) "JsonObject" else "String"}")
                
                if (customContentElement.isJsonObject) {
                    // New format: customContent is stored as JsonObject
                    android.util.Log.d("PreviewScreen", "✅ Using new format (JsonObject)")
                    gson.fromJson(customContentElement, TemplateCustomContent::class.java)
                } else {
                    // Old format: customContent is stored as String (backward compatibility)
                    android.util.Log.d("PreviewScreen", "⚠️ Using old format (String) - backward compatibility")
                    gson.fromJson(
                        customContentElement.asString,
                        TemplateCustomContent::class.java
                    )
                }
            } else {
                // Fallback to template defaults
                android.util.Log.d("PreviewScreen", "⚠️ No job-specific customContent, using template defaults")
                template?.parseCustomContent()
            }
        } catch (e: Exception) {
            // Fallback to template defaults
            android.util.Log.e("PreviewScreen", "❌ Error loading customContent: ${e.message}", e)
            template?.parseCustomContent()
        }
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
                        // Custom Header (Logo, Title, Contact)
                        customContent?.let { content ->
                            PreviewCustomHeader(
                                jobTitle = "${template!!.name} - דוח ${job!!.jobNumber}",
                                logoImagePath = content.logoImagePath,
                                contactImagePath = content.contactImagePath,
                                phone = content.phone,
                                email = content.email,
                                businessNumber = content.businessNumber,
                                website = content.website
                            )
                        } ?: run {
                            // Default header if no custom content
                            PreviewHeaderCard(
                                jobTitle = "${template!!.name} - דוח ${job!!.jobNumber}",
                                clientFirstName = job!!.clientFirstName,
                                clientLastName = job!!.clientLastName,
                                clientPhone = job!!.clientPhone,
                                clientAddress = job!!.clientAddress,
                                dateCreated = job!!.dateCreated,
                                dateModified = job!!.dateModified
                            )
                        }
                        
                        // Client Info
                        PreviewClientCard(
                            clientFirstName = job!!.clientFirstName,
                            clientLastName = job!!.clientLastName,
                            clientPhone = job!!.clientPhone,
                            clientAddress = job!!.clientAddress,
                            dateCreated = job!!.dateCreated,
                            dateModified = job!!.dateModified
                        )
                        
                        // Custom Content Sections
                        customContent?.let { content ->
                            // Top fields
                            if (content.visitReason.isNotEmpty() || content.company.isNotEmpty()) {
                                PreviewCustomSection(
                                    title = "פרטים כלליים",
                                    items = listOfNotNull(
                                        if (content.visitReason.isNotEmpty()) "סיבת הביקור: ${content.visitReason}" else null,
                                        if (content.company.isNotEmpty()) "חברה: ${content.company}" else null
                                    )
                                )
                            }
                            
                            // Inspector details
                            if (content.inspectorName.isNotEmpty() || content.experienceTitle.isNotEmpty() || content.experienceText.isNotEmpty()) {
                                PreviewInspectorSection(
                                    inspectorName = content.inspectorName,
                                    experienceTitle = content.experienceTitle,
                                    experienceText = content.experienceText,
                                    certificateImagePath = content.certificateImagePath
                                )
                            }
                            
                            // Custom sections
                            val sectionOrder = listOf(
                                "intro_report", "conclusion", "intro_work", 
                                "intro_activities", "intro_recommendations"
                            )
                            sectionOrder.forEach { sectionId ->
                                content.sections[sectionId]?.let { items ->
                                    if (items.isNotEmpty()) {
                                        PreviewCustomSection(
                                            title = when (sectionId) {
                                                "intro_report" -> "הקדמה - דוח"
                                                "conclusion" -> "בסיום"
                                                "intro_work" -> "הקדמה - עבודה"
                                                "intro_activities" -> "פעילות הקדמה"
                                                "intro_recommendations" -> "המלצת הקדמה"
                                                else -> sectionId
                                            },
                                            items = items.sortedBy { it.order }.map { it.text }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Dynamic Sections from template
                        sectionsJson.forEach { sectionJson ->
                            PreviewSectionCard(
                                sectionJson = sectionJson,
                                viewModel = editorViewModel,
                                images = images
                            )
                        }
                        
                        // Findings Section
                        PreviewFindingsSection(
                            job = job!!,
                            images = images
                        )
                        
                        // Recommendations Summary with Prices
                        PreviewRecommendationsSummary(
                            job = job!!
                        )
                        
                        // Summary sections
                        customContent?.let { content ->
                            val summarySectionOrder = listOf(
                                "summary_recommendations", "summary_activities", 
                                "work_summary", "report_summary"
                            )
                            summarySectionOrder.forEach { sectionId ->
                                content.sections[sectionId]?.let { items ->
                                    if (items.isNotEmpty()) {
                                        PreviewCustomSection(
                                            title = when (sectionId) {
                                                "summary_recommendations" -> "המלצת סיכום"
                                                "summary_activities" -> "פעילות סיכום"
                                                "work_summary" -> "סיכום - עבודה"
                                                "report_summary" -> "סיכום - דוח"
                                                else -> sectionId
                                            },
                                            items = items.sortedBy { it.order }.map { it.text }
                                        )
                                    }
                                }
                            }
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

@Composable
private fun PreviewCustomHeader(
    jobTitle: String,
    logoImagePath: String?,
    contactImagePath: String?,
    phone: String,
    email: String,
    businessNumber: String,
    website: String
) {
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
            // Logo
            logoImagePath?.let { path ->
                if (File(path).exists()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "לוגו",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Text(
                text = jobTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            // Contact info
            if (phone.isNotEmpty()) InfoRow(label = "טלפון", value = phone)
            if (email.isNotEmpty()) InfoRow(label = "אימייל", value = email)
            if (businessNumber.isNotEmpty()) InfoRow(label = "ה.פ", value = businessNumber)
            if (website.isNotEmpty()) InfoRow(label = "אתר", value = website)
            
            // Contact image
            contactImagePath?.let { path ->
                if (File(path).exists()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "כותרת תחתונה",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewClientCard(
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
                text = "פרטי לקוח",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            InfoRow(label = "שם לקוח", value = clientName)
            InfoRow(label = "טלפון", value = clientPhone)
            InfoRow(label = "כתובת", value = clientAddress)
            
            Divider()
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("he", "IL"))
            InfoRow(label = "נוצר בתאריך", value = dateFormat.format(Date(dateCreated)))
            InfoRow(label = "עודכן בתאריך", value = dateFormat.format(Date(dateModified)))
        }
    }
}

@Composable
private fun PreviewInspectorSection(
    inspectorName: String,
    experienceTitle: String,
    experienceText: String,
    certificateImagePath: String?
) {
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
                text = "פרטי מומחה",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            if (inspectorName.isNotEmpty()) {
                PreviewTextField(label = "שם המומחה", value = inspectorName)
            }
            
            if (experienceTitle.isNotEmpty()) {
                PreviewTextField(label = "כותרת נסיון", value = experienceTitle)
            }
            
            if (experienceText.isNotEmpty()) {
                PreviewTextField(label = "תיאור נסיון", value = experienceText)
            }
            
            certificateImagePath?.let { path ->
                if (File(path).exists()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "תמונת תעודה",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCustomSection(
    title: String,
    items: List<String>
) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Divider()
            
            items.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PreviewFindingsSection(
    job: com.ashaf.instanz.data.models.Job,
    images: List<JobImage>
) {
    val gson = Gson()
    val jobDataJson = try {
        if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
            gson.fromJson(job.dataJson, JsonObject::class.java)
        } else {
            return
        }
    } catch (e: Exception) {
        return
    }
    
    // Check if there are findings
    if (!jobDataJson.has("findings")) {
        return
    }
    
    val findingsArray = jobDataJson.getAsJsonArray("findings")
    if (findingsArray.size() == 0) {
        return
    }
    
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ממצאים",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                
                Divider()
                
                // Iterate through findings
                findingsArray.forEachIndexed { index, findingElement ->
                    val findingId = findingElement.asString
                    val findingObj = jobDataJson.getAsJsonObject(findingId) ?: return@forEachIndexed
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Finding header
                            Text(
                                text = "ממצא #${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                            
                            // Subject
                            val subject = findingObj.get("finding_subject")?.asString ?: ""
                            if (subject.isNotBlank()) {
                                PreviewTextField(label = "נושא", value = subject)
                            }
                            
                            // Category
                            val category = findingObj.get("finding_category")?.asString ?: ""
                            if (category.isNotBlank()) {
                                PreviewTextField(label = "תת נושא", value = category)
                            }
                            
                            // Description
                            val description = findingObj.get("finding_description")?.asString ?: ""
                            if (description.isNotBlank()) {
                                PreviewTextField(label = "תיאור הבעיה", value = description)
                            }
                            
                            // Note
                            val note = findingObj.get("finding_note")?.asString ?: ""
                            if (note.isNotBlank()) {
                                PreviewTextField(label = "הערה", value = note)
                            }
                            
                            // Images for this finding
                            val findingImages = images.filter { it.sectionId == findingId }
                            if (findingImages.isNotEmpty()) {
                                Text(
                                    text = "תמונות:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    findingImages.take(3).forEach { image ->
                                        AsyncImage(
                                            model = File(image.filePath),
                                            contentDescription = "תמונה",
                                            modifier = Modifier
                                                .size(80.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (findingImages.size > 3) {
                                        Text(
                                            text = "+${findingImages.size - 3}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }
                            }
                            
                            // Recommendations for this finding
                            val recommendationsJson = findingObj.get("recommendations")?.asString ?: ""
                            if (recommendationsJson.isNotBlank()) {
                                val recommendations = try {
                                    gson.fromJson(recommendationsJson, com.google.gson.JsonArray::class.java)
                                } catch (e: Exception) {
                                    null
                                }
                                
                                if (recommendations != null && recommendations.size() > 0) {
                                    Divider()
                                    Text(
                                        text = "המלצות:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                    
                                    recommendations.forEachIndexed { recIndex, recElement ->
                                        val recObj = recElement.asJsonObject
                                        val recDescription = recObj.get("description")?.asString ?: ""
                                        val recQuantity = recObj.get("quantity")?.asString ?: ""
                                        val recUnit = recObj.get("unit")?.asString ?: ""
                                        val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                                        val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                                        
                                        if (recDescription.isNotBlank()) {
                                            Text(
                                                text = "${recIndex + 1}. $recDescription${if (recQuantity.isNotBlank()) " ($recQuantity $recUnit)" else ""}${if (recTotalPrice.isNotBlank()) " - ₪$recTotalPrice" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (index < findingsArray.size() - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

@Composable
private fun PreviewRecommendationsSummary(
    job: com.ashaf.instanz.data.models.Job
) {
    val gson = Gson()
    val jobDataJson = try {
        if (job.dataJson.isNotBlank() && job.dataJson != "{}") {
            gson.fromJson(job.dataJson, JsonObject::class.java)
        } else {
            return
        }
    } catch (e: Exception) {
        return
    }
    
    // Check if there are findings
    if (!jobDataJson.has("findings")) {
        return
    }
    
    val findingsArray = jobDataJson.getAsJsonArray("findings")
    if (findingsArray.size() == 0) {
        return
    }
    
    // Collect all recommendations from all findings
    val allRecommendations = mutableListOf<Triple<Int, String, JsonObject>>() // <findingNumber, findingSubject, recObj>
        
        findingsArray.forEachIndexed { index, findingElement ->
            val findingId = findingElement.asString
            val findingObj = jobDataJson.getAsJsonObject(findingId) ?: return@forEachIndexed
            val findingSubject = findingObj.get("finding_subject")?.asString ?: "ממצא ${index + 1}"
            
            val recommendationsJson = findingObj.get("recommendations")?.asString ?: ""
            if (recommendationsJson.isNotBlank()) {
                try {
                    val recommendations = gson.fromJson(recommendationsJson, com.google.gson.JsonArray::class.java)
                    recommendations?.forEach { recElement ->
                        val recObj = recElement.asJsonObject
                        allRecommendations.add(Triple(index + 1, findingSubject, recObj))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // If no recommendations, return
        if (allRecommendations.isEmpty()) {
            return
        }
        
        // Calculate totals
        var subtotal = 0.0
        allRecommendations.forEach { (_, _, recObj) ->
            val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
            val totalPrice = recTotalPrice.toDoubleOrNull() ?: 0.0
            subtotal += totalPrice
        }
        
        val jobSettings = job.getJobSettings()
        val vatPercent = jobSettings.vatPercent
        val vatRate = vatPercent.toDouble() / 100.0
        val vat = subtotal * vatRate
        val total = subtotal + vat
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "סיכום המלצות לתיקון וריכוז מחירים",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                
                Divider(color = Color(0xFFE65100))
                
                // Recommendations table
                allRecommendations.forEachIndexed { index, (findingNum, findingSubject, recObj) ->
                    val recDescription = recObj.get("description")?.asString ?: ""
                    val recQuantity = recObj.get("quantity")?.asString ?: ""
                    val recUnit = recObj.get("unit")?.asString ?: ""
                    val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                    val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. $recDescription",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "ממצא: $findingSubject",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$recQuantity $recUnit × ₪$recPricePerUnit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "₪$recTotalPrice",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            }
                        }
                    }
                }
                
                Divider(color = Color(0xFFE65100))
                
                // Price summary
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "סה\"כ לפני מע\"מ:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = String.format("₪%.2f", subtotal),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "מע\"מ ($vatPercent%):",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = String.format("₪%.2f", vat),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                    
                    Divider(thickness = 2.dp, color = Color(0xFFE65100))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "סה\"כ כולל מע\"מ:",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = String.format("₪%.2f", total),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
