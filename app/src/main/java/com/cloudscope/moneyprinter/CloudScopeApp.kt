package com.cloudscope.moneyprinter

import android.app.Application
import android.os.Looper
import android.util.Log

class CloudScopeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isMainThread = Looper.getMainLooper().thread == thread
            Log.e(
                "CloudScopeApp",
                "Uncaught exception on thread ${thread.name} (isMainThread: $isMainThread)",
                throwable
            )
            if (isMainThread) {
                defaultHandler?.uncaughtException(thread, throwable)
            } else {
                // Background coroutines, network or audio threads should never crash the user process
                Log.w("CloudScopeApp", "Suppressed background exception to maintain app stability", throwable)
            }
        }
    }
}
