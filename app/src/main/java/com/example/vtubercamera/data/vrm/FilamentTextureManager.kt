package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

/**
 * Manages Filament textures for VRM avatars
 * Handles texture loading, format conversion, and GPU upload
 */
@Singleton
class FilamentTextureManager @Inject constructor() {
    
    companion object {
        private const val TAG = "FilamentTextureManager"
        private const val MAX_TEXTURE_SIZE = 2048
        private const val DEFAULT_TEXTURE_SIZE = 512
    }
    
    // Texture cache
    private val textureCache = mutableMapOf<String, FilamentTextureInstance>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    
    /**
     * Load texture from VRM texture data
     */
    fun loadTexture(texture: FilamentTexture): FilamentTextureInstance {
        return textureCache.getOrPut(texture.name) {
            Log.d(TAG, "Loading texture: ${texture.name}")
            
            try {
                val bitmap = loadBitmapFromData(texture.data, texture.format)
                val processedBitmap = processTexture(bitmap, texture)
                
                val instance = FilamentTextureInstance(
                    name = texture.name,
                    bitmap = processedBitmap,
                    width = processedBitmap.width,
                    height = processedBitmap.height,
                    format = determineFilamentFormat(processedBitmap),
                    sRGB = texture.sRGB,
                    mipLevels = calculateMipLevels(processedBitmap.width, processedBitmap.height),
                    originalTexture = texture
                )
                
                Log.d(TAG, "Loaded texture: ${texture.name} (${instance.width}x${instance.height})")
                instance
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load texture: ${texture.name}", e)
                createErrorTexture(texture.name)
            }
        }
    }
    
    /**
     * Load bitmap from texture data
     */
    private fun loadBitmapFromData(data: ByteArray, format: TextureFormat): Bitmap {
        return when (format) {
            TextureFormat.PNG, TextureFormat.JPEG -> {
                val inputStream = ByteArrayInputStream(data)
                BitmapFactory.decodeStream(inputStream)
                    ?: throw IllegalArgumentException("Failed to decode image data")
            }
            TextureFormat.UNKNOWN -> {
                // Try to decode anyway
                val inputStream = ByteArrayInputStream(data)
                BitmapFactory.decodeStream(inputStream)
                    ?: createDefaultBitmap()
            }
        }
    }
    
    /**
     * Process texture (resize, format conversion, etc.)
     */
    private fun processTexture(bitmap: Bitmap, texture: FilamentTexture): Bitmap {
        var processedBitmap = bitmap
        
        // Resize if too large
        if (bitmap.width > MAX_TEXTURE_SIZE || bitmap.height > MAX_TEXTURE_SIZE) {
            Log.d(TAG, "Resizing texture ${texture.name} from ${bitmap.width}x${bitmap.height}")
            
            val scale = minOf(
                MAX_TEXTURE_SIZE.toFloat() / bitmap.width,
                MAX_TEXTURE_SIZE.toFloat() / bitmap.height
            )
            
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            processedBitmap = bitmap.scale(newWidth, newHeight)
            
            if (processedBitmap != bitmap) {
                bitmap.recycle()
            }
        }
        
        // Ensure power-of-two dimensions for better GPU compatibility
        if (!isPowerOfTwo(processedBitmap.width) || !isPowerOfTwo(processedBitmap.height)) {
            val newWidth = nextPowerOfTwo(processedBitmap.width)
            val newHeight = nextPowerOfTwo(processedBitmap.height)
            
            if (newWidth != processedBitmap.width || newHeight != processedBitmap.height) {
                Log.d(TAG, "Converting texture ${texture.name} to power-of-two: ${newWidth}x${newHeight}")
                
                val powerOfTwoBitmap = processedBitmap.scale(newWidth, newHeight)
                
                if (powerOfTwoBitmap != processedBitmap) {
                    processedBitmap.recycle()
                }
                
                processedBitmap = powerOfTwoBitmap
            }
        }
        
        // Convert to appropriate format
        val targetConfig = if (texture.sRGB) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        if (processedBitmap.config != targetConfig) {
            val convertedBitmap = processedBitmap.copy(targetConfig, false)
            if (convertedBitmap != processedBitmap) {
                processedBitmap.recycle()
            }
            processedBitmap = convertedBitmap
        }
        
        return processedBitmap
    }
    
