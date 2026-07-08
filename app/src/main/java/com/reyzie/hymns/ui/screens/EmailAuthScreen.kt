package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.ui.viewmodels.AuthViewModel
import io.github.jan.supabase.auth.status.SessionStatus

private enum class EmailAuthMode { SignIn, SignUp, Reset }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthComplete: () -> Unit,
    onBackClick: () -> Unit,
) {
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    var mode by remember { mutableStateOf(EmailAuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            isLoading = false
            onAuthComplete()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    val title = when (mode) {
        EmailAuthMode.SignIn -> "Sign in with email"
        EmailAuthMode.SignUp -> "Create account"
        EmailAuthMode.Reset -> "Reset password"
    }

    fun submit() {
        when (mode) {
            EmailAuthMode.SignIn -> viewModel.signInWithEmail(
                email = email,
                password = password,
                onStart = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { err ->
                    isLoading = false
                    message = err
                },
            )
            EmailAuthMode.SignUp -> {
                if (password.length < 6) {
                    message = "Password must be at least 6 characters"
                    return
                }
                if (password != confirmPassword) {
                    message = "Passwords do not match"
                    return
                }
                viewModel.signUpWithEmail(
                    email = email,
                    password = password,
                    onStart = { isLoading = true },
                    onSuccess = {
                        isLoading = false
                        message = "Account created — check your email if confirmation is required"
                        mode = EmailAuthMode.SignIn
                    },
                    onError = { err ->
                        isLoading = false
                        message = err
                    },
                )
            }
            EmailAuthMode.Reset -> viewModel.resetPassword(
                email = email,
                onStart = { isLoading = true },
                onSuccess = {
                    isLoading = false
                    message = "Password reset email sent"
                    mode = EmailAuthMode.SignIn
                },
                onError = { err ->
                    isLoading = false
                    message = err
                },
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (mode == EmailAuthMode.Reset) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (mode == EmailAuthMode.Reset) submit() },
                    ),
                )

                if (mode != EmailAuthMode.Reset) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (mode == EmailAuthMode.SignUp) ImeAction.Next else ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }

                if (mode == EmailAuthMode.SignUp) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = !isLoading && email.isNotBlank() &&
                        (mode == EmailAuthMode.Reset || password.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when (mode) {
                            EmailAuthMode.SignIn -> "Sign In"
                            EmailAuthMode.SignUp -> "Create Account"
                            EmailAuthMode.Reset -> "Send Reset Link"
                        },
                    )
                }

                when (mode) {
                    EmailAuthMode.SignIn -> {
                        TextButton(
                            onClick = { mode = EmailAuthMode.Reset },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Forgot password?")
                        }
                        TextButton(
                            onClick = { mode = EmailAuthMode.SignUp },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Need an account? Sign up")
                        }
                    }
                    EmailAuthMode.SignUp -> {
                        TextButton(
                            onClick = { mode = EmailAuthMode.SignIn },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Already have an account? Sign in")
                        }
                    }
                    EmailAuthMode.Reset -> {
                        TextButton(
                            onClick = { mode = EmailAuthMode.SignIn },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Back to sign in")
                        }
                    }
                }

                Text(
                    "Your favorites sync securely with your account across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
