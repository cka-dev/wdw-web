package net.winedownwednesday.web.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.viewmodels.HomePageViewModel
import org.koin.compose.koinInject

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
                            // Do nothing.
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainContent() {
    val viewModel: HomePageViewModel = koinInject()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val featuredWines by viewModel.featuredWines.collectAsState()
    val highlightedMember by viewModel.highlightedMember.collectAsState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        HeroSection()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                FlowRow (
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    AutoScrollingEventListHorizontal(
                        events = upcomingEvents,
                        scrollInterval = 3000L
                    )

                    AutoScrollingWineListHorizontal(
                        wines = featuredWines,
                        scrollInterval = 3000L
                    )

                    MemberSpotlightCard(
                        title = "Member Spotlight",
                        member = highlightedMember
                    )
                }
            }
        }
    }
}

@Composable
fun MemberSpotlightCard(
    title: String = "Member Spotlight",
    member: Member?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (member == null) {
                Text("No member selected", color = Color.LightGray)
            } else {
                Card (
                    elevation = CardDefaults.cardElevation(8.dp)
                ){
                    AsyncImage(
                        model = member.profilePictureUrl,
                        contentDescription = "${member.name}' s profile picture"
                    )
                }
                Column{
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = member.profession,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun AutoScrollingEventList(
    title: String = "Upcoming Events",
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit = {},
    scrollInterval: Long = 3000L,
    modifier: Modifier = Modifier
) {
    if (events.size <= 1) {
        SingleEventOrEmpty(title, events, onEventSelectedChange)
        return
    }

    val listState = rememberLazyListState()
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(events) {
        while (true) {
            delay(scrollInterval)
            currentIndex = (currentIndex + 1) % events.size
            listState.animateScrollToItem(currentIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(state = listState) {
                items(events) { event ->
                    HomePageEventCard(
                        event = event, onEventSelectedChange = onEventSelectedChange)
                }
            }
        }
    }
}

@Composable
private fun SingleEventOrEmpty(
    title: String,
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text("No upcoming events", color = Color.LightGray)
            } else {
                HomePageEventCard(
                    event = events.first(),
                    onEventSelectedChange = onEventSelectedChange
                )
            }
        }
    }
}

@Composable
fun WineCard(
    wine: Wine,
    onWineSelectedChange: (Wine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(1f)
            .heightIn(min = 350.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onWineSelectedChange(wine)
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
        ) {
            Text(
                text = "${wine.name}                      ",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = wine.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = wine.technicalDetails,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${wine.type} - ${wine.year} - ${wine.country}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun AutoScrollingWineList(
    title: String = "Featured Wines",
    wines: List<Wine>,
    onWineSelectedChange: (Wine) -> Unit = {},
    scrollInterval: Long = 3000L,
    modifier: Modifier = Modifier
) {
    if (wines.size <= 1) {
        SingleWineOrEmpty(title, wines, onWineSelectedChange)
        return
    }

    val listState = rememberLazyListState()
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(wines) {
        while (true) {
            delay(scrollInterval)
            currentIndex = (currentIndex + 1) % wines.size
            listState.animateScrollToItem(currentIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(state = listState) {
                items(wines) { wine ->
                    WineCard(wine = wine, onWineSelectedChange = onWineSelectedChange)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SingleWineOrEmpty(
    title: String,
    wines: List<Wine>,
    onWineSelectedChange: (Wine) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(min = 200.dp)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (wines.isEmpty()) {
                Text("No featured wines", color = Color.LightGray)
            } else {
                WineCard(wine = wines.first(), onWineSelectedChange = onWineSelectedChange)
            }
        }
    }
}

//Horizontal Scrolling
@Composable
fun AutoScrollingEventListHorizontal(
    title: String = "Upcoming Events",
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit = {},
    scrollInterval: Long = 3000L,
    modifier: Modifier = Modifier
) {
    if (events.size <= 1) {
        SingleEventOrEmptyHorizontal(title, events, onEventSelectedChange)
        return
    }

    val listState = rememberLazyListState()
    var currentIndex by remember { mutableStateOf(0) }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(events) {
        while (true) {
            delay(scrollInterval)
            currentIndex = (currentIndex + 1) % events.size
            listState.animateScrollToItem(currentIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(events) { event ->
                    HomePageEventCard(
                        event = event,
                        onEventSelectedChange = onEventSelectedChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                }
            }
        }
    }
}

@Composable
private fun SingleEventOrEmptyHorizontal(
    title: String,
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text("No upcoming events", color = Color.LightGray)
            } else {
                HomePageEventCard(
                    event = events.first(),
                    onEventSelectedChange = onEventSelectedChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AutoScrollingWineListHorizontal(
    title: String = "Featured Wines",
    wines: List<Wine>,
    onWineSelectedChange: (Wine) -> Unit = {},
    scrollInterval: Long = 3000L,
    modifier: Modifier = Modifier
) {
    if (wines.size <= 1) {
        SingleWineOrEmptyHorizontal(title, wines, onWineSelectedChange)
        return
    }

    val listState = rememberLazyListState()
    var currentIndex by remember { mutableStateOf(0) }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    LaunchedEffect(wines) {
        while (true) {
            delay(scrollInterval)
            currentIndex = (currentIndex + 1) % wines.size
            listState.animateScrollToItem(currentIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(wines) { wine ->
                    WineCard(
                        wine = wine,
                        onWineSelectedChange = onWineSelectedChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    )
                    Spacer(modifier = Modifier.width(180.dp))
                }
            }
        }
    }
}

@Composable
private fun SingleWineOrEmptyHorizontal(
    title: String,
    wines: List<Wine>,
    onWineSelectedChange: (Wine) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp)
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (wines.isEmpty()) {
                Text("No wines", color = Color.LightGray)
            } else {
                WineCard(
                    wine = wines.first(),
                    onWineSelectedChange = onWineSelectedChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HomePageEventCard(
    event: Event,
    onEventSelectedChange: (Event) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onEventSelectedChange(event)
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Location: ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
            Text(
                text = "Date: ${event.date}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth())
            {
                Button(
                    onClick = {
                        event.registrationLink?.let {
                            window.open(it, "_blank")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RSVP")
                }
            }
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