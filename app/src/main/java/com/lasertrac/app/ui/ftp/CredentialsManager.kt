package com.lasertrac.app.ui.ftp

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * A singleton manager for securely storing and retrieving FTP credentials using EncryptedSharedPreferences.
 */
object CredentialsManager {

    private const val PREFERENCES_FILE_NAME = "ftp_credentials_prefs"
    private const val KEY_FTP_USERNAME = "ftp_username"
    private const val KEY_FTP_PASSWORD = "ftp_password"

    private fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFERENCES_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * Saves the FTP username and password securely.
     */
    fun saveCredentials(context: Context, username: String, password: String) {
        val prefs = getEncryptedSharedPreferences(context)
        with(prefs.edit()) {
            putString(KEY_FTP_USERNAME, username)
            putString(KEY_FTP_PASSWORD, password)
            apply()
        }
    }

    /**
     * Retrieves the saved FTP username.
     */
    fun getUsername(context: Context): String? {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(KEY_FTP_USERNAME, null)
    }

    /**
     * Retrieves the saved FTP password.
     */
    fun getPassword(context: Context): String? {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(KEY_FTP_PASSWORD, null)
    }
}
