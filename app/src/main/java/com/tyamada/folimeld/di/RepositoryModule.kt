package com.tyamada.folimeld.di

import com.tyamada.folimeld.data.repository.PdfRepositoryImpl
import com.tyamada.folimeld.data.repository.SettingsRepositoryImpl
import com.tyamada.folimeld.domain.repository.PdfRepository
import com.tyamada.folimeld.domain.repository.SettingsRepository
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
