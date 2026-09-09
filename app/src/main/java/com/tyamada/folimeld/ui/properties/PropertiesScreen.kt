package com.tyamada.folimeld.ui.properties

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tyamada.folimeld.R
import com.tyamada.folimeld.domain.model.PageLayout
import com.tyamada.folimeld.domain.model.PdfDetails
import com.tyamada.folimeld.domain.model.PdfMetadata
import com.tyamada.folimeld.domain.model.ThumbnailSize
import com.tyamada.folimeld.domain.repository.PdfDocumentState
import com.tyamada.folimeld.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val documentState by viewModel.documentState.collectAsStateWithLifecycle()
    val currentThumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    
    val loadedState = documentState as? PdfDocumentState.Loaded ?: return

    var title by remember { mutableStateOf(loadedState.metadata.title) }
    var author by remember { mutableStateOf(loadedState.metadata.author) }
    var subject by remember { mutableStateOf(loadedState.metadata.subject) }
    var keywords by remember { mutableStateOf(loadedState.metadata.keywords) }

    var version by remember { mutableStateOf(loadedState.details.version) }
    var pageLayout by remember { mutableStateOf(loadedState.details.pageLayout) }
    var isCoverPage by remember { mutableStateOf(loadedState.details.isCoverPage) }
    var isRightToLeft by remember { mutableStateOf(loadedState.details.isRightToLeft) }

    var layoutExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.properties)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateMetadata(PdfMetadata(title, author, subject, keywords))
                        viewModel.updateDetails(PdfDetails(version, pageLayout, isCoverPage, isRightToLeft))
                        onNavigateBack()
                    }) {
                        Text(stringResource(R.string.apply))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.summary), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text(stringResource(R.string.author)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.subject)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = keywords, onValueChange = { keywords = it }, label = { Text(stringResource(R.string.keywords)) }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text(stringResource(R.string.details), style = MaterialTheme.typography.titleMedium)
            
            Text(stringResource(R.string.image_size))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThumbnailSize.entries.forEach { size ->
                    FilterChip(
                        selected = currentThumbnailSize == size,
                        onClick = { viewModel.setThumbnailSize(size) },
                        label = { Text(size.name) }
                    )
                }
            }

            Text(stringResource(R.string.language))
            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (currentLanguage) {
                        "en" -> "English"
                        "ja" -> "日本語"
                        else -> "System Default"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false }
                ) {
                    listOf(null to "System Default", "en" to "English", "ja" to "日本語").forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setLanguage(code)
                                langExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text(stringResource(R.string.pdf_version)) }, modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.page_layout), style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = layoutExpanded,
                onExpandedChange = { layoutExpanded = it }
            ) {
                OutlinedTextField(
                    value = pageLayout.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = layoutExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = layoutExpanded,
                    onDismissRequest = { layoutExpanded = false }
                ) {
                    PageLayout.entries.forEach { layout ->
                        DropdownMenuItem(
                            text = { Text(layout.name) },
                            onClick = {
                                pageLayout = layout
                                layoutExpanded = false
                            }
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCoverPage, onCheckedChange = { isCoverPage = it })
                Text(stringResource(R.string.cover_page))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRightToLeft, onCheckedChange = { isRightToLeft = it })
                Text(stringResource(R.string.scroll_direction))
            }

            // Dynamic Description Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.page_display), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val isSingle = pageLayout == PageLayout.SinglePage || pageLayout == PageLayout.OneColumn
                    Text("• " + stringResource(if (isSingle) R.string.single_page_display else R.string.two_page_display))

                    val coverLayoutMap = mapOf(
                        PageLayout.TwoColumnLeft to PageLayout.TwoColumnRight,
                        PageLayout.TwoPageLeft to PageLayout.TwoPageRight
                    )
                    val coverLayout = if (isCoverPage) coverLayoutMap[pageLayout] else null

                    if (coverLayout != null || pageLayout == PageLayout.TwoColumnRight || pageLayout == PageLayout.TwoPageRight) {
                        Text("• " + stringResource(R.string.show_cover_page))
                    }
                    if (pageLayout == PageLayout.OneColumn || pageLayout == PageLayout.TwoColumnLeft || pageLayout == PageLayout.TwoColumnRight) {
                        Text("• " + stringResource(R.string.scrolling_enabled))
                    }

                    if (coverLayout != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.change_page_layout).replace("{layout}", coverLayout.name),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
