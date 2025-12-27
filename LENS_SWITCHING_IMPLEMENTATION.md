# Pinch-to-Switch Lens Implementation

## Overview

This implementation adds pinch-to-switch functionality between normal and wide-angle lenses to the VTuber Camera app. Users can pinch out to switch to wide-angle lens and pinch in to switch back to normal lens.

## Implementation Components

### 1. CameraCapabilityManager (`app/src/main/java/com/example/vtubercamera/utils/CameraCapabilityManager.kt`)

**Purpose**: Detects and manages multiple camera lenses on the device.

**Key Features**:
- Detects available cameras using both Camera2 API and CameraX fallbacks
- Identifies lens types (Normal, Wide-angle, Telephoto, Front)
- Uses focal length and lens characteristics for accurate detection
- Provides fallback strategies for devices with limited camera API support
- Includes permission checks and error handling

**Key Methods**:
- `detectCameraCapabilities()`: Scans device for available cameras
- `canSwitchLens()`: Returns true if multiple rear cameras are available
- `getAlternateRearCamera()`: Gets the alternate camera for switching
- `getLensDisplayName()`: Returns user-friendly lens names

### 2. Enhanced Gesture Handling (`app/src/main/java/com/example/vtubercamera/ui/modifiers/ModernCameraGestures.kt`)

**Purpose**: Extends existing gesture handling to support lens switching via pinch gestures.

**Key Features**:
- Pinch out beyond threshold (default 1.8x) switches to wide-angle
- Pinch in below threshold switches back to normal lens
- Haptic feedback for lens switches
- Gesture state management to prevent repeated triggers
- Error handling to prevent stuck gesture states

**New Parameters**:
- `onLensSwitch`: Callback when lens switch is triggered
- `canSwitchLens`: Whether lens switching is available
- `lensSwitchThreshold`: Sensitivity threshold for switching

### 3. Updated Repository Layer (`app/src/main/java/com/example/vtubercamera/data/`)

**Purpose**: Handles camera session management for lens switching.

**Enhanced Features**:
- `CameraRepository.switchToCamera()`: New method for camera switching
- `CameraRepositoryImpl.switchToCamera()`: Implementation with proper unbinding/rebinding
- Maintains existing flash mode and settings during switch

### 4. Enhanced ViewModel (`app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt`)

**Purpose**: Manages lens switching state and UI updates.

**New State Variables**:
- `canSwitchLens`: Boolean indicating if device supports lens switching
- `currentLensType`: Current active lens type (Normal/Wide-angle)
- `lensDisplayName`: User-friendly display name for current lens

**New Methods**:
- `switchLens()`: Handles lens switching with error handling
- `initializeCameraCapabilities()`: Initializes camera detection
- `isLensSwitchingAvailable()`: Public API for checking availability

### 5. UI Components (`app/src/main/java/com/example/vtubercamera/ui/components/LensIndicator.kt`)

**Purpose**: Provides visual feedback for lens switching.

**Components**:
- `LensIndicator`: Shows current active lens in top-left corner
- `LensSwitchHint`: Shows pinch gesture hint when lens switching is available
- `LensSwitchFeedback`: Animated overlay when switching occurs

**Features**:
- Animated transitions and scaling effects
- Auto-hide timers for temporary feedback
- Consistent with app's design system

### 6. Updated Camera Screen (`app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt`)

**Purpose**: Integrates all lens switching components into the main camera interface.

**Integration Points**:
- Updated `modernCameraGestures` call with lens switching parameters
- Added lens indicator components to camera preview overlay
- State management for lens switch feedback display
- Auto-hide logic for feedback messages

### 7. Dependency Injection (`app/src/main/java/com/example/vtubercamera/di/CameraModule.kt`)

**Purpose**: Provides `CameraCapabilityManager` instance via Hilt.

**Addition**:
- `provideCameraCapabilityManager()`: Singleton provider for capability manager

## Usage Instructions

### For Users:
1. Open the camera app
2. If device has multiple rear cameras, a lens indicator appears in the top-left
3. A pinch gesture hint appears in the top-center
4. **Pinch out** (spread fingers) to switch to wide-angle lens
5. **Pinch in** (bring fingers together) to switch back to normal lens
6. Haptic feedback and visual confirmation appear during switches

### For Developers:

