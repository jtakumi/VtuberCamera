# AR Avatar API Specification

## Overview

This document provides comprehensive API documentation for the VRM AR Avatar feature in VTuber Camera. The API is designed following MVVM architecture patterns with reactive programming using StateFlow.

## Core APIs

### VRMRepository Interface

The primary interface for VRM file operations and avatar management.

```kotlin
interface VRMRepository {
    /**
     * Loads a VRM file from the given URI
     * @param uri The URI of the VRM file to load
     * @return Result containing VRMModel on success or error on failure
     */
    suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel>
    
    /**
     * Saves a VRM model to the avatar library
     * @param vrmModel The VRM model to save
     * @param name Custom name for the avatar
     * @return Result containing avatar ID on success
     */
    suspend fun saveVRMToLibrary(vrmModel: VRMModel, name: String): Result<String>
    
    /**
     * Gets the list of avatars in the library
     * @return Flow of avatar info list
     */
    suspend fun getAvatarLibrary(): Flow<List<AvatarInfo>>
    
    /**
     * Deletes an avatar from the library
     * @param avatarId The ID of the avatar to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteAvatar(avatarId: String): Result<Unit>
    
    /**
     * Validates a VRM file before loading
     * @param uri The URI of the VRM file to validate
     * @return ValidationResult with details
     */
    fun validateVRMFile(uri: Uri): ValidationResult
}
```

### ARRenderer Interface

Interface for AR rendering operations using Filament engine.

```kotlin
interface ARRenderer {
    /**
     * Initializes the AR renderer with surface and AR session
     * @param surface The rendering surface
     * @param arSession The ARCore session
     */
    fun initialize(surface: Surface, arSession: Session)
    
    /**
     * Updates the rendering frame with AR data
     * @param frame The current AR frame
     * @param avatarState The current avatar state
     */
    fun updateFrame(frame: Frame, avatarState: AvatarState)
    
    /**
     * Renders the VRM avatar with given transform
     * @param vrmModel The VRM model to render
     * @param transform The transformation to apply
     */
    fun renderAvatar(vrmModel: VRMModel, transform: Transform)
    
    /**
     * Sets lighting based on AR environment estimation
     * @param lightEstimate The light estimation from ARCore
     */
    fun setLighting(lightEstimate: LightEstimate)
    
    /**
     * Captures the current frame as bitmap
     * @return Bitmap of the rendered frame
     */
    fun captureFrame(): Bitmap
    
    /**
     * Cleans up renderer resources
     */
    fun cleanup()
}
```

### AvatarController Class

Controls avatar transformations and animations.

```kotlin
class AvatarController {
    /**
     * Updates avatar position
     * @param deltaX X-axis movement delta
     * @param deltaY Y-axis movement delta  
     * @param deltaZ Z-axis movement delta
     */
    fun updatePosition(deltaX: Float, deltaY: Float, deltaZ: Float)
    
    /**
     * Updates avatar rotation
     * @param deltaYaw Yaw rotation delta
     * @param deltaPitch Pitch rotation delta
     * @param deltaRoll Roll rotation delta
     */
    fun updateRotation(deltaYaw: Float, deltaPitch: Float, deltaRoll: Float)
    
    /**
     * Updates avatar scale
     * @param scaleFactor Scale multiplication factor
     */
    fun updateScale(scaleFactor: Float)
    
    /**
     * Sets avatar expression
     * @param expression The expression to apply
     */
    fun setExpression(expression: Expression)
    
    /**
     * Sets avatar pose
     * @param pose The pose to apply
     */
    fun setPose(pose: Pose)
    
    /**
     * Resets avatar to default state
     */
    fun resetToDefault()
}
```

## Data Models

### VRMModel

Core data structure representing a VRM avatar.

```kotlin
data class VRMModel(
    val id: String,                                    // Unique identifier
    val name: String,                                  // Display name
    val meshData: ByteArray,                          // 3D mesh data
    val textureData: Map<String, ByteArray>,          // Texture mappings
    val expressions: List<Expression>,                 // Available expressions
    val poses: List<Pose>,                            // Available poses
    val metadata: VRMMetadata                         // VRM metadata
)
```

### AvatarState

Represents the current state of an avatar in AR space.

```kotlin
data class AvatarState(
    val model: VRMModel?,                             // Current VRM model
    val transform: Transform,                         // Position/rotation/scale
    val currentExpression: Expression?,               // Active expression
    val currentPose: Pose?,                          // Active pose
    val isVisible: Boolean                           // Visibility flag
)
```

### Transform

3D transformation data structure.

```kotlin
data class Transform(
    val position: Vector3,                           // 3D position
    val rotation: Quaternion,                        // 3D rotation
    val scale: Vector3                              // 3D scale
) {
    companion object {
        fun identity(): Transform                     // Identity transform
    }
}
```

## Error Handling

### VRMLoadingError

Sealed class for VRM loading errors.

