package com.ashaf.instanz.ui.screens.job

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashaf.instanz.data.models.JobSettings
import com.ashaf.instanz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSettingsScreen(
    jobId: Long,
    currentSettings: JobSettings = JobSettings.default(),
    onSaveSettings: (JobSettings) -> Unit,
    onBackClick: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "הגדרת עבודה",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onSaveSettings(settings)
                        onBackClick()
                    }) {
                        Icon(Icons.Default.Check, "אישור", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // תמונות בדו"ח
            SwitchCard(
                label = "הצג תמונות בדו\"ח",
                checked = settings.showImagesInReport,
                onCheckedChange = { settings = settings.copy(showImagesInReport = it) },
                backgroundColor = Accent
            )

            SwitchCard(
                label = "הצג מספר תמונות בדו\"ח",
                checked = settings.showImageCount,
                onCheckedChange = { settings = settings.copy(showImageCount = it) },
                backgroundColor = Accent
            )

            SwitchCard(
                label = "הצג תאריך פעילות\\ממצא",
                checked = settings.showActivityDate,
                onCheckedChange = { settings = settings.copy(showActivityDate = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            SwitchCard(
                label = "הפרדת פעילויות\\ממצאים לפי יום",
                checked = settings.separateActivitiesByDay,
                onCheckedChange = { settings = settings.copy(separateActivitiesByDay = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            SwitchCard(
                label = "הוצאת פעילויות לפי תאריך בסדר עולה",
                checked = settings.sortActivitiesAscending,
                onCheckedChange = { settings = settings.copy(sortActivitiesAscending = it) },
                backgroundColor = Accent
            )

            SwitchCard(
                label = "הצג תוכן עניינים",
                checked = settings.showTableOfContents,
                onCheckedChange = { settings = settings.copy(showTableOfContents = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // מחירים
            Text(
                text = "מחירים",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SwitchCard(
                label = "הצג מחירים בדו\"ח",
                checked = settings.showPricesInReport,
                onCheckedChange = { settings = settings.copy(showPricesInReport = it) },
                backgroundColor = Accent
            )

            SwitchCard(
                label = "הצג מחיר יחידה וכמות בדו\"ח",
                checked = settings.showUnitPriceAndQuantity,
                onCheckedChange = { settings = settings.copy(showUnitPriceAndQuantity = it) },
                backgroundColor = Accent
            )

            SwitchCard(
                label = "הצג מע\"מ בדו\"ח",
                checked = settings.showVatInReport,
                onCheckedChange = { settings = settings.copy(showVatInReport = it) },
                backgroundColor = Accent
            )

            // אחוז מע"מ
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
                        "אחוז מע\"מ:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = settings.vatPercent.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { newValue ->
                                settings = settings.copy(vatPercent = newValue)
                            }
                        },
                        modifier = Modifier.width(120.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // פרטי סיכום מחיר
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF8BC34A)),
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
                        "פרטי סיכום מחיר",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { /* TODO: Add pricing item */ }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "הוספה",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                text = "אין כרגע פריטי סיכום מחיר ברשימה, ניתן להוסיף פריטים על ידי לחיצה על כפתור הוספה, דוגמא לפריטים -הוספת 10 אחוז פקיקות למחיר20 אחוז הנחה למחיר",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // הגדרות מתקדמות
            Text(
                text = "הגדרות מתקדמות",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SwitchCard(
                label = "הצג בבחירת תבניות\\חברות אם - עבודת מאסטר",
                checked = settings.showInMasterTemplates,
                onCheckedChange = { settings = settings.copy(showInMasterTemplates = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // אנשי קשר לטיפול\אישור
            Text(
                text = "אנשי קשר לטיפול\\אישור",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SwitchCard(
                label = "הצג אנשי קשר בדוח",
                checked = settings.showContactsInReport,
                onCheckedChange = { settings = settings.copy(showContactsInReport = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            SwitchCard(
                label = "הצג אי מייל של אנשי קשר",
                checked = settings.showContactEmail,
                onCheckedChange = { settings = settings.copy(showContactEmail = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            SwitchCard(
                label = "הצג טלפון של אנשי קשר",
                checked = settings.showContactPhone,
                onCheckedChange = { settings = settings.copy(showContactPhone = it) },
                backgroundColor = Color(0xFFCCCCCC)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // חתימות
            Text(
                text = "חתימות",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SwitchCard(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (backgroundColor == Accent || backgroundColor == Color(0xFF8BC34A)) Color.White else Color.Black,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = if (backgroundColor == Accent) Primary else Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}

