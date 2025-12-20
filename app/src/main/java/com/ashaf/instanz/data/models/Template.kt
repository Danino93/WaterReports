package com.ashaf.instanz.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: String,
    val version: String,
    val jsonData: String,
    val isCustom: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val customContent: String? = null // JSON של תוכן מותאם אישית
) {
    fun toTemplateData(): TemplateData {
        val type = object : TypeToken<TemplateData>() {}.type
        return Gson().fromJson(jsonData, type)
    }
    
    fun parseCustomContent(): TemplateCustomContent? {
        return if (customContent != null) {
            try {
                Gson().fromJson(customContent, TemplateCustomContent::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
}

data class TemplateData(
    val id: String,
    val name: String,
    val icon: String,
    val version: String,
    val sections: List<Section>,
    val pdfLayout: PdfLayout? = null
)

data class Section(
    val id: String,
    val title: String,
    val order: Int,
    val editable: Boolean = true,
    val fields: List<Field>
)

sealed class Field {
    abstract val id: String
    abstract val label: String
    abstract val required: Boolean

    data class TextField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val placeholder: String = "",
        val lines: Int = 1,
        val default: String = ""
    ) : Field()

    data class TextAreaField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val placeholder: String = "",
        val lines: Int = 5,
        val default: String = ""
    ) : Field()

    data class ImageField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val maxImages: Int = 10
    ) : Field()

    data class TableField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val columns: List<String>,
        val rows: Int = 5
    ) : Field()

    data class CheckboxField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val checked: Boolean = false
    ) : Field()

    data class DropdownField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val options: List<String>,
        val default: String = ""
    ) : Field()

    data class NumberField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false,
        val default: Double = 0.0,
        val min: Double? = null,
        val max: Double? = null
    ) : Field()

    data class DateField(
        override val id: String,
        override val label: String,
        override val required: Boolean = false
    ) : Field()
}

data class PdfLayout(
    val headerHeight: Int = 80,
    val footerHeight: Int = 60,
    val margins: List<Int> = listOf(20, 20, 20, 20),
    val colors: PdfColors = PdfColors()
)

data class PdfColors(
    val primary: String = "#1976D2",
    val secondary: String = "#4CAF50"
)

// Recommendation/Quote Item
data class RecommendationItem(
    val description: String,
    val quantity: Double,
    val unit: String,
    val pricePerUnit: Double
) {
    val total: Double
        get() = quantity * pricePerUnit
}

// Quote Summary
data class QuoteSummary(
    val items: List<RecommendationItem>,
    val vatPercent: Int = 17
) {
    val subtotal: Double
        get() = items.sumOf { it.total }
    
    val vat: Double
        get() = subtotal * vatPercent / 100
    
    val total: Double
        get() = subtotal + vat
}

// Template Custom Content - תוכן מותאם אישית של תבנית
data class TemplateCustomContent(
    val inspectorName: String = "",
    val experienceTitle: String = "",
    val experienceText: String = "",
    val certificateImagePath: String? = null,
    val sections: Map<String, List<TemplateSectionItem>> = emptyMap()
)

data class TemplateSectionItem(
    val text: String,
    val order: Int = 0
)
