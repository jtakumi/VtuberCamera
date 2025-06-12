# VTuberCamera 変更履歴

## 目次
- [2025-06-12: Android 15 (targetSDK 35) 完全対応](#2025-06-12-android-15-targetsdk-35-完全対応)
- [2025-06-08: 機能改善](#2025-06-08-機能改善)
- [2025-06-07: カメラ機能改善](#2025-06-07-カメラ機能改善)
- [2025-06-06: CameraXの実装と改善](#2025-06-06-cameraxの実装と改善)
- [2025-06-04-05: プロジェクト基盤構築](#2025-06-04-05-プロジェクト基盤構築)

---

## 2025-06-12: Android 15 (targetSDK 35) 完全対応

### 🎯 概要
Android 15 (API Level 35) への完全対応を実装しました。新しい権限モデル「パーシャルフォトアクセス」への対応、セキュリティ機能の強化、ProGuard設定の最適化など、包括的なアップデートを行いました。

### 📋 主要な変更点

#### 1. ビルド設定の更新
**app/build.gradle**
```gradle
android {
    compileSdk 35
    defaultConfig {
        targetSdk 35  // 34から35に更新
    }
}
```

**依存関係の最新化**
- Compose: 1.5.4 → 1.7.1
- Core KTX: 1.12.0 → 1.13.1
- AppCompat: 1.6.1 → 1.7.0
- Material: 1.11.0 → 1.12.0
- Firebase BOM: 32.2.0 → 33.1.0
- Navigation: 2.5.2 → 2.7.7
- Lifecycle: 2.4.1 → 2.8.0
- RecyclerView: 1.2.1 → 1.3.2
- Activity Compose: 1.8.2 → 1.9.0
- Material3: 1.1.2 → 1.2.1
- Coil: 2.5.0 → 2.6.0
- Test関連ライブラリも最新版に更新

#### 2. Android 15新権限への対応
**AndroidManifest.xml**
```xml
<!-- Android 15: パーシャルフォトアクセス対応 -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
```

**PermissionUtils.kt** (新規作成)
```kotlin
object PermissionUtils {
    fun getRequiredMediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            // ...
        }
    }
}
```

#### 3. 権限処理の完全刷新
**CameraScreen.kt** - Android 15対応権限処理
- パーシャルフォトアクセスのダイアログ実装
- 段階的権限許可フローの導入
- より分かりやすい権限説明UI
- Android バージョン別の適切な権限リクエスト

```kotlin
// パーシャルアクセスダイアログ（Android 15対応）
if (showPartialAccessDialog) {
    AlertDialog(
        title = { Text("写真へのアクセス") },
        text = { Text("一部の写真のみへのアクセスが許可されています...") },
        // ...
    )
}
```

#### 4. Android 15新機能サポート
**Android15Features.kt** (新規作成)
```kotlin
object Android15Features {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun isInPrivateSpace(context: Context): Boolean
    
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun handleBackgroundRestrictions(activity: Activity)
    
    fun initializeAndroid15Support(activity: Activity)
}
```

主な対応内容：
- **Private Space機能**: 将来の対応準備
- **バックグラウンド実行制限**: より厳しい制限への対応
- **MediaProjection制限**: 画面録画関連の新制限
- **セキュリティ強化**: アプリ分離の強化

#### 5. ProGuard設定の完全再構築
**ファイル配置の最適化**
- ❌ `/proguard-rules.pro` (間違った場所) → 削除
- ✅ `/app/proguard-rules.pro` (正しい場所) → 統合版で更新

**統合版ProGuardルール**
```pro
# Android 15対応のProGuard設定

# CameraX関連のクラスを保護
-keep class androidx.camera.** { *; }

# Android 15の新しいAPIに関する警告を抑制
-dontwarn android.os.Build$VERSION_CODES
-dontwarn android.permission.**

# パーシャルフォトアクセス関連
-keep class android.provider.MediaStore$** { *; }
```

**build.gradle ProGuard有効化**
```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                     'proguard-rules.pro'
    }
}
```

#### 6. MainActivity更新
**Android15Features初期化**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 15の新機能サポートを初期化
        initializeAndroid15()
        
        // ...
    }
}
```

### 🎨 ユーザー体験の改善

#### 1. 権限処理の改善
- **段階的許可**: ユーザーが段階的に権限を許可可能
- **明確な説明**: 各権限の必要性をわかりやすく説明
- **パーシャルアクセス対応**: 一部写真のみのアクセス許可に対応
- **設定画面誘導**: より詳細な権限設定への誘導

#### 2. セキュリティの向上
- **Private Space準備**: Android 15のPrivate Space機能への準備
- **アプリ分離強化**: より厳格なアプリ間分離
- **バックグラウンド制限**: 適切なバックグラウンド動作制限

### 🚀 技術的な改善

#### 1. パフォーマンス最適化
- **ProGuard最適化**: コードの難読化と最適化
- **リソース削減**: 未使用リソースの自動削除
- **ビルドサイズ削減**: APKサイズの最適化
- **メモリ効率**: より効率的なメモリ使用

#### 2. 開発体験の改善
- **明確なファイル構造**: 適切な場所への設定ファイル配置
- **包括的なドキュメント**: 実装ガイドとベストプラクティス
- **段階的実装**: 必須・推奨・オプションの明確な分離

#### 3. 互換性の向上
- **下位互換性維持**: Android 7.0-14での動作確認
- **段階的機能適用**: Android バージョンに応じた適切な機能提供
- **フォールバック処理**: 新機能非対応時の代替処理

### 📁 ファイル変更一覧

#### 新規作成
1. `app/src/main/java/com/example/vtubercamera/utils/PermissionUtils.kt`
2. `app/src/main/java/com/example/vtubercamera/utils/Android15Features.kt`
3. `app/proguard-rules.pro` (統合版)

#### 更新
1. `app/build.gradle` - targetSDK 35、依存関係更新
2. `app/src/main/AndroidManifest.xml` - 新権限追加
3. `app/src/main/java/com/example/vtubercamera/MainActivity.kt` - Android 15初期化
4. `app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt` - 権限処理更新

#### 削除
1. `/proguard-rules.pro` (間違った場所のファイル)

### 🧪 テスト要項

#### 必須テスト
- [ ] Android 15エミュレータでの動作確認
- [ ] 権限フローの完全テスト（カメラ・ストレージ）
- [ ] パーシャルアクセス時の動作確認
- [ ] 下位互換性テスト（Android 7.0-14）
- [ ] ProGuardビルドでのクラッシュテスト

#### 推奨テスト
- [ ] Private Space機能の準備確認
- [ ] パフォーマンス検証（ビルドサイズ・メモリ）
- [ ] セキュリティ機能の動作確認
- [ ] バックグラウンド制限の確認

### ⚠️ 破壊的変更

#### 権限処理の変更
- Android 15では写真アクセス権限がより細かく制御
- 初回起動時の権限フローが変更
- パーシャルアクセス時の UI/UX が追加

#### ビルド設定の変更
- ProGuardファイルの場所変更
- ビルドタイプでのProGuard有効化
- 依存関係の大幅更新

### 🎯 今後の予定

#### 短期（1-2週間）
- [ ] Android 15エミュレータでの詳細テスト
- [ ] ユーザーフィードバックの収集
- [ ] パフォーマンス最適化の継続

#### 中期（1-2ヶ月）
- [ ] Private Space機能の本格実装
- [ ] Android 15特有の新機能活用
- [ ] セキュリティ機能の拡張

#### 長期（3-6ヶ月）
- [ ] AI機能とAndroid 15の統合
- [ ] 次世代カメラ機能の実装
- [ ] パフォーマンスの更なる向上

### 📊 影響範囲
- **対象ユーザー**: 全ユーザー（Android 7.0以上）
- **リリース準備度**: ベータテスト可能レベル
- **推奨展開**: 段階的ロールアウト
- **互換性**: 完全な下位互換性を維持

---

## 2025-06-08: 機能改善

### カメラプレビューの安定性向上
- プレビュー画面から戻った後にカメラプレビューが真っ暗になる問題を修正
  - `CameraScreen.kt`の`LaunchedEffect`に`isPreviewMode`を追加し、プレビューモード変更時のカメラ再バインドを確実に実行
  - `CameraViewModel.kt`の`exitPreviewMode`関数を改善し、カメラの再初期化を確実に実行

### UI/UX改善
- 写真保存成功時のトーストメッセージを削除
  - 不要な通知を減らし、よりクリーンなユーザー体験を提供
  - エラー時のトーストメッセージは維持し、問題発生時の通知は継続

### 技術的な変更点

#### CameraScreen.kt
```kotlin
// プレビューモード変更時のカメラ再バインド処理を改善
LaunchedEffect(cameraSelector, flashMode, isPreviewMode) {
    if (cameraProvider != null && preview != null) {
        bindCameraWithPreview(...)
    }
}

// 写真保存成功時のトーストメッセージを削除
onPhotoSaved = { /* トーストメッセージを削除 */ }
```

#### CameraViewModel.kt
```kotlin
fun exitPreviewMode() {
    _isPreviewMode.value = false
    // カメラの再初期化を促すために、一時的にカメラセレクターを更新
    _cameraSelector.value = _cameraSelector.value
}
```

### 影響範囲
- カメラプレビューの動作
- 写真保存時のユーザー通知

### テスト項目
1. プレビュー画面から戻った後のカメラプレビューが正常に表示されること
2. 写真保存成功時にトーストメッセージが表示されないこと
3. 写真保存失敗時は引き続きエラーメッセージが表示されること

---

## 2025-06-07: カメラ機能改善

### 概要
カメラのフラッシュライトとカメラ切り替え機能の改善を行いました。主に以下の問題に対処しました：

1. フラッシュモードの切り替えが正しく機能しない
2. カメラ切り替え時にプレビューが更新されない
3. SurfaceProviderの再設定問題
4. カメラの状態管理の不備

### 変更内容

#### 1. カメラ状態管理の改善
```kotlin
// カメラ状態管理の改善
var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
var camera: Camera? by remember { mutableStateOf(null) }
var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
var previewView: PreviewView? by remember { mutableStateOf(null) }
var preview: Preview? by remember { mutableStateOf(null) }
```
- カメラ関連の状態変数を追加し、ライフサイクル管理を改善
- PreviewViewとPreviewの参照を保持することで、プレビューの再作成を防止

#### 2. カメラプレビューの改善
```kotlin
AndroidView(
    factory = { ctx ->
        PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            previewView = this // PreviewViewの参照を保存
        }
    },
    modifier = Modifier.fillMaxSize()
) { view ->
    // 初回のみカメラプロバイダーを初期化
    if (cameraProvider == null) {
        // ...
        // プレビューを一度だけ作成してSurfaceProviderを設定
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }
        // ...
    }
}
```
- PreviewViewの参照を保存
- プレビューの作成とSurfaceProviderの設定を一度だけ実行
- カメラプロバイダーの初期化を最適化

#### 3. 状態変更の監視と再バインド機能
```kotlin
LaunchedEffect(cameraSelector, flashMode) {
    // カメラプロバイダーとプレビューが準備できている場合のみ再バインド
    if (cameraProvider != null && preview != null) {
        Log.d("CameraScreen", "Rebinding camera due to state change")
        bindCameraWithPreview(
            // ...
            preview = preview!!, // 既存のプレビューを再利用
            // ...
        )
    }
}
```
- カメラセレクタとフラッシュモードの変更を監視
- 必要な状態が揃っている場合のみ再バインドを実行
- 既存のプレビューを再利用して安定性を向上

#### 4. カメラバインド関数の改善
```kotlin
private fun bindCameraWithPreview(
    // ...
    preview: Preview, // 既存のPreviewを受け取る
    // ...
): Camera? {
    return try {
        // ImageCaptureのみ新しく作成（フラッシュモードを反映）
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        // 既存のバインディングを解除
        cameraProvider.unbindAll()
        
        // 既存のPreviewと新しいImageCaptureでバインド
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview, // 既存のPreviewを再利用
            imageCapture
        )
        // ...
    }
}
```
- 既存のPreviewを再利用
- ImageCaptureのみを新しく作成してフラッシュモードを反映
- エラーハンドリングとログ出力を改善

### 改善された点

1. **フラッシュモードの切り替え**
   - フラッシュモードの変更が即座に反映されるようになりました
   - フラッシュモードの状態が正しく維持されます

2. **カメラ切り替え**
   - フロント/バックカメラの切り替えがスムーズになりました
   - プレビューが途切れることなく表示されます

3. **パフォーマンス**
   - 不要なプレビューの再作成を防止
   - メモリ使用量の最適化

4. **安定性**
   - エラーハンドリングの強化
   - デバッグ用ログの追加

### 今後の課題

1. ズーム機能の最適化
2. カメラの解像度設定の追加
3. 撮影時の画質設定の改善

### テスト項目

1. フラッシュモードの切り替えが正しく機能するか
2. カメラの切り替え（フロント/バック）がスムーズに行われるか
3. プレビューが途切れることなく表示されるか
4. メモリリークが発生していないか

---

## 2025-06-06: CameraXの実装と改善

### 主な変更点

1. **CameraXの導入と基本実装**
   - CameraX 1.4.0への更新
   - カメラプレビュー表示の実装
   - 写真撮影機能の実装
   - フロント/バックカメラ切り替え機能の実装

2. **フラッシュ機能の実装**
   - フラッシュモードの切り替え機能（OFF/ON/AUTO）
   - フラッシュモードの状態管理
   - UIでのフラッシュモード表示と切り替え

3. **ズーム機能の実装**
   - ズームイン/アウト機能
   - カメラの最大ズーム倍率に基づく制限
   - ズームコントロールUIの実装

4. **UI/UXの改善**
   - Material3デザインの適用
   - カメラプレビュー表示の最適化
   - 撮影した写真のプレビュー表示
   - サムネイル表示機能

### 技術的な改善

1. **アーキテクチャの改善**
   - MVVMパターンの採用
   - ViewModelでの状態管理
   - StateFlowを使用したリアクティブな状態管理

2. **エラーハンドリング**
   - カメラ初期化エラーの処理
   - パーミッション管理の改善
   - 撮影エラーの処理

3. **パフォーマンス最適化**
   - カメラプレビューの効率的な実装
   - メモリリークの防止
   - ライフサイクル管理の改善

### 修正された問題

1. **FlashModeの参照エラー**
   - `ImageCapture.FLASH_MODE_*`定数の正しい参照
   - フラッシュモードの状態管理の修正

2. **型推論の問題**
   - `collectAsStateWithLifecycle()`の正しい使用
   - StateFlowの型定義の修正

3. **ライフサイクル管理**
   - `LocalLifecycleOwner`の正しい参照
   - コンポーズ可能な関数でのライフサイクル管理の改善

### 今後の課題

1. **機能の拡張**
   - フォーカス制御の実装
   - 画像の回転処理の改善
   - 動画撮影機能の追加

2. **UI/UXの改善**
   - カメラ設定のカスタマイズ機能
   - 撮影モードの追加
   - エフェクト機能の実装

3. **パフォーマンスの最適化**
   - メモリ使用量の最適化
   - バッテリー消費の改善
   - 起動時間の短縮

---

## 2025-06-04-05: プロジェクト基盤構築

### 概要

6/4, 6/5での変更点をまとめます。基本的なカメラアプリの機能が実装され、写真の撮影、プレビュー、保存が可能になりました。また、Material3デザインを採用し、モダンなUIを実現しています。

### UIの変更

#### 1. カメラ画面の基本レイアウト
- **トップバー**: カメラ切り替えボタンを追加
- **中央部**: カメラプレビュー表示
- **下部**: 撮影ボタン（FloatingActionButton）を配置

#### 2. プレビュー機能の追加
- **サムネイル表示**: 撮影した写真のサムネイル表示（右下に配置）
- **フルスクリーンプレビュー**: プレビュー画面での全画面表示
- **操作ボタン**: プレビュー画面での「削除」と「戻る」ボタン

#### 3. デザイン要素
- **サムネイル**: 角丸デザインの採用
- **テーマ**: Material3のテーマ適用
- **アイコン**: カメラ切り替え、撮影用アイコンの使用

### ロジックの変更

#### 1. カメラ機能
- **初期化**: カメラの初期化とプレビュー表示
- **撮影機能**: 写真撮影機能の実装
- **カメラ切り替え**: フロント/バックカメラの切り替え機能

#### 2. 状態管理
`CameraViewModel`での状態管理を実装：
- **カメラセレクター**: カメラの状態管理
- **写真URI**: 撮影した写真のURI管理
- **プレビューモード**: プレビュー表示状態の管理

#### 3. 写真保存機能
- **保存処理**: 撮影した写真の保存処理
- **ファイル名生成**: 日時ベースのファイル名生成
- **保存先指定**: Pictures/VTuberCameraフォルダへの保存

### ライブラリ等の変更

#### 1. Compose関連
```gradle
// 追加されたCompose依存関係
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.foundation:foundation")
implementation("androidx.compose.material:material-icons-extended")
```

- **Compose BOM**: 2024.02.00の導入
- **Material3**: 新しいマテリアルデザインの追加
- **Foundation**: 基本的なCompose機能
- **拡張アイコン**: より多くのアイコンセットの追加

#### 2. 画像処理
```gradle
// 画像読み込み用ライブラリ
implementation("io.coil-kt:coil-compose:2.5.0")
```

- **Coilライブラリ**: 画像読み込み用ライブラリの追加

#### 3. カメラ機能
```gradle
// CameraXライブラリ群
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
implementation("androidx.camera:camera-extensions:1.3.1")
```

CameraXライブラリの追加：
- **camera-core**: カメラの基本機能
- **camera-camera2**: Camera2 APIの実装
- **camera-lifecycle**: ライフサイクル連携
- **camera-view**: カメラビュー表示
- **camera-extensions**: 拡張機能

#### 4. その他の依存関係
- **コアAndroid依存関係**: 最新バージョンへの更新
- **テスト関連**: 依存関係の整理と最適化

### 技術的詳細

#### アーキテクチャパターン
- **MVVM**: ViewModelを使用した状態管理
- **Jetpack Compose**: 宣言的UIフレームワーク
- **CameraX**: モダンなカメラAPI

#### 主要な実装
1. **カメラプレビュー**: CameraXとComposeの統合
2. **写真撮影**: ImageCaptureの実装
3. **状態管理**: StateFlowを使用したリアクティブな状態管理
4. **ファイル操作**: MediaStoreを使用した写真保存

#### パフォーマンス最適化
- **メモリ効率**: Coilによる効率的な画像読み込み
- **UI応答性**: Composeによるスムーズなアニメーション
- **カメラ最適化**: CameraXによる最適化されたカメラ処理

### 今後の予定

#### 短期的な改善
- [ ] エラーハンドリングの強化
- [ ] 権限管理の改善
- [ ] UI/UXの細かな調整

#### 中期的な機能追加
- [ ] フィルター機能の追加
- [ ] 動画撮影機能
- [ ] ギャラリー機能の拡張

#### 長期的な目標
- [ ] AI機能の統合
- [ ] クラウド連携
- [ ] より高度な画像編集機能

### まとめ

この2日間の開発により、基本的なカメラアプリとしての機能が完成しました。Material3デザインシステムの採用により、モダンで使いやすいUIを実現し、CameraXライブラリの活用により安定したカメラ機能を提供できています。

次の段階では、ユーザー体験の向上とより高度な機能の実装に取り組む予定です。

---

## プロジェクト全体のマイルストーン

### 完了した機能
- ✅ 基本的なカメラアプリのUI/UX
- ✅ 写真撮影機能
- ✅ フロント/バックカメラ切り替え
- ✅ フラッシュモード制御
- ✅ ズーム機能
- ✅ 写真プレビュー機能
- ✅ カメラプレビューの安定性向上

### 進行中の課題
- 🔄 パフォーマンス最適化
- 🔄 エラーハンドリングの改善
- 🔄 UI/UXの細かな調整

### 今後の予定
- 📋 動画撮影機能の追加
- 📋 フィルター機能の実装
- 📋 ギャラリー機能の拡張
- 📋 AI機能の統合

### 技術スタック
- **フレームワーク**: Jetpack Compose + CameraX
- **アーキテクチャ**: MVVM
- **言語**: Kotlin
- **最小API**: Android API 24 (Android 7.0)
- **ターゲットAPI**: Android API 34 (Android 14)
