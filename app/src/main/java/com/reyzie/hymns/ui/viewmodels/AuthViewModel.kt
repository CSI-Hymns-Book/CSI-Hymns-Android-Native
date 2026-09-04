package com.reyzie.hymns.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.data.ConsentManager
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    companion object {
        const val ACCOUNT_DEACTIVATED_MESSAGE =
            "This account has been deactivated. Contact support if you need help."
    }

    private val supabaseService = SupabaseService.getInstance()

    val sessionStatus: StateFlow<SessionStatus> = supabaseService.authStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionStatus.NotAuthenticated(isSignOut = false)
        )

    private val _accountBlockedMessage = MutableStateFlow<String?>(null)
    val accountBlockedMessage: StateFlow<String?> = _accountBlockedMessage.asStateFlow()

    private val _sessionVerified = MutableStateFlow(false)
    val sessionVerified: StateFlow<Boolean> = _sessionVerified.asStateFlow()

    fun consumeAccountBlockedMessage() {
        _accountBlockedMessage.value = null
    }

    init {
        viewModelScope.launch {
            sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    _sessionVerified.value = false
                    syncProfileAndPrivacy()
                } else if (status is SessionStatus.NotAuthenticated) {
                    _sessionVerified.value = false
                    com.reyzie.hymns.data.AnalyticsService.syncAuthIdentity(null)
                }
            }
        }
    }

    private suspend fun syncProfileAndPrivacy() {
        try {
            val user = supabaseService.currentUser ?: return
            if (supabaseService.isAccountDeleted()) {
                supabaseService.signOut()
                _accountBlockedMessage.value = ACCOUNT_DEACTIVATED_MESSAGE
                return
            }
            val name = user.userMetadata?.get("full_name")?.toString()
                ?: user.userMetadata?.get("name")?.toString()

            if (name != null) {
                supabaseService.upsertProfile(name)
            }

            ConsentManager.syncToProfile()
            com.reyzie.hymns.data.AnalyticsService.syncAuthIdentity(user.id)
            if (supabaseService.currentUser != null) {
                _sessionVerified.value = true
            }
        } catch (_: Exception) {
            if (supabaseService.currentUser != null) {
                _sessionVerified.value = true
            }
        }
    }

    val isLoggedIn: StateFlow<Boolean> = sessionStatus
        .map { it is SessionStatus.Authenticated }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Display name for sidebar — profile DB name, then Google metadata, never raw email unless fallback. */
    val accountDisplayName: StateFlow<String> = sessionStatus
        .map { status ->
            if (status !is SessionStatus.Authenticated) return@map "Account"
            val user = status.session.user
            val fromMeta = user?.userMetadata?.get("full_name")?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: user?.userMetadata?.get("name")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            fromMeta ?: "Account"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Account"
        )

    suspend fun resolveAccountDisplayName(): String {
        if (sessionStatus.value !is SessionStatus.Authenticated) return "Account"
        val fromDb = supabaseService.getProfileName()?.trim()?.takeIf { it.isNotEmpty() }
        if (fromDb != null) return fromDb
        val user = supabaseService.currentUser ?: return accountDisplayName.value
        return user.userMetadata?.get("full_name")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: user.userMetadata?.get("name")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: accountDisplayName.value
    }

    fun signInWithGoogle(onStart: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                onStart()
                supabaseService.signInWithGoogle()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to start Google Sign-In")
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onStart: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                onStart()
                supabaseService.signInWithEmail(email.trim(), password)
                if (supabaseService.isAccountDeleted()) {
                    supabaseService.signOut()
                    onError(ACCOUNT_DEACTIVATED_MESSAGE)
                    return@launch
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        onStart: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                onStart()
                supabaseService.signUpWithEmail(email.trim(), password)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun resetPassword(
        email: String,
        onStart: () -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                onStart()
                supabaseService.resetPasswordForEmail(email.trim())
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Could not send reset email")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            supabaseService.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            supabaseService.deleteAccount()
        }
    }
}
