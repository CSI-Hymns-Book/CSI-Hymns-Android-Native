package com.reyzie.hymns.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"

    suspend fun checkForUpdate(
        activity: ComponentActivity,
        onUpToDate: () -> Unit,
        onPlayStoreFallback: () -> Unit,
    ) = withContext(Dispatchers.Main) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            val info = appUpdateManager.requestAppUpdateInfo()
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateType = when {
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        else -> {
                            onPlayStoreFallback()
                            return@withContext
                        }
                    }
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(updateType).build(),
                        UPDATE_REQUEST_CODE,
                    )
                }
                else -> onUpToDate()
            }
        } catch (e: Exception) {
            Log.w(TAG, "In-app update check failed", e)
            onPlayStoreFallback()
        }
    }

    suspend fun checkSilentlyOnLaunch(activity: ComponentActivity) = withContext(Dispatchers.Main) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            val info = appUpdateManager.requestAppUpdateInfo()
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE,
                    )
                } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        UPDATE_REQUEST_CODE,
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent update check skipped: ${e.message}")
        }
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    const val UPDATE_REQUEST_CODE = 9001
}
