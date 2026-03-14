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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.koin.compose.koinInject

enum class AppBarState {
    HOME,
    ABOUT,
    MEMBERS,
    PODCASTS,
    EVENTS,
    WINES,
    LOGIN,
    PROFILE,
    BLOG,
    MESSAGING
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation(
    authViewModel: AuthPageViewModel = koinInject()
) {
    val appBarState = remember { mutableStateOf(AppBarState.HOME) }
    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedIn = uiState is LoginUIState.Authenticated
    val isNewUser by authViewModel.isNewUser.collectAsState()
    val userEmail by authViewModel.email.collectAsState()
    val userProfileData by authViewModel.profileData.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val isCompactScreen = windowSizeClass.widthSizeClass ==
            WindowWidthSizeClass.Compact
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Sync appBarState with auth state for auto-redirection if needed
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && appBarState.value == AppBarState.LOGIN) {
            appBarState.value = AppBarState.PROFILE
            authViewModel.requestNotificationPermissionAndGetToken()
        } else if (!isLoggedIn && appBarState.value == AppBarState.PROFILE) {
            appBarState.value = AppBarState.HOME
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1E1E1E),
            secondary = Color(0xFF333333),
            surface = Color(0xFF2A2A2A),
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
        val mainContent: @Composable () -> Unit = {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TopNavBar(
                        appBarState = appBarState,
                        uiState = uiState,
                        onLogout = {
                            scope.launch {
                                authViewModel.logout()
                            }
                        },
                        userProfileImageUrl = userProfileData?.profileImageUrl,
                        isCompactScreen = isCompactScreen,
                        onHamburgerClick = {
                            scope.launch { drawerState.open() }
                        }
                    )

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        when (appBarState.value) {
                            AppBarState.HOME -> HomePage(
                                isCompactScreen = isCompactScreen
                            )

                            AppBarState.ABOUT -> AboutPage(
                                isCompactScreen = isCompactScreen
                            )

                            AppBarState.MEMBERS -> MembersPage(
                                isCompactScreen = isCompactScreen,
                                uiState = uiState,
                                userProfileData = userProfileData
                            )

                            AppBarState.PODCASTS -> PodcastsPage(
                                isCompactScreen = isCompactScreen
                            )

                            AppBarState.EVENTS -> EventsPage(
                                isCompactScreen = isCompactScreen,
                                uiState = uiState,
                                authPageViewModel = authViewModel
                            )

                            AppBarState.WINES -> WinePage(
                                isCompactScreen = isCompactScreen
                            )

                            AppBarState.LOGIN -> {
                                if (!isLoggedIn) {
                                    LoginScreen(
                                        isCompactScreen = isCompactScreen,
                                        onLoginSuccess = {
                                            // Redirection handled by LaunchedEffect
                                        },
                                        viewModel = authViewModel
                                    )
                                } else {
                                    appBarState.value = AppBarState.HOME
                                }
                            }

                            AppBarState.PROFILE -> {
                                ProfilePage(
                                    isCompactScreen = isCompactScreen,
                                    onLogout = {
                                        scope.launch {
                                            authViewModel.logout()
                                        }
                                    },
                                    isNewUser = isNewUser,
                                    viewModel = authViewModel,
                                    userEmail = userEmail
                                )
                            }

                            AppBarState.BLOG -> {
                                BlogPage(isCompactScreen = isCompactScreen)
                            }

                            AppBarState.MESSAGING -> {
                                if (isLoggedIn) {
                                    MessagingScreen(isCompactScreen = isCompactScreen)
                                } else {
                                    appBarState.value = AppBarState.LOGIN
                                }
                            }
                        }
                    }

                    if (isCompactScreen) {
                        MobileBottomNavBar(
                            appBarState = appBarState,
                            uiState = uiState
                        )
                    } else {
                        Footer(
                            isMobile = isCompactScreen,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (isCompactScreen) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(240.dp),
                        drawerContainerColor = Color(0xFF1E1E1E)
                    ) {
                        NavDrawerContent(
                            appBarState = appBarState,
                            onItemSelected = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                mainContent()
            }
        } else {
            mainContent()
        }
    }
}