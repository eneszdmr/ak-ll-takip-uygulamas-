package com.example

import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpiryCalculatorTest {

  private val testToday = LocalDate.of(2026, 6, 1)

  @Test
  fun testUnopenedProductUsesClassicExpiry() {
    val expiry = "2027-06-01"
    val effective = ExpiryCalculator.calculateEffectiveExpiryDate(expiry, null, null, testToday)
    assertEquals("2027-06-01", effective)

    val days = ExpiryCalculator.getDaysLeft(effective, testToday)
    assertEquals(365, days)
    assertEquals(ExpiryStatus.SAFE, ExpiryCalculator.getStatus(days))
  }

  @Test
  fun testOpenedProductWithPaoBeforeExpiry() {
    // Classic expiry is 1 year later (2027-06-01), but opened on 2026-05-01 with PAO 1 month -> 2026-06-01
    // Let's test opened on 2026-05-15 with PAO 1 month -> 2026-06-15 (earlier than 2027-06-01)
    val expiry = "2027-06-01"
    val opened = "2026-05-15"
    val paoMonths = 1
    val effective = ExpiryCalculator.calculateEffectiveExpiryDate(expiry, opened, paoMonths, testToday)
    assertEquals("2026-06-15", effective)

    val days = ExpiryCalculator.getDaysLeft(effective, testToday)
    assertEquals(14, days) // June 1 to June 15 = 14 days
    assertEquals(ExpiryStatus.WARNING, ExpiryCalculator.getStatus(days))
  }

  @Test
  fun testExpiredOrCriticalProduct() {
    val expiry = "2026-06-05" // 4 days from testToday (June 1)
    val effective = ExpiryCalculator.calculateEffectiveExpiryDate(expiry, null, null, testToday)
    assertEquals("2026-06-05", effective)

    val days = ExpiryCalculator.getDaysLeft(effective, testToday)
    assertEquals(4, days)
    assertEquals(ExpiryStatus.CRITICAL, ExpiryCalculator.getStatus(days))
  }

  @Test
  fun testLeapYearAndMonthEndEdgeCases() {
    // Opened on 2024-01-31 (leap year) with 1 month PAO -> 2024-02-29
    val effective = ExpiryCalculator.calculateEffectiveExpiryDate("2025-01-31", "2024-01-31", 1, LocalDate.of(2024, 2, 1))
    assertEquals("2024-02-29", effective)
  }
}
