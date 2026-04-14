package net.winedownwednesday.web.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.hideYouTubePlayer
import net.winedownwednesday.web.showYouTubePlayer
import net.winedownwednesday.web.updateYouTubePlayerPosition
import net.winedownwednesday.web.viewmodels.PodcastsPageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject


@Composable
fun PodcastsPage(
    sizeInfo: WindowSizeInfo
) {
    val viewModel: PodcastsPageViewModel = koinInject()
    val episodes by viewModel.episodes.collectAsState()
    val sortedEpisodes = remember(episodes) {
        episodes?.sortedByDescending { it.date }
    }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredEpisodes = remember (sortedEpisodes, searchQuery){
        sortedEpisodes?.filter { it.matchesQuery(searchQuery) }
    }
    val selectedEpisode = viewModel.selectedEpisode.collectAsState()

    if (sizeInfo.useCompactNav) {
        // Compact + Medium → stacked card list
        CompactPodcastsScreen(
            episodes = filteredEpisodes,
            selectedEpisode = selectedEpisode.value,
            searchQuery = searchQuery,
            onSelectedEpisodeChange = { viewModel.setSelectedEpisode(it) },
            onDismissRequest = { viewModel.clearSelectedEpisode() },
            onSearchQueryChange = { viewModel.setSearchQuery(it) }
        )
    } else {
        // Expanded / Large / XLarge → side-by-side list + video detail
        val listWeight = when (sizeInfo.widthClass) {
            WidthClass.Large, WidthClass.XLarge -> 1f  // narrower list on big screens
            else -> 1f                                  // Expanded default
        }
        val detailWeight = when (sizeInfo.widthClass) {
            WidthClass.Large, WidthClass.XLarge -> 3f
            else -> 2f  // Expanded
        }
        LargeScreenPodcastPage(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            filteredEpisodes = filteredEpisodes,
            onEpisodeSelected = { viewModel.setSelectedEpisode(it) },
            selectedEpisode = selectedEpisode.value,
            listWeight = listWeight,
            detailWeight = detailWeight
        )
    }


}

