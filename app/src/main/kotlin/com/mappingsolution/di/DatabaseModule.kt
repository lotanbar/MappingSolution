package com.mappingsolution.di

import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.fs.RouteFileRepository
import com.mappingsolution.data.util.StorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStorageManager(): StorageManager = StorageManager()

    @Provides
    @Singleton
    fun provideGroupFileRepository(storageManager: StorageManager): GroupFileRepository =
        GroupFileRepository(storageManager)

    @Provides
    @Singleton
    fun providePoiFileRepository(storageManager: StorageManager): PoiFileRepository =
        PoiFileRepository(storageManager)

    @Provides
    @Singleton
    fun provideRouteFileRepository(storageManager: StorageManager): RouteFileRepository =
        RouteFileRepository(storageManager)
}
