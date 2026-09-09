package com.tyamada.folimeld.domain.repository

import com.tyamada.folimeld.domain.model.ThumbnailSize
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val thumbnailSize: Flow<ThumbnailSize>
    val language: Flow<String?>

    suspend fun setThumbnailSize(size: ThumbnailSize)
    suspend fun setLanguage(language: String?)
}
