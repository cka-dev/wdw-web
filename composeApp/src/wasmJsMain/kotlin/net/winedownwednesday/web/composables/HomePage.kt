package net.winedownwednesday.web.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: HomePageViewModel = koinInject()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val featuredWines by viewModel.featuredWines.collectAsState()
    val highlightedMember by viewModel.highlightedMember.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    val maxListSize = maxOf(upcomingEvents.size, featuredWines.size)
    LaunchedEffect(maxListSize) {
        if (maxListSize >= 1) {
            while (true) {
                delay(5000L)
                currentIndex = (currentIndex + 1) % maxListSize
            }
        }
    }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var selectedWine by remember { mutableStateOf<Wine?>(null) }

    val verticalPadding = if (isCompactScreen) 20.dp else 50.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Welcome to the Wine Down Wednesday Social Club",
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Text(
                    text = "Connecting wine enthusiasts from around Atlanta and the world.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            item {
                Spacer(modifier = Modifier.height(verticalPadding))
            }
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AutoScrollingEventDisplay(
                        events = upcomingEvents,
                        currentIndex = currentIndex,
                        isCompactScreen = isCompactScreen,
                        onEventDetailsClick = { event -> selectedEvent = event }
                    )
                    AutoScrollingWineListHorizontal(
                        wines = featuredWines,
                        currentIndex = currentIndex,
                        isCompactScreen = isCompactScreen,
                        onWineDetailsClick = { wine -> selectedWine = wine }
                    )
                    MemberSpotlightCard(
                        member = highlightedMember,
                        isCompactScreen = isCompactScreen
                    )
                }
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            isCompactScreen = isCompactScreen,
            onDismiss = { selectedEvent = null }
        )
    }
    selectedWine?.let { wine ->
        WineDetailsDialog(
            wine = wine,
            isCompactScreen = isCompactScreen,
            onDismiss = { selectedWine = null }
        )
    }
}

@Composable
fun AutoScrollingEventDisplay(
    title: String = "Upcoming Events",
    events: List<Event>,
    onEventDetailsClick: (Event) -> Unit = {},
    currentIndex: Int,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    if (events.size <= 1) {
        SingleEventOrEmptyHorizontal(title, events, onEventDetailsClick)
        return
    }
    val transition = updateTransition(targetState = currentIndex, label = "eventSlide")
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(
                if (!isCompactScreen) Modifier.fillMaxWidth(0.3f) else Modifier.fillMaxWidth()
            )
            .then(
                if (!isCompactScreen) Modifier.height(500.dp) else Modifier.heightIn(min = 350.dp)
            ),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                events.forEachIndexed { index, event ->
                    val offset by transition.animateFloat(
                        transitionSpec = {
                            tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        },
                        label = "eventOffset"
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
                                alpha = if (abs(offset) <= 1f) 1f - abs(offset) else 0f
                            }
                    ) {
                        HomePageEventCard(
                            event = event,
                            onDetailsClick = { onEventDetailsClick(event) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutoScrollingWineListHorizontal(
    title: String = "Featured Wines",
    wines: List<Wine>,
    onWineDetailsClick: (Wine) -> Unit = {},
    currentIndex: Int,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    if (wines.size <= 1) {
        SingleWineOrEmptyHorizontal(title, wines, onWineDetailsClick)
        return
    }
    val transition = updateTransition(targetState = currentIndex, label = "wineSlide")
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (!isCompactScreen) Modifier.fillMaxWidth(0.3f) else Modifier.fillMaxWidth())
            .then(if (!isCompactScreen) Modifier.height(500.dp) else Modifier.heightIn(min = 350.dp)),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                wines.forEachIndexed { index, wine ->
                    val offset by transition.animateFloat(
                        transitionSpec = {
                            tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        },
                        label = "wineOffset"
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
                                alpha = if (abs(offset) <= 1f) 1f - abs(offset) else 0f
                            }
                    ) {
                        WineCard(
                            wine = wine,
                            onDetailsClick = { onWineDetailsClick(wine) },
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
    onEventDetailsClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text(
                    text = "Fetching upcoming events",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                LinearProgressBar()
            } else {
                HomePageEventCard(
                    event = events.first(),
                    onDetailsClick = { onEventDetailsClick(events.first()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SingleWineOrEmptyHorizontal(
    title: String,
    wines: List<Wine>,
    onWineDetailsClick: (Wine) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(600.dp)
            .padding(32.dp),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (wines.isEmpty()) {
                Text(
                    text = "Fetching wines",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                LinearProgressBar()
            } else {
                WineCard(
                    wine = wines.first(),
                    onDetailsClick = { onWineDetailsClick(wines.first()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HomePageEventCard(
    event: Event,
    onDetailsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDetailsClick() },
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
        elevation = cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location: ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "Date: ${event.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDetailsClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
fun WineCard(
    wine: Wine,
    onDetailsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 350.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDetailsClick() },
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
        elevation = cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = wine.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = wine.technicalDetails,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${wine.type} - ${wine.year} - ${wine.country}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Transparent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDetailsClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Learn More")
                }
            }
        }
    }
}


@Composable
fun EventDetailsDialog(
    event: Event,
    isCompactScreen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .then(
                    if (!isCompactScreen) Modifier.fillMaxWidth(0.9f)
                    else Modifier.fillMaxWidth()
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = "${event.name} image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date: ${event.date} ${event.time.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Location: ${event.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                event.additionalInfo?.let { info ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun WineDetailsDialog(wine: Wine, isCompactScreen: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .then(
                    if (!isCompactScreen) Modifier.fillMaxWidth(0.9f)
                    else Modifier.fillMaxWidth()
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AsyncImage(
                    model = wine.imageUrl,
                    contentDescription = "${wine.name} image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = wine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${wine.type} - ${wine.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Country: ${wine.country}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Region: ${wine.region}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = wine.technicalDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                wine.whyWeLovedIt?.let { why ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = why,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun MemberSpotlightCard(
    member: Member?,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(
                if (!isCompactScreen) Modifier.fillMaxWidth(0.3f)
                else Modifier.fillMaxWidth()
            )
            .then(
                if (!isCompactScreen) Modifier.height(500.dp)
                else Modifier.heightIn(min = 350.dp)
            ),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Text(
                text = "Member Spotlight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = member) { featuredMember ->
                if (featuredMember == null) {
                    Column {
                        Text(
                            text = "Fetching featured member",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        LinearProgressBar()
                    }
                } else {
                    Column {
                        Card(
                            modifier = Modifier.padding(bottom = 16.dp),
                            elevation = cardElevation(defaultElevation = 8.dp)
                        ) {
                            AsyncImage(
                                model = featuredMember.profilePictureUrl,
                                contentDescription = "${featuredMember.name}'s profile picture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                            )
                        }
                        Text(
                            text = featuredMember.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = featuredMember.profession,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
//                            text = "\"${featuredMember.quote}\"",
                            text = "\"Wine Down Wednesday is a great way to meet new people and" +
                                    "try new wines. I always look forward to our events!. I " +
                                    "also love the community engagement and the opportunity to " +
                                    "learn more about wine.\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
