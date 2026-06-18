package com.reyzie.hymns.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

class HymnCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val appId = CastService.getCachedAppId(context)
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(
                NotificationOptions.Builder()
                    .setTargetActivityClassName(com.reyzie.hymns.MainActivity::class.java.name)
                    .build()
            )
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(appId)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
