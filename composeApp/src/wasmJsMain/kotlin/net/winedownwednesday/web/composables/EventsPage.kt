package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.dom.appendElement
import net.winedownwednesday.web.HtmlView
import net.winedownwednesday.web.LocalLayerContainer
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.MediaItem
import net.winedownwednesday.web.data.MediaType
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import org.koin.compose.koinInject
import kotlin.contracts.ExperimentalContracts


@Composable
fun EventsPage(
    isCompactScreen: Boolean
) {
    val viewModel: EventsPageViewModel = koinInject()

    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val pastEvents by viewModel.pastEvents.collectAsState()

    var showUpcoming by remember { mutableStateOf(true) }

    val selectedEvent by viewModel.selectedEvent.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Row {
                Button(
                    onClick = { showUpcoming = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showUpcoming) Color(0xFFFF7F33) else Color(0xFF2A2A2A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Upcoming Events")
                }

                Button(
                    onClick = { showUpcoming = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showUpcoming) Color(0xFFFF7F33) else Color(0xFF2A2A2A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Past Events")
                }
            }
        }

        val eventsToDisplay = if (showUpcoming) upcomingEvents else pastEvents
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (!eventsToDisplay.isNullOrEmpty()) {
                items(eventsToDisplay) { event ->
                    EventCard(
                        event = event,
                        onEventSelectedChange = {
                            viewModel.setSelectedEvent(it)
                        }
                    )
                }
            }
        }
    }

    if (selectedEvent != null) {
        if (isCompactScreen){
            CompactScreenEventDetailPopup(
                event = selectedEvent!!,
                onDismissRequest = {
                    viewModel.setSelectedEvent(null)
                }
            )
        } else {
            LargeScreenEventDetailPopup(
                selectedEvent = selectedEvent!!,
                onDismissRequest = {
                    viewModel.setSelectedEvent(null)
                })
        }
    }
}

@Composable
fun EventCard(
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
            containerColor = Color(0xFF2D2D2D)
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
                modifier = Modifier.fillMaxWidth()
            )
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

@Composable
fun LargeScreenEventDetailPopup(
    selectedEvent: Event,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismissRequest)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.4f)
                .padding(16.dp)
        ) {
            EventDetailContent(event = selectedEvent, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun CompactScreenEventDetailPopup(
    event: Event,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismissRequest)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            EventDetailContent(event = event, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun EventDetailContent(
    event: Event,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = rememberAsyncImagePainter(
                model = event.imageUrl,
//                placeholder = painterResource(R.drawable.placeholder),
//                error = painterResource(R.drawable.error_placeholder)
            ),
            contentDescription = "${event.name} Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        EventDetailRow(
            label = "Date",
            value = formatDate(stringToDate(event.date))
        )
        if (!event.time.isNullOrBlank()) {
            EventDetailRow(label = "Time", value = event.time)
        }
        EventDetailRow(label = "Event Type", value = event.eventType.toString().replace('_', ' '))
        EventDetailRow(label = "Location", value = event.location)
        EventDetailRow(label = "Description", value = event.description)

        if (event.wineSelection != null) {
            EventDetailRow(label = "Wine Selection", value = event.wineSelection)
        }
        if (event.wineSelector != null) {
            EventDetailRow(label = "Wine Selector", value = event.wineSelector)
        }
        if (event.additionalInfo != null) {
            EventDetailRow(label = "Additional Info", value = event.additionalInfo)
        }

        if (event.registrationLink != null && stringToDate(event.date) > Clock.System.now()
                .toLocalDateTime(
                    TimeZone.currentSystemDefault()
                ).date
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    window.open(event.registrationLink, "_blank")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Register")
            }
        }

        if (event.gallery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            MediaGallery(mediaItems = event.gallery)
        }
    }
}

@Composable
fun EventDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun painterResourcePlaceholder(): Painter {
    return ColorPainter(Color.DarkGray)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaGallery(
    mediaItems: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    var showGallery by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mediaItems.forEachIndexed { index, mediaItem ->
            MediaItemThumbnail(
                mediaItem = mediaItem,
                modifier = Modifier.size(100.dp),
                onClick = {
                    showGallery = true
                    selectedIndex = index
                }
            )
        }
    }

    if (showGallery) {
        GalleryViewer(
            mediaItems = mediaItems,
            initialPage = selectedIndex,
            onDismissRequest = { showGallery = false }
        )
    }
}

@Composable
fun GalleryViewer(
    mediaItems: List<MediaItem>,
    initialPage: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { mediaItems.size }
    )

    CompositionLocalProvider(LocalLayerContainer provides document.body!!) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card(

            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val mediaItem = mediaItems[page]
                        when {
                            mediaItem.type == MediaType.IMAGE && !mediaItem.contentUrl.isNullOrEmpty() -> {
                                AsyncImage(
                                    model = mediaItem.contentUrl,
                                    contentDescription = "Image ${mediaItem.contentUrl}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            mediaItem.type == MediaType.VIDEO && !mediaItem.contentUrl.isNullOrEmpty() -> {
                                KmpVideoPlayer(mediaItem.contentUrl)
                            }

                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Unable to load media", color = Color.White)
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    CustomPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

}

@Composable
fun CustomPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val pageCount = pagerState.pageCount

    val currentPage = pagerState.currentPage

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        repeat(pageCount) { index ->
            val isSelected = (index == currentPage)
            Box(
                modifier = Modifier
                    .size(if (isSelected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.Gray)
            )
        }
    }
}

@Composable
fun MediaItemThumbnail(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        if (!mediaItem.thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = mediaItem.thumbnailUrl,
                contentDescription = "${mediaItem.type}' event picture or video"
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray)
            )
        }

        if (mediaItem.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play Video",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }
    }
}

@OptIn(ExperimentalContracts::class)
@Composable
fun KmpVideoPlayer(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HtmlView(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            factory = {
                val video = createElement("video")
                video.setAttribute("controls", "")
                video.setAttribute("preload", "auto")
                video.setAttribute("data-setup", "{}")
                video.appendElement("source") {
                    setAttribute("src", url)
                    setAttribute("type", "video/mp4")
                }
                video
            }
        )
    }
}


fun stringToDate(date: String): LocalDate {
    val (year, month, day) = date.split(", ").map { it.toInt() }
    return LocalDate(year, month, day)
}

private fun formatDate(date: LocalDate): String {
    val month = when (date.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }

    return "$month ${date.dayOfMonth}, ${date.year}"
}