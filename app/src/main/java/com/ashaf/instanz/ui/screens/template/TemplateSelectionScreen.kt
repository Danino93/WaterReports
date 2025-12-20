package com.ashaf.instanz.ui.screens.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.R
import com.ashaf.instanz.data.models.Template
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.TemplateViewModel
import com.ashaf.instanz.ui.viewmodel.ViewModelFactory
import com.ashaf.instanz.utils.TemplateLoader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionScreen(
    onTemplateSelected: (String, Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: TemplateViewModel = viewModel(
        factory = ViewModelFactory(
            jobRepository = appContainer.jobRepository,
            templateRepository = appContainer.templateRepository
        )
    )
    
    val templates by viewModel.templates.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Load default templates if none exist
    LaunchedEffect(Unit) {
        if (templates.isEmpty()) {
            // Load water damage template
            val waterDamageTemplate = TemplateLoader.loadTemplateFromRaw(
                context = context,
                resId = R.raw.template_water_damage
            )
            waterDamageTemplate?.let {
                appContainer.templateRepository.insertTemplate(it)
            }
            
            // Load quote template
            val quoteTemplate = TemplateLoader.loadTemplateFromRaw(
                context = context,
                resId = R.raw.template_quote
            )
            quoteTemplate?.let {
                appContainer.templateRepository.insertTemplate(it)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "בחר סוג עבודה",
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            if (templates.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(templates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = {
                                scope.launch {
                                    // Create a new job immediately
                                    val newJob = com.ashaf.instanz.data.models.Job(
                                        templateId = template.id,
                                        jobNumber = "JOB-${System.currentTimeMillis()}",
                                        title = template.name,
                                        clientFirstName = "לקוח",
                                        clientLastName = "חדש",
                                        clientPhone = "",
                                        clientAddress = "",
                                        clientCompany = null,
                                        dateCreated = System.currentTimeMillis(),
                                        dateModified = System.currentTimeMillis(),
                                        status = com.ashaf.instanz.data.models.JobStatus.DRAFT,
                                        dataJson = "{}"
                                    )
                                    val jobId = appContainer.jobRepository.insertJob(newJob)
                                    onTemplateSelected(template.id, jobId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = template.icon.ifEmpty { "📋" },
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}


