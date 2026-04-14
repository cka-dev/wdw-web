package net.winedownwednesday.web.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.runtime.LaunchedEffect
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
import net.winedownwednesday.web.viewmodels.WineRecommendation
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject


@Composable
fun WinePage(
    sizeInfo: WindowSizeInfo,
    isLoggedIn: Boolean = false,
    userName: String? = null,
    userEmail: String? = null
) {
    val viewModel: WinePageViewModel = koinInject()
    val wines by viewModel.wineList.collectAsState()
    val selectedWine by viewModel.selectedWine.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pendingWineName by viewModel.pendingWineName.collectAsState()
    val recommendations by viewModel.vinoRecommendations.collectAsState()
    val isFetchingRecs by viewModel.isFetchingRecs.collectAsState()
    val isWinesLoaded = wines != null

    val recPanelExpanded by viewModel.recPanelExpanded.collectAsState()

    val filteredWines = remember(wines, searchQuery) {
        wines?.filter { it.matchesQuery(searchQuery) } ?: emptyList()
    }

    // Auto-select a wine by name when navigated from a Vino card
    LaunchedEffect(wines, pendingWineName) {
        val name = pendingWineName ?: return@LaunchedEffect
        val match = wines?.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (match != null) {
            viewModel.setSelectedWine(match)
            viewModel.clearPendingWineName()
        }
    }

    // Silently fetch recommendations when user logs in — show panel only if results arrive
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && recommendations.isEmpty() && !isFetchingRecs) {
            viewModel.fetchVinoRecommendations()
        }
    }

    if (sizeInfo.useCompactNav) {
        CompactScreenWinePage(
            favoriteWines = filteredWines,
            selectedWine = selectedWine,
            viewModel = viewModel,
            isLoggedIn = isLoggedIn,
            userName = userName,
            userEmail = userEmail,
            recommendations = recommendations,
            isFetchingRecs = isFetchingRecs,
            isLoaded = isWinesLoaded,
            recPanelExpanded = recPanelExpanded,
            onRecPanelExpandedChange = { viewModel.setRecPanelExpanded(it) }
        )
    } else {
        val listFraction = when (sizeInfo.widthClass) {
            WidthClass.Large, WidthClass.XLarge -> 0.25f
            else -> 0.30f
        }
        LargeScreenWinePage(
            filteredWines = filteredWines,
            selectedWine = selectedWine,
            viewModel = viewModel,
            isLoggedIn = isLoggedIn,
            userName = userName,
            userEmail = userEmail,
            listFraction = listFraction,
            recommendations = recommendations,
            isFetchingRecs = isFetchingRecs,
            isLoaded = isWinesLoaded,
            recPanelExpanded = recPanelExpanded,
            onRecPanelExpandedChange = { viewModel.setRecPanelExpanded(it) }
        )
    }
}

@Composable
fun WineListItem(
    wine: Wine,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue  = if (isSelected) Color(0xFFFF7F33).copy(alpha = 0.4f) else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 300),
        label        = "wineSelection"
    )

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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${wine.type} - ${wine.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                WineRatingBadge(averageRating = wine.averageRating, reviewCount = wine.reviewCount)
            }
        }
    }
}

