package com.ashaf.instanz.ui.screens.template

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: String = "water_damage_v1",
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: TemplateEditorViewModel = viewModel(
        factory = TemplateEditorViewModelFactory(
            templateRepository = appContainer.templateRepository,
            jobRepository = appContainer.jobRepository,
            templateId = templateId,
            jobId = null // Always null for master template editing
        )
    )
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var currentImageType by remember { mutableStateOf<String?>(null) } // "logo", "contact", "certificate"
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Copy image to app's internal storage
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageDir = File(context.filesDir, "template_images")
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }
                
                val imageFileName = "template_${templateId}_${currentImageType}_${System.currentTimeMillis()}.jpg"
                val imageFile = File(imageDir, imageFileName)
                
                inputStream?.use { input ->
                    FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Update view model
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
    
    // All state flows
    val templateName by viewModel.templateName.collectAsState()
    val logoImagePath by viewModel.logoImagePath.collectAsState()
    val contactImagePath by viewModel.contactImagePath.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val email by viewModel.email.collectAsState()
    val businessNumber by viewModel.businessNumber.collectAsState()
    val website by viewModel.website.collectAsState()
    val visitReason by viewModel.visitReason.collectAsState()
    val company by viewModel.company.collectAsState()
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
                        "עריכת תבנית אם",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.saveTemplate()
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
                            Icon(Icons.Default.Check, "שמור וצא", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F0F0))
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Primary,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    selectedContentColor = Primary,
                    unselectedContentColor = TextSecondary,
                    text = {
                        Text(
                            text = "תבנית דוח",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                )
                
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    selectedContentColor = Primary,
                    unselectedContentColor = TextSecondary,
                    text = {
                        Text(
                            text = "סעיפי דוח",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                )
            }
            
            // Tab Content
            when (selectedTabIndex) {
                0 -> TemplateReportTab(
                    templateName = templateName,
                    logoImagePath = logoImagePath,
                    contactImagePath = contactImagePath,
                    phone = phone,
                    email = email,
                    businessNumber = businessNumber,
                    website = website,
                    visitReason = visitReason,
                    company = company,
                    inspectorName = inspectorName,
                    experienceTitle = experienceTitle,
                    experienceText = experienceText,
                    certificateImagePath = certificateImagePath,
                    disclaimerText = disclaimerText,
                    sections = sections,
                    sectionTitles = sectionTitles,
                    viewModel = viewModel,
                    onLogoImageClick = {
                        currentImageType = "logo"
                        showImageSourceDialog = true
                    },
                    onContactImageClick = {
                        currentImageType = "contact"
                        showImageSourceDialog = true
                    },
                    onCertificateImageClick = {
                        currentImageType = "certificate"
                        showImageSourceDialog = true
                    },
                    onAddClick = { sectionId ->
                        currentCategory = sectionId
                        showAddDialog = true
                    }
                )
                
                1 -> ReportSectionsTab()
            }
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
}

