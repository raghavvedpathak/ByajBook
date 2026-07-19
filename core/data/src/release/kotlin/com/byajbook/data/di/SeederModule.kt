package com.byajbook.data.di

import com.byajbook.data.debug.DatabaseSeeder
import com.byajbook.data.debug.NoOpDatabaseSeeder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SeederModule {
    @Binds
    @Singleton
    abstract fun bindSeeder(impl: NoOpDatabaseSeeder): DatabaseSeeder
}
