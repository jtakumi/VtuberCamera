package com.example.vtubercamera.data.vrm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and manages thumbnails for VRM avatars
 */
@Singleton
class AvatarThumbnailGenerator @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "AvatarThumbnailGenerator"
        private const val THUMBNAIL_SIZE = 256
        private const val THUMBNAIL_QUALITY = 85
        private const val THUMBNAILS_DIR = "avatar_thumbnails"
    }
    
    private val thumbnailsDir: File by lazy {
        File(context.filesDir, THUMBNAILS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Generate a thumbnail for the given VRM model
     * 
     * @param vrmModel The VRM model to generate thumbnail for
     * @param avatarId The avatar ID to use for the thumbnail filename
     * @return Path to the generated thumbnail file, or null if generation failed
     */
    suspend fun generateThumbnail(vrmModel: VRMModel, avatarId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating thumbnail for avatar: ${vrmModel.name}")
            
            // For now, create a placeholder thumbnail
            // In a real implementation, this would render the 3D model
            val thumbnail = createPlaceholderThumbnail(vrmModel)
            
            val thumbnailFile = File(thumbnailsDir, "${avatarId}_thumbnail.jpg")
            
            FileOutputStream(thumbnailFile).use { output ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, output)
            }
            
            Log.d(TAG, "Thumbnail generated successfully: ${thumbnailFile.absolutePath}")
            thumbnailFile.absolutePath
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to generate thumbnail for avatar: ${vrmModel.name}", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory while generating thumbnail", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error generating thumbnail", e)
            null
        }
    }
    
    /**
     * Generate a thumbnail from VRM texture data
     * 
     * @param vrmModel The VRM model containing texture data
     * @param avatarId The avatar ID to use for the thumbnail filename
     * @return Path to the generated thumbnail file, or null if generation failed
     */
    suspend fun generateThumbnailFromTexture(vrmModel: VRMModel, avatarId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Try to extract main texture from VRM model
            val mainTexture = extractMainTexture(vrmModel)
            
            val thumbnail = if (mainTexture != null) {
                createThumbnailFromBitmap(mainTexture)
            } else {
                createPlaceholderThumbnail(vrmModel)
            }
            
            val thumbnailFile = File(thumbnailsDir, "${avatarId}_thumbnail.jpg")
            
            FileOutputStream(thumbnailFile).use { output ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, output)
            }
            
            thumbnailFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate thumbnail from texture", e)
            null
        }
    }
    
    /**
     * Delete thumbnail for the given avatar ID
     * 
     * @param avatarId The avatar ID whose thumbnail should be deleted
     * @return True if deletion was successful, false otherwise
     */
    suspend fun deleteThumbnail(avatarId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailsDir, "${avatarId}_thumbnail.jpg")
            if (thumbnailFile.exists()) {
                val deleted = thumbnailFile.delete()
                Log.d(TAG, "Thumbnail deletion result for $avatarId: $deleted")
                deleted
            } else {
                Log.d(TAG, "Thumbnail file not found for avatar: $avatarId")
                true // Consider it successful if file doesn't exist
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting thumbnail for avatar: $avatarId", e)
            false
        }
    }
    
    /**
     * Get thumbnail path for the given avatar ID
     * 
     * @param avatarId The avatar ID
     * @return Path to thumbnail file if it exists, null otherwise
     */
    fun getThumbnailPath(avatarId: String): String? {
        val thumbnailFile = File(thumbnailsDir, "${avatarId}_thumbnail.jpg")
        return if (thumbnailFile.exists()) {
            thumbnailFile.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Check if thumbnail exists for the given avatar ID
     * 
     * @param avatarId The avatar ID
     * @return True if thumbnail exists, false otherwise
     */
    fun hasThumbnail(avatarId: String): Boolean {
        val thumbnailFile = File(thumbnailsDir, "${avatarId}_thumbnail.jpg")
        return thumbnailFile.exists()
    }
    
    /**
     * Get thumbnail bitmap for the given avatar ID
     * 
     * @param avatarId The avatar ID
     * @return Bitmap of the thumbnail, or null if not found
     */
    suspend fun getThumbnailBitmap(avatarId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val thumbnailPath = getThumbnailPath(avatarId)
            if (thumbnailPath != null) {
                BitmapFactory.decodeFile(thumbnailPath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading thumbnail bitmap for avatar: $avatarId", e)
            null
        }
    }
    
    /**
     * Clean up orphaned thumbnails (thumbnails without corresponding avatars)
     * 
     * @param existingAvatarIds List of existing avatar IDs
     * @return Number of orphaned thumbnails deleted
     */
    suspend fun cleanupOrphanedThumbnails(existingAvatarIds: Set<String>): Int = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            
            thumbnailsDir.listFiles { file -> 
                file.extension == "jpg" && file.name.endsWith("_thumbnail.jpg")
            }?.forEach { thumbnailFile ->
                val avatarId = thumbnailFile.nameWithoutExtension.removeSuffix("_thumbnail")
                if (!existingAvatarIds.contains(avatarId)) {
                    if (thumbnailFile.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted orphaned thumbnail: ${thumbnailFile.name}")
                    }
                }
            }
            
            Log.d(TAG, "Cleaned up $deletedCount orphaned thumbnails")
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up orphaned thumbnails", e)
            0
        }
    }
    
    /**
     * Get total size of all thumbnails
     * 
     * @return Total size in bytes
     */
    suspend fun getTotalThumbnailSize(): Long = withContext(Dispatchers.IO) {
        try {
            thumbnailsDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating thumbnail directory size", e)
            0L
        }
    }
    
    /**
     * Clear all thumbnails
     * 
     * @return Number of thumbnails deleted
     */
    suspend fun clearAllThumbnails(): Int = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            
            thumbnailsDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }
            
            Log.d(TAG, "Cleared $deletedCount thumbnails")
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing thumbnails", e)
            0
        }
    }
    
    /**
     * Extract main texture from VRM model
     * This is a placeholder implementation - in a real app, you'd parse the VRM structure
     */
    private fun extractMainTexture(vrmModel: VRMModel): Bitmap? {
        try {
            // Try to find main texture in texture data
            val mainTextureData = vrmModel.textureData.values.firstOrNull()
            
            return if (mainTextureData != null) {
                BitmapFactory.decodeByteArray(mainTextureData, 0, mainTextureData.size)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract main texture from VRM", e)
            return null
        }
    }
    
    /**
     * Create a thumbnail from an existing bitmap
     */
    private fun createThumbnailFromBitmap(sourceBitmap: Bitmap): Bitmap {
        val size = minOf(sourceBitmap.width, sourceBitmap.height)
        val x = (sourceBitmap.width - size) / 2
        val y = (sourceBitmap.height - size) / 2
        
        // Create square crop
        val croppedBitmap = Bitmap.createBitmap(sourceBitmap, x, y, size, size)
        
        // Scale to thumbnail size
        return Bitmap.createScaledBitmap(croppedBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
    }
    
    /**
     * Create a placeholder thumbnail for VRM models without textures
     */
    private fun createPlaceholderThumbnail(vrmModel: VRMModel): Bitmap {
        val bitmap = Bitmap.createBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background gradient
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // Create a simple gradient background based on avatar name hash
        val nameHash = vrmModel.name.hashCode()
        val hue = (nameHash % 360).toFloat()
        val backgroundColor = Color.HSVToColor(floatArrayOf(hue, 0.3f, 0.9f))
        val accentColor = Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.7f))
        
        // Fill background
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, THUMBNAIL_SIZE.toFloat(), THUMBNAIL_SIZE.toFloat(), paint)
        
        // Draw avatar icon/shape
        paint.color = accentColor
        val centerX = THUMBNAIL_SIZE / 2f
        val centerY = THUMBNAIL_SIZE / 2f
        val radius = THUMBNAIL_SIZE * 0.3f
        
        // Draw simple avatar representation
        canvas.drawCircle(centerX, centerY - radius * 0.2f, radius * 0.6f, paint) // Head
        canvas.drawCircle(centerX, centerY + radius * 0.8f, radius * 0.8f, paint) // Body
        
        // Draw text
        paint.color = Color.WHITE
        paint.textSize = THUMBNAIL_SIZE * 0.1f
        paint.textAlign = Paint.Align.CENTER
        
        val textY = THUMBNAIL_SIZE - (THUMBNAIL_SIZE * 0.1f)
        val displayName = if (vrmModel.name.length > 10) {
            vrmModel.name.take(8) + "..."
        } else {
            vrmModel.name
        }
        canvas.drawText(displayName, centerX, textY, paint)
        
        return bitmap
    }
}