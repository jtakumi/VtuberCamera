package com.example.vtubercamera.di

import com.example.vtubercamera.data.vrm.ARRenderer
import com.example.vtubercamera.data.vrm.FilamentARRenderer
import com.example.vtubercamera.data.vrm.VRMFilamentConverter
import com.example.vtubercamera.data.vrm.FilamentMaterialManager
import com.example.vtubercamera.data.vrm.FilamentTextureManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for AR rendering dependencies
 * Provides AR renderer and related components for VRM Filament rendering
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
    
    companion object {
        /**
         * Provide VRM Filament converter
         */
        @Provides
        @Singleton
        fun provideVRMFilamentConverter(): VRMFilamentConverter {
            return VRMFilamentConverter()
        }
        
        /**
         * Provide Filament material manager
         */
        @Provides
        @Singleton
        fun provideFilamentMaterialManager(): FilamentMaterialManager {
            return FilamentMaterialManager()
        }
        
        /**
         * Provide Filament texture manager
         */
        @Provides
        @Singleton
        fun provideFilamentTextureManager(): FilamentTextureManager {
            return FilamentTextureManager()
        }
    }
}