package com.example.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object OCRParser {

  private const val LABEL_PATTERN =
    """(?:SKT|S\.?\s*K\.?\s*T\.?|SON\s*KULL?(?:ANMA|ANIM)?\.?\s*TAR(?:İHİ|IHI)?\.?|S\.?\s*KULL?\.?\s*TAR\.?|TETT|T\.?\s*E\.?\s*T\.?\s*T\.?|EXP(?:IRY)?(?:\s*DATE)?\.?|BBD|USE\s*BY|BEST\s*BEFORE)"""

  private val monthNameMap = mapOf(
    "OCA" to 1, "OCAK" to 1, "JAN" to 1,
    "ŞUB" to 2, "SUB" to 2, "ŞUBAT" to 2, "SUBAT" to 2, "FEB" to 2,
    "MAR" to 3, "MART" to 3,
    "NİS" to 4, "NIS" to 4, "NİSAN" to 4, "NISAN" to 4, "APR" to 4,
    "MAY" to 5, "MAYIS" to 5,
    "HAZ" to 6, "HAZİRAN" to 6, "HAZIRAN" to 6, "JUN" to 6,
    "TEM" to 7, "TEMMUZ" to 7, "JUL" to 7,
    "AĞU" to 8, "AGU" to 8, "AĞUSTOS" to 8, "AGUSTOS" to 8, "AUG" to 8,
    "EYL" to 9, "EYLÜL" to 9, "EYLUL" to 9, "SEP" to 9,
    "EKİ" to 10, "EKI" to 10, "EKİM" to 10, "EKIM" to 10, "OCT" to 10,
    "KAS" to 11, "KASIM" to 11, "NOV" to 11,
    "ARA" to 12, "ARALIK" to 12, "DEC" to 12
  )

  /**
   * Parses free-text OCR scan from medicine boxes, cosmetics, and sunscreens
   * and extracts the expiry date in YYYY-MM-DD format.
   *
   * Supports:
   * - Labeled dates with any Turkish or English prefix (SKT, S.K.T., SON KUL. TAR., SON KULLANMA TARİHİ, TETT, EXP, BBD)
   * - Standalone dates shown directly to the camera without any prefix:
   *   - YYYY-MM-DD, YYYY.MM.DD, YYYY/MM/DD
   *   - DD.MM.YYYY, DD/MM/YYYY, DD-MM-YYYY
   *   - MM/YYYY, MM.YYYY, MM-YYYY, MM YYYY
   *   - YYYY/MM, YYYY.MM
   *   - MM/YY, MM.YY
   *   - Month Name + Year (e.g. EKİM 2027, 10 AĞU 2028, EXP OCT 27)
   */
  fun parseExpiryFromOCR(ocrText: String): String? {
    if (ocrText.isBlank()) return null

    val upper = ocrText.uppercase(Locale("tr", "TR"))

    // 1. Check explicitly labeled dates first (highest confidence)
    val labeledFullIso = Regex("""$LABEL_PATTERN\s*[:\-\.]?\s*(20\d{2})[\-/\.\s](\d{1,2})[\-/\.\s](\d{1,2})""")
    labeledFullIso.find(upper)?.let { m ->
      formatValidYmd(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let { return it }
    }

    val labeledDmy = Regex("""$LABEL_PATTERN\s*[:\-\.]?\s*(\d{1,2})[\-/\.\s](\d{1,2})[\-/\.\s](20\d{2}|\d{2})\b""")
    labeledDmy.find(upper)?.let { m ->
      val day = m.groupValues[1].toInt()
      val month = m.groupValues[2].toInt()
      val year = normalizeYear(m.groupValues[3].toInt())
      formatValidYmd(year, month, day)?.let { return it }
    }

    val labeledMy = Regex("""$LABEL_PATTERN\s*[:\-\.]?\s*(\d{1,2})[\-/\.\s](20\d{2}|\d{2})\b""")
    labeledMy.find(upper)?.let { m ->
      val month = m.groupValues[1].toInt()
      val year = normalizeYear(m.groupValues[2].toInt())
      formatEndOfMonth(year, month)?.let { return it }
    }

    // 2. Check Month Name + Year (e.g. "SON KUL TAR: EKİM 2027", "AĞUSTOS 2028", "EXP OCT 27")
    val monthNameRegex = Regex("""\b([A-ZÇĞİÖŞÜ]{3,7})[\.\s\-/]+(20\d{2}|\d{2})\b""")
    for (match in monthNameRegex.findAll(upper)) {
      val mName = match.groupValues[1]
      val month = monthNameMap[mName] ?: continue
      val year = normalizeYear(match.groupValues[2].toIntOrNull() ?: continue)
      if (year in 2022..2045) {
        formatEndOfMonth(year, month)?.let { return it }
      }
    }

    // 3. Check standalone YYYY-MM-DD or YYYY.MM.DD or YYYY/MM/DD
    val standaloneIso = Regex("""\b(20\d{2})[\-/\.](0?[1-9]|1[0-2])[\-/\.](0?[1-9]|[12]\d|3[01])\b""")
    for (match in standaloneIso.findAll(upper)) {
      val year = match.groupValues[1].toInt()
      val month = match.groupValues[2].toInt()
      val day = match.groupValues[3].toInt()
      formatValidYmd(year, month, day)?.let { return it }
    }

    // 4. Check standalone DD.MM.YYYY or DD/MM/YYYY or DD-MM-YYYY
    val standaloneDmy = Regex("""\b(0?[1-9]|[12]\d|3[01])[\-/\.](0?[1-9]|1[0-2])[\-/\.](20\d{2})\b""")
    for (match in standaloneDmy.findAll(upper)) {
      val day = match.groupValues[1].toInt()
      val month = match.groupValues[2].toInt()
      val year = match.groupValues[3].toInt()
      formatValidYmd(year, month, day)?.let { return it }
    }

    // 5. Check standalone MM/YYYY or MM.YYYY or MM-YYYY or MM YYYY
    val standaloneM4y = Regex("""\b(0?[1-9]|1[0-2])[\-/\.\s](20[2-4]\d)\b""")
    for (match in standaloneM4y.findAll(upper)) {
      val month = match.groupValues[1].toInt()
      val year = match.groupValues[2].toInt()
      formatEndOfMonth(year, month)?.let { return it }
    }

    // 6. Check standalone YYYY/MM or YYYY.MM or YYYY-MM
    val standalone4yM = Regex("""\b(20[2-4]\d)[\-/\.](0?[1-9]|1[0-2])\b""")
    for (match in standalone4yM.findAll(upper)) {
      val year = match.groupValues[1].toInt()
      val month = match.groupValues[2].toInt()
      formatEndOfMonth(year, month)?.let { return it }
    }

    // 7. Check standalone MM/YY or MM.YY (e.g. 08/27, 12.28)
    val standaloneM2y = Regex("""\b(0[1-9]|1[0-2])[\-/\.]([2-3]\d)\b""")
    for (match in standaloneM2y.findAll(upper)) {
      val month = match.groupValues[1].toInt()
      val year = 2000 + match.groupValues[2].toInt()
      formatEndOfMonth(year, month)?.let { return it }
    }

    return null
  }

  private fun normalizeYear(y: Int): Int = if (y < 100) 2000 + y else y

  private fun formatValidYmd(year: Int, month: Int, day: Int): String? {
    if (year !in 2020..2045 || month !in 1..12 || day !in 1..31) return null
    return try {
      LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_DATE)
    } catch (_: Exception) {
      null
    }
  }

  private fun formatEndOfMonth(year: Int, month: Int): String? {
    if (year !in 2020..2045 || month !in 1..12) return null
    return try {
      LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).format(DateTimeFormatter.ISO_DATE)
    } catch (_: Exception) {
      null
    }
  }
}
