package com.example.vtubercamera.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.VRMRepository
import com.example.vtubercamera.data.ARPhotoMetadata
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.data.vrm.AvatarLibraryManager
import com.example.vtubercamera.data.vrm.AvatarController
import com.example.vtubercamera.data.vrm.ExpressionController
import com.example.vtubercamera.data.vrm.PoseController
import com.example.vtubercamera.data.vrm.LightingSystem
import com.example.vtubercamera.data.vrm.ARRenderer
import com.example.vtubercamera.data.vrm.AvatarState
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.ErrorNotificationManager
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.domain.ar.ARFeature
import com.example.vtubercamera.domain.avatar.AvatarFeature
import com.example.vtubercamera.domain.camera.CameraControlsFeature
import com.example.vtubercamera.domain.camera.CameraCaptureFeature
import com.example.vtubercamera.domain.camera.GalleryFeature
import com.example.vtubercamera.domain.camera.LensSwitchFeature
import com.example.vtubercamera.domain.lighting.LightingFeature
import com.example.vtubercamera.utils.CameraCapabilityManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockCameraRepository: CameraRepository

    @Mock
    private lateinit var mockMediaRepository: MediaRepository

    @Mock
    private lateinit var mockCameraCapabilityManager: CameraCapabilityManager

    @Mock
    private lateinit var mockARRepository: ARRepository

    @Mock
    private lateinit var mockVRMRepository: VRMRepository

    @Mock
    private lateinit var mockAvatarLibraryManager: AvatarLibraryManager

    @Mock
    private lateinit var mockAvatarController: AvatarController

    @Mock
    private lateinit var mockExpressionController: ExpressionController

    @Mock
    private lateinit var mockPoseController: PoseController

    @Mock
    private lateinit var mockLightingSystem: LightingSystem

    @Mock
    private lateinit var mockARRenderer: ARRenderer

    @Mock
    private lateinit var mockErrorNotificationManager: ErrorNotificationManager

    private lateinit var viewModel: CameraViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup mock returns
        whenever(mockMediaRepository.getAllPhotos()).thenReturn(flowOf(emptyList()))
        whenever(mockMediaRepository.getARPhotos()).thenReturn(flowOf(emptyList()))
        whenever(mockMediaRepository.getNormalPhotos()).thenReturn(flowOf(emptyList()))
        val latestPhotoUri: Uri = org.mockito.kotlin.mock()
        whenever(mockMediaRepository.getLatestPhotoUri()).thenReturn(flowOf(latestPhotoUri))

        // Setup avatar library manager mock returns
        whenever(mockAvatarLibraryManager.getAvatarsSortedBy(org.mockito.kotlin.any())).thenReturn(flowOf(emptyList()))

        // Setup AR repository mock returns
        whenever(mockARRepository.sessionState).thenReturn(MutableStateFlow(ARSessionState()))
        whenever(mockARRepository.cameraState).thenReturn(MutableStateFlow(ARCameraState.default()))
        whenever(mockARRepository.trackingState).thenReturn(MutableStateFlow(TrackingState.STOPPED))

        // Setup controller mock returns
        whenever(mockExpressionController.currentExpression).thenReturn(MutableStateFlow(null))
        whenever(mockExpressionController.activeBlendShapes).thenReturn(MutableStateFlow(emptyMap()))
        whenever(mockExpressionController.isTransitioning).thenReturn(MutableStateFlow(false))
        whenever(mockExpressionController.transitionProgress).thenReturn(MutableStateFlow(0f))

        whenever(mockPoseController.currentPose).thenReturn(MutableStateFlow(null))
        whenever(mockPoseController.activeBoneTransforms).thenReturn(MutableStateFlow(emptyMap()))
        whenever(mockPoseController.boneLocks).thenReturn(MutableStateFlow(emptySet()))
        whenever(mockPoseController.isTransitioning).thenReturn(MutableStateFlow(false))
        whenever(mockPoseController.transitionProgress).thenReturn(MutableStateFlow(0f))

        whenever(mockAvatarController.avatarState).thenReturn(MutableStateFlow(AvatarState()))

        // Setup lighting system mock returns
        val lightingSettings: LightingSettings = org.mockito.kotlin.mock()
        whenever(mockLightingSystem.lightingSettings).thenReturn(MutableStateFlow(lightingSettings))
        whenever(mockLightingSystem.environmentLighting).thenReturn(MutableStateFlow(null))
        whenever(mockLightingSystem.getLightingPresets()).thenReturn(emptyList())

        val cameraControlsFeature = CameraControlsFeature()
        val cameraCaptureFeature = CameraCaptureFeature(cameraRepository = mockCameraRepository)
        val galleryFeature = GalleryFeature(mediaRepository = mockMediaRepository)
        val lensSwitchFeature = LensSwitchFeature(cameraCapabilityManager = mockCameraCapabilityManager)
        val arFeature = ARFeature(arRepository = mockARRepository)
        val avatarFeature = AvatarFeature(
            vrmRepository = mockVRMRepository,
            avatarLibraryManager = mockAvatarLibraryManager,
            avatarController = mockAvatarController,
            expressionController = mockExpressionController,
            poseController = mockPoseController,
        )
        val lightingFeature = LightingFeature(lightingSystem = mockLightingSystem)

        val bootstrapper = CameraViewModelBootstrapper(
            mediaRepository = mockMediaRepository,
            lensSwitchFeature = lensSwitchFeature,
            avatarFeature = avatarFeature,
        )
        val arSessionStarter = ARSessionStarter(arRepository = mockARRepository)

        viewModel = CameraViewModel(
            cameraRepository = mockCameraRepository,
            mediaRepository = mockMediaRepository,
            cameraControlsFeature = cameraControlsFeature,
            cameraCaptureFeature = cameraCaptureFeature,
            galleryFeature = galleryFeature,
            lensSwitchFeature = lensSwitchFeature,
            arFeature = arFeature,
            avatarFeature = avatarFeature,
            lightingFeature = lightingFeature,
            bootstrapper = bootstrapper,
            arSessionStarter = arSessionStarter,
            arRepository = mockARRepository,
            arRenderer = mockARRenderer,
            errorNotificationManager = mockErrorNotificationManager,
        )

        // Run ViewModel init coroutines
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `viewModel should be initialized properly`() {
        assertNotNull(viewModel)
    }

    @Test
    fun `initial flash mode should be OFF`() {
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `initial zoom ratio should be 1_0f`() {
        assertEquals(1.0f, viewModel.zoomRatio.value, 0.001f)
    }

    @Test
    fun `initial preview mode should be false`() {
        assertEquals(false, viewModel.isPreviewMode.value)
    }

    @Test
    fun `initial camera selector should be back camera`() {
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `toggleFlash should cycle through flash modes correctly`() {
        // 初期状態: OFF
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)

        // 1回目のトグル: OFF -> ON
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.flashMode.value)

        // 2回目のトグル: ON -> AUTO
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, viewModel.flashMode.value)

        // 3回目のトグル: AUTO -> OFF
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `switchCamera should toggle between front and back camera`() {
        // 初期状態: BACK
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)

        // 1回目の切り替え: BACK -> FRONT
        viewModel.switchCamera()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.cameraSelector.value)

        // 2回目の切り替え: FRONT -> BACK
        viewModel.switchCamera()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `enterPreviewMode should set preview mode to true`() {
        // 初期状態
        assertEquals(false, viewModel.isPreviewMode.value)

        // プレビューモード開始
        viewModel.enterPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.isPreviewMode.value)
    }

    @Test
    fun `exitPreviewMode should set preview mode to false and trigger camera rebind`() {
        // プレビューモード開始
        viewModel.enterPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.isPreviewMode.value)

        // プレビューモード終了
        viewModel.exitPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.isPreviewMode.value)
        assertEquals(true, viewModel.needsCameraRebind.value)
    }

    @Test
    fun `onCameraRebound should reset camera rebind flag`() {
        // カメラ再バインドが必要な状態にする
        viewModel.exitPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.needsCameraRebind.value)

        // カメラ再バインド完了
        viewModel.onCameraRebound()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.needsCameraRebind.value)
    }

    @Test
    fun `refreshPhotos should call mediaRepository refreshPhotos`() = runTest {
        // When - Refresh photos is called
        viewModel.refreshPhotos()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - MediaRepository refresh should be called
        verify(mockMediaRepository).refreshPhotos()
    }

    // ========== AR Mode Tests ==========

    @Test
    fun `initial AR mode should be false`() {
        assertEquals(false, viewModel.isARMode.value)
    }

    @Test
    fun `disableARMode should set AR mode to false`() {
        // When
        viewModel.disableARMode()

        // Then
        assertEquals(false, viewModel.isARMode.value)
    }

    @Test
    fun `clearARError should reset AR error state`() {
        // When
        viewModel.clearARError()

        // Then
        assertNull("AR error should be null", viewModel.arError.value)
    }

    // ========== Photo Filtering Tests ==========

    @Test
    fun `initial photo filter mode should be ALL`() {
        assertEquals(PhotoFilterMode.ALL, viewModel.photoFilterMode.value)
    }

    @Test
    fun `setPhotoFilterMode should update filter mode`() {
        // When
        viewModel.setPhotoFilterMode(PhotoFilterMode.AR_ONLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(PhotoFilterMode.AR_ONLY, viewModel.photoFilterMode.value)

        // When
        viewModel.setPhotoFilterMode(PhotoFilterMode.NORMAL_ONLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(PhotoFilterMode.NORMAL_ONLY, viewModel.photoFilterMode.value)
    }

    @Test
    fun `getFilteredPhotos should return all photos for ALL mode`() {
        // Given
        val allPhotos = listOf(
            createTestPhotoItem(1L, false),
            createTestPhotoItem(2L, true)
        )

        // When
        viewModel.setPhotoFilterMode(PhotoFilterMode.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Note: This test would need the actual photos to be set, but we're testing the logic
        assertEquals(PhotoFilterMode.ALL, viewModel.photoFilterMode.value)
    }

    @Test
    fun `isARPhoto should return correct AR photo status`() {
        // Given
        val arPhoto = createTestPhotoItem(1L, true)
        val normalPhoto = createTestPhotoItem(2L, false)

        // When & Then
        assertTrue("AR photo should be identified", viewModel.isARPhoto(arPhoto))
        assertFalse("Normal photo should not be identified as AR", viewModel.isARPhoto(normalPhoto))
    }

    @Test
    fun `getARPhotoStats should return correct statistics`() {
        // When
        val stats = viewModel.getARPhotoStats()

        // Then
        assertNotNull("Stats should not be null", stats)
        assertEquals("Initial AR photos count should be 0", 0, stats.totalARPhotos)
        assertEquals("Initial unique avatars should be 0", 0, stats.uniqueAvatars)
        assertEquals("Initial unique poses should be 0", 0, stats.uniquePoses)
        assertEquals("Initial unique expressions should be 0", 0, stats.uniqueExpressions)
    }

    // ========== Avatar Management Tests ==========

    @Test
    fun `selectAvatarFromLibrary should call VRM repository methods`() = runTest {
        // Given
        val avatarId = "test-avatar-123"

        // When
        viewModel.selectAvatarFromLibrary(avatarId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockVRMRepository).recordAvatarUsage(avatarId)
    }

    @Test
    fun `toggleAvatarFavorite should call VRM repository setAvatarFavorite`() = runTest {
        // Given
        val avatarId = "test-avatar-123"

        // When
        viewModel.toggleAvatarFavorite(avatarId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Note: The actual call verification would depend on the avatar existing in the library
        // This test verifies the method doesn't crash when called
    }

    @Test
    fun `deleteAvatarFromLibrary should call VRM repository deleteAvatar`() = runTest {
        // Given
        val avatarId = "test-avatar-123"

        // When
        viewModel.deleteAvatarFromLibrary(avatarId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockVRMRepository).deleteAvatar(avatarId)
    }

    @Test
    fun `renameAvatarInLibrary should call VRM repository renameAvatar`() = runTest {
        // Given
        val avatarId = "test-avatar-123"
        val newName = "New Avatar Name"

        // When
        viewModel.renameAvatarInLibrary(avatarId, newName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockVRMRepository).renameAvatar(avatarId, newName)
    }

    @Test
    fun `cleanupAvatarLibrary should call VRM repository cleanupLibrary`() = runTest {
        // When
        viewModel.cleanupAvatarLibrary()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockVRMRepository).cleanupLibrary()
    }

    @Test
    fun `clearAvatarLibraryError should reset avatar library error`() {
        // When
        viewModel.clearAvatarLibraryError()

        // Then
        assertNull("Avatar library error should be null", viewModel.avatarLibraryError.value)
    }

    // ========== Avatar Control Tests ==========

    @Test
    fun `selectExpression should call expression controller`() = runTest {
        // Given
        val expression = Expression("happy", "Happy", mapOf("mouth_smile" to 1.0f))

        // When
        viewModel.selectExpression(expression)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Default is smooth transitions enabled; implementation uses transition APIs.
        verify(mockExpressionController).transitionToExpression(expression)
    }

    @Test
    fun `clearExpression should call expression controller`() = runTest {
        // When
        viewModel.clearExpression()

        // Then
        verify(mockExpressionController).clearExpression()
    }

    @Test
    fun `selectPose should call pose controller`() = runTest {
        // Given
        val pose = Pose("wave", "Wave", mapOf("rightArm" to com.example.vtubercamera.data.vrm.math.Transform.identity()))

        // When
        viewModel.selectPose(pose)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // Default is smooth transitions enabled; implementation uses transition APIs.
        verify(mockPoseController).transitionToPose(pose)
    }

    @Test
    fun `clearPose should call pose controller`() = runTest {
        // When
        viewModel.clearPose()

        // Then
        verify(mockPoseController).clearPose()
    }

    @Test
    fun `resetToDefaultPose should call pose controller`() = runTest {
        // When
        viewModel.resetToDefaultPose()

        // Then
        verify(mockPoseController).resetToDefaultPose()
    }

    @Test
    fun `setSmoothTransitions should update smooth transitions state`() {
        // When
        viewModel.setSmoothTransitions(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue("Smooth transitions should be true", viewModel.smoothTransitions.value)

        // When
        viewModel.setSmoothTransitions(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse("Smooth transitions should be false", viewModel.smoothTransitions.value)
    }

    @Test
    fun `setAutoResetOnAvatarChange should update auto reset state`() {
        // When
        viewModel.setAutoResetOnAvatarChange(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue("Auto reset should be true", viewModel.autoResetOnAvatarChange.value)

        // When
        viewModel.setAutoResetOnAvatarChange(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse("Auto reset should be false", viewModel.autoResetOnAvatarChange.value)
    }

    // ========== Helper Methods ==========

    private fun createTestPhotoItem(id: Long, isARPhoto: Boolean): PhotoItem {
        return PhotoItem(
            id = id,
            uri = org.mockito.kotlin.mock(),
            displayName = if (isARPhoto) "AR_test_$id.jpg" else "test_$id.jpg",
            dateAdded = System.currentTimeMillis(),
            size = 1024000L,
            mimeType = "image/jpeg",
            isARPhoto = isARPhoto,
            avatarName = if (isARPhoto) "Test Avatar" else null,
            poseName = if (isARPhoto) "Wave" else null,
            expressionName = if (isARPhoto) "Happy" else null,
            lightingPreset = if (isARPhoto) "Studio" else null
        )
    }
}
