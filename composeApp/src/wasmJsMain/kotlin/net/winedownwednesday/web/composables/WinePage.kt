package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.viewmodels.WinePageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject


@Composable
fun WinePage(
    isCompactScreen: Boolean
) {
    val viewModel: WinePageViewModel = koinInject()
    val wines by viewModel.wineList.collectAsState()
    val selectedWine by viewModel.selectedWine.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredWines = remember(wines, searchQuery) {
        wines.filter { it.matchesQuery(searchQuery) }
    }

    if (isCompactScreen) {
        CompactScreenWinePage(
            favoriteWines= filteredWines,
            selectedWine = selectedWine,
            searchQuery = searchQuery,
            onSearchQueryChange = {viewModel.setSearchQuery(it)},
            onWineClick = {viewModel.setSelectedWine(it)}
        )
    } else {
        LargeScreenWinePage(
            searchQuery = searchQuery,
            onQueryChange = {viewModel.setSearchQuery(it)},
            filteredWines = filteredWines,
            selectedWine = selectedWine,
            onSelectedWineChange = {viewModel.setSelectedWine(it)}
        )
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

@Composable
fun WineCard(
    wine: Wine,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = wine.imageUrl,
                contentDescription = "${wine.name} Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = wine.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Text(
                text = "${wine.type} - ${wine.country}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = wine.technicalDetails,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            if (isFavorite && wine.whyWeLovedIt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Why we loved it:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = wine.whyWeLovedIt,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun WineDetailPopup(wine: Wine, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
        ) {
            WineDetailContent(wine = wine, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun WineDetailContent(wine: Wine, onCloseClick: () -> Unit) {
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
                text = wine.name,
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
        AsyncImage(
            model = wine.imageUrl,
            contentDescription = "${wine.name} Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(16.dp))
                .align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${wine.type} - ${wine.country}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = wine.technicalDetails,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (wine.whyWeLovedIt != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why we loved it:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = wine.whyWeLovedIt,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CompactScreenWinePage(
    favoriteWines: List<Wine>,
    selectedWine: Wine?,
    searchQuery: String,
    onWineClick: (Wine) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            SearchBar(
                label = "Search our wine list",
                query = searchQuery,
                onQueryChange = { onSearchQueryChange(it) }
            )
        }

        item {
            Text(
                text = "Our Favorites",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(favoriteWines.filter { it.matchesQuery(searchQuery) }) { wine ->
            WineCard(
                wine = wine,
                isFavorite = true,
                onClick = {
                    onWineClick(wine)
                    showDialog = true
                }
            )
        }

        item {
            Text(
                text = "Wines We've Tried",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showDialog) {
        selectedWine?.let { wine ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WineDetailPopup(
                    wine = wine,
                    onDismissRequest = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun LargeScreenWinePage(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    filteredWines: List<Wine>,
    selectedWine: Wine?,
    onSelectedWineChange: (Wine) -> Unit
) {
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
                onQueryChange = { onQueryChange(it) }
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
                                onClick = { onSelectedWineChange(wine) }
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
                        wine =  selectedWine,
                    )
                }
            }
        }
    }
}

