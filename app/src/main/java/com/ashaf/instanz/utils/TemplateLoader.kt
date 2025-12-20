package com.ashaf.instanz.utils

import android.content.Context
import com.ashaf.instanz.data.models.Template
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStream

object TemplateLoader {
    private val gson = Gson()
    
    suspend fun loadTemplateFromRaw(context: Context, resId: Int): Template? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(resId)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            
            Template(
                id = jsonObject.get("id").asString,
                name = jsonObject.get("name").asString,
                icon = jsonObject.get("icon").asString,
                version = jsonObject.get("version").asString,
                jsonData = jsonString,
                isCustom = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

