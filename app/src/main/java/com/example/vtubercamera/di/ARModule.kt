package com.example.vtubercamera.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.ARRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ARModule {
    
    @Binds
    @Singleton
    abstract fun bindARRepository(
        arRepositoryImpl: ARRepositoryImpl
    ): ARRepository
}