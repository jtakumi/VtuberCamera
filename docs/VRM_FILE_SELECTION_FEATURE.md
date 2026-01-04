# VRM File Selection Feature

## Overview
This document describes the VRM file selection feature that allows users to import VRM avatar files from their device into the VTuber Camera app.

## Feature Description
The feature allows users to select and import VRM files stored on their device when the "Open Avatar Library" button is pressed in the AR Camera screen.

## Implementation Details

### User Flow
1. User launches the AR Camera screen
2. User sees a message "No Avatar Loaded" with a button "Open Avatar Library" (if no avatar is loaded)
3. User can also click the person icon in the top app bar to access the avatar library
4. Upon navigating to the Avatar Library screen, user can:
   - Click the FAB (Floating Action Button) with a "+" icon
   - Click the "Add" icon in the top app bar
5. This launches the system file picker
6. User selects a VRM file from their device
7. The app imports the VRM file and adds it to the avatar library
8. User can now select and use the imported avatar

### Technical Components

#### 1. **ARCameraScreen.kt** (`app/src/main/java/com/example/vtubercamera/ui/screens/ARCameraScreen.kt`)
- Lines 533-534: "Open Avatar Library" button
- Line 232: Avatar library access button in the top app bar
- Function: `onNavigateToAvatarLibrary: () -> Unit` callback for navigation

#### 2. **AvatarLibraryScreen.kt** (`app/src/main/java/com/example/vtubercamera/ui/screens/AvatarLibraryScreen.kt`)
- Lines 71-79: File picker implementation using `ActivityResultContracts.OpenDocument()`
- Lines 94-106: Import button in top app bar
- Lines 111-125: FAB for importing avatars
- Supported MIME types:
  - `model/gltf-binary` - Binary glTF format (which VRM is based on)
  - `application/octet-stream` - Generic binary format
  - `application/vrm` - VRM-specific MIME type
  - `application/*` - Fallback for all application types

#### 3. **AvatarLibraryViewModel.kt** (`app/src/main/java/com/example/vtubercamera/ui/viewmodels/AvatarLibraryViewModel.kt`)
- Lines 100-143: `importAvatar(uri: Uri)` function
- Handles the import workflow:
  1. Loads VRM from the provided URI
  2. Validates the VRM file
  3. Saves it to the avatar library
  4. Refreshes the library view
  5. Handles errors gracefully

#### 4. **VRMRepository & VRMRepositoryImpl** (`app/src/main/java/com/example/vtubercamera/data/`)
- **VRMRepository.kt**: Interface defining VRM operations
  - Line 23: `loadVRMFromUri(uri: Uri): Result<VRMModel>`
  - Line 32: `saveVRMToLibrary(vrmModel: VRMModel, name: String?): Result<String>`
  
- **VRMRepositoryImpl.kt**: Implementation with comprehensive error handling
  - Lines 99-160: VRM loading with validation
  - Lines 179-228: Saving VRM to library with thumbnail generation
  - Error handling for:
    - File not found
    - Invalid format
    - File size exceeded
    - Permission denied
    - Insufficient memory
    - Parse errors

### File Validation
The implementation includes comprehensive VRM file validation:
- File size limits (100MB maximum)
- Format validation
- VRM version support (1.0 and 0.0)
- Permission checks
- Error recovery

### Permissions
The app requires appropriate storage permissions to access files:
- `READ_EXTERNAL_STORAGE` (for Android 12 and below)
- Media permissions (for Android 13+)

### Error Handling
The implementation provides robust error handling:
- User-friendly error messages
- Error notifications via `ErrorNotificationManager`
- Graceful degradation when imports fail
- Validation before import to prevent corrupt files

## Testing
The feature can be tested through:
1. **Manual Testing**:
   - Navigate to AR Camera screen
   - Click "Open Avatar Library"
   - Click the "+" button
   - Select a valid VRM file
   - Verify the avatar is imported successfully

2. **E2E Tests**:
   - `VRMLoadingE2ETest.kt` contains end-to-end tests for the VRM loading workflow

## Future Enhancements
Potential improvements to consider:
- Drag-and-drop support for VRM files
- Cloud storage integration (Google Drive, Dropbox)
- QR code scanning to download VRM files from URLs
- Batch import of multiple VRM files
- Import progress indicator for large files
- Preview before importing

## Related Files
- `app/src/main/java/com/example/vtubercamera/ui/screens/ARCameraScreen.kt`
- `app/src/main/java/com/example/vtubercamera/ui/screens/AvatarLibraryScreen.kt`
- `app/src/main/java/com/example/vtubercamera/ui/viewmodels/AvatarLibraryViewModel.kt`
- `app/src/main/java/com/example/vtubercamera/data/VRMRepository.kt`
- `app/src/main/java/com/example/vtubercamera/data/VRMRepositoryImpl.kt`
- `app/src/androidTest/java/com/example/vtubercamera/e2e/VRMLoadingE2ETest.kt`

## Conclusion
The VRM file selection feature is fully implemented and ready for use. Users can easily import VRM avatar files from their device through an intuitive interface with comprehensive error handling and validation.
