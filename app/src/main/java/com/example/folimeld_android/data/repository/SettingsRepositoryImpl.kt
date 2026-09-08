package com.example.folimeld_android.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.folimeld_android.domain.model.ThumbnailSize
import com.example.folimeld_android.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val TAG = "SettingsRepositoryImpl"
    private val THUMBNAIL_SIZE_KEY = stringPreferencesKey("thumbnail_size")
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    override val thumbnailSize: Flow<ThumbnailSize> = context.dataStore.data.map { preferences ->
        val name = preferences[THUMBNAIL_SIZE_KEY] ?: ThumbnailSize.Medium.name
        try {
            ThumbnailSize.valueOf(name)
        } catch (e: Exception) {
            ThumbnailSize.Medium
        }
    }

    override val language: Flow<String?> = context.dataStore.data.map { preferences ->
        val lang = preferences[LANGUAGE_KEY]
        Log.d(TAG, "Retrieved language from DataStore: $lang")
        lang
    }

    override suspend fun setThumbnailSize(size: ThumbnailSize) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_SIZE_KEY] = size.name
        }
    }

    override suspend fun setLanguage(language: String?) {
        Log.d(TAG, "Saving language to DataStore: $language")
        context.dataStore.edit { preferences ->
            if (language == null) {
                preferences.remove(LANGUAGE_KEY)
            } else {
                preferences[LANGUAGE_KEY] = language
            }
        }
    }
}