@Composable
fun WineDetail(
    wine: Wine,
    viewModel: WinePageViewModel,
    isLoggedIn: Boolean,
    userName: String?,
    userEmail: String?
) {
    val reviews by viewModel.reviewsForSelectedWine.collectAsState()
    val myReview by viewModel.myReviewForSelectedWine.collectAsState()
    val isLoadingReviews by viewModel.isLoadingReviews.collectAsState()
    val hasFetchedReviews by viewModel.hasFetchedReviews.collectAsState()
    val isSubmittingReview by viewModel.isSubmittingReview.collectAsState()
    val reviewSubmitError by viewModel.reviewSubmitError.collectAsState()
    val isFlaggingReview by viewModel.isFlaggingReview.collectAsState()
    val flagSuccess by viewModel.flagSuccess.collectAsState()
    val flagError by viewModel.flagError.collectAsState()
    var showWriteReview by rememberSaveable { mutableStateOf(false) }

    // Close dialog when success
    val reviewSubmitSuccess by viewModel.reviewSubmitSuccess.collectAsState()
    if (reviewSubmitSuccess) {
        showWriteReview = false
        viewModel.clearReviewFeedback()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        WineRatingBadge(averageRating = wine.averageRating, reviewCount = wine.reviewCount)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = wine.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = rememberAsyncImagePainter(wine.imageUrl),
            contentDescription = "Wine Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${wine.type} - ${wine.year} (${wine.country}, ${wine.region})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = wine.technicalDetails,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (!wine.whyWeLovedIt.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why We Loved It:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = wine.whyWeLovedIt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        ReviewsSection(
            wineId = wine.id,
            isLoggedIn = isLoggedIn,
            userName = userName,
            userEmail = userEmail,
            reviews = reviews,
            myReview = myReview,
            isLoading = isLoadingReviews,
            hasFetchedReviews = hasFetchedReviews,
            isFlaggingReview = isFlaggingReview,
            flagSuccess = flagSuccess,
            flagError = flagError,
            onWriteReviewClick = { showWriteReview = true },
            onEditReviewClick = { showWriteReview = true },
            onDeleteReview = { viewModel.deleteMyReview(it) },
            onFlagReview = { email, reason -> viewModel.flagReview(wine.id, email, reason) },
            onClearFlagFeedback = { viewModel.clearFlagFeedback() }
        )
    }

    if (showWriteReview) {
        WriteReviewDialog(
            wineName = wine.name,
            initialRating = myReview?.rating ?: 0,
            initialReviewText = myReview?.reviewText ?: "",
            isEditing = myReview != null,
            isSubmitting = isSubmittingReview,
            errorMsg = reviewSubmitError,
            onDismiss = {
                showWriteReview = false
                viewModel.clearReviewFeedback()
            },
            onSubmit = { rating, text ->
                viewModel.submitReview(wine.id, rating, text, userName)
            }
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
            .hoverScale()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = wine.imageUrl,
                contentDescription = "${wine.name} Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = wine.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            WineRatingBadge(averageRating = wine.averageRating, reviewCount = wine.reviewCount)
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${wine.type} - ${wine.country}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = wine.technicalDetails,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
fun WineDetailPopup(
    wine: Wine,
    viewModel: WinePageViewModel,
    isLoggedIn: Boolean,
    userName: String?,
    userEmail: String?,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            WineDetailContent(
                wine = wine,
                viewModel = viewModel,
                isLoggedIn = isLoggedIn,
                userName = userName,
                userEmail = userEmail,
                onCloseClick = onDismissRequest
            )
        }
    }
}

