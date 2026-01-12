---
name: investigation-and-implementation-plan
description: Use when asked to create an investigation report (調査書) and an implementation plan (実装計画書) for a refactor or feature change.
---

# Skill Instructions

## When to use
- ユーザーから「調査書を作成して」「その調査書をベースに実装計画書を作って」と依頼されたとき
- 大きめのリファクタ（例: View直接操作 → MVVM/StateFlow 化、責務分割、依存方向の整理）を進める前の整理が必要なとき

## Inputs you should collect (ask only if missing)
- 対象画面・機能の名称（例: 画面A）
- 既存コードの入口（例: `screens/ScreenA.kt`）と関連ユーティリティ（例: `utils/DataHelper.kt`）
- 既存の問題点/目的（なぜMVVM化するか、どう改善したいか）
- 受け入れ条件（挙動を変えない/一部改善する、など）

## Investigation report (調査書) expectations
調査書は「現状把握」と「影響範囲」「リスク」「実装順序の提案」までを、根拠（コード上の観察）付きでまとめる。

### Required sections
- **現象 / 背景**: 何を実装（またはリファクタ）したいか
- **期待するゴール**: MVVM化で達成したい状態（状態管理、責務分離、テスト容易性など）
- **調査範囲**:
  - 主要ファイル（例: `screens/ScreenA.kt`, `utils/DataHelper.kt`）
  - 依存関係（ViewModel/Repository/UseCase/Helper/DI/Navigation 等）
- **影響範囲の特定**（調査項目1）:
  - 直接/間接参照（呼び出し元/呼び出し先）
  - UI状態の流れ（UI→VM→Domain→Data の依存方向が守れているか）
  - テスト・モックの置き場
- **考慮すべきリスク**（調査項目2）:
  - 既存動作への影響（イベント順序、初期化タイミング、ライフサイクル）
  - パフォーマンス（メインスレッド負荷、再composition、IO/Dispatcher）
  - 移行中の二重管理（旧Stateと新Stateが混在する期間の破綻）
- **実装順序の提案**（調査項目3）:
  - 段階移行（1PRで全部やらず、差分を小さくする）
  - 先にテスト可能な層から整備（例: DataHelper→Repository化、VMの状態モデル化）
- **未確定事項 / 要確認**: 追加でユーザー確認が必要な点

### Output file
- `docs/INVESTIGATION_<TOPIC>.md`

## Implementation plan (実装計画書) expectations
計画書は「意図/目的」→「仕様」→「変更箇所」→「手順」→「テスト」→「注意事項」の順で、実装に着手できる粒度まで落とす。

### Required sections
1. **実装の意図と目的**（なぜやるか、何を良くするか）
2. **詳細な仕様**
   - UI state の設計（StateFlow, UiState data class, one-shot event 方針）
   - 画面Aのイベント一覧（ユーザー操作→VMイベント→State更新→UI反映）
   - `DataHelper` の責務整理（同期/非同期、キャッシュ、エラーハンドリング）
3. **修正が必要なファイルと箇所**
   - 例: `screens/ScreenA.kt`（View直接操作を除去し、状態駆動に）
   - 例: `utils/DataHelper.kt`（Repository/UseCaseへの移行、suspend化、Dispatcher注入 等）
   - ViewModel/DI/Hilt module/テスト追加箇所
4. **実装順序（段階的なステップ）**
   - Stepごとに「目的」「変更内容」「完了条件」を書く
   - 既存機能の挙動維持が必要なら、feature flag/adapter を用いた段階移行も検討
5. **テスト項目**
   - Unit test（ViewModel, repository, helperの変換ロジック）
   - 重要な回帰観点（初期表示、権限、エラー、再表示、回転/バックグラウンド復帰 等）
   - 実行コマンド例: `./gradlew testDebugUnitTest`, `./gradlew lintDebug`
6. **注意事項とリスク対策**
   - 破壊的変更を避けるための互換層
   - スレッド/ライフサイクル/再compositionの落とし穴

### Output file
- `docs/IMPLEMENTATION_PLAN_<TOPIC>.md`

## Deliverable
- 調査書と実装計画書をそれぞれ1ファイルずつ作成し、相互参照（計画書冒頭に「調査書: <link>」）を入れる
- 可能なら変更箇所をコード記号レベルで具体化（関数名、State名、責務境界）
- 計画は「小さくマージできる単位（PR単位）」で区切る
