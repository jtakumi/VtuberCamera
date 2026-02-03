# 調査書: 画面Aの表示ロジックをMVVMへリファクタリング（View直接操作 → ViewModel経由）

## 0. 前提 / スコープの扱い
本リポジトリ内に、依頼文にある以下のファイルは見当たりませんでした。
- `screens/ScreenA.kt`
- `utils/DataHelper.kt`

そのため本調査書では、依頼内容（"画面Aの表示ロジックをMVVMにリファクタ"・"View直接操作からViewModel経由へ"）を、このリポジトリで同様の課題が発生しやすい領域へ **読み替え** て整理します。

読み替え候補（実コード上の該当イメージ）:
- 画面A: `app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt`（大規模なComposableで状態・権限・CameraX周りの扱いが多い）
- DataHelper相当: `app/src/main/java/com/example/vtubercamera/utils/CameraCapabilityManager.kt` などの、UI外の補助ロジック

※もし本当に `ScreenA.kt` / `DataHelper.kt` が別モジュールや別ブランチに存在する場合、対象ファイルパスを指定してもらえれば、その実ファイルに対して差し替え版の調査書を作れます。

---

## 1. 実装内容（依頼事項の再確認）
- 画面Aの表示ロジックを MVVM パターンにリファクタリング
- 既存の View 直接操作から ViewModel を介した実装へ変更

ここでいう「View直接操作」は、以下のような状態が該当します。
- Composable内で、状態更新・副作用・I/O・Androidフレームワーク呼び出しが混在
- `remember { mutableStateOf(...) }` のローカル状態が増殖し、状態の一貫性がViewModelと二重化
- 画面の責務が肥大化し、テストが困難（ユースケース単位に分けにくい）

---

## 2. 調査項目1: 影響範囲の特定（関連ファイル、依存関係）

### 2.1 関連ファイル候補（画面A = CameraScreen想定）
- UI
  - `app/src/main/java/com/example/vtubercamera/ui/screens/CameraScreen.kt`
  - `app/src/main/java/com/example/vtubercamera/ui/screens/ARCameraScreen.kt`（AR側も同様の構造になりやすい）
- ViewModel
  - `app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt`
  - `app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraUiState.kt`
- Domain（Feature/UseCase相当）
  - `app/src/main/java/com/example/vtubercamera/domain/camera/*`（CameraControlsFeature等）
  - `app/src/main/java/com/example/vtubercamera/domain/ar/ARFeature.kt`
  - `app/src/main/java/com/example/vtubercamera/domain/avatar/AvatarFeature.kt`
- Data（Repository相当）
  - `app/src/main/java/com/example/vtubercamera/data/CameraRepository*.kt`
  - `app/src/main/java/com/example/vtubercamera/data/MediaRepository*.kt`
- Utils（DataHelper相当の候補）
  - `app/src/main/java/com/example/vtubercamera/utils/CameraCapabilityManager.kt`
  - `app/src/main/java/com/example/vtubercamera/utils/PermissionUtils.kt`

### 2.2 依存関係の方向（期待）
- UI（Composable）→ ViewModel → Domain（Feature）→ Data（Repository）
- Android framework依存（`ProcessCameraProvider`, `PreviewView`, `ContentResolver` 等）は、可能なら UI 層に寄せ、
  ViewModel/Domainは「抽象化されたI/F」と「状態遷移」に集中させる。

### 2.3 画面Aの状態（UI State）とイベント
MVVM化の鍵は「UIに必要な状態を `UiState` としてViewModelから供給し、UIはその状態の描画だけを担当する」こと。

典型的に整理すべき項目:
- `UiState`（表示に必要な値）
  - カメラ: selector/flash/zoom/permission状態/プレビュー状態
  - ギャラリー: 読み込み中/一覧/選択状態
  - AR: セッション状態/エラー
- `UiEvent`（ユーザー操作）
  - シャッター/ズーム/フラッシュ/レンズ切替/ギャラリー表示/削除/AR遷移

---

## 3. 調査項目2: 考慮すべきリスク（既存動作への影響、パフォーマンス）

### 3.1 既存動作への影響（回帰リスク）
- ライフサイクル
  - `LaunchedEffect`/`rememberLauncherForActivityResult` のタイミングが変わると、権限要求や初期化が二重実行されやすい
- CameraXの再バインド
  - `bindToLifecycle`/`unbindAll` の呼び出し順序変更で、プレビューがブラックアウト/例外化する可能性
- 状態の二重管理
  - UIローカル state と ViewModel state が同じ概念を持つと、不整合（表示と実処理の乖離）が起きる

### 3.2 パフォーマンス・安定性リスク
- Compose再composition
  - 重い処理（カメラ初期化、MediaStoreクエリなど）がUI側で走るとフレーム落ちの原因
- メインスレッド負荷
  - `ProcessCameraProvider.getInstance(...).get()` のようなブロッキングは避ける
- コルーチン/Dispatcher
  - I/Oは `Dispatchers.IO`、CPU処理は `Default` を原則。ViewModelでdispatcher注入できる設計が望ましい

### 3.3 設計面のリスク
- Android framework依存をVMへ移しすぎる
  - VMが `Context` や `PreviewView` を持つ設計はテスタビリティを損なう
  - 代替: UI層でframework操作、VMは「必要なパラメータ」や「トリガー」をState/Eventで渡す

---

## 4. 調査項目3: 実装順序の提案（段階的）

### Step 1: 現状の責務棚卸し（影響範囲の確定）
- 画面A内のロジックを分類
  - 表示ロジック（純粋な描画）
  - 状態管理（選択/ダイアログ/モード）
  - 副作用（権限要求、カメラバインド、I/O）
- どれがVM/Domainへ移せるかを決める

### Step 2: UiState/UiEvent の定義を先に固める
- UIが必要とする state を `UiState` に集約
- UIからVMへ送るイベント（ボタン押下等）を `UiEvent` として整理

### Step 3: 「データ取得ロジック(DataHelper相当)」をRepository/UseCaseに寄せる
- 例: カメラ能力検出やMediaStoreアクセスを、UI直書きからRepository/Featureへ
- テストしやすい単位（純粋関数、I/F）に分割

### Step 4: UIから「直接操作」を削り、VM経由で状態駆動へ
- UIは `collectAsStateWithLifecycle()` で state を購読し描画
- UIローカル `remember` 状態を最小限に

### Step 5: 回帰防止テスト/ログ整備
- ViewModelの状態遷移テスト
- 権限/初期化/カメラ再バインド周りの回帰観点を明文化

---

## 5. 補足: 受け入れ条件（例）
- 画面Aの主要操作（撮影、ズーム、フラッシュ、レンズ切替、ギャラリー）が従来通り動く
- 状態はViewModelの `UiState` を正とし、UI側の二重状態が減る
- 重い処理はUIスレッドで行わない

---

## 6. 未確定事項 / 要確認
- 「画面A」「DataHelper」が指す実ファイル（このリポジトリでは未検出）
- リファクタのゴールが「挙動維持」か「改善（例: 初期化の安定化）」か
- どの程度の分割を狙うか（Domain Feature追加、Repository分割、DI変更の許容範囲）

---

## 次の成果物
- 本調査書をベースに、実装計画書 `docs/IMPLEMENTATION_PLAN_MVVM_REFACTOR_SCREEN_A.md` を作成可能