@Composable
fun WineDetailContent(
    wine: Wine,
    viewModel: WinePageViewModel,
    isLoggedIn: Boolean,
    userName: String?,
    userEmail: String?,
    onCloseClick: () -> Unit
) {
    val reviews by viewModel.reviewsForSelectedWine.collectAsState()
    val myReview by viewModel.myReviewForSelectedWine.collectAsState()
    val isLoadingReviews by viewModel.isLoadingReviews.collectAsState()
    val hasFetchedReviews by viewModel.hasFetchedReviews.collectAsState()
    val isSubmittingReview by viewModel.isSubmittingReview.collectAsState()
    val isDeletingReview by viewModel.isDeletingReview.collectAsState()
    val reviewSubmitError by viewModel.reviewSubmitError.collectAsState()
    val isFlaggingReview by viewModel.isFlaggingReview.collectAsState()
    val flagSuccess by viewModel.flagSuccess.collectAsState()
    val flagError by viewModel.flagError.collectAsState()
    var showWriteReview by rememberSaveable { mutableStateOf(false) }

    // Close dialog when success
    val reviewSubmitSuccess by viewModel.reviewSubmitSuccess.collectAsState()
    if (reviewSubmitSuccess) {
        showWriteReview = false
        viewModel.clearReviewFeedback()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WineRatingBadge(averageRating = wine.averageRating, reviewCount = wine.reviewCount)
            }
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
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
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

        ReviewsSection(
            wineId = wine.id,
            isLoggedIn = isLoggedIn,
            userName = userName,
            userEmail = userEmail,
            reviews = reviews,
            myReview = myReview,
            isLoading = isLoadingReviews,
            isDeletingReview = isDeletingReview,
            hasFetchedReviews = hasFetchedReviews,
            isFlaggingReview = isFlaggingReview,
            flagSuccess = flagSuccess,
            flagError = flagError,
            onWriteReviewClick = { showWriteReview = true },
            onEditReviewClick = { showWriteReview = true },
            onDeleteReview = { viewModel.deleteMyReview(it) },
            onFlagReview = { email, reason -> viewModel.flagReview(wine.id, email, reason) },
            onClearFlagFeedback = { viewModel.clearFlagFeedback() }
        )
    }

    if (showWriteReview) {
        WriteReviewDialog(
            wineName = wine.name,
            initialRating = myReview?.rating ?: 0,
            initialReviewText = myReview?.reviewText ?: "",
            isEditing = myReview != null,
            isSubmitting = isSubmittingReview,
            errorMsg = reviewSubmitError,
            onDismiss = {
                showWriteReview = false
                viewModel.clearReviewFeedback()
            },
            onSubmit = { rating, text ->
                viewModel.submitReview(wine.id, rating, text, userName)
            }
        )
    }
}

