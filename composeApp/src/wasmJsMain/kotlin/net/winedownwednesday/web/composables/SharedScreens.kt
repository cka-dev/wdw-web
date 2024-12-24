package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.ig_logo_96
import wdw_web.composeapp.generated.resources.tiktok_logo_96
import wdw_web.composeapp.generated.resources.wdw_logo_2_96
import wdw_web.composeapp.generated.resources.yt_logo_96

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    selectedPageState: MutableState<WDWPages>
 ) {

    Surface {
        TopAppBar(title = {
            Row {
                Image(
                    painter = painterResource(Res.drawable.wdw_logo_2_96),
                    contentDescription = "Wine Down Wednesday Logo",
                    modifier = Modifier.clickable {

                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wine Down Wednesday",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {

                        }
                        .align(Alignment.CenterVertically)
                )
            }
        }, actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Home",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.HOME
                        }
                )
                Text(
                    text = "About",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.ABOUT
                        }
                )
                Text(
                    text = "Members",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.MEMBERS
                        }
                )
                Text(
                    text = "Uncorked Conversations",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.PODCASTS
                        }
                )
                Text(
                    text = "Events",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.EVENTS
                        }
                )
                Text(
                    text = "Our Wine",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedPageState.value = WDWPages.WINE
                        }
                )
            }
        })
    }
}

@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
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
                            modifier = Modifier.clickable {

                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.tiktok_logo_96),
                            contentDescription = "Tiktok logo",
                            modifier = Modifier.clickable {

                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.yt_logo_96),
                            contentDescription = "Youtube logo",
                            modifier = Modifier.clickable {

                            }
                        )
                    }
                }
                FooterColumn(
                    title = "Contact",
                    items = listOf("info@winesocialclub.com", "+1 234 567 890")
                )
                FooterColumn(
                    title = "Resources",
                    items = listOf("Privacy Policy", "Terms of Service"),
                    isLinks = true
                )
            }
        }
    }
    }

@Composable
fun FooterColumn(
    title: String,
    items: List<String>,
    isLinks: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
        )
        items.forEach { item ->
            if (isLinks) {
                Text(
                    text = item
                )
            } else {
                Text(
                    text = item
                )
            }
        }
    }
}