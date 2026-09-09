package com.tyamada.folimeld.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyamada.folimeld.domain.model.PdfDetails
import com.tyamada.folimeld.domain.model.PdfMetadata
import com.tyamada.folimeld.domain.model.ThumbnailSize
import com.tyamada.folimeld.domain.repository.PdfDocumentState
import com.tyamada.folimeld.domain.repository.PdfRepository
import com.tyamada.folimeld.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PdfRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val documentState: StateFlow<PdfDocumentState> = repository.documentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PdfDocumentState.Idle)

    val isDirty: StateFlow<Boolean> = repository.isDirty
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPasswordProtected: StateFlow<Boolean> = repository.isPasswordProtected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIndices: StateFlow<Set<Int>> = _selectedIndices.asStateFlow()

    val thumbnailSize: StateFlow<ThumbnailSize> = settingsRepository.thumbnailSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThumbnailSize.Medium)

    val language: StateFlow<String?> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setThumbnailSize(size: ThumbnailSize) {
        viewModelScope.launch {
            settingsRepository.setThumbnailSize(size)
        }
    }

    fun setLanguage(language: String?) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Requesting language change to: $language")
            settingsRepository.setLanguage(language)
        }
    }

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private fun <T> runOperation(block: suspend () -> Result<T>) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                block()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun openPdf(uri: Uri, password: String? = null) = runOperation {
        repository.open(uri, password).onSuccess {
            _selectedIndices.value = emptySet()
        }
    }

    fun savePdf(uri: Uri) = runOperation {
        repository.save(uri)
    }

    fun toggleSelection(index: Int) {
        _selectedIndices.value = if (_selectedIndices.value.contains(index)) {
            _selectedIndices.value - index
        } else {
            _selectedIndices.value + index
        }
    }

    fun deleteSelected() = runOperation {
        repository.deletePages(_selectedIndices.value.toList()).onSuccess {
            _selectedIndices.value = emptySet()
        }
    }

    fun rotateSelected(degrees: Int) = runOperation {
        repository.rotatePages(_selectedIndices.value.toList(), degrees)
    }

    fun moveSelected(direction: Int) = runOperation {
        repository.moveSelected(_selectedIndices.value.toList(), direction).onSuccess { newSelected ->
            _selectedIndices.value = newSelected.toSet()
        }
    }

    fun insertPdf(uri: Uri) = runOperation {
        val afterIndex = _selectedIndices.value.maxOrNull()
        repository.insertPdf(uri, afterIndex)
    }

    fun insertBlankPage() = runOperation {
        repository.insertBlankPage(_selectedIndices.value.toList())
    }

    fun updateMetadata(metadata: PdfMetadata) {
        viewModelScope.launch {
            repository.updateMetadata(metadata)
        }
    }

    fun updateDetails(details: PdfDetails) {
        viewModelScope.launch {
            repository.updateDetails(details)
        }
    }

    fun setPassword(password: String?) {
        viewModelScope.launch {
            repository.setPassword(password)
        }
    }

    fun close() {
        viewModelScope.launch {
            repository.close()
            _selectedIndices.value = emptySet()
        }
    }
}
