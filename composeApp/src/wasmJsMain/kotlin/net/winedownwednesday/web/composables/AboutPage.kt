package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
fun AboutPage(
    isCompactScreen: Boolean
) {
    val viewModel: AboutPageViewModel = koinInject()
    val aboutSections by viewModel.aboutSections.collectAsState()
    var selectedSection by remember { mutableStateOf<AboutSection?>(null) }

    Surface(color = Color(0xFF141414)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                        intervalMillis = 4000L,
                        isCompactScreen = isCompactScreen
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "About Wine Down Wednesday",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Exploring the rich history, mission, and values of our" +
                                        "community dedicated to the appreciation of fine wines.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            if (isCompactScreen) {
                items(aboutSections) { aboutSection ->
                    CompactScreenAboutCard(
                        section = aboutSection,
                        onClick = { selectedSection = aboutSection }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
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
                                LSAboutCard(
                                    section = section,
                                    onClick = { selectedSection = section }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    selectedSection?.let { section ->
        AboutSectionDialog(
            section = section,
            onDismiss = { selectedSection = null }
        )
    }
}

@Composable
fun LSAboutCard(
    section: AboutSection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AboutImage(
                    imageRes = section.imageRes,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f)) {
                    AboutText(section.title, section.body, maxLines = 3)
                }
            }
        }
    }
}

@Composable
fun CompactScreenAboutCard(
    section: AboutSection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AboutImage(
                imageRes = section.imageRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                AboutText(section.title, section.body, maxLines = 3)
            }
        }
    }
}

@Composable
fun AboutText(title: String, body: String, maxLines: Int = Int.MAX_VALUE) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray.copy(alpha = 0.9f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AboutImage(imageRes: DrawableResource?, modifier: Modifier = Modifier) {
    if (imageRes == null) {
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("No Image", color = Color.White)
        }
    } else {
        Image(
            painter = painterResource(imageRes),
            contentDescription = "About Image",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun AutoScrollingImageCarousel(
    imagePaths: List<DrawableResource>,
    intervalMillis: Long = 3000L,
    isCompactScreen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = imagePaths) {
        if (imagePaths.isNotEmpty()) {
            while (true) {
                delay(intervalMillis)
                currentIndex = (currentIndex + 1) % imagePaths.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCompactScreen) 320.dp else 450.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 1400.dp) // the constrained inner container
                .fillMaxSize()
                .clip(RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(imagePaths[currentIndex]),
                contentDescription = "Carousel Image",
                contentScale = ContentScale.Crop, // crops evenly now, but limited in width
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 0f
                    )
                )
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            content()
        }
    }
}

@Composable
fun AboutSectionDialog(
    section: AboutSection,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AboutImage(
                    imageRes = section.imageRes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}





