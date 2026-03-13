package com.example.vtubercamera.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedModuleIntegrationTest {
    @Test
    fun canCallSharedStoreFromAndroidModule() {
        val store = com.example.vtubercamera.shared.SharedCameraStore()
        store.markCaptured()
        assertEquals(1, store.uiState.value.captureCount)
    }
}