#### Testing Lens Switching:
```kotlin
// Check if lens switching is supported
if (viewModel.isLensSwitchingAvailable()) {
    // Device supports lens switching
    viewModel.switchLens() // Programmatically switch
}

// Get current lens info
val lensInfo = viewModel.getCurrentLensInfo()
Log.d("Camera", "Current lens: $lensInfo")
```

#### Adding Custom Lens Types:
```kotlin
// In CameraCapabilityManager.kt, add to LensType enum:
enum class LensType {
    NORMAL,
    WIDE_ANGLE,
    TELEPHOTO,
    FRONT,
    CUSTOM_LENS // Add new lens type
}
```

## Compatibility and Fallbacks

### Device Support:
- **Android API 21+**: Basic lens detection using CameraX
- **Android API 23+**: Enhanced detection using Camera2 API
- **Android API 28+**: Advanced lens characteristic analysis

### Fallback Strategy:
1. **Primary**: Camera2 API with detailed characteristics analysis
2. **Secondary**: CameraX API with basic lens detection
3. **Fallback**: Single camera mode if detection fails

### Error Handling:
- Permission checks before camera access
- Graceful degradation on unsupported devices
- Gesture state reset on errors
- Comprehensive logging for debugging

## Testing Recommendations

### Manual Testing:
1. Test on devices with multiple rear cameras (iPhone-style camera arrays)
2. Test on devices with single rear camera (should disable lens switching)
3. Test pinch gestures at various speeds and scales
4. Test during video recording, photo capture, and preview modes
5. Test camera permission revocation/restoration
6. Test orientation changes during lens switching

### Edge Cases to Test:
- Rapid repeated pinch gestures
- Pinch gesture during photo capture
- Camera switching with different flash modes
- Memory pressure during lens switching
- Background/foreground transitions
- Device rotation during switch

### Performance Testing:
- Camera session switch time (should be < 1 second)
- Memory usage during switches
- Battery impact of camera capability detection
- UI responsiveness during lens transitions

## Configuration Options

### Gesture Sensitivity:
```kotlin
// In CameraScreen.kt, modify lensSwitchThreshold:
.modernCameraGestures(
    // ... other parameters
    lensSwitchThreshold = 1.8f // Lower = more sensitive
)
```

### Focal Length Thresholds:
```kotlin
// In CameraCapabilityManager.kt, modify determineLensTypeFromFocalLength():
private fun determineLensTypeFromFocalLength(focalLength: Float): LensType {
    return when {
        focalLength < 3.5f -> LensType.WIDE_ANGLE // Adjust threshold
        focalLength < 7.0f -> LensType.NORMAL
        else -> LensType.TELEPHOTO
    }
}
```

### UI Feedback Timing:
```kotlin
// In CameraScreen.kt, modify auto-hide delay:
LaunchedEffect(showLensSwitchFeedback) {
    if (showLensSwitchFeedback) {
        kotlinx.coroutines.delay(1500) // Adjust delay
        showLensSwitchFeedback = false
    }
}
```

## Future Enhancements

1. **Telephoto Support**: Add support for telephoto lenses (>7mm focal length)
2. **Smooth Transitions**: Add crossfade animation during lens switches  
3. **Zoom Integration**: Automatic zoom adjustment when switching lenses
4. **Settings Integration**: Allow users to disable lens switching
5. **Accessibility**: Add TalkBack support for lens switching
6. **Analytics**: Track lens switching usage patterns

## Known Limitations

1. Camera2 API characteristics may not be available on all devices
2. Focal length detection accuracy varies by manufacturer
3. Some devices may report incorrect lens characteristics
4. Lens switching requires camera session restart (brief preview interruption)
5. Wide-angle detection heuristics may need device-specific tuning

## Build Information

- **Minimum SDK**: 25 (Android 7.1)
- **Target SDK**: 36 (Android 15)
- **CameraX Version**: 1.4.2
- **Compile SDK**: 36

## Verification Checklist

- [x] Build compiles successfully
- [x] Camera capability detection implemented
- [x] Pinch gesture handling integrated
- [x] Camera session switching functional
- [x] UI indicators and feedback implemented
- [x] Error handling and fallbacks added
- [x] Dependency injection configured
- [ ] Manual testing on multiple devices
- [ ] Performance optimization
- [ ] Accessibility testing
- [ ] Documentation review

---

**Implementation Date**: September 2025
**Author**: Claude AI Assistant
**Status**: Ready for Testing