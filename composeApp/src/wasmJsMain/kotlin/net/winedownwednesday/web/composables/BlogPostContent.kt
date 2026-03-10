package net.winedownwednesday.web.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import net.winedownwednesday.web.data.models.ContentBlock

@Composable
fun BlogPostContent(
    blocks: List<ContentBlock>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Paragraph -> {
                    Markdown(
                        content = block.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.displayMedium
                        2 -> MaterialTheme.typography.headlineLarge
                        3 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(
                        text = block.text,
                        style = style,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
                    )
                }
                is ContentBlock.Image -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        AsyncImage(
                            model = block.imageUrl,
                            contentDescription = block.caption,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!block.caption.isNullOrBlank()) {
                            Text(
                                text = block.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        }
                    }
                }
                is ContentBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color(0xFFFF7F33),
                                modifier = Modifier.fillMaxWidth().fillMaxHeight()
                            ) {}
                        }
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.headlineSmall,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(start = 24.dp, end = 16.dp),
                            color = Color.LightGray
                        )
                    }
                }
                is ContentBlock.ListBlock -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        block.items.forEachIndexed { index, item ->
                            val prefix = if (block.ordered) "${index + 1}." else "•"
                            Row {
                                Text(
                                    text = "$prefix ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF7F33)
                                )
                                // Use Markdown here in case list items contain inner formatting (bold/links)
                                Markdown(
                                    content = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is ContentBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
