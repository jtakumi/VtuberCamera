package com.example.vtubercamera.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedModuleIntegrationTest {
    @Test
    fun canCallSharedStoreFromAndroidModule() {
        val store = com.example.vtubercamera.shared.SharedCameraStore()
        store.markCaptured()
        assertEquals(1, store.uiState.value.captureCount)
    }
}
