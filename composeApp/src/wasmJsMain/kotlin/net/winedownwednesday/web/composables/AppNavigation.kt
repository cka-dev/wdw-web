package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.winedownwednesday.web.loadThemePreference
import net.winedownwednesday.web.saveThemePreference
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.BlogPageViewModel
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.WinePageViewModel
import net.winedownwednesday.web.data.repositories.AppRepository
import org.koin.compose.koinInject
import org.w3c.dom.events.Event

// ---------------------------------------------------------------------------
// Route definitions — sealed interface implementing NavKey.
// ---------------------------------------------------------------------------
@Serializable sealed interface Route : NavKey {
    @Serializable data object Home      : Route
    @Serializable data object About     : Route
    @Serializable data object Members   : Route
    @Serializable data object Podcasts  : Route
    @Serializable data object Events    : Route
    @Serializable data object Wines     : Route
    @Serializable data object Login     : Route
    @Serializable data object Profile   : Route
    @Serializable data object Blog      : Route
    @Serializable data object Messaging : Route
    @Serializable data object Settings  : Route
}

// Map a Route to its URL hash fragment (e.g. Route.Home → "home")
private fun Route.toHash(): String = when (this) {
    is Route.Home      -> "home"
    is Route.About     -> "about"
    is Route.Members   -> "members"
    is Route.Podcasts  -> "podcasts"
    is Route.Events    -> "events"
    is Route.Wines     -> "wines"
    is Route.Login     -> "login"
    is Route.Profile   -> "profile"
    is Route.Blog      -> "blog"
    is Route.Messaging -> "messaging"
    is Route.Settings  -> "settings"
}

// Restore a Route from a URL hash string (handles "#home" or "home")
// Strips query params so "events?eventId=123" resolves to Route.Events
private fun routeFromHash(hash: String): Route {
    val name = hash.removePrefix("#").trimEnd('/').substringBefore('?')
    return when (name) {
        "home"      -> Route.Home
        "about"     -> Route.About
        "members"   -> Route.Members
        "podcasts"  -> Route.Podcasts
        "events"    -> Route.Events
        "wines"     -> Route.Wines
        "login"     -> Route.Login
        "profile"   -> Route.Profile
        "blog"      -> Route.Blog
        "messaging" -> Route.Messaging
        "settings"  -> Route.Settings
        else        -> Route.Home
    }
}

/**
 * Extract a query parameter from a hash fragment.
 * e.g. extractHashParam("#events?eventId=123", "eventId") → "123"
 */
private fun extractHashParam(hash: String, key: String): String? {
    val query = hash.substringAfter('?', "")
    if (query.isEmpty()) return null
    return query.split('&')
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotBlank() }
}

