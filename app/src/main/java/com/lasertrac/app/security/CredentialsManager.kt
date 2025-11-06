package com.lasertrac.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context.applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences

    init {
        encryptedPrefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            "secure_credentials_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(email: String, pass: String) {
        with(encryptedPrefs.edit()) {
            putString("EMAIL_KEY", email)
            putString("PASSWORD_KEY", pass) // For production, hash this password before saving
            apply()
        }
    }

    fun getSavedEmail(): String? = encryptedPrefs.getString("EMAIL_KEY", null)
    
    fun getSavedPassword(): String? = encryptedPrefs.getString("PASSWORD_KEY", null)

    fun clearCredentials() {
        with(encryptedPrefs.edit()) {
            remove("EMAIL_KEY")
            remove("PASSWORD_KEY")
            apply()
        }
    }
}
