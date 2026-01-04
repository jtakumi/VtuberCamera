# VRM File Selection Feature - Flow Diagram

## User Interaction Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          VRM File Import Workflow                           │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  User launches  │
│  AR Camera      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│  ARCameraScreen                 │
│  ┌───────────────────────────┐  │
│  │ No Avatar Loaded          │  │
│  │                           │  │         Alternative path:
│  │ [Open Avatar Library]     │  │         ┌────────────────┐
│  └───────────────────────────┘  │◄────────┤ Click person   │
│                                  │         │ icon (👤) in   │
└──────────────┬──────────────────┘         │ top app bar    │
               │                             └────────────────┘
               ▼
┌─────────────────────────────────┐
│  AvatarLibraryScreen            │
│  ┌───────────────────────────┐  │
│  │ Avatar Library      + ⟳   │  │
│  ├───────────────────────────┤  │
│  │ No avatars in library     │  │
│  │                           │  │
│  │   [+ Import Avatar]       │  │
│  │                           │  │
│  │                      (+)  │  │◄─── Click FAB or + icon
│  └───────────────────────────┘  │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  System File Picker             │
│  ┌───────────────────────────┐  │
│  │ Select a file             │  │
│  ├───────────────────────────┤  │
│  │ 📁 Downloads              │  │
│  │   📄 my_avatar.vrm        │◄─── User selects VRM file
│  │   📄 character.vrm        │  │
│  │                           │  │
│  │ 📁 Documents              │  │
│  └───────────────────────────┘  │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  importLauncher (ActivityResultContracts.OpenDocument)          │
│  - Receives selected file URI                                   │
│  - Calls viewModel.importAvatar(uri)                            │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  AvatarLibraryViewModel.importAvatar(uri)                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 1. Set loading state                                     │   │
│  │ 2. Call vrmRepository.loadVRMFromUri(uri) ───────┐       │   │
│  │ 3. On success: vrmRepository.saveVRMToLibrary()  │       │   │
│  │ 4. On success: refreshLibrary()                  │       │   │
│  │ 5. On failure: Show error message                │       │   │
│  │ 6. hideImportDialog()                            │       │   │
│  └──────────────────────────────────────────────────┼───────┘   │
└─────────────────────────────────────────────────────┼───────────┘
                                                      │
                                                      ▼
