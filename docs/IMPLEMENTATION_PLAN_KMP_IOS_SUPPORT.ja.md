# Kotlin Multiplatform 化 実装計画（iOS 対応）

## 1. 目的とゴール

### 目的
- 既存 Android アプリ（Jetpack Compose + CameraX + MVVM）の資産を最大限活用しつつ、iOS でも同等体験を提供する。
- ビジネスロジック・状態管理・一部ドメイン層を共通化し、将来的な機能追加コストを削減する。

### ゴール（初期リリース）
- Android / iOS の両方で以下を提供する。
  - カメラプレビュー表示
  - 写真撮影
  - フロント/バックカメラ切り替え
  - フラッシュ制御（可能な範囲）
  - 撮影画像の確認・削除
- UI は当面ネイティブ実装（Android: Compose、iOS: SwiftUI）とし、ドメイン/状態管理の共通化を優先する。

### 非ゴール（初期段階ではやらない）
- 既存 Android UI を Compose Multiplatform で完全共通化する。
- AR / VRM 表示の完全共通化（iOS は別実装の調査が必要）。

---

## 2. 現状整理（Android 実装）

- Android 固有依存が UI 層とカメラ実装に集中している。
  - Jetpack Compose
  - CameraX
  - Android 権限 API
- `CameraViewModel` 周辺は、設計次第で共通化余地がある（状態遷移やユースケースなど）。

このため、**段階的な KMP 化**（core → platform bridge → iOS UI）が最もリスクが低い。

---

## 3. 推奨アーキテクチャ

## 3.1 モジュール構成（案）

- `:shared`
  - KMP 共通モジュール
  - `commonMain`: ドメインモデル、ユースケース、状態、Repository 抽象
  - `androidMain`: Android 実装（既存ロジックとの接続）
  - `iosMain`: iOS 実装（AVFoundation ラッパ）
- `:app`（既存 Android アプリ）
  - Compose UI + Android 依存の実装を保持
  - `:shared` を利用
- `:iosApp`（新規 Xcode/Gradle 連携）
  - SwiftUI + `shared` フレームワーク連携

## 3.2 レイヤ分割方針

- **共通化する層**
  - ドメインモデル（CameraState, FlashMode, CaptureResult など）
  - ユースケース（撮影要求、モード切替、エラー処理ポリシー）
  - 状態管理（StateFlow ベースの Store / ViewModel 相当）
- **プラットフォーム別に残す層**
  - カメラデバイス制御（CameraX / AVFoundation）
  - 権限ダイアログ起動
  - プレビュー描画（PreviewView / AVCaptureVideoPreviewLayer）

---

## 4. 実装フェーズ

## Phase 0: 事前調査（1〜1.5 週間）

1. KMP と iOS 連携の技術検証（PoC）
   - Kotlin 2.x + KMP plugin で `shared` を生成
   - SwiftUI から `StateFlow` を購読できることを確認
2. iOS カメラ要件確認
   - `AVFoundation` で必要機能（切替/フラッシュ/撮影）を実現可能か
3. CI の方針決定
   - Android build + shared tests
   - iOS build（最低限 `xcodebuild`）

**完了条件**
- PoC で Android/iOS 双方から shared 呼び出し成功
- 技術的ブロッカーなし（または回避策確定）

## Phase 1: 共通ドメイン層抽出（1.5〜2 週間）

1. `:shared` モジュールを追加
2. 既存 ViewModel のうちプラットフォーム非依存ロジックを共通化
3. Repository / Controller の interface 化
4. Android 側実装を adapter 化して差し替え

**完了条件**
- Android アプリが `:shared` 経由で動作
- 主要状態遷移のユニットテストが `commonTest` でパス

## Phase 2: iOS カメラ実装（2〜3 週間）

1. `iosMain` に AVFoundation bridge 実装
2. `expect/actual` または interface DI で platform 実装を注入
3. 権限・ライフサイクル対応
4. エラーハンドリング統一（権限拒否、カメラ利用不可など）

**完了条件**
- iOS 実機でプレビュー/撮影/切替が動作
- 主要エラーケースが UI に反映

## Phase 3: iOS UI 実装（1.5〜2.5 週間）

1. SwiftUI で Camera 画面を実装
2. `shared` 状態を監視して UI と同期
3. 画像プレビュー・削除フロー実装
4. 最低限のローカライズ（日/英）

**完了条件**
- Android と機能パリティの MVP 完了
- 主要ユーザーフロー E2E 手動確認完了

