# AR Avatar Architecture Documentation

## Overview

The AR Avatar feature extends the existing VTuber Camera application with VRM model support and AR rendering capabilities. The architecture follows clean architecture principles with clear separation of concerns across UI, Domain, and Data layers.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose)                       │
├─────────────────────────────────────────────────────────────────┤
│  ARCameraScreen  │  AvatarLibrary  │  AvatarControls  │  Settings │
├─────────────────────────────────────────────────────────────────┤
│                     ViewModels (MVVM)                           │
├─────────────────────────────────────────────────────────────────┤
│  CameraViewModel │  AvatarManager  │  ARRenderer      │  States   │
├─────────────────────────────────────────────────────────────────┤
│                    Domain Layer                                 │
├─────────────────────────────────────────────────────────────────┤
│  VRM Processing  │  AR Tracking    │  3D Rendering    │  Controls │
├─────────────────────────────────────────────────────────────────┤
│                     Data Layer                                  │
├─────────────────────────────────────────────────────────────────┤
│ VRMRepository    │ ARRepository    │ FileRepository   │ Cache     │
├─────────────────────────────────────────────────────────────────┤
│                   External APIs                                 │
├─────────────────────────────────────────────────────────────────┤
│  ARCore SDK      │ Filament Engine │ File System      │ CameraX   │
└─────────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### UI Layer
- **Compose Screens**: Declarative UI components for AR camera, avatar library, and controls
- **ViewModels**: State management and business logic coordination
- **UI State**: Reactive state management using StateFlow and Compose State

### Domain Layer
- **Use Cases**: Business logic for VRM processing, AR operations, and avatar control
- **Models**: Core data structures (VRMModel, AvatarState, Transform)
- **Interfaces**: Abstractions for repositories and external services

### Data Layer
- **Repositories**: Data access abstractions and implementations
- **Data Sources**: Local file system, cache, and external API integrations
- **Mappers**: Data transformation between layers

## Component Architecture

### VRM Processing Pipeline

```
VRM File Input
      ↓
File Validation
      ↓
VRM Parser (glTF/VRM)
      ↓
Mesh Extraction
      ↓
Texture Processing
      ↓
Expression/Pose Data
      ↓
VRMModel Creation
      ↓
Filament Conversion
      ↓
3D Rendering
```

### AR Rendering Pipeline

```
ARCore Session
      ↓
Camera Frame
      ↓
Tracking State
      ↓
Light Estimation
      ↓
Avatar Transform
      ↓
Filament Renderer
      ↓
Frame Composition
      ↓
Display Output
```

## Key Components

### 1. VRM Management System

#### VRMRepository
- **Purpose**: Manages VRM file operations and avatar library
- **Dependencies**: File system, cache, VRM parser
- **Key Methods**: loadVRMFromUri, saveVRMToLibrary, getAvatarLibrary

```kotlin
@Singleton
class VRMRepositoryImpl @Inject constructor(
    private val fileManager: FileManager,
    private val vrmParser: VRMParser,
    private val cacheManager: CacheManager
) : VRMRepository
```

#### VRM Parser
- **Purpose**: Parses VRM files and extracts 3D data
- **Format Support**: VRM 0.0, VRM 1.0, glTF 2.0
- **Output**: Structured VRMModel with mesh, textures, animations

### 2. AR Integration System

#### ARRepository
- **Purpose**: Manages ARCore session and tracking
- **Dependencies**: ARCore SDK, permission manager
- **Key Features**: Session lifecycle, tracking state, light estimation

```kotlin
@Singleton
class ARRepositoryImpl @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : ARRepository
```

#### FilamentARRenderer
- **Purpose**: 3D rendering using Filament engine
- **Integration**: ARCore + Filament + CameraX
- **Features**: PBR materials, lighting, shadows, post-processing

### 3. Avatar Control System

#### AvatarController
- **Purpose**: Manages avatar transformations and animations
- **Input**: Touch gestures, UI controls
- **Output**: Transform updates, animation triggers

#### Transform System
- **Components**: Position (Vector3), Rotation (Quaternion), Scale (Vector3)
- **Operations**: Matrix calculations, interpolation, constraints
- **Performance**: Optimized for real-time updates

### 4. State Management

#### ARCameraViewModel
- **Pattern**: MVVM with reactive programming
- **State**: StateFlow for UI reactivity
- **Lifecycle**: Handles configuration changes and memory management

```kotlin
@HiltViewModel
class ARCameraViewModel @Inject constructor(
    private val vrmRepository: VRMRepository,
    private val arRepository: ARRepository,
    cameraRepository: CameraRepository
) : ViewModel() {
    
    private val _isARMode = MutableStateFlow(false)
    val isARMode: StateFlow<Boolean> = _isARMode.asStateFlow()
    
    private val _currentAvatar = MutableStateFlow<VRMModel?>(null)
    val currentAvatar: StateFlow<VRMModel?> = _currentAvatar.asStateFlow()
    
    private val _avatarState = MutableStateFlow(AvatarState.default())
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()
}
```

## Data Flow

### Avatar Loading Flow

```
User selects VRM file
        ↓
UI triggers ViewModel.loadAvatar()
        ↓
ViewModel calls VRMRepository.loadVRMFromUri()
        ↓
Repository validates file
        ↓
VRM Parser extracts data
        ↓
VRMModel created and cached
        ↓
StateFlow updated
        ↓
UI recomposes with new avatar
        ↓
AR Renderer displays avatar
```

### AR Rendering Flow

