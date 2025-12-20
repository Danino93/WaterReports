package com.ashaf.instanz.ui.di

import androidx.compose.runtime.compositionLocalOf

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}