┌──────────────────────────────────────────────────────────────────────┐
│  VRMRepositoryImpl.loadVRMFromUri(uri)                               │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ 1. Validate VRM file ───────┐                                  │  │
│  │ 2. Check file size          │                                  │  │
│  │ 3. Check permissions        │                                  │  │
│  │ 4. Read file content        │                                  │  │
│  │ 5. Parse VRM structure      │                                  │  │
│  │ 6. Create VRMModel          │                                  │  │
│  │ 7. Return Result<VRMModel>  │                                  │  │
│  └─────────────────────────────┼──────────────────────────────────┘  │
└─────────────────────────────────┼──────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  VRMValidator.validateVRMFile()                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Checks:                                                        │  │
│  │ ✓ File exists and readable                                    │  │
│  │ ✓ Valid glTF/VRM structure                                    │  │
│  │ ✓ File size within limits (< 100MB)                           │  │
│  │ ✓ Supported VRM version (1.0 or 0.0)                          │  │
│  │ ✓ Required VRM extensions present                             │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  VRMRepositoryImpl.saveVRMToLibrary(vrmModel)                        │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ 1. Generate unique avatar ID                                  │  │
│  │ 2. Save VRM file to library directory                         │  │
│  │ 3. Generate thumbnail ───┐                                    │  │
│  │ 4. Create AvatarInfo      │                                   │  │
│  │ 5. Add to library list    │                                   │  │
│  │ 6. Persist metadata       │                                   │  │
│  └───────────────────────────┼────────────────────────────────────┘  │
└───────────────────────────────┼────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  AvatarThumbnailGenerator.generateThumbnail()                        │
│  - Renders VRM model preview                                         │
│  - Saves thumbnail image                                             │
│  - Returns thumbnail path                                            │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Success! Avatar appears in library                                  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Avatar Library                                                 │  │
│  │ ┌──────────────────┐                                           │  │
│  │ │ 👤 My Avatar     │ ◄─── Newly imported avatar               │  │
│  │ │ 2.5 MB           │                                           │  │
│  │ │ Just now         │                                           │  │
│  │ └──────────────────┘                                           │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
┌─────────────────────┐
│  File Selection     │
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────┐
│  Error Scenarios                                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ❌ File Not Found                                          │
│  └─► VRMLoadingError.FileNotFound                          │
│      └─► User sees: "Failed to load VRM: File not found"   │
│                                                              │
│  ❌ Invalid Format                                          │
│  └─► VRMLoadingError.InvalidFormat                         │
│      └─► User sees: "Failed to load VRM: Invalid format"   │
│                                                              │
│  ❌ File Too Large (> 100MB)                                │
│  └─► VRMLoadingError.FileSizeExceeded                      │
│      └─► User sees: "Failed to load VRM: File too large"   │
│                                                              │
│  ❌ Permission Denied                                       │
│  └─► VRMLoadingError.PermissionDenied                      │
│      └─► User sees: "Failed to load VRM: Access denied"    │
│                                                              │
│  ❌ Out of Memory                                           │
│  └─► VRMLoadingError.InsufficientMemory                    │
│      └─► User sees: "Failed to load VRM: Not enough memory"│
│                                                              │
│  ❌ Parse Error                                             │
│  └─► VRMLoadingError.ParseError                            │
│      └─► User sees: "Failed to load VRM: Parse error"      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────┐
│  Error Notification                                          │
│  - ErrorHandler.handleVRMError()                             │
│  - ErrorNotificationManager.showErrorNotification()          │
│  - User sees friendly error message                          │
│  - Import dialog is hidden                                   │
│  - User can try again                                        │
└──────────────────────────────────────────────────────────────┘
```

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐              ┌───────────────────┐          │
│  │ ARCameraScreen   │──navigates──►│ AvatarLibrary     │          │
│  │                  │              │ Screen            │          │
│  │ - Shows avatar   │              │                   │          │
│  │ - Avatar library │              │ - File picker     │          │
│  │   button         │              │ - Avatar list     │          │
│  └──────────────────┘              │ - Import button   │          │
│                                    └─────────┬─────────┘          │
└──────────────────────────────────────────────┼────────────────────┘
                                               │
┌──────────────────────────────────────────────┼────────────────────┐
│                    ViewModel Layer           │                    │
├──────────────────────────────────────────────┼────────────────────┤
│                                              ▼                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ AvatarLibraryViewModel                                      │  │
│  │                                                             │  │
│  │ - importAvatar(uri)                                         │  │
│  │ - selectAvatar(id)                                          │  │
│  │ - deleteAvatar(id)                                          │  │
│  │ - toggleFavorite(id)                                        │  │
│  │ - UI state management                                       │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
└──────────────────────────────┼────────────────────────────────────┘
                               │
┌──────────────────────────────┼────────────────────────────────────┐
│                  Repository Layer (Data)                          │
├──────────────────────────────┼────────────────────────────────────┤
│                              ▼                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ VRMRepository (Interface)                                   │  │
│  │                                                             │  │
│  │ - loadVRMFromUri(uri): Result<VRMModel>                    │  │
│  │ - saveVRMToLibrary(model): Result<String>                  │  │
│  │ - getAvatarLibrary(): Flow<List<AvatarInfo>>               │  │
│  │ - deleteAvatar(id): Result<Unit>                           │  │
│  │ - validateVRMFile(uri): ValidationResult                   │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
│                              │                                     │
│                              ▼                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ VRMRepositoryImpl                                           │  │
│  │                                                             │  │
│  │ Dependencies:                                               │  │
│  │ - AvatarThumbnailGenerator                                 │  │
│  │ - ErrorHandler                                             │  │
│  │ - ErrorNotificationManager                                 │  │
│  │                                                             │  │
│  │ Features:                                                   │  │
│  │ - File I/O operations                                      │  │
│  │ - VRM parsing                                              │  │
│  │ - Validation                                               │  │
│  │ - Error handling                                           │  │
│  │ - Thumbnail generation                                     │  │
│  └─────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────┐
│                      Supporting Services                          │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ VRMValidator     │  │ AvatarThumbnail  │  │ ErrorHandler  │  │
│  │                  │  │ Generator        │  │               │  │
│  │ - File format    │  │                  │  │ - Error       │  │
│  │   validation     │  │ - Render avatar  │  │   context     │  │
│  │ - Size checks    │  │ - Save thumbnail │  │ - Recovery    │  │
│  │ - VRM version    │  │                  │  │   strategies  │  │
│  └──────────────────┘  └──────────────────┘  └───────────────┘  │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User Action → UI Component → ViewModel → Repository → File System
    ↓             ↓              ↓           ↓            ↓
  Select       Launch         Import      Load VRM     Read file
   File        Picker         Avatar      Validate     Parse data
                                Save        Generate    Store file
                                Library     Thumbnail
                                            
                ↓              ↓           ↓            ↓
  Result  ←  UI Update  ←  State Flow ←  Result   ←  Success/Error
```
