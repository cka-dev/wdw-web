package net.winedownwednesday.web.composables

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.viewmodels.HomePageViewModel
import org.koin.compose.koinInject
import kotlin.math.abs


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePage(
    isCompactScreen: Boolean
) {
    val viewModel: HomePageViewModel = koinInject()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val featuredWines by viewModel.featuredWines.collectAsState()
    val highlightedMember by viewModel.highlightedMember.collectAsState()
    var sharedIndex by remember { mutableIntStateOf(0) }
    val maxListSize = maxOf(upcomingEvents.size, featuredWines.size)

    LaunchedEffect(maxListSize) {
        if (maxListSize >= 1) {
            while (true) {
                delay(5000L)
                sharedIndex = (sharedIndex + 1) % maxListSize
            }
        }
    }

    val padding = if (isCompactScreen) {
        20.dp
    } else {
        50.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
            item {
                Spacer(modifier = Modifier.height(padding))
            }
            item {
                FlowRow (
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    AutoScrollingEventDisplay(
                        events = upcomingEvents,
                        sharedIndex = sharedIndex,
                        isCompactScreen = isCompactScreen
                    )

                    AutoScrollingWineListHorizontal(
                        wines = featuredWines,
                        sharedIndex = sharedIndex,
                        isCompactScreen = isCompactScreen,
                    )

                    MemberSpotlightCard(
                        title = "Member Spotlight",
                        member = highlightedMember,
                        isCompactScreen = isCompactScreen
                    )
                }
            }
        }


    }
}

@Composable
fun MemberSpotlightCard(
    title: String = "Member Spotlight",
    member: Member?,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (!isCompactScreen) Modifier.fillMaxWidth(0.3f) else Modifier.fillMaxWidth())
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
                Text("Fetching featured member", color = Color.LightGray)
                LinearProgressBar()
            } else {
                Card(
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    AsyncImage(
                        model = member.profilePictureUrl,
                        contentDescription = "${member.name}' s profile picture"
                    )
                }
                Column {
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
fun WineCard(
    wine: Wine,
    onWineSelectedChange: (Wine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
fun AutoScrollingEventDisplay(
    title: String = "Upcoming Events",
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit = {},
    sharedIndex: Int,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    if (events.size <= 1) {
        SingleEventOrEmptyHorizontal(title, events, onEventSelectedChange)
        return
    }

    val transition = updateTransition(targetState = sharedIndex, label = "slideTransition")

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (!isCompactScreen) Modifier.fillMaxWidth(0.3f) else Modifier.fillMaxWidth())
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                events.forEachIndexed { index, event ->
                    val offset by transition.animateFloat(
                        label = "slideAnimation",
                        transitionSpec = {
                            tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        }
                    ) { target ->
                        val distance = (index - target + events.size) % events.size
                        when (distance) {
                            0 -> 0f
                            1 -> 1f
                            events.size - 1 -> -1f
                            else -> 2f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offset * size.width
                                alpha = if (abs(offset) <= 1f) 1 - abs(offset) else 0f
                            }
                    ) {
                        HomePageEventCard(
                            event = event,
                            onEventSelectedChange = onEventSelectedChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleEventOrEmptyHorizontal(
    title: String,
    events: List<Event>,
    onEventSelectedChange: (Event) -> Unit,
    modifier: Modifier = Modifier
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
                Text("Fetching upcoming events", color = Color.LightGray)
                LinearProgressBar()
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
    sharedIndex: Int,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    if (wines.size <= 1) {
        SingleWineOrEmptyHorizontal(title, wines, onWineSelectedChange)
        return
    }

    val transition = updateTransition(targetState = sharedIndex, label = "slideTransition")

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (!isCompactScreen) Modifier.fillMaxWidth(0.3f) else Modifier.fillMaxWidth())
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                wines.forEachIndexed { index, wine ->
                    val offset by transition.animateFloat(
                        label = "slideAnimation",
                        transitionSpec = {
                            tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        }
                    ) { target ->
                        val distance = (index - target + wines.size) % wines.size
                        when (distance) {
                            0 -> 0f
                            1 -> 1f
                            wines.size - 1 -> -1f
                            else -> 2f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offset * size.width
                                alpha = if (abs(offset) <= 1f) 1 - abs(offset) else 0f
                            }
                    ) {
                        WineCard(
                            wine = wine,
                            onWineSelectedChange = onWineSelectedChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                Text("Fetching wines", color = Color.LightGray)
                LinearProgressBar()
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
                        // TODO: show RSVP dialog
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