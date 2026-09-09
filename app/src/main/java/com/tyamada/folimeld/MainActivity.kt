/*
 * Folimeld for Android
 * Copyright (C) 2026 Takuma Yamada
 * Licensed under the GNU Affero General Public License v3.0
 */
package com.tyamada.folimeld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tyamada.folimeld.ui.main.AboutScreen
import com.tyamada.folimeld.ui.main.MainScreen
import com.tyamada.folimeld.ui.main.MainViewModel
import com.tyamada.folimeld.ui.main.SupportScreen
import com.tyamada.folimeld.ui.properties.PropertiesScreen
import com.tyamada.folimeld.ui.theme.Folimeld_AndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val currentLanguage by mainViewModel.language.collectAsState()

            // Update app locale when language preference changes
            LaunchedEffect(currentLanguage) {
                val appLocale: LocaleListCompat = if (currentLanguage.isNullOrEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(currentLanguage)
                }
                
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            Folimeld_AndroidTheme {
                FolimeldApp(mainViewModel)
            }
        }
    }
}

@Composable
fun FolimeldApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onNavigateToProperties = { navController.navigate("properties") },
                onNavigateToAbout = { navController.navigate("about") },
                onNavigateToSupport = { navController.navigate("support") }
            )
        }
        composable("properties") {
            PropertiesScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("support") {
            SupportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
