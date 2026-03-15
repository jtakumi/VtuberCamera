package com.example.vtubercamera.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StateFlowSubscription internal constructor(
    private val job: Job
) {
    fun cancel() = job.cancel()
}

fun <T> StateFlow<T>.subscribe(onEach: (T) -> Unit): StateFlowSubscription {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val job = scope.launch {
        collect { value -> onEach(value) }
    }
    return StateFlowSubscription(job)
}
