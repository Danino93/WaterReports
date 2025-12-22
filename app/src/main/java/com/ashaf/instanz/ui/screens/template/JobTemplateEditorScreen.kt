package com.ashaf.instanz.ui.screens.template

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ashaf.instanz.data.models.TemplateSectionItem
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.TemplateEditorViewModel
import com.ashaf.instanz.ui.viewmodel.TemplateEditorViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Template editor for job-specific content - looks identical to "תבנית דוח" tab
 * Accessed from "עריכת מכלל בדוח" menu in EditorScreen
 * Loads from master template by default, allows override for this specific job
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobTemplateEditorScreen(
    jobId: Long,
    templateId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: TemplateEditorViewModel = viewModel(
        factory = TemplateEditorViewModelFactory(
            templateRepository = appContainer.templateRepository,
            jobRepository = appContainer.jobRepository,
            templateId = templateId,
            jobId = jobId // This makes it job-specific
        )
    )
    
    var showAddDialog by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var currentImageType by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageDir = File(context.filesDir, "template_images")
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }
                
                val imageFileName = "job_${jobId}_${currentImageType}_${System.currentTimeMillis()}.jpg"
                val imageFile = File(imageDir, imageFileName)
                
                inputStream?.use { input ->
                    FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                when (currentImageType) {
                    "logo" -> viewModel.updateLogoImage(imageFile.absolutePath)
                    "contact" -> viewModel.updateContactImage(imageFile.absolutePath)
                    "certificate" -> viewModel.updateCertificateImage(imageFile.absolutePath)
                }
                
                // Save to database immediately
                viewModel.saveTemplate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Header images and contact
    val logoImagePath by viewModel.logoImagePath.collectAsState()
    val contactImagePath by viewModel.contactImagePath.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val email by viewModel.email.collectAsState()
    val businessNumber by viewModel.businessNumber.collectAsState()
    val website by viewModel.website.collectAsState()
    
    // Top fields
    val visitReason by viewModel.visitReason.collectAsState()
    val company by viewModel.company.collectAsState()
    
    // Inspector details
    val inspectorName by viewModel.inspectorName.collectAsState()
    val experienceTitle by viewModel.experienceTitle.collectAsState()
    val experienceText by viewModel.experienceText.collectAsState()
    val certificateImagePath by viewModel.certificateImagePath.collectAsState()
    val disclaimerText by viewModel.disclaimerText.collectAsState()
    
    val sections by viewModel.sections.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val sectionTitles = mapOf(
        "intro_report" to "הקדמה - דו\"ח",
        "conclusion" to "בסיום",
        "intro_work" to "הקדמה - עבודה",
        "intro_activities" to "פעילות הקדמה",
        "intro_recommendations" to "המלצת הקדמה",
        "summary_recommendations" to "המלצת סיכום",
        "summary_activities" to "פעילות סיכום",
        "work_summary" to "סיכום - עבודה",
        "report_summary" to "סיכום - דו\"ח"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "עריכת תבנית לעבודה זו",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowForward, "חזרה", tint = Color.White)
                    }
                },
                actions = {
                    // כפתור איפוס לברירת מחדל
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Refresh, "אפס לברירת מחדל", tint = Color.White)
                    }
                    
                    // כפתור שמירה
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.saveTemplate()
                                showSaveSuccess = true
                                kotlinx.coroutines.delay(1500)
                                showSaveSuccess = false
                                kotlinx.coroutines.delay(300)
                                onBackClick()
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, "שמור", tint = Color.White)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F0F0))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Info card at the top - improved design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "עריכה לעבודה זו בלבד",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            "שינויים שתבצע כאן ישמרו רק לעבודה זו.\nהתבנית האם לא תשתנה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            // Header images section
            HeaderImagesSection(
                logoImagePath = logoImagePath,
                contactImagePath = contactImagePath,
                phone = phone,
                email = email,
                businessNumber = businessNumber,
                website = website,
                onLogoImageSelect = {
                    currentImageType = "logo"
                    showImageSourceDialog = true
                },
                onContactImageSelect = {
                    currentImageType = "contact"
                    showImageSourceDialog = true
                },
                onPhoneChange = { viewModel.updatePhone(it) },
                onEmailChange = { viewModel.updateEmail(it) },
                onBusinessNumberChange = { viewModel.updateBusinessNumber(it) },
                onWebsiteChange = { viewModel.updateWebsite(it) },
                onLogoImageDelete = {
                    viewModel.updateLogoImage(null)
                    viewModel.saveTemplate()
                },
                onContactImageDelete = {
                    viewModel.updateContactImage(null)
                    viewModel.saveTemplate()
                }
            )
            
            // Top fields section
            TopFieldsSection(
                visitReason = visitReason,
                company = company,
                onVisitReasonChange = { viewModel.updateVisitReason(it) },
                onCompanyChange = { viewModel.updateCompany(it) }
            )
            
            // כללי ומבוא header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB2DFDB))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "כללי ומבוא",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            
            // First section - הקדמה דו"ח (with example content, expanded by default)
            TemplateSection(
                sectionId = "intro_report",
                title = sectionTitles["intro_report"] ?: "",
                items = sections["intro_report"] ?: emptyList(),
                hasDefaultContent = true,
                isExpandedByDefault = true,
                inspectorName = inspectorName,
                experienceTitle = experienceTitle,
                experienceText = experienceText,
                certificateImagePath = certificateImagePath,
                onInspectorNameChange = { viewModel.updateInspectorName(it) },
                onExperienceTitleChange = { viewModel.updateExperienceTitle(it) },
                onExperienceTextChange = { viewModel.updateExperienceText(it) },
                onCertificateImageSelect = {
                    currentImageType = "certificate"
                    showImageSourceDialog = true
                },
                onAddClick = {
                    currentCategory = "intro_report"
                    showAddDialog = true
                },
                onDeleteItem = { index ->
                    viewModel.deleteSectionItem("intro_report", index)
                },
                onUpdateItem = { index, text ->
                    viewModel.updateSectionItem("intro_report", index, text)
                }
            )
            
            // Other sections
            listOf("conclusion", "intro_work", "intro_activities", "intro_recommendations").forEach { sectionId ->
                TemplateSection(
                    sectionId = sectionId,
                    title = sectionTitles[sectionId] ?: "",
                    items = sections[sectionId] ?: emptyList(),
                    onAddClick = {
                        currentCategory = sectionId
                        showAddDialog = true
                    },
                    onDeleteItem = { index ->
                        viewModel.deleteSectionItem(sectionId, index)
                    },
                    onUpdateItem = { index, text ->
                        viewModel.updateSectionItem(sectionId, index, text)
                    }
                )
            }
            
            // סיכום header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB2DFDB))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "סיכום",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            
            // Disclaimer section - editable for this specific job
            DisclaimerSection(
                disclaimerText = disclaimerText,
                onDisclaimerTextChange = { viewModel.updateDisclaimerText(it) }
            )
            
            // Summary sections
            listOf("summary_recommendations", "summary_activities", "work_summary", "report_summary").forEach { sectionId ->
                TemplateSection(
                    sectionId = sectionId,
                    title = sectionTitles[sectionId] ?: "",
                    items = sections[sectionId] ?: emptyList(),
                    onAddClick = {
                        currentCategory = sectionId
                        showAddDialog = true
                    },
                    onDeleteItem = { index ->
                        viewModel.deleteSectionItem(sectionId, index)
                    },
                    onUpdateItem = { index, text ->
                        viewModel.updateSectionItem(sectionId, index, text)
                    }
                )
            }
        }
        
        // Image source dialog (Camera or Gallery)
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = {
                    Text(
                        "בחר מקור תמונה",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                // TODO: Open camera
                                showImageSourceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("צלם תמונה")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showImageSourceDialog = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("בחר מהגלריה")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("ביטול")
                    }
                }
            )
        }
        
        // Add dialog
        if (showAddDialog) {
            AddTemplateItemDialog(
                categoryName = sectionTitles[currentCategory] ?: "",
                onDismiss = { showAddDialog = false },
                onAdd = { text ->
                    viewModel.addSectionItem(currentCategory, text)
                    showAddDialog = false
                }
            )
        }
        
        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "אפס לברירת מחדל?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "פעולה זו תמחק את כל השינויים שעשית ותחזיר את התבנית האם.\n\nהאם להמשיך?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            scope.launch {
                                viewModel.resetToDefaults()
                                // Show success message
                                android.widget.Toast.makeText(
                                    context,
                                    "✅ אופס לברירת מחדל בהצלחה!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("אפס")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("ביטול")
                    }
                }
            )
        }
        
        // Save success indicator (floating card)
        if (showSaveSuccess) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 80.dp)
                        .widthIn(max = 300.dp),
                    colors = CardDefaults.cardColors(containerColor = Secondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "נשמר בהצלחה!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

