package com.ashaf.instanz.utils

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private const val LOG_FILE_NAME = "app_logs.txt"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_LOG_FILES = 3
    
    private var logFile: File? = null
    private var context: Context? = null
    
    fun initialize(ctx: Context) {
        context = ctx.applicationContext
        val logDir = File(ctx.filesDir, "logs").apply {
            if (!exists()) mkdirs()
        }
        logFile = File(logDir, LOG_FILE_NAME)
        
        // Rotate logs if file is too large
        logFile?.let { file ->
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                rotateLogs(logDir)
            }
        }
    }
    
    private fun rotateLogs(logDir: File) {
        // Delete oldest log file
        val oldLog3 = File(logDir, "$LOG_FILE_NAME.3")
        if (oldLog3.exists()) {
            oldLog3.delete()
        }
        
        // Rename existing logs
        val oldLog2 = File(logDir, "$LOG_FILE_NAME.2")
        if (oldLog2.exists()) {
            oldLog2.renameTo(oldLog3)
        }
        
        val oldLog1 = File(logDir, "$LOG_FILE_NAME.1")
        if (oldLog1.exists()) {
            oldLog1.renameTo(oldLog2)
        }
        
        // Rename current log
        logFile?.let { currentLog ->
            if (currentLog.exists()) {
                currentLog.renameTo(oldLog1)
            }
        }
    }
    
    private fun writeToFile(tag: String, level: String, message: String, throwable: Throwable? = null) {
        val ctx = context ?: return
        val file = logFile ?: return
        
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val threadName = Thread.currentThread().name
            
            FileWriter(file, true).use { writer ->
                PrintWriter(writer).use { pw ->
                    pw.println("[$timestamp] [$level] [$tag] [$threadName] $message")
                    throwable?.let {
                        it.printStackTrace(pw)
                    }
                    pw.flush()
                }
            }
        } catch (e: Exception) {
            // If we can't write to file, at least try to log to system log
            android.util.Log.e("FileLogger", "Failed to write to log file", e)
        }
    }
    
    fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
        writeToFile(tag, "DEBUG", message)
    }
    
    fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
        writeToFile(tag, "INFO", message)
    }
    
    fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
        writeToFile(tag, "WARN", message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, message, throwable)
        writeToFile(tag, "ERROR", message, throwable)
    }
    
    fun getLogFile(): File? {
        return logFile
    }
    
    fun getLogFileUri(context: Context): android.net.Uri? {
        return logFile?.let { file ->
            try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

