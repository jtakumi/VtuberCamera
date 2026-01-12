# Repository overview (for GitHub Copilot)

## What this repo is
- Product: VTuber Camera（Android）
- Purpose: Jetpack ComposeベースのVTuber/コンテンツクリエイター向けカメラアプリ。リアルタイムプレビューと写真撮影を提供。
- Primary modules: app
- Tech stack: Kotlin, Gradle, Jetpack Compose, Hilt, Coroutines/StateFlow, CameraX, Material 3, Coil

## How to navigate
- Entry points
  - App: `:app` (`app/src/main/java/com/example/vtubercamera/MainActivity.kt`, `VtuberCameraApp.kt`)
  - Domain: `app/src/main/java/com/example/vtubercamera/ui/viewmodels` (状態管理とユースケース相当)
  - Data: `app/src/main/java/com/example/vtubercamera/data` (Repository/データモデル)
  - UI: `app/src/main/java/com/example/vtubercamera/ui` (Compose screens/components/theme)
- Architecture rules
  - UI -> Domain -> Data の依存方向を守る
  - Android framework 依存は UI 層に寄せる
  - Side effects は境界（Repository等）に閉じ込める

## Coding conventions
- Kotlin style: 既存のコードスタイルに合わせる（Compose/MVVM）。
- Naming:
  - Composable: `PascalCase`
  - ViewModel: `*ViewModel`
  - Repository: `XxxRepository` / `XxxRepositoryImpl`
- Nullability: 原則 non-null、例外は理由コメント必須
- Threading: suspend/Coroutine を基本、dispatcher は注入可能に

## Build & test (local)
### Quick start
- Build:
  - `./gradlew assembleDebug`
- Unit tests (JVM):
  - `./gradlew testDebugUnitTest`
- Lint/static analysis:
  - `./gradlew lintDebug`

### CI expectations
- PR では少なくとも以下が通ること:
  - unit tests
  - lint

## How to make changes safely
- Prefer small diffs; keep behavior changes isolated
- When changing production logic:
  - add/update unit tests first (or in same PR)
  - explain risk & rollback notes in PR description
- Avoid:
  - “とりあえず通すため”のテスト弱体化
  - 依存方向の逆転（UIがDataに直依存など）

## Common gotchas (project-specific)
- Flavors/buildTypes:
  - Available: debug/release
- Versioning:
  - minSdk: 25
  - targetSdk: 36
  - compileSdk: 36
- Feature flags / BuildConfig:
  - 現状は特別な機構なし

## What I want Copilot to do
- When asked to implement code:
  - follow existing architecture & module boundaries
  - include tests for non-trivial logic
  - show exact Gradle commands to validate
- When unsure:
  - search existing patterns in the repo before inventing new abstractions
