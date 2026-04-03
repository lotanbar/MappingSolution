package com.mappingsolution.di

import android.content.Context
import com.mappingsolution.data.db.AppDatabase
import com.mappingsolution.data.db.dao.GroupDao
import com.mappingsolution.data.db.dao.PoiDao
import com.mappingsolution.data.db.dao.RouteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStorageManager(): com.mappingsolution.data.util.StorageManager =
        com.mappingsolution.data.util.StorageManager()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()

    @Provides
    fun providePoiDao(db: AppDatabase): PoiDao = db.poiDao()

    @Provides
    fun provideRouteDao(db: AppDatabase): RouteDao = db.routeDao()
}
