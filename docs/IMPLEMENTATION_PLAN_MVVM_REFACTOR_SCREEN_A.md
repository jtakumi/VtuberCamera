# 実装計画書: CameraScreenのMVVMリファクタリング

**対象**: カメラ画面のUIロジックを整理し、保守性を向上させる  
**作成日**: 2026-01-12  
**難易度**: 中級〜上級  
**想定作業時間**: 3〜5日

---

## このドキュメントについて

このドキュメントは、VTuber Cameraアプリのカメラ画面をリファクタリングするための実装計画書です。
**初めてこのプロジェクトに参加する方でも理解できるよう、背景知識から具体的な実装手順まで詳しく説明します。**

### 前提知識
以下の知識があると理解しやすいです（必須ではありません）：
- Kotlin基礎
- Jetpack Compose の基本（`@Composable`, `remember`, `State`）
- MVVMアーキテクチャパターンの概念
- Android の権限システム

---

## 0. プロジェクト概要

### 0.1 このアプリは何か
VTuber Camera は、VTuber やコンテンツクリエイター向けのAndroidカメラアプリです。
以下の機能を提供しています：
- カメラプレビューと写真撮影
- ピンチズーム、レンズ切り替え
- ギャラリー表示、写真削除
- AR機能（VRMモデル表示）

### 0.2 使用している技術スタック
- **UI**: Jetpack Compose（宣言的UI）
- **アーキテクチャ**: MVVM（Model-View-ViewModel）+ Domain層（Feature）
- **DI**: Hilt（依存性注入）
- **カメラ**: CameraX（Androidの最新カメラAPI）
- **非同期処理**: Kotlin Coroutines + StateFlow

### 0.3 対象ファイル
本計画書では以下のファイルをリファクタリング対象とします：

- **画面（View）**: `app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt`
- **ViewModel**: `app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt`
- **状態定義**: `app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraUiState.kt`

---

## 1. なぜこのリファクタリングが必要なのか

### 1.1 現在の問題点

現在の `CameraScreen.kt` には、以下のような課題があります：

**問題1: UIに状態管理が散らばっている**
```kotlin
// CameraScreen.kt の現状（簡略化）
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    // ❌ UI内で画面状態を管理している
    var showGalleryView by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(...) }
    
    // ViewModelの状態とUI独自の状態が混在
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 画面分岐が複雑化
    when {
        !hasCameraPermission -> { /* 権限要求画面 */ }
        showGalleryView -> { /* ギャラリー画面 */ }
        uiState.isPreviewMode -> { /* プレビュー画面 */ }
        // ...
    }
}
```

**問題2: 副作用（Toast、ダイアログ表示）がUI内に直書きされている**
```kotlin
// ❌ Toast表示がUI層に散在
Toast.makeText(context, "写真を削除しました", Toast.LENGTH_SHORT).show()
```

**問題3: Android framework依存がViewModel に漏れている**
```kotlin
// ViewModel.kt
fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
    // ❌ ViewModelがAndroidのViewを直接受け取っている
}
```

### 1.2 リファクタリング後の理想の姿

MVVMパターンを徹底することで、以下のような状態を目指します：

**✅ ViewModelが全ての画面状態を管理**
```kotlin
// CameraViewModel.kt
data class CameraUiState(
    val screenMode: CameraScreenMode,  // 画面モードを一元管理
    val dialogs: DialogState,          // ダイアログ状態を一元管理
    val permissions: PermissionState,   // 権限状態を一元管理
    // ...
)
```

**✅ UIは状態を描画するだけ**
```kotlin
// CameraScreen.kt（理想）
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // UIローカルstateが最小限
    when (uiState.screenMode) {
        PermissionGateCamera -> PermissionRequestScreen()
        Camera -> CameraPreviewScreen()
        Gallery -> GalleryScreen()
        // ...
    }
}
```

**✅ 一回きりのイベント（Toast等）はUiEffectで通知**
```kotlin
// ViewModelからイベントを発火
LaunchedEffect(Unit) {
    viewModel.uiEffect.collect { effect ->
        when (effect) {
            is ShowToast -> Toast.makeText(context, effect.message, ...).show()
        }
    }
}
```

