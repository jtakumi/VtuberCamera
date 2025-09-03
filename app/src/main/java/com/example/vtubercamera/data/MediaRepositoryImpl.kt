package com.example.vtubercamera.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.vtubercamera.data.PhotoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    private val _allPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())

    override fun getAllPhotos(): Flow<List<PhotoItem>> = _allPhotos.asStateFlow()

    override suspend fun refreshPhotos() {
        withContext(Dispatchers.IO) {
            try {
                val photos = getAllPhotosFromDevice()
                _allPhotos.value = photos
                Log.d("MediaRepository", "Refreshed photos: ${photos.size}")
            } catch (e: Exception) {
                Log.e("MediaRepository", "Failed to refresh photos", e)
                _allPhotos.value = emptyList()
            }
        }
    }

    override suspend fun deletePhoto(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deletedRows = context.contentResolver.delete(uri, null, null)
                val success = deletedRows > 0

                if (success) {
                    Log.d("MediaRepository", "Photo deleted: $uri")
                    refreshPhotos()
                } else {
                    Log.w("MediaRepository", "Failed to delete photo: $uri")
                }

                success
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error deleting photo", e)
                false
            }
        }
    }

    override suspend fun deleteMultiplePhotos(uris: List<Uri>): Int {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            uris.forEach { uri ->
                try {
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    if (deletedRows > 0) {
                        successCount++
                    }
                } catch (e: Exception) {
                    Log.e("MediaRepository", "Error deleting photo", e)
                }
            }

            if (successCount > 0) {
                refreshPhotos()
            }

            successCount
        }
    }

    private fun getAllPhotosFromDevice(): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""

                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString(),
                    )

                    photos.add(
                        PhotoItem(
                            id = id,
                            uri = uri,
                            displayName = displayName,
                            dateAdded = dateAdded * 1000,
                            size = size,
                            mimeType = mimeType,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching photos", e)
        }

        return photos
    }
}