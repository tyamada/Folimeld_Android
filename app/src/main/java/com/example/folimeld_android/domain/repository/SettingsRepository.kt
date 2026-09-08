package com.example.folimeld_android.domain.repository

import com.example.folimeld_android.domain.model.ThumbnailSize
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val thumbnailSize: Flow<ThumbnailSize>
    val language: Flow<String?>

    suspend fun setThumbnailSize(size: ThumbnailSize)
    suspend fun setLanguage(language: String?)
}
