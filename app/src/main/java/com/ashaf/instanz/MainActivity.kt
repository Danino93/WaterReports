package com.ashaf.instanz

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.ashaf.instanz.ui.di.AppContainer
import com.ashaf.instanz.ui.di.LocalAppContainer
import com.ashaf.instanz.ui.navigation.NavGraph
import com.ashaf.instanz.ui.theme.WaterDamageReportsTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set Hebrew locale explicitly
        val locale = Locale("he", "IL")
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = android.os.LocaleList(locale)
            config.setLocales(locales)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Allow keyboard to show
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        setContent {
            WaterDamageReportsTheme {
                val appContainer = AppContainer.create(this@MainActivity)
                // RTL Support for Hebrew
                CompositionLocalProvider(
                    LocalAppContainer provides appContainer,
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}
