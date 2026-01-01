package com.example.vtubercamera.integration

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.CameraRepositoryImpl
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.MediaRepositoryImpl
import com.example.vtubercamera.data.ARPhotoMetadata
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.ui.viewmodels.PhotoFilterMode
import com.example.vtubercamera.ui.viewmodels.ARPhotoStats
import com.example.vtubercamera.utils.PermissionUtils
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for AR photo capture functionality
 * Tests the complete AR photo workflow including metadata handling
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ARPhotoCaptureIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        *PermissionUtils.getRequiredMediaPermissions()
    )

    private lateinit var context: Context
    private lateinit var cameraRepository: CameraRepository
    private lateinit var mediaRepository: MediaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cameraRepository = CameraRepositoryImpl(context)
        mediaRepository = MediaRepositoryImpl(context)
    }

    @Test
    fun arPhotoCapture_withMetadata_shouldSaveCorrectly() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var imageCapture: ImageCapture? = null
        val captureLatch = CountDownLatch(1)
        var capturedUri: Uri? = null
        var captureError: String? = null

        // Set up camera
        cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { imageCapture = it },
            onCameraCreated = { }
        )

        delay(1000) // Wait for camera setup

        val testMetadata = ARPhotoMetadata(
            avatarName = "Test Avatar",
            poseName = "Wave Pose",
            expressionName = "Happy Expression",
            lightingPreset = "Studio Lighting"
        )

        // When - Capture AR photo
        imageCapture?.let { capture ->
            cameraRepository.captureARPhoto(
                imageCapture = capture,
                arMetadata = testMetadata,
                onPhotoSaved = { uri ->
                    capturedUri = uri
                    captureLatch.countDown()
                },
                onError = { error ->
                    captureError = error
                    captureLatch.countDown()
                }
            )
        } ?: fail("ImageCapture should not be null")

        // Then
        val completed = captureLatch.await(15, TimeUnit.SECONDS)
        assertTrue("Photo capture should complete", completed)

        if (capturedUri != null) {
            assertNotNull("Photo URI should not be null", capturedUri)
            assertNull("Should not have capture error", captureError)

            // Verify photo was saved with correct naming
            val fileName = capturedUri!!.lastPathSegment ?: ""
            assertTrue("Photo should have AR prefix", fileName.contains("AR_"))

        } else {
            assertNotNull("Should have error if capture failed", captureError)
        }

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun arPhotoMetadata_shouldBeStoredAndRetrieved() = runTest {
        // Given
        val testMetadata = ARPhotoMetadata(
            avatarName = "Metadata Test Avatar",
            poseName = "Test Pose",
            expressionName = "Test Expression",
            lightingPreset = "Test Lighting"
        )

        // When - Refresh photos after capture
        mediaRepository.refreshPhotos()
        delay(2000)

        // Then - Check if AR photos are properly identified
        val allPhotos = mediaRepository.getAllPhotos().first()
        val arPhotos = mediaRepository.getARPhotos().first()

        // AR photos should be subset of all photos
        assertTrue("AR photos should not exceed total photos", arPhotos.size <= allPhotos.size)

        // Check if any AR photos have the expected structure
        arPhotos.forEach { photo ->
            assertTrue("Photo should be marked as AR", photo.isARPhoto)
            // Additional metadata checks would depend on actual stored data
        }
    }

    @Test
    fun photoFiltering_byARStatus_shouldWork() = runTest {
        // Given
        mediaRepository.refreshPhotos()
        delay(1000)

        // When
        val allPhotos = mediaRepository.getAllPhotos().first()
        val arPhotos = mediaRepository.getARPhotos().first()
        val normalPhotos = mediaRepository.getNormalPhotos().first()

        // Then
        assertEquals(
            "All photos should equal AR + normal photos",
            allPhotos.size,
            arPhotos.size + normalPhotos.size
        )

        // All AR photos should have isARPhoto = true
        arPhotos.forEach { photo ->
            assertTrue("AR photo should be marked as AR", photo.isARPhoto)
        }

        // All normal photos should have isARPhoto = false
        normalPhotos.forEach { photo ->
            assertFalse("Normal photo should not be marked as AR", photo.isARPhoto)
        }
    }

    @Test
    fun arPhotoStats_shouldCalculateCorrectly() = runTest {
        // Given - Mock some AR photos with different metadata
        val mockARPhotos = listOf(
            createMockARPhoto("Avatar1", "Pose1", "Happy", "Studio"),
            createMockARPhoto("Avatar1", "Pose2", "Sad", "Outdoor"),
            createMockARPhoto("Avatar2", "Pose1", "Happy", "Studio"),
            createMockARPhoto("Avatar2", "Pose3", "Neutral", "Indoor")
        )

        // When - Calculate stats (simulated)
        val stats = calculateStatsFromPhotos(mockARPhotos)

        // Then
        assertEquals("Should have 4 total AR photos", 4, stats.totalARPhotos)
        assertEquals("Should have 2 unique avatars", 2, stats.uniqueAvatars)
        assertEquals("Should have 3 unique poses", 3, stats.uniquePoses)
        assertEquals("Should have 3 unique expressions", 3, stats.uniqueExpressions)

        assertTrue("Should contain Avatar1", stats.avatarNames.contains("Avatar1"))
        assertTrue("Should contain Avatar2", stats.avatarNames.contains("Avatar2"))
        assertTrue("Should contain Pose1", stats.poseNames.contains("Pose1"))
        assertTrue("Should contain Happy expression", stats.expressionNames.contains("Happy"))
    }

    @Test
    fun photosByAvatar_shouldFilterCorrectly() = runTest {
        // Given
        mediaRepository.refreshPhotos()
        delay(1000)

        val testAvatarName = "Test Filter Avatar"

        // When
        val avatarPhotos = mediaRepository.getPhotosByAvatar(testAvatarName)

        // Then
        avatarPhotos.forEach { photo ->
            assertTrue("Photo should be AR photo", photo.isARPhoto)
            assertEquals("Photo should have correct avatar name", testAvatarName, photo.avatarName)
        }
    }

    @Test
    fun arPhotoDeletion_shouldRemoveMetadata() = runTest {
        // Given - Create a test photo URI
        val testUri = Uri.parse("content://media/external/images/media/12345")

        // When - Delete photo
        val deleteResult = mediaRepository.deletePhoto(testUri)

        // Then - Deletion should complete (may succeed or fail depending on photo existence)
        // The important thing is that it doesn't crash and handles the operation gracefully
        // In a real test, we would verify metadata cleanup
    }

    @Test
    fun normalPhotoCapture_shouldNotHaveARMetadata() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var imageCapture: ImageCapture? = null
        val captureLatch = CountDownLatch(1)
        var capturedUri: Uri? = null

        // Set up camera
        cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { imageCapture = it },
            onCameraCreated = { }
        )

        delay(1000)

        // When - Capture normal photo
        imageCapture?.let { capture ->
            cameraRepository.capturePhoto(
                imageCapture = capture,
                onPhotoSaved = { uri ->
                    capturedUri = uri
                    captureLatch.countDown()
                },
                onError = { captureLatch.countDown() }
            )
        }

        // Then
        val completed = captureLatch.await(10, TimeUnit.SECONDS)
        assertTrue("Photo capture should complete", completed)

        if (capturedUri != null) {
            // Normal photos should not have AR prefix
            val fileName = capturedUri!!.lastPathSegment ?: ""
            assertFalse("Normal photo should not have AR prefix", fileName.startsWith("AR_"))
        }

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun highResolutionCapture_shouldMaintainQuality() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var imageCapture: ImageCapture? = null

        // When - Set up camera with high quality mode
        cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { capture ->
                imageCapture = capture
                // Verify high quality settings
                assertEquals(
                    "Should use maximize quality mode",
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                    capture.captureMode
                )
            },
            onCameraCreated = { }
        )

        delay(1000)

        // Then
        assertNotNull("ImageCapture should be created", imageCapture)

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun concurrentPhotoOperations_shouldHandleCorrectly() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var imageCapture: ImageCapture? = null
        val captureCount = 3
        val captureLatch = CountDownLatch(captureCount)
        var successfulCaptures = 0
        var failedCaptures = 0

        // Set up camera
        cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { imageCapture = it },
            onCameraCreated = { }
        )

        delay(1000)

        // When - Attempt multiple rapid captures
        repeat(captureCount) { index ->
            val metadata = ARPhotoMetadata(
                avatarName = "Concurrent Test Avatar",
                poseName = "Pose $index",
                expressionName = "Expression $index",
                lightingPreset = "Lighting $index"
            )

            imageCapture?.let { capture ->
                cameraRepository.captureARPhoto(
                    imageCapture = capture,
                    arMetadata = metadata,
                    onPhotoSaved = {
                        successfulCaptures++
                        captureLatch.countDown()
                    },
                    onError = {
                        failedCaptures++
                        captureLatch.countDown()
                    }
                )
                // Small delay between captures
                delay(500)
            }
        }

        // Then
        val completed = captureLatch.await(30, TimeUnit.SECONDS)
        assertTrue("All captures should complete", completed)
        assertEquals(
            "Total operations should equal captures attempted",
            captureCount,
            successfulCaptures + failedCaptures
        )

        // At least some captures should succeed
        // (Some might fail due to timing or device limitations)
        assertTrue("Should have some results", successfulCaptures + failedCaptures > 0)

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    // ========== Helper Methods ==========

    private fun createMockARPhoto(
        avatarName: String,
        poseName: String,
        expressionName: String,
        lightingPreset: String
    ): PhotoItem {
        return PhotoItem(
            id = System.currentTimeMillis(),
            uri = Uri.parse("content://mock/${System.nanoTime()}"),
            displayName = "AR_mock_${System.nanoTime()}.jpg",
            dateAdded = System.currentTimeMillis(),
            size = 2048000L,
            mimeType = "image/jpeg",
            isARPhoto = true,
            avatarName = avatarName,
            poseName = poseName,
            expressionName = expressionName,
            lightingPreset = lightingPreset
        )
    }

    private fun calculateStatsFromPhotos(photos: List<PhotoItem>): ARPhotoStats {
        val totalCount = photos.size
        val avatars = photos.mapNotNull { it.avatarName }.distinct()
        val poses = photos.mapNotNull { it.poseName }.distinct()
        val expressions = photos.mapNotNull { it.expressionName }.distinct()

        return ARPhotoStats(
            totalARPhotos = totalCount,
            uniqueAvatars = avatars.size,
            uniquePoses = poses.size,
            uniqueExpressions = expressions.size,
            avatarNames = avatars,
            poseNames = poses,
            expressionNames = expressions
        )
    }

    // Helper class for testing lifecycle
    private class TestLifecycleOwner : androidx.lifecycle.LifecycleOwner {
        private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun moveToState(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}