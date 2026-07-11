package com.reyzie.hymns.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.reyzie.hymns.utils.HapticFeedbackManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.reyzie.hymns.carols.ui.list.CommunityCarolsScreen
import com.reyzie.hymns.data.*
import com.reyzie.hymns.ui.navigation.Screen
import com.reyzie.hymns.ui.navigation.christmasMainScreens
import com.reyzie.hymns.ui.navigation.christmasNavScreens
import com.reyzie.hymns.ui.navigation.mainNavScreens
import com.reyzie.hymns.ui.navigation.mainScreens
import com.reyzie.hymns.ui.viewmodels.AudioViewModel
import com.reyzie.hymns.ui.viewmodels.FavoritesViewModel
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.ui.widgets.HymnsFloatingToolbar
import com.reyzie.hymns.ui.widgets.MenuShowcaseOverlay
import com.reyzie.hymns.ui.motion.ExpressiveOverlayScreen
import com.reyzie.hymns.ui.motion.PredictiveExpressiveBackHandler
import com.reyzie.hymns.ui.motion.expressiveBackEnter
import com.reyzie.hymns.ui.motion.expressiveBackExit
import com.reyzie.hymns.ui.motion.expressiveForwardEnter
import com.reyzie.hymns.ui.motion.expressiveForwardExit
import com.reyzie.hymns.ui.motion.expressivePagerPage
import com.reyzie.hymns.data.AppSection
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import com.reyzie.hymns.ui.motion.expressivePredictiveBackTransform

private val HymnSaver = Saver<Hymn?, String>(
    save = { hymn ->
        if (hymn == null) "" else {
            val json = org.json.JSONObject()
            json.put("number", hymn.number)
            json.put("title", hymn.title)
            json.put("signature", hymn.signature)
            json.put("lyrics", hymn.lyrics)
            json.put("kannadaLyrics", hymn.kannadaLyrics ?: "")
            json.toString()
        }
    },
    restore = { value ->
        if (value.isEmpty()) null else {
            val json = org.json.JSONObject(value)
            Hymn(
                number = json.getInt("number"),
                title = json.getString("title"),
                signature = json.getString("signature"),
                lyrics = json.getString("lyrics"),
                kannadaLyrics = json.optString("kannadaLyrics").takeIf { it.isNotEmpty() }
            )
        }
    }
)

private val KeerthaneSaver = Saver<Keerthane?, String>(
    save = { keerthane ->
        if (keerthane == null) "" else {
            val json = org.json.JSONObject()
            json.put("number", keerthane.number)
            json.put("title", keerthane.title)
            json.put("signature", keerthane.signature)
            json.put("lyrics", keerthane.lyrics)
            json.put("kannadaLyrics", keerthane.kannadaLyrics ?: "")
            json.toString()
        }
    },
    restore = { value ->
        if (value.isEmpty()) null else {
            val json = org.json.JSONObject(value)
            Keerthane(
                number = json.getInt("number"),
                title = json.getString("title"),
                signature = json.getString("signature"),
                lyrics = json.getString("lyrics"),
                kannadaLyrics = json.optString("kannadaLyrics").takeIf { it.isNotEmpty() }
            )
        }
    }
)

private val PairSaver = Saver<Pair<Int, String>?, String>(
    save = { pair ->
        if (pair == null) "" else "${pair.first}|${pair.second}"
    },
    restore = { value ->
        if (value.isEmpty()) null else {
            val parts = value.split("|", limit = 2)
            Pair(parts[0].toInt(), parts[1])
        }
    }
)

