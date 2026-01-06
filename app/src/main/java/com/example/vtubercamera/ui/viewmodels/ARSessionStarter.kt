package com.example.vtubercamera.ui.viewmodels

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.vrm.ARError
import javax.inject.Inject

class ARSessionStarter @Inject constructor(
    private val arRepository: ARRepository,
) {
    suspend fun start(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onSessionReady: () -> Unit,
        onError: (ARError) -> Unit,
    ) {
        arRepository.initializeSession(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onSessionReady = onSessionReady,
            onError = onError,
        )
    }
}
