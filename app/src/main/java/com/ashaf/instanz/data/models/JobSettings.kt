package com.ashaf.instanz.data.models

data class JobSettings(
    // תמונות בדו"ח
    val showImagesInReport: Boolean = true,
    val showImageCount: Boolean = true,
    val showActivityDate: Boolean = false,
    val separateActivitiesByDay: Boolean = false,
    val sortActivitiesAscending: Boolean = true,
    val showTableOfContents: Boolean = false,
    
    // מחירים
    val showPricesInReport: Boolean = true,
    val showUnitPriceAndQuantity: Boolean = true,
    val showVatInReport: Boolean = true,
    val vatPercent: Int = 18,
    
    // הגדרות מתקדמות
    val showInMasterTemplates: Boolean = false,
    
    // אנשי קשר
    val showContactsInReport: Boolean = false,
    val showContactEmail: Boolean = false,
    val showContactPhone: Boolean = false,
    
    // פרטי סיכום מחיר
    val pricingSummaryItems: List<String> = emptyList()
) {
    companion object {
        fun default() = JobSettings()
    }
}

