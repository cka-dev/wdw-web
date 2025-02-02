package net.winedownwednesday.web.composables

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.MemberSection
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.matchesQuery
import org.koin.compose.koinInject

@Composable
fun MembersPage(
    isCompactScreen: Boolean
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
        )
    } else {
        LargeScreenMemberPage(
            memberSections= memberSections,
            selectedMember = selectedMember,
            onSelectedMemberChange = { it?.let {
                viewModel.setSelectedMember(it)
            } },
            onDismissRequest = {viewModel.clearSelectedMember()},
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
fun MemberDetailPopup(member: Member, onDismissRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable { onDismissRequest() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.4f)
                .fillMaxHeight(0.8f)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            IconButton(
                onClick = { onDismissRequest() },
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = member.profilePictureUrl,
                        contentDescription = "${member.name}'s profile picture",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RectangleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(member.name, style = MaterialTheme.typography.headlineSmall)
                        Text(member.role, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                MemberDetailRow("Email", member.email)
                MemberDetailRow("Phone", member.phoneNumber)
                MemberDetailRow("Birthday", member.birthday)
                MemberDetailRow("Profession", member.profession)
                MemberDetailRow("Company", member.company)
                member.business?.takeIf { it.isNotEmpty() }?.let {
                    MemberDetailRow("Business", it)
                }
                MemberDetailRow(
                    "Interests/Hobbies",
                    member.interests.joinToString(separator = ", ")
                )
                MemberDetailRow(
                    "Favorite Wines",
                    member.favoriteWines.joinToString(separator = ", ")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
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
        CompactMemberDetailPopup(
            member = selectedMember,
            onDismissRequest = { onDismissRequest() }
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
                    .size(175.dp)
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
fun CompactMemberDetailPopup(member: Member, onDismissRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismissRequest)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            MemberDetailContent(member = member, onCloseClick = onDismissRequest)
        }
    }
}

@Composable
fun MemberDetailContent(member: Member, onCloseClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = member.name,
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
            model = member.profilePictureUrl,
            contentDescription = "${member.name}'s profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        MemberDetailRow(label = "Role", value = member.role)
        MemberDetailRow(label = "Email", value = member.email)
        MemberDetailRow(label = "Phone", value = member.phoneNumber)
        MemberDetailRow(label = "Birthday", value = member.birthday)
        MemberDetailRow(label = "Profession", value = member.profession)
        MemberDetailRow(label = "Company", value = member.company)
        if (!member.business.isNullOrEmpty()) {
            MemberDetailRow(label = "Business", value = member.business)
        }
        MemberDetailRow(
            label = "Interests/Hobbies",
            value = member.interests.joinToString(separator = ", ")
        )
        MemberDetailRow(
            label = "Favorite Wines",
            value = member.favoriteWines.joinToString(separator = ", ")
        )
    }
}

@Composable
fun MemberDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LargeScreenMemberPage(
    memberSections: List<MemberSection>,
    selectedMember: Member?,
    onSelectedMemberChange: (Member?) -> Unit,
    onDismissRequest: () -> Unit,
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
            MemberDetailPopup(
                member = selectedMember,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

