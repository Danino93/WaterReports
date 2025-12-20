package com.ashaf.instanz.ui.screens.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.data.models.TemplateSectionItem
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.TemplateEditorViewModel
import com.ashaf.instanz.ui.viewmodel.TemplateEditorViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: String = "water_damage_v1",
    jobId: Long? = null,
    onBackClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: TemplateEditorViewModel = viewModel(
        factory = TemplateEditorViewModelFactory(
            templateRepository = appContainer.templateRepository,
            jobRepository = appContainer.jobRepository,
            templateId = templateId,
            jobId = jobId
        )
    )
    
    var showAddDialog by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf("") }
    
    val inspectorName by viewModel.inspectorName.collectAsState()
    val experienceTitle by viewModel.experienceTitle.collectAsState()
    val experienceText by viewModel.experienceText.collectAsState()
    val certificateImagePath by viewModel.certificateImagePath.collectAsState()
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
                        "מכלל דו\"ח",
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
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
                onCertificateImageSelect = { /* TODO: Image picker */ },
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
                                label = { Text("תיאור נסיון") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                minLines = 4,
                                maxLines = 8
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
                                    // TODO: Display image
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            "תמונת תעודה",
                                            modifier = Modifier.align(Alignment.Center)
                                        )
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

