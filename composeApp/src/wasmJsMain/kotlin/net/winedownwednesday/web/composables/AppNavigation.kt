package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
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
}

// Restore a Route from a URL hash string (handles "#home" or "home")
private fun routeFromHash(hash: String): Route {
    val name = hash.removePrefix("#").trimEnd('/')
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
        else        -> Route.Home
    }
}

// ---------------------------------------------------------------------------
// AppNavigation — main entry point composable
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalWasmJsInterop::class)
@Composable
fun AppNavigation(
    authViewModel: AuthPageViewModel = koinInject()
) {
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
            }
        }
    }
    val backStack = rememberNavBackStack(
        SavedStateConfiguration { serializersModule = navSerializersModule },
        Route.Home
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
    val windowSizeClass = calculateWindowSizeClass()
    val isCompactScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

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
        val initialHash = window.location.hash
        if (initialHash.isNotEmpty() && initialHash != "#${currentRoute.toHash()}") {
            replaceTop(routeFromHash(initialHash))
        }
    }

    // Listen for browser Back/Forward buttons
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = {
            val targetRoute = routeFromHash(window.location.hash)
            // If the 2nd-to-last entry matches the target this is a back press → pop
            val prevRoute = if (backStack.size >= 2)
                backStack[backStack.lastIndex - 1] as? Route else null
            if (prevRoute == targetRoute) {
                backStack.removeAt(backStack.lastIndex)
            } else {
                replaceTop(targetRoute)
            }
        }
        window.addEventListener("popstate", listener)
        onDispose { window.removeEventListener("popstate", listener) }
    }

    // --- Theme & layout --------------------------------------------------
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary   = Color(0xFF1E1E1E),
            secondary = Color(0xFF333333),
            surface   = Color(0xFF2A2A2A),
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
        val mainContent: @Composable () -> Unit = {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopNavBar(
                        currentRoute        = currentRoute,
                        uiState             = uiState,
                        onNavigate          = { navigateTo(it) },
                        onLogout            = { scope.launch { authViewModel.logout() } },
                        userProfileImageUrl = userProfileData?.profileImageUrl,
                        isCompactScreen     = isCompactScreen,
                        onHamburgerClick    = { scope.launch { drawerState.open() } }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        NavDisplay(
                            backStack = backStack,
                            entryProvider = entryProvider {
                                entry<Route.Home>      { HomePage(isCompactScreen = isCompactScreen) }
                                entry<Route.About>     { AboutPage(isCompactScreen = isCompactScreen) }
                                entry<Route.Members>   {
                                    MembersPage(
                                        isCompactScreen = isCompactScreen,
                                        uiState         = uiState,
                                        userProfileData = userProfileData
                                    )
                                }
                                entry<Route.Podcasts>  { PodcastsPage(isCompactScreen = isCompactScreen) }
                                entry<Route.Events>    {
                                    EventsPage(
                                        isCompactScreen   = isCompactScreen,
                                        uiState           = uiState,
                                        authPageViewModel = authViewModel
                                    )
                                }
                                entry<Route.Wines>     { WinePage(isCompactScreen = isCompactScreen) }
                                entry<Route.Login>     {
                                    if (!isLoggedIn) {
                                        LoginScreen(
                                            isCompactScreen = isCompactScreen,
                                            onLoginSuccess  = { },
                                            viewModel       = authViewModel
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { replaceTop(Route.Home) }
                                    }
                                }
                                entry<Route.Profile>   {
                                    ProfilePage(
                                        isCompactScreen = isCompactScreen,
                                        onLogout        = { scope.launch { authViewModel.logout() } },
                                        isNewUser       = isNewUser,
                                        viewModel       = authViewModel,
                                        userEmail       = userEmail
                                    )
                                }
                                entry<Route.Blog>      { BlogPage(isCompactScreen = isCompactScreen) }
                                entry<Route.Messaging> {
                                    if (isLoggedIn) {
                                        MessagingScreen(isCompactScreen = isCompactScreen)
                                    } else {
                                        LaunchedEffect(Unit) { replaceTop(Route.Login) }
                                    }
                                }
                            }
                        )
                    }

                    if (isCompactScreen) {
                        CompactFooter()
                        MobileBottomNavBar(
                            currentRoute = currentRoute,
                            uiState      = uiState,
                            onNavigate   = { navigateTo(it) }
                        )
                    } else {
                        Footer(
                            onNavClick = { navigateTo(it) },
                            modifier   = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (isCompactScreen) {
            ModalNavigationDrawer(
                drawerState   = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier             = Modifier.width(240.dp),
                        drawerContainerColor = Color(0xFF1E1E1E)
                    ) {
                        NavDrawerContent(
                            currentRoute = currentRoute,
                            onNavigate   = { route ->
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
}