---

## 2. MVVMアーキテクチャとは（基礎知識）

初めてMVVMに触れる方のために、基本概念を説明します。

### 2.1 MVVMの3つの層

```
┌─────────────────────────────────────┐
│  View (UI層 - Composable)           │ ← ユーザーが見る画面
│  - 状態を描画する                    │
│  - ユーザー入力をViewModelに伝える    │
└─────────────────────────────────────┘
              ↓ 状態の購読
              ↑ イベントの送信
┌─────────────────────────────────────┐
│  ViewModel                          │ ← ビジネスロジック・状態管理
│  - UiStateを保持・更新する            │
│  - Repositoryからデータを取得         │
└─────────────────────────────────────┘
              ↓ データ要求
              ↑ データ提供
┌─────────────────────────────────────┐
│  Model (Repository, UseCase)        │ ← データソース
│  - カメラ操作、ストレージアクセス       │
└─────────────────────────────────────┘
```

### 2.2 重要な用語の定義

| 用語 | 説明 | 例 |
|------|------|-----|
| **UiState** | 画面の表示に必要な全ての状態 | `data class CameraUiState(screenMode, zoomRatio, ...)` |
| **StateFlow** | 状態を流し続けるストリーム | `val uiState: StateFlow<CameraUiState>` |
| **UiEffect** | 一度だけ実行されるイベント | Toast表示、ナビゲーション |
| **SharedFlow** | イベントを流すストリーム | `val uiEffect: SharedFlow<CameraUiEffect>` |
| **Feature** | ドメイン層の機能単位 | `CameraControlsFeature`, `GalleryFeature` |

### 2.3 このプロジェクトのアーキテクチャ

```
UI層（Compose）
  ↓
ViewModel（状態管理）
  ↓
Feature（ドメインロジック）
  ↓
Repository（データアクセス）
```

---

## 3. 実装のゴール

### 3.1 達成したいこと（ゴール）

- ✅ `CameraScreen` 内の `remember { mutableStateOf(...) }` を段階的に削減
- ✅ 画面の状態を `CameraViewModel.uiState` に集約
- ✅ Toast やダイアログ表示を `UiEffect` で管理
- ✅ 既存機能（撮影、ズーム、レンズ切替、ギャラリー）の挙動を100%維持

### 3.2 やらないこと（非ゴール）

- ❌ CameraX/Camera2 の根本的な変更
- ❌ Repository層の大規模リファクタリング
- ❌ Feature層の設計変更（必要なら別計画で実施）
- ❌ UI デザインの変更

---

## 4. 現状分析（何が問題なのか）

### 4.1 UIローカルstateが画面状態として使われている

**現状のコード**:
```kotlin
// CameraScreen.kt の一部（現在）
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    // ❌ これらの状態がUI内で管理されている
    var showGalleryView by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPartialAccessDialog by remember { mutableStateOf(false) }
    var showLensSwitchFeedback by remember { mutableStateOf(false) }
    var lensSwitchFeedbackName by remember { mutableStateOf("") }
    
    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtils.hasCameraPermission(context))
    }
    var hasMediaPermissions by remember {
        mutableStateOf(PermissionUtils.hasMediaPermissions(context))
    }
}
```

**何が問題か**:
- `showGalleryView` の状態がViewModel外にあるため、テストできない
- 権限状態とViewModel状態が分離しているため、画面遷移ロジックが複雑化
- 画面回転などでUI再生成時に状態が失われる可能性がある

### 4.2 副作用がUI層に直接書かれている

**現状のコード**:
```kotlin
// CameraScreen.kt（Toast表示の例）
viewModel.deletePhoto(uri) { success ->
    if (success) {
        // ❌ UI層で直接Toast表示
        Toast.makeText(context, "写真を削除しました", Toast.LENGTH_SHORT).show()
        viewModel.exitPreviewMode()
    } else {
        Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
    }
}
```

