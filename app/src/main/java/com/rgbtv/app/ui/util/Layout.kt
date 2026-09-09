package com.rgbtv.app.ui.util

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Width buckets based on the Material 3 window size classes, so phone, tablet and TV layouts
 * can share one implementation.
 */
enum class LayoutSize { Compact, Medium, Expanded }

@Composable
fun rememberLayoutSize(): LayoutSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> LayoutSize.Compact
            widthDp < 840 -> LayoutSize.Medium
            else -> LayoutSize.Expanded
        }
    }
}

data class DeviceKind(val isTv: Boolean, val isTouchscreen: Boolean)

@Composable
fun rememberDeviceKind(): DeviceKind {
    val packageManager = LocalContext.current.packageManager
    return remember {
        DeviceKind(
            isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
            isTouchscreen = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        )
    }
}
