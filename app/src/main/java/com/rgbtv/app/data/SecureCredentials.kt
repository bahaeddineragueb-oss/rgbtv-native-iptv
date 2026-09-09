package com.rgbtv.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentials(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rgbtv_secure_credentials",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun put(profileId: String, username: String, password: String) {
        prefs.edit().putString("$profileId:user", username).putString("$profileId:pass", password).apply()
    }

    fun password(profileId: String): String? = prefs.getString("$profileId:pass", null)
    fun username(profileId: String): String? = prefs.getString("$profileId:user", null)
    fun remove(profileId: String) { prefs.edit().remove("$profileId:user").remove("$profileId:pass").apply() }
}