// ---------------------------------------------------------------------------
// AppNavigation — main entry point composable
// ---------------------------------------------------------------------------
@OptIn(ExperimentalWasmJsInterop::class)
@Composable
fun AppNavigation(
    authViewModel: AuthPageViewModel = koinInject()
) {
    val eventsViewModel: EventsPageViewModel = koinInject()
    val wineViewModel: WinePageViewModel = koinInject()
    val membersViewModel: MembersPageViewModel = koinInject()
    val blogViewModel: BlogPageViewModel = koinInject()
    val appRepository: AppRepository = koinInject()
    val featureFlags by appRepository.featureFlags.collectAsState()
    // --- Nav 3 back stack ------------------------------------------------
    // SavedStateConfiguration is required by the API. An empty config is safe
    // because browser URL hash handles route restoration on reload.
    // SerializersModule required by Nav3 to find Route subtypes in the NavKey polymorphic scope
    val navSerializersModule = remember {
        SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Route.Home::class)
                subclass(Route.About::class)
                subclass(Route.Members::class)
                subclass(Route.Podcasts::class)
                subclass(Route.Events::class)
                subclass(Route.Wines::class)
                subclass(Route.Login::class)
                subclass(Route.Profile::class)
                subclass(Route.Blog::class)
                subclass(Route.Messaging::class)
                subclass(Route.Settings::class)
            }
        }
    }
    val backStack = rememberNavBackStack(
        SavedStateConfiguration { serializersModule = navSerializersModule },
        routeFromHash(window.location.hash)
    )

    // Convenience navigate helpers
    fun navigateTo(route: Route) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }
    fun replaceTop(route: Route) {
        if (backStack.isEmpty()) backStack.add(route)
        else backStack[backStack.lastIndex] = route
    }

    // --- Auth state -------------------------------------------------------
    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedIn = uiState is LoginUIState.Authenticated
    val isNewUser by authViewModel.isNewUser.collectAsState()
    val userEmail by authViewModel.email.collectAsState()
    val userProfileData by authViewModel.profileData.collectAsState()

    // --- Responsive layout ------------------------------------------------
    val sizeInfo = rememberWindowSizeInfo()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // --- Theme state (persisted in localStorage) --------------------------
    var isDarkTheme by remember { mutableStateOf(loadThemePreference()) }
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        saveThemePreference(isDarkTheme)
    }

    // Current top-of-stack route (drives nav bar highlighting)
    val currentRoute by remember {
        derivedStateOf { backStack.lastOrNull() as? Route ?: Route.Home }
    }

    // --- Auth-driven redirects -------------------------------------------
    LaunchedEffect(isLoggedIn) {
        val top = backStack.lastOrNull() as? Route
        if (isLoggedIn && top == Route.Login) {
            authViewModel.requestNotificationPermissionAndGetToken()
            replaceTop(Route.Profile)
        } else if (!isLoggedIn && top == Route.Profile) {
            replaceTop(Route.Home)
        }
    }

    // Capture the initial hash at composition time — BEFORE any LaunchedEffect
    // can overwrite window.location.hash (e.g. the currentRoute sync below).
    val initialHash = remember { window.location.hash }

    // Helper: extract deep-link query params from a hash fragment
    // and set the corresponding pending IDs on the ViewModels.
    fun applyDeepLinkParams(hash: String) {
        extractHashParam(hash, "eventId")
            ?.toLongOrNull()
            ?.let { eventsViewModel.setPendingEventId(it) }
        extractHashParam(hash, "wineId")
            ?.toLongOrNull()
            ?.let { wineViewModel.setPendingWineId(it) }
        extractHashParam(hash, "memberId")
            ?.toLongOrNull()
            ?.let { membersViewModel.setPendingMemberId(it) }
        extractHashParam(hash, "postId")
            ?.let { blogViewModel.setPendingPostId(it) }
    }

    // --- Browser history sync --------------------------------------------
    // LaunchedEffect(currentRoute) re-runs every time the route changes,
    // so the URL hash always reflects the current page.
    LaunchedEffect(currentRoute) {
        val hash = "#${currentRoute.toHash()}"
        if (window.location.hash != hash) {
            window.history.pushState(null?.toJsString(), "", hash)
        }
    }

    // Restore route from URL hash on first launch (e.g. bookmarked link)
    LaunchedEffect(Unit) {
        if (initialHash.isNotEmpty() && initialHash != "#${currentRoute.toHash()}") {
            replaceTop(routeFromHash(initialHash))
        }
        // Deep-link: auto-open content by ID from query params
        applyDeepLinkParams(initialHash)
    }

    // Listen for browser Back/Forward and hash changes (e.g. pasting a deep-link URL)
    DisposableEffect(Unit) {
        val popStateListener: (Event) -> Unit = {
            val hash = window.location.hash
            val targetRoute = routeFromHash(hash)
            // If the 2nd-to-last entry matches the target this is a back press → pop
            val prevRoute = if (backStack.size >= 2)
                backStack[backStack.lastIndex - 1] as? Route else null
            if (prevRoute == targetRoute) {
                backStack.removeAt(backStack.lastIndex)
            } else {
                replaceTop(targetRoute)
            }
            // Also extract deep-link params from the hash
            applyDeepLinkParams(hash)
        }
        val hashChangeListener: (Event) -> Unit = {
            val hash = window.location.hash
            val targetRoute = routeFromHash(hash)
            if (backStack.lastOrNull() != targetRoute) {
                replaceTop(targetRoute)
            }
            // Extract deep-link params from the new hash
            applyDeepLinkParams(hash)
        }
        window.addEventListener("popstate", popStateListener)
        window.addEventListener("hashchange", hashChangeListener)
        onDispose {
            window.removeEventListener("popstate", popStateListener)
            window.removeEventListener("hashchange", hashChangeListener)
        }
    }

    // --- Theme & layout --------------------------------------------------
    CompositionLocalProvider(LocalFeatureFlags provides featureFlags) {
    WdwTheme(isDark = isDarkTheme) {
        val mainContent: @Composable () -> Unit = {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopNavBar(
                        currentRoute        = currentRoute,
                        uiState             = uiState,
                        onNavigate          = { navigateTo(it) },
                        onLogout            = { scope.launch { authViewModel.logout() } },
                        userProfileImageUrl = userProfileData?.profileImageUrl,
                        isCompactScreen     = sizeInfo.useCompactNav,
                        isDarkTheme         = isDarkTheme,
                        onThemeToggle       = ::toggleTheme,
                        onHamburgerClick    = {
                            hapticVibrate(
                                HapticDuration.TICK,
                                HapticCategory.NAVIGATION
                            )
                            scope.launch { drawerState.open() }
                        }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        NavDisplay(
                            backStack = backStack,
                            entryProvider = entryProvider {
                                entry<Route.Home> {
                                    FadeInPage {
                                        HomePage(
                                            sizeInfo = sizeInfo,
                                            isLoggedIn = isLoggedIn,
                                            onJoinClick = { navigateTo(Route.Login) }
                                        )
                                    }
                                }
                                entry<Route.About> {
                                    FadeInPage {
                                        AboutPage(
                                            sizeInfo = sizeInfo
                                        )
                                    }
                                }
                                entry<Route.Members>   {
                                    FadeInPage {
                                        MembersPage(
                                            sizeInfo = sizeInfo,
                                            uiState = uiState,
                                            userProfileData = userProfileData
                                        )
                                    }
                                }
                                entry<Route.Podcasts>  {
                                    FadeInPage {
                                        PodcastsPage(
                                            sizeInfo = sizeInfo
                                        )
                                    }
                                }
                                entry<Route.Events>    {
                                    FadeInPage {
                                        EventsPage(
                                            sizeInfo = sizeInfo,
                                            uiState = uiState,
                                            authPageViewModel = authViewModel
                                        )
                                    }
                                }
                                entry<Route.Wines>     {
                                    FadeInPage {
                                        WinePage(
                                            sizeInfo = sizeInfo,
                                            isLoggedIn = isLoggedIn,
                                            userName = userProfileData?.name,
                                            userEmail = userEmail
                                        )
                                    }
                                }
                                entry<Route.Login>     {
                                    if (!isLoggedIn) {
                                        FadeInPage {
                                            LoginScreen(
                                                isCompactScreen = sizeInfo.useCompactNav,
                                                onLoginSuccess  = { },
                                                viewModel       = authViewModel
                                            )
                                        }
                                    } else {
                                        LaunchedEffect(Unit) { replaceTop(Route.Home) }
                                    }
                                }
                                entry<Route.Profile>   {
                                    FadeInPage {
                                        ProfilePage(
                                            isCompactScreen        = sizeInfo.useCompactNav,
                                            onLogout               = { scope.launch { authViewModel.logout() } },
                                            isNewUser              = isNewUser,
                                            viewModel              = authViewModel,
                                            userEmail              = userEmail,
                                            onNavigateToSettings   = { navigateTo(Route.Settings) }
                                        )
                                    }
                                }
                                entry<Route.Blog>      { FadeInPage { BlogPage(sizeInfo = sizeInfo) } }
                                entry<Route.Messaging> {
                                    if (isLoggedIn) {
                                        MessagingScreen(
                                            isCompactScreen = sizeInfo.useCompactNav,
                                            onNavigateToEvents = { eventName ->
                                                if (eventName.isNotBlank()) {
                                                    eventsViewModel.setPendingEventName(eventName)
                                                }
                                                navigateTo(Route.Events)
                                            },
                                            onNavigateToWines = { wineName ->
                                                if (wineName.isNotBlank()) {
                                                    wineViewModel.setPendingWineName(wineName)
                                                }
                                                navigateTo(Route.Wines)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { replaceTop(Route.Login) }
                                    }
                                }
                                entry<Route.Settings> {
                                    if (isLoggedIn) {
                                        FadeInPage {
                                            SettingsPage(
                                                isCompactScreen = sizeInfo.useCompactNav,
                                                viewModel       = authViewModel,
                                                onLogout        = { scope.launch { authViewModel.logout() } },
                                                onNavigateBack  = {
                                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                                    else replaceTop(Route.Profile)
                                                }
                                            )
                                        }
                                    } else {
                                        LaunchedEffect(Unit) { replaceTop(Route.Login) }
                                    }
                                }
                            }
                        )
                    // Floating theme toggle — only on wide screens
                    if (!sizeInfo.useCompactNav && currentRoute !is Route.Messaging) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            ThemeTogglePill(
                                isDarkTheme = isDarkTheme,
                                onClick     = ::toggleTheme,
                                modifier    = Modifier.padding(end = 24.dp, bottom = 24.dp)
                            )
                        }
                    }
                    }

                    if (sizeInfo.useCompactNav) {
                        CompactFooter()
                        MobileBottomNavBar(
                            currentRoute = currentRoute,
                            uiState      = uiState,
                            onNavigate   = { navigateTo(it) }
                        )
                    } else {
                        Footer(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (sizeInfo.useCompactNav) {
            ModalNavigationDrawer(
                drawerState   = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier             = Modifier.width(240.dp),
                        drawerContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ) {
                        NavDrawerContent(
                            currentRoute  = currentRoute,
                            isDarkTheme   = isDarkTheme,
                            onThemeToggle = ::toggleTheme,
                            onNavigate    = { route ->
                                navigateTo(route)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) { mainContent() }
        } else {
            mainContent()
        }
    }
    } // CompositionLocalProvider(LocalFeatureFlags)
}