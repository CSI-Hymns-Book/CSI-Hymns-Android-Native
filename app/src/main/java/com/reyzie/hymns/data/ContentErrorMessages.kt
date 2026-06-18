package com.reyzie.hymns.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ContentErrorMessages {
    const val OFFLINE =
        "You're offline. Showing saved content. Check your internet connection and try again."
    const val NETWORK_TIMEOUT =
        "The connection timed out. Check your internet and try again."
    const val SERVER_UNAVAILABLE =
        "Couldn't reach the update server. Your saved content is still available offline."
    const val NO_LOCAL_DATA =
        "Content isn't available yet. Connect to the internet once to download worship materials."
    const val AUDIO_OFFLINE =
        "Audio needs an internet connection. Lyrics and order of service work offline."
    const val REFRESH_SUCCESS = "Content updated successfully."
    const val REFRESH_PARTIAL = "Some content couldn't be updated. Showing your saved copy."

    fun forThrowable(throwable: Throwable?, hasLocalData: Boolean = true): String {
        if (throwable == null) {
            return if (hasLocalData) SERVER_UNAVAILABLE else NO_LOCAL_DATA
        }
        val message = throwable.message?.lowercase().orEmpty()
        return when {
            throwable is UnknownHostException ||
                message.contains("unable to resolve host") ||
                message.contains("no address associated") ||
                message.contains("network is unreachable") ->
                if (hasLocalData) OFFLINE else NO_LOCAL_DATA
            throwable is SocketTimeoutException ||
                message.contains("timeout") ||
                message.contains("timed out") ->
                if (hasLocalData) NETWORK_TIMEOUT else NO_LOCAL_DATA
            throwable is IOException ->
                if (hasLocalData) OFFLINE else NO_LOCAL_DATA
            else ->
                if (hasLocalData) SERVER_UNAVAILABLE else NO_LOCAL_DATA
        }
    }
}
