package com.betshield.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class PasswordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var sessionUnlockUntil: Long = 0L

    companion object {
        private const val PREFS_NAME = "betshield_security"
        private const val KEY_PASSWORD_HASH = "key_password_hash"
        private const val KEY_PASSWORD_SALT = "key_password_salt"
        private const val HASH_ITERATIONS = 10000
    }

    /**
     * Checks if a master password has already been configured.
     */
    fun isPasswordSet(): Boolean {
        return prefs.contains(KEY_PASSWORD_HASH) && prefs.contains(KEY_PASSWORD_SALT)
    }

    /**
     * Saves a new master password with a cryptographic salt and SHA-256 stretching.
     */
    fun setPassword(password: String): Boolean {
        if (password.length < 4) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPassword(password, salt)

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PASSWORD_SALT, saltBase64)
            .putString(KEY_PASSWORD_HASH, hashBase64)
            .apply()

        return true
    }

    /**
     * Verifies the user-entered password against the stored salt & hash.
     */
    fun verifyPassword(password: String): Boolean {
        val storedSaltBase64 = prefs.getString(KEY_PASSWORD_SALT, null) ?: return false
        val storedHashBase64 = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false

        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val computedHash = hashPassword(password, salt)
        val computedHashBase64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        val matches = storedHashBase64 == computedHashBase64
        if (matches) {
            // Unlock session for 5 minutes of administrative operations
            unlockSession(5)
        }
        return matches
    }

    /**
     * Allows temporary bypass of password prompts while modifying multiple settings.
     */
    fun isTemporarilyUnlocked(): Boolean {
        return System.currentTimeMillis() < sessionUnlockUntil
    }

    fun unlockSession(durationMinutes: Int = 5) {
        sessionUnlockUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    }

    fun lockSession() {
        sessionUnlockUntil = 0L
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(salt)
        var input = digest.digest(password.toByteArray(Charsets.UTF_8))
        for (i in 0 until HASH_ITERATIONS) {
            digest.reset()
            input = digest.digest(input)
        }
        return input
    }
}
