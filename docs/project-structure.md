
# VtuberCamera Project Structure

VtuberCameraプロジェクトの全体構造とアーキテクチャを説明します。

## 目次

1. [プロジェクト概要](#プロジェクト概要)
2. [ルートディレクトリ](#ルートディレクトリ)
3. [アーキテクチャパターン](#アーキテクチャパターン)
4. [パッケージ構成](#パッケージ構成)
5. [主要クラス一覧](#主要クラス一覧)

## プロジェクト概要

VtuberCameraは、ARアバター機能を備えたカメラアプリケーションです。VRMモデルの読み込み、表情・ポーズ制御、AR撮影など、VTuber配信に必要な機能を提供します。

**技術スタック:**
- Kotlin
- Jetpack Compose (UI)
- Hilt (DI)
- CameraX (カメラ)
- ARCore (AR機能)
- Filament (3Dレンダリング)
- Coroutines & Flow (非同期処理)

## ルートディレクトリ

```
vtuberCamera/
├── .git/                      # Git version control
├── app/                       # メインアプリケーションモジュール
├── build/                     # ビルド成果物
├── docs/                      # プロジェクトドキュメント
├── gradle/                    # Gradle wrapper files
├── build.gradle               # プロジェクトレベルのGradle設定
├── settings.gradle            # プロジェクト設定
├── gradlew                    # Gradle wrapper (Unix)
├── gradlew.bat               # Gradle wrapper (Windows)
├── local.properties          # ローカル環境設定
├── gradle.properties         # Gradleプロパティ
├── README.md                 # プロジェクト説明 (英語)
├── README.ja.md              # プロジェクト説明 (日本語)
├── ZOOM_IMPLEMENTATION.md    # ズーム実装ドキュメント
└── LENS_SWITCHING_IMPLEMENTATION.md  # レンズ切り替え実装ドキュメント
```

## アーキテクチャパターン

### MVVM + Repository Pattern

```
┌─────────────┐
│    View     │ (Jetpack Compose Screens/Components)
│  (UI Layer) │
└──────┬──────┘
       │ observes StateFlow
       ▼
┌─────────────┐
│  ViewModel  │ (@HiltViewModel)
│ (UI State)  │
└──────┬──────┘
       │ calls
       ▼
┌─────────────┐
│ Repository  │ (@Singleton)
│(Data Layer) │
└──────┬──────┘
       │ accesses
       ▼
┌─────────────┐
│ Data Source │ (File, MediaStore, ARCore, etc.)
└─────────────┘
```

### Dependency Injection (Hilt)

- **@HiltAndroidApp**: Application class
- **@AndroidEntryPoint**: Activities
- **@HiltViewModel**: ViewModels
- **@Singleton**: Repositories, Managers, Services
- **@Module + @InstallIn**: DI modules

### レイヤー構成

1. **UI Layer** (`ui/`)
   - Composable Screens
   - Components
   - ViewModels
   - UI State

2. **Domain Layer** (一部機能で実装)
   - Use Cases (必要に応じて)
   - Business Logic

3. **Data Layer** (`data/`)
   - Repositories (Interface + Implementation)
   - Data Models
   - Data Sources

4. **DI Layer** (`di/`)
   - Hilt Modules

## パッケージ構成

```
app/src/main/java/com/example/vtubercamera/
├── MainActivity.kt                    # アプリのエントリーポイント
├── VtuberCameraApp.kt                # Hiltアプリケーションクラス
│
├── data/                             # データレイヤー
│   ├── CameraRepository.kt           # カメラ操作インターフェース
│   ├── CameraRepositoryImpl.kt       # カメラ操作実装
│   ├── MediaRepository.kt            # メディア操作インターフェース
│   ├── MediaRepositoryImpl.kt        # メディア操作実装
│   ├── VRMRepository.kt              # VRMファイル管理インターフェース
│   ├── VRMRepositoryImpl.kt          # VRMファイル管理実装
│   ├── ARRepository.kt               # AR機能インターフェース
│   ├── ARRepositoryImpl.kt           # AR機能実装
│   ├── ARFallbackManager.kt          # ARフォールバック管理
│   ├── ARTrackingMonitor.kt          # ARトラッキング監視
│   ├── PhotoItem.kt                  # 写真データモデル
│   │
│   ├── performance/                  # パフォーマンス管理
│   │   ├── PerformanceMonitor.kt     # パフォーマンス監視
│   │   ├── PerformanceOptimizer.kt   # パフォーマンス最適化
│   │   ├── PerformanceManager.kt     # パフォーマンス管理統括
│   │   ├── BatteryMonitor.kt         # バッテリー監視
│   │   ├── FrameRateMonitor.kt       # フレームレート監視
│   │   └── PerformanceModels.kt      # パフォーマンスデータモデル
│   │
│   └── vrm/                          # VRM/AR関連データ
│       ├── VRMManager.kt             # VRM管理の中核
│       ├── VRMLoader.kt              # VRMファイルローダー
│       ├── VRMParser.kt              # VRMパーサー
│       ├── VRMValidator.kt           # VRMバリデーター
│       ├── VRMModel.kt               # VRMモデルデータ
│       ├── VRMMetadata.kt            # VRMメタデータ
│       ├── VRMLoadingError.kt        # VRMロードエラー
│       ├── ValidationResult.kt       # バリデーション結果
│       ├── VRMMeshExtractor.kt       # メッシュ抽出
│       ├── VRMTextureExtractor.kt    # テクスチャ抽出
│       ├── VRMExpressionLoader.kt    # 表情データローダー
│       ├── VRMPoseLoader.kt          # ポーズデータローダー
│       ├── VRMFilamentConverter.kt   # Filament変換
│       │
│       ├── AvatarController.kt       # アバター制御
│       ├── AvatarState.kt            # アバター状態
│       ├── AvatarInfo.kt             # アバター情報
│       ├── AvatarLibraryManager.kt   # アバターライブラリ管理
│       ├── AvatarLibraryStats.kt     # ライブラリ統計
│       ├── AvatarThumbnailGenerator.kt # サムネイル生成
│       │
│       ├── Expression.kt             # 表情データモデル
│       ├── ExpressionController.kt   # 表情制御
│       ├── Pose.kt                   # ポーズデータモデル
│       ├── PoseController.kt         # ポーズ制御
│       │
│       ├── ARRenderer.kt             # ARレンダラーインターフェース
│       ├── FilamentARRenderer.kt     # Filament ARレンダラー実装
│       ├── ARSceneManager.kt         # ARシーン管理
│       ├── ARSessionState.kt         # ARセッション状態
│       ├── ARCameraState.kt          # ARカメラ状態
│       ├── ARCameraConfig.kt         # ARカメラ設定
│       ├── ARError.kt                # ARエラー定義
│       │
│       ├── LightingSystem.kt         # ライティングシステム
│       ├── ShadowSystem.kt           # シャドウシステム
│       │
│       ├── FilamentMaterialManager.kt    # Filamentマテリアル管理
│       ├── FilamentShaderManager.kt      # Filamentシェーダー管理
│       ├── FilamentTextureManager.kt     # Filamentテクスチャ管理
│       │
│       ├── ErrorHandler.kt           # エラーハンドラー
│       ├── ErrorNotificationManager.kt # エラー通知管理
│       ├── ErrorRecoveryManager.kt   # エラー復旧管理
│       ├── ErrorState.kt             # エラー状態
│       ├── FileAccessErrorHandler.kt # ファイルアクセスエラー
│       ├── NetworkErrorHandler.kt    # ネットワークエラー
│       │
│       ├── MeshStructures.kt         # メッシュデータ構造
│       └── math/                     # 数学ライブラリ
│           ├── Vector3.kt
│           ├── Quaternion.kt
│           └── Transform.kt
│
├── di/                               # Dependency Injection
│   ├── CameraModule.kt               # カメラ関連DI
│   ├── ARModule.kt                   # AR関連DI
│   └── ARRenderingModule.kt          # ARレンダリングDI
│
├── managers/                         # システムマネージャー
│   ├── PermissionManager.kt          # 権限管理
│   ├── ARPermissionManager.kt        # AR権限管理
│   └── ARSessionManager.kt           # ARセッション管理
│
├── utils/                            # ユーティリティ
│   ├── PermissionUtils.kt            # 権限ユーティリティ
│   ├── CameraCapabilityManager.kt    # カメラ機能管理
│   ├── ARDeviceCompatibility.kt      # ARデバイス互換性
│   └── Android15Features.kt          # Android 15機能
│
└── ui/                               # UIレイヤー
    ├── screens/                      # 画面
    │   ├── CameraScreen.kt           # カメラ画面
    │   ├── ARCameraScreen.kt         # ARカメラ画面
    │   ├── AvatarLibraryScreen.kt    # アバターライブラリ画面
    │   ├── AvatarControlScreen.kt    # アバター制御画面
    │   ├── ARLightingDemoScreen.kt   # ライティングデモ画面
    │   └── PerformanceMonitorDemoScreen.kt # パフォーマンス監視画面
    │
    ├── viewmodels/                   # ViewModels
    │   ├── CameraViewModel.kt        # カメラViewModel
    │   ├── CameraUiState.kt          # カメラUI状態
    │   ├── AvatarLibraryViewModel.kt # アバターライブラリViewModel
    │   ├── AvatarControlViewModel.kt # アバター制御ViewModel
    │   ├── ARLightingDemoViewModel.kt # ライティングデモViewModel
    │   └── PerformanceMonitorDemoViewModel.kt # パフォーマンス監視ViewModel
    │
    ├── components/                   # 再利用可能UIコンポーネント
    │   ├── AsyncImage.kt             # 非同期画像読み込み
    │   ├── AvatarListComponents.kt   # アバターリスト
    │   ├── AvatarSelectorPanel.kt    # アバター選択パネル
    │   ├── AvatarTransformPanel.kt   # アバター変形パネル
    │   ├── CommonDialogs.kt          # 共通ダイアログ
    │   ├── ErrorNotificationComponent.kt # エラー通知
    │   ├── ExpressionControlPanel.kt # 表情制御パネル
    │   ├── ExpressionSelectionMenu.kt # 表情選択メニュー
    │   ├── GalleryComponents.kt      # ギャラリーコンポーネント
    │   ├── LensIndicator.kt          # レンズインジケーター
    │   ├── LightingControlPanel.kt   # ライティング制御パネル
    │   ├── PerformanceMonitorPanel.kt # パフォーマンス監視パネル
    │   ├── PermissionRequestComponent.kt # 権限リクエスト
    │   ├── PhotoDetailComponent.kt   # 写真詳細
    │   ├── PhotoPreviewComponent.kt  # 写真プレビュー
    │   ├── PoseControlPanel.kt       # ポーズ制御パネル
    │   └── PoseSelectionMenu.kt      # ポーズ選択メニュー
    │
    ├── modifiers/                    # カスタムModifiers
    │   └── (ジェスチャー、動作制御など)
    │
    └── theme/                        # テーマ
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 主要クラス一覧

### アプリケーション基盤

| クラス名 | 役割 | アノテーション |
|---------|------|--------------|
| `VtuberCameraApp` | アプリケーションクラス | `@HiltAndroidApp` |
| `MainActivity` | メインアクティビティ | `@AndroidEntryPoint` |

### UI層 - ViewModels

| クラス名 | 役割 | 主な責務 |
|---------|------|---------|
| `CameraViewModel` | カメラ画面の状態管理 | カメラ制御、撮影、AR/通常モード切替、写真管理、アバター統合制御 |
| `AvatarLibraryViewModel` | アバターライブラリ画面 | アバター一覧表示、検索、ソート、インポート、削除 |
| `AvatarControlViewModel` | アバター制御画面 | 表情・ポーズ制御、アバター状態管理 |
| `ARLightingDemoViewModel` | ライティングデモ画面 | ライティング設定、プリセット管理 |
| `PerformanceMonitorDemoViewModel` | パフォーマンス監視画面 | パフォーマンス指標表示、監視制御 |

### UI層 - UI State

| クラス名 | 役割 |
|---------|------|
| `CameraUiState` | カメラ画面のUI状態を保持 |

### データ層 - Repositories

| インターフェース | 実装クラス | 役割 |
|----------------|-----------|------|
| `CameraRepository` | `CameraRepositoryImpl` | カメラ操作（撮影、フォーカス、ズーム、レンズ切替） |
| `MediaRepository` | `MediaRepositoryImpl` | 写真・動画管理、MediaStore操作 |
| `VRMRepository` | `VRMRepositoryImpl` | VRMファイル管理、アバターライブラリ管理 |
| `ARRepository` | `ARRepositoryImpl` | ARセッション管理、トラッキング、平面検出 |

### データ層 - VRM/Avatar管理

| クラス名 | 役割 | スコープ |
|---------|------|---------|
| `VRMManager` | VRM操作の中核、パース・ロード・キャッシュ | `@Singleton` |
| `VRMLoader` | VRMファイルのロード | - |
| `VRMParser` | VRMファイルのパース | - |
| `VRMValidator` | VRMファイルのバリデーション | - |
| `VRMMeshExtractor` | メッシュデータ抽出 | - |
| `VRMTextureExtractor` | テクスチャデータ抽出 | - |
| `VRMExpressionLoader` | 表情データローダー | - |
| `VRMPoseLoader` | ポーズデータローダー | - |
| `VRMFilamentConverter` | FilamentエンジンへのVRM変換 | - |
| `AvatarLibraryManager` | アバターライブラリの高レベル管理 | `@Singleton` |
| `AvatarThumbnailGenerator` | アバターサムネイル生成 | `@Singleton` |
| `AvatarController` | アバター制御の統括 | `@Singleton` |
| `ExpressionController` | 表情制御 | `@Singleton` |
| `PoseController` | ポーズ制御 | `@Singleton` |

### データ層 - AR/レンダリング

| クラス名 | 役割 | スコープ |
|---------|------|---------|
| `ARSceneManager` | ARシーン管理 | `@Singleton` |
| `FilamentARRenderer` | Filamentを使用したARレンダリング | `@Singleton` |
| `FilamentMaterialManager` | Filamentマテリアル管理 | `@Singleton` |
| `FilamentShaderManager` | Filamentシェーダー管理 | `@Singleton` |
| `FilamentTextureManager` | Filamentテクスチャ管理 | `@Singleton` |
| `LightingSystem` | ライティングシステム | `@Singleton` |
| `ShadowSystem` | シャドウシステム | `@Singleton` |

### データ層 - パフォーマンス

| クラス名 | 役割 | スコープ |
|---------|------|---------|
| `PerformanceManager` | パフォーマンス管理統括 | `@Singleton` |
| `PerformanceMonitor` | パフォーマンス監視 | `@Singleton` |
| `PerformanceOptimizer` | パフォーマンス最適化 | `@Singleton` |
| `BatteryMonitor` | バッテリー状態監視 | `@Singleton` |
| `FrameRateMonitor` | フレームレート監視 | `@Singleton` |

### データ層 - エラーハンドリング

| クラス名 | 役割 | スコープ |
|---------|------|---------|
| `ErrorHandler` | エラー処理の統括 | `@Singleton` |
| `ErrorNotificationManager` | エラー通知管理 | `@Singleton` |
| `ErrorRecoveryManager` | エラー復旧処理 | `@Singleton` |
| `FileAccessErrorHandler` | ファイルアクセスエラー処理 | `@Singleton` |
| `NetworkErrorHandler` | ネットワークエラー処理 | `@Singleton` |

### マネージャー層

| クラス名 | 役割 | スコープ |
|---------|------|---------|
| `PermissionManager` | アプリ権限管理 | `@Singleton` |
| `ARPermissionManager` | AR機能権限管理 | `@Singleton` |
| `ARSessionManager` | ARセッションライフサイクル管理 | `@Singleton` |
| `ARFallbackManager` | AR非対応デバイスのフォールバック | `@Singleton` |
| `ARTrackingMonitor` | ARトラッキング状態監視 | - |

### ユーティリティ層

| クラス名 | 役割 |
|---------|------|
| `CameraCapabilityManager` | カメラ機能検出・管理 |
| `ARDeviceCompatibility` | ARデバイス互換性チェック |
| `Android15Features` | Android 15固有機能対応 |
| `PermissionUtils` | 権限関連ユーティリティ |

### データモデル

| クラス名 | 役割 |
|---------|------|
| `VRMModel` | VRMモデルデータ |
| `VRMMetadata` | VRMメタデータ |
| `AvatarInfo` | アバター情報 |
| `AvatarState` | アバター状態 |
| `Expression` | 表情データ |
| `Pose` | ポーズデータ |
| `PhotoItem` | 写真アイテム |
| `ARSessionState` | ARセッション状態 |
| `ARCameraState` | ARカメラ状態 |
| `PerformanceModels` | パフォーマンス指標データ |

### DI Modules

| モジュール名 | 提供する依存関係 |
|------------|----------------|
| `CameraModule` | Camera, Media, VRM Repositories, Thumbnail Generator, Performance系 |
| `ARModule` | AR Repository, Error Handlers, Fallback Manager |
| `ARRenderingModule` | Filament Renderer, Scene Manager, Lighting/Shadow Systems |

## データフロー

### カメラ撮影フロー

```
User Action (UI)
    ↓
CameraViewModel.capturePhoto()
    ↓
CameraRepository.capturePhoto()
    ↓
CameraX ImageCapture
    ↓
MediaRepository.savePhoto()
    ↓
MediaStore + File System
    ↓
StateFlow更新
    ↓
UI再コンポーズ
```

### VRMアバター読み込みフロー

```
User selects VRM file
    ↓
AvatarLibraryViewModel.importAvatar(uri)
    ↓
VRMRepository.importVRMFromUri(uri)
    ↓
VRMManager.loadVRMModel(uri)
    ↓
┌─ VRMParser.parse()
├─ VRMValidator.validate()
├─ VRMMeshExtractor.extract()
├─ VRMTextureExtractor.extract()
├─ VRMExpressionLoader.load()
└─ VRMPoseLoader.load()
    ↓
VRMFilamentConverter.convert()
    ↓
AvatarThumbnailGenerator.generate()
    ↓
アバターライブラリに保存
    ↓
StateFlow更新
    ↓
UI更新
```

### AR撮影フロー

```
User enables AR Mode
    ↓
CameraViewModel.setARMode(true)
    ↓
ARRepository.initializeSession()
    ↓
ARCore Session Start
    ↓
ARSceneManager.setupScene()
    ↓
FilamentARRenderer.render()
    ├─ LightingSystem (環境光推定)
    ├─ ShadowSystem (影生成)
    └─ VRM Avatar Rendering
    ↓
User captures photo
    ↓
CameraRepository.capturePhoto()
    ↓
AR overlay + camera frame
    ↓
MediaRepository.savePhoto(with AR metadata)
```

### 表情・ポーズ制御フロー

```
User selects expression/pose
    ↓
AvatarControlViewModel
    ↓
ExpressionController / PoseController
    ↓
AvatarController.updateState()
    ↓
StateFlow更新
    ↓
FilamentARRenderer observes change
    ↓
Blend shapes / bone transforms 更新
    ↓
レンダリング更新
```

## テストディレクトリ

```
app/src/
├── androidTest/              # Instrumentation Tests
│   └── java/com/example/vtubercamera/
│       ├── e2e/             # E2Eテスト
│       └── ...
│
└── test/                    # Unit Tests
    └── java/com/example/vtubercamera/
        ├── ui/viewmodels/   # ViewModelテスト
        ├── data/            # Repositoryテスト
        └── ...
```

## リソースディレクトリ

```
app/src/main/res/
├── values/
│   ├── strings.xml          # 文字列リソース (英語)
│   ├── colors.xml           # カラーリソース
│   └── themes.xml           # テーマ定義
├── values-ja/
│   └── strings.xml          # 文字列リソース (日本語)
├── values-night/
│   └── themes.xml           # ダークモードテーマ
├── drawable/                # ドローアブルリソース
├── mipmap/                  # アプリアイコン
└── xml/                     # XMLリソース
```

## ドキュメント

プロジェクトの詳細ドキュメントは `docs/` ディレクトリに格納されています：

- `AR_AVATAR_ARCHITECTURE.md` - ARアバターアーキテクチャ
- `AR_AVATAR_API_SPECIFICATION.md` - ARアバターAPI仕様
- `AR_AVATAR_CODE_ORGANIZATION.md` - コード組織
- `AR_AVATAR_DOCUMENTATION.md` - ARアバター全般ドキュメント
- `AR_AVATAR_PERFORMANCE_GUIDE.md` - パフォーマンスガイド
- `AR_USER_GUIDE.md` - ARユーザーガイド
- `VRM_REQUIREMENTS.md` - VRM要件
- `DEPENDENCIES_AND_DI.md` - 依存関係とDI
- `TROUBLESHOOTING.md` - トラブルシューティング
- `CHANGE_LOG.md` - 変更履歴

---

**Last Updated:** 2025-11-15

