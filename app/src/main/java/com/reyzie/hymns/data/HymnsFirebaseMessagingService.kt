package com.reyzie.hymns.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

class HymnsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HymnsFCM"
        const val CHANNEL_ID = "csi_hymns_announcements"
        const val CHANNEL_NAME = "CSI Hymns Announcements & Updates"
        const val PREF_FCM_TOKEN = "fcm_token"

        fun subscribeToDefaultTopics() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: all_users")
                        }
                    }
                FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: announcements")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to FCM topics", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token generated: $token")
        
        // 1. Save locally in SharedPreferences
        getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_FCM_TOKEN, token)
            .apply()

        // 2. Auto-subscribe to default topics
        subscribeToDefaultTopics()

        // 3. Sync to Supabase user profile if logged in
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseService.getInstance().updateProfileFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync FCM token to Supabase profile", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "CSI Hymns Book"
            
        val message = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["message"] 
            ?: ""

        val targetScreen = remoteMessage.data["target_screen"] 
            ?: remoteMessage.data["deep_link"]

        if (title.isNotEmpty() || message.isNotEmpty()) {
            sendNotification(title, message, targetScreen)
        }
    }

    private fun sendNotification(title: String, messageBody: String, targetScreen: String?) {
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
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for hymn announcements, updates, and community news"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
