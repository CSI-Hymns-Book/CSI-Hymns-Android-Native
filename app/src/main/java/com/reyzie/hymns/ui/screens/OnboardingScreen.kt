package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.R
import com.reyzie.hymns.data.OnboardingPrefs
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })

    fun finish(accepted: Boolean) {
        if (busy) return
        busy = true
        HapticFeedbackManager.mediumClick(context)
        scope.launch {
            val value = if (accepted) 1 else 0
            OnboardingPrefs.markWelcomeCompleted(context, value, pendingChangelog = true)
            SupabaseService.getInstance().setPrivacyPolicyAcceptedInProfile(value)
            busy = false
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.45f),
                        scheme.surface,
                        scheme.secondaryContainer.copy(alpha = 0.35f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1f) / ONBOARDING_PAGE_COUNT },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp)),
                trackColor = scheme.surfaceContainerHighest,
                color = scheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(ONBOARDING_PAGE_COUNT) { index ->
                    val selected = pagerState.currentPage == index
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp),
                        shape = CircleShape,
                        color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingFeaturesPage()
                    else -> OnboardingPrivacyPage(onOpenPrivacyPolicy = onOpenPrivacyPolicy)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            HapticFeedbackManager.smoothClick(context)
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    enabled = pagerState.currentPage > 0 && !busy,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }

                if (pagerState.currentPage < ONBOARDING_PAGE_COUNT - 1) {
                    Button(
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        enabled = !busy,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = { finish(accepted = true) },
                        enabled = !busy,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        )
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = scheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Let's Go!", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            if (pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { finish(accepted = false) },
                    enabled = !busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Continue without accepting")
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(28.dp),
            color = scheme.primary,
            shadowElevation = 8.dp
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = Modifier.padding(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            "Welcome to",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurfaceVariant
        )
        Text(
            "CSI Hymns Book",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Kannada hymns, keerthane, categories, and worship tools — beautifully organized for church and home.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun OnboardingFeaturesPage() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Everything you need",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Browse, search, favorite, and listen with expressive Material motion.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingFeatureRow(Icons.Default.MenuBook, "Hymns & Keerthane", "Number or meter sorting, rich lyrics, page flip reading.")
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingFeatureRow(Icons.Default.Category, "Categories", "Occasions, custom collections, and recent songs.")
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingFeatureRow(Icons.Default.AudioFile, "Audio & favorites", "Built-in playback, save songs you love.")
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingFeatureRow(Icons.Default.Favorite, "Made for worship", "Clean, focused UI designed for services and practice.")
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OnboardingPrivacyPage(onOpenPrivacyPolicy: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "You're almost there",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Review privacy once, then jump into the app.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHighest
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = scheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "We respect your privacy. Limited analytics may be used to improve the app. " +
                        "We do not sell your personal data.\n\n" +
                        "By tapping Let's Go, you confirm you have reviewed our Privacy Policy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text("Read full Privacy Policy")
                }
            }
        }
    }
}
