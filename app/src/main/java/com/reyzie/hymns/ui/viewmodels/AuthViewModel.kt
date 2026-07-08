package com.reyzie.hymns.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.SupabaseService
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val supabaseService = SupabaseService.getInstance()

    val sessionStatus: StateFlow<SessionStatus> = supabaseService.authStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionStatus.NotAuthenticated(isSignOut = false)
        )

    init {
        viewModelScope.launch {
            sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    syncProfileAndPrivacy()
                }
            }
        }
    }

    private fun syncProfileAndPrivacy() {
        viewModelScope.launch {
            // Get name if available from metadata or provider
            val user = supabaseService.currentUser
            val name = user?.userMetadata?.get("full_name")?.toString() 
                ?: user?.userMetadata?.get("name")?.toString()
            
            if (name != null) {
                supabaseService.upsertProfile(name)
            }
            
            // Sync privacy choice from local prefs would go here
            // supabaseService.syncPrivacyPolicyFromLocalPrefs()
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
