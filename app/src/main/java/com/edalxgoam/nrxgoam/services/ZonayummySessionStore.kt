package com.edalxgoam.nrxgoam.services

import android.content.Context
import java.util.UUID

data class ZonayummySession(
    val token: String,
    val username: String,
)

object ZonayummySessionStore {
    private const val PREFS = "zonayummy_auth"
    private const val KEY_TOKEN = "zonayummy_token"
    private const val KEY_USERNAME = "zonayummy_username"
    private const val KEY_DEVICE_ID = "zonayummy_deviceId"

    fun getSession(context: Context): ZonayummySession? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: "Usuario"
        return ZonayummySession(token = token, username = username)
    }

    fun saveSession(context: Context, session: ZonayummySession) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USERNAME, session.username)
            .apply()
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .apply()
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val id = "dev_${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }
}


