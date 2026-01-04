# VRM File Selection - Developer Quick Reference

## Quick Facts
- **Feature Status**: ✅ Fully Implemented
- **Location**: Avatar Library Screen
- **Implementation**: ActivityResultContracts.OpenDocument()
- **Supported Formats**: .vrm (VRM 1.0, VRM 0.0)
- **File Size Limit**: 100MB
- **MIME Types**: `model/gltf-binary`, `application/octet-stream`, `application/vrm`, `application/*`

## Key Files

### UI Components
```
app/src/main/java/com/example/vtubercamera/ui/screens/
├── ARCameraScreen.kt          (Lines 533-534: "Open Avatar Library" button)
└── AvatarLibraryScreen.kt     (Lines 71-79: File picker implementation)
```

### ViewModel
```
app/src/main/java/com/example/vtubercamera/ui/viewmodels/
└── AvatarLibraryViewModel.kt  (Lines 100-143: importAvatar() method)
```

### Repository
```
app/src/main/java/com/example/vtubercamera/data/
├── VRMRepository.kt           (Interface)
└── VRMRepositoryImpl.kt       (Lines 99-160: loadVRMFromUri())
                               (Lines 179-228: saveVRMToLibrary())
```

### Tests
```
app/src/test/java/com/example/vtubercamera/data/
└── VRMRepositoryTest.kt       (Unit tests for validation)

app/src/androidTest/java/com/example/vtubercamera/e2e/
└── VRMLoadingE2ETest.kt       (E2E tests for VRM loading)
```

## Code Snippets

### 1. Launching File Picker
```kotlin
// In AvatarLibraryScreen.kt
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri != null) {
        viewModel.importAvatar(uri)
    } else {
        viewModel.hideImportDialog()
    }
}

// Trigger file picker
importLauncher.launch(
    arrayOf(
        "model/gltf-binary",
        "application/octet-stream",
        "application/vrm",
        "application/*"
    )
)
```

### 2. Import Processing
```kotlin
// In AvatarLibraryViewModel.kt
fun importAvatar(uri: Uri) {
    viewModelScope.launch {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load VRM from the given Uri
            val loadResult = vrmRepository.loadVRMFromUri(uri)
            loadResult.fold(
                onSuccess = { model ->
                    // Save to library
                    val saveResult = vrmRepository.saveVRMToLibrary(model)
                    if (saveResult.isSuccess) {
                        refreshLibrary()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to save avatar"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load VRM: ${error.message}"
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Avatar import failed: ${e.message}"
                )
            }
        } finally {
            hideImportDialog()
        }
    }
}
```

### 3. VRM Loading with Validation
```kotlin
// In VRMRepositoryImpl.kt
override suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel> = withContext(Dispatchers.IO) {
    val context = ErrorContext.vrmLoading(uri.toString())
    
    try {
        // First validate the file
        val validationResult = validateVRMFile(uri)
        if (validationResult.isInvalid()) {
            val errors = (validationResult as ValidationResult.Invalid).errors
            val criticalErrors = errors.filter { it.isCritical() }
            if (criticalErrors.isNotEmpty()) {
                val error = VRMLoadingError.ParseError(criticalErrors.first().message)
                val errorState = errorHandler.handleVRMError(error, context)
                errorNotificationManager.showErrorNotification(errorState)
                return@withContext Result.failure(error)
            }
        }
        
        // Read file content
        val inputStream = this@VRMRepositoryImpl.context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            val error = VRMLoadingError.FileNotFound
            return@withContext Result.failure(error)
        }
        
        val fileBytes = inputStream.use { it.readBytes() }
        
        // Parse VRM file
        val vrmModel = parseVRMFile(fileBytes, uri.toString())
        
        Result.success(vrmModel)
        
    } catch (e: SecurityException) {
        Result.failure(VRMLoadingError.PermissionDenied)
    } catch (e: IOException) {
        Result.failure(VRMLoadingError.IOError(e.message ?: "Unknown IO error"))
    } catch (e: OutOfMemoryError) {
        Result.failure(VRMLoadingError.InsufficientMemory)
    } catch (e: Exception) {
        Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
    }
}
```

### 4. Saving to Library
```kotlin
// In VRMRepositoryImpl.kt
override suspend fun saveVRMToLibrary(vrmModel: VRMModel, name: String?): Result<String> = 
    withContext(Dispatchers.IO) {
        try {
            val avatarId = UUID.randomUUID().toString()
            val avatarName = name ?: vrmModel.name
            val fileName = "${avatarId}.vrm"
            val avatarFile = File(avatarLibraryDir, fileName)
            
            // Save VRM file to library directory
            FileOutputStream(avatarFile).use { output ->
                output.write(vrmModel.meshData)
            }
            
            // Generate thumbnail
            val thumbnailPath = thumbnailGenerator.generateThumbnail(vrmModel, avatarId)
            
            // Create avatar info
            val avatarInfo = AvatarInfo(
                id = avatarId,
                name = avatarName,
                originalFileName = vrmModel.name,
                thumbnailPath = thumbnailPath ?: "",
                filePath = avatarFile.absolutePath,
                fileSize = vrmModel.meshData.size.toLong(),
                metadata = vrmModel.metadata,
                availableExpressions = vrmModel.getExpressionNames(),
                availablePoses = vrmModel.getPoseNames(),
                version = vrmModel.version,
                dateAdded = System.currentTimeMillis(),
                dateLastUsed = System.currentTimeMillis()
            )
            
            // Add to library and persist
            val currentLibrary = _avatarLibrary.value.toMutableList()
            currentLibrary.add(avatarInfo)
            _avatarLibrary.value = currentLibrary
            saveAvatarLibraryToDisk()
            
            Result.success(avatarId)
            
        } catch (e: IOException) {
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to save VRM"))
        } catch (e: Exception) {
            Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
        }
    }
```

