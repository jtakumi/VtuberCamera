package com.example.vtubercamera.data.vrm

import android.content.Context
import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ErrorRecoveryManagerTest {

    private lateinit var context: Context
    private lateinit var recoveryManager: ErrorRecoveryManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        recoveryManager = ErrorRecoveryManager(context)
    }

    @Test
    fun `executeRecoveryAction REQUEST_PERMISSIONS should request file re-selection without opening settings`() {
        val errorState = ErrorState(
            type = ErrorType.VRM_PERMISSION_DENIED,
            message = "Permission denied",
            userMessage = "Permission denied",
            severity = ErrorSeverity.HIGH,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.REQUEST_PERMISSIONS),
            context = ErrorContext.vrmLoading("content://test.vrm")
        )

        val result = recoveryManager.executeRecoveryAction(ErrorAction.REQUEST_PERMISSIONS, errorState)

        assertEquals(
            RecoveryResult.ActionRequired("Please re-select the file to re-grant document access and try again"),
            result
        )
        verify(exactly = 0) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `executeRecoveryAction OPEN_SETTINGS should open app settings`() {
        val errorState = ErrorState(
            type = ErrorType.FILE_PERMISSION_DENIED,
            message = "Permission denied",
            userMessage = "Permission denied",
            severity = ErrorSeverity.HIGH,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.OPEN_SETTINGS),
            context = ErrorContext.fileAccess("content://test.vrm")
        )

        val result = recoveryManager.executeRecoveryAction(ErrorAction.OPEN_SETTINGS, errorState)

        assertEquals(
            RecoveryResult.ActionRequired("Please check app settings and try again"),
            result
        )
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }
}