**何が問題か**:
- 成功/失敗のメッセージがUI層に散らばっている
- ViewModelのテストで「Toastが表示されたか」を検証できない
- 多言語対応が難しい（リソースIDをViewModelに渡せない）

### 4.3 Android framework依存がViewModelに漏れている

**現状のコード**:
```kotlin
// CameraViewModel.kt
fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
    // ❌ ViewModelがAndroidのViewクラスに依存している
    cameraControlsFeature.focusOnPoint(previewView, x, y, viewModelScope, ...)
}
```

**何が問題か**:
- `PreviewView` はAndroid framework の View クラス
- ViewModelのユニットテストで `PreviewView` のモックが必要になる
- ViewModelの再利用性が下がる

---

## 5. リファクタリング方針

### 5.1 基本原則

| 原則 | 説明 |
|------|------|
| **UIはStateを描画するだけ** | UI層は `uiState` を購読し、その内容を画面に表示する |
| **VMは状態を管理するだけ** | ViewModelは状態の更新とビジネスロジックに専念 |
| **一回きりイベントはEffect** | Toast、ナビゲーションなど状態に載せられないものはSharedFlowで通知 |
| **framework依存はUI層** | Android固有のクラス（View等）はUI層に留める |

### 5.2 段階的移行戦略

一度に全てを変えるのではなく、**5つのステップに分けて段階的に移行**します：

```
Step 0: 観測点追加（ログ・テスト） ← 安全網を張る
  ↓
Step 1: 画面モードの一元化 ← showGalleryView等を整理
  ↓
Step 2: Dialog/Overlay状態の移動 ← ダイアログをUiStateへ
  ↓
Step 3: 権限状態の移動 ← hasCameraPermission等をUiStateへ
  ↓
Step 4: UiEffect導入 ← Toast等をイベント化
  ↓
Step 5: CameraX境界の整理 ← framework依存を整理
```

各ステップごとにテストを実行し、動作確認を行います。

---

## 6. 変更対象ファイルと役割

### 6.1 主要ファイル

| ファイルパス | 役割 | 変更内容 |
|-------------|------|----------|
| `ui/screens/CameraScreen.kt` | カメラ画面のUI | UIローカルstateを削減、UiEffect対応 |
| `ui/viewmodels/CameraViewModel.kt` | カメラ画面の状態管理 | 画面モード管理、UiEffect発火メソッド追加 |
| `ui/viewmodels/CameraUiState.kt` | 状態の定義 | screenMode, dialogs, permissions等を追加 |

### 6.2 影響を受ける可能性があるファイル

| ファイルパス | 役割 | 変更の可能性 |
|-------------|------|-------------|
| `ui/camerax/CameraXPreviewHost.kt` | CameraXのプレビュー管理 | コールバック追加の可能性（低） |
| `domain/camera/CameraControlsFeature.kt` | カメラ操作のドメインロジック | UiEffect発火点の追加（低） |
| `domain/camera/GalleryFeature.kt` | ギャラリー機能のロジック | UiEffect発火点の追加（低） |

### 6.3 テストファイル

| ファイルパス | 役割 |
|-------------|------|
| `test/.../CameraViewModelTest.kt` | ViewModelのユニットテスト |

---

## 7. 詳細実装ステップ

### Step 0: 観測点の追加（準備作業）

**目的**: 後続の変更で回帰を検知できるように、ログとテストを整備する

**作業内容**:
1. 画面モード遷移の一覧表を作成
2. `CameraViewModel` に簡易的なログを追加（Debug時のみ）

**成果物**:
- 画面遷移の状態遷移図（Markdown表）

**所要時間**: 0.5日

---

### Step 1: 画面モードをUiStateで一元化

**目的**: `showGalleryView`、`isPreviewMode`、`currentViewingPhoto` の組み合わせで決まる画面状態を、一つの `screenMode` で表現する

#### 1.1 enum class の追加

**ファイル**: `ui/viewmodels/CameraUiState.kt`

