package net.winedownwednesday.web.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
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
    sizeInfo: WindowSizeInfo
) {
    val viewModel: AboutPageViewModel = koinInject()
    val aboutSections by viewModel.aboutSections.collectAsState()
    var selectedSection by remember { mutableStateOf<AboutSection?>(null) }

    val aboutListState = rememberLazyListState()
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state    = aboutListState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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
                        sizeInfo = sizeInfo
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

            if (sizeInfo.isCompact) {
                // Compact: single-column stacked cards
                itemsIndexed(aboutSections) { index, aboutSection ->
                    GridItemReveal(index = index) {
                        CompactScreenAboutCard(
                            section = aboutSection,
                            onClick = { selectedSection = aboutSection }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Medium, Expanded, Large, XLarge: 2-column grid
                val chunkedSections = aboutSections.chunked(2)

                items(chunkedSections.size) { chunkIndex ->
                    val chunk = chunkedSections[chunkIndex]
                    GridItemReveal(index = chunkIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = sizeInfo.horizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            chunk.forEach { section ->
                                Box(modifier = Modifier.weight(1f)) {
                                    LSAboutCard(
                                        section = section,
                                        onClick = { selectedSection = section }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            }
            VerticalScrollbar(
                adapter  = rememberScrollbarAdapter(aboutListState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                style    = wdwScrollbarStyle()
            )
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
fun AboutSectionDialog(
    section: AboutSection,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Close", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
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
            .hoverScale()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            .hoverScale()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
    sizeInfo: WindowSizeInfo,
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

    val carouselHeight = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> 320.dp
        WidthClass.Medium   -> 380.dp
        WidthClass.Expanded -> 450.dp
        WidthClass.Large    -> 480.dp
        WidthClass.XLarge   -> 520.dp
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(carouselHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 1400.dp) // the constrained inner container
                .fillMaxSize()
                .clip(RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState  = currentIndex,
                animationSpec = tween(durationMillis = 600),
                label        = "carouselCrossfade"
            ) { index ->
                Image(
                    painter = painterResource(imagePaths[index]),
                    contentDescription = "Carousel Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
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

