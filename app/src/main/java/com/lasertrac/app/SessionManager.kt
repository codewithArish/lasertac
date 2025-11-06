package com.lasertrac.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val sessionPrefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveCredentials(email: String, pass: String) {
        with(encryptedPrefs.edit()) {
            putString("EMAIL", email)
            putString("PASSWORD", pass)
            apply()
        }
    }

    fun getSavedEmail(): String? = encryptedPrefs.getString("EMAIL", null)

    fun getSavedPassword(): String? = encryptedPrefs.getString("PASSWORD", null)

    fun clearCredentials() {
        with(encryptedPrefs.edit()) {
            remove("EMAIL")
            remove("PASSWORD")
            apply()
        }
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        with(sessionPrefs.edit()) {
            putBoolean("IS_LOGGED_IN", isLoggedIn)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = sessionPrefs.getBoolean("IS_LOGGED_IN", false)
}
