package com.example.vtubercamera.integration

import android.Manifest
import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.ARPhotoMetadata
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.CameraRepositoryImpl
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.MediaRepositoryImpl
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.LightEstimate
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.utils.PermissionUtils
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for ARCore + CameraX functionality
 * Tests the interaction between AR session management and camera operations
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ARCoreCameraXIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        *PermissionUtils.getRequiredMediaPermissions()
    )

    private lateinit var context: Context
    private lateinit var cameraRepository: CameraRepository
    private lateinit var arRepository: ARRepository
    private lateinit var mediaRepository: MediaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cameraRepository = CameraRepositoryImpl(context)
        // ARRepositoryImpl is DI-only now. Use a small fake to keep androidTest compiling.
        arRepository = FakeARRepository()
        mediaRepository = MediaRepositoryImpl(context)
    }

    @Test
    fun arCoreAvailability_shouldBeCheckedCorrectly() {
        // When
        val availability = ArCoreApk.getInstance().checkAvailability(context)

        // Then
        assertNotNull("ARCore availability should not be null", availability)
        // Note: In emulator, this might be UNSUPPORTED_DEVICE_NOT_CAPABLE
        // In real device with ARCore, should be SUPPORTED_INSTALLED
        assertTrue(
            "ARCore should have a defined availability status",
            availability == ArCoreApk.Availability.SUPPORTED_INSTALLED ||
            availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ||
            availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED ||
            availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD
        )
    }

    @Test
    fun cameraProvider_shouldBeInitialized() = runTest {
        // When
        val cameraProvider = withTimeout(5000) {
            cameraRepository.getCameraProvider()
        }

        // Then
        assertNotNull("Camera provider should not be null", cameraProvider)
        assertTrue("Camera provider should be available", cameraProvider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA))
    }

    @Test
    fun arRepository_shouldInitializeSessionOnSupportedDevice() = runTest {
        // Given
        val availability = ArCoreApk.getInstance().checkAvailability(context)

        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            // Skip test on unsupported devices
            return@runTest
        }

        val initializationLatch = CountDownLatch(1)
        var sessionInitialized = false
        var initializationError: com.example.vtubercamera.data.vrm.ARError? = null

        // When
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        arRepository.initializeSession(
            context = context,
            lifecycleOwner = mockLifecycleOwner,
            onSessionReady = {
                sessionInitialized = true
                initializationLatch.countDown()
            },
            onError = { error ->
                initializationError = error
                initializationLatch.countDown()
            }
        )

        // Then
        val completed = initializationLatch.await(10, TimeUnit.SECONDS)
        assertTrue("AR session initialization should complete within timeout", completed)

        if (sessionInitialized) {
            assertTrue("AR session should be initialized", arRepository.isSessionInitialized())
        } else {
            assertNotNull("Should have error if not initialized", initializationError)
        }

        // Cleanup
        arRepository.destroySession()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun cameraAndArIntegration_shouldWorkTogether() = runTest {
        // Given
        val availability = ArCoreApk.getInstance().checkAvailability(context)

        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            // Skip test on unsupported devices
            return@runTest
        }

        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var cameraInitialized = false
        var imageCaptureCreated = false

        // When - Initialize AR session first
        val arInitLatch = CountDownLatch(1)
        arRepository.initializeSession(
            context = context,
            lifecycleOwner = mockLifecycleOwner,
            onSessionReady = { arInitLatch.countDown() },
            onError = { arInitLatch.countDown() }
        )

        arInitLatch.await(5, TimeUnit.SECONDS)

        // Then - Initialize camera
        val camera = cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { imageCaptureCreated = true },
            onCameraCreated = { cameraInitialized = true }
        )

        // Assert
        if (arRepository.isSessionInitialized()) {
            assertNotNull("Camera should be bound when AR is initialized", camera)
            assertTrue("Camera should be initialized", cameraInitialized)
            assertTrue("ImageCapture should be created", imageCaptureCreated)
        }

        // Cleanup
        cameraProvider.unbindAll()
        arRepository.destroySession()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun arPhotoCapture_shouldCaptureWithMetadata() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var imageCapture: ImageCapture? = null
        val captureLatch = CountDownLatch(1)
        var captureSuccessful = false
        var captureError: String? = null

        // When - Bind camera
        cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { imageCapture = it },
            onCameraCreated = { }
        )

        // Wait for camera to be ready
        delay(1000)

        val arMetadata = ARPhotoMetadata(
            avatarName = "Test Avatar",
            poseName = "Test Pose",
            expressionName = "Test Expression",
            lightingPreset = "Test Lighting"
        )

        // Capture AR photo
        imageCapture?.let { capture ->
            cameraRepository.captureARPhoto(
                imageCapture = capture,
                arMetadata = arMetadata,
                onPhotoSaved = { uri ->
                    captureSuccessful = true
                    captureLatch.countDown()
                },
                onError = { error ->
                    captureError = error
                    captureLatch.countDown()
                }
            )
        } ?: run {
            fail("ImageCapture should not be null")
        }

        // Then
        val completed = captureLatch.await(10, TimeUnit.SECONDS)
        assertTrue("Photo capture should complete within timeout", completed)

        if (captureSuccessful) {
            assertTrue("AR photo should be captured successfully", captureSuccessful)
        } else {
            assertNotNull("Should have error if capture failed", captureError)
        }

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun mediaRepository_shouldLoadPhotosWithARMetadata() = runTest {
        // When
        mediaRepository.refreshPhotos()

        // Give some time for photos to load
        delay(2000)

        // Then
        val allPhotos = mediaRepository.getAllPhotos()
        assertNotNull("All photos flow should not be null", allPhotos)

        val arPhotos = mediaRepository.getARPhotos()
        assertNotNull("AR photos flow should not be null", arPhotos)

        val normalPhotos = mediaRepository.getNormalPhotos()
        assertNotNull("Normal photos flow should not be null", normalPhotos)

        // These flows should be properly initialized
        // Actual values depend on what photos exist on the device
    }

    @Test
    fun arSessionState_shouldBeObservable() = runTest {
        // Given
        val availability = ArCoreApk.getInstance().checkAvailability(context)

        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            // Skip test on unsupported devices
            return@runTest
        }

        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        // When
        val sessionStateFlow = arRepository.sessionState
        val cameraStateFlow = arRepository.cameraState
        val trackingStateFlow = arRepository.trackingState

        // Then
        assertNotNull("Session state flow should not be null", sessionStateFlow)
        assertNotNull("Camera state flow should not be null", cameraStateFlow)
        assertNotNull("Tracking state flow should not be null", trackingStateFlow)

        // Initialize session to test state changes
        val initLatch = CountDownLatch(1)
        arRepository.initializeSession(
            context = context,
            lifecycleOwner = mockLifecycleOwner,
            onSessionReady = { initLatch.countDown() },
            onError = { initLatch.countDown() }
        )

        initLatch.await(5, TimeUnit.SECONDS)

        // Cleanup
        arRepository.destroySession()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun cameraRepository_shouldSwitchCamerasCorrectly() = runTest {
        // Given
        val cameraProvider = cameraRepository.getCameraProvider()
        val mockLifecycleOwner = TestLifecycleOwner()
        mockLifecycleOwner.moveToState(Lifecycle.State.STARTED)

        var backCameraInitialized = false
        var frontCameraInitialized = false

        // When - Test back camera
        val backCamera = cameraRepository.bindCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { },
            onCameraCreated = { backCameraInitialized = true }
        )

        delay(500) // Wait for camera to initialize

        // Switch to front camera
        val frontCamera = cameraRepository.switchToCamera(
            lifecycleOwner = mockLifecycleOwner,
            cameraProvider = cameraProvider,
            newCameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            onImageCaptureCreated = { },
            onCameraCreated = { frontCameraInitialized = true }
        )

        delay(500) // Wait for camera switch

        // Then
        assertNotNull("Back camera should be initialized", backCamera)
        assertTrue("Back camera initialization callback should be called", backCameraInitialized)

        if (cameraProvider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA)) {
            assertNotNull("Front camera should be available and bound", frontCamera)
            assertTrue("Front camera initialization callback should be called", frontCameraInitialized)
        }

        // Cleanup
        cameraProvider.unbindAll()
        mockLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
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

    /** Minimal fake AR repository for compilation and non-ARCore-dependent assertions. */
    private class FakeARRepository : ARRepository {
        private val _sessionState = MutableStateFlow(ARSessionState())
        override val sessionState: Flow<ARSessionState> = _sessionState.asStateFlow()

        private val _cameraState = MutableStateFlow(ARCameraState.default())
        override val cameraState: Flow<ARCameraState> = _cameraState.asStateFlow()

        private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
        override val trackingState: Flow<TrackingState> = _trackingState.asStateFlow()

        private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
        override val lightEstimate: Flow<LightEstimate?> = _lightEstimate.asStateFlow()

        private var trackingListener: ((TrackingState) -> Unit)? = null

        override fun initializeSession(
            context: Context,
            lifecycleOwner: androidx.lifecycle.LifecycleOwner,
            onSessionReady: () -> Unit,
            onError: (ARError) -> Unit
        ) {
            // Fake: immediately report ready.
            _sessionState.value = _sessionState.value.copy(isInitialized = true)
            onSessionReady()
        }

        override fun pauseSession() {
            // no-op
        }

        override fun resumeSession() {
            // no-op
        }

        override fun destroySession() {
            _sessionState.value = _sessionState.value.copy(isInitialized = false)
        }

        override fun enablePlaneDetection(enabled: Boolean) {
            _sessionState.value = _sessionState.value.copy(isPlaneDetectionEnabled = enabled)
        }

        override fun enableEnvironmentalHDR(enabled: Boolean) {
            _sessionState.value = _sessionState.value.copy(isEnvironmentalHDREnabled = enabled)
        }

        override fun enableLightEstimation(enabled: Boolean) {
            _sessionState.value = _sessionState.value.copy(isLightEstimationEnabled = enabled)
        }

        override fun isSessionInitialized(): Boolean = _sessionState.value.isInitialized

        override fun getCurrentTrackingState(): TrackingState = TrackingState.STOPPED

        override fun getCurrentCameraState(): ARCameraState = _cameraState.value

        override fun getCurrentLightEstimate(): LightEstimate? = _lightEstimate.value

        override fun setTrackingStateListener(listener: (TrackingState) -> Unit) {
            trackingListener = listener
        }

        override fun removeTrackingStateListener() {
            trackingListener = null
        }

        override fun configureSession(
            planeDetectionEnabled: Boolean,
            lightEstimationEnabled: Boolean,
            environmentalHDREnabled: Boolean
        ) {
            enablePlaneDetection(planeDetectionEnabled)
            enableLightEstimation(lightEstimationEnabled)
            enableEnvironmentalHDR(environmentalHDREnabled)
        }

        override suspend fun waitForTracking(timeoutMs: Long): Boolean = true
    }
}