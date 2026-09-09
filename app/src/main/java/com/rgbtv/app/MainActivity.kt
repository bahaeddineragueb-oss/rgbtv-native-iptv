package com.rgbtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rgbtv.app.model.Channel
import com.rgbtv.app.ui.MainViewModel
import com.rgbtv.app.ui.RgbTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RgbTvTheme { RgbTvApp() } } }
}

@Composable
private fun RgbTvApp(vm: MainViewModel = viewModel()) {
    val channels by vm.channels.collectAsState(); val playerState by vm.player.collectAsState()
    var query by remember { mutableStateOf("") }; var selectedGroup by remember { mutableStateOf("All") }; var showAccounts by remember { mutableStateOf(true) }
    val groups = listOf("All") + channels.map { it.group }.distinct()
    val filtered = channels.filter { (selectedGroup == "All" || it.group == selectedGroup) && it.name.contains(query, true) }
    LaunchedEffect(Unit) { vm.loadDemoCatalog() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            TopBar(query, { query = it }) { showAccounts = true }
            Row(Modifier.fillMaxSize()) {
                Sidebar(groups, selectedGroup) { selectedGroup = it }
                Column(Modifier.weight(1f).padding(horizontal = 22.dp)) {
                    HeroPlayer(playerState.selectedChannel)
                    Spacer(Modifier.height(18.dp)); SectionHeader("Live channels", filtered.size)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(filtered) { c -> ChannelCard(c, { vm.selectChannel(c) }, { vm.toggleFavorite(c.id) }) } }
                }
            }
        }
    }
    if (showAccounts) AccountDialog(vm) { showAccounts = false }
}

@Composable
private fun TopBar(query: String, onQuery: (String) -> Unit, onAccounts: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFFFF3D71), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) { Text("RGB", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(12.dp)); Text("RGB", fontSize = 21.sp, fontWeight = FontWeight.Black); Text("Tv", color = MaterialTheme.colorScheme.primary, fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f)); OutlinedTextField(query, onQuery, Modifier.width(330.dp), singleLine = true, leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Search channels and movies") }); Spacer(Modifier.width(12.dp)); IconButton(onClick = onAccounts) { Icon(Icons.Rounded.AccountCircle, "Accounts") }
    }
}

@Composable
private fun AccountDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }; var server by remember { mutableStateOf("") }; var user by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var portal by remember { mutableStateOf("") }; var mac by remember { mutableStateOf("") }; var m3u by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add IPTV source") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Connect your Xtream, Stalker, or M3U account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            TabRow(selectedTabIndex = tab) { listOf("Xtream", "Stalker", "M3U").forEachIndexed { i, label -> Tab(i == tab, { tab = i }, text = { Text(label) }) } }
            when (tab) {
                0 -> { OutlinedTextField(server, { server = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Server URL (HTTP/HTTPS)") }); OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Username") }); OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password") }) }
                1 -> { OutlinedTextField(portal, { portal = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Portal URL (HTTP/HTTPS)") }); OutlinedTextField(mac, { mac = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("MAC address") }) }
                else -> OutlinedTextField(m3u, { m3u = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("M3U URL (HTTP/HTTPS)") })
            }
        }
    }, confirmButton = { Button(onClick = { when (tab) { 0 -> vm.loadXtream(server, user, pass); 1 -> vm.loadStalker(portal, mac); else -> vm.loadM3uUrl(m3u) }; onDismiss() }) { Text("Connect") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun Sidebar(groups: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.width(220.dp).fillMaxHeight().padding(16.dp)) { Text("BROWSE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(12.dp)); groups.forEach { group -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected == group) MaterialTheme.colorScheme.primary.copy(.16f) else Color.Transparent).clickable { onSelect(group) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (group == "All") Icons.Rounded.GridView else Icons.Rounded.Tv, null, tint = if (selected == group) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp)); Text(group, fontWeight = if (selected == group) FontWeight.Bold else FontWeight.Normal) } } }
}

@Composable
private fun HeroPlayer(channel: Channel?) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    LaunchedEffect(channel?.streamUrl) { channel?.let { player.setMediaItem(MediaItem.fromUri(it.streamUrl)); player.prepare(); player.playWhenReady = true } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    Card(Modifier.fillMaxWidth().height(330.dp), RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.Black)) { Box { AndroidView({ PlayerView(it).apply { this.player = player; useController = true } }, Modifier.fillMaxSize()); if (channel == null) Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF171B2B), Color(0xFF090B12)))), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Tv, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text("Select a channel to start watching", color = Color.White) } } } }
}

@Composable private fun SectionHeader(title: String, count: Int) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Text("$count available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }

@Composable
private fun ChannelCard(channel: Channel, onSelect: () -> Unit, onFavorite: () -> Unit) { Card(Modifier.width(168.dp).clickable { onSelect() }, RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp)) { Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF202A44), Color(0xFF121725)))), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(10.dp)); Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1); Text(channel.group, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Spacer(Modifier.weight(1f)); IconButton(onClick = onFavorite, Modifier.size(30.dp)) { Icon(if (channel.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (channel.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
