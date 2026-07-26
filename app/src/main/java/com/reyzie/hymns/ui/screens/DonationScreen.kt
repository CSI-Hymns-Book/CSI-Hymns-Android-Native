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
    val serverDrivenPaymentService = remember { com.reyzie.hymns.data.ServerDrivenPaymentService(context) }

    var selectedCurrency by remember { mutableStateOf(CurrencyType.INR) }
    var selectedTierIndex by remember { mutableIntStateOf(0) } // Default to 1st tier (₹50)
    var customAmountText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }
    var isPaymentSuccess by remember { mutableStateOf<Boolean?>(null) }

    var availableGateways by remember { mutableStateOf<List<com.reyzie.hymns.data.PaymentGatewayRow>>(emptyList()) }
    var selectedGateway by remember { mutableStateOf<com.reyzie.hymns.data.PaymentGatewayRow?>(null) }

    val inrTiers = listOf(50L, 100L, 250L, 500L)
    val usdTiers = listOf(2L, 5L, 10L, 25L)
    val currentTiers = if (selectedCurrency == CurrencyType.INR) inrTiers else usdTiers

    val selectedAmountNumber: Long = remember(selectedCurrency, selectedTierIndex, customAmountText) {
        val custom = customAmountText.toLongOrNull()
        if (custom != null && custom > 0) {
            custom
        } else if (selectedTierIndex in currentTiers.indices) {
            currentTiers[selectedTierIndex]
        } else {
            currentTiers[0]
        }
    }

    LaunchedEffect(Unit) {
        val remoteConfig = com.reyzie.hymns.data.AppConfigRepository(context = context).fetchRemoteConfig()
        val list = com.reyzie.hymns.data.SupabaseService.getInstance().getEnabledPaymentGateways()
        val rawList = if (list.isNotEmpty()) {
            list
        } else {
            listOf(
                com.reyzie.hymns.data.PaymentGatewayRow(
                    id = "razorpay",
                    name = "razorpay",
                    displayName = "Razorpay (UPI, GPay, PhonePe, Cards)",
                    description = "UPI, QR Code, Netbanking & International Cards/PayPal",
                    edgeFunctionUrl = "https://vvlyyysdfpsikayymeyv.supabase.co/functions/v1/razorpay-checkout",
                    isEnabled = true,
                    iconType = "upi"
                ),
                com.reyzie.hymns.data.PaymentGatewayRow(
                    id = "adyen",
                    name = "adyen",
                    displayName = "Adyen Global Payments",
                    description = "International Credit & Debit Cards",
                    edgeFunctionUrl = "https://vvlyyysdfpsikayymeyv.supabase.co/functions/v1/adyen-checkout",
                    isEnabled = true,
                    iconType = "card"
                )
            )
        }

        // Filter gateways based on remote app_config toggles
        val filteredList = rawList.filter { gateway ->
            when (gateway.name.lowercase()) {
                "adyen" -> remoteConfig.isAdyenEnabled == true
                "razorpay" -> remoteConfig.isRazorpayEnabled != false
                else -> gateway.isEnabled
            }
        }

        availableGateways = filteredList
        selectedGateway = filteredList.firstOrNull { it.name.lowercase() == "razorpay" } ?: filteredList.firstOrNull()
    }

    // Auto-select best gateway when currency changes
    LaunchedEffect(selectedCurrency, availableGateways) {
        if (availableGateways.isNotEmpty()) {
            if (selectedCurrency == CurrencyType.INR) {
                selectedGateway = availableGateways.firstOrNull { it.name.lowercase() == "razorpay" } ?: availableGateways.first()
            } else {
                selectedGateway = availableGateways.firstOrNull { it.name.lowercase() == "razorpay" }
                    ?: availableGateways.firstOrNull { it.name.lowercase() == "adyen" }
                    ?: availableGateways.first()
            }
        }
    }

    var lastClickTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Support CSI Hymns",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Keep CSI Hymns Free & Ad-Free",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your voluntary contribution helps fund database servers, domain renewal, audio hosting, and continuous app improvements for the community.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }

            // Currency Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Minimal Currency Segmented Switch
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    HapticFeedbackManager.smoothClick(context)
                                    selectedCurrency = CurrencyType.INR
                                    selectedTierIndex = 0
                                    customAmountText = ""
                                },
                            shape = CircleShape,
                            color = if (selectedCurrency == CurrencyType.INR) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = "₹ INR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCurrency == CurrencyType.INR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    HapticFeedbackManager.smoothClick(context)
                                    selectedCurrency = CurrencyType.USD
                                    selectedTierIndex = 0
                                    customAmountText = ""
                                },
                            shape = CircleShape,
                            color = if (selectedCurrency == CurrencyType.USD) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = "$ USD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCurrency == CurrencyType.USD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Preset Amount Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentTiers.forEachIndexed { index, amount ->
                    val isSelected = selectedTierIndex == index && customAmountText.isEmpty()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                HapticFeedbackManager.smoothClick(context)
                                selectedTierIndex = index
                                customAmountText = ""
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
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

            // Custom Amount TextField
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
                placeholder = { Text("Enter amount in ${selectedCurrency.code}") },
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
                shape = RoundedCornerShape(16.dp)
            )

            // Payment Gateway Selector Header (if multiple gateways exist)
            if (availableGateways.size > 1) {
                Text(
                    text = "PAYMENT METHOD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableGateways.forEach { gateway ->
                        val isSelected = selectedGateway?.id == gateway.id || selectedGateway?.name == gateway.name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticFeedbackManager.smoothClick(context)
                                    selectedGateway = gateway
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (gateway.iconType?.lowercase() ?: gateway.name.lowercase()) {
                                                "upi", "razorpay" -> Icons.Default.FlashOn
                                                "card", "adyen" -> Icons.Default.CreditCard
                                                else -> Icons.Default.AccountBalanceWallet
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gateway.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!gateway.description.isNullOrBlank()) {
                                        Text(
                                            text = gateway.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Status Alert Banner
            paymentStatusMessage?.let { msg ->
                val isSuccess = isPaymentSuccess == true
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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

            // Main Donate CTA Button
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 2000L) {
                        return@Button
                    }
                    lastClickTime = now

                    HapticFeedbackManager.smoothClick(context)
                    if (selectedAmountNumber <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val targetGateway = selectedGateway ?: availableGateways.firstOrNull()
                    if (targetGateway == null) {
                        Toast.makeText(context, "No active payment gateway available", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    paymentStatusMessage = null
                    isPaymentSuccess = null

                    scope.launch {
                        val result = serverDrivenPaymentService.startCheckout(
                            gateway = targetGateway,
                            amount = selectedAmountNumber.toDouble(),
                            currency = selectedCurrency.code
                        )

                        isLoading = false
                        result.fold(
                            onSuccess = {
                                android.util.Log.i("DonationScreen", "Checkout launched successfully for ${targetGateway.displayName}")
                            },
                            onFailure = { error ->
                                paymentStatusMessage = "Payment launch error: ${error.localizedMessage}"
                                isPaymentSuccess = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
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
                        text = "Donate ${selectedCurrency.symbol}$selectedAmountNumber via ${selectedGateway?.displayName?.substringBefore(" ") ?: "Server Gateway"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security & Encryption Note
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
                    text = "Encrypted & Processed via Serverless Payment Gateways",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    }
}
