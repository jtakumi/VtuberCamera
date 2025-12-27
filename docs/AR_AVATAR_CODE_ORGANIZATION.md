# AR Avatar Code Organization Guide

## Overview

This document provides guidelines for code organization, documentation standards, and best practices for the AR Avatar feature in VTuber Camera. It ensures maintainability, readability, and consistency across the codebase.

## Package Structure

### Core VRM Packages

```
com.example.vtubercamera.data.vrm/
├── models/                          # Data models and entities
│   ├── VRMModel.kt                 # Core VRM model representation
│   ├── AvatarInfo.kt               # Avatar library metadata
│   ├── AvatarState.kt              # Runtime avatar state
│   ├── Expression.kt               # VRM expression data
│   ├── Pose.kt                     # VRM pose data
│   └── VRMMetadata.kt              # VRM metadata and licensing
├── math/                           # Mathematical utilities
│   ├── Vector3.kt                  # 3D vector operations
│   ├── Quaternion.kt               # Quaternion for rotations
│   ├── Transform.kt                # 3D transformation matrix
│   └── MathUtils.kt                # Common math operations
├── repositories/                   # Data access layer
│   ├── VRMRepository.kt            # VRM repository interface
│   ├── VRMRepositoryImpl.kt        # VRM repository implementation
│   ├── ARRepository.kt             # AR session repository interface
│   └── ARRepositoryImpl.kt         # AR session repository implementation
├── rendering/                      # 3D rendering components
│   ├── ARRenderer.kt               # AR renderer interface
│   ├── FilamentARRenderer.kt       # Filament-based renderer
│   ├── VRMFilamentConverter.kt     # VRM to Filament conversion
│   ├── FilamentMaterialManager.kt  # Material management
│   ├── FilamentTextureManager.kt   # Texture management
│   └── FilamentShaderManager.kt    # Shader management
├── lighting/                       # Lighting and shadows
│   ├── LightingSystem.kt           # Lighting calculations
│   ├── ShadowSystem.kt             # Shadow rendering
│   └── LightingParameters.kt       # Lighting configuration
├── controllers/                    # Business logic controllers
│   ├── AvatarController.kt         # Avatar manipulation
│   └── ARCameraController.kt       # AR camera control
├── validation/                     # File validation
│   ├── VRMValidator.kt             # VRM file validation
│   └── ValidationResult.kt         # Validation results
├── errors/                         # Error handling
│   ├── VRMLoadingError.kt          # VRM loading errors
│   ├── ARError.kt                  # AR-related errors
│   ├── ErrorHandler.kt             # Error handling logic
│   ├── ErrorRecoveryManager.kt     # Error recovery strategies
│   └── ErrorNotificationManager.kt # Error notifications
├── performance/                    # Performance monitoring
│   ├── PerformanceMonitor.kt       # Performance metrics
│   ├── PerformanceOptimizer.kt     # Performance optimization
│   ├── FrameRateMonitor.kt         # Frame rate monitoring
│   └── BatteryMonitor.kt           # Battery usage monitoring
└── utils/                          # Utility classes
    ├── VRMFileUtils.kt             # File operations
    ├── ThumbnailGenerator.kt       # Thumbnail generation
    └── CacheManager.kt             # Caching utilities
```

### UI Packages

```
com.example.vtubercamera.ui.screens/
├── ARCameraScreen.kt               # Main AR camera screen
├── AvatarLibraryScreen.kt          # Avatar library management
└── AvatarSettingsScreen.kt         # Avatar configuration

com.example.vtubercamera.ui.components/
├── AvatarOverlay.kt                # AR avatar overlay
├── AvatarControlPanel.kt           # Avatar manipulation controls
├── LightingControlPanel.kt         # Lighting adjustment controls
├── ExpressionSelector.kt           # Expression selection UI
├── PoseSelector.kt                 # Pose selection UI
└── PerformanceMonitorPanel.kt      # Performance monitoring UI

com.example.vtubercamera.ui.viewmodels/
├── ARCameraViewModel.kt            # AR camera view model
├── AvatarLibraryViewModel.kt       # Avatar library view model
└── AvatarSettingsViewModel.kt      # Avatar settings view model
```

## Documentation Standards

### KDoc Guidelines

