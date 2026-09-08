package com.example.folimeld_android.di

import com.example.folimeld_android.data.repository.PdfRepositoryImpl
import com.example.folimeld_android.data.repository.SettingsRepositoryImpl
import com.example.folimeld_android.domain.repository.PdfRepository
import com.example.folimeld_android.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(
        pdfRepositoryImpl: PdfRepositoryImpl
    ): PdfRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
