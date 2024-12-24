package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.profile1
import wdw_web.composeapp.generated.resources.profile2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MembersPage() {

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    val viewModel: MembersPageViewModel = koinInject()
    val members by viewModel.members.collectAsState()
    val memberSections by viewModel.allMemberSections.collectAsState()

    println("Collected members: $members")

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black),
    ) {
        MemberHeroSection()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (memberSections.isEmpty()) {
                item {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(memberSections) { section ->
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        section.members.forEach { member ->
                            MemberCard(
                                member = member,
                                modifier = Modifier.width(200.dp),
                                onClick = { selectedMember = member }
                            )
                        }
                    }
                }
            }



        }
        if (selectedMember != null) {
            MemberDetailPopup(member = selectedMember!!) {
                selectedMember = null
            }
        }
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
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(
                    Res.drawable.profile1
                ),
                contentDescription = "${member.name}'s photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                alignment = Alignment.Center
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
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismissRequest() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(
                            Res.drawable.profile2
                        ),
                        contentDescription = "${member.name}'s photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
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

                Text(
                    "Click outside this card to close",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun MemberDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MemberHeroSection(
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(50.dp, 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Meet our Members",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text(
                text = "The people and the stories behind Wine Down.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}