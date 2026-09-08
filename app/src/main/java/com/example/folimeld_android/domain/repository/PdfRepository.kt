package com.example.folimeld_android.domain.repository

import android.net.Uri
import com.example.folimeld_android.domain.model.PdfDetails
import com.example.folimeld_android.domain.model.PdfMetadata
import com.example.folimeld_android.domain.model.PdfPage
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    val documentState: Flow<PdfDocumentState>
    val isDirty: Flow<Boolean>
    val currentPath: Flow<String?>
    val isPasswordProtected: Flow<Boolean>

    suspend fun open(uri: Uri, password: String? = null): Result<Unit>
    suspend fun close()
    suspend fun save(uri: Uri): Result<Unit>
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
