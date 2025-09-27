package com.example.vtubercamera.di

import com.example.vtubercamera.data.vrm.ARRenderer
import com.example.vtubercamera.data.vrm.FilamentARRenderer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for AR rendering dependencies
 * Provides AR renderer and related components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ARRenderingModule {
    
    /**
     * Bind FilamentARRenderer as the implementation of ARRenderer
     */
    @Binds
    @Singleton
    abstract fun bindARRenderer(
        filamentARRenderer: FilamentARRenderer
    ): ARRenderer
}