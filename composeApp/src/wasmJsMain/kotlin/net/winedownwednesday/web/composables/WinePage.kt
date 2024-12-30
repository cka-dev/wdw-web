package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.viewmodels.WinePageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject


@Composable
fun WinePage() {
    val viewModel: WinePageViewModel = koinInject()
    val wines by viewModel.wineList.collectAsState()
    val selectedWine by viewModel.selectedWine.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredWines = remember(wines, searchQuery) {
        wines.filter { it.matchesQuery(searchQuery) }
    }

    Row(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .background(Color.Black)
        ) {
            SearchBar(
                label = "Search our wine list",
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            if (filteredWines.isEmpty()) {
                Text(
                    text = "No wines found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ){
                    Text(
                        text = "Our Favorite Wine List",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredWines) { wine ->
                            WineListItem(
                                wine = wine,
                                isSelected = (wine == selectedWine),
                                onClick = { viewModel.setSelectedWine(wine) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.Black),
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
            ){
                if (selectedWine == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a wine to see details",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    WineDetail(
                        wine =  selectedWine!!,
                    )
                }
            }
        }
    }
}

@Composable
fun WineListItem(
    wine: Wine,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFFF7F33) else Color(0xFF2A2A2A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ){
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = wine.imageUrl,
                    contentDescription = "${wine.name}' s picture",
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(
            modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${wine.name} (${wine.year})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${wine.type} - ${wine.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun WineDetail(
    wine: Wine,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = wine.name,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = rememberAsyncImagePainter(wine.imageUrl),
            contentDescription = "Wine Image",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${wine.type} - ${wine.year} (${wine.country}, ${wine.region})",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = wine.technicalDetails,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        if (!wine.whyWeLovedIt.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why We Loved It:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = wine.whyWeLovedIt,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun WineImageCard(
    wineImageUrl: String
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(8.dp)
    ){

        Image(
            painter = rememberAsyncImagePainter(wineImageUrl),
            contentDescription = "Wine Image",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )

    }
}

