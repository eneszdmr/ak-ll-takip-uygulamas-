package com.example.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ExpiryStatus {
  SAFE,      // > 30 days
  WARNING,   // 7 - 30 days
  CRITICAL   // <= 7 days or expired
}

object ExpiryCalculator {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
   * Calculates effective expiry date comparing Classic Expiry Date and PAO (Period After Opening).
   * Effective Expiry = MIN(Classic Expiry Date, Opened Date + PAO Months)
   */
  fun calculateEffectiveExpiryDate(
    expiryDate: String,
    openedDate: String?,
    paoMonths: Int?,
    currentDate: LocalDate = LocalDate.now()
  ): String {
    val classicExpiry = try {
      LocalDate.parse(expiryDate, formatter)
    } catch (e: Exception) {
      currentDate.plusYears(1)
    }

    if (openedDate.isNullOrBlank() || paoMonths == null || paoMonths <= 0) {
      return classicExpiry.format(formatter)
    }

    val opened = try {
      LocalDate.parse(openedDate, formatter)
    } catch (e: Exception) {
      return classicExpiry.format(formatter)
    }

    val paoExpiry = opened.plusMonths(paoMonths.toLong())

    // Return the earlier of classicExpiry and paoExpiry
    return if (paoExpiry.isBefore(classicExpiry)) {
      paoExpiry.format(formatter)
    } else {
      classicExpiry.format(formatter)
    }
  }

  fun getDaysLeft(effectiveExpiryDate: String, currentDate: LocalDate = LocalDate.now()): Int {
    val effective = try {
      LocalDate.parse(effectiveExpiryDate, formatter)
    } catch (e: Exception) {
      return 999
    }
    return ChronoUnit.DAYS.between(currentDate, effective).toInt()
  }

  fun getStatus(daysLeft: Int): ExpiryStatus {
    return when {
      daysLeft <= 7 -> ExpiryStatus.CRITICAL
      daysLeft <= 30 -> ExpiryStatus.WARNING
      else -> ExpiryStatus.SAFE
    }
  }
}
