package com.example.vtubercamera.data

import android.net.Uri
import com.example.vtubercamera.data.PhotoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllPhotos(): Flow<List<PhotoItem>>
    fun getARPhotos(): Flow<List<PhotoItem>>
    fun getNormalPhotos(): Flow<List<PhotoItem>>
    suspend fun refreshPhotos()
    suspend fun deletePhoto(uri: Uri): Boolean
    suspend fun deleteMultiplePhotos(uris: List<Uri>): Int
    suspend fun getPhotosByAvatar(avatarName: String): List<PhotoItem>
}