package net.winedownwednesday.web

import androidx.compose.runtime.Composable
import coil3.compose.setSingletonImageLoaderFactory
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade

/**
 * Initialises the Coil 3 singleton [ImageLoader] with:
 * - A memory cache sized at 25 % of available memory
 * - Crossfade transitions enabled for smoother image loading
 *
 * Call this composable once, near the root of the composition tree,
 * **before** any [coil3.compose.AsyncImage] is rendered.
 */
@Composable
fun InitCoilImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
