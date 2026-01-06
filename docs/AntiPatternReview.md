# Anti-Pattern Review

This document summarizes anti-patterns identified in the current codebase with respect to common Android architecture guidance (e.g., avoiding fat ViewModels, keeping domain logic platform-agnostic).

## Fat ViewModel responsibilities
- `CameraViewModel` orchestrates camera controls, gallery management, AR mode toggling, avatar loading/cleanup, and lighting state within a single class. The ViewModel subscribes directly to multiple repository flows, initializes capabilities, and delegates numerous feature operations (photo capture/deletion, AR enable/disable, avatar management, etc.) from one place, resulting in a class exceeding 1,000 lines of mixed responsibilities.
  - Initialization pulls photo streams, tracks latest media, sets up camera capabilities, and bootstraps avatar/AR observers in the constructor scope.【F:app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt†L322-L328】
  - The same class handles gallery mutations such as deleting single/multiple photos and clearing captured images, alongside AR lifecycle and avatar controls.【F:app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt†L448-L520】【F:app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt†L685-L760】【F:app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt†L1009-L1080】
  - Concentrating disparate concerns here risks the "fat ViewModel" anti-pattern noted in the design guidance.

## Context-dependent domain logic (addressed)
- `ARFeature` no longer requires `Context` or `LifecycleOwner`; session start is injected as a callback, keeping domain logic platform-agnostic while preserving repository-driven AR observations.【F:app/src/main/java/com/example/vtubercamera/domain/ar/ARFeature.kt†L1-L188】【F:app/src/main/java/com/example/vtubercamera/ui/viewmodels/CameraViewModel.kt†L625-L678】

## Repository scope creep (potential)
- Because `ARFeature` invokes session initialization and error handling directly through `ARRepository` while also updating UI-related flags, the boundary between data acquisition and UI state orchestration is blurred. Further separating platform/session management from domain orchestration would reduce the risk of the repository or feature layer becoming an all-purpose coordinator.
