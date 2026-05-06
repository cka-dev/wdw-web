package net.winedownwednesday.web.composables

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Scales the composable up smoothly on mouse hover (web/desktop).
 * Effectively a no-op on touch-only devices.
 */
fun Modifier.hoverScale(scale: Float = 1.04f): Modifier = composed {
    var hovered by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (hovered) scale else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "hoverScale"
    )
    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Enter -> hovered = true
                        PointerEventType.Exit  -> hovered = false
                        else                   -> Unit
                    }
                }
            }
        }
}

/**
 * Animated shimmer brush for skeleton loading placeholders.
 */
@Composable
fun shimmerBrush(
    baseColor: Color      = Color.Unspecified,
    highlightColor: Color = Color.Unspecified
): Brush {
    val isDark = LocalIsDarkTheme.current
    val base      = if (baseColor      == Color.Unspecified) {
        if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
    } else baseColor
    val highlight = if (highlightColor == Color.Unspecified) {
        if (isDark) Color(0xFF404040) else Color(0xFFC8C8C8)
    } else highlightColor
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue  = 1400f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start  = Offset(translateX, 0f),
        end    = Offset(translateX + 400f, 200f)
    )
}

/**
 * Micro-drift card reveal: content fades in over 750ms with a barely-perceptible
 * 16dp lateral drift. The motion is so subtle that inner carousels/animations
 * are never visibly "moving" during the reveal — it reads purely as a fade
 * with a hint of directionality. Industry-standard for premium content grids.
 */

// Ease-out-expo: fast entry, extremely gradual settle — precise and deliberate
private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun SlideInCard(delayMs: Int = 0, content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        appeared = true
    }
    val progress by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutExpo),
        label        = "slideInProgress"
    )
    val density = LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 60.dp.toPx() } }
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = slideOffsetPx * (1f - progress)
            alpha        = progress
        }
    ) {
        content()
    }
}

/**
 * Page-level fade: uses graphicsLayer alpha (NOT AnimatedVisibility) so that HtmlView
 * elements (YouTube iframes, etc.) remain correctly positioned during the fade.
 * AnimatedVisibility's slideInVertically clips content which mispositions HTML elements.
 */
@Composable
fun FadeInPage(content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val alpha by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label        = "pageAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

/**
 * Animates dialog card content: scale 0.88→1.0 + linear alpha over ~400ms.
 * Use as: Dialog { DialogReveal { Surface/Card { content } } }
 * The dark backdrop appears instantly (browser limitation), but the card entrance is smooth.
 */
@Composable
fun DialogReveal(content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0.88f,
        animationSpec = tween(400, easing = EaseOut),
        label        = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(380, easing = LinearEasing),
        label        = "dialogAlpha"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX     = scale
            scaleY     = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Replaces the platform Dialog composable with a fully-animated in-place overlay.
 * Animates the dark scrim (fade in over 350ms) AND the dialog card (scale + fade over 380ms).
 * Must be placed inside a Box(Modifier.fillMaxSize()) parent to cover the full screen.
 *
 * Usage:
 *   Box(Modifier.fillMaxSize()) {
 *       // ... page content ...
 *       if (showDialog) {
 *           AnimatedScrimOverlay(onDismiss = { showDialog = false }) {
 *               YourDialogCard(...)
 *           }
 *       }
 *   }
 */
@Composable
fun AnimatedScrimOverlay(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scrimAlpha by animateFloatAsState(
        targetValue  = if (appeared) 0.65f else 0f,
        animationSpec = tween(350, easing = LinearEasing),
        label        = "scrimAlpha"
    )
    val dialogScale by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0.88f,
        animationSpec = tween(400, easing = EaseOut),
        label        = "dialogScale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(380, easing = LinearEasing),
        label        = "dialogAlpha"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha }
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        )
        // Animated dialog content, centered
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX     = dialogScale
                    scaleY     = dialogScale
                    this.alpha = dialogAlpha
                },
            content = content
        )
    }
}

/**
 * Bottom-up staggered reveal for grid items.
 * Each card rises from 40dp below with a stagger of 60ms per index (capped at 360ms).
 * Fade + translationY animate together over 500ms EaseOut.
 * Usage: GridItemReveal(index = index) { YourCard(...) }
 */
@Composable
fun GridItemReveal(index: Int, animationKey: Any? = Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val delayMs = 120L + minOf((index * (index + 1) / 2) * 70L, 600L)
    var appeared by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        appeared = false   // reset on key change before stagger delay
        delay(delayMs)
        appeared = true
    }
    val density = LocalDensity.current
    val offsetPx = remember(density) { with(density) { 800.dp.toPx() } }
    val progress by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(750, easing = EaseOut),
        label        = "gridItemReveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            translationY = offsetPx * (1f - progress)
            alpha        = progress
        }
    ) {
        content()
    }
}

/**
 * Scroll-triggered slide-up reveal for lazy list items.
 * Because LazyColumn only composes items when they enter the viewport,
 * LaunchedEffect(Unit) fires naturally the moment each item scrolls into view.
 * The item slides up 80dp while fading in over 500ms EaseOut.
 * (Re-animates on re-entry — intentional "live feed" feel.)
 */
