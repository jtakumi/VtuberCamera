package com.example.vtubercamera.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCameraStoreTest {
    @Test
    fun updatesStateWithCameraFlashAndCapture() {
        val store = SharedCameraStore()

        store.switchCamera()
        store.setFlashEnabled(true)
        store.markCaptured()

        val state = store.uiState.value
        assertEquals(LensType.FRONT, state.currentLens)
        assertEquals(true, state.isFlashEnabled)
        assertEquals(1, state.captureCount)
    }
}
