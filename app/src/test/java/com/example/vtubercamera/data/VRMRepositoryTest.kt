package com.example.vtubercamera.data

import android.net.Uri
import com.example.vtubercamera.data.vrm.ValidationError
import com.example.vtubercamera.data.vrm.ValidationResult
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.VRMMetadata
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for VRM validation, data models, and AR photo functionality
 */
class VRMRepositoryTest {

    @Mock
    private lateinit var mockVRMRepository: VRMRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `ValidationResult Valid should return true for isValid`() {
        val result = ValidationResult.Valid
        assertTrue(result.isValid())
        assertFalse(result.isInvalid())
    }
    
    @Test
    fun `ValidationResult Invalid should return false for isValid`() {
        val errors = listOf(
            ValidationError.fileNotFound("/test/path")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertFalse(result.isValid())
        assertTrue(result.isInvalid())
        assertTrue(result.hasErrors())
        assertEquals(1, result.errors.size)
    }
    
    @Test
    fun `ValidationError critical should create critical error`() {
        val error = ValidationError.critical(
            ValidationError.ErrorType.FILE_NOT_FOUND,
            "Test error"
        )
        
        assertTrue(error.isCritical())
        assertFalse(error.isWarning())
        assertEquals(ValidationError.ErrorType.FILE_NOT_FOUND, error.type)
        assertEquals("Test error", error.message)
    }
    
    @Test
    fun `ValidationError warning should create warning error`() {
        val error = ValidationError.warning(
            ValidationError.ErrorType.INVALID_METADATA,
            "Test warning"
        )
        
        assertFalse(error.isCritical())
        assertTrue(error.isWarning())
        assertEquals(ValidationError.ErrorType.INVALID_METADATA, error.type)
        assertEquals("Test warning", error.message)
    }
    
    @Test
    fun `ValidationResult Invalid should identify critical errors`() {
        val errors = listOf(
            ValidationError.critical(ValidationError.ErrorType.FILE_NOT_FOUND, "Critical"),
            ValidationError.warning(ValidationError.ErrorType.INVALID_METADATA, "Warning")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertTrue(result.hasCriticalErrors())
        assertEquals(1, result.getCriticalErrors().size)
        assertEquals(1, result.getWarnings().size)
    }
    
    @Test
    fun `ValidationResult Invalid should get first error message`() {
        val errors = listOf(
            ValidationError.critical(ValidationError.ErrorType.FILE_NOT_FOUND, "First error"),
            ValidationError.warning(ValidationError.ErrorType.INVALID_METADATA, "Second error")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertEquals("First error", result.getFirstErrorMessage())
        assertEquals(2, result.getAllErrorMessages().size)
    }

    // ========== VRM Repository Tests ==========

    @Test
    fun `VRMRepository loadVRMFromUri should return success result with valid model`() = runTest {
        // Given
        val testUri: Uri = mock()
        val testModel = createTestVRMModel()

        whenever(mockVRMRepository.loadVRMFromUri(testUri))
            .thenReturn(Result.success(testModel))

        // When
        val result = mockVRMRepository.loadVRMFromUri(testUri)

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        assertEquals("Model name should match", "Test Avatar", result.getOrNull()?.name)
        verify(mockVRMRepository).loadVRMFromUri(testUri)
    }

    @Test
    fun `VRMRepository loadVRMFromUri should return failure result with invalid file`() = runTest {
        // Given
        val testUri: Uri = mock()
        val exception = RuntimeException("Invalid VRM file")

        whenever(mockVRMRepository.loadVRMFromUri(testUri))
            .thenReturn(Result.failure(exception))

        // When
        val result = mockVRMRepository.loadVRMFromUri(testUri)

        // Then
        assertTrue("Result should be failure", result.isFailure)
        assertEquals("Exception message should match", "Invalid VRM file", result.exceptionOrNull()?.message)
    }

    @Test
    fun `VRMRepository getLibraryStatistics should return correct stats`() = runTest {
        // Given
        val expectedStats = AvatarLibraryStats(
            totalAvatars = 5,
            totalFileSize = 1024000L,
            totalThumbnailSize = 256000L,
            favoriteCount = 2,
            recentlyUsedCount = 3,
            newlyAddedCount = 1,
            withExpressionsCount = 4,
            withPosesCount = 2,
            availableTags = listOf("cute", "anime"),
            usageFrequencies = emptyMap()
        )

        whenever(mockVRMRepository.getLibraryStatistics())
            .thenReturn(expectedStats)

        // When
        val stats = mockVRMRepository.getLibraryStatistics()

        // Then
        assertEquals("Total avatars should match", 5, stats.totalAvatars)
        assertEquals("Total file size should match", 1024000L, stats.totalFileSize)
        assertEquals("Average file size should match", 204800L, stats.getAverageFileSize())
        verify(mockVRMRepository).getLibraryStatistics()
    }

    @Test
    fun `VRMRepository deleteAvatar should return success for valid ID`() = runTest {
        // Given
        val avatarId = "test-avatar-123"

        whenever(mockVRMRepository.deleteAvatar(avatarId))
            .thenReturn(Result.success(Unit))

        // When
        val result = mockVRMRepository.deleteAvatar(avatarId)

        // Then
        assertTrue("Deletion should be successful", result.isSuccess)
        verify(mockVRMRepository).deleteAvatar(avatarId)
    }

    @Test
    fun `VRMRepository renameAvatar should return success with valid parameters`() = runTest {
        // Given
        val avatarId = "test-avatar-123"
        val newName = "New Avatar Name"

        whenever(mockVRMRepository.renameAvatar(avatarId, newName))
            .thenReturn(Result.success(Unit))

        // When
        val result = mockVRMRepository.renameAvatar(avatarId, newName)

        // Then
        assertTrue("Rename should be successful", result.isSuccess)
        verify(mockVRMRepository).renameAvatar(avatarId, newName)
    }

    @Test
    fun `VRMRepository setAvatarFavorite should toggle favorite status`() = runTest {
        // Given
        val avatarId = "test-avatar-123"
        val isFavorite = true

        whenever(mockVRMRepository.setAvatarFavorite(avatarId, isFavorite))
            .thenReturn(Result.success(Unit))

        // When
        val result = mockVRMRepository.setAvatarFavorite(avatarId, isFavorite)

        // Then
        assertTrue("Set favorite should be successful", result.isSuccess)
        verify(mockVRMRepository).setAvatarFavorite(avatarId, isFavorite)
    }

    @Test
    fun `VRMRepository recordAvatarUsage should track usage correctly`() = runTest {
        // Given
        val avatarId = "test-avatar-123"

        whenever(mockVRMRepository.recordAvatarUsage(avatarId))
            .thenReturn(Unit)

        // When
        mockVRMRepository.recordAvatarUsage(avatarId)

        // Then
        verify(mockVRMRepository).recordAvatarUsage(avatarId)
    }

    // ========== AR Photo Metadata Tests ==========

    @Test
    fun `ARPhotoMetadata should be created with all parameters`() {
        // Given
        val metadata = ARPhotoMetadata(
            avatarName = "Test Avatar",
            poseName = "Wave Pose",
            expressionName = "Happy",
            lightingPreset = "Bright Studio"
        )

        // Then
        assertEquals("Avatar name should match", "Test Avatar", metadata.avatarName)
        assertEquals("Pose name should match", "Wave Pose", metadata.poseName)
        assertEquals("Expression name should match", "Happy", metadata.expressionName)
        assertEquals("Lighting preset should match", "Bright Studio", metadata.lightingPreset)
    }

    @Test
    fun `ARPhotoMetadata should handle null values`() {
        // Given
        val metadata = ARPhotoMetadata(
            avatarName = null,
            poseName = null,
            expressionName = "Happy",
            lightingPreset = null
        )

        // Then
        assertNull("Avatar name should be null", metadata.avatarName)
        assertNull("Pose name should be null", metadata.poseName)
        assertEquals("Expression name should match", "Happy", metadata.expressionName)
        assertNull("Lighting preset should be null", metadata.lightingPreset)
    }

    // ========== PhotoItem Tests ==========

    @Test
    fun `PhotoItem should be created with AR metadata`() {
        // Given
        val testUri: Uri = mock()
        val photoItem = PhotoItem(
            id = 123L,
            uri = testUri,
            displayName = "AR_2025-01-15-12-30-45-123.jpg",
            dateAdded = System.currentTimeMillis(),
            size = 2048000L,
            mimeType = "image/jpeg",
            isARPhoto = true,
            avatarName = "Cute Avatar",
            poseName = "Peace Sign",
            expressionName = "Smile",
            lightingPreset = "Outdoor"
        )

        // Then
        assertTrue("Should be AR photo", photoItem.isARPhoto)
        assertEquals("Avatar name should match", "Cute Avatar", photoItem.avatarName)
        assertEquals("Pose name should match", "Peace Sign", photoItem.poseName)
        assertEquals("Expression name should match", "Smile", photoItem.expressionName)
        assertEquals("Lighting preset should match", "Outdoor", photoItem.lightingPreset)
    }

    @Test
    fun `PhotoItem should be created as normal photo`() {
        // Given
        val testUri: Uri = mock()
        val photoItem = PhotoItem(
            id = 456L,
            uri = testUri,
            displayName = "2025-01-15-12-30-45-456.jpg",
            dateAdded = System.currentTimeMillis(),
            size = 1536000L,
            mimeType = "image/jpeg"
        )

        // Then
        assertFalse("Should not be AR photo", photoItem.isARPhoto)
        assertNull("Avatar name should be null", photoItem.avatarName)
        assertNull("Pose name should be null", photoItem.poseName)
        assertNull("Expression name should be null", photoItem.expressionName)
        assertNull("Lighting preset should be null", photoItem.lightingPreset)
    }

    // ========== Helper Methods ==========

    private fun createTestVRMModel(): VRMModel {
        return VRMModel(
            id = "test-avatar-123",
            name = "Test Avatar",
            meshData = byteArrayOf(1, 2, 3, 4, 5),
            textureData = mapOf("diffuse" to byteArrayOf(6, 7, 8, 9, 10)),
            expressions = emptyList(),
            poses = emptyList(),
            metadata = VRMMetadata(
                title = "Test Avatar",
                version = "1.0",
                author = "Test Author",
                contactInformation = "test@example.com",
                reference = "https://example.com",
                allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
                violentUsage = VRMMetadata.Usage.DISALLOW,
                sexualUsage = VRMMetadata.Usage.DISALLOW,
                commercialUsage = VRMMetadata.Usage.ALLOW,
                otherPermissionUrl = "",
                licenseName = VRMMetadata.LicenseType.OTHER,
                otherLicenseUrl = "https://example.com/license"
            )
        )
    }
}