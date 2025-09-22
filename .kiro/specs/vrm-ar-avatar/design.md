# Design Document

## Overview

VTuber Camera アプリにVRMアバターを使用したAR撮影機能を追加します。この機能は既存のCameraX基盤の上に、ARCore、Filament 3Dレンダリングエンジン、VRM読み込み機能を統合して実現します。ユーザーは自分のVRMアバターファイルを読み込み、リアルタイムでアバターを操作しながらAR撮影を行うことができます。

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  Domain Layer   │    │   Data Layer    │
│                 │    │                 │    │                 │
│ ARCameraScreen  │◄──►│ ARCameraVM      │◄──►│ VRMRepository   │
│ AvatarLibrary   │    │ AvatarManager   │    │ ARRepository    │
│ AvatarControls  │    │ ARRenderer      │    │ FileRepository  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Compose UI    │    │  Business Logic │    │ External APIs   │
│                 │    │                 │    │                 │
│ Material3 Theme │    │ VRM Processing  │    │ ARCore SDK      │
│ Gesture Handling│    │ AR Tracking     │    │ Filament Engine │
│ Animation       │    │ 3D Rendering    │    │ File System     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Component Integration

既存のCameraXアーキテクチャを拡張し、AR機能を統合：

```
CameraScreen (既存)
    ├── ARCameraScreen (新規)
    │   ├── ARPreviewView (ARCore + CameraX)
    │   ├── AvatarOverlay (Filament 3D)
    │   └── ARControls (Compose UI)
    │
    ├── AvatarLibraryScreen (新規)
    │   ├── VRMFileManager
    │   └── AvatarPreview
    │
    └── CameraViewModel (拡張)
        ├── ARCameraState
        ├── AvatarState
        └── VRMLoader
```

## Components and Interfaces

### 1. VRM Loading and Management

#### VRMRepository Interface
```kotlin
interface VRMRepository {
    suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel>
    suspend fun saveVRMToLibrary(vrmModel: VRMModel, name: String): Result<String>
    suspend fun getAvatarLibrary(): Flow<List<AvatarInfo>>
    suspend fun deleteAvatar(avatarId: String): Result<Unit>
    fun validateVRMFile(uri: Uri): ValidationResult
}
```

#### VRMModel Data Class
```kotlin
data class VRMModel(
    val id: String,
    val name: String,
    val meshData: ByteArray,
    val textureData: Map<String, ByteArray>,
    val expressions: List<Expression>,
    val poses: List<Pose>,
    val metadata: VRMMetadata
)

data class Expression(
    val name: String,
    val blendShapeKeys: Map<String, Float>
)

data class Pose(
    val name: String,
    val boneTransforms: Map<String, Transform>
)
```

### 2. AR Rendering System

#### ARRenderer Interface
```kotlin
interface ARRenderer {
    fun initialize(surface: Surface, arSession: Session)
    fun updateFrame(frame: Frame, avatarState: AvatarState)
    fun renderAvatar(vrmModel: VRMModel, transform: Transform)
    fun setLighting(lightEstimate: LightEstimate)
    fun captureFrame(): Bitmap
    fun cleanup()
}
```

#### FilamentARRenderer Implementation
```kotlin
class FilamentARRenderer : ARRenderer {
    private lateinit var engine: Engine
    private lateinit var scene: Scene
    private lateinit var camera: Camera
    private lateinit var renderer: Renderer
    
    // Filament + ARCore統合実装
}
```

### 3. Avatar Control System

#### AvatarController
```kotlin
class AvatarController {
    fun updatePosition(deltaX: Float, deltaY: Float, deltaZ: Float)
    fun updateRotation(deltaYaw: Float, deltaPitch: Float, deltaRoll: Float)
    fun updateScale(scaleFactor: Float)
    fun setExpression(expression: Expression)
    fun setPose(pose: Pose)
    fun resetToDefault()
}
```

### 4. AR Camera Integration

