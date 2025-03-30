package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.MemberSection
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.viewmodels.LoginUIState
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject

@Composable
fun MembersPage(
    isCompactScreen: Boolean,
    uiState: LoginUIState,
    userProfileData: UserProfileData?
) {

    val viewModel: MembersPageViewModel = koinInject()
    val memberSections by viewModel.allMemberSections.collectAsState()
    val selectedMember by viewModel.selectedMember.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    if (isCompactScreen) {
        CompactScreenMembersPage(
            allMembers= memberSections,
            selectedMember = selectedMember,
            searchQuery = searchQuery,
            onSelectedMemberChange = { it?.let {
                viewModel.setSelectedMember(it)
            } },
            onDismissRequest = {viewModel.clearSelectedMember()},
            onSearchQueryChange = {viewModel.setSearchQuery(it)},
            uiState = uiState,
            userProfileData = userProfileData,
        )
    } else {
        LargeScreenMemberPage(
            memberSections= memberSections,
            selectedMember = selectedMember,
            onSelectedMemberChange = { it?.let {
                viewModel.setSelectedMember(it)
            } },
            onDismissRequest = {viewModel.clearSelectedMember()},
            uiState = uiState,
            userProfileData = userProfileData,
        )
    }

}

@Composable
fun MemberCard(
    member: Member,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF282828)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = member.profilePictureUrl,
                contentDescription = "${member.name}' s profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactScreenMembersPage(
    allMembers: List<MemberSection>,
    selectedMember: Member?,
    searchQuery: String,
    onSelectedMemberChange: (Member?) -> Unit = {},
    onDismissRequest: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SearchBar(
            label = "Search our members directory",
            query = searchQuery,
            onQueryChange = {onSearchQueryChange(it)},
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            allMembers.forEach { section ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(section.members.filter {
                    it.matchesQuery(searchQuery)
                }) { member ->
                    CompactMemberCard(
                        member = member,
                        onClick = {
                            onSelectedMemberChange(member)
                        }
                    )
                }
            }
        }
    }

    if (selectedMember != null) {
        CompactMemberDetailDialog(
            member = selectedMember,
            onDismissRequest = { onDismissRequest() },
            uiState = uiState,
            userProfileData = userProfileData,
        )
    }
}

@Composable
fun CompactMemberCard(
    member: Member,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = member.profilePictureUrl,
                contentDescription = "${member.name}'s profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                alignment = Alignment.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactMemberDetailDialog(
    member: Member,
    onDismissRequest: () -> Unit,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            CompactMemberDetailContent(member, uiState,userProfileData, onDismissRequest)
        }
    }
}

@Composable
fun CompactMemberDetailContent(
    member: Member,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis

            )
            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AsyncImage(
            model = member.profilePictureUrl,
            contentDescription = "${member.name}'s profile picture",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MemberDetailRow("Role", member.role)
            MemberDetailRow("Email", member.email)
            AnimatedVisibility(visible = (userProfileData?.isMember == true && uiState == LoginUIState.Authenticated)) {
                MemberDetailRow("Phone", member.phoneNumber)
            }
            AnimatedVisibility(visible = (userProfileData?.isMember == true && uiState == LoginUIState.Authenticated)) {

                MemberDetailRow("Birthday", member.birthday)
            }
            MemberDetailRow("Profession", member.profession)
            MemberDetailRow("Company", member.company)
            member.business?.takeIf { it.isNotEmpty() }?.let { MemberDetailRow("Business", it) }
            MemberDetailRow("Interests/Hobbies", member.interests.joinToString(separator = ", "))
            MemberDetailRow("Favorite Wines", member.favoriteWines.joinToString(separator = ", "))
        }
    }
}

@Composable
fun MemberDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LargeScreenMemberDetailDialog(
    member: Member,
    onDismissRequest: () -> Unit,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            LargeScreenMemberDetailContent(member,uiState, userProfileData, onDismissRequest)
        }
    }
}

@Composable
fun LargeScreenMemberDetailContent(
    member: Member,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = member.profilePictureUrl,
                contentDescription = "${member.name}'s profile picture",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                MemberDetailRow("Role", member.role)
                MemberDetailRow("Email", member.email)
                AnimatedVisibility(visible = (userProfileData?.isMember == true && uiState == LoginUIState.Authenticated)) {
                    MemberDetailRow("Phone", member.phoneNumber)
                }

                AnimatedVisibility(visible = (userProfileData?.isMember == true && uiState == LoginUIState.Authenticated)) {
                    MemberDetailRow("Birthday", member.birthday)
                }
            }
        }

        Column( modifier = Modifier.padding(top = 16.dp)){
            MemberDetailRow("Profession", member.profession)
            MemberDetailRow("Company", member.company)
            if (!member.business.isNullOrEmpty()) {
                MemberDetailRow("Business", member.business)
            }
            MemberDetailRow("Interests/Hobbies", member.interests.joinToString(separator = ", "))
            MemberDetailRow("Favorite Wines", member.favoriteWines.joinToString(separator = ", "))
        }

    }
}

@Composable
fun LargeScreenMemberPage(
    memberSections: List<MemberSection>,
    selectedMember: Member?,
    onSelectedMemberChange: (Member?) -> Unit,
    onDismissRequest: () -> Unit,
    uiState: LoginUIState,
    userProfileData: UserProfileData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            memberSections.forEach { section ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(section.members) { member ->
                        MemberCard(
                            member = member,
                            onClick = { onSelectedMemberChange(member) },
                            modifier = Modifier
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
        if (selectedMember != null) {
            LargeScreenMemberDetailDialog(
                member = selectedMember,
                onDismissRequest = onDismissRequest,
                uiState = uiState,
                userProfileData = userProfileData
            )
        }
    }
}

