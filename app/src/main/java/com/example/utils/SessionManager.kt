package com.example.utils

import android.content.Context
import java.security.MessageDigest

class SessionManager(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getSavedPhone(): String? {
    val phone = prefs.getString(KEY_PHONE, null)
    if (phone.isNullOrBlank()) return null
    val normalized = normalizePhone(phone)
    if (normalized.isNotBlank() && normalized != phone) {
      prefs.edit().putString(KEY_PHONE, normalized).apply()
    }
    return normalized.ifBlank { null }
  }

  fun saveSession(phoneNumber: String) {
    val normalized = normalizePhone(phoneNumber)
    prefs.edit().putString(KEY_PHONE, normalized).apply()
  }

  fun clearSession() {
    prefs.edit().remove(KEY_PHONE).apply()
  }

  companion object {
    private const val PREFS_NAME = "eczatakip_user_session"
    private const val KEY_PHONE = "logged_in_phone"

    /**
     * Normalizes any Turkish phone input into a canonical 10-digit string without leading 0
     * (e.g. "0507 123 12 12" -> "5071231212", "(507) 123 12 12" -> "5071231212").
     */
    fun normalizePhone(raw: String): String {
      var digits = raw.filter { it.isDigit() }
      if (digits.startsWith("90") && digits.length >= 12) {
        digits = digits.removePrefix("90")
      }
      while (digits.startsWith("0")) {
        digits = digits.removePrefix("0")
      }
      return digits.take(10)
    }

    /**
     * Formats a 10-digit phone number into "(507) 123 12 12" for display.
     */
    fun formatDisplayPhone(raw: String): String {
      val d = normalizePhone(raw)
      if (d.isEmpty()) return ""
      val sb = StringBuilder()
      sb.append("(")
      sb.append(d.take(3))
      if (d.length >= 3) sb.append(") ")
      if (d.length > 3) {
        sb.append(d.substring(3, minOf(6, d.length)))
      }
      if (d.length > 6) {
        sb.append(" ")
        sb.append(d.substring(6, minOf(8, d.length)))
      }
      if (d.length > 8) {
        sb.append(" ")
        sb.append(d.substring(8, minOf(10, d.length)))
      }
      return sb.toString()
    }

    /**
     * Hashes the user password with SHA-256 so raw passwords are never stored in MariaDB.
     */
    fun hashPassword(rawPassword: String): String {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = digest.digest(rawPassword.trim().toByteArray(Charsets.UTF_8))
      return bytes.joinToString("") { "%02x".format(it) }
    }

    fun normalizeSecurityAnswer(rawAnswer: String): String {
      return rawAnswer.trim().lowercase(java.util.Locale("tr", "TR")).replace(Regex("\\s+"), " ")
    }

    fun hashSecurityAnswer(rawAnswer: String): String {
      return hashPassword(normalizeSecurityAnswer(rawAnswer))
    }

    fun normalizeRecoveryCode(rawCode: String): String {
      return rawCode.trim().uppercase(java.util.Locale.US).replace(" ", "")
    }

    fun hashRecoveryCode(rawCode: String): String {
      return hashPassword(normalizeRecoveryCode(rawCode))
    }

    fun generateRecoveryCode(): String {
      val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
      val suffix = (1..4).map { chars.random() }.joinToString("")
      return "ECZA-$suffix"
    }

    fun maskRecoveryCodeHint(rawCode: String): String {
      val clean = normalizeRecoveryCode(rawCode)
      if (clean.length <= 6) return "ECZA-****"
      return clean.take(6) + "**"
    }
  }
}
