package com.tyamada.folimeld.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tyamada.folimeld.R
import com.tyamada.folimeld.domain.model.PdfPage
import com.tyamada.folimeld.domain.model.ThumbnailSize
import com.tyamada.folimeld.domain.repository.PdfDocumentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToProperties: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val documentState by viewModel.documentState.collectAsStateWithLifecycle()
    val selectedIndices by viewModel.selectedIndices.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isPasswordProtected by viewModel.isPasswordProtected.collectAsStateWithLifecycle()

    var lastAttemptedUri by remember { mutableStateOf<Uri?>(null) }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            lastAttemptedUri = it
            viewModel.openPdf(it) 
        }
    }

    val insertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.insertPdf(it) }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { viewModel.savePdf(it) }
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showRemovePasswordConfirm by remember { mutableStateOf(false) }

    if (showDiscardDialog != null) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = null },
            title = { Text(stringResource(R.string.unsaved_title)) },
            text = { Text(stringResource(R.string.unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog?.invoke()
                    showDiscardDialog = null
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            title = { Text(stringResource(R.string.image_size)) },
            text = {
                Column {
                    ThumbnailSize.entries.forEach { size ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThumbnailSize(size)
                                    showSizeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = thumbnailSize == size,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${size.name} (${size.dp} dp)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSizeDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showSetPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPasswordDialog = false },
            title = { Text(stringResource(R.string.set_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorText != null) {
                        Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword.isEmpty()) {
                        errorText = "Password cannot be empty"
                    } else if (newPassword != confirmPassword) {
                        errorText = "Passwords do not match"
                    } else {
                        viewModel.setPassword(newPassword)
                        showSetPasswordDialog = false
                    }
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showRemovePasswordConfirm) {
        AlertDialog(
            onDismissRequest = { showRemovePasswordConfirm = false },
            title = { Text(stringResource(R.string.remove_password)) },
            text = { Text(stringResource(R.string.remove_password_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setPassword(null)
                    showRemovePasswordConfirm = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePasswordConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    LaunchedEffect(documentState) {
        if (documentState is PdfDocumentState.PasswordRequired) {
            showPasswordDialog = true
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(stringResource(R.string.password_required)) },
            text = {
                TextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(stringResource(R.string.enter_password)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    lastAttemptedUri?.let { viewModel.openPdf(it, passwordInput) }
                    showPasswordDialog = false
                }) { Text("OK") }
            }
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                val isCtrl = event.isCtrlPressed
                val isShift = event.isShiftPressed
                val isAlt = event.isAltPressed

                when {
                    // Ctrl+O: Open
                    isCtrl && event.key == Key.O -> {
                        if (isDirty) {
                            showDiscardDialog = { openLauncher.launch(arrayOf("application/pdf")) }
                        } else {
                            openLauncher.launch(arrayOf("application/pdf"))
                        }
                        true
                    }
                    // Ctrl+S: Save
                    isCtrl && event.key == Key.S -> {
                        if (documentState is PdfDocumentState.Loaded && !isProcessing) {
                            saveLauncher.launch("document.pdf")
                        }
                        true
                    }
                    // Delete: Delete Selected
                    event.key == Key.Delete || event.key == Key.Backspace -> {
                        if (selectedIndices.isNotEmpty() && !isProcessing) {
                            viewModel.deleteSelected()
                        }
                        true
                    }
                    // Ctrl+I: Insert PDF
                    isCtrl && event.key == Key.I && !isShift -> {
                        if (documentState is PdfDocumentState.Loaded && !isProcessing) {
                            insertLauncher.launch(arrayOf("application/pdf"))
                        }
                        true
                    }
                    // Ctrl+Shift+I: Insert Blank
                    isCtrl && isShift && event.key == Key.I -> {
                        if (documentState is PdfDocumentState.Loaded && !isProcessing) {
                            viewModel.insertBlankPage()
                        }
                        true
                    }
                    // Alt+Up: Move Up
                    isAlt && event.key == Key.DirectionUp -> {
                        if (selectedIndices.isNotEmpty() && !isProcessing) {
                            viewModel.moveSelected(-1)
                        }
                        true
                    }
                    // Alt+Down: Move Down
                    isAlt && event.key == Key.DirectionDown -> {
                        if (selectedIndices.isNotEmpty() && !isProcessing) {
                            viewModel.moveSelected(1)
                        }
                        true
                    }
                    // Ctrl+Left: Rotate Left
                    isCtrl && event.key == Key.DirectionLeft -> {
                        if (selectedIndices.isNotEmpty() && !isProcessing) {
                            viewModel.rotateSelected(-90)
                        }
                        true
                    }
                    // Ctrl+Right: Rotate Right
                    isCtrl && event.key == Key.DirectionRight -> {
                        if (selectedIndices.isNotEmpty() && !isProcessing) {
                            viewModel.rotateSelected(90)
                        }
                        true
                    }
                    else -> false
                }
            } else false
        },
        topBar = {
            TopAppBar(
                title = { Text("${stringResource(R.string.app_name)} ${if (isDirty) "*" else ""}") },
                actions = {
                    IconButton(onClick = { 
                        if (isDirty) {
                            showDiscardDialog = { openLauncher.launch(arrayOf("application/pdf")) }
                        } else {
                            openLauncher.launch(arrayOf("application/pdf"))
                        }
                    }, enabled = !isProcessing) {
                        Icon(Icons.Default.FileOpen, contentDescription = stringResource(R.string.open))
                    }
                    IconButton(
                        onClick = { saveLauncher.launch("document.pdf") },
                        enabled = documentState is PdfDocumentState.Loaded && !isProcessing
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, enabled = !isProcessing) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.properties)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProperties()
                                },
                                enabled = documentState is PdfDocumentState.Loaded,
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.set_password)) },
                                onClick = {
                                    showMenu = false
                                    showSetPasswordDialog = true
                                },
                                enabled = documentState is PdfDocumentState.Loaded,
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_password)) },
                                onClick = {
                                    showMenu = false
                                    showRemovePasswordConfirm = true
                                },
                                enabled = documentState is PdfDocumentState.Loaded && isPasswordProtected,
                                leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.image_size)) },
                                onClick = {
                                    showMenu = false
                                    showSizeDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.PhotoSizeSelectLarge, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.version_info)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.support)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSupport()
                                },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD64B75)) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (documentState is PdfDocumentState.Loaded) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { insertLauncher.launch(arrayOf("application/pdf")) }, enabled = !isProcessing) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.insert))
                        }
                        IconButton(onClick = { viewModel.insertBlankPage() }, enabled = !isProcessing) {
                            Icon(Icons.Default.AddBox, contentDescription = stringResource(R.string.insert_blank))
                        }
                        VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(
                            onClick = { viewModel.moveSelected(-1) },
                            enabled = selectedIndices.isNotEmpty() && !isProcessing
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.move_up))
                        }
                        IconButton(
                            onClick = { viewModel.moveSelected(1) },
                            enabled = selectedIndices.isNotEmpty() && !isProcessing
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.move_down))
                        }
                        VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(
                            onClick = { viewModel.rotateSelected(-90) },
                            enabled = selectedIndices.isNotEmpty() && !isProcessing
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = stringResource(R.string.rotate_left))
                        }
                        IconButton(
                            onClick = { viewModel.rotateSelected(90) },
                            enabled = selectedIndices.isNotEmpty() && !isProcessing
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = stringResource(R.string.rotate_right))
                        }
                    },
                    floatingActionButton = {
                        if (selectedIndices.isNotEmpty()) {
                            FloatingActionButton(
                                onClick = { if (!isProcessing) viewModel.deleteSelected() },
                                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                                contentColor = MaterialTheme.colorScheme.error
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = documentState) {
                is PdfDocumentState.Idle -> Text(stringResource(R.string.untitled))
                is PdfDocumentState.Loading -> CircularProgressIndicator()
                is PdfDocumentState.Loaded -> {
                    PageGrid(
                        pages = state.pages,
                        selectedIndices = selectedIndices,
                        thumbnailSize = thumbnailSize,
                        onPageClick = { if (!isProcessing) viewModel.toggleSelection(it) }
                    )
                }
                is PdfDocumentState.Error -> Text(stringResource(R.string.error) + ": ${state.message}")
                is PdfDocumentState.PasswordRequired -> Text(stringResource(R.string.password_required))
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PageGrid(
    pages: List<PdfPage>,
    selectedIndices: Set<Int>,
    thumbnailSize: ThumbnailSize,
    onPageClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = thumbnailSize.dp.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pages, key = { it.index }) { page ->
            PageItem(
                page = page,
                isSelected = selectedIndices.contains(page.index),
                onClick = { onPageClick(page.index) }
            )
        }
    }
}

@Composable
fun PageItem(
    page: PdfPage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 4.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        page.thumbnail?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.page, page.index + 1),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(it.width.toFloat() / it.height.toFloat()),
                contentScale = ContentScale.Fit
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .background(Color.Gray)
        )
        Text(
            text = stringResource(R.string.page, page.index + 1),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
