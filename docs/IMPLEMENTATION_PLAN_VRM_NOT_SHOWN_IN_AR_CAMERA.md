# 実装計画書: 「VRMは選択できるがAR画面にアバターが表示されない」

- 対象調査: [docs/INVESTIGATION_VRM_NOT_SHOWN_IN_AR_CAMERA.md](docs/INVESTIGATION_VRM_NOT_SHOWN_IN_AR_CAMERA.md)
- 目的: VRM選択後に「UI状態としてロード成功が見える」状態をまず成立させ、その後にAR合成の描画レイヤー統合へ進む。

## 背景 / 現状
- VRMファイルは選択できるが、AR画面で **"No Avatar Loaded"** のまま残ることがある。
- 調査より最有力原因は `VRMValidator` が `InputStream.available()` による誤判定で「ファイルが小さい」扱いとなり、VRMロードを中断している可能性。
- さらに、ロード失敗時のUIフィードバックが不足しており、ユーザーには「何も起きない」ように見える。
- なお、仮にロードできても、現状のAR画面は CameraX のプレビュー表示のみで、3Dレンダリングレイヤーの合成が未実装のため、3D表示までを期待すると追加実装が必要。

## ゴール（段階的）
### Phase 1（最優先）: VRMロードを確実に成功/失敗判定できる
- `available()` 依存を排除し、誤判定でのロード中断を無くす。

### Phase 2: 失敗時にユーザーへ原因を提示できる
- `arError` をAR画面で可視化し、問題切り分けを容易にする。

### Phase 3: AR画面に3D描画レイヤーを統合し、アバターを「見た目」として表示
- Filament/ARCore の描画Viewを Compose で提供し、CameraXプレビューと合成する。

## 非ゴール（今回スコープ外）
- 高品質なARアンカリング/オクルージョン/ライティング調整などの高度なAR表現
- VRMモデルの最適化（LOD、テクスチャ圧縮など）
- VRMインポートUI/UXの全面刷新

## 成果物
- 実装修正（Kotlin）
- 必要に応じたユニットテスト（特にバリデーション/サイズ取得ロジック）
- ドキュメント更新（本計画書 + 必要箇所へのリンク）

## 受け入れ条件（Done定義）
### Phase 1 Done
- 一般的なストレージプロバイダ（Files/Downloads/Drive等）のURIからVRMを選択しても、サイズ誤判定でロードが中断されない。
- UI状態として `currentAvatar != null` になり、AR画面の "No Avatar Loaded" が消える（または同等の状態変化が確認できる）。
- ロード失敗の場合は `arError` またはログに明確な理由が残る。

### Phase 2 Done
- VRMロード失敗時、AR画面にエラーが表示される（例: Snackbar/カード）。
- エラー表示が「一度だけ」「過剰に連発しない」など、最低限のUXが担保される。

### Phase 3 Done（AR表示）
- VRMロード後、AR画面に3Dアバター描画が確認できる（最低限: 画面上にモデルが表示される）。
- 端末回転/ライフサイクル（Pause/Resume）でクラッシュしない。

## 実装方針

## Phase 1: `VRMValidator` のファイルサイズ判定を修正（最優先）

### 変更内容
- `InputStream.available()` を「ファイル総サイズ」として扱う実装を廃止する。
- 以下の優先度でサイズ取得を試みる（取得できない場合は「不明」として扱い、critical error にしない）:
  1. `ContentResolver.query(uri, arrayOf(OpenableColumns.SIZE), ...)` の `SIZE`
  2. `contentResolver.openAssetFileDescriptor(uri, "r")?.length`
  3. 取得不可 → サイズ判定をスキップ（警告レベルに留める）

### 期待するコード配置（目安）
- `app/src/main/java/.../data/vrm/VRMValidator.kt`
  - サイズ取得ヘルパー関数追加（`private fun getFileSizeBytes(...)` など）
- `app/src/main/java/.../data/VRMRepositoryImpl.kt`
  - validator の結果に応じた継続/中断の判断を維持しつつ、誤判定が起きないようにする

### テスト方針
- JVMユニットテストで `VRMValidator` のサイズ取得分岐をカバーしやすい形にする。
  - 直接 `ContentResolver` をモックするのが難しい場合は、サイズ取得をインターフェース経由に切り出してテスト可能にする（過剰設計は避け、最小で）。
- 最低限:
  - サイズが取得できない場合でも critical error にしない
  - サイズが明確に小さい場合だけ critical error になる

### リスク
- `OpenableColumns.SIZE` が null のプロバイダがある。
- `AssetFileDescriptor.length` が -1 を返すケース。

### ロールバック
- サイズチェックを一時的に緩める（unknown の場合は許可）ことで、致命的な利用不能を回避可能。

## Phase 2: ロード失敗時のUIフィードバックを追加

### 変更内容
- `ARCameraScreen` で `arError` を表示する。
  - 例: `SnackbarHost` + `LaunchedEffect(arError)` で表示
  - あるいはエラー用カードを "No Avatar Loaded" の代わりに表示
- エラーはユーザー向けに短く、詳細はログに残す。

### Done基準の補足
- 「選べたのに何も起きない」を無くす。
- エラー表示が開発者の切り分けにも使える（例: `File too small (size unknown)` 等）。

## Phase 3: AR画面に3Dレンダリングレイヤーを追加（ボリューム大）

### 変更内容（概略）
- CameraX `PreviewView` の上に Filament/ARCore の描画View（`SurfaceView`/`TextureView` など）を重ねる。
- `arFeature.enableARMode(context, lifecycleOwner)` の初期化結果を UI 層へ橋渡しし、レンダリングに必要な Surface を提供する。
- `ARSession` のフレーム更新に合わせて `renderer.update(frame, avatarState)`（または同等）を呼ぶ。

### 実装順
1. 描画Viewの導入（何も描かなくてもクラッシュしない）
2. 1フレーム更新ループ確立（ログ/デバッグ描画）
3. VRMモデル（`currentAvatar`）が読み込まれている場合に描画へ流す
4. ライフサイクル（Pause/Resume）・解放処理

### リスク
- 端末依存（OpenGL/Vulkan、Surface周り）
- レンダリングスレッドとCompose/StateFlowの同期
- パフォーマンス（フレーム落ち、発熱）

## 作業タスクリスト（提案）
### Phase 1
- [ ] `VRMValidator` のサイズ取得を `available()` から置き換える
- [ ] unknown サイズ時の扱いを「警告」に落とす（criticalにしない）
- [ ] ロード失敗ログにサイズ取得結果（取得手段/値）を含める
- [ ] ユニットテスト追加（可能な範囲で）

### Phase 2
- [ ] `ARCameraScreen` に `arError` 表示（Snackbar/カード）
- [ ] 表示が連発しない制御（同一エラー抑制など、必要最小）

### Phase 3
- [ ] ARレンダリングViewのCompose統合
- [ ] フレーム更新ループの確立
- [ ] `currentAvatar` を描画へ接続
- [ ] ライフサイクル対応とリソース解放

## 動作確認手順（ローカル）
- ビルド: `./gradlew assembleDebug`
- ユニットテスト: `./gradlew testDebugUnitTest`
- 手動確認:
  - AR画面でVRM選択 → UI状態が変わる（"No Avatar Loaded" が消える / タイトル変化など）
  - 失敗時にエラーが表示される

## 補足: デバッグ観点
- ログ（タグ例）
  - `VRMRepositoryImpl`: ロード開始/終了、validator結果
  - `VRMValidator`: サイズ取得方法と値
  - `CameraViewModel` / `AvatarFeature`: `currentAvatar` 更新有無
