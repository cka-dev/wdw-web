package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.dom.appendElement
import net.winedownwednesday.web.HtmlView
import net.winedownwednesday.web.vibrate
import net.winedownwednesday.web.LocalLayerContainer
import net.winedownwednesday.web.NanpVisualTransformation
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.MediaItem
import net.winedownwednesday.web.data.MediaType
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.utils.toDisplayString
import net.winedownwednesday.web.utils.toEventDisplayDate
import net.winedownwednesday.web.utils.toEventLocalDate
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.EventSuggestion
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.placeholder


@Composable
fun EventsPage(
    sizeInfo: WindowSizeInfo,
    authPageViewModel: AuthPageViewModel,
    uiState: LoginUIState
) {
    val viewModel: EventsPageViewModel = koinInject()

    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val pastEvents by viewModel.pastEvents.collectAsState()
    val pendingEventName by viewModel.pendingEventName.collectAsState()

    var showUpcoming by remember { mutableStateOf(true) }

    val selectedEvent by viewModel.selectedEvent.collectAsState()

    var eventForRsvp by remember { mutableStateOf<Event?>(null) }

    // Auto-select an event by name when navigated from a Vino card
    androidx.compose.runtime.LaunchedEffect(upcomingEvents, pastEvents, pendingEventName) {
        val name = pendingEventName ?: return@LaunchedEffect
        val allEventsFlat = (upcomingEvents ?: emptyList()) + (pastEvents ?: emptyList())
        val match = allEventsFlat.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (match != null) {
            // Switch to the right tab so the user sees the card highlighted
            showUpcoming = (upcomingEvents ?: emptyList()).contains(match)
            viewModel.setSelectedEvent(match)
            viewModel.clearPendingEventName()
        }
    }

    val isLoggedIn = uiState is LoginUIState.Authenticated
    val vinoSuggestions by viewModel.vinoEventSuggestions.collectAsState()
    val isFetchingEventRecs by viewModel.isFetchingEventRecs.collectAsState()

    // Fetch event suggestions once when user lands on Upcoming tab logged-in
    androidx.compose.runtime.LaunchedEffect(isLoggedIn, showUpcoming) {
        if (isLoggedIn && showUpcoming && vinoSuggestions.isEmpty()) {
            viewModel.fetchVinoEventSuggestions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
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
                color = MaterialTheme.colorScheme.onBackground
            )

        val upcomingColor by animateColorAsState(
            targetValue  = if (showUpcoming) Color(0xFFFF7F33) else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(durationMillis = 300),
            label        = "upcomingToggle"
        )
        val pastColor by animateColorAsState(
            targetValue  = if (!showUpcoming) Color(0xFFFF7F33) else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(durationMillis = 300),
            label        = "pastToggle"
        )

            Row {
                Button(
                    onClick = { showUpcoming = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = upcomingColor,
                        contentColor   = if (LocalIsDarkTheme.current) Color.White
                                         else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Upcoming Events")
                }

                Button(
                    onClick = { showUpcoming = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pastColor,
                        contentColor   = if (LocalIsDarkTheme.current) Color.White
                                         else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Past Events")
                }
            }
        }

        val eventsToDisplay = if (showUpcoming) upcomingEvents else pastEvents

        // ── Vino event suggestions banner (upcoming only, logged-in only) ────────
        if (showUpcoming && isLoggedIn && (vinoSuggestions.isNotEmpty() || isFetchingEventRecs)) {
            EventSuggestionBanner(
                suggestions = vinoSuggestions,
                isLoading = isFetchingEventRecs,
                onSuggestionClick = { name ->
                    val match = (upcomingEvents ?: emptyList())
                        .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    if (match != null) viewModel.setSelectedEvent(match)
                }
            )
        }

        // Adaptive column min size grows with screen width
        val colMinSize = when (sizeInfo.widthClass) {
            WidthClass.Compact  -> 280.dp
            WidthClass.Medium   -> 300.dp
            WidthClass.Expanded -> 340.dp
            WidthClass.Large    -> 380.dp
            WidthClass.XLarge   -> 420.dp
        }
        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = colMinSize),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (!eventsToDisplay.isNullOrEmpty()) {
                    itemsIndexed(eventsToDisplay) { index, event ->
                        GridItemReveal(index = index, animationKey = showUpcoming) {
                            EventCard(
                                event = event,
                                onEventSelectedChange = {
                                    viewModel.setSelectedEvent(it)
                                },
                                viewModel = viewModel,
                                showUpcoming = showUpcoming,
                                isCompactScreen = sizeInfo.useCompactNav,
                                uiState = uiState,
                                authPageViewModel = authPageViewModel,
                                onRsvpClick = {
                                    eventForRsvp = event
                                },
                                onDismissRequest = {
                                    eventForRsvp = null
                                },
                                eventForRsvp = eventForRsvp,
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter  = rememberScrollbarAdapter(gridState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                style = wdwScrollbarStyle()
            )
        }
    }

    if (selectedEvent != null) {
        if (sizeInfo.useCompactNav) {
            CompactScreenEventDetailPopup(
                event = selectedEvent!!,
                onDismissRequest = { viewModel.setSelectedEvent(null) },
                onRsvpClick = { eventForRsvp = selectedEvent }
            )
        } else {
            LargeScreenEventDetailPopup(
                selectedEvent = selectedEvent!!,
                onDismissRequest = { viewModel.setSelectedEvent(null) },
                onRsvpClick = { eventForRsvp = selectedEvent }
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onEventSelectedChange: (Event) -> Unit = {},
    viewModel: EventsPageViewModel,
    authPageViewModel: AuthPageViewModel,
    showUpcoming: Boolean,
    isCompactScreen: Boolean,
    uiState: LoginUIState,
    onRsvpClick: () -> Unit,
    onDismissRequest: () -> Unit,
    eventForRsvp: Event?,
    modifier: Modifier = Modifier
) {

    val userProfileData by authPageViewModel.profileData.collectAsState()

    val isUserAuthenticated = (uiState is LoginUIState.Authenticated)

    val userHasRsvped = authPageViewModel.hasUserRsvped(event.id)

    val buttonLabel = when {
        userHasRsvped -> "Modify Your RSVP"
        else -> "RSVP"
    }

    val showSuccessToast = remember { mutableStateOf(false) }
    val showErrorToast = remember { mutableStateOf(false) }
    val showProgressBar = remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .hoverScale()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                hapticVibrate(HapticDuration.TICK, HapticCategory.DIALOGS)
                onEventSelectedChange(event)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 1,
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
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Location: ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Date: ${event.date.toEventDisplayDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            )
            {
                Button(
                    enabled = showUpcoming,
                    onClick = {
                        hapticVibrate(HapticDuration.MEDIUM, HapticCategory.ALERTS)
                        if (isUserAuthenticated) {
                            onRsvpClick()
                        } else {
                            window.alert("You need to register or log in in to RSVP")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = buttonLabel
                    )
                }
            }
        }
    }

    if (eventForRsvp != null && eventForRsvp.id == event.id) {
        RSVPComponent(
            onDismissRequest = { onDismissRequest() },
            event = event,
            existingRsvp = if (userHasRsvped) authPageViewModel.getRsvpForEvent(
                event.id
            ) else null,
            isCompactScreen = isCompactScreen,
            uiState = uiState,
            userProfileData = userProfileData,
            showProgressBar = showProgressBar.value,
            onSubmit = { rsvpRequest ->
                showProgressBar.value = true
                authPageViewModel.saveRsvpInProfile(rsvpRequest) { profileSaveSuccess ->
                    if (profileSaveSuccess) {
                        viewModel.addRsvpToEvent(rsvpRequest) { eventUpdateSuccess ->
                            showProgressBar.value = false
                            if (eventUpdateSuccess) {
                                hapticVibrate(HapticDuration.MEDIUM, HapticCategory.ALERTS)
                                showSuccessToast.value = true
                                coroutineScope.launch {
                                    delay(2000)
                                    showSuccessToast.value = false
                                }
                                onDismissRequest()
                            } else {
                                hapticVibrate(HapticDuration.HEAVY, HapticCategory.ALERTS)
                                showErrorToast.value = true
                                coroutineScope.launch {
                                    delay(2000)
                                    showErrorToast.value = false
                                }
                            }
                        }
                    } else {
                        showProgressBar.value = false
                        hapticVibrate(HapticDuration.HEAVY, HapticCategory.ALERTS)
                        showErrorToast.value = true
                        coroutineScope.launch {
                            delay(2000)
                            showErrorToast.value = false
                        }
                    }
                }
            }
        )
        if (showSuccessToast.value) {
            Toast(
                message = "RSVP submitted successfully!"
            )
        }

        if (showErrorToast.value) {
            Toast(
                message = "Failed to submit RSVP. Please try again."
            )
        }

    }
}

@Composable
fun LargeScreenEventDetailPopup(
    selectedEvent: Event,
    onDismissRequest: () -> Unit,
    onRsvpClick: () -> Unit,
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
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .clickable(
                    interactionSource = null,
                    indication = null
                ) { /* consume click so it doesn't reach dismissing background */ }
        ) {
            EventDetailContent(
                event = selectedEvent,
                onCloseClick = onDismissRequest,
                onRsvpClick = onRsvpClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CompactScreenEventDetailPopup(
    event: Event,
    onDismissRequest: () -> Unit,
    onRsvpClick: () -> Unit,
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
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .clickable(
                    interactionSource = null,
                    indication = null
                ) { /* consume click so it doesn't reach dismissing background */ }
        ) {
            EventDetailContent(
                event = event,
                onCloseClick = onDismissRequest,
                onRsvpClick = onRsvpClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun EventDetailContent(
    event: Event,
    onCloseClick: () -> Unit,
    onRsvpClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
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
                    color = MaterialTheme.colorScheme.onSurface
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
                    placeholder = painterResource(Res.drawable.placeholder),
//                error = painterResource(R.drawable.error_placeholder)
                ),
                contentDescription = "${event.name} Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            EventDetailRow(
                label = "Date",
                value = event.date.toEventDisplayDate()
            )
            if (!event.time.isNullOrBlank()) {
                EventDetailRow(label = "Time", value = event.time)
            }
            EventDetailRow(
                label = "Event Type", value = event
                    .eventType.toString().replace('_', ' ')
            )
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

            val eventLocalDate = event.date.toEventLocalDate()
            if (eventLocalDate != null && eventLocalDate > Clock.System.now()
                    .toLocalDateTime(
                        TimeZone.currentSystemDefault()
                    ).date
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onRsvpClick()
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                MediaGallery(mediaItems = event.gallery)
            }
        }
        VerticalScrollbar(
            adapter  = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 2.dp),
            style = wdwScrollbarStyle()
        )
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
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
    val coroutineScope = rememberCoroutineScope()

    // Keyboard navigation: left/right arrows + Escape
    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = { event ->
            val keyEvent = event.unsafeCast<org.w3c.dom.events.KeyboardEvent>()
            when (keyEvent.key) {
                "ArrowLeft" -> {
                    keyEvent.preventDefault()
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }
                "ArrowRight" -> {
                    keyEvent.preventDefault()
                    if (pagerState.currentPage < mediaItems.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
                "Escape" -> {
                    keyEvent.preventDefault()
                    onDismissRequest()
                }
            }
        }
        window.addEventListener("keydown", listener)
        onDispose { window.removeEventListener("keydown", listener) }
    }

    CompositionLocalProvider(LocalLayerContainer provides document.body!!) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Main pager
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val mediaItem = mediaItems[page]
                        when {
                            mediaItem.type == MediaType.IMAGE &&
                                    !mediaItem.contentUrl.isNullOrEmpty() -> {
                                AsyncImage(
                                    model = mediaItem.contentUrl,
                                    contentDescription = "Image ${mediaItem.contentUrl}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            mediaItem.type == MediaType.VIDEO &&
                                    !mediaItem.contentUrl.isNullOrEmpty() -> {
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

                    // Image counter (e.g. "3 / 12")
                    if (mediaItems.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Close button
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

                    // Previous arrow
                    if (pagerState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Next arrow
                    if (pagerState.currentPage < mediaItems.size - 1) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Pager indicator
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
            val dotSize by animateDpAsState(
                targetValue  = if (isSelected) 12.dp else 8.dp,
                animationSpec = tween(durationMillis = 200),
                label        = "dotSize"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
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

@Composable
fun KmpVideoPlayer(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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

@Composable
fun RSVPComponent(
    event: Event,
    onDismissRequest: () -> Unit,
    onSubmit: (RSVPRequest) -> Unit,
    isCompactScreen: Boolean,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    existingRsvp: RSVPRequest?,
    showProgressBar: Boolean,
    modifier: Modifier = Modifier
) {
    val safeName = (userProfileData?.name ?: "").trim()
    val parts = safeName.split("\\s+".toRegex())
    val profileDataFirstName = parts.getOrNull(0).orEmpty()
    val profileDataLastName = when {
        parts.size > 1 -> parts.subList(1, parts.size).joinToString(" ")
        else -> ""
    }

    val numericRegex = Regex("[^0-9]")

    var firstName by rememberSaveable {
        mutableStateOf(existingRsvp?.firstName ?: profileDataFirstName)
    }
    var lastName by rememberSaveable {
        mutableStateOf(existingRsvp?.lastName ?: profileDataLastName)
    }
    var email by rememberSaveable {
        mutableStateOf(existingRsvp?.email ?: userProfileData?.email)
    }
    var phoneNumber by rememberSaveable {
        mutableStateOf(
            existingRsvp?.phoneNumber?.replace(
                numericRegex, ""
            )?.take(10)
                ?: userProfileData?.phone?.replace(
                    numericRegex, ""
                )?.take(10) ?: ""
        )
    }
    var guestsCount by rememberSaveable {
        mutableStateOf(existingRsvp?.guestsCount ?: 1)
    }
    var allowUpdates by rememberSaveable {
        mutableStateOf(true)
    }
    var guestsCountText by rememberSaveable {
        mutableStateOf(existingRsvp?.guestsCount ?: "1")
    }

    var firstNameError by rememberSaveable { mutableStateOf("") }
    var lastNameError by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf("") }
    var phoneError by rememberSaveable { mutableStateOf("") }
    var guestsError by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Column {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (!isCompactScreen) {
                        NonCompactReservationFields(
                            event = event,
                            firstName = firstName,
                            onFirstNameChange = {
                                firstName = it
                                firstNameError = ""
                            },
                            lastName = lastName,
                            onLastNameChange = {
                                lastName = it
                                lastNameError = ""
                            },
                            email = email,
                            onEmailChange = {
                                email = it
                                emailError = ""
                            },
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = {
                                val stripped = numericRegex.replace(it, "")
                                phoneNumber = stripped.take(10)
                                phoneError = ""
                            },
                            guestsCountText = guestsCountText.toString(),
                            guestCount = guestsCount,
                            onGuestsCountChange = { newVal ->
                                guestsError = ""
                                if (newVal.isEmpty()) {
                                    guestsCountText = ""
                                    guestsCount = 1
                                } else {
                                    newVal.toIntOrNull()?.let { parsed ->
                                        if (parsed in 1..10) {
                                            guestsCountText = newVal
                                            guestsCount = parsed
                                        }
                                    }
                                }
                            },
                            allowUpdates = allowUpdates,
                            onAllowUpdatesToggle = { allowUpdates = it },
                            firstNameError = firstNameError,
                            lastNameError = lastNameError,
                            emailError = emailError,
                            phoneError = phoneError,
                            guestsError = guestsError,
                            onSubmit = {
                                val rsvp = RSVPRequest(
                                    eventId = event.id,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email ?: "",
                                    phoneNumber = phoneNumber,
                                    allowUpdates = allowUpdates,
                                    guestsCount = guestsCount
                                )
                                onSubmit(rsvp)
                            },
                            uiState = uiState,
                            showProgressBar = showProgressBar
                        )

                    } else {
                        CompactScreenReservationFields(
                            event = event,
                            firstName = firstName,
                            onFirstNameChange = {
                                firstName = it
                                firstNameError = ""
                            },
                            lastName = lastName,
                            onLastNameChange = {
                                lastName = it
                                lastNameError = ""
                            },
                            email = email,
                            onEmailChange = {
                                email = it
                                emailError = ""
                            },
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = {
                                val stripped = numericRegex.replace(it, "")
                                phoneNumber = stripped.take(10)
                                phoneError = ""
                            },
                            guestsCountText = guestsCountText.toString(),
                            onGuestsCountChange = { newVal ->
                                guestsError = ""
                                if (newVal.isEmpty()) {
                                    guestsCountText = ""
                                    guestsCount = 1
                                } else {
                                    newVal.toIntOrNull()?.let { parsed ->
                                        if (parsed in 1..10) {
                                            guestsCountText = newVal
                                            guestsCount = parsed
                                        }
                                    }
                                }
                            },
                            allowUpdates = allowUpdates,
                            onAllowUpdatesToggle = { allowUpdates = it },
                            firstNameError = firstNameError,
                            lastNameError = lastNameError,
                            emailError = emailError,
                            phoneError = phoneError,
                            guestCount = guestsCount,
                            guestsError = guestsError,
                            onSubmit = {
                                val rsvp = RSVPRequest(
                                    eventId = event.id,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email ?: "",
                                    phoneNumber = phoneNumber,
                                    allowUpdates = allowUpdates,
                                    guestsCount = guestsCount
                                )
                                onSubmit(rsvp)
                            },
                            showProgressBar = showProgressBar
                        )
                    }
                    AnimatedVisibility(showProgressBar) {
                        LinearProgressBar()
                    }
                }
            }
        }
    }


}

@Composable
fun CompactScreenReservationFields(
    event: Event,
    firstName: String?,
    onFirstNameChange: (String) -> Unit,
    lastName: String?,
    onLastNameChange: (String) -> Unit,
    email: String?,
    onEmailChange: (String) -> Unit,
    phoneNumber: String?,
    onPhoneNumberChange: (String) -> Unit,
    guestsCountText: String,
    onGuestsCountChange: (String) -> Unit,
    allowUpdates: Boolean,
    onAllowUpdatesToggle: (Boolean) -> Unit,
    firstNameError: String,
    lastNameError: String,
    emailError: String,
    phoneError: String,
    guestCount: Int,
    guestsError: String,
    showProgressBar: Boolean = false,
    onSubmit: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Card(
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = "${event.name}'s picture",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        item {
            Text(
                text = "RSVP for ${event.name}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Your contact information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = allowUpdates,
                        onValueChange = onAllowUpdatesToggle,
                        role = Role.Checkbox
                    )
                    .padding(horizontal = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allowUpdates,
                    onCheckedChange = onAllowUpdatesToggle
                )
                Text(
                    text = "Keep me updated on WDW events",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = firstName ?: "",
                onValueChange = {
                    onFirstNameChange(it)
                },
                label = { Text("First Name", color = MaterialTheme.colorScheme.onSurface) },
                isError = firstNameError.isNotEmpty(),
                supportingText = {
                    if (firstNameError.isNotEmpty()) {
                        Text(text = firstNameError, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = lastName ?: "",
                onValueChange = {
                    onLastNameChange(it)
                },
                label = { Text("Last Name", color = MaterialTheme.colorScheme.onSurface) },
                isError = lastNameError.isNotEmpty(),
                supportingText = {
                    if (lastNameError.isNotEmpty()) {
                        Text(text = lastNameError, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = email ?: "",
                onValueChange = {
                    onEmailChange(it)
                },
                label = { Text("Email", color = MaterialTheme.colorScheme.onSurface) },
                isError = emailError.isNotEmpty(),
                supportingText = {
                    if (emailError.isNotEmpty()) {
                        Text(text = emailError, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = phoneNumber ?: "",
                onValueChange = {
                    onPhoneNumberChange(it)
                },
                label = { Text("Phone number", color = MaterialTheme.colorScheme.onSurface) },
                isError = phoneError.isNotEmpty(),
                supportingText = {
                    if (phoneError.isNotEmpty()) {
                        Text(text = phoneError, color = MaterialTheme.colorScheme.error)
                    }
                },
                visualTransformation = NanpVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Column(
            ) {
                Text("Number of guests: ", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.clickable {
                            if (guestCount > 1) {
                                onGuestsCountChange((guestCount - 1).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease guests count",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = guestsCountText,
                        onValueChange = onGuestsCountChange,
                        label = { Text("Guests") },
                        isError = guestsError.isNotEmpty(),
                        supportingText = {
                            if (guestsError.isNotEmpty()) {
                                Text(guestsError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(4.dp),
                        modifier = Modifier.clickable {
                            if (guestCount < 10) {
                                onGuestsCountChange((guestCount + 1).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase guests count",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                enabled = !showProgressBar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7F33),
                    contentColor = Color.White
                )
            ) {
                if (showProgressBar) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
fun NonCompactReservationFields(
    event: Event,
    firstName: String?,
    onFirstNameChange: (String) -> Unit,
    lastName: String?,
    onLastNameChange: (String) -> Unit,
    email: String?,
    onEmailChange: (String) -> Unit,
    phoneNumber: String?,
    onPhoneNumberChange: (String) -> Unit,
    guestCount: Int,
    guestsCountText: String,
    onGuestsCountChange: (String) -> Unit,
    allowUpdates: Boolean,
    onAllowUpdatesToggle: (Boolean) -> Unit,
    firstNameError: String,
    lastNameError: String,
    emailError: String,
    phoneError: String,
    guestsError: String,
    showProgressBar: Boolean = false,
    onSubmit: () -> Unit,
    uiState: LoginUIState,
    modifier: Modifier = Modifier
) {
    LazyColumn {
        item {
            Card(
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = "${event.name}'s picture",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        item {
            Text(
                text = "RSVP for ${event.name}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Your contact information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = allowUpdates,
                        onValueChange = onAllowUpdatesToggle,
                        role = Role.Checkbox
                    )
                    .padding(horizontal = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allowUpdates,
                    onCheckedChange = onAllowUpdatesToggle
                )
                Text(
                    text = "Keep me updated on WDW events",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstName ?: "",
                    onValueChange = { onFirstNameChange(it) },
                    label = { Text("First Name", color = MaterialTheme.colorScheme.onSurface) },
                    isError = firstNameError.isNotEmpty(),
                    supportingText = {
                        if (firstNameError.isNotEmpty()) {
                            Text(firstNameError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lastName ?: "",
                    onValueChange = { onLastNameChange(it) },
                    label = { Text("Last Name", color = MaterialTheme.colorScheme.onSurface) },
                    isError = lastNameError.isNotEmpty(),
                    supportingText = {
                        if (lastNameError.isNotEmpty()) {
                            Text(lastNameError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = email ?: "",
                    onValueChange = { onEmailChange(it) },
                    label = { Text("Email", color = MaterialTheme.colorScheme.onSurface) },
                    isError = emailError.isNotEmpty(),
                    supportingText = {
                        if (emailError.isNotEmpty()) {
                            Text(emailError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phoneNumber ?: "",
                    onValueChange = { onPhoneNumberChange(it) },
                    label = { Text("Phone Number", color = MaterialTheme.colorScheme.onSurface) },
                    isError = phoneError.isNotEmpty(),
                    supportingText = {
                        if (phoneError.isNotEmpty()) {
                            Text(phoneError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    visualTransformation = NanpVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Number of guests: ", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.clickable {
                        if (guestCount > 1) {
                            onGuestsCountChange((guestCount - 1).toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease guests count",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = guestsCountText,
                    onValueChange = onGuestsCountChange,
                    label = { Text("Guests") },
                    isError = guestsError.isNotEmpty(),
                    supportingText = {
                        if (guestsError.isNotEmpty()) {
                            Text(guestsError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.width(80.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(4.dp),
                    modifier = Modifier.clickable {
                        if (guestCount < 10) {
                            onGuestsCountChange((guestCount + 1).toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase guests count",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = !showProgressBar && uiState is LoginUIState.Authenticated,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7F33),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text =
                        if (uiState is LoginUIState.Authenticated) "Submit" else "Log in to RSVP"
                )
            }
        }
    }

}


// Backward-compatible: handles "2025, 3, 19" AND "2025-03-19"
fun stringToDate(date: String): LocalDate? = date.toEventLocalDate()

private fun formatDate(date: LocalDate): String = date.toDisplayString()


sealed class SubmissionStatus {
    object Idle : SubmissionStatus()
    object InProgress : SubmissionStatus()
    object Success : SubmissionStatus()
    data class Failure(val errorMessage: String) : SubmissionStatus()
}
// ─── Vino Event Suggestion Banner ────────────────────────────────────────────

@Composable
fun EventSuggestionBanner(
    suggestions: List<EventSuggestion>,
    isLoading: Boolean,
    onSuggestionClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3A1C5A).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "✨ You might like this",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCE93D8),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF9C6ADE)
                )
            } else {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionClick(suggestion.name) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Column {
                            Text(
                                text = suggestion.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Text(
                                text = suggestion.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
