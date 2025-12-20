package com.ashaf.instanz.utils

import android.content.Context
import com.ashaf.instanz.data.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    
    private val gson = Gson()
    
    suspend fun createBackup(): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "backup_$timestamp.zip"
            val backupDir = File(context.filesDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            val backupFile = File(backupDir, backupFileName)
            
            // Create ZIP file
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Export database
                val dbFile = context.getDatabasePath("ashaf_database")
                if (dbFile.exists()) {
                    addFileToZip(zipOut, dbFile, "database.db")
                }
                
                // Export images
                val imagesDir = File(context.filesDir, "job_images")
                if (imagesDir.exists()) {
                    addDirectoryToZip(zipOut, imagesDir, "job_images")
                }
                
                // Export metadata
                val metadata = createBackupMetadata()
                val metadataJson = gson.toJson(metadata)
                zipOut.putNextEntry(ZipEntry("metadata.json"))
                zipOut.write(metadataJson.toByteArray())
                zipOut.closeEntry()
            }
            
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun restoreBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // Close database
            database.close()
            
            // Extract ZIP file
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "database.db" -> {
                            val dbFile = context.getDatabasePath("ashaf_database")
                            extractFile(zipIn, dbFile)
                        }
                        entry.name.startsWith("job_images/") -> {
                            val fileName = entry.name.substringAfter("job_images/")
                            if (fileName.isNotEmpty()) {
                                val imagesDir = File(context.filesDir, "job_images").apply {
                                    if (!exists()) mkdirs()
                                }
                                val imageFile = File(imagesDir, fileName)
                                extractFile(zipIn, imageFile)
                            }
                        }
                        entry.name == "metadata.json" -> {
                            // Read metadata (for validation)
                            val metadata = zipIn.readBytes().toString(Charsets.UTF_8)
                            // TODO: Validate metadata
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            zipOut.putNextEntry(ZipEntry(entryName))
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
    
    private fun addDirectoryToZip(zipOut: ZipOutputStream, directory: File, basePath: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                val entryName = "$basePath/${file.name}"
                addFileToZip(zipOut, file, entryName)
            } else if (file.isDirectory) {
                addDirectoryToZip(zipOut, file, "$basePath/${file.name}")
            }
        }
    }
    
    private fun extractFile(zipIn: ZipInputStream, destinationFile: File) {
        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { fos ->
            zipIn.copyTo(fos)
        }
    }
    
    private fun createBackupMetadata(): JsonObject {
        return JsonObject().apply {
            addProperty("version", "1.0.0")
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("device", android.os.Build.MODEL)
        }
    }
    
    suspend fun getBackupSize(): Long = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            
            // Database size
            val dbFile = context.getDatabasePath("ashaf_database")
            if (dbFile.exists()) {
                totalSize += dbFile.length()
            }
            
            // Images size
            val imagesDir = File(context.filesDir, "job_images")
            if (imagesDir.exists()) {
                totalSize += calculateDirectorySize(imagesDir)
            }
            
            totalSize
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isFile) {
                file.length()
            } else {
                calculateDirectorySize(file)
            }
        }
        return size
    }
}

