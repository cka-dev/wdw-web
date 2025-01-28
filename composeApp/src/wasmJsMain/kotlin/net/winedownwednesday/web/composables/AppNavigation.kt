package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
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
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation(
    authViewModel: AuthPageViewModel = koinInject()
) {
    val appBarState = remember { mutableStateOf(AppBarState.HOME) }
    val uiState by authViewModel.uiState.collectAsState()
    var isLoggedIn by remember { mutableStateOf(false) }
    val isNewUser by authViewModel.isNewUser.collectAsState()
    val userEmil by authViewModel.email.collectAsState()
    val windowSizeClass = calculateWindowSizeClass()
    val isCompactScreen = windowSizeClass.widthSizeClass ==
            WindowWidthSizeClass.Compact

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1E1E1E),
            secondary = Color(0xFF333333),
            surface = Color(0xFF141414),
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
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
                        isLoggedIn = false
                        appBarState.value = AppBarState.HOME
                        authViewModel.logout()
                    }
                )

                Box(
                    modifier = Modifier.weight(1f)
                ){
                    when (appBarState.value) {
                        AppBarState.HOME -> HomePage(
                            isCompactScreen = isCompactScreen
                        )


                        AppBarState.ABOUT -> AboutPage(
                            isCompactScreen = isCompactScreen
                        )
                        AppBarState.MEMBERS -> MembersPage(
                            isCompactScreen = isCompactScreen
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
                                        appBarState.value = AppBarState.PROFILE
                                        isLoggedIn = true
                                        authViewModel.requestNotificationPermissionAndGetToken()
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
                                    isLoggedIn = false
                                    appBarState.value = AppBarState.HOME
                                    authViewModel.logout()
                                },
                                isNewUser = isNewUser,
                                viewModel = authViewModel,
                                userEmail = userEmil
                            )
                        }
                    }

                }

                Footer(
                    isMobile = isCompactScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}