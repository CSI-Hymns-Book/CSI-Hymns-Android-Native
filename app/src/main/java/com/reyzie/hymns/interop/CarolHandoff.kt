package com.reyzie.hymns.interop

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.reyzie.hymns.data.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The partner app that opened Carols, as shown on the "back" button. */
class CarolCaller(val packageName: String, val label: String, val icon: Drawable?)

/**
 * Where a partner app's request to open Carols is held until the screens pick it up, and who
 * the "back" button returns to. Held in memory only: nothing is saved, and a request is
 * dropped once handled.
 */
object CarolHandoff {
    private val _caller = MutableStateFlow<CarolCaller?>(null)

    /** The partner to return to, or null when Carols was not opened by a partner app. */
    val caller: StateFlow<CarolCaller?> = _caller.asStateFlow()

    private val _openRequest = MutableStateFlow(0)

    /** Goes up by one each time a partner app asks to open Carols. */
    val openRequest: StateFlow<Int> = _openRequest.asStateFlow()

    private val _selectionPending = MutableStateFlow(false)

    /** True until the Carols screen has chosen between the default church and the church list. */
    val selectionPending: StateFlow<Boolean> = _selectionPending.asStateFlow()

    /**
     * Checks an incoming intent and, if it is the Carols link from an allowed partner app,
     * records the request. Must be called straight from `onCreate` / `onNewIntent`, where the
     * system's referrer is for this very intent. Returns true when the request was accepted.
     */
    fun handle(activity: android.app.Activity, intent: Intent): Boolean {
        if (!CarolInterop.isCarolsLink(intent.data?.scheme, intent.data?.host)) return false
        val allowed = allowedPackages(activity)
        val writtenByCaller = intent.hasExtra(Intent.EXTRA_REFERRER) || intent.hasExtra(Intent.EXTRA_REFERRER_NAME)
        val callerPackage = CarolInterop.resolveCaller(
            referrer = activity.referrer?.toString(),
            referrerWrittenByCaller = writtenByCaller,
            allowed = allowed,
            ownPackage = activity.packageName,
        ) ?: return false
        _caller.value = describe(activity, callerPackage)
        _selectionPending.value = true
        _openRequest.value += 1
        return true
    }

    fun selectionHandled() {
        _selectionPending.value = false
    }

    /** Takes the user back to the partner app, if it is still allowed. Clears the button. */
    fun returnToCaller(context: Context) {
        val caller = _caller.value ?: return
        _caller.value = null
        if (!CarolInterop.canReturnTo(caller.packageName, allowedPackages(context), context.packageName)) return
        val back = Intent(CarolInterop.ACTION_RETURN_TO_CALLER)
            .setPackage(caller.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        try {
            context.startActivity(back)
        } catch (_: ActivityNotFoundException) {
            // The partner app was removed; there is nothing to return to.
        }
    }

    private fun allowedPackages(context: Context): Set<String> =
        CarolInterop.parseAllowlist(AppConfigRepository(context = context).getCachedRemoteConfig().interopAllowedAndroid)

    /** Looks up the partner's name and icon; null when it has not opted in to being returned to. */
    private fun describe(context: Context, packageName: String): CarolCaller? {
        val pm = context.packageManager
        val query = Intent(CarolInterop.ACTION_RETURN_TO_CALLER).setPackage(packageName)
        val target = pm.queryIntentActivities(query, 0).firstOrNull() ?: return null
        return CarolCaller(packageName, target.loadLabel(pm).toString(), runCatching { target.loadIcon(pm) }.getOrNull())
    }
}