```
ARCore provides camera frame
        ↓
AR tracking updates pose
        ↓
Light estimation calculated
        ↓
Avatar transform applied
        ↓
Filament renders 3D scene
        ↓
Frame composed with camera
        ↓
Result displayed to user
```

## Threading Model

### Main Thread
- UI updates and Compose recomposition
- StateFlow emissions
- User interaction handling

### Background Threads
- VRM file loading and parsing
- File I/O operations
- Cache management
- Network operations (if any)

### Render Thread
- Filament 3D rendering
- ARCore frame processing
- GPU operations
- Frame composition

### Coroutine Scopes
- **ViewModelScope**: ViewModel lifecycle-bound operations
- **ApplicationScope**: Long-running background tasks
- **IOScope**: File and network operations

## Memory Management

### VRM Model Caching
- **Strategy**: LRU cache with size limits
- **Eviction**: Based on memory pressure and usage patterns
- **Persistence**: Critical models saved to disk

### Texture Management
- **Compression**: Automatic texture compression for GPU
- **Streaming**: Large textures loaded progressively
- **Cleanup**: Unused textures released immediately

### AR Resources
- **Session Management**: Proper ARCore session lifecycle
- **Frame Buffers**: Efficient buffer reuse
- **GPU Memory**: Monitored and optimized

## Performance Optimizations

### Rendering Optimizations
- **LOD System**: Multiple detail levels based on distance
- **Frustum Culling**: Skip off-screen objects
- **Occlusion Culling**: Skip hidden objects
- **Batch Rendering**: Minimize draw calls

### Memory Optimizations
- **Object Pooling**: Reuse frequently created objects
- **Lazy Loading**: Load resources on demand
- **Memory Monitoring**: Track and optimize usage
- **Garbage Collection**: Minimize allocations

### Battery Optimizations
- **Frame Rate Control**: Adaptive frame rate based on performance
- **Background Processing**: Pause non-essential operations
- **Thermal Management**: Reduce quality under thermal stress

## Error Handling Strategy

### Error Categories
1. **VRM Loading Errors**: File format, corruption, size limits
2. **AR Errors**: Device compatibility, permissions, tracking
3. **Rendering Errors**: GPU issues, memory constraints
4. **System Errors**: Storage, permissions, network

### Recovery Mechanisms
- **Graceful Degradation**: Fallback to lower quality modes
- **User Guidance**: Clear error messages with solutions
- **Automatic Retry**: Intelligent retry with backoff
- **State Recovery**: Restore previous working state

### Error Reporting
- **Local Logging**: Detailed logs for debugging
- **User Feedback**: Optional crash reporting
- **Performance Metrics**: Monitor error rates

## Security Considerations

### File Security
- **Sandboxing**: VRM files processed in isolated environment
- **Validation**: Comprehensive file format validation
- **Size Limits**: Prevent resource exhaustion attacks
- **Malware Detection**: Basic checks for suspicious content

### Privacy Protection
- **Local Processing**: All VRM data processed locally
- **No External Transmission**: Avatar data never leaves device
- **Secure Storage**: Encrypted storage for sensitive data
- **Permission Management**: Minimal required permissions

## Testing Strategy

### Unit Testing
- **Repository Layer**: Mock external dependencies
- **ViewModel Layer**: Test state management and business logic
- **Utility Classes**: Test mathematical operations and transformations

### Integration Testing
- **AR + Camera**: Test ARCore and CameraX integration
- **VRM + Rendering**: Test VRM loading and Filament rendering
- **UI + ViewModel**: Test UI state synchronization

### Performance Testing
- **Memory Usage**: Monitor memory consumption patterns
- **Frame Rate**: Ensure consistent 30+ FPS
- **Battery Impact**: Measure power consumption
- **Thermal Impact**: Test under sustained load

### Device Testing
- **AR Compatibility**: Test on various ARCore-supported devices
- **Performance Tiers**: Test on low, mid, and high-end devices
- **Android Versions**: Test across supported API levels

## Deployment Architecture

### Build Configuration
- **Debug**: Full logging, debug symbols, development features
- **Release**: Optimized, obfuscated, minimal logging
- **Staging**: Production-like with additional monitoring

### Resource Management
- **APK Size**: Minimize through resource optimization
- **Dynamic Delivery**: Consider for large assets
- **Compression**: Optimize textures and models

### Monitoring
- **Performance Metrics**: Frame rate, memory usage, crashes
- **User Analytics**: Feature usage, error rates
- **Device Compatibility**: Track supported device matrix

## Future Architecture Considerations

### Scalability
- **Plugin Architecture**: Support for additional 3D formats
- **Cloud Integration**: Optional cloud avatar storage
- **Multi-Avatar**: Support for multiple simultaneous avatars

### Performance
- **Vulkan Support**: Next-generation graphics API
- **Machine Learning**: AI-powered avatar animation
- **Streaming**: Real-time avatar streaming capabilities

### Platform Expansion
- **Cross-Platform**: Shared business logic for iOS
- **Desktop**: Windows/Mac support considerations
- **Web**: WebXR integration possibilities

## Conclusion

The AR Avatar architecture is designed for maintainability, performance, and extensibility. The clean separation of concerns allows for independent testing and development of each layer, while the reactive programming model ensures consistent UI updates and optimal user experience.

Key architectural strengths:
- **Modularity**: Clear component boundaries
- **Testability**: Comprehensive testing strategy
- **Performance**: Optimized for mobile constraints
- **Maintainability**: Clean code and documentation
- **Extensibility**: Designed for future enhancements