package com.lasertrac.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

class CredentialsManager(context: Context) {

    private val appContext = context.applicationContext
    private val keyAlias = "_lasertrac_secure_credentials_master_key_"
    private val prefsFilename = "secure_credentials_prefs"

    // Make SharedPreferences nullable. It will be null if initialization fails catastrophically.
    private var encryptedPrefs: SharedPreferences? = null

    init {
        try {
            initializeDependencies()
        } catch (e: Exception) {
            when (e) {
                is GeneralSecurityException, is IOException -> {
                    Log.e("CredentialsManager", "First attempt to init failed. Attempting recovery.", e)
                    handleRecovery(e)
                    // Second and final attempt
                    try {
                        initializeDependencies()
                        Log.i("CredentialsManager", "Successfully initialized after recovery.")
                    } catch (e2: Exception) {
                        Log.e("CredentialsManager", "CRITICAL: Failed to initialize EncryptedSharedPreferences even after recovery. Offline login will be disabled for this session.", e2)
                        // Initialization failed completely. encryptedPrefs remains null.
                    }
                }
                else -> {
                    Log.e("CredentialsManager", "An unexpected error occurred during initialization.", e)
                    // For any other exception, we also disable the feature.
                }
            }
        }
    }

    private fun initializeDependencies() {
        val masterKey = MasterKey.Builder(appContext, keyAlias)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            appContext,
            prefsFilename,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun handleRecovery(e: Exception) {
        Log.w("CredentialsManager", "Deleting potentially corrupted preference files.", e)
        val sharedPrefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        val prefFile = File(sharedPrefsDir, "$prefsFilename.xml")
        if (prefFile.exists()) {
            if (prefFile.delete()) {
                Log.i("CredentialsManager", "Successfully deleted corrupted pref file.")
            }
        }
    }

    // All public methods must now be null-safe.
    fun saveCredentials(email: String, pass: String, deviceId: String) {
        encryptedPrefs?.edit()?.apply {
            putString("EMAIL_KEY", email)
            putString("PASSWORD_KEY", pass)
            putString("DEVICE_ID_KEY", deviceId)
            apply()
        }
    }

    fun getSavedEmail(): String? = encryptedPrefs?.getString("EMAIL_KEY", null)
    
    fun getSavedPassword(): String? = encryptedPrefs?.getString("PASSWORD_KEY", null)

    fun getSavedDeviceId(): String? = encryptedPrefs?.getString("DEVICE_ID_KEY", null)

    fun clearCredentials() {
        encryptedPrefs?.edit()?.apply {
            remove("EMAIL_KEY")
            remove("PASSWORD_KEY")
            remove("DEVICE_ID_KEY")
            apply()
        }
    }

    fun isUserLoggedIn(): Boolean {
        val email = encryptedPrefs?.getString("EMAIL_KEY", null)
        val password = encryptedPrefs?.getString("PASSWORD_KEY", null)
        return !email.isNullOrEmpty() && !password.isNullOrEmpty()
    }
}