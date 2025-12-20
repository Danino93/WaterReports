package com.ashaf.instanz.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: com.ashaf.instanz.ui.viewmodel.SettingsViewModel = viewModel(
        factory = com.ashaf.instanz.ui.viewmodel.SettingsViewModelFactory(
            settingsDataStore = appContainer.settingsDataStore
        )
    )
    
    // Collect all settings from ViewModel
    val versionNumber by viewModel.versionNumber.collectAsState()
    val language by viewModel.language.collectAsState()
    val reportColorScheme by viewModel.reportColorScheme.collectAsState()
    val maxJobsPerScreen by viewModel.maxJobsPerScreen.collectAsState()
    val imagesPerRow by viewModel.imagesPerRow.collectAsState()
    val specialImagesPerRow by viewModel.specialImagesPerRow.collectAsState()
    val vatPercent by viewModel.vatPercent.collectAsState()
    val showHeaderInReport by viewModel.showHeaderInReport.collectAsState()
    val textFormattingSupport by viewModel.textFormattingSupport.collectAsState()
    val quickPhoto by viewModel.quickPhoto.collectAsState()
    val showChartInReport by viewModel.showChartInReport.collectAsState()
    val showOnlyMultipleStatuses by viewModel.showOnlyMultipleStatuses.collectAsState()
    val unlimitedJobs by viewModel.unlimitedJobs.collectAsState()
    val accountStatus by viewModel.accountStatus.collectAsState()
    val lastBackupDate by viewModel.lastBackupDate.collectAsState()
    
    val context = LocalContext.current
    
    // Format last backup date
    val lastBackupText = remember(lastBackupDate) {
        if (lastBackupDate > 0) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("he", "IL"))
            "גיבוי אחרון תהליך עבור באהלהח, ${dateFormat.format(Date(lastBackupDate))}"
        } else {
            "לא בוצע גיבוי עדיין"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "הגדרות/אודות",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowForward, "חזרה", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Save settings */ }) {
                        Icon(Icons.Default.Check, "סגירה", tint = Color.White)
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
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // תוכנה
            SectionHeader(title = "תוכנה", color = Accent)
            SettingsCard {
                SettingsItem(
                    label = "מספר גרסה",
                    value = versionNumber,
                    icon = Icons.Default.Info
                )
            }

            // תנאים
            SectionHeader(title = "תנאים", color = Accent)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sabaza.com/terms"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "תנאי שימוש באפליקציה",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Primary
                        )
                    }
                    Divider()
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sabaza.com/privacy"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "מדיניות פרטיות",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Primary
                        )
                    }
                }
            }

            // הגדרות
            SectionHeader(title = "הגדרות", color = Accent)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // שפה
                    DropdownSettingItem(
                        label = "שפה",
                        value = language,
                        options = listOf("Hebrew/עברית", "English/אנגלית"),
                        onValueChange = { viewModel.saveLanguage(it) }
                    )

                    Divider()

                    // צבעי דוח
                    DropdownSettingItem(
                        label = "צבעי דוח",
                        value = reportColorScheme,
                        options = listOf("בהיר", "כהה"),
                        onValueChange = { viewModel.saveReportColorScheme(it) }
                    )

                    Divider()

                    // מקסימום עבודות במסך
                    SliderSettingItem(
                        label = "מקסימום עבודות במסך עבודות",
                        value = maxJobsPerScreen,
                        range = 10f..200f,
                        onValueChange = { viewModel.saveMaxJobsPerScreen(it) },
                        enabled = !unlimitedJobs
                    )
                    
                    Divider()
                    
                    // מתג ללא הגבלה
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "הצג את כל העבודות (ללא הגבלה)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = unlimitedJobs,
                            onCheckedChange = { viewModel.saveUnlimitedJobs(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Accent
                            )
                        )
                    }

                    Divider()

                    // מספר תמונות בשורה
                    SliderSettingItem(
                        label = "מספר תמונות בשורה בדוח",
                        value = imagesPerRow,
                        range = 1f..4f,
                        steps = 2,
                        onValueChange = { viewModel.saveImagesPerRow(it) }
                    )

                    Divider()

                    // מספר תמונות מיוחדות בשורה
                    SliderSettingItem(
                        label = "מספר תמונות מיוחדות בשורה בדוח",
                        value = specialImagesPerRow,
                        range = 1f..4f,
                        steps = 2,
                        onValueChange = { viewModel.saveSpecialImagesPerRow(it) }
                    )

                    Divider()

                    // אחוז מע"מ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "אחוז מע\"מ",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedTextField(
                            value = vatPercent.toInt().toString(),
                            onValueChange = { 
                                it.toFloatOrNull()?.let { newValue -> 
                                    viewModel.saveVatPercent(newValue)
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // סליידרים - תצוגה
            SwitchSettingItem(
                label = "הצג נושא בראש הדף בדוח",
                checked = showHeaderInReport,
                onCheckedChange = { viewModel.saveShowHeaderInReport(it) }
            )

            SwitchSettingItem(
                label = "תמיכה בעיצוב טקסט",
                checked = textFormattingSupport,
                onCheckedChange = { viewModel.saveTextFormattingSupport(it) }
            )

            SwitchSettingItem(
                label = "צילום מהיר",
                checked = quickPhoto,
                onCheckedChange = { viewModel.saveQuickPhoto(it) }
            )

            // תרשים סיכום מצב עבודה
            SectionHeader(title = "תרשים סיכום מצב עבודה", color = Accent)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwitchRow(
                        label = "הצג תרשים בדוח",
                        checked = showChartInReport,
                        onCheckedChange = { viewModel.saveShowChartInReport(it) }
                    )
                    Divider()
                    SwitchRow(
                        label = "הצג רק סטטוסים שנפסקו יותר מפעם אחת",
                        checked = showOnlyMultipleStatuses,
                        onCheckedChange = { viewModel.saveShowOnlyMultipleStatuses(it) }
                    )
                }
            }

            // פרטי רישום משתמש
            SectionHeader(title = "פרטי רישום משתמש", color = Accent)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("מצב חשבון משתמש", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            accountStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Divider()

                    Text(
                        "ניתן לשלוח בקשה להפעלת חשבון ואנחנו נחזור אליכם לאי מייל ל danino93@gmail.com עם הוראות ההפעלה או שאפשר ליצור קשר עם info@sabaza.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("danino93@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "בקשה להפעלת חשבון - WaterReports")
                                putExtra(Intent.EXTRA_TEXT, "שלום,\n\nאני מבקש להפעיל את חשבוני באפליקציה.\n\nתודה")
                            }
                            context.startActivity(Intent.createChooser(intent, "שלח מייל"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("שלח בקשה להפעלת חשבון")
                    }
                }
            }

            // תחזוקת מכשיר
            SectionHeader(title = "תחזוקת מכשיר", color = Accent)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateLastBackupDate()
                            // TODO: Implement actual backup logic
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lastBackupText, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            // TODO: Stop backup process
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("עצירת תהליך גיבוי")
                    }

                    Button(
                        onClick = {
                            // TODO: Open location settings
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ומקום")
                    }
                }
            }

            // ניהום מכשיר
            Button(
                onClick = { /* TODO: Device management */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("ניהום מכשיר", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SettingsItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = TextSecondary)
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
fun DropdownSettingItem(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.width(200.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(value)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        leadingIcon = {
                            if (option == value) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SliderSettingItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label, 
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.Unspecified else TextSecondary
            )
            Text(
                value.toInt().toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Primary else TextSecondary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                disabledThumbColor = Gray500,
                disabledActiveTrackColor = Gray500
            )
        )
    }
}

@Composable
fun SwitchSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary
                )
            )
        }
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary
            )
        )
    }
}
