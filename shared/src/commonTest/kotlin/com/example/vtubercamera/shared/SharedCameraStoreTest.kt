package com.example.vtubercamera.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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

    @Test
    fun markCapturedCountsConcurrentCaptures() = runTest {
        val store = SharedCameraStore()
        val captureCount = 1_000

        coroutineScope {
            repeat(captureCount) {
                launch(Dispatchers.Default) {
                    store.markCaptured()
                }
            }
        }

        assertEquals(captureCount, store.uiState.value.captureCount)
    }
}
