/*
 * Folimeld for Android
 * Copyright (C) 2026 Takuma Yamada
 * Licensed under the GNU Affero General Public License v3.0
 */
package com.tyamada.folimeld.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tyamada.folimeld.domain.model.PageLayout
import com.tyamada.folimeld.domain.model.PdfDetails
import com.tyamada.folimeld.domain.model.PdfMetadata
import com.tyamada.folimeld.domain.model.PdfPage
import com.tyamada.folimeld.domain.repository.PdfDocumentState
import com.tyamada.folimeld.domain.repository.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfRepository {

    private val TAG = "PdfRepositoryImpl"
    private val mutex = Mutex()

    private val _documentState = MutableStateFlow<PdfDocumentState>(PdfDocumentState.Idle)
    override val documentState = _documentState.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    override val isDirty = _isDirty.asStateFlow()

    private val _currentPath = MutableStateFlow<String?>(null)
    override val currentPath = _currentPath.asStateFlow()

    private val _isPasswordProtected = MutableStateFlow(false)
    override val isPasswordProtected = _isPasswordProtected.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    override val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    override val canRedo = _canRedo.asStateFlow()

    private var currentDocument: PDDocument? = null
    private var currentUri: Uri? = null
    private var currentPassword: String? = null

    // History management
    private val history = mutableListOf<File>()
    private var historyIndex = -1
    private val MAX_HISTORY = 20

    override suspend fun open(uri: Uri, password: String?): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Opening PDF: $uri")
                _documentState.value = PdfDocumentState.Loading
                
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext Result.failure(Exception("Failed to open input stream"))
                val bytes = inputStream.readBytes()
                inputStream.close()

                val doc = try {
                    if (password != null) {
                        PDDocument.load(bytes, password)
                    } else {
                        PDDocument.load(bytes)
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("password", ignoreCase = true) == true || e.message?.contains("encrypted", ignoreCase = true) == true) {
                        Log.d(TAG, "Password required for $uri")
                        _documentState.value = PdfDocumentState.PasswordRequired
                        return@withContext Result.success(Unit)
                    }
                    throw e
                }

                currentDocument?.close()
                currentDocument = doc
                currentUri = uri
                currentPassword = password
                _isPasswordProtected.value = doc.isEncrypted
                _currentPath.value = uri.path
                _isDirty.value = false

                clearHistoryInternal()
                saveHistoryInternal(false) // Initial state, not dirty
                
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening PDF", e)
                _documentState.value = PdfDocumentState.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    private suspend fun saveHistoryInternal(setDirty: Boolean = true) {
        val doc = currentDocument ?: return
        
        // Remove redo steps
        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1).delete()
        }

        // Create new history file
        val tempFile = File(context.cacheDir, "history_${System.currentTimeMillis()}_${history.size}.pdf")
        doc.save(tempFile)
        history.add(tempFile)
        historyIndex++

        // Limit size
        if (history.size > MAX_HISTORY) {
            history.removeAt(0).delete()
            historyIndex--
        }

        if (setDirty) _isDirty.value = true
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    private fun clearHistoryInternal() {
        history.forEach { it.delete() }
        history.clear()
        historyIndex = -1
        updateUndoRedoStates()
    }

    override suspend fun undo(): Result<Unit> = mutex.withLock {
        if (historyIndex <= 0) return@withLock Result.failure(Exception("Cannot undo"))
        
        historyIndex--
        loadFromHistoryInternal()
    }

    override suspend fun redo(): Result<Unit> = mutex.withLock {
        if (historyIndex >= history.size - 1) return@withLock Result.failure(Exception("Cannot redo"))
        
        historyIndex++
        loadFromHistoryInternal()
    }

    private suspend fun loadFromHistoryInternal(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = history[historyIndex]
            val doc = if (currentPassword != null) {
                PDDocument.load(file, currentPassword)
            } else {
                PDDocument.load(file)
            }
            
            currentDocument?.close()
            currentDocument = doc
            _isDirty.value = historyIndex > 0 // Dirty if not at the initial state
            
            refreshStateInternal()
            updateUndoRedoStates()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from history", e)
            Result.failure(e)
        }
    }

    private suspend fun refreshStateInternal() {
        val doc = currentDocument ?: return
        Log.d(TAG, "Refreshing state for doc with ${doc.numberOfPages} pages")
        val pages = mutableListOf<PdfPage>()
        
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
        try {
            doc.save(tempFile)

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 0.5f
                val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val pdPage = doc.getPage(i)
                pages.add(PdfPage(
                    index = i,
                    thumbnail = bitmap,
                    rotation = pdPage.rotation,
                    width = page.width.toFloat(),
                    height = page.height.toFloat()
                ))
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshStateInternal", e)
            _documentState.value = PdfDocumentState.Error("Failed to render thumbnails: ${e.message}")
            return
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        val info = doc.documentInformation
        val metadata = PdfMetadata(
            title = info.title ?: "",
            author = info.author ?: "",
            subject = info.subject ?: "",
            keywords = info.keywords ?: ""
        )

        val details = try {
            val layout = mapPageLayout(doc.documentCatalog.pageLayout?.name)
            PdfDetails(
                version = doc.version.toString(),
                pageLayout = layout,
                isCoverPage = layout == PageLayout.TwoColumnRight || layout == PageLayout.TwoPageRight,
                isRightToLeft = doc.documentCatalog.viewerPreferences?.getReadingDirection() == "R2L"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load some details", e)
            PdfDetails(version = doc.version.toString())
        }

        _documentState.value = PdfDocumentState.Loaded(pages, metadata, details)
        Log.d(TAG, "State refreshed successfully with ${pages.size} pages")
    }

    private fun mapPageLayout(name: String?): PageLayout {
        return when (name?.uppercase()) {
            "SINGLEPAGE", "SINGLE_PAGE" -> PageLayout.SinglePage
            "ONECOLUMN", "ONE_COLUMN" -> PageLayout.OneColumn
            "TWOCOLUMNLEFT", "TWO_COLUMN_LEFT" -> PageLayout.TwoColumnLeft
            "TWOCOLUMNRIGHT", "TWO_COLUMN_RIGHT" -> PageLayout.TwoColumnRight
            "TWOPAGELEFT", "TWO_PAGE_LEFT" -> PageLayout.TwoPageLeft
            "TWOPAGERIGHT", "TWO_PAGE_RIGHT" -> PageLayout.TwoPageRight
            else -> PageLayout.SinglePage
        }
    }

    override suspend fun close() = mutex.withLock {
        currentDocument?.close()
        currentDocument = null
        currentUri = null
        currentPassword = null
        _documentState.value = PdfDocumentState.Idle
        _isDirty.value = false
        _currentPath.value = null
        clearHistoryInternal()
    }

    override suspend fun save(uri: Uri): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving PDF to: $uri")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                
                if (currentPassword != null) {
                    val ap = AccessPermission()
                    val spp = StandardProtectionPolicy(UUID.randomUUID().toString(), currentPassword, ap)
                    spp.encryptionKeyLength = 256
                    doc.protect(spp)
                }

                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext Result.failure(Exception("Failed to open output stream"))
                doc.save(outputStream)
                outputStream.close()
                
                currentUri = uri
                _isDirty.value = false
                // Reset history base to current saved state
                clearHistoryInternal()
                saveHistoryInternal(false)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving PDF", e)
                Result.failure(e)
            }
        }
    }

    private fun PDDocument.rebuildWithOrder(order: List<Int>): PDDocument {
        val newDoc = PDDocument()
        newDoc.documentInformation.title = this.documentInformation.title
        newDoc.documentInformation.author = this.documentInformation.author
        newDoc.documentInformation.subject = this.documentInformation.subject
        newDoc.documentInformation.keywords = this.documentInformation.keywords
        
        for (i in order) {
            newDoc.importPage(this.getPage(i))
        }
        return newDoc
    }

    override suspend fun insertPdf(uri: Uri, afterIndex: Int?): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting PDF from $uri after index $afterIndex")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext Result.failure(Exception("Failed to open input stream"))
                val sourceDoc = PDDocument.load(inputStream)
                
                val totalPages = doc.numberOfPages
                val insertAt = if (afterIndex == null) totalPages else (afterIndex + 1).coerceIn(0, totalPages)
                
                val newDoc = PDDocument()
                newDoc.documentInformation.title = doc.documentInformation.title
                
                for (i in 0 until insertAt) {
                    newDoc.importPage(doc.getPage(i))
                }
                for (i in 0 until sourceDoc.numberOfPages) {
                    newDoc.importPage(sourceDoc.getPage(i))
                }
                for (i in insertAt until totalPages) {
                    newDoc.importPage(doc.getPage(i))
                }
                
                sourceDoc.close()
                inputStream.close()
                currentDocument?.close()
                currentDocument = newDoc
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting PDF", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun insertBlankPage(afterIndices: List<Int>): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting blank pages after indices $afterIndices")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                val sortedAfter = afterIndices.distinct().sorted()
                
                val newDoc = PDDocument()
                newDoc.documentInformation.title = doc.documentInformation.title
                
                for (i in 0 until doc.numberOfPages) {
                    newDoc.importPage(doc.getPage(i))
                    if (sortedAfter.contains(i)) {
                        val rect = doc.getPage(i).mediaBox
                        newDoc.addPage(PDPage(rect))
                    }
                }
                
                currentDocument?.close()
                currentDocument = newDoc
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting blank page", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deletePages(indices: List<Int>): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting pages: $indices")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                if (indices.size >= doc.numberOfPages) return@withContext Result.failure(Exception("At least one page must remain"))
                
                val toKeep = (0 until doc.numberOfPages).filter { it !in indices }
                val newDoc = doc.rebuildWithOrder(toKeep)
                
                currentDocument?.close()
                currentDocument = newDoc
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting pages", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun reorderPage(oldIndex: Int, newIndex: Int): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Reordering page from $oldIndex to $newIndex")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                val order = (0 until doc.numberOfPages).toMutableList()
                val item = order.removeAt(oldIndex)
                order.add(newIndex.coerceIn(0, order.size), item)
                
                val newDoc = doc.rebuildWithOrder(order)
                currentDocument?.close()
                currentDocument = newDoc
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error reordering page", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun moveSelected(indices: List<Int>, direction: Int): Result<List<Int>> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Moving selected pages $indices in direction $direction")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                val selected = indices.toMutableSet()
                val totalPages = doc.numberOfPages
                val order = (0 until totalPages).toMutableList()
                
                val scan = if (direction < 0) 1 until totalPages else (totalPages - 2) downTo 0
                var changed = false
                
                for (i in scan) {
                    val neighbor = i + direction
                    if (i in selected && neighbor !in selected) {
                        val temp = order[i]
                        order[i] = order[neighbor]
                        order[neighbor] = temp
                        
                        selected.remove(i)
                        selected.add(neighbor)
                        changed = true
                    }
                }
                
                if (changed) {
                    val newDoc = doc.rebuildWithOrder(order)
                    currentDocument?.close()
                    currentDocument = newDoc
                    saveHistoryInternal()
                    refreshStateInternal()
                }
                Result.success(selected.toList().sorted())
            } catch (e: Exception) {
                Log.e(TAG, "Error moving selected pages", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun rotatePages(indices: List<Int>, degrees: Int): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Rotating pages $indices by $degrees degrees")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                for (idx in indices) {
                    if (idx >= 0 && idx < doc.numberOfPages) {
                        val page = doc.getPage(idx)
                        val currentRotation = page.rotation
                        page.rotation = (currentRotation + degrees + 360) % 360
                    }
                }
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error rotating pages", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateMetadata(metadata: PdfMetadata): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating metadata: $metadata")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                val info = doc.documentInformation
                info.title = metadata.title
                info.author = metadata.author
                info.subject = metadata.subject
                info.keywords = metadata.keywords
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating metadata", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDetails(details: PdfDetails): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating details: $details")
                val doc = currentDocument ?: return@withContext Result.failure(Exception("No document open"))
                
                doc.version = details.version.toFloatOrNull() ?: doc.version
                
                val catalog = doc.documentCatalog
                catalog.pageLayout = when (details.pageLayout) {
                    PageLayout.SinglePage -> com.tom_roush.pdfbox.pdmodel.PageLayout.SINGLE_PAGE
                    PageLayout.OneColumn -> com.tom_roush.pdfbox.pdmodel.PageLayout.ONE_COLUMN
                    PageLayout.TwoColumnLeft -> com.tom_roush.pdfbox.pdmodel.PageLayout.TWO_COLUMN_LEFT
                    PageLayout.TwoColumnRight -> com.tom_roush.pdfbox.pdmodel.PageLayout.TWO_COLUMN_RIGHT
                    PageLayout.TwoPageLeft -> com.tom_roush.pdfbox.pdmodel.PageLayout.TWO_PAGE_LEFT
                    PageLayout.TwoPageRight -> com.tom_roush.pdfbox.pdmodel.PageLayout.TWO_PAGE_RIGHT
                }
                
                val prefs = catalog.viewerPreferences ?: PDViewerPreferences(doc.documentCatalog.cosObject).also {
                    catalog.viewerPreferences = it
                }
                prefs.setReadingDirection(if (details.isRightToLeft) "R2L" else "L2R")
                
                saveHistoryInternal()
                refreshStateInternal()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating details", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun setPassword(password: String?): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            currentPassword = password
            _isPasswordProtected.value = password != null
            saveHistoryInternal()
            Result.success(Unit)
        }
    }
}