@Composable
fun CompactScreenWinePage(
    favoriteWines: List<Wine>,
    selectedWine: Wine?,
    viewModel: WinePageViewModel,
    isLoggedIn: Boolean,
    userName: String?,
    userEmail: String?,
    modifier: Modifier = Modifier,
    recommendations: List<WineRecommendation> = emptyList(),
    isFetchingRecs: Boolean = false,
    isLoaded: Boolean = false,
    recPanelExpanded: Boolean = false,
    onRecPanelExpandedChange: (Boolean) -> Unit = {}
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            SearchBar(
                label = "Search our wine list",
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )
        }

        // Only show panel if there are results or a fetch is in flight
        if (isLoggedIn && (recommendations.isNotEmpty() || isFetchingRecs)) {
            item {
                WineRecommendationPanel(
                    recommendations = recommendations,
                    isLoading = isFetchingRecs,
                    expanded = recPanelExpanded,
                    onExpandedChange = onRecPanelExpandedChange,
                    onWineClick = { name ->
                        val match = favoriteWines.firstOrNull {
                            it.name.equals(name, ignoreCase = true)
                        }
                        if (match != null) {
                            viewModel.setSelectedWine(match)
                            showDialog = true
                        }
                    }
                )
            }
        }

        item {
            Text(
                text = "Our Favorites",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (!isLoaded) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading wines",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    LinearProgressBar()
                }
            }
        } else if (favoriteWines.isEmpty()) {
            item {
                ComingSoonPlaceholder(
                    title = "Wine Collection",
                    subtitle = "Our curated wine list is being prepared. Check back soon!"
                )
            }
        } else {
            itemsIndexed(favoriteWines) { index, wine ->
                GridItemReveal(index = index, animationKey = searchQuery) {
                    WineCard(
                        wine = wine,
                        isFavorite = true,
                        onClick = {
                            hapticVibrate(HapticDuration.TICK, HapticCategory.DIALOGS)
                            viewModel.setSelectedWine(wine)
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        selectedWine?.let { wine ->
            WineDetailPopup(
                wine = wine,
                viewModel = viewModel,
                isLoggedIn = isLoggedIn,
                userName = userName,
                userEmail = userEmail,
                onDismissRequest = { 
                    showDialog = false 
                    viewModel.clearSelectedWine()
                }
            )
        }
    }
}

@Composable
fun LargeScreenWinePage(
    filteredWines: List<Wine>,
    selectedWine: Wine?,
    viewModel: WinePageViewModel,
    isLoggedIn: Boolean,
    userName: String?,
    userEmail: String?,
    listFraction: Float = 0.30f,
    recommendations: List<WineRecommendation> = emptyList(),
    isFetchingRecs: Boolean = false,
    isLoaded: Boolean = false,
    recPanelExpanded: Boolean = false,
    onRecPanelExpandedChange: (Boolean) -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth(listFraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchBar(
                label = "Search our wine list",
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            if (!isLoaded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading wines",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    LinearProgressBar()
                }
            } else if (filteredWines.isEmpty()) {
                ComingSoonPlaceholder(
                    title = "Wine Collection",
                    subtitle = "Our curated wine list is being prepared. Check back soon!"
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
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                    val wineListState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state          = wineListState,
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(filteredWines) { wine ->
                                ScrollReveal {
                                    WineListItem(
                                        wine = wine,
                                        isSelected = (wine == selectedWine),
                                        onClick = { viewModel.setSelectedWine(wine) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        VerticalScrollbar(
                            adapter  = rememberScrollbarAdapter(wineListState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 2.dp),
                            style    = wdwScrollbarStyle()
                        )
                    }
                }
            }
        }

            Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Only show panel if there are results or a fetch is in flight
            if (isLoggedIn && (recommendations.isNotEmpty() || isFetchingRecs)) {
                WineRecommendationPanel(
                    recommendations = recommendations,
                    isLoading = isFetchingRecs,
                    expanded = recPanelExpanded,
                    onExpandedChange = onRecPanelExpandedChange,
                    onWineClick = { name ->
                        viewModel.setSelectedWine(
                            filteredWines.firstOrNull {
                                it.name.equals(name, ignoreCase = true)
                            }
                        )
                    }
                )
            }
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
            ){
                Crossfade(
                    targetState  = selectedWine,
                    animationSpec = tween(durationMillis = 280),
                    label        = "wineDetailCrossfade"
                ) { wine ->
                    if (wine == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a wine to see details",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        WineDetail(
                            wine = wine,
                            viewModel = viewModel,
                            isLoggedIn = isLoggedIn,
                            userName = userName,
                            userEmail = userEmail
                        )
                    }
                }
            }
        }
    }
}

// ─── Vino Wine Recommendation Panel ─────────────────────────────────────────

@Composable
fun WineRecommendationPanel(
    recommendations: List<WineRecommendation>,
    isLoading: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onWineClick: (String) -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // ── Orange pill trigger ───────────────────────────────────────
        Card(
            modifier = Modifier
                .clickable { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(
                containerColor = if (expanded) WdwOrange else WdwOrange.copy(alpha = 0.18f)
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isLoading) "✨ Finding your next wine…"
                           else if (expanded) "✨ Vino's Picks  ▲"
                           else "✨ Vino's Picks",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (expanded) Color.White else WdwOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Recommendation cards (only when expanded) ─────────────────
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = WdwOrange
                )
            } else {
                recommendations.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onWineClick(rec.name) }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Orange left accent strip
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(IntrinsicSize.Min)
                                .background(WdwOrange)
                                .fillMaxHeight()
                        )
                        Column(
                            modifier = Modifier.padding(
                                start = 10.dp, end = 12.dp,
                                top = 8.dp, bottom = 8.dp
                            )
                        ) {
                            Text(
                                text = rec.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = WdwOrange,
                                fontWeight = FontWeight.Bold
                            )
                            if (rec.wine_type.isNotEmpty() || rec.region.isNotEmpty()) {
                                Text(
                                    text = listOf(rec.wine_type, rec.region)
                                        .filter { it.isNotEmpty() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Text(
                                text = rec.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
