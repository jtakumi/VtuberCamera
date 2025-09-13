# VTuber Camera - Technology Stack

## Build System
- **Gradle**: 8.12+ with Android Gradle Plugin 8.12.1
- **Kotlin**: 1.9.25 with JVM target 11
- **Android SDK**: Min 25, Target 36, Compile 36
- **JDK**: 11+ required

## Core Technologies
- **Language**: Kotlin 1.9.25
- **UI Framework**: Jetpack Compose 1.7.8 with Compose Compiler 1.5.15
- **Camera**: CameraX 1.4.2 (core, camera2, lifecycle, view, extensions)
- **Architecture**: MVVM + StateFlow pattern
- **Design System**: Material Design 3 (Material3 1.3.2)
- **Image Loading**: Coil 2.7.0 for efficient image processing
- **Dependency Injection**: Hilt 2.52 with Dagger

## Key Dependencies
```kotlin
// CameraX Stack
androidx.camera:camera-core:1.4.2
androidx.camera:camera-camera2:1.4.2
androidx.camera:camera-lifecycle:1.4.2

// Compose Stack  
androidx.compose.ui:ui:1.7.8
androidx.compose.material3:material3:1.3.2
androidx.activity:activity-compose:1.10.1

// Architecture
androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2
androidx.core:core-ktx:1.17.0

// DI
com.google.dagger:hilt-android:2.52
```

## Development Environment
- **IDE**: Android Studio Koala or above
- **Build Features**: ViewBinding, Compose, BuildConfig enabled
- **ProGuard**: Enabled for release builds with optimization

## Common Commands

### Build & Run
```bash
# Sync dependencies
./gradlew sync

# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Development
```bash
# Install debug APK
./gradlew installDebug

# Uninstall app
./gradlew uninstallAll

# Generate signed APK
./gradlew assembleRelease
```

## Testing Framework
- **Unit Tests**: JUnit 4.13.2, Mockito 5.7.0, Kotlinx Coroutines Test
- **UI Tests**: Compose UI Test, Espresso 3.7.0
- **Architecture Testing**: AndroidX Arch Core Testing 2.2.0
- **DI Testing**: Hilt Android Testing 2.52

## Performance Optimizations
- Compose compiler optimization enabled
- R8 code shrinking and obfuscation for release
- Parallel Gradle execution enabled
- Build caching enabled
- Non-transitive R class enabled for faster builds