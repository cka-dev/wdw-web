package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.MemberSection
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.viewmodels.LoginUIState
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import net.winedownwednesday.web.vibrate
import org.koin.compose.koinInject

// ---------------------------------------------------------------------------
// MembersPage — adaptive list + detail
// ---------------------------------------------------------------------------
@Composable
fun MembersPage(
    sizeInfo: WindowSizeInfo,
    uiState: LoginUIState,
    userProfileData: UserProfileData?
) {
    val viewModel: MembersPageViewModel = koinInject()
    val memberSections by viewModel.allMemberSections.collectAsState()
    val selectedMember by viewModel.selectedMember.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()

    if (sizeInfo.useTwoColumnLayout) {
        // ── Wide: persistent side-by-side list + detail ──────────────────
        MembersListDetailLayout(
            memberSections   = memberSections,
            selectedMember   = selectedMember,
            searchQuery      = searchQuery,
            onMemberClick    = {
                hapticVibrate(HapticDuration.TICK, HapticCategory.DIALOGS)
                viewModel.setSelectedMember(it)
            },
            onDetailClose    = { viewModel.clearSelectedMember() },
            onSearchChange   = { viewModel.setSearchQuery(it) },
            uiState          = uiState,
            userProfileData  = userProfileData,
        )
    } else {
        // ── Narrow: grid + modal dialog ───────────────────────────────────
        MembersGridLayout(
            memberSections  = memberSections,
            selectedMember  = selectedMember,
            searchQuery     = searchQuery,
            onMemberClick   = {
                hapticVibrate(HapticDuration.TICK, HapticCategory.DIALOGS)
                viewModel.setSelectedMember(it)
            },
            onDismiss       = { viewModel.clearSelectedMember() },
            onSearchChange  = { viewModel.setSearchQuery(it) },
            uiState         = uiState,
            userProfileData = userProfileData,
        )
    }
}

