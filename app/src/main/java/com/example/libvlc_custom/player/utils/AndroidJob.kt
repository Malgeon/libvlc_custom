package com.example.libvlc_custom.player.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Job

/**
 * A Kotlin coroutine [@see Job] that cancels itself when the lifecycle it's
 * bound to is destroyed. This class can be used as a parent job to prevent
 * memory leaks and null reference exceptions.
 */
class AndroidJob(lifecycle: Lifecycle) : Job by Job(), LifecycleObserver {
    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() = cancel()
}