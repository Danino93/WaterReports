package com.ashaf.instanz.ui.screens.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.JobViewModel
import com.ashaf.instanz.ui.viewmodel.JobViewModelFactory
import com.ashaf.instanz.ui.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    templateId: String,
    onJobCreated: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: JobViewModel = viewModel(
        factory = JobViewModelFactory(
            jobRepository = appContainer.jobRepository,
            jobId = null
        )
    )
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createdJobId by viewModel.createdJobId.collectAsState()
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Navigate when job is created
    LaunchedEffect(createdJobId) {
        createdJobId?.let { jobId ->
            if (jobId > 0) {
                onJobCreated(jobId)
            }
        }
    }
    
    LaunchedEffect(error) {
        error?.let {
            errorMessage = it
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "פרטי עבודה",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "חזרה")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                // Skip button
                FloatingActionButton(
                    onClick = {
                        errorMessage = null
                        viewModel.createJob(
                            templateId = templateId,
                            clientFirstName = "לקוח",
                            clientLastName = "חדש",
                            clientPhone = "",
                            clientAddress = "",
                            clientCompany = null
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("דלג", style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Default.KeyboardArrowLeft, "דלג")
                    }
                }
                
                // Continue button
                FloatingActionButton(
                    onClick = {
                        errorMessage = null
                        viewModel.createJob(
                            templateId = templateId,
                            clientFirstName = firstName.ifEmpty { "לקוח" },
                            clientLastName = lastName.ifEmpty { "חדש" },
                            clientPhone = phone,
                            clientAddress = address,
                            clientCompany = company.ifEmpty { null }
                        )
                    },
                    containerColor = Accent
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.ArrowForward, "המשך")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                errorMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                Text(
                    text = "פרטי לקוח",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("שם פרטי") },
                    placeholder = { Text("הכנס שם פרטי (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("שם משפחה") },
                    placeholder = { Text("הכנס שם משפחה (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("טלפון") },
                    placeholder = { Text("הכנס מספר טלפון (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("כתובת") },
                    placeholder = { Text("הכנס כתובת (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("חברה (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

// Validation function removed - all fields are now optional