#### ARCameraViewModel (CameraViewModelの拡張)
```kotlin
@HiltViewModel
class ARCameraViewModel @Inject constructor(
    private val vrmRepository: VRMRepository,
    private val arRepository: ARRepository,
    cameraRepository: CameraRepository
) : ViewModel() {
    
    // 既存のカメラ状態
    private val _isARMode = MutableStateFlow(false)
    val isARMode: StateFlow<Boolean> = _isARMode.asStateFlow()
    
    private val _currentAvatar = MutableStateFlow<VRMModel?>(null)
    val currentAvatar: StateFlow<VRMModel?> = _currentAvatar.asStateFlow()
    
    private val _avatarTransform = MutableStateFlow(Transform.identity())
    val avatarTransform: StateFlow<Transform> = _avatarTransform.asStateFlow()
    
    // AR機能
    fun enableARMode()
    fun disableARMode()
    fun loadAvatar(uri: Uri)
    fun updateAvatarTransform(transform: Transform)
    fun captureARPhoto()
}
```

## Data Models

### Avatar Management
```kotlin
data class AvatarInfo(
    val id: String,
    val name: String,
    val thumbnailPath: String,
    val filePath: String,
    val dateAdded: Long,
    val fileSize: Long
)

data class AvatarState(
    val model: VRMModel?,
    val transform: Transform,
    val currentExpression: Expression?,
    val currentPose: Pose?,
    val isVisible: Boolean
)

data class Transform(
    val position: Vector3,
    val rotation: Quaternion,
    val scale: Vector3
) {
    companion object {
        fun identity() = Transform(
            Vector3.ZERO,
            Quaternion.IDENTITY,
            Vector3.ONE
        )
    }
}
```

### AR Session Management
```kotlin
data class ARSessionState(
    val isInitialized: Boolean,
    val trackingState: TrackingState,
    val lightEstimate: LightEstimate?,
    val planeDetection: Boolean,
    val environmentalHDR: Boolean
)
```

## Error Handling

### VRM Loading Errors
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

### AR Errors
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

### Error Recovery Strategies
1. **VRM読み込み失敗**: ファイル形式チェック、代替ファイル提案、修復ツール案内
2. **ARCore問題**: フォールバック2Dモード、ARCore更新案内
3. **レンダリング問題**: 品質設定の自動調整、デバイス性能チェック
4. **メモリ不足**: アバター品質の自動調整、キャッシュクリア

## Testing Strategy

### Unit Tests
- VRMRepository: ファイル読み込み、バリデーション、キャッシュ管理
- AvatarController: 変形操作、表情制御、ポーズ制御
- ARCameraViewModel: 状態管理、AR/通常モード切り替え

### Integration Tests
- ARCore + CameraX統合: セッション管理、フレーム処理
- Filament + ARCore: 3Dレンダリング、ライティング
- VRM + AR: アバター表示、リアルタイム操作

### UI Tests
- ARカメラ画面: ジェスチャー操作、UI応答性
- アバターライブラリ: ファイル選択、プレビュー表示
- 設定画面: 品質設定、権限管理

### Performance Tests
- メモリ使用量: VRMモデル読み込み時、AR描画時
- フレームレート: 30fps維持、ジッター測定
- バッテリー消費: AR使用時の電力効率

### Device Compatibility Tests
- ARCore対応デバイス: Pixel、Galaxy、OnePlus等
- 性能レベル別: ハイエンド、ミッドレンジ、エントリー
- Android版本: API 25-36対応確認

## Performance Considerations

### Memory Management
- VRMモデルの遅延読み込み
- テクスチャの動的圧縮
- 未使用アセットの自動解放
- メモリプールによる効率的な割り当て

### Rendering Optimization
- LOD（Level of Detail）システム
- フラストラムカリング
- オクルージョンカリング
- バッチレンダリング

### Battery Efficiency
- フレームレート適応制御
- 非アクティブ時の処理停止
- GPUワークロードの最適化
- 熱制御による性能調整

## Security and Privacy

### File Security
- VRMファイルのサンドボックス化
- 悪意のあるファイルの検出
- ファイルサイズ制限の実装
- 安全なファイル解析

### Privacy Protection
- アバターデータのローカル保存
- 外部送信の明示的な同意
- 一時ファイルの確実な削除
- ユーザーデータの暗号化

### Permission Management
- 最小権限の原則
- 段階的な権限要求
- 権限拒否時の適切な対応
- 透明性のある権限説明