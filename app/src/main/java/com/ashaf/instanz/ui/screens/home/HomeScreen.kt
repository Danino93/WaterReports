package com.ashaf.instanz.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.data.models.Job
import com.ashaf.instanz.data.models.JobStatus
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import com.ashaf.instanz.ui.viewmodel.HomeViewModel
import com.ashaf.instanz.ui.viewmodel.JobWithTemplate
import com.ashaf.instanz.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewJobClick: () -> Unit,
    onJobClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onTemplateListClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            jobRepository = appContainer.jobRepository,
            templateRepository = appContainer.templateRepository
        )
    )
    
    val jobs by viewModel.filteredJobs.collectAsState()
    val totalJobs by viewModel.totalJobsCount.collectAsState()
    val monthlyJobs by viewModel.monthlyJobsCount.collectAsState()
    val draftJobs by viewModel.draftJobsCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Load max jobs setting
    val maxJobsPerScreen by appContainer.settingsDataStore.maxJobsPerScreen.collectAsState(initial = 100f)
    val unlimitedJobs by appContainer.settingsDataStore.unlimitedJobs.collectAsState(initial = true)
    
    val displayedJobs = remember(jobs, maxJobsPerScreen, unlimitedJobs) {
        if (unlimitedJobs) {
            jobs // הצג הכל - ללא הגבלה! ✅
        } else {
            jobs.take(maxJobsPerScreen.toInt()) // הגבלה לפי מספר שנבחר
        }
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onTemplateListClick = {
                    scope.launch { drawerState.close() }
                    onTemplateListClick()
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onSettingsClick()
                }
            )
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "עבודות",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Menu, "תפריט", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onNewJobClick,
                containerColor = Secondary,
                contentColor = Color.White,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                text = {
                    Text(
                        "עבודה חדשה",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Statistics Cards
            StatisticsRow(
                totalJobs = totalJobs,
                monthlyJobs = monthlyJobs,
                draftJobs = draftJobs
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
                       // Jobs List
                       LazyColumn(
                           contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                           verticalArrangement = Arrangement.spacedBy(12.dp)
                       ) {
                           items(displayedJobs) { jobWithTemplate ->
                               JobCard(
                                   job = jobWithTemplate.job,
                                   templateName = jobWithTemplate.templateName,
                                   onClick = { onJobClick(jobWithTemplate.job.id) }
                               )
                           }
                       }
        }
    }
    }
}

@Composable
fun StatisticsRow(
    totalJobs: Int,
    monthlyJobs: Int,
    draftJobs: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "סה״כ",
            value = totalJobs.toString(),
            color = Primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "החודש",
            value = monthlyJobs.toString(),
            color = Secondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "טיוטות",
            value = draftJobs.toString(),
            color = Accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun JobCard(
    job: Job,
    templateName: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("he", "IL")) }
    val dateText = dateFormat.format(Date(job.dateModified))
    
    val statusColor = when (job.status) {
        JobStatus.DRAFT -> Accent
        JobStatus.IN_PROGRESS -> Color(0xFFFF9800)
        JobStatus.COMPLETED -> Secondary
        JobStatus.SENT -> Primary
        JobStatus.ARCHIVED -> Gray500
    }
    
    val statusText = when (job.status) {
        JobStatus.DRAFT -> "טיוטה"
        JobStatus.IN_PROGRESS -> "בתהליך"
        JobStatus.COMPLETED -> "הושלם"
        JobStatus.SENT -> "נשלח"
        JobStatus.ARCHIVED -> "ארכיון"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image placeholder with gradient
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.1f),
                                Secondary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Client name with larger font
                Text(
                    text = "${job.clientFirstName} ${job.clientLastName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary
                )
                
                // Template name in a chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = templateName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Address with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = job.clientAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Date and job number in a row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextSecondary
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Status badge - improved
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Arrow icon
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Gray400,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DrawerContent(
    onTemplateListClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "עבודות",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "תבניות\\הברות אם",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Divider()
        
        // Menu Items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFA000)) },
            label = { Text("תיקיות\\פרויקטים") },
            selected = false,
            onClick = { /* TODO */ }
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = Accent) },
            label = { Text("דוח אקסל מרכז") },
            selected = false,
            onClick = { /* TODO */ }
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F)) },
            label = { Text("דוח PDF עבודות מרכז") },
            selected = false,
            onClick = { /* TODO */ }
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF0288D1)) },
            label = { Text("אנשי מקצוע מקושרים") },
            selected = false,
            onClick = { /* TODO */ }
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = Accent) },
            label = { Text("אקסל -ביקורי לקוח CRM") },
            selected = false,
            onClick = { /* TODO */ }
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9C27B0)) },
            label = { Text("תבניות אם") },
            selected = false,
            onClick = onTemplateListClick
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF757575)) },
            label = { Text("הגדרות/אודות") },
            selected = false,
            onClick = onSettingsClick
        )
    }
}
