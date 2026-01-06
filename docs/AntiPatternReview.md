# Anti-Pattern Review

This document summarizes anti-patterns identified in the current codebase with respect to common Android architecture guidance (e.g., avoiding fat ViewModels, keeping domain logic platform-agnostic).

## Fat ViewModel responsibilities
- `CameraViewModel` is still a large class (>1,000 lines) and remains the central coordinator for camera UI, gallery UI, AR toggling, avatar controls, and lighting-related UI state. While several responsibilities have been extracted into “Feature” classes, the ViewModel still wires many inputs/outputs together and maintains a large UI state surface.
  - **Improved (but not eliminated):** repository subscriptions / bootstrapping were extracted into `CameraViewModelBootstrapper`.
    - Photo streams (`MediaRepository.getAllPhotos()` / `getARPhotos()` / `getNormalPhotos()` / `getLatestPhotoUri()`) are collected in `CameraViewModelBootstrapper`, not directly inside the ViewModel.
    - Lens capability initialization and avatar library bootstrap/observers are also started from `CameraViewModelBootstrapper`.
    - References: [app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt](../app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt), [app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModelBootstrapper.kt](../app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModelBootstrapper.kt)
  - **Still “fat” due to UI surface area:** the ViewModel exposes many derived `StateFlow`s “for backward compatibility” on top of a single `CameraUiState`, which materially contributes to file size and mixed concerns.
  - **Direct repository calls remain:** some operations are still implemented directly in the ViewModel (e.g., normal photo capture via `CameraRepository.capturePhoto`, camera switching callback wiring, and AR photo capture/compositing logic), which keeps camera/media concerns anchored in the ViewModel.
  - Net: responsibility extraction is underway, but `CameraViewModel` is still at risk of becoming an “all-purpose coordinator” due to API surface and cross-feature wiring.

## Context-dependent domain logic (addressed)
- `ARFeature` no longer requires `Context` or `LifecycleOwner`. Instead, session start is injected as a callback (`startSession`), keeping the feature logic decoupled from Android component lifecycles.
  - The platform-bound session initialization is encapsulated in `ARSessionStarter`, which delegates to `ARRepository.initializeSession(context, lifecycleOwner, ...)`.
  - References: [app/src/main/java/com/example/vtubercamera/domain/ar/ARFeature.kt](../app/src/main/java/com/example/vtubercamera/domain/ar/ARFeature.kt), [app/src/main/java/com/example/vtubercamera/ui/viewmodels/ARSessionStarter.kt](../app/src/main/java/com/example/vtubercamera/ui/viewmodels/ARSessionStarter.kt)

## Repository scope creep (potential)
- `ARRepository` remains strongly platform-oriented (it requires `Context`/`LifecycleOwner` and owns session lifecycle methods like `initializeSession()` / `pauseSession()` / `resumeSession()` / `destroySession()`), which is expected for an ARCore-backed implementation.
- However, the boundary between “domain feature” and “UI orchestration” is still somewhat blurred:
  - `ARFeature` updates UI-related state (e.g., toggling avatar visibility based on tracking state) and triggers session teardown (`destroySession()`), so it functions as both a coordinator and a state-to-UI mapper.
  - If the architecture continues to grow, consider further splitting responsibilities so that the AR layer provides state/events, and the UI layer decides how to project that into UI state (e.g., avatar visibility/transform resets).
  - Reference: [app/src/main/java/com/example/vtubercamera/data/ARRepository.kt](../app/src/main/java/com/example/vtubercamera/data/ARRepository.kt)
