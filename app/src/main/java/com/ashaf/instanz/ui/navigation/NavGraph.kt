package com.ashaf.instanz.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.ashaf.instanz.data.datastore.PreferencesManager
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.screens.camera.CameraScreen
import com.ashaf.instanz.ui.screens.editor.EditorScreen
import com.ashaf.instanz.ui.screens.home.HomeScreen
import com.ashaf.instanz.ui.screens.job.JobDetailsScreen
import com.ashaf.instanz.ui.screens.preview.PreviewScreen
import com.ashaf.instanz.ui.screens.settings.SettingsScreen
import com.ashaf.instanz.ui.screens.template.TemplateSelectionScreen
import com.ashaf.instanz.ui.screens.template.TemplateEditorScreen
import com.ashaf.instanz.ui.screens.template.TemplateListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TemplateSelection : Screen("template_selection")
    object TemplateList : Screen("template_list")
    object TemplateEditor : Screen("template_editor/{templateId}?jobId={jobId}") {
        fun createRoute(templateId: String, jobId: Long? = null) = 
            if (jobId != null) "template_editor/$templateId?jobId=$jobId"
            else "template_editor/$templateId"
    }
    object JobDetails : Screen("job_details/{templateId}") {
        fun createRoute(templateId: String) = "job_details/$templateId"
    }
    object Editor : Screen("editor/{jobId}") {
        fun createRoute(jobId: Long) = "editor/$jobId"
    }
    object Preview : Screen("preview/{jobId}") {
        fun createRoute(jobId: Long) = "preview/$jobId"
    }
    object Camera : Screen("camera/{jobId}/{sectionId}/{fieldId}") {
        fun createRoute(jobId: Long, sectionId: String, fieldId: String) = 
            "camera/$jobId/$sectionId/$fieldId"
    }
    object Settings : Screen("settings")
    object JobSettings : Screen("job_settings/{jobId}") {
        fun createRoute(jobId: Long) = "job_settings/$jobId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNewJobClick = {
                    navController.navigate(Screen.TemplateSelection.route)
                },
                onJobClick = { jobId ->
                    navController.navigate(Screen.Editor.createRoute(jobId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onTemplateListClick = {
                    navController.navigate(Screen.TemplateList.route)
                }
            )
        }
        
        composable(Screen.TemplateSelection.route) {
            TemplateSelectionScreen(
                onTemplateSelected = { templateId, jobId ->
                    // Navigate directly to editor with the created job
                    navController.navigate(Screen.Editor.createRoute(jobId)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.JobDetails.route,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            JobDetailsScreen(
                templateId = templateId,
                onJobCreated = { jobId ->
                    navController.navigate(Screen.Editor.createRoute(jobId)) {
                        // Clear back stack up to home
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("jobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L
            EditorScreen(
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                },
                onPreviewClick = { id ->
                    navController.navigate(Screen.Preview.createRoute(id))
                },
                onCameraClick = { jId, sectionId, fieldId ->
                    navController.navigate(Screen.Camera.createRoute(jId, sectionId, fieldId))
                },
                onTemplateEditClick = { templateId ->
                    navController.navigate(Screen.TemplateEditor.createRoute(templateId, jobId))
                },
                onJobSettingsClick = { jId ->
                    navController.navigate(Screen.JobSettings.createRoute(jId))
                }
            )
        }
        
        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.LongType },
                navArgument("sectionId") { type = NavType.StringType },
                navArgument("fieldId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L
            val sectionId = backStackEntry.arguments?.getString("sectionId") ?: ""
            val fieldId = backStackEntry.arguments?.getString("fieldId") ?: ""
            CameraScreen(
                jobId = jobId,
                sectionId = sectionId,
                fieldId = fieldId,
                onImageCaptured = { filePath ->
                    // Navigate back to editor after capturing
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Preview.route,
            arguments = listOf(navArgument("jobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L
            PreviewScreen(
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                },
                onExportPdfClick = { id ->
                    // TODO: Export PDF functionality
                },
                onShareClick = { id ->
                    // TODO: Share functionality
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.JobSettings.route,
            arguments = listOf(navArgument("jobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L
            val appContainer = LocalAppContainer.current
            
            // Load current job settings
            var currentSettings by remember { mutableStateOf(com.ashaf.instanz.data.models.JobSettings.default()) }
            
            LaunchedEffect(jobId) {
                val job = appContainer.jobRepository.getJobById(jobId)
                currentSettings = job?.getJobSettings() ?: com.ashaf.instanz.data.models.JobSettings.default()
            }
            
            com.ashaf.instanz.ui.screens.job.JobSettingsScreen(
                jobId = jobId,
                currentSettings = currentSettings,
                onSaveSettings = { settings ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val job = appContainer.jobRepository.getJobById(jobId)
                        job?.let {
                            val gson = com.google.gson.Gson()
                            val updatedJob = it.copy(
                                jobSettingsJson = gson.toJson(settings),
                                dateModified = System.currentTimeMillis()
                            )
                            appContainer.jobRepository.updateJob(updatedJob)
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TemplateList.route) {
            TemplateListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onTemplateClick = { templateId ->
                    navController.navigate(Screen.TemplateEditor.createRoute(templateId))
                }
            )
        }
        
        composable(
            route = Screen.TemplateEditor.route,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType },
                navArgument("jobId") { 
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "water_damage_v1"
            val jobIdArg = backStackEntry.arguments?.getLong("jobId") ?: 0L
            val jobId = if (jobIdArg == 0L) null else jobIdArg
            TemplateEditorScreen(
                templateId = templateId,
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