```kotlin
// 追加: 画面モードの定義
enum class CameraScreenMode {
    /** カメラ権限が未許可 */
    PermissionGateCamera,
    
    /** ストレージ権限が未許可 */
    PermissionGateMedia,
    
    /** 通常のカメラプレビュー画面 */
    Camera,
    
    /** ギャラリー一覧画面 */
    Gallery,
    
    /** 写真詳細表示画面 */
    PhotoDetail,
    
    /** 撮影直後のプレビュー画面 */
    PhotoPreview
}

// CameraUiState に追加
data class CameraUiState(
    val screenMode: CameraScreenMode = CameraScreenMode.Camera,  // 追加
    // ... 既存のフィールド
)
```

#### 1.2 ViewModel にモード遷移メソッドを追加

**ファイル**: `ui/viewmodels/CameraViewModel.kt`

```kotlin
// 追加: 画面モード遷移メソッド
class CameraViewModel @Inject constructor(...) : ViewModel() {
    
    /** ギャラリー画面を開く */
    fun openGallery() {
        updateUiState { 
            copy(screenMode = CameraScreenMode.Gallery) 
        }
    }
    
    /** ギャラリー画面を閉じる */
    fun closeGallery() {
        updateUiState { 
            copy(
                screenMode = CameraScreenMode.Camera,
                gallery = gallery.copy(
                    isSelectionMode = false,
                    selectedPhotos = emptySet()
                )
            ) 
        }
    }
    
    /** 写真詳細画面を開く */
    fun openPhotoDetail(photo: PhotoItem) {
        updateUiState { 
            copy(
                screenMode = CameraScreenMode.PhotoDetail,
                gallery = gallery.copy(currentViewingPhoto = photo)
            ) 
        }
    }
    
    /** 写真詳細画面を閉じる */
    fun closePhotoDetail() {
        updateUiState { 
            copy(
                screenMode = CameraScreenMode.Gallery,  // ギャラリーに戻る
                gallery = gallery.copy(currentViewingPhoto = null)
            ) 
        }
    }
    
    /** 撮影後プレビュー画面を開く */
    fun openPhotoPreview() {
        if (_uiState.value.latestLibraryPhotoUri != null) {
            updateUiState { 
                copy(
                    screenMode = CameraScreenMode.PhotoPreview,
                    camera = camera.copy(isPreviewMode = true)
                ) 
            }
        }
    }
    
    /** 撮影後プレビュー画面を閉じる */
    fun closePhotoPreview() {
        updateUiState { 
            copy(
                screenMode = CameraScreenMode.Camera,
                camera = camera.copy(
                    isPreviewMode = false,
                    needsCameraRebind = true
                )
            ) 
        }
    }
}
```

#### 1.3 UI側を書き換え

**ファイル**: `ui/screens/CameraScreen.kt`

```kotlin
// Before（現在）
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    var showGalleryView by remember { mutableStateOf(false) }  // ❌ 削除対象
    
    when {
        !hasCameraPermission -> { /* ... */ }
        showGalleryView -> { /* ギャラリー */ }
        // ...
    }
}

// After（リファクタリング後）
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (uiState.screenMode) {
        PermissionGateCamera -> PermissionRequestScreen(...)
        PermissionGateMedia -> PermissionRequestScreen(...)
        Camera -> CameraCaptureScreen(...)
        Gallery -> GalleryScreen(...)
        PhotoDetail -> PhotoDetailScreen(...)
        PhotoPreview -> PhotoPreviewScreen(...)
    }
}
```

**検証コマンド**:
```bash
./gradlew testDebugUnitTest --tests "*CameraViewModelTest*"
./gradlew assembleDebug
```

**所要時間**: 1日

---

### Step 2: Dialog/Overlay状態をUiStateへ移動

**目的**: ダイアログやオーバーレイ表示状態をViewModelで管理する

#### 2.1 状態定義の追加

**ファイル**: `ui/viewmodels/CameraUiState.kt`

```kotlin
// 追加: ダイアログ状態
data class CameraDialogState(
    val showDeleteConfirm: Boolean = false,
    val showPartialAccess: Boolean = false
)

// 追加: オーバーレイ状態
data class CameraOverlayState(
    val showLensSwitchFeedback: Boolean = false,
    val lensSwitchFeedbackName: String = ""
)

// CameraUiState に追加
data class CameraUiState(
    // ... 既存フィールド
    val dialogs: CameraDialogState = CameraDialogState(),      // 追加
    val overlays: CameraOverlayState = CameraOverlayState(),  // 追加
)
```

