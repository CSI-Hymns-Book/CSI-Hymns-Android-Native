package com.reyzie.hymns

import android.app.Application
import com.reyzie.hymns.data.HymnsFirebaseMessagingService

class HymnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Auto-subscribe device to default FCM topics
        HymnsFirebaseMessagingService.subscribeToDefaultTopics()
    }
}
