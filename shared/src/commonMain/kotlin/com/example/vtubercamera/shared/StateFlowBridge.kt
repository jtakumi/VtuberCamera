package com.example.vtubercamera.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StateFlowSubscription internal constructor(
    private val parentJob: Job
) {
    fun cancel() = parentJob.cancel()
}

fun <T> StateFlow<T>.subscribe(onEach: (T) -> Unit): StateFlowSubscription {
    val parentJob = SupervisorJob()
    val scope = CoroutineScope(parentJob + Dispatchers.Main)
    scope.launch {
        collect { value -> onEach(value) }
    }
    return StateFlowSubscription(parentJob)
}
