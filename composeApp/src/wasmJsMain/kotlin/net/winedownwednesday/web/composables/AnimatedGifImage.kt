package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.w3c.dom.HTMLImageElement
import kotlinx.browser.document

/**
 * Renders an animated GIF using a browser-native HTML <img> overlay.
 * The <img> element is positioned absolutely over the Compose canvas
 * at the GIF's layout location. The browser handles GIF animation natively.
 */
@Composable
fun AnimatedGifImage(
    url: String,
    contentDescription: String = "GIF",
    modifier: Modifier = Modifier,
    maxWidth: Dp = 200.dp,
    maxHeight: Dp = 160.dp,
) {
    val density = LocalDensity.current
    var posX by remember { mutableStateOf(0f) }
    var posY by remember { mutableStateOf(0f) }
    var imgWidth by remember { mutableStateOf(0) }
    var imgHeight by remember { mutableStateOf(0) }
    val imgId = remember { "gif-img-${url.hashCode()}-${kotlin.random.Random.nextInt()}" }

    val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
    val maxHeightPx = with(density) { maxHeight.toPx() }.toInt()

    // Create the overlay <img> when composable enters composition
    DisposableEffect(url) {
        val img = document.createElement("img") as HTMLImageElement
        img.id = imgId
        img.src = url
        img.crossOrigin = "anonymous"
        img.alt = contentDescription

        // Position fixed over the compose canvas
        img.style.position = "fixed"
        img.style.setProperty("pointer-events", "none")
        img.style.zIndex = "1000"
        img.style.setProperty("border-radius", "8px")
        img.style.setProperty("object-fit", "contain")
        img.style.display = "none" // hidden until loaded & positioned

        document.body?.appendChild(img)

        img.onload = {
            val naturalW = img.naturalWidth
            val naturalH = img.naturalHeight
            if (naturalW > 0 && naturalH > 0) {
                val aspectRatio = naturalW.toDouble() / naturalH.toDouble()
                var drawW = naturalW
                var drawH = naturalH

                if (drawW > maxWidthPx) {
                    drawW = maxWidthPx
                    drawH = (drawW / aspectRatio).toInt()
                }
                if (drawH > maxHeightPx) {
                    drawH = maxHeightPx
                    drawW = (drawH * aspectRatio).toInt()
                }

                img.style.width = "${drawW}px"
                img.style.height = "${drawH}px"
                imgWidth = drawW
                imgHeight = drawH

                // Show the image now that dimensions are set
                img.style.display = "block"
            }
            Unit
        }

        onDispose {
            img.parentNode?.removeChild(img)
        }
    }

    // Update img position whenever the Compose layout position changes
    LaunchedEffect(posX, posY) {
        val img = document.getElementById(imgId) as? HTMLImageElement
        if (img != null) {
            img.style.left = "${posX}px"
            img.style.top = "${posY}px"
        }
    }

    // Box reserves layout space and tracks position
    val widthDp = with(density) { imgWidth.toDp() }
    val heightDp = with(density) { imgHeight.toDp() }

    Box(
        modifier = modifier
            .then(
                if (imgWidth > 0 && imgHeight > 0)
                    Modifier.size(widthDp, heightDp)
                else
                    Modifier.size(maxWidth, maxHeight)
            )
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                posX = pos.x
                posY = pos.y
            }
    )
}
