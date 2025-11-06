package com.lasertrac.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveCredentials(email: String, pass: String) {
        with(prefs.edit()) {
            putString("EMAIL", email)
            putString("PASSWORD", pass)
            apply()
        }
    }

    fun getSavedEmail(): String? = prefs.getString("EMAIL", null)

    fun getSavedPassword(): String? = prefs.getString("PASSWORD", null)

    fun clearCredentials() {
        with(prefs.edit()) {
            remove("EMAIL")
            remove("PASSWORD")
            apply()
        }
    }

    fun setLoggedIn() {
        with(prefs.edit()) {
            putBoolean("IS_LOGGED_IN", true)
            putLong("LOGIN_TIMESTAMP", System.currentTimeMillis())
            apply()
        }
    }

    fun isSessionValid(): Boolean {
        if (!prefs.getBoolean("IS_LOGGED_IN", false)) {
            return false
        }

        val loginTimestamp = prefs.getLong("LOGIN_TIMESTAMP", 0)
        if (loginTimestamp == 0L) {
            return false
        }

        val loginCal = Calendar.getInstance().apply { timeInMillis = loginTimestamp }
        val currentCal = Calendar.getInstance()

        return loginCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
               loginCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)
    }

    fun logout() {
        with(prefs.edit()) {
            putBoolean("IS_LOGGED_IN", false)
            putLong("LOGIN_TIMESTAMP", 0)
            apply()
        }
    }
}
