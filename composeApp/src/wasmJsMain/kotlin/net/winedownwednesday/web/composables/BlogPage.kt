package net.winedownwednesday.web.composables

import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.data.models.ContentBlock
import net.winedownwednesday.web.viewmodels.BlogPageViewModel
import org.kodein.emoji.compose.m3.TextWithNotoImageEmoji
import org.koin.compose.koinInject

@Composable
fun BlogPage(
    sizeInfo: WindowSizeInfo,
    viewModel: BlogPageViewModel = koinInject()
) {
    val blogPosts by viewModel.blogPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val summaries by viewModel.summaries.collectAsState()
    val summarizing by viewModel.summarizing.collectAsState()
    val selectedPost by viewModel.selectedPost.collectAsState()
    val showTldr by viewModel.showTldr.collectAsState()

    // Deep-link: auto-select a blog post by ID
    val pendingPostId by viewModel.pendingPostId.collectAsState()
    LaunchedEffect(blogPosts, pendingPostId) {
        val id = pendingPostId ?: return@LaunchedEffect
        val match = blogPosts?.firstOrNull { it.id == id }
        if (match != null) {
            viewModel.selectPost(match)
            viewModel.clearPendingPostId()
        }
    }

    // URL sync for selected post
    DisposableEffect(selectedPost?.id) {
        val post = selectedPost
        if (post != null) {
            val hash = "#blog?postId=${post.id}"
            kotlinx.browser.window.history.pushState(
                null?.toJsString(), "", hash
            )
        }
        onDispose {
            if (selectedPost == null) {
                kotlinx.browser.window.history.pushState(
                    null?.toJsString(), "", "#blog"
                )
            }
        }
    }

    // Auto-trigger summarization when a post is opened
    LaunchedEffect(selectedPost) {
        viewModel.setShowTldr(false)  // collapse when switching posts
        val post = selectedPost ?: return@LaunchedEffect
        val bodyText = post.content.joinToString(" ") { block ->
            when (block) {
                is ContentBlock.Paragraph  -> block.text
                is ContentBlock.Heading    -> block.text
                is ContentBlock.Quote      -> block.text
                is ContentBlock.ListBlock  -> block.items.joinToString(" ")
                else -> ""
            }
        }.trim()
        if (bodyText.length >= 50) {
            viewModel.summarizePost(post.id, bodyText)
        }
    }

    val padding = when (sizeInfo.widthClass) {
        WidthClass.Compact  -> 16.dp
        WidthClass.Medium   -> 24.dp
        WidthClass.Expanded -> 48.dp
        WidthClass.Large    -> 80.dp
        WidthClass.XLarge   -> 120.dp
    }

    val isTouchDevice = LocalIsTouchDevice.current
    var isRefreshing by remember { mutableStateOf(false) }

    val blogContent: @Composable () -> Unit = {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
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
                                    .clickable { viewModel.selectPost(null) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = WdwOrange,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Back to Posts",
                                    color = WdwOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Share actions
                            var shareConfirmation by remember {
                                mutableStateOf<String?>(null)
                            }
                            var showQrDialog by remember {
                                mutableStateOf(false)
                            }
                            val shareUrl = buildShareUrl(
                                "blog", selectedPost!!.id
                            )
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        val usedNative = shareOrCopy(
                                            url = shareUrl,
                                            title = selectedPost!!.title,
                                            text = "Check out: " +
                                                selectedPost!!.title
                                        )
                                        shareConfirmation =
                                            if (usedNative) null
                                            else "Link copied!"
                                    }
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            if (shareConfirmation != null)
                                                Icons.Default.Check
                                            else Icons.Default.Share,
                                        contentDescription =
                                            "Share post",
                                        tint =
                                            if (shareConfirmation != null)
                                                Color(0xFF4CAF50)
                                            else WdwOrange
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        showQrDialog = true
                                    }
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            Icons.Default.QrCode2,
                                        contentDescription =
                                            "Show QR code",
                                        tint = WdwOrange
                                    )
                                }
                            }
                            if (shareConfirmation != null) {
                                LaunchedEffect(shareConfirmation) {
                                    kotlinx.coroutines.delay(1500)
                                    shareConfirmation = null
                                }
                            }
                            if (showQrDialog) {
                                QrCodeDialog(
                                    url = shareUrl,
                                    title = selectedPost!!.title,
                                    onDismiss = {
                                        showQrDialog = false
                                    }
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
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "By ${selectedPost!!.author} • ${(selectedPost!!.publishedAt ?: selectedPost!!.createdAt).take(10)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                             // ── TL;DR pill ───────────────────────────────────────────
                             val postId = selectedPost!!.id
                             val summary = summaries[postId]
                             val isSummarizing = summarizing.contains(postId)

                             if (summary != null || isSummarizing) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     modifier = Modifier.padding(bottom = 8.dp)
                                 ) {
                                     Card(
                                         modifier = Modifier.clickable { viewModel.setShowTldr(!showTldr) },
                                         shape = RoundedCornerShape(50),
                                         colors = CardDefaults.cardColors(
                                             containerColor = if (showTldr)
                                                 WdwOrange
                                             else
                                                 MaterialTheme.colorScheme.surfaceVariant
                                         )
                                     ) {
                                         Row(
                                             modifier = Modifier.padding(
                                                 horizontal = 10.dp, vertical = 6.dp
                                             ),
                                             verticalAlignment = Alignment.CenterVertically,
                                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             Box(
                                                 modifier = Modifier
                                                     .size(16.dp)
                                                     .clip(CircleShape),
                                                 contentAlignment = Alignment.Center
                                             ) {
                                                 AsyncImage(
                                                     model = VinoAvatarUrl,
                                                     contentDescription = "Vino",
                                                     modifier = Modifier.fillMaxSize(),
                                                     contentScale = ContentScale.Crop
                                                 )
                                             }
                                             Text(
                                                 text = if (isSummarizing) "Summarizing…"
                                                        else if (showTldr) "TL;DR ▲"
                                                        else "TL;DR",
                                                 style = MaterialTheme.typography.labelMedium,
                                                 color = if (showTldr) Color.White else WdwOrange
                                             )
                                         }
                                     }
                                 }
                                 if (showTldr && summary != null) {
                                     Row(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .padding(bottom = 16.dp)
                                             .clip(RoundedCornerShape(8.dp))
                                             .background(MaterialTheme.colorScheme.surfaceVariant)
                                     ) {
                                         // Orange left accent strip
                                         Box(
                                             modifier = Modifier
                                                 .width(3.dp)
                                                 .fillMaxHeight()
                                                 .background(WdwOrange)
                                         )
                                         Text(
                                             text = summary,
                                             style = MaterialTheme.typography.bodyMedium,
                                             color = MaterialTheme.colorScheme.onSurface,
                                             modifier = Modifier.padding(12.dp)
                                         )
                                     }
                                 }
                             }

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
                        contentPadding      = PaddingValues(
                            start = padding, end = padding,
                            top = padding, bottom = padding + 100.dp
                        ),
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Stories, news, and insights from the club.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                        }

                        val posts = blogPosts ?: emptyList()
                        if (posts.isEmpty()) {
                            item {
                                ComingSoonPlaceholder(
                                    title = "Tasting Notes Blog",
                                    subtitle = "Our blog posts are being crafted. Check back soon for stories, news, and insights from the club.",
                                    emoji = "📝"
                                )
                            }
                        } else {
                            items(posts) { post ->
                                Box(modifier = Modifier.widthIn(max = 800.dp)) {
                                    ScrollReveal {
                                        BlogSummaryCard(post = post, onClick = { viewModel.selectPost(post) })
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

    if (isTouchDevice) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refresh { isRefreshing = false }
            },
            modifier = Modifier.fillMaxSize()
        ) { blogContent() }
    } else {
        blogContent()
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.summary.replace(Regex("""[*#_~`]"""), ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                        color = WdwOrange,
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