#### 2.2 ViewModel にメソッド追加

**ファイル**: `ui/viewmodels/CameraViewModel.kt`

```kotlin
/** 削除確認ダイアログを表示 */
fun requestDeleteConfirm() {
    updateUiState { 
        copy(dialogs = dialogs.copy(showDeleteConfirm = true)) 
    }
}

/** 削除確認ダイアログを閉じる */
fun dismissDeleteConfirm() {
    updateUiState { 
        copy(dialogs = dialogs.copy(showDeleteConfirm = false)) 
    }
}

/** 部分アクセスダイアログを表示 */
fun showPartialAccessDialog() {
    updateUiState { 
        copy(dialogs = dialogs.copy(showPartialAccess = true)) 
    }
}

/** 部分アクセスダイアログを閉じる */
fun dismissPartialAccessDialog() {
    updateUiState { 
        copy(dialogs = dialogs.copy(showPartialAccess = false)) 
    }
}

/** レンズ切替フィードバックを表示 */
fun showLensSwitchFeedback(lensName: String) {
    updateUiState { 
        copy(
            overlays = overlays.copy(
                showLensSwitchFeedback = true,
                lensSwitchFeedbackName = lensName
            )
        ) 
    }
}

/** レンズ切替フィードバックを非表示 */
fun hideLensSwitchFeedback() {
    updateUiState { 
        copy(
            overlays = overlays.copy(showLensSwitchFeedback = false)
        ) 
    }
}
```

#### 2.3 UI側を書き換え

```kotlin
// Before
var showDeleteConfirmDialog by remember { mutableStateOf(false) }  // ❌ 削除

// After
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

if (uiState.dialogs.showDeleteConfirm) {
    DeleteConfirmDialog(
        onDismiss = { viewModel.dismissDeleteConfirm() },
        onConfirm = { 
            viewModel.confirmDelete()
            viewModel.dismissDeleteConfirm()
        }
    )
}
```

**所要時間**: 0.5日

---

### Step 3: 権限状態をUiStateへ移動

**目的**: 権限の判定結果をViewModelで管理し、画面モードに反映する

#### 3.1 状態定義の追加

```kotlin
// 追加: 権限状態
data class PermissionState(
    val hasCameraPermission: Boolean = false,
    val hasMediaPermissions: Boolean = false,
    val hasPartialMediaAccess: Boolean = false
)

// CameraUiState に追加
data class CameraUiState(
    // ...
    val permissions: PermissionState = PermissionState(),  // 追加
)
```

#### 3.2 ViewModel に権限更新メソッドを追加

```kotlin
/** 権限状態が変更された際に呼ばれる */
fun onPermissionsChanged(
    cameraGranted: Boolean,
    mediaGranted: Boolean,
    hasPartialAccess: Boolean
) {
    updateUiState { 
        val newPermissions = PermissionState(
            hasCameraPermission = cameraGranted,
            hasMediaPermissions = mediaGranted,
            hasPartialMediaAccess = hasPartialAccess
        )
        
        // 画面モードも更新
        val newMode = when {
            !cameraGranted -> CameraScreenMode.PermissionGateCamera
            !mediaGranted -> CameraScreenMode.PermissionGateMedia
            else -> CameraScreenMode.Camera
        }
        
        copy(
            permissions = newPermissions,
            screenMode = newMode
        )
    }
}
```

#### 3.3 UI側で権限結果をViewModelに通知