    /**
     * Determine Filament texture format from bitmap
     */
    private fun determineFilamentFormat(bitmap: Bitmap): FilamentTextureFormat {
        return when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> FilamentTextureFormat.RGBA8
            Bitmap.Config.RGB_565 -> FilamentTextureFormat.RGB565
            Bitmap.Config.ALPHA_8 -> FilamentTextureFormat.R8
            else -> FilamentTextureFormat.RGBA8
        }
    }
    
    /**
     * Calculate number of mip levels
     */
    private fun calculateMipLevels(width: Int, height: Int): Int {
        val maxDimension = maxOf(width, height)
        return (kotlin.math.log2(maxDimension.toFloat()).toInt() + 1).coerceAtMost(10)
    }
    
    /**
     * Create error texture for failed loads
     */
    private fun createErrorTexture(name: String): FilamentTextureInstance {
        Log.w(TAG, "Creating error texture for: $name")
        
        val errorBitmap = createErrorBitmap()
        
        return FilamentTextureInstance(
            name = name,
            bitmap = errorBitmap,
            width = errorBitmap.width,
            height = errorBitmap.height,
            format = FilamentTextureFormat.RGBA8,
            sRGB = true,
            mipLevels = 1,
            originalTexture = null
        )
    }
    
    /**
     * Create default bitmap for unknown formats
     */
    private fun createDefaultBitmap(): Bitmap {
        val size = DEFAULT_TEXTURE_SIZE
        val bitmap = createBitmap(size, size)
        
        // Fill with white
        bitmap.eraseColor(0xFFFFFFFF.toInt())
        
        return bitmap
    }
    
    /**
     * Create error bitmap (magenta checkerboard)
     */
    private fun createErrorBitmap(): Bitmap {
        val size = 64
        val bitmap = createBitmap(size, size)
        
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val checker = ((x / 8) + (y / 8)) % 2
                pixels[y * size + x] = if (checker == 0) 0xFFFF00FF.toInt() else 0xFF000000.toInt()
            }
        }
        
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
    
    /**
     * Check if number is power of two
     */
    private fun isPowerOfTwo(n: Int): Boolean {
        return n > 0 && (n and (n - 1)) == 0
    }
    
    /**
     * Get next power of two
     */
    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) {
            power *= 2
        }
        return power.coerceAtMost(MAX_TEXTURE_SIZE)
    }
    
    /**
     * Generate mipmaps for texture
     */
    fun generateMipmaps(texture: FilamentTextureInstance): List<Bitmap> {
        val mipmaps = mutableListOf<Bitmap>()
        var currentBitmap = texture.bitmap
        mipmaps.add(currentBitmap)
        
        var width = currentBitmap.width
        var height = currentBitmap.height
        
        while (width > 1 || height > 1) {
            width = maxOf(1, width / 2)
            height = maxOf(1, height / 2)
            
            val mipmap = currentBitmap.scale(width, height)
            mipmaps.add(mipmap)
            
            // Don't recycle the original bitmap
            if (currentBitmap != texture.bitmap) {
                currentBitmap.recycle()
            }
            
            currentBitmap = mipmap
        }
        
        Log.d(TAG, "Generated ${mipmaps.size} mipmap levels for ${texture.name}")
        return mipmaps
    }
    
    /**
     * Compress texture data for GPU upload
     */
    fun compressTexture(texture: FilamentTextureInstance, compressionFormat: TextureCompressionFormat): ByteArray {
        return when (compressionFormat) {
            TextureCompressionFormat.NONE -> {
                // Convert bitmap to raw RGBA data
                val pixels = IntArray(texture.width * texture.height)
                texture.bitmap.getPixels(pixels, 0, texture.width, 0, 0, texture.width, texture.height)
                
                val byteArray = ByteArray(pixels.size * 4)
                for (i in pixels.indices) {
                    val pixel = pixels[i]
                    byteArray[i * 4] = ((pixel shr 16) and 0xFF).toByte() // R
                    byteArray[i * 4 + 1] = ((pixel shr 8) and 0xFF).toByte() // G
                    byteArray[i * 4 + 2] = (pixel and 0xFF).toByte() // B
                    byteArray[i * 4 + 3] = ((pixel shr 24) and 0xFF).toByte() // A
                }
                byteArray
            }
            TextureCompressionFormat.ETC2 -> {
                // TODO: Implement ETC2 compression
                Log.w(TAG, "ETC2 compression not implemented, using uncompressed")
                compressTexture(texture, TextureCompressionFormat.NONE)
            }
            TextureCompressionFormat.ASTC -> {
                // TODO: Implement ASTC compression
                Log.w(TAG, "ASTC compression not implemented, using uncompressed")
                compressTexture(texture, TextureCompressionFormat.NONE)
            }
        }
    }
    
    /**
     * Get texture memory usage
     */
    fun getTextureMemoryUsage(texture: FilamentTextureInstance): Long {
        val bytesPerPixel = when (texture.format) {
            FilamentTextureFormat.RGBA8 -> 4
            FilamentTextureFormat.RGB565 -> 2
            FilamentTextureFormat.R8 -> 1
        }
        
        var totalMemory = 0L
        var width = texture.width
        var height = texture.height
        
        // Calculate memory for all mip levels
        for (level in 0 until texture.mipLevels) {
            totalMemory += width * height * bytesPerPixel
            width = maxOf(1, width / 2)
            height = maxOf(1, height / 2)
        }
        
        return totalMemory
    }
    
    /**
     * Clear texture cache
     */
    fun clearCache() {
        Log.d(TAG, "Clearing texture cache")
        
        // Recycle bitmaps
        bitmapCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        
        textureCache.values.forEach { texture ->
            if (!texture.bitmap.isRecycled) {
                texture.bitmap.recycle()
            }
        }
        
        textureCache.clear()
        bitmapCache.clear()
    }
    
    /**
     * Get texture manager statistics
     */
    fun getStatistics(): TextureManagerStatistics {
        val totalMemory = textureCache.values.sumOf { getTextureMemoryUsage(it) }
        
        return TextureManagerStatistics(
            cachedTextures = textureCache.size,
            totalMemoryUsage = totalMemory,
            averageTextureSize = if (textureCache.isNotEmpty()) {
                textureCache.values.map { it.width * it.height }.average().toInt()
            } else 0
        )
    }
}