@Composable
fun ScrollReveal(content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val density = LocalDensity.current
    val offsetPx = remember(density) { with(density) { 80.dp.toPx() } }
    val progress by animateFloatAsState(
        targetValue  = if (appeared) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label        = "scrollReveal"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            translationY = offsetPx * (1f - progress)
            alpha        = progress
        }
    ) {
        content()
    }
}

// ── Shared scrollbar style (visible on dark backgrounds) ─────────────────
fun wdwScrollbarStyle() = ScrollbarStyle(
    minimalHeight       = 48.dp,
    thickness           = 10.dp,
    shape               = RoundedCornerShape(5.dp),
    hoverDurationMillis = 300,
    unhoverColor        = Color(0xFFFF7F33).copy(alpha = 0.6f),
    hoverColor          = Color(0xFFFF7F33)
)

// ── Always-visible scrollbar track ───────────────────────────────────────
// Renders a subtle persistent track strip so the scrollbar gutter is always
// visible even when content doesn't overflow.  The real VerticalScrollbar
// thumb renders on top via the `scrollbar` slot.
@Composable
fun BoxScope.WdwScrollbarTrack(
    endPadding: Dp = 4.dp,
    scrollbar: @Composable BoxScope.() -> Unit,
) {
    // Persistent track background
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = endPadding)
            .width(10.dp)
            .background(
                Color(0xFFFF7F33).copy(alpha = 0.12f),
                RoundedCornerShape(5.dp)
            )
    )
    // Actual scrollbar thumb on top
    scrollbar()
}

// ── MarqueeText: auto-scrolling title for overflowing single-line text ────
/**
 * A single-line text composable that auto-scrolls horizontally when
 * the content overflows the available width.
 *
 * Auto-starts the scroll cycle after [autoStartDelayMs].
 * The scroll cycle repeats: pause → scroll right → pause → snap back.
 * Subtle edge-fade gradients indicate clipped content.
 *
 * Has NO hover interaction so it does not interfere with parent
 * composables that use [hoverScale] or similar hover effects.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    initialDelayMs: Int = 1500,
    endPauseMs: Int = 2000,
    scrollSpeedDpPerSec: Float = 60f,
    autoStartDelayMs: Int = 3000,
    fadeEdgeWidth: Dp = 24.dp,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Measure container vs text width to detect overflow
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var textWidthPx by remember { mutableIntStateOf(0) }
    val isOverflowing = textWidthPx > containerWidthPx &&
        containerWidthPx > 0

    var scrollRunning by remember { mutableStateOf(false) }

    // Auto-start after delay when overflow is detected
    LaunchedEffect(isOverflowing) {
        if (!isOverflowing) return@LaunchedEffect
        delay(autoStartDelayMs.toLong())
        scrollRunning = true
    }

    // Drive the scroll cycle
    LaunchedEffect(scrollRunning) {
        if (!scrollRunning) return@LaunchedEffect
        // Small delay for scroll state to compute maxValue
        delay(50)
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0) return@LaunchedEffect

        val speedPx = with(density) {
            scrollSpeedDpPerSec.dp.toPx()
        }
        val scrollDurationMs = (
            (maxScroll.toFloat() / speedPx) * 1000
        ).toInt().coerceIn(800, 8000)

        while (isActive) {
            // Phase 1: initial pause
            delay(initialDelayMs.toLong())

            // Phase 2: scroll to end
            scrollState.animateScrollTo(
                maxScroll,
                animationSpec = tween(
                    durationMillis = scrollDurationMs,
                    easing = LinearEasing
                )
            )

            // Phase 3: pause at end
            delay(endPauseMs.toLong())

            // Phase 4: fast snap back to start
            scrollState.animateScrollTo(
                0,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = EaseOut
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { size ->
                containerWidthPx = size.width
            }
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                onTextLayout = { result ->
                    textWidthPx = result.size.width
                }
            )
        }
        // Right edge fade (when not fully scrolled)
        if (isOverflowing &&
            scrollState.value < scrollState.maxValue
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .matchParentSize()
                    .width(fadeEdgeWidth)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                surfaceColor
                            )
                        )
                    )
            )
        }
        // Left edge fade (when scrolled past start)
        if (isOverflowing && scrollState.value > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .matchParentSize()
                    .width(fadeEdgeWidth)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                surfaceColor,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ── ContentReveal: subtle slide-up + fade for card content ───────────────

/**
 * Wraps content in a slide-up + fade-in reveal animation.
 * Triggers on initial composition — re-triggers when the composable
 * re-enters the composition tree (e.g., card transitions).
 *
 * @param offsetDp  How far below the content starts (default 20dp)
 * @param durationMs  Animation duration (default 500ms)
 */
@Composable
fun ContentReveal(
    offsetDp: Dp = 20.dp,
    durationMs: Int = 500,
    delayMs: Int = 150,
    content: @Composable () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        appeared = true
    }
    val density = LocalDensity.current
    val offsetPx = remember(density) {
        with(density) { offsetDp.toPx() }
    }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMs, easing = EaseOut),
        label = "contentReveal"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            translationY = offsetPx * (1f - progress)
            alpha = progress
        }
    ) {
        content()
    }
}
