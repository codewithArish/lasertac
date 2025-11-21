package com.lasertrac.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages user login sessions securely using EncryptedSharedPreferences.
 *
 * @param context The application context, used to create MasterKey and EncryptedSharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences

    companion object {
        private const val PREFS_FILENAME = "user_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    init {
        // Create a master key for encryption
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Saves the login state of the user as logged in.
     */
    fun saveLoginSession() {
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return `true` if the user is logged in, `false` otherwise.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Clears the current user session, effectively logging them out.
     */
    fun clearSession() {
        with(prefs.edit()) {
            remove(KEY_IS_LOGGED_IN)
            apply()
        }
    }
}
