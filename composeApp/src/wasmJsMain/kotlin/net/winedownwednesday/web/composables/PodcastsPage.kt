package net.winedownwednesday.web.composables

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.browser.document
import net.winedownwednesday.web.HtmlView
import net.winedownwednesday.web.LocalLayerContainer
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.viewmodels.PodcastsPageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject


@Composable
fun PodcastsPage(
    isCompactScreen: Boolean
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

    if (isCompactScreen) {
        CompactPodcastsScreen(
            episodes = filteredEpisodes,
            selectedEpisode = selectedEpisode.value,
            searchQuery = searchQuery,
            onSelectedEpisodeChange = {viewModel.setSelectedEpisode(it)},
            onDismissRequest = { viewModel.clearSelectedEpisode() },
            onSearchQueryChange = { viewModel.setSearchQuery(it) }
        )
    } else {
        LargeScreenPodcastPage(
            searchQuery = searchQuery,
            onQueryChange = {viewModel.setSearchQuery(it)},
            filteredEpisodes = filteredEpisodes,
            onEpisodeSelected = {viewModel.setSelectedEpisode(it)},
            selectedEpisode = selectedEpisode.value
        )
    }


}

@Composable
fun LargeScreenPodcastPage(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    filteredEpisodes: List<Episode>?,
    onEpisodeSelected: (Episode) -> Unit,
    selectedEpisode: Episode?
){
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Black)
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
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    filteredEpisodes?.let { episodes ->
                        items(episodes) { episode ->
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
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                }
            }

        }

        CompositionLocalProvider(LocalLayerContainer provides document.body!!) {
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
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
                                color = Color.LightGray
                            )
                        }
                    } else {
                        EpisodeVideoDetail(
                            episode = selectedEpisode!!
                        )
                    }
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
    val bgColor = if (isSelected) Color(0xFF333333) else Color(0xFF1E1E1E)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Guest: ${episode.guestName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun EpisodeVideoDetail(
    episode: Episode
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = episode.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = episode.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            KmpYoutubeVideoPlayer(url = episode.videoUrl)
        }

    }
}

@Composable
fun KmpYoutubeVideoPlayer(url: String) {
    val videoId = extractYouTubeVideoId(url)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoId.isNotEmpty()) {
            val embedUrl = "https://www.youtube.com/embed/$videoId"
            println("Embedding video from: $embedUrl")

            HtmlView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val iframe = createElement("iframe")
                    iframe.setAttribute("src", embedUrl)
                    iframe.setAttribute("frameborder", "0")
                    iframe.setAttribute(
                        "allow",
                        "accelerometer; autoplay; " +
                                "clipboard-write; encrypted-media; " +
                                "gyroscope; picture-in-picture"
                    )
                    iframe.setAttribute("allowfullscreen", "true")
                    iframe
                },
                update = {
                }
            )
        } else {
            Text(
                text = "Invalid video URL",
                color = Color.White
            )
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
        modifier = Modifier.fillMaxSize()
    ) {

        SearchBar(
            label = "Search Uncorked Conversations episodes",
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )
        val filteredEpisodes = episodes?.filter { it.matchesQuery(searchQuery) }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (!filteredEpisodes.isNullOrEmpty()) {
                    items(filteredEpisodes) { episode ->
                        EpisodeCard(
                            episode = episode,
                            onClick = { onSelectedEpisodeChange(episode) }
                        )
                    }
                }
            }

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
                    color = Color.White
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
                    color = Color.White
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
        ) {
            EpisodeVideoContent(episode = episode, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun EpisodeVideoContent(episode: Episode, onCloseClick: () -> Unit) {

    CompositionLocalProvider(LocalLayerContainer provides document.body!!) {
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
}
