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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.winedownwednesday.web.data.models.AboutSection
import net.winedownwednesday.web.viewmodels.AboutPageViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.wdw_web_carousel_1
import wdw_web.composeapp.generated.resources.wdw_web_carousel_2
import wdw_web.composeapp.generated.resources.wdw_web_carousel_3


@Composable
fun AboutPage() {
    val viewModel: AboutPageViewModel = koinInject()
    val aboutSections by viewModel.aboutSections.collectAsState()

    Surface(color = Color(0xFF141414)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
                .background(Color.Black)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AutoScrollingImageCarousel(
                        imagePaths = listOf(
                            Res.drawable.wdw_web_carousel_1,
                            Res.drawable.wdw_web_carousel_2,
                            Res.drawable.wdw_web_carousel_3
                        ),
                        intervalMillis = 4000L
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = "About Wine Down Wednesday",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "Exploring the rich history, mission, and values of our community dedicated to the appreciation of fine wines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }


            val chunkedSections = aboutSections.chunked(2)

            items(chunkedSections.size) { chunkIndex ->
                val chunk = chunkedSections[chunkIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    chunk.forEachIndexed { indexInChunk, section ->
                        Box(modifier = Modifier
                            .weight(1f)
                        ) {
                            AboutCard(section)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AboutCard(section: AboutSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AboutImage(section.imageRes)
            Spacer(modifier = Modifier.width(16.dp))
            AboutText(section.title, section.body)
        }
    }
}

@Composable
fun AboutText(title: String, body: String) {
    Column(
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
    }
}

@Composable
fun AboutImage(imageRes: DrawableResource?) {
    if (imageRes == null) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 100.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("No image", color = Color.White)
        }
    } else {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 100.dp, height = 100.dp)
        )
    }
}


@Composable
fun AutoScrollingImageCarousel(
    imagePaths: List<DrawableResource>,
    intervalMillis: Long = 3000L,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = imagePaths) {
        while (true) {
            delay(intervalMillis)
            currentIndex = (currentIndex + 1) % imagePaths.size
        }
    }

    Box(
        modifier = modifier
            .height(450.dp)
            .width(1200.dp)
            .clip(RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Image(
                painter = painterResource(imagePaths[currentIndex]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



