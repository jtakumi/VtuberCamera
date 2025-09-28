package com.example.vtubercamera.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.data.ARPhotoMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    private val _allPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())

    override fun getAllPhotos(): Flow<List<PhotoItem>> = _allPhotos.asStateFlow()

    override fun getARPhotos(): Flow<List<PhotoItem>> =
        _allPhotos.asStateFlow().map { photos ->
            photos.filter { it.isARPhoto }
        }

    override fun getNormalPhotos(): Flow<List<PhotoItem>> =
        _allPhotos.asStateFlow().map { photos ->
            photos.filter { !it.isARPhoto }
        }

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
                    // Also remove AR metadata if it exists
                    removeARMetadata(uri)
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

    override suspend fun getPhotosByAvatar(avatarName: String): List<PhotoItem> {
        return withContext(Dispatchers.IO) {
            _allPhotos.value.filter { photo ->
                photo.isARPhoto && photo.avatarName == avatarName
            }
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

                    // Load AR metadata if it exists
                    val arMetadata = getARMetadata(uri)

                    photos.add(
                        PhotoItem(
                            id = id,
                            uri = uri,
                            displayName = displayName,
                            dateAdded = dateAdded * 1000,
                            size = size,
                            mimeType = mimeType,
                            isARPhoto = arMetadata != null || displayName.startsWith("AR_"),
                            avatarName = arMetadata?.avatarName,
                            poseName = arMetadata?.poseName,
                            expressionName = arMetadata?.expressionName,
                            lightingPreset = arMetadata?.lightingPreset
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching photos", e)
        }

        return photos
    }

    private fun getARMetadata(uri: Uri): ARPhotoMetadata? {
        return try {
            val prefs = context.getSharedPreferences("ar_photo_metadata", Context.MODE_PRIVATE)
            val metadataJson = prefs.getString(uri.toString(), null) ?: return null

            // Simple JSON parsing
            parseARMetadata(metadataJson)
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to load AR metadata for $uri", e)
            null
        }
    }

    private fun parseARMetadata(json: String): ARPhotoMetadata? {
        return try {
            // Simple manual JSON parsing for AR metadata
            val avatarName = extractJsonValue(json, "avatarName")
            val poseName = extractJsonValue(json, "poseName")
            val expressionName = extractJsonValue(json, "expressionName")
            val lightingPreset = extractJsonValue(json, "lightingPreset")

            if (avatarName != null || poseName != null || expressionName != null || lightingPreset != null) {
                ARPhotoMetadata(
                    avatarName = avatarName,
                    poseName = poseName,
                    expressionName = expressionName,
                    lightingPreset = lightingPreset
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to parse AR metadata: $json", e)
            null
        }
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\":\"([^\"]*)\""
        val regex = Regex(pattern)
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun removeARMetadata(uri: Uri) {
        try {
            val prefs = context.getSharedPreferences("ar_photo_metadata", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(uri.toString())
            editor.apply()
            Log.d("MediaRepository", "Removed AR metadata for: $uri")
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to remove AR metadata", e)
        }
    }
}