## Error Types

```kotlin
sealed class VRMLoadingError : Exception() {
    object FileNotFound : VRMLoadingError()
    object InvalidFormat : VRMLoadingError()
    object FileSizeExceeded : VRMLoadingError()
    object PermissionDenied : VRMLoadingError()
    object InsufficientMemory : VRMLoadingError()
    data class IOError(override val message: String) : VRMLoadingError()
    data class ParseError(override val message: String) : VRMLoadingError()
}
```

## Validation Result

```kotlin
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    
    fun isValid(): Boolean = this is Valid
    fun isInvalid(): Boolean = this is Invalid
    fun hasCriticalErrors(): Boolean = 
        (this as? Invalid)?.errors?.any { it.isCritical() } ?: false
}
```

## Testing

### Unit Test Example
```kotlin
@Test
fun `importAvatar should load and save VRM successfully`() = runTest {
    // Given
    val mockUri = mock<Uri>()
    val mockVRMModel = VRMModel(/* ... */)
    
    whenever(vrmRepository.loadVRMFromUri(mockUri))
        .thenReturn(Result.success(mockVRMModel))
    whenever(vrmRepository.saveVRMToLibrary(mockVRMModel))
        .thenReturn(Result.success("avatar-id"))
    
    // When
    viewModel.importAvatar(mockUri)
    
    // Then
    verify(vrmRepository).loadVRMFromUri(mockUri)
    verify(vrmRepository).saveVRMToLibrary(mockVRMModel)
    verify(vrmRepository).getAvatarLibrary()
}
```

### E2E Test Example
```kotlin
@Test
fun vrmLoadingWorkflow_fromFileToDisplay_shouldWork() {
    // Navigate to avatar library
    composeTestRule.onNodeWithContentDescription("Avatar Library")
        .performClick()
    
    // Click import button
    composeTestRule.onNodeWithContentDescription("Import Avatar")
        .performClick()
    
    // File picker will open (handled by system)
    // After selection, verify avatar appears in library
    composeTestRule.waitForIdle()
    
    // Verify avatar is displayed
    composeTestRule.onNodeWithText("My Avatar")
        .assertIsDisplayed()
}
```

## Permissions Required

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />

<!-- For Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

## Dependencies

```kotlin
// app/build.gradle
dependencies {
    // Activity Result API
    implementation "androidx.activity:activity-compose:1.x.x"
    
    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.x.x"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x.x"
    
    // Dependency Injection
    implementation "com.google.dagger:hilt-android:2.x.x"
    kapt "com.google.dagger:hilt-compiler:2.x.x"
}
```

## Common Issues & Solutions

### Issue: File picker doesn't open
**Solution**: Ensure proper MIME types are specified and permissions are granted

### Issue: Import fails with "Permission Denied"
**Solution**: Request and verify READ_EXTERNAL_STORAGE permission

### Issue: "File too large" error
**Solution**: File exceeds 100MB limit. Consider compressing or using a smaller file

### Issue: "Invalid format" error
**Solution**: Ensure file is a valid VRM file (glTF with VRM extensions)

### Issue: Out of memory during import
**Solution**: Close other apps to free memory, or reduce VRM file complexity

## Performance Considerations

1. **File Loading**: Done on IO dispatcher to avoid blocking main thread
2. **Validation**: Happens before full file load to fail fast
3. **Thumbnail Generation**: Async operation, doesn't block import
4. **State Flow**: Reactive updates minimize unnecessary recompositions
5. **Error Recovery**: Graceful handling prevents app crashes

## Best Practices

1. ✅ Always validate files before loading
2. ✅ Use Result types for error handling
3. ✅ Show loading states during async operations
4. ✅ Provide clear error messages to users
5. ✅ Clean up resources (close streams, etc.)
6. ✅ Use coroutines for async file operations
7. ✅ Handle all error scenarios gracefully
8. ✅ Test with various file sizes and formats

## Related Documentation
- [VRM File Selection Feature](./VRM_FILE_SELECTION_FEATURE.md) - Complete technical documentation
- [User Guide](./USER_GUIDE_VRM_IMPORT.md) - User-facing documentation
- [Flow Diagram](./VRM_IMPORT_FLOW_DIAGRAM.md) - Visual flow diagrams
