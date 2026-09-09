package com.rgbtv.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rgbtv.app.R
import com.rgbtv.app.data.M3uParser
import com.rgbtv.app.data.ProfileStore
import com.rgbtv.app.data.SecureCredentials
import com.rgbtv.app.model.Channel
import com.rgbtv.app.model.PlayerState
import com.rgbtv.app.model.Profile
import com.rgbtv.app.model.SourceType
import com.rgbtv.app.model.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val store = ProfileStore(appContext)

    /**
     * EncryptedSharedPreferences initialises the Android Keystore, so it is created lazily and is
     * only ever touched from a background thread. A Keystore failure must not crash the app: the
     * source simply behaves like a non-persisted one.
     */
    private val credentials: SecureCredentials? by lazy { runCatching { SecureCredentials(appContext) }.getOrNull() }

    private var favoriteIds: Set<String> = store.favorites()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    private val _profiles = MutableStateFlow(store.profiles())
    private val _query = MutableStateFlow("")
    private val _selectedGroup = MutableStateFlow<String?>(null) // null means "All"
    private val _favoritesOnly = MutableStateFlow(false)
    private val _loading = MutableStateFlow(false)
    private val _message = MutableStateFlow<UiMessage?>(null)
    private val _player = MutableStateFlow(PlayerState())

    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    val message: StateFlow<UiMessage?> = _message.asStateFlow()
    val player: StateFlow<PlayerState> = _player.asStateFlow()

    /** Category list derived from the loaded channels, sorted and de-duplicated. */
    val groups: StateFlow<List<String>> = _channels
        .map { channels -> channels.map { it.group }.distinct().sortedBy { it.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Search + category + favorites filtering, computed in the ViewModel instead of on every recomposition. */
    val visibleChannels: StateFlow<List<Channel>> = combine(
        _channels, _query, _selectedGroup, _favoritesOnly
    ) { channels: List<Channel>, query: String, group: String?, favoritesOnly: Boolean ->
        val needle = query.trim()
        channels.filter { channel -> matches(channel, needle, group, favoritesOnly) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastRequest: (suspend () -> List<Channel>)? = null

    init {
        // Restore the last used source on startup; fall back to the demo catalogue on first run.
        val saved = _profiles.value
        val active = saved.firstOrNull { it.id == store.activeId() } ?: saved.firstOrNull()
        if (active == null) loadDemoCatalog() else connectProfile(active)
    }

    /* ---------------------------------------------------------------------- */
    /* Loading sources                                                        */
    /* ---------------------------------------------------------------------- */

    fun loadDemoCatalog() {
        launchLoad { demoChannels() }
    }

    fun connectXtream(serverInput: String, username: String, password: String): Boolean {
        val user = username.trim()
        if (serverInput.isBlank()) return fail(R.string.error_empty_server)
        if (user.isBlank()) return fail(R.string.error_empty_username)
        if (password.isBlank()) return fail(R.string.error_empty_password)

        val server = normalizeServer(serverInput)
        val profile = Profile(
            id = "xt:${server.hashCode()}",
            name = appContext.getString(R.string.profile_xtream, hostOf(server)),
            sourceType = SourceType.XTREAM,
            endpoint = server,
            account = user
        )

        launchLoad {
            val channels = fetchXtream(server, user, password)
            rememberSource(profile)
            storeCredentials(profile.id, user, password)
            channels
        }
        return true
    }

    fun connectStalker(portalInput: String, macInput: String): Boolean {
        val mac = normalizeMac(macInput)
        if (portalInput.isBlank()) return fail(R.string.error_empty_portal)
        if (mac == null) return fail(R.string.error_invalid_mac)

        val portal = normalizeServer(portalInput)
        val profile = Profile(
            id = "st:${portal.hashCode()}",
            name = appContext.getString(R.string.profile_stalker, hostOf(portal)),
            sourceType = SourceType.STALKER,
            endpoint = portal,
            account = mac
        )

        launchLoad {
            val channels = fetchStalker(portal, mac)
            rememberSource(profile)
            channels
        }
        return true
    }

    fun connectM3u(urlInput: String): Boolean {
        if (urlInput.isBlank()) return fail(R.string.error_empty_m3u)

        val url = requireHttpOrNull(urlInput)
            ?: return fail(R.string.error_unsupported_url)
        val profile = Profile(
            id = "m3u:${url.hashCode()}",
            name = appContext.getString(R.string.profile_m3u, hostOf(url)),
            sourceType = SourceType.M3U,
            endpoint = url
        )

        launchLoad {
            val text = withContext(Dispatchers.IO) { openText(url) }
            val channels = parsePlaylist(text)
            rememberSource(profile)
            channels
        }
        return true
    }

    fun loadM3uText(text: String) {
        launchLoad { parsePlaylist(text) }
    }

    /** Re-connects a previously saved source (used on startup and from the sources list). */
    fun connectProfile(profile: Profile) {
        store.setActive(profile.id)
        _profiles.value = store.profiles()

        launchLoad {
            when (profile.sourceType) {
                SourceType.XTREAM -> {
                    val user = withContext(Dispatchers.IO) { credentials?.username(profile.id) }.orEmpty()
                    val pass = withContext(Dispatchers.IO) { credentials?.password(profile.id) }.orEmpty()
                    if (user.isBlank() || pass.isBlank()) {
                        throw SourceException(UiMessage(R.string.error_restore))
                    }
                    fetchXtream(profile.endpoint, user, pass)
                }

                SourceType.STALKER -> fetchStalker(profile.endpoint, profile.account.orEmpty())

                SourceType.M3U -> {
                    val text = withContext(Dispatchers.IO) { openText(requireHttp(profile.endpoint)) }
                    parsePlaylist(text)
                }
            }
        }
    }

    fun removeProfile(profile: Profile) {
        val wasActive = store.activeId() == profile.id
        store.delete(profile.id)
        if (wasActive) store.setActive(null)
        viewModelScope.launch(Dispatchers.IO) { runCatching { credentials?.remove(profile.id) } }
        _profiles.value = store.profiles()

        if (wasActive) {
            _channels.value = emptyList()
            _player.value = _player.value.copy(selectedChannel = null)
            if (_profiles.value.isEmpty()) loadDemoCatalog()
        }
    }

    fun retry() {
        lastRequest?.let { launchLoad(it) }
    }

    private fun launchLoad(block: suspend () -> List<Channel>) {
        lastRequest = block
        viewModelScope.launch {
            _loading.value = true
            try {
                publish(block())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: SourceException) {
                _message.value = failure.uiMessage
            } catch (failure: HttpStatusException) {
                _message.value = uiMessageForHttp(failure.code)
            } catch (failure: IOException) {
                _message.value = UiMessage(R.string.error_network)
            } catch (failure: Exception) {
                _message.value = UiMessage(R.string.error_generic)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun publish(channels: List<Channel>) {
        _selectedGroup.value = null
        val restored = channels.map { channel ->
            if (channel.id in favoriteIds) channel.copy(isFavorite = true) else channel
        }
        _channels.value = restored
        if (restored.isEmpty()) {
            _player.value = _player.value.copy(selectedChannel = null)
            _message.value = UiMessage(R.string.error_no_channels)
        } else {
            _player.value = _player.value.copy(selectedChannel = restored.first())
        }
    }

    private fun rememberSource(profile: Profile) {
        store.save(profile)
        store.setActive(profile.id)
        _profiles.value = store.profiles()
    }

    private fun storeCredentials(profileId: String, user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { credentials?.put(profileId, user, pass) }
        }
    }

    private suspend fun parsePlaylist(text: String): List<Channel> {
        if (text.length > M3uParser.MAX_PLAYLIST_CHARS) {
            throw SourceException(UiMessage(R.string.error_playlist_too_large))
        }
        return withContext(Dispatchers.Default) {
            runCatching { M3uParser.parse(text) }
                .getOrElse { throw SourceException(UiMessage(R.string.error_invalid_playlist)) }
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Xtream Codes                                                           */
    /* ---------------------------------------------------------------------- */

    private suspend fun fetchXtream(serverInput: String, user: String, pass: String): List<Channel> =
        withContext(Dispatchers.IO) {
            val server = normalizeServer(serverInput)
            val categoryNames = runCatching { xtreamApi(server, user, pass, "get_live_categories") }
                .getOrDefault(JSONArray())
                .let { array -> readCategoryNames(array) }

            val live = xtreamApi(server, user, pass, "get_live_streams")
            buildList {
                for (i in 0 until live.length()) {
                    val stream = live.optJSONObject(i) ?: continue
                    val id = stream.optString("stream_id")
                    if (id.isBlank()) continue
                    add(
                        Channel(
                            id = "xt:$id",
                            name = stream.optString("name").ifBlank { "Channel" },
                            group = categoryNames[stream.optString("category_id")] ?: "Live",
                            logo = stream.optString("stream_icon").ifBlank { null },
                            // Encoded: the credentials would otherwise break on special characters.
                            streamUrl = "$server/live/${enc(user)}/${enc(pass)}/$id.m3u8"
                        )
                    )
                }
            }
        }

    private fun xtreamApi(server: String, user: String, pass: String, action: String): JSONArray {
        val url = "$server/player_api.php?username=${enc(user)}&password=${enc(pass)}&action=$action"
        return JSONArray(openText(url))
    }

    private fun readCategoryNames(array: JSONArray): Map<String, String> {
        val names = HashMap<String, String>(array.length())
        for (i in 0 until array.length()) {
            val category = array.optJSONObject(i) ?: continue
            names[category.optString("category_id")] = category.optString("category_name").ifBlank { "Live" }
        }
        return names
    }

    /* ---------------------------------------------------------------------- */
    /* Stalker portal                                                         */
    /* ---------------------------------------------------------------------- */

    private suspend fun fetchStalker(portalInput: String, mac: String): List<Channel> =
        withContext(Dispatchers.IO) {
            val base = "${normalizeServer(portalInput)}/server/load.php"

            val handshake = stalkerRequest(base, mac, "type=stb&action=handshake&token=&JsHttpRequest=1-xml")
            val token = handshake.optJSONObject("js")?.optString("token").orEmpty()
            if (token.isBlank()) throw SourceException(UiMessage(R.string.error_stalker_handshake))

            val data = stalkerRequest(base, mac, "type=itv&action=get_all_channels&JsHttpRequest=1-xml", token)
                .optJSONObject("js")
                ?.optJSONArray("data")
                ?: throw SourceException(UiMessage(R.string.error_stalker_empty))

            buildList {
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val cmd = item.optString("cmd").substringAfterLast(' ')
                    if (!cmd.startsWith("http://", true) && !cmd.startsWith("https://", true)) continue
                    add(
                        Channel(
                            id = "st:${item.optString("id").ifBlank { i.toString() }}",
                            name = item.optString("name").ifBlank { "Channel" },
                            group = "Stalker",
                            logo = item.optString("logo").ifBlank { null },
                            streamUrl = cmd
                        )
                    )
                }
            }
        }

    private fun stalkerRequest(base: String, mac: String, query: String, token: String? = null): JSONObject {
        val url = "$base?$query"
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/London")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")

            val code = connection.responseCode
            if (code !in 200..299) throw HttpStatusException(code)
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Networking helpers                                                     */
    /* ---------------------------------------------------------------------- */

    private fun openText(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val code = connection.responseCode
            if (code !in 200..299) throw HttpStatusException(code)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeServer(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) trimmed else "http://$trimmed"
    }

    private fun requireHttp(input: String): String =
        requireHttpOrNull(input) ?: throw SourceException(UiMessage(R.string.error_unsupported_url))

    private fun requireHttpOrNull(input: String): String? {
        val trimmed = input.trim()
        val scheme = runCatching { URI(trimmed) }.getOrNull()?.scheme
        val isSupported = scheme != null && (scheme.equals("http", true) || scheme.equals("https", true))
        return trimmed.takeIf { isSupported }
    }

    private fun normalizeMac(input: String): String? {
        val cleaned = input.trim().uppercase(Locale.US).replace('-', ':')
        val normalized = cleaned.split(':').joinToString(":") { it.padStart(2, '0') }
        return normalized.takeIf { MAC_PATTERN.matches(it) }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /* ---------------------------------------------------------------------- */
    /* UI state                                                               */
    /* ---------------------------------------------------------------------- */

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setFavoritesOnly(enabled: Boolean) {
        _favoritesOnly.value = enabled
    }

    fun selectChannel(channel: Channel) {
        _player.value = _player.value.copy(selectedChannel = channel)
    }

    fun toggleFavorite(channelId: String) {
        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) channel.copy(isFavorite = !channel.isFavorite) else channel
        }
        favoriteIds = _channels.value.filter { it.isFavorite }.map { it.id }.toSet()
        viewModelScope.launch(Dispatchers.IO) { store.setFavorites(favoriteIds) }
    }

    fun setPlaying(playing: Boolean) {
        _player.value = _player.value.copy(isPlaying = playing)
    }

    fun setBuffering(buffering: Boolean) {
        _player.value = _player.value.copy(isBuffering = buffering)
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun fail(resId: Int): Boolean {
        _message.value = UiMessage(resId)
        return false
    }

    private fun matches(channel: Channel, needle: String, group: String?, favoritesOnly: Boolean): Boolean {
        if (favoritesOnly && !channel.isFavorite) return false
        if (group != null && channel.group != group) return false
        if (needle.isEmpty()) return true
        return channel.name.contains(needle, ignoreCase = true) || channel.group.contains(needle, ignoreCase = true)
    }

    private fun demoChannels(): List<Channel> = listOf(
        Channel("demo:news", "World News", "News", null, "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
        Channel("demo:cinema", "Cinema Showcase", "Movies", null, "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"),
        Channel("demo:sports", "Sports Live", "Sports", null, "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
    )

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val USER_AGENT = "RGBTv/2.0 (Android; IPTV)"
        val MAC_PATTERN = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

        fun uiMessageForHttp(code: Int): UiMessage = when (code) {
            401, 403 -> UiMessage(R.string.error_unauthorized, listOf(code))
            else -> UiMessage(R.string.error_http, listOf(code))
        }
    }
}

/** Resolves the readable host of a provider URL for the sources list. */
private fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: url

/** A source failure that maps to a user-facing string resource (never to a raw provider URL). */
private class SourceException(val uiMessage: UiMessage) : Exception("IPTV source failure")

/** Non-2xx response from a provider. */
private class HttpStatusException(val code: Int) : Exception("HTTP $code")