#### Class Documentation
```kotlin
/**
 * Brief description of the class purpose.
 * 
 * Detailed description explaining:
 * - What the class does
 * - Key features and capabilities
 * - Usage patterns and examples
 * - Important constraints or limitations
 * 
 * Usage example:
 * ```kotlin
 * val controller = AvatarController()
 * controller.loadModel(vrmModel)
 * controller.setPosition(Vector3(1f, 0f, -2f))
 * ```
 * 
 * @param dependency1 Description of constructor parameter
 * @param dependency2 Description of constructor parameter
 * 
 * @author VTuber Camera Team
 * @since 1.0.0
 * 
 * @see RelatedClass
 * @see RelatedInterface
 */
class ExampleClass @Inject constructor(
    private val dependency1: Dependency1,
    private val dependency2: Dependency2
) {
```

#### Method Documentation
```kotlin
/**
 * Brief description of what the method does.
 * 
 * Detailed description explaining:
 * - Method behavior and side effects
 * - Parameter validation and constraints
 * - Return value details
 * - Error conditions and exceptions
 * 
 * @param param1 Description of parameter including valid ranges/values
 * @param param2 Description of parameter with constraints
 * @return Description of return value and possible states
 * 
 * @throws ExceptionType Description of when this exception is thrown
 * @throws AnotherException Description of another exception condition
 * 
 * @see relatedMethod
 * @since 1.0.0
 */
suspend fun exampleMethod(param1: String, param2: Int): Result<String> {
```

#### Property Documentation
```kotlin
/**
 * Brief description of the property.
 * 
 * Detailed description including:
 * - What the property represents
 * - Valid values or ranges
 * - When it changes
 * - Thread safety considerations
 * 
 * @since 1.0.0
 */
val exampleProperty: StateFlow<String> = _exampleProperty.asStateFlow()
```

### Code Comments

#### Inline Comments
```kotlin
// Explain complex logic or non-obvious behavior
val result = complexCalculation() // Brief explanation of what this does

// TODO: Implement feature X when dependency Y is available
// FIXME: Handle edge case when input is null
// NOTE: This workaround is needed for Android API < 26
```

#### Block Comments
```kotlin
/*
 * Multi-line explanation for complex algorithms or business logic.
 * 
 * This block explains:
 * 1. The algorithm being used
 * 2. Why this approach was chosen
 * 3. Any trade-offs or limitations
 * 4. References to external documentation
 */
```

## Naming Conventions

### Classes and Interfaces
```kotlin
// Classes: PascalCase with descriptive names
class VRMModelLoader
class FilamentARRenderer
class AvatarController

// Interfaces: PascalCase, often ending with -able or starting with I-
interface ARRenderer
interface Drawable
interface IVRMRepository

// Data classes: PascalCase, descriptive of the data
data class AvatarState
data class LightingParameters
data class RenderingStatistics
```

### Methods and Properties
```kotlin
// Methods: camelCase with verb-noun pattern
fun loadVRMModel()
fun updateAvatarTransform()
fun calculateLightingParameters()

// Properties: camelCase, descriptive nouns
val currentAvatar: VRMModel?
val isInitialized: Boolean
val renderingStatistics: RenderingStatistics

// Boolean properties: is/has/can/should prefix
val isVisible: Boolean
val hasExpressions: Boolean
val canRender: Boolean
val shouldUpdate: Boolean
```

### Constants and Enums
```kotlin
// Constants: SCREAMING_SNAKE_CASE
companion object {
    private const val TAG = "FilamentARRenderer"
    private const val MAX_FILE_SIZE = 100 * 1024 * 1024L
    private const val DEFAULT_SCALE = 1.0f
}

// Enums: PascalCase for enum class, SCREAMING_SNAKE_CASE for values
enum class RenderingQuality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}
```

## Error Handling Patterns

### Result Pattern
```kotlin
/**
 * Use Result<T> for operations that can fail
 */
suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel> {
    return try {
        val model = parseVRMFile(uri)
        Result.success(model)
    } catch (e: VRMLoadingError) {
        Result.failure(e)
    }
}
```

### Sealed Class Errors
```kotlin
/**
 * Define specific error types for different failure modes
 */
sealed class VRMLoadingError : Exception() {
    object FileNotFound : VRMLoadingError()
    object InvalidFormat : VRMLoadingError()
    data class ParseError(val details: String) : VRMLoadingError()
}
```

### Error Context
```kotlin
/**
 * Provide context for error handling and recovery
 */
data class ErrorContext(
    val operation: String,
    val resourceId: String?,
    val timestamp: Long = System.currentTimeMillis()
)
```

