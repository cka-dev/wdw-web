package net.winedownwednesday.web.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import net.winedownwednesday.web.getSafeAreaInsetBottom
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.jetbrains.compose.resources.painterResource
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.wdw_new_logo


@Composable
fun TopNavItem(
    label: String,
    target: Route,
    currentRoute: Route,
    onClick: () -> Unit
) {
    val isSelected = currentRoute == target

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF7F33)
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    val indicatorHeight by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "navPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .hoverScale(scale = 1.12f)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFF7F33).copy(alpha = 0.35f * pulseAlpha),
                                Color(0xFFFF7F33).copy(alpha = 0.35f * pulseAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .height(indicatorHeight)
                .fillMaxWidth()
                .background(Color(0xFFFF7F33).copy(alpha = if (isSelected) pulseAlpha else 1f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    currentRoute: Route,
    uiState: LoginUIState,
    onNavigate: (Route) -> Unit,
    onLogout: () -> Unit,
    userProfileImageUrl: String? = null,
    isCompactScreen: Boolean = false,
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onHamburgerClick: () -> Unit = {}
) {
    val navBgBrush = if (!isDarkTheme)
        Brush.horizontalGradient(listOf(ChampagneLight, ChampagneDark))
    else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { m ->
                if (navBgBrush != null) m.background(navBgBrush)
                else m.background(MaterialTheme.colorScheme.primary)
            }
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            navigationIcon = {
                if (isCompactScreen) {
                    IconButton(onClick = onHamburgerClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            title = {
                val logoReady by remember { mutableStateOf(false) }.let { state ->
                    LaunchedEffect(Unit) { delay(300); state.value = true }
                    state
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.clickable { onNavigate(Route.Home) }
                ) {
                    if (logoReady) {
                        Image(
                            painter = painterResource(Res.drawable.wdw_new_logo),
                            contentDescription = "Wine Down Wednesday Logo",
                            modifier = Modifier.height(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Wine Down Wednesday",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            actions = {
                if (isCompactScreen) {
                    // Compact: just profile avatar or login
                    if (uiState is LoginUIState.Authenticated) {
                        IconButton(
                            onClick = { onNavigate(Route.Profile) }
                        ) {
                            if (!userProfileImageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfileImageUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Account Circle",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else if (uiState is LoginUIState.Idle) {
                        IconButton(
                            onClick = { onNavigate(Route.Login) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Login",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Desktop: full horizontal nav
                    val navItems by remember(uiState) {
                        derivedStateOf {
                            buildList {
                                add("Home"                    to Route.Home)
                                add("Our Story"               to Route.About)
                                add("Tasting Notes"            to Route.Blog)
                                add("Members"                 to Route.Members)
                                add("Uncorked Conversations"  to Route.Podcasts)
                                add("Gatherings"              to Route.Events)
                                add("The Cellar"              to Route.Wines)
                                if (uiState is LoginUIState.Authenticated) {
                                    add("Bubbly Chat" to Route.Messaging)
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        navItems.forEach { (label, route) ->
                            TopNavItem(
                                label        = label,
                                target       = route,
                                currentRoute = currentRoute,
                                onClick      = { onNavigate(route) }
                            )
                        }
                        // Theme toggle removed from here — placed after login below
                        if (uiState is LoginUIState.Authenticated) {
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = { onNavigate(Route.Profile) }
                            ) {
                                if (!userProfileImageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = userProfileImageUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Account Circle",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else if (uiState is LoginUIState.Idle) {
                            Spacer(modifier = Modifier.width(16.dp))
                            TopNavItem(
                                label        = "Member Login",
                                target       = Route.Login,
                                currentRoute = currentRoute,
                                onClick      = { onNavigate(Route.Login) }
                            )
                        }
                    }
                }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTogglePill(
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(if (isDarkTheme) "Switch to light mode" else "Switch to dark mode")
            }
        },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = WdwOrange,
            contentColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode"
            )
        }
    }
}

@Composable
fun MobileBottomNavBar(
    currentRoute: Route,
    uiState: LoginUIState,
    onNavigate: (Route) -> Unit
) {
    val bottomTabs = listOf(
        Triple("Home",         Icons.Default.Home,                  Route.Home),
        Triple("Podcasts",     Icons.Default.Podcasts,              Route.Podcasts),
        Triple("Gatherings",   Icons.Default.Event,                 Route.Events),
        Triple("The Cellar",   Icons.Default.WineBar,               Route.Wines),
        Triple("Bubbly Chat",  Icons.AutoMirrored.Filled.Chat,      Route.Messaging)
    )

    val isDark = LocalIsDarkTheme.current
    val navBgBrush = if (!isDark)
        Brush.horizontalGradient(listOf(ChampagneLight, ChampagneDark))
    else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { m ->
                if (navBgBrush != null) m.background(navBgBrush)
                else m.background(MaterialTheme.colorScheme.primary)
            }
    ) {
        var safeAreaBottom by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            safeAreaBottom = getSafeAreaInsetBottom()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    bottom = 8.dp + safeAreaBottom.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.forEach { (label, icon, route) ->
                val isSelected = currentRoute == route
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFF7F33) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                    animationSpec = tween(200)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            hapticVibrate(HapticDuration.LIGHT, HapticCategory.NAVIGATION)
                            if (route == Route.Messaging && uiState !is LoginUIState.Authenticated) {
                                onNavigate(Route.Login)
                            } else {
                                onNavigate(route)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        color = color,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NavDrawerContent(
    currentRoute: Route,
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onNavigate: (Route) -> Unit
) {
    val drawerItems = listOf(
        Triple("Members",       Icons.Default.People,                  Route.Members),
        Triple("Tasting Notes", Icons.AutoMirrored.Filled.Article,     Route.Blog)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "More",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        drawerItems.forEach { (label, icon, route) ->
            val isSelected = currentRoute == route
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        hapticVibrate(HapticDuration.LIGHT, HapticCategory.NAVIGATION)
                        onNavigate(route)
                    }
                    .background(
                        if (isSelected) Color(0xFF333333) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color(0xFFFF7F33) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = if (isSelected) Color(0xFFFF7F33) else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Push haptic settings to the bottom
        Spacer(modifier = Modifier.weight(1f))

        // ── Theme Toggle ──────────────────────────────────────────────────
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onThemeToggle() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isDarkTheme) "Dark Mode" else "Light Mode",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            androidx.compose.material3.Icon(
                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = WdwOrange,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Haptic Feedback Settings ─────────────────────────────────────
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = "Haptic Feedback",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Intensity row
        val (hapticIntensity, setHapticIntensity) = rememberHapticIntensity()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HapticIntensity.entries.forEach { level ->
                val isSelected = hapticIntensity == level
                Surface(
                    onClick = {
                        if (level == HapticIntensity.OFF && hapticIntensity == HapticIntensity.OFF) {
                            // Currently off → restore to Normal
                            setHapticIntensity(HapticIntensity.NORMAL)
                            hapticVibrate(HapticDuration.MEDIUM)
                        } else {
                            setHapticIntensity(level)
                            if (level != HapticIntensity.OFF) {
                                hapticVibrate(HapticDuration.MEDIUM)
                            }
                        }
                    },
                    color = if (isSelected) Color(0xFFFF7F33) else Color(0xFF333333),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (level == HapticIntensity.OFF && hapticIntensity == HapticIntensity.OFF)
                            "On" else level.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }

        // Per-category toggles (only shown when not OFF)
        if (hapticIntensity != HapticIntensity.OFF) {
            Spacer(modifier = Modifier.height(8.dp))
            HapticCategory.entries.forEach { category ->
                val (enabled, setEnabled) = rememberCategoryEnabled(category)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newVal = !enabled
                            setEnabled(newVal)
                            if (newVal) hapticVibrate(HapticDuration.TICK)
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.label,
                        color = if (enabled) Color.White else Color(0xFF777777),
                        fontSize = 13.sp
                    )
                    Surface(
                        color = if (enabled) Color(0xFFFF7F33) else Color(0xFF444444),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(width = 36.dp, height = 20.dp)
                    ) {
                        Box(contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(16.dp)
                            ) {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
            color = MaterialTheme.colorScheme.onSurface
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