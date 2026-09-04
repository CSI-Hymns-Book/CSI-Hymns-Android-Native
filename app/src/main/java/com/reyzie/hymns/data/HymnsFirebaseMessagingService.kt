package com.reyzie.hymns.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reyzie.hymns.MainActivity
import com.reyzie.hymns.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class HymnsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HymnsFCM"
        const val CHANNEL_ANNOUNCEMENTS = "csi_hymns_announcements"
        const val CHANNEL_DAILY = "csi_hymns_daily"
        const val PREF_FCM_TOKEN = "fcm_token"
        const val ACTION_NOTIFICATION_RECEIVED = "com.reyzie.hymns.ACTION_NOTIFICATION_RECEIVED"

        fun fetchAndSyncToken(context: Context) {
            if (!ConsentManager.pushConsent.value) return
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d(TAG, "DEVICE FCM TOKEN REGISTERED: ${token.take(8)}***")

                        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString(PREF_FCM_TOKEN, token)
                            .apply()

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                SupabaseService.getInstance().updateProfileFcmToken(token)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to sync FCM token to Supabase profile", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching FCM token", e)
            }
        }

        fun subscribeToDefaultTopics(context: Context? = null) {
            if (!ConsentManager.pushConsent.value) return
            context?.let { fetchAndSyncToken(it) }
            try {
                val fcm = FirebaseMessaging.getInstance()
                fcm.subscribeToTopic("all_users")
                fcm.subscribeToTopic("announcements")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to FCM topics", e)
            }
        }

        fun optOutPush() {
            try {
                val fcm = FirebaseMessaging.getInstance()
                fcm.unsubscribeFromTopic("all_users")
                fcm.unsubscribeFromTopic("announcements")
                fcm.unsubscribeFromTopic("kannada_hymns")
                fcm.unsubscribeFromTopic("english_hymns")
            } catch (e: Exception) {
                Log.e(TAG, "Error opting out of FCM topics", e)
            }
        }

        fun syncUserLanguageTopics(isKannada: Boolean = true, isEnglish: Boolean = true) {
            if (!ConsentManager.pushConsent.value) return
            try {
                val fcm = FirebaseMessaging.getInstance()
                if (isKannada) fcm.subscribeToTopic("kannada_hymns") else fcm.unsubscribeFromTopic("kannada_hymns")
                if (isEnglish) fcm.subscribeToTopic("english_hymns") else fcm.unsubscribeFromTopic("english_hymns")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing language topics", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: ${token.take(8)}***")
        
        getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_FCM_TOKEN, token)
            .apply()

        if (ConsentManager.pushConsent.value) {
            subscribeToDefaultTopics(this)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (!ConsentManager.pushConsent.value) return
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "CSI Hymns Book"
            
        val message = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["message"] 
            ?: ""

        val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            ?: remoteMessage.data["image_url"]
            ?: remoteMessage.data["image"]

        val targetScreen = remoteMessage.data["target_screen"] 
            ?: remoteMessage.data["deep_link"]

        val channelId = remoteMessage.data["channel_id"] ?: CHANNEL_ANNOUNCEMENTS

        // Broadcast locally for in-app banner if app is open
        sendInAppBroadcast(title, message, targetScreen, imageUrl)

        if (title.isNotEmpty() || message.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = getBitmapFromUrl(imageUrl)
                sendNotification(title, message, targetScreen, bitmap, channelId)
            }
        }
    }

    private fun sendInAppBroadcast(title: String, message: String, targetScreen: String?, imageUrl: String?) {
        try {
            val intent = Intent(ACTION_NOTIFICATION_RECEIVED).apply {
                putExtra("title", title)
                putExtra("message", message)
                putExtra("target_screen", targetScreen)
                putExtra("image_url", imageUrl)
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending in-app broadcast", e)
        }
    }

    private fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading notification image from $imageUrl", e)
            null
        }
    }

    private fun sendNotification(
        title: String, 
        messageBody: String, 
        targetScreen: String?, 
        bitmap: Bitmap? = null,
        channelId: String = CHANNEL_ANNOUNCEMENTS
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!targetScreen.isNullOrBlank()) {
                putExtra("target_screen", targetScreen)
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val selectedChannel = if (channelId == CHANNEL_DAILY) CHANNEL_DAILY else CHANNEL_ANNOUNCEMENTS
        
        val notificationBuilder = NotificationCompat.Builder(this, selectedChannel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            notificationBuilder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(messageBody)
                )
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val announcementChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "CSI Hymns Announcements & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for hymn announcements, updates, and community news"
                enableVibration(true)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily Hymn & Devotional",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily hymn recommendations and devotionals"
            }

            notificationManager.createNotificationChannel(announcementChannel)
            notificationManager.createNotificationChannel(dailyChannel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
