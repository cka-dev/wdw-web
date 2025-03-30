package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.window
import kotlinx.coroutines.delay
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.jetbrains.compose.resources.painterResource
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.ig_logo_96
import wdw_web.composeapp.generated.resources.wdw_logo_2_96
import wdw_web.composeapp.generated.resources.yt_logo_96


@Composable
fun TopNavItem(
    label: String,
    target: AppBarState,
    currentState: AppBarState,
    onClick: () -> Unit
) {
    val isSelected = currentState == target

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF7F33)
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    val indicatorHeight by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .height(indicatorHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    appBarState: MutableState<AppBarState>,
    uiState: LoginUIState,
    onLogout: () -> Unit
) {
    Surface {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.clickable {
                        appBarState.value = AppBarState.HOME
                    }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.wdw_logo_2_96),
                        contentDescription = "Wine Down Wednesday Logo",
                        modifier = Modifier.clickable {
                            appBarState.value = AppBarState.HOME
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wine Down Wednesday",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            appBarState.value = AppBarState.HOME
                        }
                    )
                }
            },
            actions = {
                val navItems = listOf(
                    "Home" to AppBarState.HOME,
                    "About" to AppBarState.ABOUT,
                    "Members" to AppBarState.MEMBERS,
                    "Uncorked Conversations" to AppBarState.PODCASTS,
                    "Events" to AppBarState.EVENTS,
                    "Our Wine" to AppBarState.WINES
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    navItems.forEach { (label, state) ->
                        TopNavItem(
                            label = label,
                            target = state,
                            currentState = appBarState.value,
                            onClick = { appBarState.value = state }
                        )
                    }
                    if (uiState is LoginUIState.Authenticated) {
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = {
                                appBarState.value = AppBarState.PROFILE
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account Circle",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (uiState is LoginUIState.Idle) {
                        Spacer(modifier = Modifier.width(16.dp))
                        TopNavItem(
                            label = "Member Login",
                            target = AppBarState.LOGIN,
                            currentState = appBarState.value,
                            onClick = { appBarState.value = AppBarState.LOGIN }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun Footer(
    isMobile: Boolean,
    modifier: Modifier = Modifier,
) {
    val instagramLink = "https://www.instagram.com/uncorked.conversations/"
    val youtubeLink = "https://www.youtube.com/@FreeHDvideosnocopyright"

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1E1E1E),
            secondary = Color(0xFF333333),
            surface = Color(0xFF141414),
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Connect with Us",
                        fontWeight = FontWeight.Bold,
                    )

                    Row {
                        Icon(
                            painter = painterResource(Res.drawable.ig_logo_96),
                            contentDescription = "Instagram logo",
                            tint = Color.White,
                            modifier = Modifier
                                .clickable {
                                    window.open(instagramLink)
                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.yt_logo_96),
                            contentDescription = "Youtube logo",
                            tint = Color.White,
                            modifier = Modifier.clickable {
                                window.open(youtubeLink)
                            }
                        )
                    }
                }
                FooterColumn(
                    title = "Contact",
                    items = listOf("info@winedownwednesday.net", "+1 (404) 939-3370"),
                    onLinkClicked = {}
                )
                AnimatedVisibility(!isMobile){
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Get our Apps",
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = "Coming Soon",
                            modifier = Modifier.selectable(
                                selected = false,
                                enabled = true,
                                onClick = {}
                            )
                        )

//                        Row {
//                            Icon(
//                                painter = painterResource(Res.drawable.Google_Play_App),
//                                contentDescription = "Get it on Google Play",
//                                tint = Color.White,
//                                modifier = Modifier
//                                    .clickable {
//                                        window.open(instagramLink)
//                                    }
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Icon(
//                                painter = painterResource(Res.drawable.yt_logo_96),
//                                contentDescription = "Youtube logo",
//                                tint = Color.White,
//                                modifier = Modifier.clickable {
//                                    window.open(youtubeLink)
//                                }
//                            )
//                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FooterColumn(
    title: String,
    items: List<String>,
    isLinks: Boolean = false,
    onLinkClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
        )
        items.forEach { item ->
            if (isLinks) {
                Text(
                    text = item,
                    modifier = Modifier.selectable(
                        selected = false,
                        enabled = true,
                        onClick = { onLinkClicked() }
                    )
                )
            } else {
                Text(
                    text = item,
                    modifier = Modifier.selectable(
                        selected = false,
                        enabled = true,
                        onClick = {}
                    )
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = { onQueryChange(it) },
        label = { Text(
            text = label,
            color = Color.White
        ) },
        colors = TextFieldDefaults.colors(
            cursorColor = Color(0xFFFF7F33)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toast(message: String) {
    var showToast by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = message) {
        delay(3000)
        showToast = false
    }

    if (showToast) {
        BasicAlertDialog(
            onDismissRequest = { showToast = false },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp)
                ) {
                    Text(text = message, color = Color.White)
                }
            }
        }

    }
}

@Composable
fun LinearProgressBar(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        LinearProgressIndicator(
            color = Color(0xFFFF7F33),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}