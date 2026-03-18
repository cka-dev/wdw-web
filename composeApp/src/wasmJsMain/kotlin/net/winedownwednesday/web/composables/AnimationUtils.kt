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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
    baseColor: Color      = Color(0xFF2A2A2A),
    highlightColor: Color = Color(0xFF404040)
): Brush {
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
        colors = listOf(baseColor, highlightColor, baseColor),
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
fun GridItemReveal(index: Int, animationKey: Any? = Unit, content: @Composable () -> Unit) {
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
        modifier = Modifier.graphicsLayer {
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
