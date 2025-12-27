package com.example.vtubercamera.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.ARRepositoryImpl
import com.example.vtubercamera.data.ARFallbackManager
import com.example.vtubercamera.data.ARTrackingMonitor
import com.example.vtubercamera.data.vrm.ErrorHandler
import com.example.vtubercamera.data.vrm.ErrorNotificationManager
import com.example.vtubercamera.data.vrm.ErrorRecoveryManager
import com.example.vtubercamera.data.vrm.NetworkErrorHandler
import com.example.vtubercamera.data.vrm.FileAccessErrorHandler
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
        
        @Provides
        @Singleton
        fun provideErrorHandler(
            @ApplicationContext context: Context
        ): ErrorHandler = ErrorHandler(context)
        
        @Provides
        @Singleton
        fun provideErrorNotificationManager(
            @ApplicationContext context: Context
        ): ErrorNotificationManager = ErrorNotificationManager(context)
        
        @Provides
        @Singleton
        fun provideErrorRecoveryManager(
            @ApplicationContext context: Context
        ): ErrorRecoveryManager = ErrorRecoveryManager(context)
        
        @Provides
        @Singleton
        fun provideNetworkErrorHandler(
            @ApplicationContext context: Context,
            errorHandler: ErrorHandler,
            errorNotificationManager: ErrorNotificationManager
        ): NetworkErrorHandler = NetworkErrorHandler(context, errorHandler, errorNotificationManager)
        
        @Provides
        @Singleton
        fun provideFileAccessErrorHandler(
            @ApplicationContext context: Context,
            errorHandler: ErrorHandler,
            errorNotificationManager: ErrorNotificationManager
        ): FileAccessErrorHandler = FileAccessErrorHandler(context, errorHandler, errorNotificationManager)
    }
}