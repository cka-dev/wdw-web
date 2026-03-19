package net.winedownwednesday.web.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.utils.toEventDisplayDate
import net.winedownwednesday.web.viewmodels.HomePageViewModel
import org.koin.compose.koinInject
import kotlin.math.abs


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePage(
    sizeInfo: WindowSizeInfo,
    modifier: Modifier = Modifier
) {
    val viewModel: HomePageViewModel = koinInject()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val featuredWines by viewModel.featuredWines.collectAsState()
    val highlightedMember by viewModel.highlightedMember.collectAsState()
    val eventsLoaded by viewModel.eventsLoaded.collectAsState()
    val campaignName by viewModel.campaignName.collectAsState()
    val campaignDescription by viewModel.campaignDescription.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    val maxListSize = maxOf(upcomingEvents.size, featuredWines.size)
    LaunchedEffect(maxListSize) {
        if (maxListSize >= 1) {
            delay(1800L) // wait for all cards to finish sliding in (~1650ms total)
            while (true) {
                delay(5000L)
                currentIndex = (currentIndex + 1) % maxListSize
            }
        }
    }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var selectedWine by remember { mutableStateOf<Wine?>(null) }

    val verticalPadding = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> 20.dp
        WidthClass.Medium   -> 30.dp
        else                -> 50.dp
    }

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
                val fullTitle = "Welcome to the Wine Down Wednesday Social Club"
                var displayedTitle by remember { mutableStateOf("") }
                var subtitleVisible by remember { mutableStateOf(false) }
                val subtitleAlpha by animateFloatAsState(
                    targetValue  = if (subtitleVisible) 1f else 0f,
                    animationSpec = tween(700, easing = EaseOut),
                    label        = "subtitleFade"
                )
                LaunchedEffect(Unit) {
                    for (i in 1..fullTitle.length) {
                        delay(38L)
                        displayedTitle = fullTitle.take(i)
                    }
                    subtitleVisible = true
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = displayedTitle,
                        fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connecting wine enthusiasts from around Atlanta and the world.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                    )
                }
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
                    SlideInCard(delayMs = 200) {
                        AutoScrollingEventDisplay(
                            events = upcomingEvents,
                            currentIndex = currentIndex,
                            sizeInfo = sizeInfo,
                            onEventDetailsClick = { event -> selectedEvent = event },
                            isLoaded = eventsLoaded
                        )
                    }
                    SlideInCard(delayMs = 400) {
                        AutoScrollingWineListHorizontal(
                            title = campaignName,
                            description = campaignDescription,
                            wines = featuredWines,
                            currentIndex = currentIndex,
                            sizeInfo = sizeInfo,
                            onWineDetailsClick = { wine -> selectedWine = wine }
                        )
                    }
                    SlideInCard(delayMs = 650) {
                        MemberSpotlightCard(
                            member = highlightedMember,
                            sizeInfo = sizeInfo
                        )
                    }
                }
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            isCompactScreen = sizeInfo.useCompactNav,
            onDismiss = { selectedEvent = null }
        )
    }
    selectedWine?.let { wine ->
        WineDetailsDialog(
            wine = wine,
            isCompactScreen = sizeInfo.useCompactNav,
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
    sizeInfo: WindowSizeInfo,
    isLoaded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (events.size <= 1) {
        SingleEventOrEmptyHorizontal(title, events, onEventDetailsClick, sizeInfo, isLoaded, modifier)
        return
    }
    val transition = updateTransition(targetState = currentIndex, label = "eventSlide")
    val cardWidthMod = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> Modifier.fillMaxWidth()
        WidthClass.Medium   -> Modifier.fillMaxWidth(0.8f)
        else                -> Modifier.fillMaxWidth(0.3f)
    }
    val cardHeightMod = when (sizeInfo.widthClass) {
        WidthClass.Compact -> Modifier.heightIn(min = 350.dp)
        WidthClass.Medium  -> Modifier.height(420.dp)
        else               -> Modifier.height(500.dp)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(cardWidthMod)
            .then(cardHeightMod)
            .hoverScale(),
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
    description: String = "",
    wines: List<Wine>,
    onWineDetailsClick: (Wine) -> Unit = {},
    currentIndex: Int,
    sizeInfo: WindowSizeInfo,
    modifier: Modifier = Modifier
) {
    if (wines.size <= 1) {
        SingleWineOrEmptyHorizontal(title, description, wines, onWineDetailsClick, sizeInfo, modifier)
        return
    }
    val transition = updateTransition(targetState = currentIndex, label = "wineSlide")
    val wineCardWidthMod = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> Modifier.fillMaxWidth()
        WidthClass.Medium   -> Modifier.fillMaxWidth(0.8f)
        else                -> Modifier.fillMaxWidth(0.3f)
    }
    val wineCardHeightMod = when (sizeInfo.widthClass) {
        WidthClass.Compact -> Modifier.heightIn(min = 350.dp)
        WidthClass.Medium  -> Modifier.height(420.dp)
        else               -> Modifier.height(500.dp)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(wineCardWidthMod)
            .then(wineCardHeightMod)
            .hoverScale(),
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
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
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
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
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
    sizeInfo: WindowSizeInfo,
    isLoaded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val seWidthMod = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> Modifier.fillMaxWidth()
        WidthClass.Medium   -> Modifier.fillMaxWidth(0.8f)
        else                -> Modifier.fillMaxWidth(0.3f)
    }
    val seHeightMod = when (sizeInfo.widthClass) {
        WidthClass.Compact -> Modifier.heightIn(min = 350.dp)
        WidthClass.Medium  -> Modifier.height(420.dp)
        else               -> Modifier.height(500.dp)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(seWidthMod)
            .then(seHeightMod),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                if (isLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No Upcoming Events",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stay tuned for our next gathering!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Fetching upcoming events",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    LinearProgressBar()
                }
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
    description: String,
    wines: List<Wine>,
    onWineDetailsClick: (Wine) -> Unit,
    sizeInfo: WindowSizeInfo,
    modifier: Modifier = Modifier
) {
    val swWidthMod = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> Modifier.fillMaxWidth()
        WidthClass.Medium   -> Modifier.fillMaxWidth(0.8f)
        else                -> Modifier.fillMaxWidth(0.3f)
    }
    val swHeightMod = when (sizeInfo.widthClass) {
        WidthClass.Compact -> Modifier.heightIn(min = 350.dp)
        WidthClass.Medium  -> Modifier.height(420.dp)
        else               -> Modifier.height(500.dp)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(swWidthMod)
            .then(swHeightMod),
        elevation = cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
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
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                text = "Date: ${event.date.toEventDisplayDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )

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
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = wine.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                    text = "Date: ${event.date.toEventDisplayDate()} ${event.time.orEmpty()}",
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!wine.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = wine.imageUrl,
                        contentDescription = "${wine.name} image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
    sizeInfo: WindowSizeInfo,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    val spotlightWidthMod = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> Modifier.fillMaxWidth()
        WidthClass.Medium   -> Modifier.fillMaxWidth(0.8f)
        else                -> Modifier.fillMaxWidth(0.3f)
    }
    val spotlightHeightMod = when (sizeInfo.widthClass) {
        WidthClass.Compact -> Modifier.heightIn(min = 350.dp)
        WidthClass.Medium  -> Modifier.height(420.dp)
        else               -> Modifier.height(500.dp)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .hoverScale()
            .then(spotlightWidthMod)
            .then(spotlightHeightMod)
            .then(
                if (member != null) Modifier.clickable { showDetails = true }
                else Modifier
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
                        // Reason badge
                        val isBirthdayMonth = if (featuredMember.spotlightReason != null) {
                            featuredMember.spotlightReason == "birthday"
                        } else {
                            featuredMember.birthday.isNotBlank() && isBirthdayInCurrentMonth(featuredMember.birthday)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBirthdayMonth) Color(0xFF4A3000) else Color(0xFF1A3A1A),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = if (isBirthdayMonth) "🎂 Birthday Spotlight" else "⭐ Featured Member",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBirthdayMonth) Color(0xFFFFD54F) else Color(0xFF81C784),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

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
                        if (isBirthdayMonth) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎉 Birthday: ${featuredMember.birthday}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD54F)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap for more details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    if (showDetails && member != null) {
        MemberDetailsDialog(
            member = member,
            isCompactScreen = sizeInfo.useCompactNav,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
private fun MemberDetailsDialog(
    member: Member,
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                AsyncImage(
                    model = member.profilePictureUrl,
                    contentDescription = "${member.name}'s profile picture",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"Wine is about bringing people together, sharing moments, and building community. That's what WDW is all about.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (member.role.isNotBlank()) {
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (member.profession.isNotBlank()) {
                    Text(
                        text = "💼 ${member.profession}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!member.company.isNullOrBlank()) {
                    Text(
                        text = "🏢 ${member.company}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (member.birthday.isNotBlank()) {
                    Text(
                        text = "🎂 ${member.birthday}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (member.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Interests",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = member.interests.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (member.favoriteWines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Favorite Wines",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = member.favoriteWines.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
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

private fun isBirthdayInCurrentMonth(birthday: String): Boolean {
    val monthNames = mapOf(
        "january" to 1, "february" to 2, "march" to 3,
        "april" to 4, "may" to 5, "june" to 6,
        "july" to 7, "august" to 8, "september" to 9,
        "october" to 10, "november" to 11, "december" to 12
    )
    val currentMonth = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .monthNumber

    val trimmed = birthday.trim()
    // Try "MM/DD" format
    if (trimmed.contains("/")) {
        val parts = trimmed.split("/")
        if (parts.size >= 2) {
            return parts[0].toIntOrNull() == currentMonth
        }
    }
    // Try "Month Day" format
    val parts = trimmed.split("\\s+".toRegex())
    if (parts.isNotEmpty()) {
        val monthNum = monthNames[parts[0].lowercase()]
        if (monthNum != null) {
            return monthNum == currentMonth
        }
    }
    return false
}