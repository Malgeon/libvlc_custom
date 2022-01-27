package com.example.libvlc_custom.player.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object ThreadUtils {

    /**
     * Execute the provided runnable on a background thread.
     *
     * @param runnable The runnable instance.
     */
    fun onBackground(runnable: Runnable?) {
        Executors.newSingleThreadExecutor().execute(runnable)
    }

    /**
     * Execute the provided runnable on a main thread.
     *
     * @param runnable The runnable instance.
     */
    fun onMain(runnable: Runnable) {
        val mainLooper = Looper.getMainLooper()

        // Already on main thread, execute and return.
        if (Thread.currentThread() === mainLooper.thread) {
            runnable.run()
            return
        }

        // Push work to main thread and execute.
        Handler(mainLooper).post(runnable)
    }
}
