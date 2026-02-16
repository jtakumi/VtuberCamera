# Androidアプリ設計: リファクタリング候補一覧

本ドキュメントは、現行コードを確認して「設計的に優先して手を入れると効果が高い箇所」を整理したものです。

## 優先度A（先に着手）

1. **`CameraViewModel` の責務分割（Fat ViewModel解消）**
   - `CameraViewModel` は 1,289 行あり、カメラ制御・ギャラリー・AR・アバター・ライティングを横断して扱っています。
   - 単一 `UiState` を持ちながら後方互換のために多数の `StateFlow` を再公開しており、責務が集約しすぎています。
   - 一部操作（例: `cameraRepository.capturePhoto`）が直接 ViewModel に残っており、Feature 層への移譲が中途半端です。
   - **提案**: `CameraCaptureCoordinator` / `GalleryCoordinator` / `ARModeCoordinator` のようにユースケースごとに分割し、`CameraViewModel` はイベントルーティング中心に縮小。

2. **`CameraScreen` の巨大Composable分割（UI責務の境界明確化）**
   - `CameraScreen.kt` は 905 行で、権限要求・CameraXバインド・ダイアログ制御・トースト表示・ギャラリー表示切替まで1画面に集中しています。
   - `remember` のローカル状態と ViewModel state が混在し、画面責務が肥大化しています。
   - **提案**: `CameraPermissionSection` / `CameraPreviewSection` / `CameraOverlaySection` / `CameraDialogs` へ分割し、イベントを `UiEvent` として ViewModel に一本化。

3. **AR境界の再設計（DomainとUI投影責務の分離）**
   - `ARFeature` は `arRepository.destroySession()` を呼びつつ、`avatar.isVisible` や `avatarTransform` まで更新しており、セッション制御とUI投影が同居しています。
   - **提案**: `ARFeature` は AR 状態遷移とドメインイベント発行に限定し、UIへの投影（可視/不可視・Transform初期化）は ViewModel 側に寄せる。

4. **`ARRepository` インターフェースの純化（Android依存の隔離）**
   - `ARRepository.initializeSession` が `Context` / `LifecycleOwner` を直接受け取り、インターフェース段階でプラットフォーム依存しています。
   - **提案**: `ARSessionHost`（UI層）と `ARRepository`（データ/状態層）を分離し、ドメイン層に Android 型を漏らさない。

## 優先度B（中期で改善）

5. **`FilamentARRenderer` の分割（Rendererの単一責務化）**
   - `FilamentARRenderer.kt` は 1,147 行で、初期化・描画・ライティング・シャドウ・キャプチャ・統計を1クラスで実施しています。
   - `setLighting` / `captureFrame` にプレースホルダ実装（TODO）が残り、責務の混在が未実装要素の温床になっています。
   - **提案**: `RenderPipeline`, `LightingApplier`, `FrameCaptureService` に分割し、未実装部分をインターフェースで明示。

6. **`VRMManager` の機能分離（ロード/解析/キャッシュ責務の分割）**
   - `VRMManager` はロード進行管理、詳細解析、複数キャッシュ、メモリ統計、検証を一括管理しています。
   - **提案**: `VRMLoadFacade`（進行制御）, `VRMAssetCache`, `VRMValidationService` に分離してテスト容易性を上げる。

7. **権限管理の重複整理（`PermissionManager` と `ARPermissionManager`）**
   - 通常権限とAR権限で別マネージャーが存在し、権限判定ロジックの重複・分散が起こりやすい構成です。
   - **提案**: 共通の `PermissionGateway` を作り、AR固有のARCoreインストール確認のみ `ARPermissionManager` 側に残す。

## 優先度C（並行で着手可能）

8. **Feature API の引数整理（`updateUiState` コールバック多用の見直し）**
   - `CameraViewModel` から Feature 呼び出し時に `updateUiState` / `uiStateProvider` / `scope` を毎回渡しており、呼び出し規約が複雑化しています。
   - **提案**: `CameraActionContext` のようなコンテキストオブジェクトで引数を束ね、Featureの利用面を簡潔化。

9. **技術的負債（TODO）をチケット化して段階解消**
   - VRM/AR描画周辺に TODO が多数存在し、実装完了条件が曖昧になりやすい状態です。
   - **提案**: TODOを `must-have / nice-to-have` で分類し、描画品質に直結する項目（フレームキャプチャ、ライティング反映）を先に消化。

10. **設計ドキュメントとコードの同期運用**
    - 既にアンチパターンレビューやMVVM調査資料が存在するため、実装との差分管理を明示すると改善速度が上がります。
    - **提案**: 「設計負債バックログ」ファイルを1つ作り、リファクタ実施ごとに更新。

---

## 参照した主要箇所
- `app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt`
- `app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt`
- `app/src/main/java/com/example/vtubercamera/domain/ar/ARFeature.kt`
- `app/src/main/java/com/example/vtubercamera/data/ARRepository.kt`
- `app/src/main/java/com/example/vtubercamera/data/vrm/FilamentARRenderer.kt`
- `app/src/main/java/com/example/vtubercamera/data/vrm/VRMManager.kt`
- `app/src/main/java/com/example/vtubercamera/managers/PermissionManager.kt`
- `app/src/main/java/com/example/vtubercamera/managers/ARPermissionManager.kt`
- `docs/AntiPatternReview.md`
- `docs/INVESTIGATION_MVVM_REFACTOR_SCREEN_A.md`
