package io.github.thomaswcode.notificationvibrator.ui.components

import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Caches app labels and icons, which are slow to load from PackageManager. */
object AppInfoCache {
    private val labels = ConcurrentHashMap<String, String>()
    private val icons = LruCache<String, ImageBitmap>(300)

    fun label(pm: PackageManager, packageName: String): String = labels.getOrPut(packageName) {
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))).toString()
        }.getOrDefault(packageName)
    }

    fun icon(pm: PackageManager, packageName: String, sizePx: Int): ImageBitmap? =
        icons.get(packageName) ?: runCatching {
            pm.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
        }.getOrNull()?.also { icons.put(packageName, it) }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val pm = LocalContext.current.packageManager
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) { AppInfoCache.icon(pm, packageName, sizePx) }
    }
    Box(modifier.size(size)) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
    }
}

@Composable
fun rememberAppLabel(packageName: String): String {
    val pm = LocalContext.current.packageManager
    var label by remember(packageName) { mutableStateOf(packageName) }
    LaunchedEffect(packageName) {
        label = withContext(Dispatchers.IO) { AppInfoCache.label(pm, packageName) }
    }
    return label
}
