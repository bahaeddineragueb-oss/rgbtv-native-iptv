package com.rgbtv.app.data

import android.content.Context
import com.rgbtv.app.model.Profile
import com.rgbtv.app.model.SourceType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small JSON store for IPTV sources and favorites.
 *
 * Deliberately built on SharedPreferences + org.json instead of Room: the catalogue itself is
 * re-fetched from the provider, so the only things worth persisting are a handful of source
 * descriptors and the favorite channel ids. Room (with real migrations) belongs here once
 * offline catalogues and watch history are added.
 */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun profiles(): List<Profile> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PROFILES, "[]").orEmpty())
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                add(
                    Profile(
                        id = id,
                        name = item.optString("name"),
                        sourceType = runCatching { SourceType.valueOf(item.optString("type")) }
                            .getOrDefault(SourceType.M3U),
                        endpoint = item.optString("endpoint"),
                        account = item.optString("account").ifBlank { null }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(profile: Profile) {
        val updated = profiles().filter { it.id != profile.id } + profile
        prefs.edit().putString(KEY_PROFILES, encodeProfiles(updated)).apply()
    }

    fun delete(id: String) {
        prefs.edit().putString(KEY_PROFILES, encodeProfiles(profiles().filter { it.id != id })).apply()
    }

    fun activeId(): String? = prefs.getString(KEY_ACTIVE, null)

    fun setActive(id: String?) {
        prefs.edit().apply { if (id == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, id) }.apply()
    }

    fun favorites(): Set<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY_FAVORITES, "[]").orEmpty())
        buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }.getOrDefault(emptySet())

    fun setFavorites(ids: Set<String>) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    private fun encodeProfiles(profiles: List<Profile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("type", profile.sourceType.name)
                    .put("endpoint", profile.endpoint)
                    .put("account", profile.account.orEmpty())
            )
        }
        return array.toString()
    }

    private companion object {
        const val PREFS_NAME = "rgbtv_sources"
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE = "active_profile"
        const val KEY_FAVORITES = "favorite_channel_ids"
    }
}
