package com.lasertrac.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    companion object {
        private const val PREF_NAME = "auth_pref"
        private const val USER_TOKEN = "user_token"
    }

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        sharedPreferences = EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(USER_TOKEN, null)
    }

    fun clearAuthToken() {
        val editor = sharedPreferences.edit()
        editor.remove(USER_TOKEN)
        editor.apply()
    }
}
