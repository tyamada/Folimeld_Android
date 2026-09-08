/*
 * Folimeld for Android
 * Copyright (C) 2026 Takuma Yamada
 * Licensed under the GNU Affero General Public License v3.0
 */
package com.example.folimeld_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.folimeld_android.domain.repository.SettingsRepository
import com.example.folimeld_android.ui.main.AboutScreen
import com.example.folimeld_android.ui.main.MainScreen
import com.example.folimeld_android.ui.main.MainViewModel
import com.example.folimeld_android.ui.main.SupportScreen
import com.example.folimeld_android.ui.properties.PropertiesScreen
import com.example.folimeld_android.ui.theme.Folimeld_AndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved locale before anything else
        val savedLang = runBlocking { settingsRepository.language.first() }
        val appLocale: LocaleListCompat = if (savedLang.isNullOrEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(savedLang)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
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
