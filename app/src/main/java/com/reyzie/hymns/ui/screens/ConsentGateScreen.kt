package com.reyzie.hymns.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.R
import com.reyzie.hymns.data.ConsentManager
import com.reyzie.hymns.data.HymnsFirebaseMessagingService
import com.reyzie.hymns.data.LegalDocumentKind
import com.reyzie.hymns.data.OnboardingPrefs
import com.reyzie.hymns.utils.HapticFeedbackManager

@Composable
fun rememberPostConsentNotificationPrompt(): () -> Unit {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        ConsentManager.setPushConsent(granted)
        if (granted) {
            HymnsFirebaseMessagingService.subscribeToDefaultTopics(context)
        }
        OnboardingPrefs.markNotificationPromptDone(context)
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ConsentManager.setPushConsent(true)
            HymnsFirebaseMessagingService.subscribeToDefaultTopics(context)
            OnboardingPrefs.markNotificationPromptDone(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentGateScreen() {
    val promptNotifications = rememberPostConsentNotificationPrompt()
    ConsentPolicyPage(
        isPolicyUpdate = true,
        interceptRootBack = true,
        onBack = null,
        onAgree = {
            ConsentManager.acceptCurrentPolicy()
            promptNotifications()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentPolicyPage(
    isPolicyUpdate: Boolean,
    interceptRootBack: Boolean = false,
    onBack: (() -> Unit)?,
    onAgree: () -> Unit
) {
    val language by ConsentManager.language.collectAsState()
    var openDocument by remember { mutableStateOf<LegalDocumentKind?>(null) }
    val context = LocalContext.current

    BackHandler(enabled = openDocument == null && interceptRootBack) { /* cannot skip the agree step */ }
    BackHandler(enabled = openDocument == null && onBack != null) { onBack?.invoke() }

    if (openDocument != null) {
        LegalDocumentScreen(
            kind = openDocument!!,
            onBackClick = { openDocument = null }
        )
        return
    }

    val kannada = language == ConsentManager.LegalLanguage.KANNADA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF163152), Color(0xFF0D1B2A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.playstore_icon),
                contentDescription = "CSI Hymns",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(24.dp))
            Text(
                when {
                    kannada && isPolicyUpdate -> "ನೀತಿ ನವೀಕರಣ"
                    kannada -> "ಮುಂದುವರಿಯಿರಿ"
                    isPolicyUpdate -> "Policy update"
                    else -> "One more step"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (kannada) {
                    "ಒಪ್ಪಿ ಒತ್ತುವ ಮೂಲಕ ನೀವು ನಮ್ಮ ಗೌಪ್ಯತಾ ನೀತಿ ಮತ್ತು ನಿಯಮಗಳನ್ನು ಒಪ್ಪುತ್ತೀರಿ."
                } else {
                    "By tapping Agree, you accept our Privacy Policy and Terms of Use."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (kannada) "ಗೌಪ್ಯತಾ ನೀತಿ" else "Privacy Policy",
                    color = Color(0xFF8CE0B3),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openDocument = LegalDocumentKind.PRIVACY }
                )
                Text(
                    if (kannada) "ನಿಯಮಗಳು" else "Terms of Use",
                    color = Color(0xFF8CE0B3),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openDocument = LegalDocumentKind.TERMS }
                )
            }
            Spacer(Modifier.height(28.dp))
            val options = ConsentManager.LegalLanguage.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 24.dp)) {
                options.forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = language == lang,
                        onClick = { ConsentManager.setLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color.White.copy(alpha = 0.18f),
                            activeContentColor = Color.White,
                            inactiveContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(lang.label)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }
                Spacer(Modifier.width(12.dp))
            }
            Button(
                onClick = {
                    HapticFeedbackManager.mediumClick(context)
                    onAgree()
                },
                enabled = true,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    if (kannada) "ಒಪ್ಪಿ" else "Agree",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ConsentCheckRow(
    checked: Boolean,
    title: String,
    onToggle: () -> Unit,
    dark: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (dark) {
                if (checked) Color.White else Color.White.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (dark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
        )
    }
}
