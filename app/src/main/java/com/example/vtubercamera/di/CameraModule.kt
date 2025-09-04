package com.example.vtubercamera.di

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.CameraRepositoryImpl
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.MediaRepositoryImpl
import com.example.vtubercamera.utils.CameraCapabilityManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        cameraRepositoryImpl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    companion object {
        @Provides
        @Singleton
        fun provideProcessCameraProvider(
            @ApplicationContext context: Context
        ): ProcessCameraProvider {
            return ProcessCameraProvider.getInstance(context).get()
        }

        @Provides
        @Singleton
        fun provideCameraCapabilityManager(
            @ApplicationContext context: Context
        ): CameraCapabilityManager {
            return CameraCapabilityManager(context)
        }
    }
}