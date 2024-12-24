package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Home(
) {
    val selectedPageState = remember {
        mutableStateOf(WDWPages.HOME)
    }
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
            modifier =
            Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopNavBar(
                    selectedPageState = selectedPageState
                )
                Box(
                    modifier = Modifier.weight(1f)
                ){
                    when (selectedPageState.value) {
                        WDWPages.HOME -> {
                            PageBody (
                                content = {
                                    MainContent()
                                }
                            )
                        }
                        WDWPages.ABOUT -> {
                            PageBody(
                                content = {
                                    AboutPage()
                                }
                            )
                        }
                        WDWPages.MEMBERS -> {
                            PageBody(
                                content = {
                                    MembersPage()
                                }
                            )
                        }
                        WDWPages.PODCASTS -> {
                            PageBody(
                                content = {
                                    PodcastsPage()
                                }
                            )
                        }
                        WDWPages.EVENTS -> {
                            PageBody(
                                content = {
                                    EventsPage()
                                }
                            )
                        }
                        WDWPages.WINE -> {
                            PageBody(
                                content = {
                                    WinePage()
                                }
                            )
                        }
                        else -> {
                            // Nothing to do.
                        }
                    }
                }
                Footer()
            }
        }
    }
}

@Composable
fun PageBody(
    content: @Composable () -> Unit
) {
    content()
}

@Composable
fun HeroSection() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(50.dp, 40.dp)
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Welcome to the Wine Down Wednesday Social Club",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text(
                text = "Connecting wine enthusiasts from around Atlanta and the world.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainContent() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        HeroSection()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center

        ) {
            CardSection(
                title = "Upcoming Events",
                imageRes = null,
                contentTitle = "Wine Tasting Extravaganza - March 20th",
                contentText = "Join us for an evening of exquisite wines and delightful company."
            )

            Spacer(modifier = Modifier.width(16.dp))

            CardSection(
                title = "Featured Wines",
                imageRes = null,
                contentTitle = "Château Margaux 2015",
                contentText = "A rich and complex wine, perfect for any occasion."
            )

            Spacer(modifier = Modifier.width(16.dp))

            CardSection(
                title = "Member Spotlight",
                imageRes = null,
                contentTitle = "Jessica Parker",
                contentText = "\"Being a part of this club has enriched my wine journey immensely.\""
            )
        }
    }
}

@Composable
fun CardSection(
    title: String,
    imageRes: Painter?,
    contentTitle: String,
    contentText: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(min = 200.dp).height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (imageRes != null) {
                Image(
                    painter = imageRes,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = contentTitle, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = contentText)
        }
    }
}

enum class WDWPages {
    HOME,
    ABOUT,
    MEMBERS,
    PODCASTS,
    EVENTS,
    WINE
}