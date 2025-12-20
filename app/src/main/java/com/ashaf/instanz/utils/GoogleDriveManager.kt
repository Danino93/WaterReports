package com.ashaf.instanz.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveManager(private val context: Context) {
    
    companion object {
        const val REQUEST_CODE_SIGN_IN = 1001
        private const val APP_FOLDER_NAME = "WaterDamageReports"
    }
    
    private var driveService: Drive? = null
    
    fun getSignInIntent(): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        val client = GoogleSignIn.getClient(context, signInOptions)
        return client.signInIntent
    }
    
    fun initializeDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Water Damage Reports")
            .build()
    }
    
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }
    
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    suspend fun signOut() = withContext(Dispatchers.IO) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        val client = GoogleSignIn.getClient(context, signInOptions)
        client.signOut()
        driveService = null
    }
    
    suspend fun uploadFile(
        localFile: java.io.File,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null
            
            // Get or create app folder
            val folderId = getOrCreateAppFolder()
            
            // Create file metadata
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }
            
            // Upload file
            val mediaContent = com.google.api.client.http.FileContent(mimeType, localFile)
            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute()
            
            file.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun downloadFile(fileId: String, destinationFile: java.io.File): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext false
                
                val outputStream = FileOutputStream(destinationFile)
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    
    suspend fun listFilesInAppFolder(): List<File> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext emptyList()
            val folderId = getOrCreateAppFolder()
            
            val result: FileList = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime, modifiedTime, size)")
                .execute()
            
            result.files ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext false
            drive.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun getOrCreateAppFolder(): String {
        val drive = driveService ?: throw IllegalStateException("Drive service not initialized")
        
        // Search for existing folder
        val result: FileList = drive.files().list()
            .setQ("name='$APP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        
        val existingFolder = result.files?.firstOrNull()
        if (existingFolder != null) {
            return existingFolder.id
        }
        
        // Create new folder
        val folderMetadata = File().apply {
            name = APP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        
        val folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()
        
        return folder.id
    }
}