```kotlin
// Before
var hasCameraPermission by remember { 
    mutableStateOf(PermissionUtils.hasCameraPermission(context))
}

// After
LaunchedEffect(Unit) {
    val cameraGranted = PermissionUtils.hasCameraPermission(context)
    val mediaGranted = PermissionUtils.hasMediaPermissions(context)
    val partialAccess = PermissionUtils.hasPartialMediaAccess(context)
    
    viewModel.onPermissionsChanged(cameraGranted, mediaGranted, partialAccess)
}

val permissionLauncher = rememberLauncherForActivityResult(...) { granted ->
    // 権限結果をViewModelに通知
    viewModel.onPermissionsChanged(
        cameraGranted = granted,
        mediaGranted = PermissionUtils.hasMediaPermissions(context),
        hasPartialAccess = PermissionUtils.hasPartialMediaAccess(context)
    )
}
```

**所要時間**: 0.5日

---

### Step 4: UiEffect導入（Toast等のイベント化）

**目的**: Toast表示やナビゲーションなど、一回きりのイベントを状態から分離する

#### 4.1 UiEffect の定義

**新規ファイル**: `ui/viewmodels/CameraUiEffect.kt`

```kotlin
package com.example.vtubercamera.ui.viewmodels

import androidx.annotation.StringRes

/** 一度だけ実行されるUIイベント */
sealed interface CameraUiEffect {
    /** Toastメッセージを表示（リソースID指定） */
    data class ShowToast(@StringRes val messageResId: Int, val args: Array<Any> = emptyArray()) : CameraUiEffect
    
    /** Toastメッセージを表示（文字列直接指定） */
    data class ShowToastText(val message: String) : CameraUiEffect
    
    /** AR画面へ遷移 */
    data object NavigateToAR : CameraUiEffect
}
```

#### 4.2 ViewModel に SharedFlow を追加

```kotlin
class CameraViewModel @Inject constructor(...) : ViewModel() {
    // 追加
    private val _uiEffect = MutableSharedFlow<CameraUiEffect>()
    val uiEffect: SharedFlow<CameraUiEffect> = _uiEffect.asSharedFlow()
    
    /** UiEffectを発火する内部メソッド */
    private fun emitEffect(effect: CameraUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
    
    /** 写真削除（完了後にToastを表示） */
    fun confirmDelete() {
        if (isSelectionMode && selectedPhotos.isNotEmpty()) {
            deleteSelectedPhotos { deletedCount ->
                // ✅ ViewModelからToast表示を指示
                emitEffect(CameraUiEffect.ShowToast(
                    R.plurals.photos_deleted_count,
                    arrayOf(deletedCount)
                ))
            }
        } else {
            // ...
        }
    }
}
```

#### 4.3 UI側で UiEffect を購読

```kotlin
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    
    // UiEffectの購読
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CameraUiEffect.ShowToast -> {
                    val message = context.resources.getQuantityString(
                        effect.messageResId,
                        effect.args.size,
                        *effect.args
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is CameraUiEffect.ShowToastText -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is CameraUiEffect.NavigateToAR -> {
                    // ナビゲーション処理
                }
            }
        }
    }
    
    // ...
}
```

**所要時間**: 1日

---

### Step 5: CameraX境界の整理（オプショナル）

**目的**: `setMaxZoomRatio` 等の再compose時実行を防ぐ

#### 5.1 ズーム範囲の更新を LaunchedEffect に移動

```kotlin
// Before（問題のあるコード）
@Composable
fun CameraCaptureContent(...) {
    viewModel.apply {
        // ❌ 再composeのたびに実行される
        setMaxZoomRatio(camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10.0f)
        setMinZoomRatio(camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f)
    }
}

// After（改善後）
@Composable
fun CameraCaptureContent(...) {
    // ✅ camera変更時のみ実行
    LaunchedEffect(camera) {
        camera?.let { cam ->
            cam.cameraInfo.zoomState.value?.let { zoomState ->
                viewModel.setMaxZoomRatio(zoomState.maxZoomRatio)
                viewModel.setMinZoomRatio(zoomState.minZoomRatio)
            }
        }
    }
}
```

**所要時間**: 0.5日

---

## 8. テスト計画

### 8.1 ユニットテスト（必須）

**ファイル**: `test/.../CameraViewModelTest.kt`

