package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent user session and credentials.
 * Ensures that student/admin login details, identifiers, and saved state
 * are NEVER lost or deleted on app restart, background kill, or APK update.
 */
class SessionPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveStudentSession(
        identifier: String,
        displayName: String = identifier,
        loginType: String = "password"
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_IDENTIFIER, identifier.trim())
            .putString(KEY_DISPLAY_NAME, displayName.trim())
            .putString(KEY_LOGIN_TYPE, loginType)
            .putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun saveAdminSession(adminUser: String) {
        prefs.edit()
            .putBoolean(KEY_IS_ADMIN_LOGGED_IN, true)
            .putString(KEY_ADMIN_USER, adminUser.trim())
            .putLong(KEY_ADMIN_LOGIN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun isStudentLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun isAdminLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_ADMIN_LOGGED_IN, false)
    }

    fun getStudentIdentifier(): String {
        return prefs.getString(KEY_IDENTIFIER, "") ?: ""
    }

    fun getStudentDisplayName(): String {
        val name = prefs.getString(KEY_DISPLAY_NAME, "")
        if (!name.isNullOrBlank()) return name
        val ident = getStudentIdentifier()
        if (ident.isNotBlank()) return ident
        return "Student"
    }

    fun getLoginType(): String {
        return prefs.getString(KEY_LOGIN_TYPE, "password") ?: "password"
    }

    fun getAdminUser(): String {
        return prefs.getString(KEY_ADMIN_USER, "") ?: ""
    }

    fun clearStudentSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_IDENTIFIER)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_LOGIN_TYPE)
            .remove(KEY_LOGIN_TIMESTAMP)
            .apply()
    }

    fun clearAdminSession() {
        prefs.edit()
            .putBoolean(KEY_IS_ADMIN_LOGGED_IN, false)
            .remove(KEY_ADMIN_USER)
            .remove(KEY_ADMIN_LOGIN_TIMESTAMP)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "akshayclasses_session_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_IDENTIFIER = "key_identifier"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_LOGIN_TYPE = "key_login_type"
        private const val KEY_LOGIN_TIMESTAMP = "key_login_timestamp"

        private const val KEY_IS_ADMIN_LOGGED_IN = "key_is_admin_logged_in"
        private const val KEY_ADMIN_USER = "key_admin_user"
        private const val KEY_ADMIN_LOGIN_TIMESTAMP = "key_admin_login_timestamp"

        @Volatile
        private var INSTANCE: SessionPreferences? = null

        fun getInstance(context: Context): SessionPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
