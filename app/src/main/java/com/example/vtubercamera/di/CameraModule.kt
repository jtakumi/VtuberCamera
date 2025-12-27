package com.example.vtubercamera.di

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.CameraRepositoryImpl
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.MediaRepositoryImpl
import com.example.vtubercamera.data.VRMRepository
import com.example.vtubercamera.data.VRMRepositoryImpl
import com.example.vtubercamera.data.vrm.AvatarThumbnailGenerator
import com.example.vtubercamera.data.vrm.AvatarLibraryManager
import com.example.vtubercamera.data.performance.PerformanceMonitor
import com.example.vtubercamera.data.performance.PerformanceOptimizer
import com.example.vtubercamera.data.performance.BatteryMonitor
import com.example.vtubercamera.data.performance.FrameRateMonitor
import com.example.vtubercamera.data.performance.PerformanceManager
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

    @Binds
    @Singleton
    abstract fun bindVRMRepository(
        vrmRepositoryImpl: VRMRepositoryImpl
    ): VRMRepository

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

        @Provides
        @Singleton
        fun provideAvatarThumbnailGenerator(
            @ApplicationContext context: Context
        ): AvatarThumbnailGenerator {
            return AvatarThumbnailGenerator(context)
        }

        @Provides
        @Singleton
        fun providePerformanceMonitor(
            @ApplicationContext context: Context
        ): PerformanceMonitor {
            return PerformanceMonitor(context)
        }

        @Provides
        @Singleton
        fun providePerformanceOptimizer(): PerformanceOptimizer {
            return PerformanceOptimizer()
        }

        @Provides
        @Singleton
        fun provideBatteryMonitor(
            @ApplicationContext context: Context
        ): BatteryMonitor {
            return BatteryMonitor(context)
        }

        @Provides
        @Singleton
        fun provideFrameRateMonitor(): FrameRateMonitor {
            return FrameRateMonitor()
        }

        @Provides
        @Singleton
        fun providePerformanceManager(
            performanceMonitor: PerformanceMonitor,
            performanceOptimizer: PerformanceOptimizer,
            batteryMonitor: BatteryMonitor,
            frameRateMonitor: FrameRateMonitor
        ): PerformanceManager {
            return PerformanceManager(
                performanceMonitor,
                performanceOptimizer,
                batteryMonitor,
                frameRateMonitor
            )
        }
    }
}