## Performance Considerations

### Memory Management
```kotlin
/**
 * Use lifecycle-aware components for resource management
 */
class ResourceManager : LifecycleObserver {
    
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        // Release non-essential resources
        clearCache()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        // Release all resources
        cleanup()
    }
}
```

### Coroutine Usage
```kotlin
/**
 * Use appropriate dispatchers for different operations
 */
class VRMRepository {
    
    // I/O operations
    suspend fun loadFile() = withContext(Dispatchers.IO) {
        // File operations
    }
    
    // CPU-intensive operations
    suspend fun parseVRM() = withContext(Dispatchers.Default) {
        // Parsing logic
    }
    
    // UI updates
    suspend fun updateUI() = withContext(Dispatchers.Main) {
        // UI updates
    }
}
```

## Testing Patterns

### Unit Test Structure
```kotlin
/**
 * Follow Given-When-Then pattern for test clarity
 */
@Test
fun `loadVRMFromUri should return success for valid file`() = runTest {
    // Given
    val mockUri = mockk<Uri>()
    val expectedModel = createTestVRMModel()
    every { fileSystem.readFile(mockUri) } returns validVRMData
    
    // When
    val result = repository.loadVRMFromUri(mockUri)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(expectedModel, result.getOrNull())
}
```

### Integration Test Structure
```kotlin
/**
 * Test component interactions
 */
@Test
fun `AR renderer should display avatar correctly`() {
    // Setup
    val renderer = createRenderer()
    val vrmModel = loadTestModel()
    
    // Execute
    renderer.initialize(mockSurface, mockARSession)
    renderer.renderAvatar(vrmModel, Transform.identity())
    
    // Verify
    val frame = renderer.captureFrame()
    assertNotNull(frame)
    assertTrue(frame.containsAvatar())
}
```

## Code Review Checklist

### Documentation
- [ ] All public classes have comprehensive KDoc
- [ ] All public methods have parameter and return documentation
- [ ] Complex algorithms have explanatory comments
- [ ] Error conditions are documented
- [ ] Usage examples are provided where helpful

### Code Quality
- [ ] Naming follows established conventions
- [ ] Methods have single responsibility
- [ ] Classes have clear, focused purpose
- [ ] Error handling is comprehensive
- [ ] Resource cleanup is proper

### Performance
- [ ] Appropriate use of coroutines and dispatchers
- [ ] Memory allocations are minimized
- [ ] Resources are properly managed
- [ ] Performance-critical paths are optimized

### Testing
- [ ] Unit tests cover main functionality
- [ ] Error cases are tested
- [ ] Integration tests verify component interaction
- [ ] Performance tests validate requirements

## Maintenance Guidelines

### Version Management
```kotlin
/**
 * Use @since tags to track API additions
 */
@since("1.0.0")
fun originalMethod()

@since("1.1.0")
fun newMethod()

@Deprecated("Use newMethod() instead", ReplaceWith("newMethod()"))
@since("1.0.0")
fun oldMethod()
```

### Backward Compatibility
```kotlin
/**
 * Maintain backward compatibility with deprecation warnings
 */
@Deprecated(
    message = "Use loadVRMFromUri(Uri) instead",
    replaceWith = ReplaceWith("loadVRMFromUri(uri)"),
    level = DeprecationLevel.WARNING
)
fun loadVRM(path: String): Result<VRMModel> {
    return loadVRMFromUri(Uri.fromFile(File(path)))
}
```

### Migration Guides
```kotlin
/**
 * Provide clear migration paths for API changes
 */
// Old API (deprecated)
fun setAvatarPosition(x: Float, y: Float, z: Float)

// New API (recommended)
fun setAvatarPosition(position: Vector3)

// Migration helper
fun setAvatarPosition(x: Float, y: Float, z: Float) {
    setAvatarPosition(Vector3(x, y, z))
}
```

## Conclusion

Following these code organization and documentation standards ensures:

1. **Maintainability**: Clear structure and documentation make code easy to understand and modify
2. **Consistency**: Uniform naming and patterns across the codebase
3. **Quality**: Comprehensive error handling and testing practices
4. **Performance**: Optimized resource usage and memory management
5. **Collaboration**: Clear guidelines for team development

Regular code reviews should verify adherence to these standards, and the guidelines should be updated as the project evolves.