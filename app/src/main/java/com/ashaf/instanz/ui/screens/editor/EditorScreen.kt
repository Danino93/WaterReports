package com.ashaf.instanz.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ashaf.instanz.data.models.JobImage
import com.ashaf.instanz.data.models.JobStatus
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.EditorViewModel
import com.ashaf.instanz.ui.viewmodel.EditorViewModelFactory
import com.ashaf.instanz.ui.viewmodel.ImageViewModel
import com.ashaf.instanz.ui.viewmodel.ImageViewModelFactory
import com.ashaf.instanz.utils.QuoteCalculator
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    jobId: Long,
    onBackClick: () -> Unit,
    onPreviewClick: (Long) -> Unit,
    onCameraClick: (Long, String, String) -> Unit,
    onTemplateEditClick: (String) -> Unit,
    onJobSettingsClick: (Long) -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: EditorViewModel = viewModel(
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
    
    val job by viewModel.job.collectAsState()
    val template by viewModel.template.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val findings by viewModel.findings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    // Reload job data when returning to screen
    LaunchedEffect(jobId) {
        viewModel.loadJobAndTemplate()
    }
    
    // Auto-save state
    var showSaveMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Selected tab
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    
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
    
    // Define tabs structure
    val tabTitles = listOf("מידע כללי", "מזמין הבדיקה", "ממצאים")
    
    // Get sections for first two tabs
    val tabSections = remember(sectionsJson) {
        if (sectionsJson.isEmpty()) {
            listOf(emptyList(), emptyList())
        } else if (sectionsJson.size >= 2) {
            listOf(sectionsJson.take(1), listOf(sectionsJson[1]))
        } else {
            listOf(sectionsJson, emptyList())
        }
    }
    
    // Manual save function (only when clicking Save button or switching tabs)
    fun manualSave() {
        scope.launch {
            viewModel.saveChanges()
            showSaveMessage = true
            delay(2000)
            showSaveMessage = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "עבודה",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowForward, "חזרה")
                    }
                },
                actions = {
                    // Save button
                    IconButton(onClick = { manualSave() }) {
                        Icon(Icons.Default.Save, "שמירה", tint = Color.White)
                    }
                    
                    // Share button
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Default.Share, "שיתוף", tint = Color.White)
                    }
                    
                    // PDF/Preview button
                    IconButton(onClick = { 
                        // Force save before preview to ensure consistency
                        android.util.Log.d("EditorScreen", "💾 Forcing save before preview...")
                        scope.launch {
                            viewModel.saveChanges()
                            kotlinx.coroutines.delay(100) // Wait for DB write
                            android.util.Log.d("EditorScreen", "✅ Save complete, opening preview")
                            jobId.let(onPreviewClick)
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, "דוח", tint = Color.White)
                    }
                    
                    // Menu
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, "עוד", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, "PDF", tint = Primary)
                                        Text("דוחות מיוחדים")
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                    // Force save before preview to ensure consistency
                                    android.util.Log.d("EditorScreen", "💾 Forcing save before preview (from menu)...")
                                    scope.launch {
                                        viewModel.saveChanges()
                                        kotlinx.coroutines.delay(100) // Wait for DB write
                                        android.util.Log.d("EditorScreen", "✅ Save complete, opening preview")
                                    jobId.let(onPreviewClick)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.TableChart, "Excel", tint = Accent)
                                        Text("אקסל")
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                    scope.launch {
                                        showSaveMessage = true
                                        delay(2000)
                                        showSaveMessage = false
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, "יצירת החתמה", tint = TextSecondary)
                                        Text("יצירת החתמה")
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("הגדרות עבודה נוספות") },
                                onClick = { 
                                    showMenu = false
                                    onJobSettingsClick(jobId)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "שכפול", tint = TextSecondary)
                                        Text("שכפול עבודה")
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                    showDuplicateDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, "ייבוא", tint = TextSecondary)
                                        Text("ייבוא")
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("עריכת מכלל בדו\"ח") },
                                onClick = { 
                                    showMenu = false
                                    template?.let { onTemplateEditClick(it.id) }
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Delete, "מחיקה", tint = MaterialTheme.colorScheme.error)
                                        Text("מחיקת עבודה", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("העברת עבודה לארכיון") },
                                onClick = { 
                                    showMenu = false
                                    showArchiveDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                            )
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Job Info Card
                    JobInfoCard(
                        job = job,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    
                    // Tabs - ALWAYS SHOW
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
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                selectedContentColor = Primary,
                                unselectedContentColor = TextSecondary,
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            )
                        }
                    }
                    
                    // Sections Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedTabIndex) {
                            0, 1 -> {
                                // First two tabs: show sections from JSON
                                val currentSections = if (selectedTabIndex < tabSections.size) {
                                    tabSections[selectedTabIndex]
                                } else {
                                    emptyList()
                                }
                                
                                currentSections.forEach { sectionJson ->
                                    SectionCardFromJson(
                                        sectionJson = sectionJson,
                                        viewModel = viewModel,
                                        imageViewModel = imageViewModel,
                                        onCameraClick = onCameraClick,
                                        jobId = jobId
                                    )
                                }
                            }
                            2 -> {
                                // Third tab: Hierarchical findings with categories
                                
                                // Check if migration is needed
                                if (categories.isEmpty() && findings.isNotEmpty()) {
                                    // Show migration button
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF57C00)
                                                )
                                                Text(
                                                    text = "מבנה ממצאים חדש זמין!",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "יש לך ${findings.size} ממצאים במבנה הישן. לחץ כדי לשדרג למבנה היררכי חדש עם קטגוריות.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                Button(
                                                onClick = { viewModel.migrateToHierarchicalStructure() },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                                            ) {
                                                Icon(Icons.Default.Upgrade, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("שדרג למבנה חדש")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Add Category button
                                Button(
                                    onClick = { viewModel.addCategory() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("הוסף קטגוריה", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Display hierarchical categories
                                if (categories.isNotEmpty()) {
                                    categories.forEach { category ->
                                        CategoryCard(
                                            category = category,
                                        viewModel = viewModel,
                                        imageViewModel = imageViewModel,
                                        jobId = jobId,
                                            onCameraClick = onCameraClick
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                } else if (findings.isEmpty()) {
                                    // Empty state
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Assessment,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(64.dp)
                                            )
                                    Text(
                                                text = "אין כרגע קטגוריות",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                            Text(
                                                text = "לחץ על 'הוסף קטגוריה' כדי להתחיל להוסיף ממצאים מסודרים בקטגוריות",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                                textAlign = TextAlign.Center
                                    )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Save message
            if (showSaveMessage) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Accent
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("השינויים נשמרו בהצלחה", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("מחיקת עבודה", fontWeight = FontWeight.Bold) },
            text = { Text("האם אתה בטוח שברצונך למחוק את העבודה? פעולה זו לא ניתנת לשחזור.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            job?.let { viewModel.deleteJob(it.id) }
                            showDeleteDialog = false
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
    
    // Duplicate Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("שכפול עבודה", fontWeight = FontWeight.Bold) },
            text = { Text("האם ברצונך לשכפל את העבודה הנוכחית? תיווצר עבודה חדשה עם כל הנתונים.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            job?.let { viewModel.duplicateJob(it.id) }
                            showDuplicateDialog = false
                            showSaveMessage = true
                            delay(2000)
                            showSaveMessage = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("שכפל")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
    
    // Archive Dialog
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("העברה לארכיון", fontWeight = FontWeight.Bold) },
            text = { Text("האם ברצונך להעביר את העבודה לארכיון? העבודה תשמר אך לא תוצג ברשימת העבודות הפעילות.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            job?.let { viewModel.archiveJob(it.id) }
                            showArchiveDialog = false
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Text("העבר לארכיון")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun JobInfoCard(
    job: com.ashaf.instanz.data.models.Job?,
    modifier: Modifier = Modifier
) {
    if (job == null) return
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and time on the right
            Column(horizontalAlignment = Alignment.End) {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("he", "IL"))
                val dayFormat = SimpleDateFormat("EEEE", Locale("he", "IL"))
                
                Text(
                    text = "תאריך: ${dayFormat.format(Date(job.dateModified))}, ${dateFormat.format(Date(job.dateModified))}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            // Job number on the left
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "מספר עבודה:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "#${job.id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun SectionCardFromJson(
    sectionJson: JsonObject,
    viewModel: EditorViewModel,
    imageViewModel: ImageViewModel,
    onCameraClick: (Long, String, String) -> Unit,
    jobId: Long,
    onValueChanged: () -> Unit = {}
) {
    val sectionId = sectionJson.get("id")?.asString ?: ""
    val sectionTitle = sectionJson.get("title")?.asString ?: ""
    val fieldsJson = sectionJson.getAsJsonArray("fields")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
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
            
            // Special handling for invoice_items section - show dynamic items table
            if (sectionId == "invoice_items") {
                InvoiceItemsTable(
                    sectionId = sectionId,
                    viewModel = viewModel,
                    onValueChanged = onValueChanged
                )
            } else {
                // Track if we've shown the address header
                var addressHeaderShown = false
                
                fieldsJson?.forEachIndexed { index, fieldJsonElement ->
                val fieldObj = fieldJsonElement.asJsonObject
                val fieldType = fieldObj.get("type")?.asString ?: ""
                val fieldId = fieldObj.get("id")?.asString ?: ""
                val fieldLabel = fieldObj.get("label")?.asString ?: ""
                val required = fieldObj.get("required")?.asBoolean ?: false
                
                // Show "כתובת" header before address fields in client section
                if (!addressHeaderShown && sectionId == "client_details" && fieldId == "client_city") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "כתובת",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Accent, // Green color
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    addressHeaderShown = true
                }
                
                when (fieldType) {
                    "text" -> {
                        TextFieldEditorFromJson(
                            fieldId = fieldId,
                            fieldLabel = fieldLabel,
                            required = required,
                            placeholder = fieldObj.get("placeholder")?.asString ?: "",
                            lines = fieldObj.get("lines")?.asInt ?: 1,
                            sectionId = sectionId,
                            viewModel = viewModel,
                            onValueChanged = onValueChanged
                        )
                    }
                    "textarea" -> {
                        TextAreaEditorFromJson(
                            fieldId = fieldId,
                            fieldLabel = fieldLabel,
                            required = required,
                            placeholder = fieldObj.get("placeholder")?.asString ?: "",
                            lines = fieldObj.get("lines")?.asInt ?: 5,
                            sectionId = sectionId,
                            viewModel = viewModel,
                            onValueChanged = onValueChanged
                        )
                    }
                    "image" -> {
                        ImageFieldFromJson(
                            fieldId = fieldId,
                            fieldLabel = fieldLabel,
                            required = required,
                            maxImages = fieldObj.get("maxImages")?.asInt ?: 10,
                            sectionId = sectionId,
                            imageViewModel = imageViewModel,
                            onCameraClick = { onCameraClick(jobId, sectionId, fieldId) }
                        )
                    }
                    "checkbox" -> {
                        CheckboxEditorFromJson(
                            fieldId = fieldId,
                            fieldLabel = fieldLabel,
                            required = required,
                            checked = fieldObj.get("checked")?.asBoolean ?: false,
                            sectionId = sectionId,
                            viewModel = viewModel,
                            onValueChanged = onValueChanged
                        )
                    }
                    "number" -> {
                        NumberFieldEditorFromJson(
                            fieldId = fieldId,
                            fieldLabel = fieldLabel,
                            required = required,
                            default = fieldObj.get("default")?.asDouble ?: 0.0,
                            sectionId = sectionId,
                            viewModel = viewModel,
                            onValueChanged = { newValue ->
                                // Auto-calculate totals for quote templates
                                handleQuoteCalculations(sectionId, fieldId, newValue, viewModel)
                                onValueChanged()
                            }
                        )
                    }
                    else -> {
                        Text("שדה מסוג $fieldType - יושלם בקרוב")
                    }
                }
            }
            }
        }
    }
}

// Invoice Items Table Component
@Composable
fun InvoiceItemsTable(
    sectionId: String,
    viewModel: EditorViewModel,
    onValueChanged: () -> Unit = {}
) {
    val job by viewModel.job.collectAsState()
    
    // Load existing items from dataJson
    val items = remember(job?.dataJson) {
        loadInvoiceItems(viewModel, sectionId)
    }
    
    var invoiceItems by remember { mutableStateOf(items) }
    
    // Recalculate totals when items change
    LaunchedEffect(invoiceItems) {
        calculateInvoiceTotals(invoiceItems, viewModel)
        onValueChanged()
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "פריטים/שירותים",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
            
            Button(
                onClick = {
                    invoiceItems = invoiceItems + InvoiceItem(
                        index = invoiceItems.size + 1,
                        description = "",
                        quantity = 1.0,
                        unit = "",
                        unitPrice = 0.0,
                        total = 0.0
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("הוסף פריט")
            }
        }
        
        // Items table
        if (invoiceItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Text(
                    text = "לחץ על 'הוסף פריט' כדי להתחיל",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#",
                    modifier = Modifier.width(30.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "תיאור",
                    modifier = Modifier.weight(2f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "כמות",
                    modifier = Modifier.weight(0.8f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "יחידה",
                    modifier = Modifier.weight(0.8f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "מחיר יחידה",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "סה\"כ",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(40.dp)) // Space for delete button
            }
            
            // Items rows
            invoiceItems.forEachIndexed { index, item ->
                InvoiceItemRow(
                    item = item,
                    index = index,
                    onItemChange = { updatedItem ->
                        invoiceItems = invoiceItems.toMutableList().apply {
                            this[index] = updatedItem
                        }
                        saveInvoiceItem(sectionId, index, updatedItem, viewModel)
                    },
                    onDelete = {
                        invoiceItems = invoiceItems.toMutableList().apply {
                            removeAt(index)
                        }
                        deleteInvoiceItem(sectionId, index, viewModel)
                    }
                )
            }
        }
    }
}

@Composable
fun InvoiceItemRow(
    item: InvoiceItem,
    index: Int,
    onItemChange: (InvoiceItem) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember(item.description) { mutableStateOf(item.description) }
    var quantity by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
    var unit by remember(item.unit) { mutableStateOf(item.unit) }
    var unitPrice by remember(item.unitPrice) { mutableStateOf(item.unitPrice.toString()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index
            Text(
                text = "${index + 1}",
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { newValue ->
                    description = newValue
                    val total = quantity.toDoubleOrNull()?.let { q ->
                        unitPrice.toDoubleOrNull()?.let { p -> q * p } ?: 0.0
                    } ?: 0.0
                    onItemChange(item.copy(description = newValue, total = total))
                },
                modifier = Modifier.weight(2f),
                placeholder = { Text("תיאור הפריט") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = TextDirection.ContentOrRtl
                )
            )
            
            // Quantity
            OutlinedTextField(
                value = quantity,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                        quantity = newValue
                        val q = newValue.toDoubleOrNull() ?: 0.0
                        val p = unitPrice.toDoubleOrNull() ?: 0.0
                        val total = q * p
                        onItemChange(item.copy(quantity = q, total = total))
                    }
                },
                modifier = Modifier.weight(0.8f),
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Ltr
                )
            )
            
            // Unit
            OutlinedTextField(
                value = unit,
                onValueChange = { newValue ->
                    unit = newValue
                    onItemChange(item.copy(unit = newValue))
                },
                modifier = Modifier.weight(0.8f),
                placeholder = { Text("יח'") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.ContentOrRtl
                )
            )
            
            // Unit Price
            OutlinedTextField(
                value = unitPrice,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                        unitPrice = newValue
                        val q = quantity.toDoubleOrNull() ?: 0.0
                        val p = newValue.toDoubleOrNull() ?: 0.0
                        val total = q * p
                        onItemChange(item.copy(unitPrice = p, total = total))
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Ltr
                )
            )
            
            // Total (read-only)
            Text(
                text = String.format("%.2f ₪", item.total),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "מחק",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

// Data class for invoice item
data class InvoiceItem(
    val index: Int,
    val description: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val total: Double
)

// Helper functions
private fun loadInvoiceItems(viewModel: EditorViewModel, sectionId: String): List<InvoiceItem> {
    val items = mutableListOf<InvoiceItem>()
    
    for (i in 1..50) { // Support up to 50 items
        val description = viewModel.getFieldValue(sectionId, "item_${i}_description")
        if (description.isEmpty()) continue
        
        val quantity = viewModel.getFieldValue(sectionId, "item_${i}_quantity").toDoubleOrNull() ?: 1.0
        val unit = viewModel.getFieldValue(sectionId, "item_${i}_unit")
        val unitPrice = viewModel.getFieldValue(sectionId, "item_${i}_unit_price").toDoubleOrNull() ?: 0.0
        val total = quantity * unitPrice
        
        items.add(InvoiceItem(i, description, quantity, unit, unitPrice, total))
    }
    
    return items
}

private fun saveInvoiceItem(
    sectionId: String,
    index: Int,
    item: InvoiceItem,
    viewModel: EditorViewModel
) {
    viewModel.updateFieldValue(sectionId, "item_${index + 1}_description", item.description)
    viewModel.updateFieldValue(sectionId, "item_${index + 1}_quantity", item.quantity.toString())
    viewModel.updateFieldValue(sectionId, "item_${index + 1}_unit", item.unit)
    viewModel.updateFieldValue(sectionId, "item_${index + 1}_unit_price", item.unitPrice.toString())
    viewModel.updateFieldValue(sectionId, "item_${index + 1}_total", item.total.toString())
}

private fun deleteInvoiceItem(
    sectionId: String,
    index: Int,
    viewModel: EditorViewModel
) {
    // Shift all items after deleted one
    for (i in index + 1..50) {
        val nextDescription = viewModel.getFieldValue(sectionId, "item_${i + 1}_description")
        val nextQuantity = viewModel.getFieldValue(sectionId, "item_${i + 1}_quantity")
        val nextUnit = viewModel.getFieldValue(sectionId, "item_${i + 1}_unit")
        val nextUnitPrice = viewModel.getFieldValue(sectionId, "item_${i + 1}_unit_price")
        val nextTotal = viewModel.getFieldValue(sectionId, "item_${i + 1}_total")
        
        viewModel.updateFieldValue(sectionId, "item_${i}_description", nextDescription)
        viewModel.updateFieldValue(sectionId, "item_${i}_quantity", nextQuantity)
        viewModel.updateFieldValue(sectionId, "item_${i}_unit", nextUnit)
        viewModel.updateFieldValue(sectionId, "item_${i}_unit_price", nextUnitPrice)
        viewModel.updateFieldValue(sectionId, "item_${i}_total", nextTotal)
    }
    
    // Clear last item
    viewModel.updateFieldValue(sectionId, "item_51_description", "")
    viewModel.updateFieldValue(sectionId, "item_51_quantity", "")
    viewModel.updateFieldValue(sectionId, "item_51_unit", "")
    viewModel.updateFieldValue(sectionId, "item_51_unit_price", "")
    viewModel.updateFieldValue(sectionId, "item_51_total", "")
}

private fun calculateInvoiceTotals(items: List<InvoiceItem>, viewModel: EditorViewModel) {
    val subtotal = items.sumOf { it.total }
    val vatPercent = 18.0 // Default VAT
    val vatAmount = subtotal * vatPercent / 100.0
    val totalWithVat = subtotal + vatAmount
    
    viewModel.updateFieldValue("pricing_summary", "subtotal", subtotal.toString())
    viewModel.updateFieldValue("pricing_summary", "vat_rate", vatPercent.toString())
    viewModel.updateFieldValue("pricing_summary", "vat_amount", vatAmount.toString())
    viewModel.updateFieldValue("pricing_summary", "total_with_vat", totalWithVat.toString())
}

@Composable
fun TextFieldEditorFromJson(
    fieldId: String,
    fieldLabel: String,
    required: Boolean,
    placeholder: String,
    lines: Int,
    sectionId: String,
    viewModel: EditorViewModel,
    onValueChanged: () -> Unit = {}
) {
    val job by viewModel.job.collectAsState()
    
    // Get value directly from viewModel - will update when job, section, or field changes
    val storedValue = viewModel.getFieldValue(sectionId, fieldId)
    var textValue by remember(job?.id, sectionId, fieldId) { mutableStateOf(storedValue) }
    
    // Sync with stored value when returning to screen OR switching tabs
    LaunchedEffect(job?.id, sectionId, fieldId) {
        textValue = viewModel.getFieldValue(sectionId, fieldId)
    }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            viewModel.updateFieldValue(sectionId, fieldId, newValue)
            onValueChanged()
        },
        label = { Text(fieldLabel) }, // Removed required indicator
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = lines == 1,
        minLines = if (lines > 1) lines else 1,
        maxLines = if (lines > 1) lines + 3 else 1,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.ContentOrRtl
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = if (lines == 1) ImeAction.Next else ImeAction.Default
        )
    )
}

@Composable
fun TextAreaEditorFromJson(
    fieldId: String,
    fieldLabel: String,
    required: Boolean,
    placeholder: String,
    lines: Int,
    sectionId: String,
    viewModel: EditorViewModel,
    onValueChanged: () -> Unit = {}
) {
    val job by viewModel.job.collectAsState()
    val currentValue = remember(sectionId, fieldId, job?.dataJson) {
        viewModel.getFieldValue(sectionId, fieldId)
    }
    var textValue by remember(sectionId, fieldId, job?.dataJson) { mutableStateOf(currentValue) }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            viewModel.updateFieldValue(sectionId, fieldId, newValue)
            onValueChanged()
        },
        label = { Text(fieldLabel) }, // Removed required indicator
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (lines * 24).dp),
        minLines = lines,
        maxLines = lines + 3,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.ContentOrRtl
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Default
        )
    )
}

@Composable
fun CheckboxEditorFromJson(
    fieldId: String,
    fieldLabel: String,
    required: Boolean,
    checked: Boolean,
    sectionId: String,
    viewModel: EditorViewModel,
    onValueChanged: () -> Unit = {}
) {
    val currentValue = remember(fieldId) {
        viewModel.getFieldValue(sectionId, fieldId)
    }
    var isChecked by remember(currentValue) {
        mutableStateOf(currentValue.toBooleanStrictOrNull() ?: checked)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { newChecked ->
                isChecked = newChecked
                viewModel.updateFieldValue(sectionId, fieldId, newChecked.toString())
                onValueChanged()
            }
        )
        Text(
            text = fieldLabel + if (required) " *" else "",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NumberFieldEditorFromJson(
    fieldId: String,
    fieldLabel: String,
    required: Boolean,
    default: Double,
    sectionId: String,
    viewModel: EditorViewModel,
    onValueChanged: (Double) -> Unit = {}
) {
    val job by viewModel.job.collectAsState()
    
    val storedValue = viewModel.getFieldValue(sectionId, fieldId)
    var textValue by remember(job?.id, sectionId, fieldId) {
        mutableStateOf(if (storedValue.isBlank()) default.toString() else storedValue)
    }
    
    // Sync with stored value when returning to screen OR switching tabs
    LaunchedEffect(job?.id, sectionId, fieldId) {
        val newValue = viewModel.getFieldValue(sectionId, fieldId)
        textValue = if (newValue.isBlank()) default.toString() else newValue
    }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                textValue = newValue
                viewModel.updateFieldValue(sectionId, fieldId, newValue)
                onValueChanged(newValue.toDoubleOrNull() ?: 0.0)
            }
        },
        label = { Text(fieldLabel) }, // Removed required indicator
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        suffix = { Text("ש\"ח", style = MaterialTheme.typography.bodySmall) }
    )
}

// Helper function for quote calculations
private fun handleQuoteCalculations(
    sectionId: String,
    fId: String,
    newValue: Double,
    viewModel: EditorViewModel
) {
    // Auto-calculate work section totals
    when {
        fId.endsWith("_unit_price") || fId.endsWith("_quantity") -> {
            val baseId = fId.substringBeforeLast("_")
            val quantity = viewModel.getFieldValue(sectionId, "${baseId}_quantity").toDoubleOrNull() ?: 1.0
            val unitPrice = viewModel.getFieldValue(sectionId, "${baseId}_unit_price").toDoubleOrNull() ?: 0.0
            val total = quantity * unitPrice
            viewModel.updateFieldValue(sectionId, "${baseId}_total_price", total.toString())
        }
        
        // Auto-calculate VAT and total for pricing_summary section
        sectionId == "pricing_summary" -> {
            when (fId) {
                "subtotal", "vat_rate" -> {
                    val subtotal = viewModel.getFieldValue("pricing_summary", "subtotal").toDoubleOrNull() ?: 0.0
                    val vatRate = viewModel.getFieldValue("pricing_summary", "vat_rate").toDoubleOrNull() ?: 17.0
                    
                    val vatAmount = QuoteCalculator.calculateVat(subtotal, vatRate)
                    val totalWithVat = subtotal + vatAmount
                    
                    viewModel.updateFieldValue("pricing_summary", "vat_amount", vatAmount.toString())
                    viewModel.updateFieldValue("pricing_summary", "total_with_vat", totalWithVat.toString())
                }
            }
        }
    }
}

@Composable
fun ImageFieldFromJson(
    fieldId: String,
    fieldLabel: String,
    required: Boolean,
    maxImages: Int,
    sectionId: String,
    imageViewModel: ImageViewModel,
    onCameraClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val images by imageViewModel.images.collectAsState()
    val sectionImages = remember(images, sectionId, fieldId) {
        images.filter { it.sectionId == sectionId }
    }
    
    var showDeleteDialog by remember { mutableStateOf<JobImage?>(null) }
    var showCaptionDialog by remember { mutableStateOf<JobImage?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    // Gallery launcher
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageDir = java.io.File(context.filesDir, "job_images")
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }
                
                val imageFileName = "image_${sectionId}_${System.currentTimeMillis()}.jpg"
                val imageFile = java.io.File(imageDir, imageFileName)
                
                inputStream?.use { input ->
                    java.io.FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Add to imageViewModel
                imageViewModel.addImageFromGallery(sectionId, imageFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = fieldLabel + if (required) " *" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        // Images Grid
        if (sectionImages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sectionImages) { image ->
                    ImageThumbnail(
                        image = image,
                        onDeleteClick = { showDeleteDialog = image },
                        onCaptionClick = { showCaptionDialog = image }
                    )
                }
                
                // Add button at the end if not at max
                if (sectionImages.size < maxImages) {
                    item {
                        AddImageButton(onClick = { showImageSourceDialog = true })
                    }
                }
            }
        } else {
            // Empty state - show add button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(onClick = { showImageSourceDialog = true }),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "לחץ להוספת תמונה",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "עד $maxImages תמונות",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Text(
            text = "${sectionImages.size}/$maxImages תמונות",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    // Image Source Dialog (Camera or Gallery)
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    "הוסף תמונה",
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
                            showImageSourceDialog = false
                            onCameraClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("צלם תמונה")
                    }
                    
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
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
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { image ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("מחק תמונה") },
            text = { Text("האם אתה בטוח שברצונך למחוק תמונה זו?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageViewModel.deleteImage(image)
                        showDeleteDialog = null
                    }
                ) {
                    Text("מחק", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("ביטול")
                }
            }
        )
    }
    
    // Caption Dialog
    showCaptionDialog?.let { image ->
        var captionText by remember { mutableStateOf(image.caption ?: "") }
        AlertDialog(
            onDismissRequest = { showCaptionDialog = null },
            title = { Text("הוסף כיתוב") },
            text = {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("כיתוב") },
                    placeholder = { Text("הכנס כיתוב לתמונה") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageViewModel.updateImageCaption(image.id, captionText)
                        showCaptionDialog = null
                    }
                ) {
                    Text("שמור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptionDialog = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
private fun ImageThumbnail(
    image: JobImage,
    onDeleteClick: () -> Unit,
    onCaptionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onCaptionClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = File(image.filePath),
                contentDescription = image.caption?.ifEmpty { "תמונה" } ?: "תמונה",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "מחק",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Caption indicator
            if (image.caption?.isNotEmpty() == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = image.caption ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AddImageButton(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "הוסף תמונה",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "צלם נוסף",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FindingCard(
    findingId: String,
    findingNumber: Int,
    viewModel: EditorViewModel,
    imageViewModel: ImageViewModel,
    jobId: Long,
    onCameraClick: (Long, String, String) -> Unit,
    onDelete: () -> Unit,
    onValueChanged: () -> Unit = {}
) {
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) } // Start expanded by default
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Gallery launcher for picking images
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            // Copy image from URI to internal storage
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(selectedUri)
                
                if (inputStream != null) {
                    // Create a unique file name
                    val fileName = "finding_${findingId}_${System.currentTimeMillis()}.jpg"
                    val outputFile = File(context.filesDir, fileName)
                    
                    // Copy file
                    inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Add to imageViewModel with the internal file path
                    imageViewModel.addImageFromGallery(
                        sectionId = findingId,
                        filePath = outputFile.absolutePath,
                        caption = ""
                    )
                    
                    android.util.Log.d("FindingCard", "✅ Image added from gallery: ${outputFile.absolutePath}")
                } else {
                    throw Exception("לא ניתן לקרוא את התמונה")
                }
            } catch (e: Exception) {
                android.util.Log.e("FindingCard", "❌ Error adding image from gallery", e)
                android.widget.Toast.makeText(
                    context,
                    "שגיאה בטעינת תמונה: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
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
            // Get subject for header display
            val job by viewModel.job.collectAsState()
            val subjectValue = viewModel.getFieldValue(findingId, "finding_subject")
            
            // Header with title, expand/collapse, and delete button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }, // Click to toggle
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "כווץ" else "הרחב",
                            tint = Primary
                        )
                    }
                    Column {
                Text(
                    text = "ממצא #$findingNumber",
                            style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                        if (!isExpanded && subjectValue.isNotBlank()) {
                            Text(
                                text = subjectValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Move up button
                    IconButton(
                        onClick = { viewModel.moveFindingUp(findingId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "העלה למעלה",
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Move down button
                    IconButton(
                        onClick = { viewModel.moveFindingDown(findingId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "הורד למטה",
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "מחק ממצא",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                    )
                    }
                }
            }
            
            if (isExpanded) {
            Divider()
            
            // Subject field
            val job by viewModel.job.collectAsState()
            val subjectValue = viewModel.getFieldValue(findingId, "finding_subject")
            var subject by remember(job?.id, findingId) { mutableStateOf(subjectValue) }
            
            // Sync with stored value when returning to screen OR switching findings
            LaunchedEffect(job?.id, findingId) {
                subject = viewModel.getFieldValue(findingId, "finding_subject")
            }
            
            OutlinedTextField(
                value = subject,
                onValueChange = { newValue ->
                    subject = newValue
                    viewModel.updateFieldValue(findingId, "finding_subject", newValue)
                    onValueChanged()
                },
                label = { Text("נושא") },
                placeholder = { Text("בדיקה דירה 13") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.ContentOrRtl
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            
            // Category field
            val categoryValue = viewModel.getFieldValue(findingId, "finding_category")
            var category by remember(job?.id, findingId) { mutableStateOf(categoryValue) }
            
            LaunchedEffect(job?.id, findingId) {
                category = viewModel.getFieldValue(findingId, "finding_category")
            }
            
            OutlinedTextField(
                value = category,
                onValueChange = { newValue ->
                    category = newValue
                    viewModel.updateFieldValue(findingId, "finding_category", newValue)
                    onValueChanged()
                },
                label = { Text("תת נושא") },
                placeholder = { Text("דוגמא: איטום\\ריצוף\\נגרות וכו") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.ContentOrRtl
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            
            // Description field
            val descriptionValue = viewModel.getFieldValue(findingId, "finding_description")
            var description by remember(job?.id, findingId) { mutableStateOf(descriptionValue) }
            
            LaunchedEffect(job?.id, findingId) {
                description = viewModel.getFieldValue(findingId, "finding_description")
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = { newValue ->
                    description = newValue
                    viewModel.updateFieldValue(findingId, "finding_description", newValue)
                    onValueChanged()
                },
                label = { Text("תיאור הבעיה לדוגמא") },
                placeholder = { Text("נמצאו ארכיטקטורי קרמיקה שבורים") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.ContentOrRtl
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )
            
            // Note field
            val noteValue = viewModel.getFieldValue(findingId, "finding_note")
            var note by remember(job?.id, findingId) { mutableStateOf(noteValue) }
            
            LaunchedEffect(job?.id, findingId) {
                note = viewModel.getFieldValue(findingId, "finding_note")
            }
            
            OutlinedTextField(
                value = note,
                onValueChange = { newValue ->
                    note = newValue
                    viewModel.updateFieldValue(findingId, "finding_note", newValue)
                    onValueChanged()
                },
                label = { Text("הערה") },
                placeholder = { Text("הערות נוספות") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.ContentOrRtl
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )
            
            // Images section
            val images by imageViewModel.images.collectAsState()
            val findingImages = remember(images, findingId) {
                images.filter { it.sectionId == findingId }
            }
            
            Text(
                text = "צירוף תמונות",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AddImageButton(
                        onClick = { showImageSourceDialog = true }  // Open dialog instead
                    )
                }
                
                items(findingImages) { image ->
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = File(image.filePath),
                                contentDescription = image.caption?.ifEmpty { "תמונה" } ?: "תמונה",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageViewModel.deleteImage(image) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "מחק תמונה",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recommendations section
            Text(
                text = "המלצות",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            // Get recommendations list
            val recommendationsJson = remember(findingId) {
                val dataValue = viewModel.getFieldValue(findingId, "recommendations")
                if (dataValue.isNotBlank()) {
                    try {
                        val gson = Gson()
                        gson.fromJson(dataValue, JsonArray::class.java) ?: JsonArray()
                    } catch (e: Exception) {
                        JsonArray()
                    }
                } else {
                    JsonArray()
                }
            }
            var recommendations by remember(recommendationsJson) { 
                mutableStateOf(recommendationsJson) 
            }
            
            // Add recommendation button
            OutlinedButton(
                onClick = {
                    val newRec = JsonObject().apply {
                        addProperty("id", System.currentTimeMillis().toString())
                        addProperty("description", "")
                        addProperty("quantity", "")
                        addProperty("unit", "")
                        addProperty("pricePerUnit", "")
                        addProperty("totalPrice", "")
                    }
                    recommendations.add(newRec)
                    val gson = Gson()
                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                    onValueChanged()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Accent
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("הוסף")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display recommendations
            recommendations.forEachIndexed { index, recElement ->
                val recObj = recElement.asJsonObject
                val recId = recObj.get("id")?.asString ?: ""
                val recDescription = recObj.get("description")?.asString ?: ""
                val recQuantity = recObj.get("quantity")?.asString ?: ""
                val recUnit = recObj.get("unit")?.asString ?: ""
                val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                
                var description by remember(recDescription) { mutableStateOf(recDescription) }
                var quantity by remember(recQuantity) { mutableStateOf(recQuantity) }
                var unit by remember(recUnit) { mutableStateOf(recUnit) }
                var pricePerUnit by remember(recPricePerUnit) { mutableStateOf(recPricePerUnit) }
                var totalPrice by remember(recTotalPrice) { mutableStateOf(recTotalPrice) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with delete button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "המלצה ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    recommendations.remove(recElement)
                                    val gson = Gson()
                                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                    onValueChanged()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "מחק המלצה",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Description field
                        OutlinedTextField(
                            value = description,
                            onValueChange = { newValue ->
                                description = newValue
                                recObj.addProperty("description", newValue)
                                val gson = Gson()
                                viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                onValueChanged()
                            },
                            label = { Text("תאור") },
                            placeholder = { Text("תיאור ההמלצה") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        
                        // Quantity and Unit in a row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                        quantity = newValue
                                        recObj.addProperty("quantity", newValue)
                                        
                                        // Auto calculate total price
                                        val qty = newValue.toDoubleOrNull() ?: 0.0
                                        val price = pricePerUnit.toDoubleOrNull() ?: 0.0
                                        val total = qty * price
                                        totalPrice = if (total > 0) total.toString() else ""
                                        recObj.addProperty("totalPrice", totalPrice)
                                        
                                        android.util.Log.d("FindingCard", "✅ Quantity changed: qty=$qty, price=$price, total=$total")
                                        
                                        val gson = Gson()
                                        viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                        viewModel.saveChanges() // Save immediately
                                        onValueChanged()
                                    }
                                },
                                label = { Text("כמות") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { newValue ->
                                    unit = newValue
                                    recObj.addProperty("unit", newValue)
                                    val gson = Gson()
                                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                    onValueChanged()
                                },
                                label = { Text("יחידה") },
                                placeholder = { Text("מ\"ר / יח' / שעות") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.ContentOrRtl
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }
                        
                        // Price per unit
                        OutlinedTextField(
                            value = pricePerUnit,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                    pricePerUnit = newValue
                                    recObj.addProperty("pricePerUnit", newValue)
                                    
                                    // Auto calculate total price
                                    val qty = quantity.toDoubleOrNull() ?: 0.0
                                    val price = newValue.toDoubleOrNull() ?: 0.0
                                    val total = qty * price
                                    totalPrice = if (total > 0) total.toString() else ""
                                    recObj.addProperty("totalPrice", totalPrice)
                                    
                                    android.util.Log.d("FindingCard", "✅ Price changed: qty=$qty, price=$price, total=$total")
                                    
                                    val gson = Gson()
                                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                    viewModel.saveChanges() // Save immediately
                                    onValueChanged()
                                }
                            },
                            label = { Text("מחיר יחידה") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        // Total price (editable - can be manually overridden)
                        OutlinedTextField(
                            value = totalPrice,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                    totalPrice = newValue
                                    recObj.addProperty("totalPrice", newValue)
                                    
                                    android.util.Log.d("FindingCard", "✅ Total price manually changed: $newValue")
                                    
                                    val gson = Gson()
                                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                    viewModel.saveChanges() // Save immediately
                                    onValueChanged()
                                }
                            },
                            label = { Text("מחיר כולל") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                focusedLabelColor = Accent
                            )
                        )
                    }
                }
                
                if (index < recommendations.size() - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            if (recommendations.size() == 0) {
                Text(
                    text = "אין כרגע פרטים מסוג המלצות בכרשימה, ניתן להוסיף פרטים על ידי לחיצה על כפתור הוסף",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            
            // Collapse button at bottom
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = false }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "כווץ",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "כווץ ממצא",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
            } // Close if (isExpanded)
    }
    
    // Image Source Dialog (Camera vs Gallery) for Findings
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("בחר מקור תמונה", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            onCameraClick(jobId, findingId, "finding_images")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("צלם תמונה")
                    }
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("בחר מהגלריה")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

/**
 * CategoryCard - Collapsible card for a finding category
 * Displays category title, controls, and nested findings
 */
@Composable
fun CategoryCard(
    category: com.ashaf.instanz.ui.viewmodel.FindingCategory,
    viewModel: EditorViewModel,
    imageViewModel: ImageViewModel,
    jobId: Long,
    onCameraClick: (Long, String, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleText by remember(category.title) { mutableStateOf(category.title) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "כווץ" else "הרחב",
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Primary.copy(alpha = 0.5f)
                            )
                        )
                        IconButton(onClick = {
                            viewModel.updateCategoryTitle(category.id, titleText)
                            isEditingTitle = false
                        }) {
                            Icon(Icons.Default.Check, "שמור", tint = Accent)
                        }
                    } else {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditingTitle = true }
                        )
                        IconButton(onClick = { isEditingTitle = true }) {
                            Icon(
                                Icons.Default.Edit,
                                "ערוך כותרת",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Category Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.moveCategoryUp(category.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, "הזז למעלה", tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { viewModel.moveCategoryDown(category.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, "הזז למטה", tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, "מחק קטגוריה", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            // Findings count badge
            if (!isExpanded) {
                Text(
                    text = "${category.findings.size} ממצאים",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
            
            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Add Finding button
                Button(
                    onClick = { viewModel.addFindingToCategory(category.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("הוסף ממצא לקטגוריה זו")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Display nested findings
                if (category.findings.isNotEmpty()) {
                    category.findings.forEachIndexed { index, finding ->
                        HierarchicalFindingCard(
                            categoryId = category.id,
                            finding = finding,
                            findingNumber = index + 1,
                            viewModel = viewModel,
                            imageViewModel = imageViewModel,
                            jobId = jobId,
                            onCameraClick = onCameraClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "אין ממצאים בקטגוריה זו",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Collapse button at bottom
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = false }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ExpandLess,
                        contentDescription = "כווץ",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "כווץ קטגוריה",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("מחיקת קטגוריה", fontWeight = FontWeight.Bold) },
            text = { Text("האם אתה בטוח שברצונך למחוק את הקטגוריה '${category.title}' ואת כל הממצאים שבה? פעולה זו לא ניתנת לשחזור.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(category.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

/**
 * HierarchicalFindingCard - Simplified finding card within a category
 * Displays finding details with minimal nesting
 */
@Composable
fun HierarchicalFindingCard(
    categoryId: String,
    finding: com.ashaf.instanz.ui.viewmodel.FindingItem,
    findingNumber: Int,
    viewModel: EditorViewModel,
    imageViewModel: ImageViewModel,
    jobId: Long,
    onCameraClick: (Long, String, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var subject by remember(finding.subject) { mutableStateOf(finding.subject) }
    var description by remember(finding.description) { mutableStateOf(finding.description) }
    var note by remember(finding.note) { mutableStateOf(finding.note) }
    
    // Gallery launcher for picking images
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(selectedUri)
                
                if (inputStream != null) {
                    val fileName = "finding_${finding.id}_${System.currentTimeMillis()}.jpg"
                    val outputFile = File(context.filesDir, fileName)
                    
                    inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    imageViewModel.addImageFromGallery(
                        sectionId = finding.id,
                        filePath = outputFile.absolutePath,
                        caption = ""
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HierarchicalFindingCard", "Error adding image from gallery", e)
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "כווץ" else "הרחב",
                        tint = Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "$findingNumber.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                        if (!isExpanded && subject.isNotBlank()) {
                            Text(
                                text = subject,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = { viewModel.moveFindingInCategory(categoryId, finding.id, -1) }) {
                        Icon(Icons.Default.ArrowUpward, "הזז למעלה", tint = Secondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.moveFindingInCategory(categoryId, finding.id, 1) }) {
                        Icon(Icons.Default.ArrowDownward, "הזז למטה", tint = Secondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "מחק", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Subject
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        viewModel.updateFindingInCategory(categoryId, finding.id, "subject", it)
                    },
                    label = { Text("נושא") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Secondary,
                        focusedLabelColor = Secondary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        viewModel.updateFindingInCategory(categoryId, finding.id, "description", it)
                    },
                    label = { Text("תיאור") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Secondary,
                        focusedLabelColor = Secondary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        viewModel.updateFindingInCategory(categoryId, finding.id, "note", it)
                    },
                    label = { Text("הערה") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Secondary,
                        focusedLabelColor = Secondary
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Images section
                val images by imageViewModel.images.collectAsState()
                val findingImages = remember(images, finding.id) {
                    images.filter { it.sectionId == finding.id }
                }
                
                Text(
                    text = "תמונות",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .size(100.dp)
                                .clickable { showImageSourceDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "הוסף תמונה",
                                    tint = Secondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    
                    items(findingImages) { image ->
                        Card(
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box {
                                AsyncImage(
                                    model = File(image.filePath),
                                    contentDescription = image.caption ?: "תמונה",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageViewModel.deleteImage(image) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "מחק",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ===================================
                // Recommendations section
                // ===================================
                Text(
                    text = "המלצות",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Get recommendations from hierarchical structure
                val recommendationsJson = remember(finding.id) {
                    // Try to load from job.dataJson -> categories -> [categoryId] -> findings -> [findingId] -> recommendations
                    try {
                        val job = viewModel.job.value
                        if (job != null && job.dataJson.isNotBlank()) {
                            val gson = Gson()
                            val jobDataJson = gson.fromJson(job.dataJson, JsonObject::class.java)
                            
                            if (jobDataJson.has("categories")) {
                                val categoriesArray = jobDataJson.getAsJsonArray("categories")
                                categoriesArray.forEach { categoryElement ->
                                    val categoryObj = categoryElement.asJsonObject
                                    if (categoryObj.get("id")?.asString == categoryId) {
                                        if (categoryObj.has("findings")) {
                                            val findingsArray = categoryObj.getAsJsonArray("findings")
                                            findingsArray.forEach { findingElement ->
                                                val findingObj = findingElement.asJsonObject
                                                if (findingObj.get("id")?.asString == finding.id) {
                                                    if (findingObj.has("recommendations")) {
                                                        return@remember findingObj.getAsJsonArray("recommendations")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        JsonArray()
                    } catch (e: Exception) {
                        android.util.Log.e("HierarchicalFindingCard", "Error loading recommendations: ${e.message}", e)
                        JsonArray()
                    }
                }
                
                var recommendations by remember(recommendationsJson) {
                    mutableStateOf(recommendationsJson)
                }
                
                // Update recommendations helper function
                fun updateRecommendations(newRecommendations: JsonArray) {
                    try {
                        val job = viewModel.job.value ?: return
                        val gson = Gson()
                        val jobDataJson = gson.fromJson(job.dataJson, JsonObject::class.java)
                        
                        if (jobDataJson.has("categories")) {
                            val categoriesArray = jobDataJson.getAsJsonArray("categories")
                            categoriesArray.forEach { categoryElement ->
                                val categoryObj = categoryElement.asJsonObject
                                if (categoryObj.get("id")?.asString == categoryId) {
                                    if (categoryObj.has("findings")) {
                                        val findingsArray = categoryObj.getAsJsonArray("findings")
                                        findingsArray.forEach { findingElement ->
                                            val findingObj = findingElement.asJsonObject
                                            if (findingObj.get("id")?.asString == finding.id) {
                                                findingObj.add("recommendations", newRecommendations)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Save to database
                            // Note: viewModel.saveChanges() already runs in viewModelScope, no need to launch here
                            viewModel.saveChanges()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HierarchicalFindingCard", "Error updating recommendations: ${e.message}", e)
                    }
                }
                
                // Add recommendation button
                OutlinedButton(
                    onClick = {
                        val newRec = JsonObject().apply {
                            addProperty("id", System.currentTimeMillis().toString())
                            addProperty("description", "")
                            addProperty("quantity", "")
                            addProperty("unit", "")
                            addProperty("pricePerUnit", "")
                            addProperty("totalPrice", "")
                        }
                        recommendations.add(newRec)
                        updateRecommendations(recommendations)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("הוסף המלצה")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display recommendations
                recommendations.forEachIndexed { index, recElement ->
                    val recObj = recElement.asJsonObject
                    val recId = recObj.get("id")?.asString ?: ""
                    val recDescription = recObj.get("description")?.asString ?: ""
                    val recQuantity = recObj.get("quantity")?.asString ?: ""
                    val recUnit = recObj.get("unit")?.asString ?: ""
                    val recPricePerUnit = recObj.get("pricePerUnit")?.asString ?: ""
                    val recTotalPrice = recObj.get("totalPrice")?.asString ?: ""
                    
                    var description by remember(recDescription) { mutableStateOf(recDescription) }
                    var quantity by remember(recQuantity) { mutableStateOf(recQuantity) }
                    var unit by remember(recUnit) { mutableStateOf(recUnit) }
                    var pricePerUnit by remember(recPricePerUnit) { mutableStateOf(recPricePerUnit) }
                    var totalPrice by remember(recTotalPrice) { mutableStateOf(recTotalPrice) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header with delete button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "המלצה ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        recommendations.remove(recElement)
                                        updateRecommendations(recommendations)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "מחק המלצה",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // Description field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { newValue ->
                                    description = newValue
                                    recObj.addProperty("description", newValue)
                                    updateRecommendations(recommendations)
                                },
                                label = { Text("תיאור") },
                                placeholder = { Text("תיאור ההמלצה") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                minLines = 2,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.ContentOrRtl
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    focusedLabelColor = Secondary
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            
                            // Quantity and Unit in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                            quantity = newValue
                                            recObj.addProperty("quantity", newValue)
                                            
                                            // Auto calculate total price
                                            val qty = newValue.toDoubleOrNull() ?: 0.0
                                            val price = pricePerUnit.toDoubleOrNull() ?: 0.0
                                            val total = qty * price
                                            totalPrice = if (total > 0) total.toString() else ""
                                            recObj.addProperty("totalPrice", totalPrice)
                                            
                                            updateRecommendations(recommendations)
                                        }
                                    },
                                    label = { Text("כמות") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Secondary,
                                        focusedLabelColor = Secondary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = { newValue ->
                                        unit = newValue
                                        recObj.addProperty("unit", newValue)
                                        updateRecommendations(recommendations)
                                    },
                                    label = { Text("יחידה") },
                                    placeholder = { Text("מ\"ר / יח' / שעות") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Secondary,
                                        focusedLabelColor = Secondary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                            }
                            
                            // Price per unit
                            OutlinedTextField(
                                value = pricePerUnit,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                        pricePerUnit = newValue
                                        recObj.addProperty("pricePerUnit", newValue)
                                        
                                        // Auto calculate total price
                                        val qty = quantity.toDoubleOrNull() ?: 0.0
                                        val price = newValue.toDoubleOrNull() ?: 0.0
                                        val total = qty * price
                                        totalPrice = if (total > 0) total.toString() else ""
                                        recObj.addProperty("totalPrice", totalPrice)
                                        
                                        updateRecommendations(recommendations)
                                    }
                                },
                                label = { Text("מחיר יחידה") },
                                placeholder = { Text("0.00") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    focusedLabelColor = Secondary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            // Total price (editable - can be manually overridden)
                            OutlinedTextField(
                                value = totalPrice,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                        totalPrice = newValue
                                        recObj.addProperty("totalPrice", newValue)
                                        updateRecommendations(recommendations)
                                    }
                                },
                                label = { Text("מחיר כולל") },
                                placeholder = { Text("0.00") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    focusedLabelColor = Accent
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    
                    if (index < recommendations.size() - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                if (recommendations.size() == 0) {
                    Text(
                        text = "אין כרגע המלצות, לחץ על 'הוסף המלצה' כדי להוסיף",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("בחר מקור תמונה", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            onCameraClick(jobId, finding.id, "finding_images")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("צלם תמונה")
                    }
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("בחר מהגלריה")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("מחיקת ממצא", fontWeight = FontWeight.Bold) },
            text = { Text("האם אתה בטוח שברצונך למחוק את הממצא? פעולה זו לא ניתנת לשחזור.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFindingFromCategory(categoryId, finding.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}


