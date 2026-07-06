package com.reyzie.hymns.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onesignal.OneSignal
import com.reyzie.hymns.R
import com.reyzie.hymns.data.OnboardingPrefs
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.delay
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
            if (accepted) {
                OneSignal.consentGiven = true
            }
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
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.6f),
                        scheme.surface,
                        scheme.surface,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                userScrollEnabled = false
            ) { page ->
                val isActive = pagerState.currentPage == page
                when (page) {
                    0 -> OnboardingWelcomePage(isActive = isActive)
                    1 -> OnboardingFeaturesPage(isActive = isActive)
                    else -> OnboardingPrivacyPage(
                        isActive = isActive,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy
                    )
                }
            }

            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = ONBOARDING_PAGE_COUNT,
                busy = busy,
                onBack = {
                    if (pagerState.currentPage > 0) {
                        HapticFeedbackManager.smoothClick(context)
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage - 1,
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                },
                onNext = {
                    HapticFeedbackManager.smoothClick(context)
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = tween(420, easing = FastOutSlowInEasing)
                        )
                    }
                },
                onFinish = { finish(accepted = true) },
                onSkipPrivacy = { finish(accepted = false) },
                scheme = scheme
            )
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    busy: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onSkipPrivacy: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val selected = currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 28.dp else 8.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "progressWidth$index"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else if (index < currentPage) 0.7f else 0.28f,
                    animationSpec = tween(300),
                    label = "progressAlpha$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(width)
                        .clip(RoundedCornerShape(50))
                        .alpha(alpha)
                        .background(
                            if (index <= currentPage) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val isLastPage = currentPage == pageCount - 1

        if (isLastPage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSkipPrivacy,
                    enabled = !busy,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Continue without accepting",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    enabled = currentPage > 0 && !busy,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onFinish,
                    enabled = !busy,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = scheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Let's Go", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    enabled = currentPage > 0 && !busy,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }

                Button(
                    onClick = onNext,
                    enabled = !busy,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage(isActive: Boolean) {
    val scheme = MaterialTheme.colorScheme
    var animationStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        if (isActive) {
            animationStep = 0
            delay(80)
            animationStep = 1
            delay(220)
            animationStep = 2
            delay(180)
            animationStep = 3
        } else {
            animationStep = 0
        }
    }

    val iconOffset by animateDpAsState(
        targetValue = if (animationStep >= 1) 0.dp else (-96).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconSlide"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (animationStep >= 1) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "iconAlpha"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (animationStep >= 2) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffset by animateDpAsState(
        targetValue = if (animationStep >= 2) 0.dp else 28.dp,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )
    val bodyAlpha by animateFloatAsState(
        targetValue = if (animationStep >= 3) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "bodyAlpha"
    )
    val bodyOffset by animateDpAsState(
        targetValue = if (animationStep >= 3) 0.dp else 20.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "bodyOffset"
    )

    val headlineColor = scheme.onSurface
    val bodyColor = scheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = iconOffset.toPx()
                    alpha = iconAlpha
                }
        ) {
            Surface(
                modifier = Modifier.size(112.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 16.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.playstore_icon),
                    contentDescription = "CSI Hymns Book",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                translationY = titleOffset.toPx()
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to",
                style = MaterialTheme.typography.titleMedium,
                color = bodyColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "CSI Hymns Book",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = headlineColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Kannada hymns, keerthane, categories, and worship tools — beautifully organized for church and home.",
            style = MaterialTheme.typography.bodyLarge,
            color = bodyColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    alpha = bodyAlpha
                    translationY = bodyOffset.toPx()
                }
        )
    }
}

@Composable
private fun OnboardingFeaturesPage(isActive: Boolean) {
    val scheme = MaterialTheme.colorScheme
    var visibleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        if (isActive) {
            visibleCount = 0
            repeat(5) { i ->
                delay(if (i == 0) 60L else 120L)
                visibleCount = i + 1
            }
        } else {
            visibleCount = 0
        }
    }

    val headlineColor = scheme.onSurface
    val bodyColor = scheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedStaggerItem(visible = visibleCount >= 1) {
            Text(
                "Everything you need",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = headlineColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedStaggerItem(visible = visibleCount >= 2) {
            Text(
                "Browse, search, favorite, and listen with expressive Material motion.",
                style = MaterialTheme.typography.bodyLarge,
                color = bodyColor
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedStaggerItem(visible = visibleCount >= 3) {
            OnboardingFeatureRow(Icons.Default.MenuBook, "Hymns & Keerthane", "Rich lyrics, meter sorting, and page-flip reading.")
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedStaggerItem(visible = visibleCount >= 4) {
            OnboardingFeatureRow(Icons.Default.Category, "Categories", "Occasions, custom collections, and recent songs.")
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedStaggerItem(visible = visibleCount >= 5) {
            OnboardingFeatureRow(Icons.Default.AudioFile, "Audio & favorites", "Built-in playback — save songs you love.")
        }
    }
}

@Composable
private fun AnimatedStaggerItem(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val offset by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "staggerOffset"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offset.toPx()
        }
    ) {
        content()
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBg = scheme.primaryContainer
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnboardingPrivacyPage(
    isActive: Boolean,
    onOpenPrivacyPolicy: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var animationStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        if (isActive) {
            animationStep = 0
            delay(80)
            animationStep = 1
            delay(200)
            animationStep = 2
        } else {
            animationStep = 0
        }
    }

    val headlineColor = scheme.onSurface
    val bodyColor = scheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedStaggerItem(visible = animationStep >= 1) {
            Text(
                "You're almost there",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = headlineColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedStaggerItem(visible = animationStep >= 1) {
            Text(
                "Review privacy once, then jump into the app.",
                style = MaterialTheme.typography.bodyLarge,
                color = bodyColor
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        AnimatedStaggerItem(visible = animationStep >= 2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeBg = scheme.primaryContainer
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = headlineColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "We respect your privacy. Limited analytics may be used to improve the app. " +
                            "We do not sell your personal data.\n\n" +
                            "By tapping Let's Go, you confirm you have reviewed our Privacy Policy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor
                    )
                    TextButton(onClick = onOpenPrivacyPolicy) {
                        Text("Read full Privacy Policy", color = scheme.primary)
                    }
                }
            }
        }
    }
}
