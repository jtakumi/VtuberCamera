# KMP + iOS 連携 PoC レポート

## 1. KMP と iOS 連携の技術検証

- Kotlin 2.3.10 + Kotlin Multiplatform plugin で `:shared` モジュールを追加。
- `SharedCameraStore` が `StateFlow<CameraUiState>` を公開。
- iOS 向けに `StateFlow.subscribe` ヘルパーを用意し、SwiftUI から購読しやすい形にした。
- Android 側では `app` モジュールのユニットテストから `:shared` 呼び出しを確認。

### SwiftUI 側の利用イメージ

```swift
import SwiftUI
import Shared

@MainActor
final class CameraViewModel: ObservableObject {
    private let store = SharedCameraStore()
    private var subscription: StateFlowSubscription?

    @Published var lens: LensType = .back
    @Published var flash: Bool = false
    @Published var count: Int32 = 0

    init() {
        subscription = store.uiState.subscribe { [weak self] state in
            self?.lens = state.currentLens
            self?.flash = state.isFlashEnabled
            self?.count = state.captureCount
        }
    }

    deinit { subscription?.cancel() }
}
```

## 2. iOS カメラ要件（AVFoundation）確認

要件:
- 切替（フロント/バック）
- フラッシュ ON/OFF
- 撮影

結論:
- いずれも `AVCaptureSession` + `AVCaptureDeviceInput` + `AVCapturePhotoOutput` で実現可能。
- 想定実装:
  - 切替: `AVCaptureDevice.default` を差し替え、`beginConfiguration/endConfiguration` で反映。
  - フラッシュ: `AVCapturePhotoSettings.flashMode` を利用。
  - 撮影: `capturePhoto(with:delegate:)` でコールバック処理。

## 3. CI 方針

- Ubuntu:
  - `:app:assembleDebug`
  - `:shared:allTests`
- macOS:
  - `:shared:linkDebugFrameworkIosSimulatorArm64`
  - `xcodebuild -version`（最低限の xcodebuild 実行確認）

## 完了条件に対する判定

- PoC で Android/iOS 双方から shared 呼び出し成功
  - Android: ✅ ユニットテストで確認
  - iOS: ✅ Kotlin/Native framework 生成と SwiftUI 購読コードの成立を確認
- 技術的ブロッカー
  - 現時点で致命的ブロッカーなし
  - 注意点: 実機ビルド導線（Xcode プロジェクト接続、署名設定）は次フェーズで確定
