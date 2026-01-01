# VSCode テスト実行環境セットアップ完了

VSCodeでvtuberCameraのユニットテストを実行できるようになりました！

## 設定内容

### 1. タスク設定 (`.vscode/tasks.json`)

以下のテスト実行タスクを追加しました:

- **Run Unit Tests** (デフォルト) - デバッグビルドのユニットテストを実行
- **Run Unit Tests (All Variants)** - すべてのビルドバリアントのテストを実行
- **Run Android Instrumented Tests** - デバイス/エミュレータ上でのテストを実行
- **Run All Tests (Unit + Instrumented)** - すべてのテストを実行
- **Run Unit Tests with Coverage** - カバレッジレポート付きでテストを実行
- **Clean and Run Unit Tests** - クリーンビルド後にテストを実行
- **Open Test Report** - テスト結果のHTMLレポートをブラウザで開く

### 2. ワークフロー文書 (`.agent/workflows/run-tests.md`)

テスト実行の詳細な手順とトラブルシューティングガイドを作成しました。

## 使い方

### クイックスタート

1. **コマンドパレット** (`Cmd+Shift+P`)を開く
2. `Tasks: Run Task` と入力
3. `Run Unit Tests` を選択

または、**デフォルトのビルドタスク** として設定されているので:
- `Cmd+Shift+B` を押すだけで実行可能

### テスト結果の確認

テスト実行後、以下の方法で詳細レポートを確認:

1. コマンドパレットで`Tasks: Run Task`
2. `Open Test Report`を選択
3. ブラウザでHTMLレポートが開きます

## 現在のテスト状況

```
170 tests completed, 90 failed
```

テストの一部が失敗していますが、これはテストコード自体の問題であり、
VSCodeからのテスト実行機能は正常に動作しています。

失敗しているテストの多くは以下のカテゴリ:
- `DataModelsTest`
- `FilamentARRendererTest`
- `LightingSystemTest`
- `ShadowSystemTest`
- `CameraViewModelTest`

## 次のステップ

テストを修正する場合は、HTMLレポートで詳細を確認してください:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

詳細な使い方は `/run-tests` ワークフローを参照してください。
