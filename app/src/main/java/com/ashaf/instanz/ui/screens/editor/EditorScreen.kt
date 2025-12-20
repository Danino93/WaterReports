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
    
    // Auto-save function
    fun autoSave() {
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
                    IconButton(onClick = { autoSave() }) {
                        Icon(Icons.Default.Save, "שמירה", tint = Color.White)
                    }
                    
                    // Share button
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Default.Share, "שיתוף", tint = Color.White)
                    }
                    
                    // PDF/Preview button
                    IconButton(onClick = { jobId.let(onPreviewClick) }) {
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
                                    jobId.let(onPreviewClick)
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
                                        jobId = jobId,
                                        onValueChanged = { autoSave() }
                                    )
                                }
                            }
                            2 -> {
                                // Third tab: Dynamic findings
                                // Add button
                                Button(
                                    onClick = { viewModel.addFinding() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("הוסף", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Display findings
                                findings.forEachIndexed { index, findingId ->
                                    FindingCard(
                                        findingId = findingId,
                                        findingNumber = index + 1,
                                        viewModel = viewModel,
                                        imageViewModel = imageViewModel,
                                        jobId = jobId,
                                        onCameraClick = onCameraClick,
                                        onDelete = { viewModel.deleteFinding(findingId) },
                                        onValueChanged = { autoSave() }
                                    )
                                }
                                
                                if (findings.isEmpty()) {
                                    Text(
                                        text = "אין כרגע ממצאים, ניתן להוסיף פרטים על ידי לחיצה על כפתור הוסף",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp)
                                    )
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
    val currentValue = remember(fieldId) {
        viewModel.getFieldValue(sectionId, fieldId)
    }
    var textValue by remember(currentValue) { mutableStateOf(currentValue) }
    
    // Check if field is empty and required
    val isError = required && textValue.isBlank()
    
    Column {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                viewModel.updateFieldValue(sectionId, fieldId, newValue)
                onValueChanged()
            },
            label = { Text(fieldLabel + if (required) " *" else "") },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = lines == 1,
            minLines = if (lines > 1) lines else 1,
            maxLines = if (lines > 1) lines + 3 else 1,
            isError = isError,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDirection = TextDirection.ContentOrRtl
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = if (lines == 1) ImeAction.Next else ImeAction.Default
            )
        )
        
        if (isError) {
            Text(
                text = "שדה חובה",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
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
    val currentValue = remember(fieldId) {
        viewModel.getFieldValue(sectionId, fieldId)
    }
    var textValue by remember(currentValue) { mutableStateOf(currentValue) }
    
    // Check if field is empty and required
    val isError = required && textValue.isBlank()
    
    Column {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                viewModel.updateFieldValue(sectionId, fieldId, newValue)
                onValueChanged()
            },
            label = { Text(fieldLabel + if (required) " *" else "") },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (lines * 24).dp),
            minLines = lines,
            maxLines = lines + 3,
            isError = isError,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDirection = TextDirection.ContentOrRtl
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default
            )
        )
        
        if (isError) {
            Text(
                text = "שדה חובה",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
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
    val currentValue = remember(fieldId) {
        viewModel.getFieldValue(sectionId, fieldId)
    }
    var textValue by remember(currentValue) {
        mutableStateOf(if (currentValue.isBlank()) default.toString() else currentValue)
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
        label = { Text(fieldLabel + if (required) " *" else "") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = required && textValue.isBlank(),
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
    val images by imageViewModel.images.collectAsState()
    val sectionImages = remember(images, sectionId, fieldId) {
        images.filter { it.sectionId == sectionId }
    }
    
    var showDeleteDialog by remember { mutableStateOf<JobImage?>(null) }
    var showCaptionDialog by remember { mutableStateOf<JobImage?>(null) }
    
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
                        AddImageButton(onClick = onCameraClick)
                    }
                }
            }
        } else {
            // Empty state - show add button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(onClick = onCameraClick),
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
                        text = "לחץ לצילום תמונה",
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
            // Header with title and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ממצא #$findingNumber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "מחק ממצא",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Divider()
            
            // Subject field
            val subjectValue = remember(findingId) {
                viewModel.getFieldValue(findingId, "finding_subject")
            }
            var subject by remember(subjectValue) { mutableStateOf(subjectValue) }
            
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
            val categoryValue = remember(findingId) {
                viewModel.getFieldValue(findingId, "finding_category")
            }
            var category by remember(categoryValue) { mutableStateOf(categoryValue) }
            
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
            val descriptionValue = remember(findingId) {
                viewModel.getFieldValue(findingId, "finding_description")
            }
            var description by remember(descriptionValue) { mutableStateOf(descriptionValue) }
            
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
            val noteValue = remember(findingId) {
                viewModel.getFieldValue(findingId, "finding_note")
            }
            var note by remember(noteValue) { mutableStateOf(noteValue) }
            
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
                        onClick = { onCameraClick(jobId, findingId, "finding_images") }
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
                                        
                                        val gson = Gson()
                                        viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
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
                                    
                                    val gson = Gson()
                                    viewModel.updateFieldValue(findingId, "recommendations", gson.toJson(recommendations))
                                    onValueChanged()
                                }
                            },
                            label = { Text("מחיר יחידה") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        // Total price (read-only)
                        OutlinedTextField(
                            value = totalPrice,
                            onValueChange = {},
                            label = { Text("מחיר") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}


