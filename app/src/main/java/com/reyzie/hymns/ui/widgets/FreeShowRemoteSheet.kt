package com.reyzie.hymns.ui.widgets

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.presentation.FreeShowConnectionState
import com.reyzie.hymns.presentation.FreeShowFormatter
import com.reyzie.hymns.presentation.FreeShowRemoteManager
import com.reyzie.hymns.presentation.LyricsLanguageMode
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeShowRemoteSheet(
    hymn: Hymn,
    isKeerthane: Boolean,
    languageMode: LyricsLanguageMode,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val connectionState by FreeShowRemoteManager.connectionState.collectAsState()
    val discoveredDevices by FreeShowRemoteManager.discoveredDevices.collectAsState()
    val currentSlideIndex by FreeShowRemoteManager.currentLiveSlideIndex.collectAsState()
    val isCleared by FreeShowRemoteManager.isScreenCleared.collectAsState()
    val connectedHost by FreeShowRemoteManager.connectedHost.collectAsState()

    var manualHost by remember { mutableStateOf(FreeShowRemoteManager.getSavedHost(context)) }
    var manualPort by remember { mutableStateOf(FreeShowRemoteManager.getSavedPort(context).toString()) }
    var manualPassword by remember { mutableStateOf(FreeShowRemoteManager.getSavedPassword(context)) }
    var showManualSetup by remember { mutableStateOf(discoveredDevices.isEmpty() && manualHost.isNotEmpty()) }

    // Prepare presentation slides and beamable show payload
    val slides = remember(hymn, isKeerthane, languageMode) {
        FreeShowFormatter.parseSlides(
            number = hymn.number,
            title = hymn.title,
            isKeerthane = isKeerthane,
            kannadaLyrics = hymn.kannadaLyrics,
            englishLyrics = hymn.lyrics,
            mode = languageMode
        )
    }

    val (showId, showData) = remember(slides) {
        FreeShowFormatter.formatShowDataForBeam(
            context = context,
            number = hymn.number,
            title = hymn.title,
            isKeerthane = isKeerthane,
            slides = slides
        )
    }

    // Start discovery when sheet is open and stop when dismissed
    DisposableEffect(Unit) {
        FreeShowRemoteManager.startDiscovery(context)
        onDispose {
            FreeShowRemoteManager.stopDiscovery()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            when (val state = connectionState) {
                is FreeShowConnectionState.Connected -> {
                    // LIVE PROJECTOR CONTROLLER MODE
                    LiveProjectorController(
                        host = state.host,
                        title = "${if (isKeerthane) "Keerthane" else "Hymn"} ${hymn.number} - ${hymn.title}",
                        showId = showId,
                        showData = showData,
                        slides = slides,
                        currentSlideIndex = currentSlideIndex,
                        isCleared = isCleared,
                        onDisconnect = {
                            HapticFeedbackManager.smoothClick(context)
                            FreeShowRemoteManager.disconnect()
                        }
                    )
                }
                else -> {
                    // DISCOVERY AND CONNECTION MODE
                    DiscoveryAndConnectView(
                        state = state,
                        discoveredDevices = discoveredDevices,
                        manualHost = manualHost,
                        onManualHostChange = { manualHost = it },
                        manualPort = manualPort,
                        onManualPortChange = { manualPort = it },
                        manualPassword = manualPassword,
                        onManualPasswordChange = { manualPassword = it },
                        showManualSetup = showManualSetup,
                        onToggleManualSetup = { showManualSetup = !showManualSetup },
                        onConnectAndBeam = { targetHost, targetPort, targetPw ->
                            HapticFeedbackManager.smoothClick(context)
                            FreeShowRemoteManager.connect(
                                context = context,
                                host = targetHost,
                                port = targetPort,
                                password = targetPw,
                                pendingShow = Pair(showId, showData)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryAndConnectView(
    state: FreeShowConnectionState,
    discoveredDevices: List<com.reyzie.hymns.presentation.FreeShowDevice>,
    manualHost: String,
    onManualHostChange: (String) -> Unit,
    manualPort: String,
    onManualPortChange: (String) -> Unit,
    manualPassword: String,
    onManualPasswordChange: (String) -> Unit,
    showManualSetup: Boolean,
    onToggleManualSetup: () -> Unit,
    onConnectAndBeam: (String, Int, String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Beam to FreeShow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Wireless slide projection on church Wi-Fi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Connection Error banner
        if (state is FreeShowConnectionState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Connecting indicator
        if (state is FreeShowConnectionState.Connecting) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connecting & beaming to ${state.host}...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Discovered Devices Section
        Text(
            text = "DISCOVERED PROJECTORS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (discoveredDevices.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Searching local network for FreeShow...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                discoveredDevices.forEach { device ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onConnectAndBeam(
                                    device.host,
                                    device.port,
                                    manualPassword.ifEmpty { "1542" }
                                )
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${device.host}:${device.port}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    onConnectAndBeam(
                                        device.host,
                                        device.port,
                                        manualPassword.ifEmpty { "1542" }
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Beam Now", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Connection Accordion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleManualSetup() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (showManualSetup) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Manual IP Connection",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = showManualSetup) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = manualHost,
                    onValueChange = onManualHostChange,
                    label = { Text("Projector IP Address") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = manualPort,
                        onValueChange = onManualPortChange,
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.4f)
                    )
                    OutlinedTextField(
                        value = manualPassword,
                        onValueChange = onManualPasswordChange,
                        label = { Text("Password (Default 1542)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                Button(
                    onClick = {
                        val portInt = manualPort.toIntOrNull() ?: 5510
                        onConnectAndBeam(manualHost, portInt, manualPassword)
                    },
                    enabled = manualHost.isNotBlank() && state !is FreeShowConnectionState.Connecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Beam Slide")
                }
            }
        }
    }
}

@Composable
private fun LiveProjectorController(
    host: String,
    title: String,
    showId: String,
    showData: Map<String, Any>,
    slides: List<com.reyzie.hymns.presentation.PresentationSlide>,
    currentSlideIndex: Int?,
    isCleared: Boolean,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Live Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isCleared) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFF1B5E20),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isCleared) MaterialTheme.colorScheme.tertiary else Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCleared) "OUTPUT BLANKED" else "LIVE ON PROJECTOR • $host",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCleared) MaterialTheme.colorScheme.onTertiaryContainer else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Song Title & Re-beam Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    FreeShowRemoteManager.beamShow(showId, showData, autoSelectSlide = currentSlideIndex ?: 0)
                    Toast.makeText(context, "Re-beamed to FreeShow", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Re-beam", style = MaterialTheme.typography.labelSmall)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Slide Grid / List
        Text(
            text = "TAP A SLIDE TO JUMP LIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(slides) { index, slide ->
                val isLive = (currentSlideIndex == index && !isCleared)
                val cardColor by animateColorAsState(
                    targetValue = if (isLive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    label = "cardColor"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            HapticFeedbackManager.smoothClick(context)
                            FreeShowRemoteManager.selectSlide(showId, index)
                        }
                        .then(
                            if (isLive) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stanza Badge
                        Surface(
                            color = if (slide.group.startsWith("Chorus", ignoreCase = true)) {
                                Color(0xFFFFB300)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = slide.group,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (slide.group.startsWith("Chorus", ignoreCase = true)) {
                                    Color.Black
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Preview lyrics
                        Text(
                            text = slide.content.lines().take(2).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isLive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ON SCREEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Bottom tactile controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Prev Slide
                FilledTonalIconButton(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        FreeShowRemoteManager.previousSlide()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous Slide",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Blackout / Clear Screen Button
                OutlinedButton(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        FreeShowRemoteManager.toggleClear()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCleared) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                        contentColor = if (isCleared) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isCleared) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCleared) "Show Screen" else "Blank Screen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Next Slide (Prominent Primary Button)
                Button(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        FreeShowRemoteManager.nextSlide()
                    },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next Slide",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
