package com.example.utils

import android.content.Context
import java.security.MessageDigest

class SessionManager(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getSavedPhone(): String? {
    val phone = prefs.getString(KEY_PHONE, null)
    return if (phone.isNullOrBlank()) null else phone
  }

  fun saveSession(phoneNumber: String) {
    prefs.edit().putString(KEY_PHONE, phoneNumber.trim()).apply()
  }

  fun clearSession() {
    prefs.edit().remove(KEY_PHONE).apply()
  }

  companion object {
    private const val PREFS_NAME = "eczatakip_user_session"
    private const val KEY_PHONE = "logged_in_phone"

    /**
     * Hashes the user password with SHA-256 so raw passwords are never stored in MariaDB.
     */
    fun hashPassword(rawPassword: String): String {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = digest.digest(rawPassword.trim().toByteArray(Charsets.UTF_8))
      return bytes.joinToString("") { "%02x".format(it) }
    }
  }
}