// ---------------------------------------------------------------------------
// Wide screen — list pane left, detail pane right (no modal)
// ---------------------------------------------------------------------------
@Composable
private fun MembersListDetailLayout(
    memberSections:  List<MemberSection>,
    selectedMember:  Member?,
    searchQuery:     String,
    onMemberClick:   (Member) -> Unit,
    onDetailClose:   () -> Unit,
    onSearchChange:  (String) -> Unit,
    uiState:         LoginUIState,
    userProfileData: UserProfileData?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullWidth = maxWidth

        // Pre-calculate the target column count based on the FINAL grid width
        // so the grid restructures on frame 1, not mid-animation
        val targetGridWidth = if (selectedMember != null) fullWidth * 0.55f else fullWidth
        val horizontalPadding = 40.dp  // 16.dp start + 24.dp end
        val spacing = 12.dp
        val minSize = 260.dp
        val targetColumns = maxOf(
            1,
            ((targetGridWidth - horizontalPadding + spacing) / (minSize + spacing)).toInt()
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Left: scrollable member grid ─────────────────────────────────
            val gridState = rememberLazyGridState()

            // Coordinated weight animation: both weights always sum to 1.0
            val detailFraction by animateFloatAsState(
                targetValue   = if (selectedMember != null) 0.45f else 0f,
                animationSpec = tween(800),
                label         = "detailFraction"
            )
            // Remember the last selected member so content stays visible during exit
            var displayedMember by remember { mutableStateOf<Member?>(null) }
            if (selectedMember != null) displayedMember = selectedMember
            // Clear once animation finishes fully
            if (detailFraction == 0f) displayedMember = null

            Column(
                modifier = Modifier
                    .weight(1f - detailFraction.coerceAtMost(0.449f))
                    .fillMaxHeight()
            ) {
                SearchBar(
                    label         = "Search members",
                    query         = searchQuery,
                    onQueryChange = onSearchChange,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(targetColumns),
                        state                 = gridState,
                        modifier              = Modifier.fillMaxSize(),
                        contentPadding        = PaddingValues(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        memberSections.forEach { section ->
                            item(
                                key  = "header_${section.title}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                MemberSectionHeader(section.title)
                            }
                            itemsIndexed(
                                items = section.members.filter { it.matchesQuery(searchQuery) },
                                key   = { _, member -> member.id }
                            ) { index, member ->
                                GridItemReveal(
                                    index        = index,
                                    animationKey = searchQuery,
                                    modifier     = Modifier.animateItem(
                                        placementSpec = tween(800),
                                        fadeInSpec    = tween(800),
                                        fadeOutSpec   = tween(800),
                                    )
                                ) {
                                    MemberCard(
                                        member     = member,
                                        isSelected = member == selectedMember,
                                        onClick    = { onMemberClick(member) },
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

            // ── Right: detail pane (coordinated weight animation) ─────────
            if (detailFraction > 0.001f) {
                Box(
                    modifier = Modifier
                        .weight(detailFraction.coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .graphicsLayer { alpha = (detailFraction / 0.45f).coerceIn(0f, 1f) }
                ) {
                    displayedMember?.let { member ->
                        MemberDetailPane(
                            member          = member,
                            onClose         = onDetailClose,
                            uiState         = uiState,
                            userProfileData = userProfileData,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Narrow screen — grid + dialog on tap
// ---------------------------------------------------------------------------
@Composable
private fun MembersGridLayout(
    memberSections:  List<MemberSection>,
    selectedMember:  Member?,
    searchQuery:     String,
    onMemberClick:   (Member) -> Unit,
    onDismiss:       () -> Unit,
    onSearchChange:  (String) -> Unit,
    uiState:         LoginUIState,
    userProfileData: UserProfileData?,
) {
    val gridState = rememberLazyGridState()
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SearchBar(
            label         = "Search our members directory",
            query         = searchQuery,
            onQueryChange = onSearchChange,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns              = GridCells.Adaptive(minSize = 140.dp),
                state                = gridState,
                modifier             = Modifier.fillMaxSize(),
                contentPadding       = PaddingValues(16.dp),
                verticalArrangement  = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                memberSections.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MemberSectionHeader(section.title)
                    }
                    itemsIndexed(
                        section.members.filter { it.matchesQuery(searchQuery) }
                    ) { index, member ->
                        GridItemReveal(index = index, animationKey = searchQuery) {
                            MemberCard(
                                member    = member,
                                isSelected = false,
                                onClick   = { onMemberClick(member) },
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
                    .padding(end = 2.dp),
                style = wdwScrollbarStyle()
            )
        }
    }

    if (selectedMember != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier  = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape     = RoundedCornerShape(16.dp),
                color     = MaterialTheme.colorScheme.surface,
            ) {
                MemberDetailPane(
                    member          = selectedMember,
                    onClose         = onDismiss,
                    uiState         = uiState,
                    userProfileData = userProfileData,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Single unified MemberCard — replaces MemberCard + CompactMemberCard
// ---------------------------------------------------------------------------
@Composable
fun MemberCard(
    member:     Member,
    isSelected: Boolean = false,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier,
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .hoverScale()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 4.dp),
        border    = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF7F33))
        else null,
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center,
        ) {
            AsyncImage(
                model              = member.profilePictureUrl,
                contentDescription = "${member.name}'s photo",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = member.name,
                style     = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                color     = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text      = member.role,
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Member detail — used both as a pane and inside the dialog
// ---------------------------------------------------------------------------
@Composable
private fun MemberDetailPane(
    member:          Member,
    onClose:         () -> Unit,
    uiState:         LoginUIState,
    userProfileData: UserProfileData?,
    modifier:        Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showFullPhoto by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(20.dp),
        ) {
        // Header: name + close button
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text      = member.name,
                style     = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onSurface,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Close",
                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Photo + role/email row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.Top,
        ) {
            AsyncImage(
                model              = member.profilePictureUrl,
                contentDescription = "${member.name}'s photo",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .clickable { showFullPhoto = true },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier            = Modifier.weight(1f),
            ) {
                MemberDetailField("Role",  member.role)
                MemberDetailField("Email", member.email)
                AnimatedVisibility(
                    visible = userProfileData?.isMember == true
                           && uiState == LoginUIState.Authenticated
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MemberDetailField("Phone",    member.phoneNumber)
                        MemberDetailField("Birthday", member.birthday)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))

        // Extended fields
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberDetailField("Profession", member.profession)
            MemberDetailField("Company",   member.company)
            member.business?.takeIf { it.isNotEmpty() }?.let {
                MemberDetailField("Business", it)
            }
            MemberDetailField(
                "Interests / Hobbies",
                member.interests.joinToString(", ")
            )
            MemberDetailField(
                "Favourite Wines",
                member.favoriteWines.joinToString(", ")
            )
        }
        }  // end Column
        VerticalScrollbar(
            adapter  = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 2.dp),
            style = wdwScrollbarStyle()
        )
    }  // end Box

    // Full-size profile photo dialog
    if (showFullPhoto) {
        Dialog(onDismissRequest = { showFullPhoto = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable { showFullPhoto = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model              = member.profilePictureUrl,
                    contentDescription = "${member.name}'s full photo",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}


// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
@Composable
private fun MemberSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color    = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun MemberDetailField(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            fontWeight = FontWeight.Medium,
        )
        Text(
            text     = value,
            fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Keep for backward compatibility with any remaining call sites
@Composable
fun MemberDetailRow(label: String, value: String) = MemberDetailField(label, value)
