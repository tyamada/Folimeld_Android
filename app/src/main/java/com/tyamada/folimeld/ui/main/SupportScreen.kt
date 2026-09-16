package com.tyamada.folimeld.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.tyamada.folimeld.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val isSupporter by viewModel.isSupporter.collectAsState()
    val price by viewModel.supportProductPrice.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support)) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.folimeld_supporter_maid),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )

            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isSupporter) Color(0xFFD64B75) else Color.Gray
            )
            
            Text(
                text = if (isSupporter) "Thank you for being a supporter!" else stringResource(R.string.support_thanks),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (isSupporter) "You have already supported the development. Your contribution is greatly appreciated!" else stringResource(R.string.support_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSupporter) {
                Button(
                    onClick = { activity?.let { viewModel.onSupportClick(it) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(price?.let { "${stringResource(R.string.support)} ($it)" } ?: stringResource(R.string.support))
                }
            }

            OutlinedButton(
                onClick = { viewModel.onRefreshClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.support_refresh))
            }
            
            TextButton(onClick = onNavigateBack) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
