package com.byajbook.pdf.di

import com.byajbook.domain.service.PdfService
import com.byajbook.pdf.PdfGeneratorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {

    @Binds
    @Singleton
    abstract fun bindPdfService(impl: PdfGeneratorImpl): PdfService
}
