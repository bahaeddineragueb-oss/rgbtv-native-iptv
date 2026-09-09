package com.rgbtv.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rgbtv.app.data.M3uParser
import com.rgbtv.app.model.Channel
import com.rgbtv.app.model.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class MainViewModel : ViewModel() {
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    private val _player = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = _player.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadDemoCatalog() {
        _channels.value = listOf(
            Channel("demo:news", "World News", "News", null, "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
            Channel("demo:cinema", "Cinema Showcase", "Movies", null, "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"),
            Channel("demo:sports", "Sports Live", "Sports", null, "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
        )
    }

    fun loadM3uText(text: String) {
        runCatching { M3uParser.parse(text) }.onSuccess { _channels.value = it }
            .onFailure { _player.value = _player.value.copy(error = it.message ?: "Invalid playlist") }
    }

    fun loadM3uUrl(url: String) = launchLoad { loadM3uText(withContext(Dispatchers.IO) { openText(requireHttp(url)) }) }

    fun loadXtream(serverInput: String, username: String, password: String) = launchLoad {
        _channels.value = withContext(Dispatchers.IO) { fetchXtream(serverInput, username, password) }
    }

    fun loadStalker(portalInput: String, macInput: String) = launchLoad {
        _channels.value = withContext(Dispatchers.IO) { fetchStalker(portalInput, macInput) }
    }

    private fun launchLoad(block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true; _player.value = _player.value.copy(error = null)
            runCatching { block() }.onFailure { _player.value = _player.value.copy(error = it.message ?: "Could not load source") }
            _loading.value = false
        }
    }

    private suspend fun fetchXtream(input: String, user: String, pass: String): List<Channel> {
        val server = normalize(input)
        fun api(action: String): JSONArray = JSONArray(openText("$server/player_api.php?username=${enc(user)}&password=${enc(pass)}&action=$action"))
        val categories = runCatching { api("get_live_categories") }.getOrDefault(JSONArray())
        val names = mutableMapOf<String, String>()
        for (i in 0 until categories.length()) { val c = categories.getJSONObject(i); names[c.optString("category_id")] = c.optString("category_name", "Live") }
        val live = api("get_live_streams")
        return buildList {
            for (i in 0 until live.length()) {
                val s = live.getJSONObject(i); val id = s.optString("stream_id")
                add(Channel("xt:$id", s.optString("name", "Channel"), names[s.optString("category_id")] ?: "Live", s.optString("stream_icon").ifBlank { null }, "$server/live/$user/$pass/$id.m3u8"))
            }
        }
    }

    private suspend fun fetchStalker(input: String, macInput: String): List<Channel> {
        val portal = normalize(input); val mac = macInput.trim().uppercase()
        require(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$").matches(mac)) { "Invalid MAC address" }
        val base = "$portal/server/load.php"
        fun request(query: String, token: String? = null): JSONObject {
            val conn = URI("$base?$query").toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000; conn.readTimeout = 20_000
            conn.setRequestProperty("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/London")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        }
        val token = request("type=stb&action=handshake&token=&JsHttpRequest=1-xml").optJSONObject("js")?.optString("token").orEmpty()
        require(token.isNotBlank()) { "Stalker handshake failed" }
        val data = request("type=itv&action=get_all_channels&JsHttpRequest=1-xml", token).optJSONObject("js")?.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i); val cmd = item.optString("cmd").substringAfterLast(' ')
                if (cmd.startsWith("http://") || cmd.startsWith("https://")) add(Channel("st:${item.optString("id", i.toString())}", item.optString("name", "Channel"), "Stalker", item.optString("logo").ifBlank { null }, cmd))
            }
        }
    }

    private fun normalize(input: String): String = input.trim().trimEnd('/').let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
    private fun requireHttp(input: String): String { val uri = URI(input.trim()); require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) { "Only HTTP and HTTPS URLs are supported" }; return input.trim() }
    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun openText(url: String): String { val c = URI(url).toURL().openConnection() as HttpURLConnection; c.connectTimeout = 20_000; c.readTimeout = 20_000; return c.inputStream.bufferedReader().use { it.readText() } }

    fun selectChannel(channel: Channel) { _player.value = _player.value.copy(selectedChannel = channel, error = null) }
    fun toggleFavorite(channelId: String) { _channels.value = _channels.value.map { if (it.id == channelId) it.copy(isFavorite = !it.isFavorite) else it } }
    fun setPlaying(playing: Boolean) { _player.value = _player.value.copy(isPlaying = playing) }
    fun setBuffering(buffering: Boolean) { _player.value = _player.value.copy(isBuffering = buffering) }
}
