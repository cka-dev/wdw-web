package net.winedownwednesday.web.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// JS Interop for QR code generation via qr-code-styling CDN
// ---------------------------------------------------------------------------

/**
 * Generate a QR code as a data-URL (PNG) with the WDW logo embedded.
 * Uses the qr-code-styling library loaded from CDN.
 * The callback receives the data URL string.
 */
@JsFun("""
(url, callback) => {
    // Lazy-load qr-code-styling from CDN if not already loaded
    if (typeof QRCodeStyling === 'undefined') {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/qr-code-styling@1.6.0-rc.1/lib/qr-code-styling.js';
        script.onload = () => generateQR(url, callback);
        document.head.appendChild(script);
    } else {
        generateQR(url, callback);
    }

    function generateQR(url, cb) {
        const qr = new QRCodeStyling({
            width: 280,
            height: 280,
            type: 'canvas',
            data: url,
            image: 'wdw_new_logo.png',
            dotsOptions: {
                color: '#FF7F33',
                type: 'rounded'
            },
            cornersSquareOptions: {
                color: '#333333',
                type: 'extra-rounded'
            },
            cornersDotOptions: {
                color: '#FF7F33',
                type: 'dot'
            },
            backgroundOptions: {
                color: '#1A1A1A'
            },
            imageOptions: {
                crossOrigin: 'anonymous',
                margin: 6,
                imageSize: 0.35
            },
            qrOptions: {
                errorCorrectionLevel: 'H'
            }
        });
        qr.getRawData('png').then(blob => {
            const reader = new FileReader();
            reader.onload = () => cb(reader.result);
            reader.readAsDataURL(blob);
        });
    }
}
""")
private external fun generateQrCodeJs(
    url: JsString,
    callback: (JsString) -> Unit
)

/**
 * Kotlin-friendly wrapper that generates a QR code data URL
 * via suspendCancellableCoroutine.
 */
suspend fun generateQrCode(url: String): String {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        generateQrCodeJs(url.toJsString()) { dataUrl ->
            cont.resumeWith(
                Result.success(dataUrl.toString())
            )
        }
    }
}

// ---------------------------------------------------------------------------
// QR Code Dialog Composable
// ---------------------------------------------------------------------------


@Composable
fun QrCodeDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit
) {
    var qrDataUrl by remember { mutableStateOf<String?>(null) }
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        qrDataUrl = generateQrCode(url)
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan to View",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QR code image
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (qrDataUrl != null) {
                    AsyncImage(
                        model = qrDataUrl,
                        contentDescription = "QR code for $title",
                        modifier = Modifier.size(280.dp)
                    )
                } else {
                    Text(
                        text = "Generating...",
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Event title
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Copy link button
            TextButton(
                onClick = {
                    copyToClipboard(url.toJsString())
                    showCopied = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = WdwOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showCopied) "Copied!" else "Copy Link",
                    color = WdwOrange
                )
            }

            if (showCopied) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    showCopied = false
                }
            }
        }
    }
}
