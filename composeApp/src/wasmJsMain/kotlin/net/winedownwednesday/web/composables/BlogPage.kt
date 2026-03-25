package net.winedownwednesday.web.composables

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.viewmodels.BlogPageViewModel
import org.koin.compose.koinInject

@Composable
fun BlogPage(
    sizeInfo: WindowSizeInfo,
    viewModel: BlogPageViewModel = koinInject()
) {
    val blogPosts by viewModel.blogPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Simple state to hold the currently selected post for reading, null means list view.
    var selectedPost by remember { mutableStateOf<BlogPost?>(null) }
    
    val padding = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> 16.dp
        WidthClass.Medium   -> 24.dp
        WidthClass.Expanded -> 48.dp
        WidthClass.Large    -> 80.dp
        WidthClass.XLarge   -> 120.dp
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
        if (isLoading && blogPosts.isNullOrEmpty()) {
            LinearProgressBar()
        } else {
            if (selectedPost != null) {
                // Detail View - Centered and constrained for reading
                LazyColumn(
                    contentPadding = PaddingValues(top = padding, bottom = padding + 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(modifier = Modifier.widthIn(max = 800.dp).padding(horizontal = padding)) {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 24.dp)
                                    .clickable { selectedPost = null },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = Color(0xFFFF7F33),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Back to Posts",
                                    color = Color(0xFFFF7F33),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            if (selectedPost!!.coverImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = selectedPost!!.coverImageUrl,
                                    contentDescription = "Cover image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f/9f)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text(
                                text = selectedPost!!.title,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "By ${selectedPost!!.author} • ${(selectedPost!!.publishedAt ?: selectedPost!!.createdAt).take(10)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            BlogPostContent(blocks = selectedPost!!.content)
                        }
                    }
                }
            } else {
                // List View - Grid or Centered List
                val blogListState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state               = blogListState,
                        contentPadding      = PaddingValues(padding),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth()) {
                                Text(
                                    text = "Tasting Notes",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = Color.White
                                )
                                Text(
                                    text = "Stories, news, and insights from the club.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                        }

                        val posts = blogPosts ?: emptyList()
                        if (posts.isEmpty()) {
                            item {
                                Text(
                                    text = "No posts available at the moment.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.LightGray
                                )
                            }
                        } else {
                            items(posts) { post ->
                                Box(modifier = Modifier.widthIn(max = 800.dp)) {
                                    ScrollReveal {
                                        BlogSummaryCard(post = post, onClick = { selectedPost = post })
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter  = rememberScrollbarAdapter(blogListState),
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
}

@Composable
fun BlogSummaryCard(post: BlogPost, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            if (post.coverImageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.coverImageUrl,
                    contentDescription = "Cover Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.summary.replace(Regex("""[*#_~`]"""), ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    lineHeight = 24.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read Article",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF7F33),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = (post.publishedAt ?: post.createdAt).take(10),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
