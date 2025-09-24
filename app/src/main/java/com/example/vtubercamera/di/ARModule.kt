package com.example.vtubercamera.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.ARRepositoryImpl
import com.example.vtubercamera.data.ARFallbackManager
import com.example.vtubercamera.data.ARTrackingMonitor
import com.example.vtubercamera.managers.ARPermissionManager
import com.example.vtubercamera.managers.ARSessionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ARModule {
    
    @Binds
    @Singleton
    abstract fun bindARRepository(
        arRepositoryImpl: ARRepositoryImpl
    ): ARRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideARFallbackManager(): ARFallbackManager = ARFallbackManager()
    }
}