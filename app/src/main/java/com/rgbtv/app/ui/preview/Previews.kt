package com.rgbtv.app.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rgbtv.app.R
import com.rgbtv.app.model.Channel
import com.rgbtv.app.model.UiMessage
import com.rgbtv.app.ui.RgbTvTheme
import com.rgbtv.app.ui.components.AccountDialog
import com.rgbtv.app.ui.components.AppTopBar
import com.rgbtv.app.ui.components.CategoryRail
import com.rgbtv.app.ui.components.ChannelCard
import com.rgbtv.app.ui.components.MessageBanner
import com.rgbtv.app.ui.components.StatePlaceholder

/**
 * Studio previews. The player surface is intentionally not previewed: it owns a real ExoPlayer,
 * which must be verified on a device or emulator instead.
 */
private val sampleChannel = Channel(
    id = "demo:1",
    name = "BeIN Sports 1",
    group = "Sports",
    logo = null,
    streamUrl = "https://example.com/live.m3u8"
)

private const val CANVAS = 0xFF080A12

@Preview(name = "Channel cards", showBackground = true, backgroundColor = CANVAS, widthDp = 420)
@Composable
private fun ChannelCardPreview() {
    RgbTvTheme {
        Column(Modifier.padding(16.dp)) {
            ChannelCard(channel = sampleChannel, selected = false, onSelect = {}, onFavorite = {})
            ChannelCard(
                channel = sampleChannel.copy(id = "demo:2", isFavorite = true),
                selected = true,
                onSelect = {},
                onFavorite = {}
            )
        }
    }
}

@Preview(name = "Top bar", showBackground = true, backgroundColor = CANVAS, widthDp = 960)
@Composable
private fun TopBarPreview() {
    RgbTvTheme {
        AppTopBar(
            query = "",
            onQueryChange = {},
            favoritesOnly = false,
            onFavoritesToggle = {},
            onAccounts = {}
        )
    }
}

@Preview(name = "Category rail", showBackground = true, backgroundColor = CANVAS, widthDp = 260, heightDp = 420)
@Composable
private fun CategoryRailPreview() {
    RgbTvTheme {
        CategoryRail(
            groups = listOf("News", "Sports", "Movies", "Kids", "Documentaries"),
            selectedGroup = "Sports",
            onSelectGroup = {}
        )
    }
}

@Preview(name = "Empty state", showBackground = true, backgroundColor = CANVAS, widthDp = 480)
@Composable
private fun EmptyStatePreview() {
    RgbTvTheme {
        StatePlaceholder(
            title = "No channels yet",
            body = "Add an Xtream, Stalker or M3U source to fill your catalogue.",
            icon = Icons.Rounded.Tv
        )
    }
}

@Preview(name = "Error banner", showBackground = true, backgroundColor = CANVAS, widthDp = 640)
@Composable
private fun MessageBannerPreview() {
    RgbTvTheme {
        MessageBanner(message = UiMessage(R.string.error_network), onDismiss = {})
    }
}

@Preview(name = "Add source dialog", showBackground = true, backgroundColor = CANVAS, widthDp = 760, heightDp = 640)
@Composable
private fun AccountDialogPreview() {
    RgbTvTheme {
        AccountDialog(
            loading = false,
            message = null,
            profiles = listOf(
                com.rgbtv.app.model.Profile("xt:1", "Xtream · provider.tv", com.rgbtv.app.model.SourceType.XTREAM, "http://provider.tv", "demo")
            ),
            onDismiss = {},
            onConnectXtream = { _, _, _ -> true },
            onConnectStalker = { _, _ -> true },
            onConnectM3u = { true },
            onSelectProfile = {},
            onRemoveProfile = {}
        )
    }
}
