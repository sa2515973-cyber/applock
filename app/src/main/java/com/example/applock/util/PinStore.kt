package com.example.applock.util

import android.content.Context
import java.security.MessageDigest

/**
 * Stores only a SHA-256 hash of the owner's real PIN — never the PIN itself.
 * (The *wrong* PINs that intruders type are logged in plain text elsewhere,
 * on purpose, as the whole point of the intruder log — but the owner's
 * actual unlock code is never stored in plain text.)
 */
object PinStore {
    private const val PREFS = "applock_secure_prefs"
    private const val KEY_HASH = "pin_hash"

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean =
        prefs(context).contains(KEY_HASH)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_HASH, hash(pin)).apply()
    }

    fun checkPin(context: Context, candidate: String): Boolean {
        val stored = prefs(context).getString(KEY_HASH, null) ?: return false
        return stored == hash(candidate)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
