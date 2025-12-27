# VtuberCamera リポジトリ依存関係 & DI 構成まとめ

最終更新: 2025-11-15

このドキュメントは、`VtuberCamera` リポジトリ全体の主な依存関係と、Hilt を用いた DI (依存性注入) 構成を俯瞰できるようにまとめたものです。

---

## 1. モジュール構成

- ルートプロジェクト
  - `build.gradle` (Settings レベル)
  - `settings.gradle`
- アプリケーションモジュール
  - `:app` (`app/build.gradle`)

現在はアプリモジュールのみで構成されています。

---

## 2. ビルドツールと言語バージョン

- Gradle Android Plugin
  - `com.android.application` 8.13.0
  - `com.android.library` 8.13.0 (将来のモジュール追加用)
- Kotlin
  - Kotlin Android プラグイン: `org.jetbrains.kotlin.android` 2.2.20
  - Kotlin Compose プラグイン: `org.jetbrains.kotlin.plugin.compose` 2.2.20
  - JVM ターゲット: 11 (`jvmTarget = "11"`)
- Google Services
  - `com.google.gms.google-services` 4.4.4
- DI / アノテーション処理
  - `com.google.dagger.hilt.android` 2.57.2
  - `org.jetbrains.kotlin.kapt` 2.2.20

---

## 3. 主要ライブラリ依存関係 (アプリ共通)

### 3.1 AndroidX / UI / Compose

- コア & UI 基盤
  - `androidx.core:core-ktx:1.17.0`
  - `androidx.appcompat:appcompat:1.7.1`
  - `com.google.android.material:material:1.13.0`
  - `androidx.legacy:legacy-support-v4:1.0.0`
  - `androidx.recyclerview:recyclerview:1.4.0`

- Lifecycle
  - `androidx.lifecycle:lifecycle-livedata-ktx:2.9.4`
  - `androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4`
  - `androidx.lifecycle:lifecycle-viewmodel-android:2.9.4`
  - `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4`

- Navigation
  - `androidx.navigation:navigation-fragment-ktx:2.9.5`
  - `androidx.navigation:navigation-ui-ktx:2.9.5`

- Jetpack Compose
  - Compose BOM 相当 (個別指定)
    - `androidx.compose.ui:ui:1.7.8`
    - `androidx.compose.material:material:1.7.8`
    - `androidx.compose.ui:ui-tooling-preview:1.7.8`
    - `androidx.compose.material3:material3:1.4.0`
    - `androidx.compose.material3:material3-window-size-class:1.4.0`
    - `androidx.compose.material:material-icons-core:1.7.8`
    - `androidx.compose.material:material-icons-extended:1.7.8`
  - Activity / Wear
    - `androidx.activity:activity-compose:1.11.0`
    - `androidx.wear.compose:compose-material3:1.5.3`

### 3.2 画像・メディア

- 画像ローディング
  - `io.coil-kt:coil-compose:2.7.0`

- メディア / カメラ
  - CameraX (`camerax_version = 1.5.1`)
    - `androidx.camera:camera-core:1.5.1`
    - `androidx.camera:camera-camera2:1.5.1`
    - `androidx.camera:camera-lifecycle:1.5.1`
    - `androidx.camera:camera-view:1.5.1`
    - `androidx.camera:camera-extensions:1.5.1`

### 3.3 AR / 3D / VRM 関連

- ARCore
  - `com.google.ar:core:1.51.0`

- Filament (コメントアウト中)
  - `com.google.android.filament:filament-android:1.17.1` (コメントアウト)
  - `com.google.android.filament:filament-utils-android:1.17.1` (コメントアウト)
  - `com.google.android.filament:gltfio-android:1.17.1` (コメントアウト)

- VRM サポート
  - 外部 VRM ライブラリ `vrm4j` はコメントアウトされており、GLTF ベースの自前実装を使用

- 数学ユーティリティ
  - `org.joml:joml:1.10.8` (Java OpenGL Math Library)

### 3.4 JSON / データ

- JSON パーサ
  - `com.google.code.gson:gson:2.13.2`

### 3.5 DI / Hilt

- 本番コード
  - `com.google.dagger:hilt-android:2.57.2`
  - `androidx.hilt:hilt-navigation-compose:1.3.0`

- テスト
  - `com.google.dagger:hilt-android-testing:2.57.2`
  - `kaptTest "com.google.dagger:hilt-android-compiler:2.57.2"`
  - `kaptAndroidTest "com.google.dagger:hilt-android-compiler:2.57.2"`

### 3.6 テスト関連