private val ChangelogEntrySaver = Saver<com.reyzie.hymns.data.ChangelogEntryData?, String>(
    save = { entry ->
        if (entry == null) "" else {
            val json = org.json.JSONObject()
            json.put("title", entry.title)
            json.put("version", entry.version)
            json.put("date", entry.date)
            val changesArray = org.json.JSONArray(entry.changes)
            json.put("changes", changesArray)
            json.toString()
        }
    },
    restore = { value ->
        if (value.isEmpty()) null else {
            val json = org.json.JSONObject(value)
            val changesArray = json.getJSONArray("changes")
            val changesList = mutableListOf<String>()
            for (i in 0 until changesArray.length()) {
                changesList.add(changesArray.getString(i))
            }
            com.reyzie.hymns.data.ChangelogEntryData(
                title = json.getString("title"),
                version = json.getString("version"),
                date = json.getString("date"),
                changes = changesList
            )
        }
    }
)

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    audioViewModel: AudioViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val authViewModel: com.reyzie.hymns.ui.viewmodels.AuthViewModel = viewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val isLoggedIn = sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Authenticated
    
    val remoteConfig by settingsViewModel.remoteAppConfig.collectAsState()
    val isMangaloreHymnsEnabled = remoteConfig.isMangaloreHymnsEnabled == true
    var activeSection by rememberSaveable { mutableStateOf<AppSection?>(null) }
    val effectiveSection = if (isMangaloreHymnsEnabled) (activeSection ?: AppSection.CSI) else AppSection.CSI

    val adminEmailsString = remoteConfig.adminEmails ?: "reynoldclare02@gmail.com,admin@reyzie.com"
    val adminEmails = remember(adminEmailsString) {
        adminEmailsString.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .replace("'", "")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
    val currentUserEmail = (sessionStatus as? io.github.jan.supabase.auth.status.SessionStatus.Authenticated)
        ?.session?.user?.email
    val isAdmin = currentUserEmail != null && currentUserEmail.lowercase().trim() in adminEmails
    val audioState by audioViewModel.audioState.collectAsState()
    val isChristmasMode by settingsViewModel.isChristmasMode.collectAsState()

    val activeScreens = when (effectiveSection) {
        AppSection.MT -> listOf(Screen.Hymns, Screen.Categories, Screen.Favorites)
        else -> if (isChristmasMode) christmasMainScreens else mainScreens
    }
    val navScreens = when (effectiveSection) {
        AppSection.MT -> listOf(Screen.Hymns, Screen.Categories)
        else -> if (isChristmasMode) christmasNavScreens else mainNavScreens
    }
    val favoritesPageIndex = activeScreens.indexOf(Screen.Favorites)
    
    var selectedHymn by rememberSaveable(stateSaver = HymnSaver) { mutableStateOf<Hymn?>(null) }
    var selectedKeerthane by rememberSaveable(stateSaver = KeerthaneSaver) { mutableStateOf<Keerthane?>(null) }
    var selectedReaderType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable(stateSaver = PairSaver) { mutableStateOf<Pair<Int, String>?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pickSongsForCategory by rememberSaveable(stateSaver = PairSaver) { mutableStateOf<Pair<Int, String>?>(null) }
    var categoryRefreshTrigger by rememberSaveable { mutableStateOf(0) }
    
    val context = LocalContext.current
    val hymnsRepo = remember { com.reyzie.hymns.data.HymnsRepository(context) }
    val keerthaneRepo = hymnsRepo
    
    var showPraiseApp by rememberSaveable { mutableStateOf(false) }
    var showTickets by rememberSaveable { mutableStateOf(false) }
    var showAboutDeveloper by rememberSaveable { mutableStateOf(false) }
    var showRecentSongs by rememberSaveable { mutableStateOf(false) }
    var showChristmasCarols by rememberSaveable { mutableStateOf(false) }
    var showHymnsFromChristmas by rememberSaveable { mutableStateOf(false) }
    var showMtHymnsFromChristmas by rememberSaveable { mutableStateOf(false) }
    var showKeerthanesFromChristmas by rememberSaveable { mutableStateOf(false) }
    var selectedCommonCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showPrivacyPolicy by rememberSaveable { mutableStateOf(false) }
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    var showAboutApp by rememberSaveable { mutableStateOf(false) }
    var showProfileEdit by rememberSaveable { mutableStateOf(false) }
    var showMenuShowcase by rememberSaveable { mutableStateOf(false) }
    var homeSettled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        OnboardingPrefs.migrateFromLegacy(context)
    }
    var showOnboarding by rememberSaveable {
        mutableStateOf(!OnboardingPrefs.isWelcomeCompleted(context))
    }
    var welcomeChangelogEntry by rememberSaveable(stateSaver = ChangelogEntrySaver) { mutableStateOf<com.reyzie.hymns.data.ChangelogEntryData?>(null) }
    var resolvedTicketAcks by remember { mutableStateOf<List<com.reyzie.hymns.data.ResolvedTicketAckItem>?>(null) }
    var initialChatTicketKey by remember { mutableStateOf<String?>(null) }
    var newReplyNotificationMsg by remember { mutableStateOf<com.reyzie.hymns.data.TicketMessage?>(null) }
    var showAdminControls by rememberSaveable { mutableStateOf(false) }
    var activeBroadcastMsg by remember { mutableStateOf<com.reyzie.hymns.data.InAppMessage?>(null) }
    val broadcastService = remember { com.reyzie.hymns.data.BroadcastMessageService(context) }
    val changelogService = remember { ChangelogService(context) }
    val ticketAckService = remember { TicketAcknowledgementService(context) }

    var forceUpdateDecision by remember { mutableStateOf<ForceUpdateDecision?>(null) }

    if (showOnboarding) {
        OnboardingScreen(
            onDone = { showOnboarding = false },
            onOpenPrivacyPolicy = { showPrivacyPolicy = true }
        )
        if (showPrivacyPolicy) {
            PredictiveExpressiveBackHandler(
                enabled = true,
                onBack = { showPrivacyPolicy = false }
            )
            PrivacyPolicyScreen(onBackClick = { showPrivacyPolicy = false })
        }
        return
    }

    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) {
            kotlinx.coroutines.delay(1400)
            homeSettled = true
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.refreshAppConfig()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.reyzie.hymns.data.ContentSyncManager(context).initialize()
        }
        val service = ForceUpdateService(context)
        val decision = service.getDecision()
        if (decision.requiresUpdate) {
            forceUpdateDecision = decision
        }
    }

    LaunchedEffect(homeSettled) {
        if (!homeSettled) return@LaunchedEffect
        if (OnboardingPrefs.isPendingChangelogAfterOnboarding(context)) {
            changelogService.getLatestChangelog()?.let {
                welcomeChangelogEntry = it
            }
            OnboardingPrefs.clearPendingChangelogAfterOnboarding(context)
            changelogService.markFirstLaunchHandled()
        } else if (changelogService.shouldShowChangelog()) {
            changelogService.getLatestChangelog()?.let {
                welcomeChangelogEntry = it
            }
        }

        try {
            val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            val lastChecked = prefs.getString("last_replies_check_time", null)
            val nowIso = java.time.Instant.now().toString()
            if (lastChecked == null) {
                prefs.edit().putString("last_replies_check_time", nowIso).apply()
            } else {
                val ticketsRepo = com.reyzie.hymns.data.TicketsRepository(context)
                val newReplies = ticketsRepo.checkForNewReplies(lastChecked)
                if (newReplies.isNotEmpty()) {
                    newReplyNotificationMsg = newReplies.first()
                }
                prefs.edit().putString("last_replies_check_time", nowIso).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val broadcast = broadcastService.getActiveUndismissedBroadcast()
            if (broadcast != null) {
                activeBroadcastMsg = broadcast
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    welcomeChangelogEntry?.let { entry ->
        WelcomeChangelogDialog(
            entry = entry,
            onDismiss = {
                changelogService.markChangelogAsShown()
                welcomeChangelogEntry = null
            }
        )
    }

    LaunchedEffect(homeSettled, welcomeChangelogEntry) {
        if (!homeSettled || welcomeChangelogEntry != null) return@LaunchedEffect
        val acks = ticketAckService.getUnacknowledgedResolvedTickets()
        if (acks.isNotEmpty()) {
            resolvedTicketAcks = acks
        }
    }

    resolvedTicketAcks?.let { items ->
        ResolvedTicketsAckDialog(
            items = items,
            onAcknowledge = {
                ticketAckService.markAcknowledged(items.map { it.ticketKey })
                resolvedTicketAcks = null
            }
        )
    }

    newReplyNotificationMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { newReplyNotificationMsg = null },
            title = { Text("New Support Reply") },
            text = { Text("You received a new reply on your ticket (${msg.ticketKey}):\n\n\"${msg.message}\"") },
            confirmButton = {
                Button(onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    initialChatTicketKey = msg.ticketKey
                    showTickets = true
                    newReplyNotificationMsg = null
                }) {
                    Text("Open Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { newReplyNotificationMsg = null }) {
                    Text("Later")
                }
            }
        )
    }

    activeBroadcastMsg?.let { msg ->
        BroadcastMessageDialog(
            message = msg,
            onDismiss = {
                broadcastService.dismissBroadcast(msg.id)
                activeBroadcastMsg = null
            }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { activeScreens.size })

    val isStackedOverlayOpen = showProfileEdit || showPrivacyPolicy || showChangelog || showAboutApp ||
        showPraiseApp || showTickets || showAboutDeveloper || showRecentSongs ||
        showChristmasCarols || showHymnsFromChristmas || showMtHymnsFromChristmas || showKeerthanesFromChristmas ||
        selectedCommonCategory != null || pickSongsForCategory != null || selectedCategory != null ||
        showAdminControls

    if (forceUpdateDecision != null) {
        AlertDialog(
            onDismissRequest = { /* Blocking */ },
            title = { Text("Update Required") },
            text = { Text(forceUpdateDecision?.message ?: "A new version of the app is available. Please update to continue using the app.") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(forceUpdateDecision?.androidStoreUrl ?: "https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }) {
                    Text("Update Now")
                }
            },
            dismissButton = null
        )
    }

    // Sync Pager with NavController for deep links or initial state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        val index = activeScreens.indexOfFirst { it.route == currentRoute }
        if (index != -1 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(isChristmasMode, currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        val routeExists = activeScreens.any { it.route == route } || route == Screen.Auth.route
        if (!routeExists) {
            navController.navigate(Screen.Hymns.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedHymn == null &&
            selectedKeerthane == null &&
            selectedReaderType == null &&
            !showSettings &&
            !isStackedOverlayOpen,
        drawerContent = {
            Sidebar(
                navController = navController,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onSettingsClick = { showSettings = true },
                onPraiseAppClick = { showPraiseApp = true },
                onTicketsClick = { showTickets = true },
                onAboutDeveloperClick = { showAboutDeveloper = true },
                onProfileEditClick = { showProfileEdit = true },
                onAdminControlsClick = { showAdminControls = true },
                isAdmin = isAdmin,
                isMangaloreHymnsEnabled = isMangaloreHymnsEnabled,
                onChangeSectionClick = { activeSection = null }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var activeSectionSwipeProgress by remember { mutableFloatStateOf(0f) }
            var activeSectionSwipeActive by remember { mutableStateOf(false) }

            val animatedActiveSectionSwipe by animateFloatAsState(
                targetValue = activeSectionSwipeProgress,
                animationSpec = if (activeSectionSwipeActive) snap() else spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "activeSectionPredictiveBack"
            )

            if (isMangaloreHymnsEnabled && activeSection != null) {
                PredictiveExpressiveBackHandler(
                    enabled = !isStackedOverlayOpen && selectedHymn == null && selectedKeerthane == null && selectedReaderType == null && !showSettings,
                    onBack = {
                        activeSection = null
                    },
                    onProgress = { progress ->
                        activeSectionSwipeActive = progress > 0f
                        activeSectionSwipeProgress = progress
                    }
                )
            }

            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    if (targetState != null) {
                        expressiveForwardEnter() togetherWith expressiveForwardExit()
                    } else {
                        expressiveBackEnter() togetherWith expressiveBackExit()
                    }
                },
                label = "sectionTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) { sectionState ->
                if (isChristmasMode && sectionState == null) {
                    ChristmasLandingScreen(
                        onOpenHymns = { showHymnsFromChristmas = true },
                        onOpenKeerthanes = { showKeerthanesFromChristmas = true },
                        onOpenCarols = { showChristmasCarols = true },
                        onOpenMtHymns = { showMtHymnsFromChristmas = true },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                } else if (isMangaloreHymnsEnabled && sectionState == null) {
                    SectionSelectorScreen(
                        onSelectCsiPage = { pageIndex ->
                            activeSection = AppSection.CSI
                            scope.launch {
                                pagerState.scrollToPage(pageIndex)
                            }
                        },
                        onSelectMtPage = {
                            activeSection = AppSection.MT
                            scope.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )
                } else {
                    val localSection = sectionState ?: AppSection.CSI
                    val localActiveScreens = when (localSection) {
                        AppSection.MT -> listOf(Screen.Hymns, Screen.Categories, Screen.Favorites)
                        else -> if (isChristmasMode) christmasMainScreens else mainScreens
                    }
                    val localNavScreens = when (localSection) {
                        AppSection.MT -> listOf(Screen.Hymns, Screen.Categories)
                        else -> if (isChristmasMode) christmasNavScreens else mainNavScreens
                    }
                    val localFavoritesPageIndex = localActiveScreens.indexOf(Screen.Favorites)

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier
                            .fillMaxSize()
                            .expressivePredictiveBackTransform(animatedActiveSectionSwipe)
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Hymns.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                localActiveScreens.forEach { screen ->
                                    composable(screen.route) {
                                        HorizontalPager(
                                            state = pagerState,
                                            beyondViewportPageCount = 1,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .expressivePagerPage(
                                                        page = page,
                                                        currentPage = pagerState.currentPage,
                                                        pageOffsetFraction = pagerState.currentPageOffsetFraction
                                                    )
                                            ) {
                                                when (localActiveScreens.getOrNull(page)) {
                                                    Screen.Hymns -> {
                                                        HymnsScreen(
                                                            onHymnClick = { hymn -> selectedHymn = hymn },
                                                            onSettingsClick = { scope.launch { drawerState.open() } },
                                                            activeSection = localSection
                                                        )
                                                    }
                                                    Screen.Keerthane -> KeerthaneScreen(
                                                        onKeerthaneClick = { keerthane -> selectedKeerthane = keerthane },
                                                        onMenuClick = { scope.launch { drawerState.open() } }
                                                    )
                                                    Screen.Service -> OrderOfServiceScreen(
                                                        onNavigateToReader = { type -> selectedReaderType = type },
                                                        onMenuClick = { scope.launch { drawerState.open() } }
                                                    )
                                                    Screen.Categories -> CategoriesScreen(
                                                        onSignInClick = { navController.navigate(Screen.Auth.route) },
                                                        onCategoryClick = { id, name -> selectedCategory = Pair(id, name) },
                                                        onRecentSongsClick = { showRecentSongs = true },
                                                        onCommonCategoryClick = { category -> selectedCommonCategory = category },
                                                        onMenuClick = { scope.launch { drawerState.open() } }
                                                    )
                                                    Screen.Favorites -> FavoritesScreen(
                                                        viewModel = favoritesViewModel,
                                                        onHymnClick = { hymn -> selectedHymn = hymn },
                                                        onKeerthaneClick = { keerthane -> selectedKeerthane = keerthane },
                                                        onMenuClick = { scope.launch { drawerState.open() } },
                                                        activeSection = localSection
                                                    )
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                }
                                composable(
                                    route = Screen.Auth.route,
                                    enterTransition = { expressiveForwardEnter() },
                                    exitTransition = { expressiveForwardExit() },
                                    popEnterTransition = { expressiveBackEnter() },
                                    popExitTransition = { expressiveBackExit() }
                                ) {
                                    AuthScreen(
                                        onAuthComplete = { navController.navigateUp() },
                                        onBackClick = { navController.navigateUp() }
                                    )
                                }
                            }

                            // --- Essentials-style vibrant floating toolbar ---
                            if (currentRoute != Screen.Auth.route) {
                                val navScrim = MaterialTheme.colorScheme.background
                                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isLandscape) 76.dp else 120.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        navScrim.copy(alpha = 0.4f),
                                                        navScrim.copy(alpha = 0.85f)
                                                    )
                                                )
                                            )
                                    )
                                    val isFavoritesSelected = pagerState.currentPage == localFavoritesPageIndex
                                    val navSelectedIndex = if (isFavoritesSelected) {
                                        -1
                                    } else {
                                        localNavScreens.indexOf(localActiveScreens.getOrNull(pagerState.currentPage))
                                    }
                                    HymnsFloatingToolbar(
                                        screens = localNavScreens,
                                        selectedIndex = navSelectedIndex,
                                        isChristmasMode = isChristmasMode,
                                        favoritesScreen = Screen.Favorites,
                                        isFavoritesSelected = isFavoritesSelected,
                                        onFavoritesSelected = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(localFavoritesPageIndex)
                                            }
                                            navController.navigate(Screen.Favorites.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onTabSelected = { navIndex ->
                                            val screen = localNavScreens[navIndex]
                                            val pageIndex = localActiveScreens.indexOf(screen)
                                            scope.launch {
                                                pagerState.animateScrollToPage(pageIndex)
                                            }
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }



            ExpressiveOverlayScreen(
                item = selectedReaderType,
                onDismiss = { selectedReaderType = null }
            ) { readerType ->
                OrderOfServiceReaderScreen(
                    type = readerType,
                    onBackClick = { selectedReaderType = null }
                )
            }

            ExpressiveOverlayScreen(
                item = showSettings.takeIf { it },
                onDismiss = { showSettings = false }
            ) {
                SettingsScreen(
                    onNavigateUp = { showSettings = false },
                    onPrivacyPolicyClick = { showPrivacyPolicy = true },
                    onChangelogClick = { showChangelog = true },
                    onAboutAppClick = { showAboutApp = true },
                    onSignInClick = {
                        showSettings = false
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            ExpressiveOverlayScreen(
                visible = showProfileEdit,
                onDismiss = { showProfileEdit = false }
            ) {
                ProfileEditScreen(
                    onBackClick = { showProfileEdit = false },
                    onAccountDeleted = {
                        showProfileEdit = false
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            ExpressiveOverlayScreen(
                visible = showPrivacyPolicy,
                onDismiss = { showPrivacyPolicy = false }
            ) {
                PrivacyPolicyScreen(onBackClick = { showPrivacyPolicy = false })
            }

            ExpressiveOverlayScreen(
                visible = showChangelog,
                onDismiss = { showChangelog = false }
            ) {
                ChangelogScreen(onBackClick = { showChangelog = false })
            }

            ExpressiveOverlayScreen(
                visible = showAboutApp,
                onDismiss = { showAboutApp = false }
            ) {
                AboutAppScreen(
                    onBackClick = { showAboutApp = false },
                    onPrivacyPolicyClick = { showPrivacyPolicy = true }
                )
            }

            ExpressiveOverlayScreen(
                visible = showPraiseApp,
                onDismiss = { showPraiseApp = false }
            ) {
                PraiseAppScreen(onBackClick = { showPraiseApp = false })
            }

            ExpressiveOverlayScreen(
                visible = showTickets,
                onDismiss = { showTickets = false; initialChatTicketKey = null }
            ) {
                TicketsScreen(
                    initialTicketKey = initialChatTicketKey,
                    onBackClick = { showTickets = false; initialChatTicketKey = null }
                )
            }

            ExpressiveOverlayScreen(
                visible = showAboutDeveloper,
                onDismiss = { showAboutDeveloper = false }
            ) {
                AboutDeveloperScreen(onBackClick = { showAboutDeveloper = false })
            }

            if (isAdmin) {
                ExpressiveOverlayScreen(
                    visible = showAdminControls,
                    onDismiss = { showAdminControls = false }
                ) {
                    AdminControlsScreen(onBackClick = { showAdminControls = false })
                }
            }

            ExpressiveOverlayScreen(
                visible = showRecentSongs,
                onDismiss = { showRecentSongs = false }
            ) {
                RecentSongsScreen(
                    onSongClick = { type, id ->
                        val num = id.toIntOrNull() ?: return@RecentSongsScreen
                        showRecentSongs = false
                        if (type == "hymn") {
                            scope.launch { selectedHymn = hymnsRepo.loadHymns().find { it.number == num } }
                        } else if (type == "keerthane") {
                            scope.launch { selectedKeerthane = keerthaneRepo.loadKeerthanes().find { it.number == num } }
                        }
                    },
                    onBackClick = { showRecentSongs = false }
                )
            }

            ExpressiveOverlayScreen(
                visible = showChristmasCarols,
                onDismiss = { showChristmasCarols = false }
            ) {
                CommunityCarolsScreen(onBackClick = { showChristmasCarols = false })
            }

            ExpressiveOverlayScreen(
                visible = showHymnsFromChristmas,
                onDismiss = { showHymnsFromChristmas = false }
            ) {
                Box(Modifier.fillMaxSize()) {
                    HymnsScreen(
                        onHymnClick = { hymn -> selectedHymn = hymn },
                        onSettingsClick = { showHymnsFromChristmas = false },
                        activeSection = AppSection.CSI,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
                    )
                    ExpressiveOverlayScreen(
                        item = selectedHymn,
                        onDismiss = { 
                            selectedHymn = null
                            audioViewModel.stopAndReset()
                        }
                    ) { hymn ->
                        HymnDetailScreen(
                            hymn = hymn,
                            isKeerthane = false,
                            favoritesViewModel = favoritesViewModel,
                            audioViewModel = audioViewModel,
                            onBackClick = { 
                                selectedHymn = null
                                audioViewModel.stopAndReset()
                            }
                        )
                    }
                }
            }

            ExpressiveOverlayScreen(
                visible = showMtHymnsFromChristmas,
                onDismiss = { showMtHymnsFromChristmas = false }
            ) {
                Box(Modifier.fillMaxSize()) {
                    HymnsScreen(
                        onHymnClick = { hymn -> selectedHymn = hymn },
                        onSettingsClick = { showMtHymnsFromChristmas = false },
                        activeSection = AppSection.MT,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
                    )
                    ExpressiveOverlayScreen(
                        item = selectedHymn,
                        onDismiss = { 
                            selectedHymn = null
                            audioViewModel.stopAndReset()
                        }
                    ) { hymn ->
                        HymnDetailScreen(
                            hymn = hymn,
                            isKeerthane = false,
                            favoritesViewModel = favoritesViewModel,
                            audioViewModel = audioViewModel,
                            onBackClick = { 
                                selectedHymn = null
                                audioViewModel.stopAndReset()
                            }
                        )
                    }
                }
            }

            ExpressiveOverlayScreen(
                visible = showKeerthanesFromChristmas,
                onDismiss = { showKeerthanesFromChristmas = false }
            ) {
                Box(Modifier.fillMaxSize()) {
                    KeerthaneScreen(
                        onKeerthaneClick = { keerthane -> selectedKeerthane = keerthane },
                        onMenuClick = { showKeerthanesFromChristmas = false },
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
                    )
                    ExpressiveOverlayScreen(
                        item = selectedKeerthane,
                        onDismiss = { 
                            selectedKeerthane = null
                            audioViewModel.stopAndReset()
                        }
                    ) { keerthane ->
                        val convertedHymn = Hymn(
                            number = keerthane.number,
                            title = keerthane.title,
                            signature = keerthane.signature,
                            lyrics = keerthane.lyrics,
                            kannadaLyrics = keerthane.kannadaLyrics
                        )
                        HymnDetailScreen(
                            hymn = convertedHymn,
                            isKeerthane = true,
                            favoritesViewModel = favoritesViewModel,
                            audioViewModel = audioViewModel,
                            onBackClick = { 
                                selectedKeerthane = null
                                audioViewModel.stopAndReset()
                            }
                        )
                    }
                }
            }

            ExpressiveOverlayScreen(
                item = selectedCommonCategory,
                onDismiss = { selectedCommonCategory = null }
            ) { categoryName ->
                Box(Modifier.fillMaxSize()) {
                    OccasionCategoryScreen(
                        categoryName = categoryName,
                        onBackClick = { selectedCommonCategory = null },
                        onHymnClick = { hymn ->
                            selectedHymn = hymn
                        },
                        onKeerthaneClick = { keerthane ->
                            selectedKeerthane = keerthane
                        },
                    )
                }
            }

            ExpressiveOverlayScreen(
                item = selectedCategory,
                onDismiss = { selectedCategory = null }
            ) { category ->
                CategoryDetailScreen(
                    categoryId = category.first,
                    categoryName = category.second,
                    refreshTrigger = categoryRefreshTrigger,
                    onBackClick = { selectedCategory = null },
                    onAddSongsClick = { pickSongsForCategory = selectedCategory },
                    onHymnClick = { hymn ->
                        selectedHymn = hymn
                    },
                    onKeerthaneClick = { keerthane ->
                        selectedKeerthane = keerthane
                    },
                    activeSection = effectiveSection
                )
            }

            ExpressiveOverlayScreen(
                item = pickSongsForCategory,
                onDismiss = { pickSongsForCategory = null }
            ) { category ->
                CategorySongPickerScreen(
                    categoryId = category.first,
                    categoryName = category.second,
                    onBackClick = { 
                        pickSongsForCategory = null 
                        categoryRefreshTrigger++
                    },
                    activeSection = effectiveSection
                )
            }
            ExpressiveOverlayScreen(
                item = selectedHymn,
                onDismiss = { 
                    selectedHymn = null
                    audioViewModel.stopAndReset()
                }
            ) { hymn ->
                HymnDetailScreen(
                    hymn = hymn,
                    isKeerthane = false,
                    favoritesViewModel = favoritesViewModel,
                    audioViewModel = audioViewModel,
                    onBackClick = { 
                        selectedHymn = null
                        audioViewModel.stopAndReset()
                    }
                )
            }

            ExpressiveOverlayScreen(
                item = selectedKeerthane,
                onDismiss = { 
                    selectedKeerthane = null
                    audioViewModel.stopAndReset()
                }
            ) { keerthane ->
                val convertedHymn = Hymn(
                    number = keerthane.number,
                    title = keerthane.title,
                    signature = keerthane.signature,
                    lyrics = keerthane.lyrics,
                    kannadaLyrics = keerthane.kannadaLyrics
                )
                HymnDetailScreen(
                    hymn = convertedHymn,
                    isKeerthane = true,
                    favoritesViewModel = favoritesViewModel,
                    audioViewModel = audioViewModel,
                    onBackClick = { 
                        selectedKeerthane = null
                        audioViewModel.stopAndReset()
                    }
                )
            }
            if (showMenuShowcase) {
                MenuShowcaseOverlay(
                    onDismiss = {
                        OnboardingPrefs.markMenuShowcaseDone(context)
                        showMenuShowcase = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderOverlay(title: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(text = "Coming Soon: $title")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelectorScreen(
    onSelectCsiPage: (Int) -> Unit,
    onSelectMtPage: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Christian Hymn Book", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onMenuClick()
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Choose Hymn Book",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select CSI Hymns and Mangalore Hymns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SectionGroupCard(
                    title = "CSI Hymns & Keerthanes",
                    bottomHint = "csi hymns",
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubItemSelectionCard(
                            title = "CSI Hymns",
                            subtitle = "CSI Kannada Hymns Collection",
                            icon = Icons.Default.MusicNote,
                            onClick = { onSelectCsiPage(0) }
                        )
                        SubItemSelectionCard(
                            title = "CSI Keerthanes",
                            subtitle = "Kannada Keerthanes Collection",
                            icon = Icons.Default.Album,
                            onClick = { onSelectCsiPage(1) }
                        )
                        SubItemSelectionCard(
                            title = "CSI Order of Service",
                            subtitle = "Church Liturgy & Services",
                            icon = Icons.Default.Book,
                            onClick = { onSelectCsiPage(2) }
                        )
                    }
                }

                SectionGroupCard(
                    title = "Mangalore Hymns",
                    bottomHint = "mt hymns",
                    accentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SubItemSelectionCard(
                            title = "MT Hymns",
                            subtitle = "Mangalore Tamil Hymns Book",
                            icon = Icons.Default.LibraryMusic,
                            onClick = onSelectMtPage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionGroupCard(
    title: String,
    bottomHint: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = bottomHint.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            content()
        }
    }
}

@Composable
private fun SubItemSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            HapticFeedbackManager.smoothClick(context)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