@Composable
fun LargeScreenPodcastPage(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    filteredEpisodes: List<Episode>?,
    onEpisodeSelected: (Episode) -> Unit,
    selectedEpisode: Episode?,
    listWeight: Float = 1f,
    detailWeight: Float = 2f
){
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(listWeight)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchBar(
                label = "Search Uncorked Conversations episodes",
                query = searchQuery,
                onQueryChange = onQueryChange
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
                val episodeListState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state          = episodeListState,
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        filteredEpisodes?.let { episodes ->
                            if (episodes.isEmpty()) {
                                item {
                                    ComingSoonPlaceholder(
                                        title = "Uncorked Conversations",
                                        subtitle = "Our podcast episodes are coming soon! Stay tuned."
                                    )
                                }
                            } else {
                                items(episodes) { episode ->
                                    ScrollReveal {
                                        EpisodeListItem(
                                            episode = episode,
                                            isSelected = (
                                                    if (selectedEpisode != null) {
                                                        episode == selectedEpisode
                                                    } else {
                                                        false
                                                    }),
                                            onClick = {
                                                onEpisodeSelected(episode)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        } ?: item {
                            ComingSoonPlaceholder(
                                title = "Uncorked Conversations",
                                subtitle = "Our podcast episodes are coming soon! Stay tuned."
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter  = rememberScrollbarAdapter(episodeListState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 2.dp),
                        style    = wdwScrollbarStyle()
                    )
                }
            }

        }

        Column(
            modifier = Modifier
                .weight(detailWeight)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                if (selectedEpisode == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select an episode to see the details and watch",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    EpisodeVideoDetail(
                        episode = selectedEpisode
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeListItem(
    episode: Episode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue  = if (isSelected) Color(0xFFFF7F33)
            .copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 250),
        label        = "episodeSelection"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .hoverScale()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = episode.guestPictureUrl,
                    contentDescription = "Guest ${episode.guestName}' s Picture",
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Guest: ${episode.guestName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun EpisodeVideoDetail(
    episode: Episode
) {
    // Right panel is dedicated to the video on large screens.
    // Episode title / guest info are shown in the left list — no need to repeat them here.
    Box(modifier = Modifier.fillMaxSize()) {
        KmpYoutubeVideoPlayer(url = episode.videoUrl)
    }
}


@Composable
fun KmpYoutubeVideoPlayer(url: String) {
    val videoId  = extractYouTubeVideoId(url)
    val density  = LocalDensity.current.density
    // Track which videoId the bridge is currently showing so we can
    // call updatePosition on layout changes instead of recreating the iframe.
    val shownId  = remember { androidx.compose.runtime.mutableStateOf("") }

    // Always hide when this composable LEAVES the composition entirely
    // (e.g. navigating away from Podcasts). Without this, the iframe stays
    // visible at its last position and overlaps other pages (like the footer).
    DisposableEffect(Unit) {
        onDispose { hideYouTubePlayer() }
    }

    // Also hide+reset when the video ID changes (switching episodes)
    DisposableEffect(videoId) {
        onDispose {
            hideYouTubePlayer()
            shownId.value = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                if (videoId.isNotEmpty()) {
                    val loc = coordinates.positionInWindow()
                    val sz  = coordinates.size
                    val l = loc.x             / density
                    val t = loc.y             / density
                    val w = sz.width.toFloat()  / density
                    val h = sz.height.toFloat() / density
                    if (shownId.value != videoId) {
                        showYouTubePlayer(videoId, l, t, w, h)
                        shownId.value = videoId
                    } else {
                        updateYouTubePlayerPosition(l, t, w, h)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (videoId.isEmpty()) {
            Text(text = "No video available", color = Color.White)
        }
    }
}

fun extractYouTubeVideoId(url: String): String {
    if (!url.contains("youtube.com") && !url.contains("youtu.be")) {
        return ""
    }

    if (url.contains("youtu.be")) {
        val parts = url.split("/")
        if (parts.isNotEmpty()) {
            val lastPart = parts.last()
            return lastPart.split("?")[0]
        }
        return ""
    }

    val queryStart = url.indexOf("?")
    if (queryStart == -1) {
        return ""
    }

    val queryString = url.substring(queryStart + 1)
    val queryParams = queryString.split("&")

    for (param in queryParams) {
        val parts = param.split("=")
        if (parts.size == 2 && parts[0] == "v") {
            return parts[1]
        }
    }

    return ""
}

@Composable
fun CompactPodcastsScreen(
    episodes: List<Episode>?,
    selectedEpisode: Episode?,
    searchQuery: String,
    onSelectedEpisodeChange: (Episode) -> Unit,
    onDismissRequest: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {

        SearchBar(
            label = "Search Uncorked Conversations episodes",
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )
        val filteredEpisodes = episodes?.filter { it.matchesQuery(searchQuery) }

        val compactListState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                state = compactListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (!filteredEpisodes.isNullOrEmpty()) {
                    itemsIndexed(filteredEpisodes) { index, episode ->
                        GridItemReveal(index = index) {
                            EpisodeCard(
                                episode = episode,
                                onClick = { onSelectedEpisodeChange(episode) }
                            )
                        }
                    }
                } else {
                    item {
                        ComingSoonPlaceholder(
                            title = "Uncorked Conversations",
                            subtitle = "Our podcast episodes are coming soon! Stay tuned."
                        )
                    }
                }
            }

            VerticalScrollbar(
                adapter  = rememberScrollbarAdapter(compactListState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                style    = wdwScrollbarStyle()
            )

            if (selectedEpisode != null) {
                EpisodeVideoPopup(
                    episode = selectedEpisode,
                    onDismissRequest = { onDismissRequest() }
                )
            }
        }
    }
}

@Composable
fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .hoverScale()
            .clickable(onClick = onClick)
            .wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = episode.guestPictureUrl,
                contentDescription =  "${episode.guestName}'s Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = episode.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EpisodeVideoPopup(episode: Episode, onDismissRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismissRequest)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                // Consume clicks so tapping inside the popup
                // does not propagate to the scrim and dismiss.
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    }
                ) { /* intentionally empty — swallows the event */ }
        ) {
            EpisodeVideoContent(episode = episode, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun EpisodeVideoContent(episode: Episode, onCloseClick: () -> Unit) {

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = episode.title,
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    KmpYoutubeVideoPlayer((episode.videoUrl))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = episode.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
}