@Composable
fun TemplateReportTab(
    templateName: String,
    logoImagePath: String?,
    contactImagePath: String?,
    phone: String,
    email: String,
    businessNumber: String,
    website: String,
    visitReason: String,
    company: String,
    inspectorName: String,
    experienceTitle: String,
    experienceText: String,
    certificateImagePath: String?,
    disclaimerText: String,
    sections: Map<String, MutableList<TemplateSectionItem>>,
    sectionTitles: Map<String, String>,
    viewModel: TemplateEditorViewModel,
    onLogoImageClick: () -> Unit,
    onContactImageClick: () -> Unit,
    onCertificateImageClick: () -> Unit,
    onAddClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF0F0F0)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Template name section at the top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "שם התבנית",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { viewModel.updateTemplateName(it) },
                    label = { Text("שם") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("לדוגמה: דוח נזקי מים") }
                )
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
            onLogoImageSelect = onLogoImageClick,
            onContactImageSelect = onContactImageClick,
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
        
        // Intro report section with default content
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
            onCertificateImageSelect = onCertificateImageClick,
            onAddClick = { onAddClick("intro_report") },
            onDeleteItem = { index -> viewModel.deleteSectionItem("intro_report", index) },
            onUpdateItem = { index, text -> viewModel.updateSectionItem("intro_report", index, text) }
        )
        
        // Other intro sections
        listOf("conclusion", "intro_work", "intro_activities", "intro_recommendations").forEach { sectionId ->
            TemplateSection(
                sectionId = sectionId,
                title = sectionTitles[sectionId] ?: "",
                items = sections[sectionId] ?: emptyList(),
                onAddClick = { onAddClick(sectionId) },
                onDeleteItem = { index -> viewModel.deleteSectionItem(sectionId, index) },
                onUpdateItem = { index, text -> viewModel.updateSectionItem(sectionId, index, text) }
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
        
        // Disclaimer section - editable text
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
                onAddClick = { onAddClick(sectionId) },
                onDeleteItem = { index -> viewModel.deleteSectionItem(sectionId, index) },
                onUpdateItem = { index, text -> viewModel.updateSectionItem(sectionId, index, text) }
            )
        }
    }
}