## Phase 4: 品質・運用整備（1〜1.5 週間）

1. テスト強化
   - commonTest ユニットテスト
   - Android instrumentation（既存）
   - iOS UI smoke test（可能な範囲）
2. CI/CD 整備
   - shared test
   - Android assemble
   - iOS build
3. 開発ガイド整備
   - モジュール責務
   - 実装規約

**完了条件**
- main ブランチで両OSビルドが安定
- リリース候補を配布可能

---

## 5. 技術選定の推奨

- **UI 戦略（初期）**: Android Compose / iOS SwiftUI のハイブリッド
  - 理由: カメラUIはネイティブ最適化の恩恵が大きく、初期工数を抑えやすい
- **非同期/状態管理**: Kotlin Coroutines + StateFlow（shared）
- **DI**: シンプルな手動DIから開始（必要なら Koin KMP へ拡張）
- **データ永続化（将来）**: Settings なら Multiplatform Settings、DB が必要なら SQLDelight

---

## 6. リスクと対策

1. **iOS カメラ仕様差異**
   - リスク: Android と完全同等の制御が難しい
   - 対策: 共通 API を「最小公倍数」で設計し、拡張点を reserved

2. **Flow と SwiftUI 連携コスト**
   - リスク: 状態反映やライフサイクルで不整合
   - 対策: 購読ラッパを早期に標準化、サンプル実装をテンプレ化

3. **既存 Android コードの移設コスト**
   - リスク: 一括移行で不安定化
   - 対策: フェーズ単位で adapter を挟み、段階的に置換

4. **CI 時間増加**
   - リスク: 開発サイクルが遅くなる
   - 対策: PR 時は smoke、nightly でフルビルド/フルテスト

---

## 7. マイルストーン（目安 6〜10 週間）

- M1（Week 1〜2）: PoC 完了・設計確定
- M2（Week 3〜4）: shared 導入・Android 接続完了
- M3（Week 5〜7）: iOS カメラ機能 MVP 完了
- M4（Week 8〜10）: 品質改善・CI/CD 整備・RC 配布

---

## 8. 直近アクション（次スプリント）

1. `:shared` モジュールの雛形作成
2. `CameraState` / `FlashMode` / `CaptureCommand` の共通モデル定義
3. Android 側で shared ViewModel 相当に接続する最小パス実装
4. iOS PoC（SwiftUI + shared state 表示のみ）作成
5. 移行設計レビュー（30〜60分）を実施し、Phase 1 backlog を確定

---

## 9. 成果物定義

- 設計ドキュメント（本書）
- KMP モジュール構成図
- 共通 API 仕様（Interface + データモデル）
- iOS 連携ガイド（Xcode セットアップ含む）
- CI 設定ドキュメント

---

## 10. 方針比較: 作り直し vs 現行コードを改修

本プロジェクトの現状（Android 版が既に動作し、カメラ機能・MVVM・状態管理の資産がある）を前提にすると、
**工数が少ないのは「現行コードを直しながら KMP 化する」アプローチ**。

### 10.1 2案の比較

| 観点 | 1から作り直し | 現行コードを改修しつつ KMP 化 |
|---|---|---|
| 初期設計自由度 | 高い | 中程度（既存制約あり） |
| 既存資産の再利用 | 低い | 高い |
| MVP 到達速度 | 遅い | 速い |
| 品質リスク | 新規バグが多くなりやすい | 既存挙動を比較しながら進められる |
| 工数見積もり（目安） | 10〜16 週間 | 6〜10 週間 |
| リリースまでの不確実性 | 高い | 中程度 |

### 10.2 なぜ改修型が有利か

1. 既存 Android 実装の「正解データ」がある
   - 画面遷移、権限フロー、撮影後処理などの期待挙動が明確。
2. KMP の主目的（共通化）はドメイン/状態層にある
   - UI・カメラデバイス制御は OS 差異が大きく、全面作り直しのメリットが小さい。
3. 段階移行で失敗コストを分割できる
   - Phase ごとに Android が動き続けるため、手戻りの影響範囲を局所化できる。

### 10.3 例外的に「作り直し」が有効なケース

- 現行アーキテクチャが破綻しており、改修コストが恒常的に高い。
- 要件が大幅に変わり、既存 UI/UX やユースケースをほぼ維持しない。
- 既存コードの品質問題（テスト不能・依存循環など）が深刻で、段階移行が成立しない。

現時点の情報では上記条件が明確ではないため、
**推奨方針は「現行コード改修 + 段階的 KMP 化」**とする。