```kotlin
@Test
fun `openGallery should change screenMode to Gallery`() = runTest {
    // Given: 初期状態（Camera画面）
    assertEquals(CameraScreenMode.Camera, viewModel.uiState.value.screenMode)
    
    // When: ギャラリーを開く
    viewModel.openGallery()
    testDispatcher.scheduler.advanceUntilIdle()
    
    // Then: 画面モードがGalleryになる
    assertEquals(CameraScreenMode.Gallery, viewModel.uiState.value.screenMode)
}

@Test
fun `requestDeleteConfirm should show delete dialog`() = runTest {
    // Given
    assertFalse(viewModel.uiState.value.dialogs.showDeleteConfirm)
    
    // When
    viewModel.requestDeleteConfirm()
    testDispatcher.scheduler.advanceUntilIdle()
    
    // Then
    assertTrue(viewModel.uiState.value.dialogs.showDeleteConfirm)
}

@Test
fun `onPermissionsChanged with camera denied should show permission gate`() = runTest {
    // When: カメラ権限が拒否された
    viewModel.onPermissionsChanged(
        cameraGranted = false,
        mediaGranted = true,
        hasPartialAccess = false
    )
    testDispatcher.scheduler.advanceUntilIdle()
    
    // Then
    assertEquals(CameraScreenMode.PermissionGateCamera, viewModel.uiState.value.screenMode)
    assertFalse(viewModel.uiState.value.permissions.hasCameraPermission)
}
```

### 8.2 手動テスト（回帰確認）

| 観点 | 手順 | 期待結果 |
|------|------|----------|
| 初回起動 | アプリ起動 → 権限許可 | カメラプレビューが表示される |
| ギャラリー | TopBarのギャラリーアイコンタップ | ギャラリー画面が表示される |
| 写真削除 | ギャラリーで写真長押し → 削除 | 削除確認ダイアログ → Toast表示 |
| レンズ切替 | ピンチアウト/イン | レンズが切り替わり、フィードバック表示 |
| 画面回転 | 縦 ↔ 横回転 | 状態が保持され、正常に表示される |

---

## 9. リスクと対策

| リスク | 影響度 | 対策 |
|--------|--------|------|
| **権限要求の二重実行** | 中 | `LaunchedEffect` のkeyを明示的に指定し、VM側の更新メソッドを冪等にする |
| **CameraX再バインドのタイミングずれ** | 高 | `needsCameraRebind` フラグを `CameraXPreviewHost` に移し、完了コールバックでVMに通知 |
| **状態の二重管理** | 中 | 旧フィールド（`isPreviewMode` 等）を段階的にdeprecateし、`screenMode` を正とする |
| **UiEffectの取りこぼし** | 低 | `SharedFlow` のバッファを1に設定し、テストで確実に検証 |
| **画面回転時の状態ロスト** | 中 | `SavedStateHandle` への保存は不要（ViewModelが保持）を確認 |

---

## 10. ロールバック方針

万が一、重大な回帰が発生した場合の対処：

1. **即座に対応**: PR単位でrevertし、main/developブランチを安定化
2. **部分的に戻す**: `screenMode`/`dialogs`/`overlays` の定義は残し、UI側の参照だけ旧ローカルstateに戻す
3. **原因調査**: 失敗したステップのテストケースを追加し、修正後に再マージ

---

## 11. 実装スケジュール（目安）

| ステップ | 作業内容 | 所要時間 | 担当者 |
|---------|---------|---------|--------|
| Step 0 | 観測点追加 | 0.5日 | - |
| Step 1 | 画面モード一元化 | 1日 | - |
| Step 2 | Dialog/Overlay移動 | 0.5日 | - |
| Step 3 | 権限状態移動 | 0.5日 | - |
| Step 4 | UiEffect導入 | 1日 | - |
| Step 5 | CameraX境界整理 | 0.5日 | - |
| **合計** | | **4日** | |

※レビュー・手動テスト時間を含めると **5日程度**

---

## 12. 検証コマンド

各ステップ完了後、以下のコマンドで動作確認を行います：

```bash
# ユニットテスト
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# ビルド
./gradlew assembleDebug

# クリーン＆ビルド（念のため）
./gradlew clean assembleDebug

# テスト＋カバレッジ（オプション）
./gradlew testDebugUnitTest jacocoTestReport
```

