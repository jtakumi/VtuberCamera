# Camera Zoom Implementation with ScaleGestureDetector

このプロジェクトに、ScaleGestureDetectorを使用したカメラのズーム機能を実装しました。

## 実装された機能

### 1. ピンチズーム (Pinch-to-Zoom)
- 2本の指でピンチイン/アウトすることでカメラのズームを制御
- スムーズなズーム操作
- 最小/最大ズーム時のハプティックフィードバック

### 2. ダブルタップでズームリセット
- 画面をダブルタップすると、ズームを1.0xにリセット
- アニメーション付きでスムーズにリセット

### 3. ズームスライダー
- 画面下部にズームスライダーを配置
- スライダーでの精密なズーム調整が可能

### 4. ズーム情報表示
- 画面右上に現在のズーム倍率を表示
- 最小・最大ズーム範囲も表示

### 5. ズームボタン
- 画面左下にズームイン/アウトボタンを配置
- 0.5倍刻みでのズーム調整

## 実装ファイル

### 新規作成ファイル

1. **ModernCameraGestures.kt**
   - パス: `/app/src/main/java/com/example/vtubercamera/ui/modifiers/`
   - ComposeのジェスチャーAPIを利用したカスタムModifier
   - ピンチズームとダブルタップ、ハプティックフィードバック対応

2. **PinchToZoomModifier.kt** (シンプル版)
   - パス: `/app/src/main/java/com/example/vtubercamera/ui/modifiers/`
   - ピンチズームのみのシンプルなModifier
   - 後方互換性のため保持

### 修正ファイル

1. **CameraScreen.kt**
   - `cameraGestures` modifierを追加
   - ズーム情報表示UI追加
   - ズームスライダー追加
   - ズームボタンのスタイル改善

2. **CameraViewModel.kt**
   - `smoothZoomTo()` メソッド追加（アニメーション付きズーム）
   - `resetZoom()` メソッド追加（ズームリセット）

## 使用方法

アプリを起動後、カメラ画面で以下の操作が可能です：

1. **ピンチジェスチャー**: 2本の指で画面をピンチしてズーム
2. **ダブルタップ**: 画面をダブルタップしてズームを1.0xにリセット
3. **スライダー**: 画面下部のスライダーでズーム調整
4. **ボタン**: 左下のズームイン/アウトボタンでズーム調整

## 技術的な詳細

### ScaleGestureDetector
Androidの標準的なジェスチャー検出器を使用して、ピンチジェスチャーを検出します。

```kotlin
ScaleGestureDetector(
    context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newZoom = (currentZoom * scaleFactor).coerceIn(minZoom, maxZoom)
            onScale(newZoom)
            return true
        }
    }
)
```

### Compose統合
`pointerInteropFilter`を使用して、ComposeのUIとAndroidのタッチイベントシステムを統合しています。

### ハプティックフィードバック
最小/最大ズームに達した際に、ユーザーに触覚的なフィードバックを提供します。

## 依存関係

必要な依存関係はすでに`build.gradle`に含まれています：
- CameraX
- Jetpack Compose
- AndroidX Core

## 今後の拡張可能性

- フォーカスポイントを中心としたズーム
- ズームプリセット（2x、5x、10xなど）
- ズーム速度の調整
- カスタムズームアニメーション
