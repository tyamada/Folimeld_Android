package com.tyamada.folimeld.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import android.os.Build
import com.tyamada.folimeld.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val packageInfo = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Exception) {
        null
    }

    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong()
    } ?: 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.version_info)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version $versionName ($versionCode)",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Copyright (C) 2026 Takuma Yamada",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.licenses),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            LicenseItem("PdfBox-Android", "Apache License 2.0")
            LicenseItem("Hilt", "Apache License 2.0")
            LicenseItem("Jetpack Compose", "Apache License 2.0")
            LicenseItem("Kotlin", "Apache License 2.0")
        }
    }
}

@Composable
fun LicenseItem(name: String, license: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(text = license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