---

## 13. よくある質問（FAQ）

### Q1: なぜ一度に全部変えないのか？
**A**: 段階的に変更することで、問題が起きた際の切り分けが容易になります。各ステップでテストを実行し、動作を確認しながら進めます。

### Q2: `remember { mutableStateOf(...) }` は完全になくなるのか？
**A**: いいえ。アニメーション用の一時的な状態など、ViewModelで管理する必要がないものは残します。ただし「画面の論理状態」はViewModelに集約します。

### Q3: UiEffectとUiStateの使い分けは？
**A**: 
- **UiState**: 画面の現在の状態（例: `screenMode`, `zoomRatio`）
- **UiEffect**: 一度だけ実行されるイベント（例: Toast表示、ナビゲーション）

### Q4: テストはどこまで書くべきか？
**A**: 最低限、以下をカバーしてください：
- 画面モード遷移
- ダイアログ表示/非表示
- 権限状態の反映
- UiEffectの発火（Toastなど）

### Q5: このリファクタリングで性能は改善される？
**A**: 直接的な性能改善は目的ではありませんが、不要な再composeを減らせる可能性があります（Step 5）。主な目的は保守性向上です。

---

## 14. 参考資料

### 公式ドキュメント
- [Compose の状態管理](https://developer.android.com/jetpack/compose/state)
- [ViewModel の概要](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [アーキテクチャ ガイド](https://developer.android.com/topic/architecture)

### 関連ファイル
- [既存の調査書](./INVESTIGATION_MVVM_REFACTOR_SCREEN_A.md)
- [レンズ切替実装](../LENS_SWITCHING_IMPLEMENTATION.md)
- [ズーム実装](../ZOOM_IMPLEMENTATION.md)

### 推奨記事
- [Android Developers: State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)
- [Now in Android アプリのアーキテクチャ](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)

---

## 15. チェックリスト

実装完了前に以下を確認してください：

- [ ] 全てのユニットテストがパスする
- [ ] Lintエラーがない
- [ ] 手動テスト（権限、撮影、ギャラリー、削除）が全て成功
- [ ] 画面回転で状態が保持される
- [ ] UIローカルstateが最小限（画面状態はViewModelにある）
- [ ] Toast表示がUiEffectで実装されている
- [ ] コードレビューを受けた
- [ ] ドキュメント（この計画書）を更新した

---

## 付録: トラブルシューティング

### 問題: テストで `IllegalStateException: ViewModelStore should be set before` が出る
**解決策**: テストで `hiltViewModel()` ではなく直接ViewModelをインスタンス化する

```kotlin
@Test
fun test() {
    val viewModel = CameraViewModel(
        cameraRepository = mockCameraRepository,
        // ...
    )
}
```

### 問題: UiEffectが複数回collectされる
**解決策**: `LaunchedEffect` のkeyを `Unit` にする（`true` や動的な値は避ける）

```kotlin
LaunchedEffect(Unit) {  // ← Unit固定
    viewModel.uiEffect.collect { ... }
}
```

### 問題: 権限要求後に画面が更新されない
**解決策**: 権限Launcherの `onResult` で `viewModel.onPermissionsChanged(...)` を呼ぶ

```kotlin
val launcher = rememberLauncherForActivityResult(...) { granted ->
    viewModel.onPermissionsChanged(
        cameraGranted = granted,
        mediaGranted = PermissionUtils.hasMediaPermissions(context),
        hasPartialAccess = false
    )
}
```

---

## まとめ

このリファクタリングにより、以下が達成されます：

✅ **保守性向上**: 状態がViewModelに集約され、UIロジックがシンプルになる  
✅ **テスタビリティ向上**: ViewModelのユニットテストで全ての画面状態をテスト可能  
✅ **可読性向上**: 画面モードが明示的になり、新規参加者でも理解しやすい  
✅ **バグ削減**: 状態の二重管理が減り、整合性バグが起きにくくなる

段階的に進め、各ステップでテストを実行することで、安全にリファクタリングできます。

**Happy Coding! 🚀**