/**
 * Filament texture instance
 */
data class FilamentTextureInstance(
    val name: String,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val format: FilamentTextureFormat,
    val sRGB: Boolean,
    val mipLevels: Int,
    val originalTexture: FilamentTexture?
) {
    fun isValid(): Boolean = !bitmap.isRecycled
    
    fun getMemoryUsage(): Long {
        val bytesPerPixel = when (format) {
            FilamentTextureFormat.RGBA8 -> 4
            FilamentTextureFormat.RGB565 -> 2
            FilamentTextureFormat.R8 -> 1
        }
        return width * height * bytesPerPixel.toLong()
    }
}

/**
 * Filament texture formats
 */
enum class FilamentTextureFormat {
    RGBA8,
    RGB565,
    R8
}

/**
 * Texture compression formats
 */
enum class TextureCompressionFormat {
    NONE,
    ETC2,
    ASTC
}

/**
 * Texture manager statistics
 */
data class TextureManagerStatistics(
    val cachedTextures: Int,
    val totalMemoryUsage: Long,
    val averageTextureSize: Int
) {
    fun getFormattedMemoryUsage(): String {
        return when {
            totalMemoryUsage < 1024 -> "${totalMemoryUsage}B"
            totalMemoryUsage < 1024 * 1024 -> "${totalMemoryUsage / 1024}KB"
            totalMemoryUsage < 1024 * 1024 * 1024 -> "${totalMemoryUsage / (1024 * 1024)}MB"
            else -> "${totalMemoryUsage / (1024 * 1024 * 1024)}GB"
        }
    }
}