@Composable
fun ReportSectionsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "סעיפי דוח",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        
        Text(
            "כאן תוכל להוסיף ולערוך סעיפים לדוח באמצעות הכפתורים הבאים:",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "הוספה",
                icon = Icons.Default.Add,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            
            ActionButton(
                text = "צ'ק ליסט",
                icon = Icons.Default.CheckCircle,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "טבלה אנכית",
                icon = Icons.Default.List,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            
            ActionButton(
                text = "טבלה אופקית",
                icon = Icons.Default.List,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text(
            "שדות בסיסיים:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        
        // Basic fields list
        val basicFields = listOf(
            "נושא",
            "תת נושא",
            "תיאור",
            "הערה",
            "תאריך"
        )
        
        basicFields.forEach { field ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        field,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ערוך",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun HeaderImagesSection(
    logoImagePath: String?,
    contactImagePath: String?,
    phone: String,
    email: String,
    businessNumber: String,
    website: String,
    onLogoImageSelect: () -> Unit,
    onContactImageSelect: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onBusinessNumberChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onLogoImageDelete: (() -> Unit)? = null,
    onContactImageDelete: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "כותרות הדוח",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        
        // Logo and contact images row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo image (left)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .clickable { onLogoImageSelect() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (logoImagePath != null && File(logoImagePath).exists()) {
                    // Show the selected logo
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = File(logoImagePath),
                            contentDescription = "לוגו עליון",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        // Delete button
                        onLogoImageDelete?.let { deleteCallback ->
                            IconButton(
                                onClick = deleteCallback,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "מחק",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Overlay to indicate it's clickable
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            Text(
                                "לחץ לשינוי",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Show placeholder
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "לוגו עליון",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "כותרת עליונה",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00BCD4),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Contact image (right)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .clickable { onContactImageSelect() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (contactImagePath != null && File(contactImagePath).exists()) {
                    // Show the selected contact image (Footer image)
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = File(contactImagePath),
                            contentDescription = "כותרת תחתונה",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        // Delete button
                        onContactImageDelete?.let { deleteCallback ->
                            IconButton(
                                onClick = deleteCallback,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "מחק",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Overlay to indicate it's clickable
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            Text(
                                "לחץ לשינוי",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Show placeholder with contact info
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "כותרת תחתונה",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00BCD4),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "טלפון: $phone",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                        Text(
                            "אימייל: $email",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                        Text(
                            "ה.פ: $businessNumber",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                        Text(
                            "אתר אינטרנט: $website",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        
        // Contact fields
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("טלפון") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("אימייל") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = businessNumber,
            onValueChange = onBusinessNumberChange,
            label = { Text("ה.פ") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text("אתר אינטרנט") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
    
    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
}

@Composable
fun TopFieldsSection(
    visitReason: String,
    company: String,
    onVisitReasonChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = visitReason,
            onValueChange = onVisitReasonChange,
            label = { Text("סיבת הביקור") },
            placeholder = { Text("איתור נקלי מים") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = company,
            onValueChange = onCompanyChange,
            label = { Text("חברה") },
            placeholder = { Text("איתור - אשף האיינסטלציה") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
    
    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
}

@Composable
fun TemplateSection(
    sectionId: String,
    title: String,
    items: List<TemplateSectionItem>,
    hasDefaultContent: Boolean = false,
    isExpandedByDefault: Boolean = false,
    inspectorName: String = "",
    experienceTitle: String = "",
    experienceText: String = "",
    certificateImagePath: String? = null,
    onInspectorNameChange: (String) -> Unit = {},
    onExperienceTitleChange: (String) -> Unit = {},
    onExperienceTextChange: (String) -> Unit = {},
    onCertificateImageSelect: () -> Unit = {},
    onAddClick: () -> Unit,
    onDeleteItem: (Int) -> Unit,
    onUpdateItem: (Int, String) -> Unit = { _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Header with title and add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add button on the left
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier
                    .widthIn(min = 90.dp)
                    .height(45.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "הוספה",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Title on the right
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                if (items.isNotEmpty() || hasDefaultContent) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
        
        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
        
        // Expanded content
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show default content for intro_report section
                if (hasDefaultContent && sectionId == "intro_report") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Inspector name - Editable
                            OutlinedTextField(
                                value = inspectorName,
                                onValueChange = onInspectorNameChange,
                                label = { Text("שם המומחה") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            // Experience title - Editable
                            OutlinedTextField(
                                value = experienceTitle,
                                onValueChange = onExperienceTitleChange,
                                label = { Text("כותרת נסיון") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            // Experience text - Editable
                            OutlinedTextField(
                                value = experienceText,
                                onValueChange = onExperienceTextChange,
                                label = { Text("אודות החברה") },
                                placeholder = { Text("תאר את החברה, ניסיון, מומחיות...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp),
                                minLines = 6,
                                maxLines = 15,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                )
                            )
                            
                            // Certificate image
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { onCertificateImageSelect() },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (certificateImagePath != null) {
                                    // Display certificate image
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = File(certificateImagePath),
                                            contentDescription = "תמונת תעודה",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // X button to remove image
                                        IconButton(
                                            onClick = { 
                                                onCertificateImageSelect() // Allow reselecting
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "ערוך תמונה",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = "הוסף תמונת תעודה",
                                            modifier = Modifier.size(56.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "לחץ להוספת תמונת תעודה",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Show added items (editable)
                items.forEachIndexed { index, item ->
                    var editableText by remember(item.text) { mutableStateOf(item.text) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "פריט ${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                IconButton(
                                    onClick = { onDeleteItem(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "מחק",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { newValue ->
                                    editableText = newValue
                                    onUpdateItem(index, newValue)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 10,
                                placeholder = { Text("הקלד טקסט...") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTemplateItemDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "הוספת טקסט ל$categoryName",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("טקסט") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 5,
                maxLines = 10
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                enabled = text.isNotBlank()
            ) {
                Text("הוסף")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        }
    )
}

@Composable
fun DisclaimerSection(
    disclaimerText: String,
    onDisclaimerTextChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "כווץ" else "הרחב"
                    )
                }
                Text(
                    "סיכום - דו\"ח (תצהיר)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57F17)
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "ערוך את תוכן התצהיר שיופיע בכל הדוחות:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = disclaimerText,
                    onValueChange = onDisclaimerTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    minLines = 10,
                    maxLines = 20,
                    label = { Text("תוכן התצהיר") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF57F17),
                        focusedLabelColor = Color(0xFFF57F17)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                // Auto-save indicator
                Text(
                    "✅ השינויים נשמרים אוטומטית",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Text(
                    "💡 טיפ: תצהיר זה יופיע בכל הדוחות שנוצרים. ניתן לערוך אותו לכל עבודה ספציפית דרך תפריט 'עריכת מלל בדוח'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57F17),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
