package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.reyzie.hymns.data.ContentErrorMessages
import com.reyzie.hymns.data.OrderOfServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.reyzie.hymns.utils.HapticFeedbackManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderOfServiceScreen(
    onNavigateToReader: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    var showEnglishPrimary by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { OrderOfServiceRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Toggle English/Kannada primary text every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            showEnglishPrimary = !showEnglishPrimary
        }
    }

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isLandscape) {
                com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar(
                    title = "Order of Service",
                    onMenuClick = onMenuClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = if (isLandscape) 60.dp else 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    item {
                        com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar(
                            title = "Order of Service",
                            onMenuClick = onMenuClick
                        )
                    }
                }
                
                item {
                    Text(
                        text = "ಆರಾಧನಾ ಕ್ರಮ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                item {
                    OrderCard(
                        leadingIcon = Icons.Default.NorthEast,
                        englishTitle = "Regular Sunday – Order of Service",
                        kannadaTitle = "ಭಾನುವಾರದ ದೇವರಾರಾಧನೆ",
                        showEnglish = showEnglishPrimary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onTap = { 
                            HapticFeedbackManager.smoothClick(context)
                            onNavigateToReader("regular") 
                        }
                    )
                }

                item {
                    OrderCard(
                        leadingIcon = Icons.Default.SouthWest,
                        englishTitle = "Festival – Order of Service",
                        kannadaTitle = "ಹಬ್ಬದ ಆರಾಧನೆ",
                        showEnglish = showEnglishPrimary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onTap = { 
                            HapticFeedbackManager.smoothClick(context)
                            onNavigateToReader("festival") 
                        }
                    )
                }

                item {
                    Button(
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            if (isRefreshing) return@Button
                            scope.launch {
                                isRefreshing = true
                                val result = withContext(Dispatchers.IO) {
                                    repository.fetchAndUpdate("regular")
                                }
                                isRefreshing = false
                                when {
                                    result.errorMessage == null && result.pages.isNotEmpty() ->
                                        snackbarHostState.showSnackbar(ContentErrorMessages.REFRESH_SUCCESS)
                                    result.errorMessage != null ->
                                        snackbarHostState.showSnackbar(result.errorMessage)
                                    else ->
                                        snackbarHostState.showSnackbar(ContentErrorMessages.SERVER_UNAVAILABLE)
                                }
                            }
                        },
                        enabled = !isRefreshing,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isRefreshing) {
                                com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress(size = 48.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh Order of Service", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    leadingIcon: ImageVector,
    englishTitle: String,
    kannadaTitle: String,
    showEnglish: Boolean,
    containerColor: Color,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onTap() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showEnglish) englishTitle else kannadaTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
