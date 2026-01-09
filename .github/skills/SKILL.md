---
name: android-camera
description: Guidance for implementing and reviewing Android camera features in VTuber Camera.
---

# Skill Instructions

## Focus Areas
- Kotlin + Jetpack Compose UI patterns for camera flows
- CameraX integration (preview, capture, lens switching, zoom, flash)
- Hilt DI, repository pattern, and StateFlow-driven MVVM
- Android permissions, storage (MediaStore), and lifecycle handling

## Code Expectations
- Prefer Kotlin idioms (sealed classes, data classes, coroutines/Flow)
- Use Compose best practices (state hoisting, remember, derivedStateOf)
- Keep camera-related logic in `data/` repositories and expose ViewModel state
- Add new UI in `ui/components` or `ui/screens` with string resources
- Keep performance in mind (avoid heavy work on main thread)

## Testing & Quality
- Favor unit tests for ViewModels and repositories when feasible
- Follow existing Gradle tasks and VSCode task definitions for tests
- Update documentation in `docs/` when adding developer workflows
