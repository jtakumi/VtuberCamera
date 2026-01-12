# 調査メモ: 「VRMは選択できるがAR画面にアバターが表示されない」

関連: [docs/IMPLEMENTATION_PLAN_VRM_NOT_SHOWN_IN_AR_CAMERA.md](docs/IMPLEMENTATION_PLAN_VRM_NOT_SHOWN_IN_AR_CAMERA.md)

## 現象
- AR画面で「VRMファイルを選択」できる（ファイルピッカーが開き、VRMを選べる）
- しかし選択後も AR 画面にはアバターが表示されず、場合によっては **"No Avatar Loaded"** のまま

（添付スクリーンショットの1枚目: AR Camera で No Avatar Loaded が残る）

## 期待する挙動
- VRM選択後、少なくともUI状態として `currentAvatar != null` になり、
  - TopBar のタイトルが `AR Camera - <name>` になる
  - "No Avatar Loaded" カードが消える
- さらにAR合成として、カメラプレビュー上に3Dアバターが描画される

## 調査範囲（関係コードの経路）

### 1) AR画面のファイル選択 → ViewModel
- `ui/screens/ARCameraScreen.kt`
  - `avatarImportLauncher(OpenDocument)` → `viewModel.loadAvatar(uri)`
  - 描画は `ARCameraPreview`(CameraX PreviewView) のみ

### 2) ViewModel → Domain
- `ui/viewmodels/CameraViewModel.kt`
  - `loadAvatar(uri)` → `avatarFeature.loadAvatar(...)`
  - `enableARMode(context, lifecycleOwner)` → `arFeature.enableARMode(...)`

### 3) Domain → Data（VRMロード）
- `domain/avatar/AvatarFeature.kt`
  - `vrmRepository.loadVRMFromUri(uri)`
  - 成功時: `updateUiState { copy(avatar = avatar.copy(currentAvatar = vrmModel, ...)) }`
  - 失敗時: `arError` に `ARError.AvatarError(...)` を入れるが、AR画面側はそれを表示していない

- `data/VRMRepositoryImpl.kt`
  - `validateVRMFile(uri)` を先に呼び、**critical error があればロードを中断**

- `data/vrm/VRMValidator.kt`
  - `context.contentResolver.openInputStream(uri)` で stream を開き
  - `val fileSize = stream.available().toLong()` でサイズ判定

## 観察できた重要ポイント

### A. AR画面自体が「3Dアバター描画」をしていない
`ARCameraScreen.kt` は CameraX の `PreviewView` を表示しているだけで、
Filament/ARCoreのレンダリングView（SurfaceView/TextureView/GLView等）を重ねていません。

- 結果として、仮に `currentAvatar` がセットされても、
  「アバターを描画する層」が存在しないため **AR合成としては表示されません**。

> ただし、添付の状況では「No Avatar Loaded のまま」なので、
> まずは **ロード成功していない/状態反映されていない** 可能性が高いです。

### B. VRMのバリデーションが `InputStream.available()` に依存している
`VRMValidator.validateVRMFile()` のファイルサイズ判定:

```kotlin
val fileSize = stream.available().toLong()
if (fileSize < MIN_FILE_SIZE) {
  // critical error
}
```

- `available()` は「ファイル総サイズ」ではなく「今すぐ読み出せるバイト数」のため、
  ContentResolver 経由のストリームでは `0` や小さい値を返すことがあります。
- その場合、**実ファイルは正常でも** `fileSize < MIN_FILE_SIZE` 判定で critical error になり、
  `VRMRepositoryImpl.loadVRMFromUri()` がロードを中断します。
- その結果:
  - `currentAvatar` が更新されない
  - AR画面は「No Avatar Loaded」のまま

この挙動は「ファイル選択はできるが、選択後に何も起きない/表示されない」現象と整合します。

## 原因候補（優先度順）

### (最有力) 1. `available()` による誤判定でVRMロードが失敗している
- 根拠: `VRMValidator` が critical error を作りやすい実装になっている
- 影響: `currentAvatar` が null のままになり、No Avatar Loaded が残る

### 2. ロード失敗時のUIフィードバック不足で、ユーザーに失敗が見えない
- `AvatarFeature.loadAvatar` は `arError` を更新するが、`ARCameraScreen` は表示していない
- 結果: 「選べたのに何も表示されない」に見える

### 3. 仮にロードできても、AR画面に3D描画レイヤーが無い
- `ARCameraScreen` は PreviewView のみで、FilamentレンダラーをViewとして表示していない
- 結果: `currentAvatar != null` でも「アバターの見た目」が出ない

## まず確認すべきチェックポイント

1) Logcatで以下タグを確認
- `CameraViewModel`: `Loading avatar from URI:` / `Avatar loaded successfully:` / `Failed to load avatar`
- `VRMRepositoryImpl`: `Loading VRM from URI:`
- エラーノーティフィケーション（`ErrorNotificationManager`）が何を出しているか

2) `VRMValidator` の `fileSize` が異常値になっていないか
- 例: `fileSize=0` → 「File is too small...」が critical になっていないか

3) AR画面で `arError` を見える化
- 失敗しているなら、画面に出すだけで原因切り分けが進む

## 改善（修正）方針案

### Fix案 1: ファイルサイズ取得の実装を修正（最優先）
`available()` をやめ、以下のいずれかで「実サイズ」を取る:
- `ContentResolver.query(uri, arrayOf(OpenableColumns.SIZE), ...)`
- `contentResolver.openAssetFileDescriptor(uri, "r")?.length`
- サイズが取得できない場合は「不明」としてサイズ判定をスキップ（criticalにしない）

### Fix案 2: ロード失敗時に AR画面へエラーを表示
- `ARCameraScreen` に `arError` を表示する SnackBar/Toast/Card を追加
- これだけでも「ロード失敗」と「描画されない」を切り分けやすくなる

### Fix案 3: AR画面にレンダリングレイヤーを追加
- `FilamentARRenderer` / `ARRenderer` の初期化に必要な Surface を Compose で提供し、
  CameraX プレビュー上に重ねる
- 例: `AndroidView` で `SurfaceView/TextureView` を置き、
  `ARSession` のフレーム更新で `renderer.update(frame, avatarState)` を呼ぶ

## 次アクション（提案）
- 最初に `VRMValidator` の `available()` 問題を直し、AR画面で `currentAvatar` が非nullになることを確認
- 次に `arError` 表示を入れて、失敗時の見え方を改善
- 最後に、AR合成の「描画レイヤー追加」を進める（これは実装ボリュームが大きい）
