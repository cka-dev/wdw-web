package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.ig_logo_96
import wdw_web.composeapp.generated.resources.wdw_logo_2_96
import wdw_web.composeapp.generated.resources.yt_logo_96

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    selectedPageState: MutableState<WDWPages>
 ) {
    val viewModel: AuthPageViewModel = koinInject()

    val showAuthPageCardState = viewModel.showAuthCard.collectAsState()

    Surface {
        TopAppBar(title = {
            Row {
                Image(
                    painter = painterResource(Res.drawable.wdw_logo_2_96),
                    contentDescription = "Wine Down Wednesday Logo",
                    modifier = Modifier.clickable {
                        selectedPageState.value = WDWPages.HOME
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wine Down Wednesday",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            selectedPageState.value = WDWPages.HOME
                        }
                        .align(Alignment.CenterVertically)
                )
            }
        }, actions = {
            LazyRow(verticalAlignment = Alignment.CenterVertically) {
                item {
                    Text(
                        text = "Home",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.HOME
                            }
                    )
                }
                item {
                    Text(
                        text = "About",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.ABOUT
                            }
                    )
                }
                item {
                    Text(
                        text = "Members",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.MEMBERS
                            }
                    )
                }
                item {
                    Text(
                        text = "Uncorked Conversations",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.PODCASTS
                            }
                    )
                }
                item {
                    Text(
                        text = "Events",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.EVENTS
                            }
                    )
                }
                item {
                    Text(
                        text = "Our Wine",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedPageState.value = WDWPages.WINE
                            }
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(16.dp))
                }
                item {
                    Text(
                        text = "Member Login",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                viewModel.setShowAuthCardState(
                                    value = true
                                )
                            }
                    )
                }
            }
        })

        AnimatedVisibility(showAuthPageCardState.value) {
            UnderConstruction(
                onDismissRequest = {
                    viewModel.setShowAuthCardState(
                        value = false
                    )
                }
            )
        }
    }
}

@Composable
fun Footer(
    isMobile: Boolean,
    modifier: Modifier = Modifier,
) {
    val instagram_link = "https://www.instagram.com/uncorked.conversations/"
    val youtube_link = "https://www.youtube.com/@FreeHDvideosnocopyright"

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
                            modifier = Modifier
                                .clickable {
                                    window.open(instagram_link)
                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.yt_logo_96),
                            contentDescription = "Youtube logo",
                            modifier = Modifier.clickable {
                                window.open(youtube_link)
                            }
                        )
                    }
                }
                FooterColumn(
                    title = "Contact",
                    items = listOf("info@winesocialclub.com", "+1 234 567 890"),
                    onLinkClicked = {}
                )
                AnimatedVisibility(!isMobile){
                    FooterColumn(
                        title = "Resources",
                        items = listOf("Privacy Policy", "Terms of Service"),
                        isLinks = true,
                        {}
                    )
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