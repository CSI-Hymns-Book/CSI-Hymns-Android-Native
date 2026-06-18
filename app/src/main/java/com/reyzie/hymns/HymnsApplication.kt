package com.reyzie.hymns

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class HymnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
        }
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
    }

    companion object {
        private const val ONESIGNAL_APP_ID = "29f2a6ba-3f56-4ffe-8075-3b70d7440b13"
    }
}
