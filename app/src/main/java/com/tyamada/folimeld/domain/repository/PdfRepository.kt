package com.tyamada.folimeld.domain.repository

import android.net.Uri
import com.tyamada.folimeld.domain.model.PdfDetails
import com.tyamada.folimeld.domain.model.PdfMetadata
import com.tyamada.folimeld.domain.model.PdfPage
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    val documentState: Flow<PdfDocumentState>
    val isDirty: Flow<Boolean>
    val currentPath: Flow<String?>
    val isPasswordProtected: Flow<Boolean>
    val canUndo: Flow<Boolean>
    val canRedo: Flow<Boolean>

    suspend fun open(uri: Uri, password: String? = null): Result<Unit>
    suspend fun close()
    suspend fun save(uri: Uri): Result<Unit>
    suspend fun undo(): Result<Unit>
    suspend fun redo(): Result<Unit>
    suspend fun insertPdf(uri: Uri, afterIndex: Int?): Result<Unit>
    suspend fun insertBlankPage(afterIndices: List<Int>): Result<Unit>
    suspend fun deletePages(indices: List<Int>): Result<Unit>
    suspend fun reorderPage(oldIndex: Int, newIndex: Int): Result<Unit>
    suspend fun moveSelected(indices: List<Int>, direction: Int): Result<List<Int>>
    suspend fun rotatePages(indices: List<Int>, degrees: Int): Result<Unit>
    suspend fun updateMetadata(metadata: PdfMetadata): Result<Unit>
    suspend fun updateDetails(details: PdfDetails): Result<Unit>
    suspend fun setPassword(password: String?): Result<Unit>
}

sealed class PdfDocumentState {
    object Idle : PdfDocumentState()
    object Loading : PdfDocumentState()
    data class Loaded(val pages: List<PdfPage>, val metadata: PdfMetadata, val details: PdfDetails) : PdfDocumentState()
    data class Error(val message: String) : PdfDocumentState()
    object PasswordRequired : PdfDocumentState()
}
