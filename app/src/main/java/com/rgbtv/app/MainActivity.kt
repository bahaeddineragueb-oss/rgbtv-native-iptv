package com.rgbtv.app

import android.app.PictureInPictureParams
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rgbtv.app.model.Channel
import com.rgbtv.app.ui.MainViewModel
import com.rgbtv.app.ui.RgbTvTheme
import com.rgbtv.app.ui.components.AccountDialog
import com.rgbtv.app.ui.components.AppTopBar
import com.rgbtv.app.ui.components.CategoryChips
import com.rgbtv.app.ui.components.CategoryRail
import com.rgbtv.app.ui.components.ChannelCard
import com.rgbtv.app.ui.components.HeroPlayer
import com.rgbtv.app.ui.components.LoadingPlaceholder
import com.rgbtv.app.ui.components.MessageBanner
import com.rgbtv.app.ui.components.StatePlaceholder
import com.rgbtv.app.ui.util.LayoutSize
import com.rgbtv.app.ui.util.rememberDeviceKind
import com.rgbtv.app.ui.util.rememberLayoutSize
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; each screen applies its own safe-area padding.
        enableEdgeToEdge()
        setContent {
            RgbTvTheme { RgbTvApp(vm) }
        }
    }

    // Keep playback alive in a small floating window when the user leaves the app.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.player.value.isPlaying) {
            runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }
        }
    }
}

@Composable
private fun RgbTvApp(vm: MainViewModel) {
    val channels by vm.visibleChannels.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedGroup by vm.selectedGroup.collectAsStateWithLifecycle()
    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val playerState by vm.player.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()

    // The dialog is only needed until a source has been saved; afterwards the ViewModel
    // restores it automatically on startup.
    var showAccounts by remember { mutableStateOf(vm.profiles.value.isEmpty()) }

    val layoutSize = rememberLayoutSize()
    val device = rememberDeviceKind()
    val compact = layoutSize == LayoutSize.Compact
    // The player keeps a share of the screen instead of a fixed height, so short landscape
    // screens (phones) and ten-foot screens (TV) both keep the channel list visible.
    val configuration = LocalConfiguration.current
    val heroHeight = remember(configuration.screenHeightDp, layoutSize, device.isTv) {
        val fraction = when (layoutSize) {
            LayoutSize.Compact -> 0.34f
            LayoutSize.Medium -> 0.44f
            LayoutSize.Expanded -> if (device.isTv) 0.56f else 0.48f
        }
        (configuration.screenHeightDp * fraction).dp.coerceIn(160.dp, 460.dp)
    }
    val horizontalPadding = if (compact) 12.dp else 22.dp

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            AppTopBar(
                query = query,
                onQueryChange = vm::setQuery,
                favoritesOnly = favoritesOnly,
                onFavoritesToggle = { vm.setFavoritesOnly(!favoritesOnly) },
                onAccounts = { showAccounts = true },
                showWordmark = !compact
            )

            if (compact) {
                CategoryChips(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onSelectGroup = vm::selectGroup
                )
            }

            Row(Modifier.fillMaxSize()) {
                if (!compact) {
                    CategoryRail(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        onSelectGroup = vm::selectGroup
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = horizontalPadding)
                ) {
                    if (!showAccounts && message != null) {
                        MessageBanner(
                            message = message,
                            onDismiss = vm::dismissMessage,
                            onRetry = vm::retry,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    HeroPlayer(
                        channel = playerState.selectedChannel,
                        modifier = Modifier.height(heroHeight),
                        onPlayingChange = vm::setPlaying
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.section_live_channels),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.channels_count, channels.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    when {
                        loading -> LoadingPlaceholder(
                            text = stringResource(R.string.state_loading_playlist),
                            modifier = Modifier.weight(1f)
                        )

                        channels.isEmpty() -> EmptyChannels(
                            hasQuery = query.isNotBlank(),
                            favoritesOnly = favoritesOnly,
                            query = query,
                            modifier = Modifier.weight(1f)
                        )

                        compact -> ChannelGrid(
                            channels = channels,
                            selectedId = playerState.selectedChannel?.id,
                            onSelect = vm::selectChannel,
                            onFavorite = vm::toggleFavorite,
                            modifier = Modifier.weight(1f)
                        )

                        else -> ChannelRow(
                            channels = channels,
                            selectedId = playerState.selectedChannel?.id,
                            onSelect = vm::selectChannel,
                            onFavorite = vm::toggleFavorite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showAccounts) {
        AccountDialog(
            loading = loading,
            message = message,
            profiles = profiles,
            onDismiss = {
                showAccounts = false
                vm.dismissMessage()
            },
            onConnectXtream = vm::connectXtream,
            onConnectStalker = vm::connectStalker,
            onConnectM3u = vm::connectM3u,
            onSelectProfile = {
                vm.connectProfile(it)
                showAccounts = false
            },
            onRemoveProfile = vm::removeProfile
        )
    }
}

@Composable
private fun ChannelRow(
    channels: List<Channel>,
    selectedId: String?,
    onSelect: (Channel) -> Unit,
    onFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                selected = channel.id == selectedId,
                onSelect = { onSelect(channel) },
                onFavorite = { onFavorite(channel.id) },
                // Keep the focused card visible when navigating with a TV remote.
                onFocus = { scope.launch { listState.animateScrollToItem(index) } }
            )
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    selectedId: String?,
    onSelect: (Channel) -> Unit,
    onFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        gridItemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                selected = channel.id == selectedId,
                onSelect = { onSelect(channel) },
                onFavorite = { onFavorite(channel.id) },
                fillWidth = true,
                onFocus = { scope.launch { gridState.animateScrollToItem(index) } }
            )
        }
    }
}

@Composable
private fun EmptyChannels(
    hasQuery: Boolean,
    favoritesOnly: Boolean,
    query: String,
    modifier: Modifier = Modifier
) {
    when {
        hasQuery -> StatePlaceholder(
            title = stringResource(R.string.state_no_results_title, query),
            body = stringResource(R.string.state_no_results_body),
            icon = Icons.Rounded.Search,
            modifier = modifier
        )

        favoritesOnly -> StatePlaceholder(
            title = stringResource(R.string.state_no_favorites_title),
            body = stringResource(R.string.state_no_favorites_body),
            icon = Icons.Rounded.Tv,
            modifier = modifier
        )

        else -> StatePlaceholder(
            title = stringResource(R.string.state_no_channels_title),
            body = stringResource(R.string.state_no_channels_body),
            icon = Icons.Rounded.Tv,
            modifier = modifier
        )
    }
}
