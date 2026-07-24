@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adyen.checkout.components.core.CheckoutConfiguration
import com.adyen.checkout.core.Environment
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.SessionDropInCallback
import com.adyen.checkout.dropin.SessionDropInResult
import com.adyen.checkout.sessions.core.CheckoutSessionProvider
import com.adyen.checkout.sessions.core.CheckoutSessionResult
import com.adyen.checkout.sessions.core.SessionModel
import com.reyzie.hymns.data.AdyenPaymentService
import com.reyzie.hymns.data.AppDropInService
import com.reyzie.hymns.ui.widgets.GroupButtonVariant
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch
import java.util.Locale

enum class CurrencyType(val symbol: String, val code: String) {
    INR("₹", "INR"),
    USD("$", "USD")
}

@Composable
fun DonationScreen(
    onBackClick: () -> Unit,
    dropInLauncher: androidx.activity.result.ActivityResultLauncher<*>? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val paymentService = remember { AdyenPaymentService() }

    var selectedCurrency by remember { mutableStateOf(CurrencyType.INR) }
    var selectedTierIndex by remember { mutableIntStateOf(1) } // Default to middle tier
    var customAmountText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }
    var isPaymentSuccess by remember { mutableStateOf<Boolean?>(null) }

    val inrTiers = listOf(20L, 50L, 100L)
    val usdTiers = listOf(1L, 5L, 10L)
    val currentTiers = if (selectedCurrency == CurrencyType.INR) inrTiers else usdTiers

    val selectedAmountNumber: Long = remember(selectedCurrency, selectedTierIndex, customAmountText) {
        val custom = customAmountText.toLongOrNull()
        if (custom != null && custom > 0) {
            custom
        } else if (selectedTierIndex in currentTiers.indices) {
            currentTiers[selectedTierIndex]
        } else {
            currentTiers[1]
        }
    }

    val minorUnitsAmount = selectedAmountNumber * 100

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Support Project",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Keep CSI Hymns Free & Ad-Free",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your voluntary contribution helps pay for database hosting, domain maintenance, audio servers, and ongoing app improvements for the community.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Currency Selector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT AMOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Currency Toggle
                StandardButtonGroup(
                    buttonCount = 2,
                    modifier = Modifier.width(140.dp)
                ) {
                    Button(
                        index = 0,
                        label = "₹ INR",
                        isSelected = selectedCurrency == CurrencyType.INR,
                        onClick = {
                            selectedCurrency = CurrencyType.INR
                            selectedTierIndex = 1
                            customAmountText = ""
                        }
                    )
                    Button(
                        index = 1,
                        label = "$ USD",
                        isSelected = selectedCurrency == CurrencyType.USD,
                        onClick = {
                            selectedCurrency = CurrencyType.USD
                            selectedTierIndex = 1
                            customAmountText = ""
                        },
                        variant = GroupButtonVariant.Tonal
                    )
                }
            }

            // Donation Tiers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                currentTiers.forEachIndexed { index, amount ->
                    val isSelected = selectedTierIndex == index && customAmountText.isEmpty()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                HapticFeedbackManager.smoothClick(context)
                                selectedTierIndex = index
                                customAmountText = ""
                            },
                        shape = MaterialTheme.shapes.large,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${selectedCurrency.symbol}$amount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Custom Amount Card
            val isCustomSelected = customAmountText.isNotEmpty() || selectedTierIndex == -1
            OutlinedTextField(
                value = customAmountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        customAmountText = input
                        if (input.isNotEmpty()) {
                            selectedTierIndex = -1
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                label = { Text("Custom Amount (${selectedCurrency.symbol})") },
                placeholder = { Text("Enter custom amount in ${selectedCurrency.code}") },
                leadingIcon = {
                    Text(
                        selectedCurrency.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )


            // Payment Status / Message Display
            paymentStatusMessage?.let { msg ->
                val isSuccess = isPaymentSuccess == true
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Donate Action Button
            Button(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    if (selectedAmountNumber <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    paymentStatusMessage = null
                    isPaymentSuccess = null

                    scope.launch {
                        val sessionResult = paymentService.createPaymentSession(
                            amount = minorUnitsAmount,
                            currency = selectedCurrency.code
                        )

                        isLoading = false
                        sessionResult.fold(
                            onSuccess = { session ->
                                scope.launch {
                                    try {
                                        if (!session.url.isNullOrBlank()) {
                                            android.util.Log.i("DonationScreen", "Launching Adyen Hosted Payment Page URL: ${session.url}")
                                            try {
                                                val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                                                    .setShowTitle(true)
                                                    .build()
                                                customTabsIntent.launchUrl(context, Uri.parse(session.url))
                                            } catch (e: Exception) {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(session.url))
                                                context.startActivity(browserIntent)
                                            }
                                        } else {
                                            val sessionModel = SessionModel(
                                                id = session.id,
                                                sessionData = session.sessionData
                                            )
                                            val env = if (session.environment.equals("LIVE", ignoreCase = true)) Environment.EUROPE else Environment.TEST
                                            val clientKey = session.clientKey.ifBlank { "test_CLIENTKEY_EXAMPLE" }

                                            val checkoutConfig = CheckoutConfiguration(
                                                environment = env,
                                                clientKey = clientKey
                                            )

                                            android.util.Log.i("DonationScreen", "Creating CheckoutSession with id=${session.id}, clientKey=$clientKey, env=$env")
                                            when (val createResult = CheckoutSessionProvider.createSession(sessionModel, checkoutConfig)) {
                                                is CheckoutSessionResult.Success -> {
                                                    android.util.Log.i("DonationScreen", "CheckoutSession created successfully! Launching DropIn UI...")
                                                    val activity = context as? ComponentActivity
                                                    if (activity != null && dropInLauncher != null) {
                                                        @Suppress("UNCHECKED_CAST")
                                                        val launcher = dropInLauncher as androidx.activity.result.ActivityResultLauncher<com.adyen.checkout.dropin.internal.ui.model.SessionDropInResultContractParams>
                                                        DropIn.startPayment(
                                                            context = activity,
                                                            dropInLauncher = launcher,
                                                            checkoutSession = createResult.checkoutSession,
                                                            checkoutConfiguration = checkoutConfig,
                                                            serviceClass = AppDropInService::class.java
                                                        )
                                                    } else {
                                                        paymentStatusMessage = "Activity launcher not ready"
                                                        isPaymentSuccess = false
                                                    }
                                                }
                                                is CheckoutSessionResult.Error -> {
                                                    android.util.Log.e("DonationScreen", "CheckoutSession creation error", createResult.exception)
                                                    paymentStatusMessage = "Session creation failed: ${createResult.exception.localizedMessage}. Verify Adyen Client Key."
                                                    isPaymentSuccess = false
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        paymentStatusMessage = "Failed to launch Adyen payment: ${e.localizedMessage}"
                                        isPaymentSuccess = false
                                    }
                                }
                            },
                            onFailure = { error ->
                                paymentStatusMessage = "Error connecting to payment service: ${error.localizedMessage}"
                                isPaymentSuccess = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isLoading && selectedAmountNumber > 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Donate ${selectedCurrency.symbol}$selectedAmountNumber via Adyen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Adyen Security Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured & Processed by Adyen Payments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (isPaymentSuccess != null) {
        AlertDialog(
            onDismissRequest = {
                isPaymentSuccess = null
                paymentStatusMessage = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaymentSuccess == true) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaymentSuccess == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isPaymentSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            },
            title = {
                Text(
                    text = if (isPaymentSuccess == true) "Donation Received!" else "Payment Incomplete",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPaymentSuccess == true) {
                        Text(
                            text = "Amount: ${selectedCurrency.symbol}${selectedAmountNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = paymentStatusMessage ?: "Thank you so much for your generous support! Your contribution keeps our servers running and supports continuous development.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "May God bless you richly! 🙏",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = paymentStatusMessage ?: "The payment process was cancelled or not completed.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wasSuccess = isPaymentSuccess == true
                        isPaymentSuccess = null
                        paymentStatusMessage = null
                        if (wasSuccess) {
                            onBackClick()
                        }
                    },
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (isPaymentSuccess == true) "Done" else "Try Again")
                }
            },
            dismissButton = {
                if (isPaymentSuccess == false) {
                    TextButton(
                        onClick = {
                            isPaymentSuccess = null
                            paymentStatusMessage = null
                        }
                    ) {
                        Text("Close")
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
