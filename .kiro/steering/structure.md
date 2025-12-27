# VTuber Camera - Project Structure

## Package Organization
The project follows standard Android package-by-feature organization under `com.example.vtubercamera`:

```
app/src/main/java/com/example/vtubercamera/
├── MainActivity.kt                    # App entry point
├── VtuberCameraApp.kt                # Hilt Application class
├── data/                             # Data layer
│   ├── CameraRepository.kt           # Camera operations interface
│   ├── CameraRepositoryImpl.kt       # Camera operations implementation
│   ├── MediaRepository.kt            # Media operations interface
│   ├── MediaRepositoryImpl.kt        # Media operations implementation
│   └── PhotoItem.kt                  # Photo data model
├── di/                               # Dependency injection
│   └── CameraModule.kt               # Hilt DI module
├── managers/                         # System managers
│   └── PermissionManager.kt          # Permission handling
├── ui/                               # UI layer
│   ├── components/                   # Reusable UI components
│   ├── modifiers/                    # Custom Compose modifiers
│   ├── screens/                      # Screen composables
│   ├── theme/                        # Material Design 3 theme
│   └── viewmodels/                   # MVVM ViewModels
└── utils/                            # Utility classes
    ├── Android15Features.kt          # Android 15 specific features
    ├── CameraCapabilityManager.kt    # Camera capability detection
    └── PermissionUtils.kt             # Permission utilities
```

## Architecture Patterns

### MVVM + Repository Pattern
- **ViewModels**: Manage UI state using StateFlow
- **Repositories**: Abstract data operations (camera, media)
- **Use Cases**: Business logic encapsulation (when needed)

### Dependency Injection
- **Hilt**: Used for all dependency injection
- **@HiltViewModel**: ViewModels are Hilt-managed
- **@Singleton**: Repositories and managers are singletons

### Compose UI Structure
- **Screens**: Top-level composables representing full screens
- **Components**: Reusable UI components
- **Modifiers**: Custom gesture and behavior modifiers
- **Theme**: Centralized Material Design 3 theming

## File Naming Conventions

### Kotlin Files
- **Activities**: `*Activity.kt` (e.g., `MainActivity.kt`)
- **ViewModels**: `*ViewModel.kt` (e.g., `CameraViewModel.kt`)
- **Repositories**: `*Repository.kt` and `*RepositoryImpl.kt`
- **Composables**: `*Screen.kt` for screens, `*Component.kt` for components
- **Utils**: Descriptive names ending in purpose (e.g., `PermissionUtils.kt`)

### Resource Files
- **Strings**: `strings.xml` (default), `strings.xml` in `values-ja/` for Japanese
- **Colors**: `colors.xml` for theme colors
- **Themes**: `themes.xml` with night mode variants
- **Drawables**: Descriptive names with underscores (e.g., `ic_menu_camera.xml`)

## Resource Organization

### Internationalization
- **Default**: `values/strings.xml` (English)
- **Japanese**: `values-ja/strings.xml`
- **Usage**: Always use `stringResource()` in Compose

### Assets
- **Images**: `drawable/` for vector drawables, `mipmap-*/` for app icons
- **Audio**: `raw/` for sound effects (camera_shutter.mp3, enter_app.mp3)
- **Configuration**: `xml/` for FileProvider paths, backup rules

## Testing Structure
```
app/src/test/                         # Unit tests
└── java/com/example/vtubercamera/
    └── ui/viewmodels/
        └── CameraViewModelTest.kt

app/src/androidTest/                  # Instrumented tests
└── java/com/example/vtubercamera/
    └── ui/screens/
        └── CameraScreenIconTest.kt
```

## Build Configuration
- **App-level**: `app/build.gradle` - dependencies, build types, signing
- **Project-level**: `build.gradle` - plugin versions, global configuration
- **Properties**: `gradle.properties` - build optimization settings
- **Settings**: `settings.gradle` - module configuration

## Documentation
- **Root**: README files in English and Japanese
- **Docs**: `docs/` folder with changelogs and screenshots
- **Implementation**: Separate markdown files for specific features (lens switching, zoom)