```kotlin
sealed class VRMLoadingError : Exception() {
    object FileNotFound : VRMLoadingError()
    object InvalidFormat : VRMLoadingError()
    object FileSizeExceeded : VRMLoadingError()
    object CorruptedData : VRMLoadingError()
    object UnsupportedVersion : VRMLoadingError()
    data class ParseError(val details: String) : VRMLoadingError()
}
```

### ARError

Sealed class for AR-related errors.

```kotlin
sealed class ARError : Exception() {
    object ARCoreNotSupported : ARError()
    object ARCoreNotInstalled : ARError()
    object CameraPermissionDenied : ARError()
    object SessionInitializationFailed : ARError()
    object TrackingLost : ARError()
    data class RenderingError(val details: String) : ARError()
}
```

## Usage Examples

### Loading and Displaying an Avatar

```kotlin
// In ViewModel
class ARCameraViewModel @Inject constructor(
    private val vrmRepository: VRMRepository,
    private val arRepository: ARRepository
) : ViewModel() {
    
    suspend fun loadAvatar(uri: Uri) {
        try {
            val result = vrmRepository.loadVRMFromUri(uri)
            result.onSuccess { vrmModel ->
                _currentAvatar.value = vrmModel
                _avatarState.value = AvatarState(
                    model = vrmModel,
                    transform = Transform.identity(),
                    currentExpression = null,
                    currentPose = null,
                    isVisible = true
                )
            }
        } catch (e: VRMLoadingError) {
            handleVRMError(e)
        }
    }
}
```

### Controlling Avatar Transform

```kotlin
// In UI gesture handler
fun handlePanGesture(deltaX: Float, deltaY: Float) {
    avatarController.updatePosition(deltaX * 0.01f, deltaY * 0.01f, 0f)
    
    // Update state
    val currentTransform = avatarState.value.transform
    val newTransform = currentTransform.copy(
        position = currentTransform.position + Vector3(deltaX * 0.01f, deltaY * 0.01f, 0f)
    )
    updateAvatarTransform(newTransform)
}
```

## Performance Considerations

### Memory Management
- VRM models are loaded lazily and cached efficiently
- Textures are compressed and managed by Filament
- Unused resources are automatically garbage collected

### Rendering Optimization
- LOD system automatically adjusts model detail based on distance
- Frustum culling eliminates off-screen rendering
- Batch rendering minimizes draw calls

### Threading
- All VRM loading operations are performed on background threads
- UI updates are dispatched to main thread via StateFlow
- AR rendering occurs on dedicated render thread

## Integration Points

### CameraX Integration
The AR avatar system integrates with existing CameraX infrastructure:

```kotlin
// Extend existing CameraViewModel
class CameraViewModel @Inject constructor(
    cameraRepository: CameraRepository,
    private val vrmRepository: VRMRepository,  // New dependency
    private val arRepository: ARRepository     // New dependency
) : ViewModel()
```

### Compose UI Integration
AR avatar controls integrate seamlessly with existing Compose UI:

```kotlin
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    // Existing camera UI
    CameraPreview()
    
    // New AR avatar overlay
    if (viewModel.isARMode.collectAsState().value) {
        AvatarOverlay(
            avatarState = viewModel.avatarState.collectAsState().value,
            onAvatarTransform = viewModel::updateAvatarTransform
        )
    }
}
```

## Testing APIs

### Unit Testing
```kotlin
@Test
fun `loadVRMFromUri should return success for valid file`() = runTest {
    // Arrange
    val mockUri = mockk<Uri>()
    val expectedModel = createTestVRMModel()
    
    // Act
    val result = vrmRepository.loadVRMFromUri(mockUri)
    
    // Assert
    assertTrue(result.isSuccess)
    assertEquals(expectedModel, result.getOrNull())
}
```

### Integration Testing
```kotlin
@Test
fun `AR avatar should render correctly with valid VRM model`() {
    // Test AR rendering pipeline
    val renderer = FilamentARRenderer()
    val vrmModel = loadTestVRMModel()
    
    renderer.initialize(mockSurface, mockARSession)
    renderer.renderAvatar(vrmModel, Transform.identity())
    
    // Verify rendering output
    val bitmap = renderer.captureFrame()
    assertNotNull(bitmap)
}
```

## Version Compatibility

- **Minimum Android API**: 25 (Android 7.1)
- **Target Android API**: 36 (Android 15)
- **ARCore Version**: 1.30+
- **Filament Version**: 1.40+
- **VRM Specification**: 0.0, 1.0

## Migration Guide

When upgrading from previous versions:

1. **Data Migration**: Existing avatar library will be automatically migrated
2. **API Changes**: Check deprecated methods and update accordingly
3. **Permission Updates**: New AR permissions may require user consent
4. **Performance**: Review performance settings for optimal experience

## Support and Troubleshooting

For common issues and solutions, refer to:
- [AR User Guide](AR_USER_GUIDE.md)
- [VRM Requirements](VRM_REQUIREMENTS.md)
- [Troubleshooting Guide](TROUBLESHOOTING.md)