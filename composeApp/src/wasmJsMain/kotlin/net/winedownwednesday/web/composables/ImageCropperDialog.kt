package net.winedownwednesday.web.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas as SkiaCanvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect as SkiaRect
import kotlin.math.max
import kotlin.math.min

private const val CROP_VIEW_SIZE = 300
private const val OUTPUT_SIZE = 512

/**
 * Dialog that lets the user pan and pinch-zoom a loaded image behind a circular mask.
 * On "Crop" it produces a 512×512 Bitmap of the visible circle region.
 */
@Composable
fun ImageCropperDialog(
    sourceBitmap: Bitmap,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    // fillScale = image just covers the entire circle (no gaps)
    val fillScale = max(
        CROP_VIEW_SIZE.toFloat() / sourceBitmap.width,
        CROP_VIEW_SIZE.toFloat() / sourceBitmap.height
    )
    // fitScale = entire image visible inside the view
    val fitScale = min(
        CROP_VIEW_SIZE.toFloat() / sourceBitmap.width,
        CROP_VIEW_SIZE.toFloat() / sourceBitmap.height
    )
    val minScale = fitScale * 0.5f   // Allow zooming out well past fit
    val maxScale = fillScale * 5f
    var scale by remember { mutableStateOf(fillScale) }
    var offset by remember {
        mutableStateOf(
            Offset(
                (CROP_VIEW_SIZE - sourceBitmap.width * fillScale) / 2f,
                (CROP_VIEW_SIZE - sourceBitmap.height * fillScale) / 2f
            )
        )
    }

    fun clampOffset(off: Offset, sc: Float): Offset {
        val imgW = sourceBitmap.width * sc
        val imgH = sourceBitmap.height * sc
        return Offset(
            // If image is smaller than view on an axis, center it
            x = if (imgW <= CROP_VIEW_SIZE) (CROP_VIEW_SIZE - imgW) / 2f
                else off.x.coerceIn(CROP_VIEW_SIZE - imgW, 0f),
            y = if (imgH <= CROP_VIEW_SIZE) (CROP_VIEW_SIZE - imgH) / 2f
                else off.y.coerceIn(CROP_VIEW_SIZE - imgH, 0f)
        )
    }

    fun applyZoom(newScale: Float, focalPoint: Offset = Offset(CROP_VIEW_SIZE / 2f, CROP_VIEW_SIZE / 2f)) {
        val clampedScale = newScale.coerceIn(minScale, maxScale)
        val scaleRatio = clampedScale / scale
        val newOff = Offset(
            focalPoint.x - (focalPoint.x - offset.x) * scaleRatio,
            focalPoint.y - (focalPoint.y - offset.y) * scaleRatio
        )
        scale = clampedScale
        offset = clampOffset(newOff, clampedScale)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Crop Photo",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Text(
                    "Drag to reposition. Pinch or scroll to zoom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                // Crop canvas
                Box(
                    modifier = Modifier
                        .size(CROP_VIEW_SIZE.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    applyZoom(scale * zoom)
                                }
                                offset = clampOffset(
                                    Offset(offset.x + pan.x, offset.y + pan.y),
                                    scale
                                )
                            }
                        }
                        .scrollable(scale, minScale) { newScale ->
                            applyZoom(newScale)
                        }
                ) {
                    val composeBitmap = remember(sourceBitmap) {
                        sourceBitmap.asComposeImageBitmap()
                    }

                    // Draw image
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = composeBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                offset.x.toInt(), offset.y.toInt()
                            ),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                (sourceBitmap.width * scale).toInt(),
                                (sourceBitmap.height * scale).toInt()
                            )
                        )
                    }

                    // Overlay with circle cutout — needs its own compositing layer
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                    ) {
                        drawCircleMask(size)
                    }
                }

                // Zoom controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { applyZoom(scale * 0.8f) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF444444), CircleShape)
                    ) {
                        Text("−", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Zoom: ${(scale / fillScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    IconButton(
                        onClick = { applyZoom(scale * 1.25f) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF444444), CircleShape)
                    ) {
                        Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            val cropped = performCrop(
                                sourceBitmap, offset, scale
                            )
                            onCropped(cropped)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7F33),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Crop")
                    }
                }
            }
        }
    }
}

/**
 * Draws a semi-transparent overlay with a circular hole in the center.
 * Must be drawn on an Offscreen compositing layer for BlendMode.Clear to work.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircleMask(canvasSize: Size) {
    val radius = min(canvasSize.width, canvasSize.height) / 2f
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

    // Semi-transparent overlay
    drawRect(
        color = Color.Black.copy(alpha = 0.55f),
        size = canvasSize
    )
    // Punch out the circle
    drawCircle(
        color = Color.Transparent,
        radius = radius - 2f,
        center = center,
        blendMode = BlendMode.Clear
    )
    // Circle border
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = radius - 1f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
}

/**
 * Produce a OUTPUT_SIZE × OUTPUT_SIZE bitmap from the visible circle region.
 */
private fun performCrop(
    source: Bitmap,
    offset: Offset,
    scale: Float
): Bitmap {
    val circleRadius = CROP_VIEW_SIZE / 2f

    // Convert circle center from canvas coords to source coords
    val srcCenterX = (circleRadius - offset.x) / scale
    val srcCenterY = (circleRadius - offset.y) / scale
    val srcRadius = circleRadius / scale

    // Source rect
    val srcLeft = (srcCenterX - srcRadius).coerceAtLeast(0f)
    val srcTop = (srcCenterY - srcRadius).coerceAtLeast(0f)
    val srcRight = (srcCenterX + srcRadius).coerceAtMost(source.width.toFloat())
    val srcBottom = (srcCenterY + srcRadius).coerceAtMost(source.height.toFloat())

    val output = Bitmap().apply {
        allocPixels(ImageInfo.makeN32Premul(OUTPUT_SIZE, OUTPUT_SIZE))
    }
    val canvas = SkiaCanvas(output)

    val sourceImage = Image.makeFromBitmap(source)
    val srcRect = SkiaRect.makeLTRB(srcLeft, srcTop, srcRight, srcBottom)
    val dstRect = SkiaRect.makeWH(OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat())

    canvas.drawImageRect(sourceImage, srcRect, dstRect, Paint())

    return output
}

/**
 * Mouse-wheel scroll modifier for zooming.
 */
@Composable
private fun Modifier.scrollable(
    currentScale: Float,
    minScale: Float,
    onScaleChange: (Float) -> Unit
): Modifier {
    return this.pointerInput(currentScale, minScale) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val scrollDelta = event.changes.firstOrNull()
                    ?.scrollDelta?.y ?: 0f
                if (scrollDelta != 0f) {
                    val zoomFactor = if (scrollDelta > 0) 0.9f else 1.1f
                    onScaleChange(currentScale * zoomFactor)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}