- 単体テスト
  - `junit:junit:4.13.2`
  - `androidx.arch.core:core-testing:2.2.0`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2`
  - `org.mockito:mockito-core:5.20.0`
  - `org.mockito.kotlin:mockito-kotlin:6.0.0`
  - `org.jetbrains.kotlin:kotlin-test:2.2.20`

- インストルメンテーションテスト
  - `androidx.test.ext:junit:1.3.0`
  - `androidx.test.espresso:espresso-core:3.7.0`
  - `androidx.test:rules:1.7.0`
  - `androidx.test:runner:1.7.0`
  - `androidx.compose.ui:ui-test-junit4:1.7.8`

- デバッグ用
  - `androidx.compose.ui:ui-tooling:1.7.8`
  - `androidx.compose.ui:ui-test-manifest`

---

## 4. DI (Hilt) 構成概要

### 4.1 アプリケーションエントリポイント

- `VtuberCameraApp` (`app/src/main/java/com/example/vtubercamera/VtuberCameraApp.kt`)
  - `@HiltAndroidApp` で注釈された `Application` クラス
  - 役割
    - Hilt コンポーネントツリーのルートを生成 (`SingletonComponent`)
    - `Singleton` スコープの依存関係をアプリ全体で共有

- `MainActivity` (`app/src/main/java/com/example/vtubercamera/MainActivity.kt`)
  - `@AndroidEntryPoint` を付与した画面エントリ Activity
  - `CameraScreen()` などの Compose UI から `hiltViewModel()` などを通じて ViewModel を取得可能

### 4.2 Hilt コンポーネントとスコープ

- 使用コンポーネント
  - `SingletonComponent`
    - 全モジュール (`CameraModule`, `ARModule`, `ARRenderingModule`) が `@InstallIn(SingletonComponent::class)` を指定
    - これらの `@Provides` / `@Binds` で提供されるインスタンスは、アプリ全体でシングルトンとして扱われます

- 主なスコープ
  - `@Singleton`
    - カメラ / AR / VRM 関連のほとんどの依存関係に付与
    - カメラプロバイダ、リポジトリ、エラーハンドラ、パフォーマンスモニタなどがアプリ全体で共有される前提

### 4.3 DI モジュール一覧

#### 4.3.1 `CameraModule`

ファイル: `app/src/main/java/com/example/vtubercamera/di/CameraModule.kt`

- アノテーション
  - `@Module`
  - `@InstallIn(SingletonComponent::class)`

- `@Binds` によるインターフェース実装のバインド
  - `CameraRepositoryImpl` → `CameraRepository`
  - `MediaRepositoryImpl` → `MediaRepository`
  - `VRMRepositoryImpl` → `VRMRepository`

- `@Provides` による具象型の提供 (すべて `@Singleton`)
  - `ProcessCameraProvider`
    - 取得方法: `ProcessCameraProvider.getInstance(context).get()`
    - 使用される `Context` は `@ApplicationContext`
  - `CameraCapabilityManager`
    - コンストラクタ: `CameraCapabilityManager(context)`
    - 役割: 端末のカメラ機能のサポート状況を判定
  - `AvatarThumbnailGenerator`
    - 役割: VRM アバターのサムネイル生成
  - `PerformanceMonitor`
    - 役割: パフォーマンス監視 (フレームレートや負荷など) の中核
  - `PerformanceOptimizer`
    - 役割: パフォーマンス最適化ロジック
  - `BatteryMonitor`
    - 役割: バッテリー状態監視
  - `FrameRateMonitor`
    - 役割: フレームレート監視
  - `PerformanceManager`
    - 依存関係: `PerformanceMonitor`, `PerformanceOptimizer`, `BatteryMonitor`, `FrameRateMonitor`
    - 役割: パフォーマンス関連の統合管理

#### 4.3.2 `ARModule`

ファイル: `app/src/main/java/com/example/vtubercamera/di/ARModule.kt`

- アノテーション
  - `@Module`
  - `@InstallIn(SingletonComponent::class)`

- `@Binds`
  - `ARRepositoryImpl` → `ARRepository`

- `@Provides` (すべて `@Singleton`)
  - `ARFallbackManager`
    - 役割: ARCore 未対応端末などのフォールバック戦略管理
  - `ErrorHandler`
    - 依存: `@ApplicationContext Context`
    - 役割: VRM / AR の共通エラーハンドリング
  - `ErrorNotificationManager`
    - 依存: `@ApplicationContext Context`
    - 役割: エラー通知 (UI / システム通知など)
  - `ErrorRecoveryManager`
    - 依存: `@ApplicationContext Context`
    - 役割: エラー発生後の復旧フロー管理
  - `NetworkErrorHandler`
    - 依存: `Context`, `ErrorHandler`, `ErrorNotificationManager`
    - 役割: ネットワーク関連エラーハンドリング
  - `FileAccessErrorHandler`
    - 依存: `Context`, `ErrorHandler`, `ErrorNotificationManager`
    - 役割: ファイルアクセス関連エラーハンドリング

> 備考: `ARPermissionManager` や `ARSessionManager` は `import` されており、将来的に DI 対象として拡張される可能性があります。

#### 4.3.3 `ARRenderingModule`

ファイル: `app/src/main/java/com/example/vtubercamera/di/ARRenderingModule.kt`

- アノテーション
  - `@Module`
  - `@InstallIn(SingletonComponent::class)`

- `@Binds`
  - `FilamentARRenderer` → `ARRenderer`
    - 役割: Filament を用いた AR レンダラー実装

- `@Provides` (すべて `@Singleton`)
  - `VRMFilamentConverter`
    - 役割: VRM モデルを Filament のエンティティに変換
  - `FilamentMaterialManager`
    - 役割: Filament 用マテリアル管理
  - `FilamentTextureManager`
    - 役割: Filament 用テクスチャ管理

### 4.4 DI 対象クラスの例

> ここでは、DI による注入が確認できる代表的なクラスをまとめます。（コードベース全体からのサンプル）

- `CameraRepositoryImpl`
  - 依存注入
    - `Context` (`@ApplicationContext`)
    - `ProcessCameraProvider`
    - `CameraCapabilityManager` など
  - 提供元: `CameraModule`

- `MediaRepositoryImpl`
  - 依存注入
    - `Context` (`@ApplicationContext`)
  - 提供元: `CameraModule`

- `VRMRepositoryImpl`
  - 依存注入
    - `Context` (`@ApplicationContext`)
    - VRM/Filament 関連クラス (`VRMFilamentConverter`, `FilamentMaterialManager`, `FilamentTextureManager`)
  - 提供元: `CameraModule` + `ARRenderingModule`

- `VRMManager`
  - 依存注入
    - `Context` (`@ApplicationContext`)
    - `VRMRepository`
    - `ARRenderer` 等
  - VRM ロード/管理のオーケストレーションを担当

- `PermissionManager` / `ARPermissionManager`
  - `@ApplicationContext Context` が注入されるマネージャクラス
  - ランタイムパーミッション周りの共通処理を提供

- `CameraViewModel` (`@HiltViewModel`)
  - Hilt によって `CameraRepository`, `MediaRepository`, `VRMRepository`, `PerformanceManager` などが自動注入される ViewModel
  - `@AndroidEntryPoint` が付与された `MainActivity` から取得される

> 実際のコンストラクタ引数や注入箇所は、各クラス (`data/`, `ui/viewmodels/` 配下) のコードを参照してください。

---

## 5. 依存関係のレイヤ構造 (概観)

概ね以下のようなレイヤ構造を想定しています。

1. **UI レイヤ** (`ui/screens`, `ui/components`, `ui/viewmodels`)
   - Compose UI (`CameraScreen` など)
   - `@HiltViewModel` な ViewModel (`CameraViewModel` など)

2. **ドメイン / アプリケーションレイヤ**
   - リポジトリインターフェース (`CameraRepository`, `MediaRepository`, `VRMRepository`, `ARRepository`)
   - パフォーマンス管理 (`PerformanceManager` 等)
   - AR / VRM 管理 (`VRMManager`, `ARSessionManager`, `ARRenderer` 等)

3. **データレイヤ** (`data/`)
   - 各リポジトリ実装 (`*RepositoryImpl`)
   - ARCore / CameraX / ファイルシステム / ネットワークへのアクセス
   - VRM パーサ・バリデータ (`VRMParser`, `VRMValidator`, `VRMLoader` etc.)

4. **インフラ / 共通ユーティリティ**
   - `CameraCapabilityManager`, `PermissionManager`, エラーハンドラ (`ErrorHandler` 系)
   - JOML ベースの数学ユーティリティ (`data/vrm/math`)

Hilt モジュールは主に 2〜4 のレイヤのクラスを提供し、UI レイヤからはインターフェース経由で依存関係にアクセスする構造になっています。

---

## 6. 今後の拡張ポイント / メモ

- Filament 依存関係
  - 現時点では Gradle 依存にコメントアウトが残っており、`ARRenderer` 周りは Filament を前提とした抽象化になっています。
  - 将来的に Filament を正式導入する場合は、バージョン整合性やリポジトリ設定を確認した上で依存を有効化してください。

- VRM ライブラリ
  - 外部ライブラリを利用せず、自前実装で GLTF/VRM を処理しています。
  - 依存の追加や置き換えを行う場合は、`data/vrm` 以下と `ARRenderingModule` の責務を見直す必要があります。

- DI 構成
  - 現状すべて `SingletonComponent` に集約されていますが、将来的に `ActivityRetainedComponent` / `ViewModelComponent` 等に分割することでスコープを細かく管理できます。

---

## 7. 参考: 関連ドキュメント

- `docs/AR_AVATAR_ARCHITECTURE.md`
- `docs/AR_AVATAR_CODE_ORGANIZATION.md`
- `docs/AR_AVATAR_PERFORMANCE_GUIDE.md`
- `app/src/main/java/com/example/vtubercamera/data/vrm/README_AR_RENDERER.md`

これらと併せて参照することで、AR/VRM 機能の全体像と依存関係をより詳細に把握できます。
