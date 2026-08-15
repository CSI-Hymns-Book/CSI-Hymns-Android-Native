package com.reyzie.hymns

import android.app.Application
import com.reyzie.hymns.data.ConsentManager

class HymnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ConsentManager.init(this)
    }
}
