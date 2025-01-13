package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import net.winedownwednesday.web.LocalLayerContainer
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.MediaItem
import net.winedownwednesday.web.data.MediaType
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.koin.compose.koinInject


@Composable
fun EventsPage(
    isCompactScreen: Boolean,
    authPageViewModel: AuthPageViewModel,
    uiState: LoginUIState
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
                        containerColor = if (showUpcoming)
                            Color(0xFFFF7F33) else Color(0xFF2A2A2A),
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
                        containerColor = if (!showUpcoming)
                            Color(0xFFFF7F33) else Color(0xFF2A2A2A),
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
                        },
                        viewModel = viewModel,
                        showUpcoming = showUpcoming,
                        isCompactScreen = isCompactScreen,
                        uiState = uiState,
                        authPageViewModel = authPageViewModel
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
    viewModel: EventsPageViewModel,
    authPageViewModel: AuthPageViewModel,
    showUpcoming: Boolean,
    isCompactScreen: Boolean,
    uiState: LoginUIState,
    modifier: Modifier = Modifier
) {
    val showRegistrationForm = remember {
        mutableStateOf(false)
    }
    val userProfileData by authPageViewModel.profileData.collectAsState()

    val isUserAuthenticated = (uiState is LoginUIState.Authenticated)

    val userHasRsvped = authPageViewModel.hasUserRsvped(event.id)

    val buttonLabel = when {
        userHasRsvped -> "Modify RSVP"
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
                    enabled = showUpcoming,
                    onClick = {
                        if (isUserAuthenticated) {
                            showRegistrationForm.value = true
                        } else {
                            window.alert("You need to login in to RSVP")
                        }
                        showRegistrationForm.value = true
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

    AnimatedVisibility (showRegistrationForm.value) {
        RSVPComponent(
            onDismissRequest = { showRegistrationForm.value = false},
            event = event,
            existingRsvp = if (userHasRsvped) authPageViewModel.getRsvpForEvent(event.id) else null,
            isCompactScreen = isCompactScreen,
            uiState = uiState,
            userProfileData = userProfileData,
            showProgressBar = showProgressBar.value,
            showSuccessToast = showSuccessToast.value,
            showErrorToast = showErrorToast.value,
            onSubmit = { rsvpRequest ->
                var submissionStatus = mutableStateOf<SubmissionStatus>(SubmissionStatus.InProgress)

                authPageViewModel.saveRsvpInProfile(rsvpRequest) { profileSaveSuccess ->
                    if (profileSaveSuccess) {
                        viewModel.addRsvpToEvent(rsvpRequest) { eventUpdateSuccess ->
                            if (eventUpdateSuccess) {
                                submissionStatus.value = SubmissionStatus.Success
                                showRegistrationForm.value = false
                            } else {
                                submissionStatus.value = SubmissionStatus.Failure("Failed to update event RSVP.")
                            }
                        }
                    } else {
                        submissionStatus.value = SubmissionStatus.Failure("Failed to save RSVP in your profile.")
                    }
                }

                when (submissionStatus.value) {
                    is SubmissionStatus.InProgress -> {
                        showProgressBar.value = true
                    }
                    is SubmissionStatus.Success -> {
                        showProgressBar.value = false
                        submissionStatus.value = SubmissionStatus.Idle
                        showSuccessToast.value = true
                        coroutineScope.launch {
                            delay(2000)
                            showSuccessToast.value = false
                        }
                    }
                    is SubmissionStatus.Failure -> {
                        showProgressBar.value = false
                        showErrorToast.value = true
                        coroutineScope.launch {
                            delay(2000)
                            showErrorToast.value = false
                        }
                    }
                    is SubmissionStatus.Idle -> {
                        // do nothing
                    }
                }
            }
//            onSubmit = { rsvpRequest ->
//                authPageViewModel.saveRsvpInProfile(rsvpRequest) { success ->
//                    if (success) {
//                        showRegistrationForm.value = false
//                    } else {
//                        window.alert("We couldn't submit your RSVP. Try again.")
//                    }
//
//                }
//                viewModel.addRsvpToEvent(rsvpRequest) { success ->
//                    if (success) {
//                        println("addRsvpToEvent succeeded")
////                        showRegistrationForm.value = false
//                    } else {
//                        println("addRsvpToEvent failed")
////                        window.alert("We couldn't submit your RSVP. Try again.")
//                    }
//                }
//            }
        )

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

        if (stringToDate(event.date) > Clock.System.now()
                .toLocalDateTime(
                    TimeZone.currentSystemDefault()
                ).date
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // TODO: show registration form
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
    showSuccessToast: Boolean,
    showErrorToast: Boolean,
    modifier: Modifier = Modifier
) {
    val safeName = (userProfileData?.name ?: "").trim()
    val parts = safeName.split("\\s+".toRegex())
    val profileDataFirstName = parts.getOrNull(0).orEmpty()
    val profileDataLastName = when {
        parts.size > 1 -> parts.subList(1, parts.size).joinToString(" ")
        else -> ""
    }

    var firstName by rememberSaveable { mutableStateOf(existingRsvp?.firstName ?: profileDataFirstName) }
    var lastName by rememberSaveable { mutableStateOf(existingRsvp?.lastName ?: profileDataLastName) }
    var email by rememberSaveable { mutableStateOf(existingRsvp?.email ?: userProfileData?.email) }
    var phoneNumber by rememberSaveable { mutableStateOf(existingRsvp?.phoneNumber ?: userProfileData?.phone) }
    var guestsCount by rememberSaveable { mutableStateOf(existingRsvp?.guestsCount ?: 1) }
    var allowUpdates by rememberSaveable { mutableStateOf(true) }
    var guestsCountText by rememberSaveable { mutableStateOf(existingRsvp?.guestsCount ?: "1") }

    var firstNameError by rememberSaveable { mutableStateOf("") }
    var lastNameError by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf("") }
    var phoneError by rememberSaveable { mutableStateOf("") }
    var guestsError by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
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
                            phoneNumber = it
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
                                phoneNumber = phoneNumber ?: "",
                                allowUpdates = allowUpdates,
                                guestsCount = guestsCount
                            )
                            onSubmit(rsvp)
                        },
                        uiState = uiState
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
                            phoneNumber = it
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
                        guestsError = guestsError,
                        onSubmit = {
                            val rsvp = RSVPRequest(
                                eventId = event.id,
                                firstName = firstName,
                                lastName = lastName,
                                email = email ?: "",
                                phoneNumber = phoneNumber ?: "",
                                allowUpdates = allowUpdates,
                                guestsCount = guestsCount
                            )
                            onSubmit(rsvp)
                        }
                    )
                }
            }
            if (showProgressBar){
                CircularProgressIndicator()
            }

            if (showSuccessToast){
                Toast(
                    message = "RSVP submitted successfully!"
                )
            }

            if (showErrorToast){
                Toast(
                    message = "Failed to submit RSVP. Please try again."
                )
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
    guestsError: String,
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
                label = { Text("First Name", color = Color.White) },
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
                label = { Text("Last Name", color = Color.White) },
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
                label = { Text("Email", color = Color.White) },
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
                label = { Text("Phone number", color = Color.White) },
                isError = phoneError.isNotEmpty(),
                supportingText = {
                    if (phoneError.isNotEmpty()) {
                        Text(text = phoneError, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Number of guests: ", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = guestsCountText,
                    onValueChange = { newVal ->
                        onGuestsCountChange(newVal)
                    },
                    label = { Text("Guests") },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    isError = guestsError.isNotEmpty(),
                    supportingText = {
                        if (guestsError.isNotEmpty()) {
                            Text(text = guestsError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7F33),
                    contentColor = Color.White
                )
            ) {
                Text("Submit")
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
    onSubmit: () -> Unit,
    uiState: LoginUIState,
    modifier: Modifier = Modifier
) {
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

    Text(
        text = "RSVP for ${event.name}",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    )

    Text(
        text = "Your contact information",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = firstName ?: "",
            onValueChange = { onFirstNameChange(it) },
            label = { Text("First Name", color = Color.White) },
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
            label = { Text("Last Name", color = Color.White) },
            isError = lastNameError.isNotEmpty(),
            supportingText = {
                if (lastNameError.isNotEmpty()) {
                    Text(lastNameError, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = email ?: "",
            onValueChange = { onEmailChange(it) },
            label = { Text("Email", color = Color.White) },
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
            label = { Text("Phone Number", color = Color.White) },
            isError = phoneError.isNotEmpty(),
            supportingText = {
                if (phoneError.isNotEmpty()) {
                    Text(phoneError, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Number of guests: ", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Card (
            modifier = Modifier.clickable {
                if (guestCount > 1) {
                    onGuestsCountChange((guestCount - 1).toString())
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease guests count",
                tint = Color.White
            )
        }
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
        Card (
            modifier = Modifier.clickable {
                if (guestCount < 10) {
                    onGuestsCountChange((guestCount + 1).toString())
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase guests count",
                tint = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onSubmit,
        enabled = uiState is LoginUIState.Authenticated,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF7F33),
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (uiState is LoginUIState.Authenticated) "Submit" else "Log in to RSVP"
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

sealed class SubmissionStatus {
    object Idle : SubmissionStatus()
    object InProgress : SubmissionStatus()
    object Success : SubmissionStatus()
    data class Failure(val errorMessage: String) : SubmissionStatus()
}