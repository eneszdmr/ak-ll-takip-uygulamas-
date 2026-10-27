package com.example.utils

data class ITSBarcodeResult(
  val gtin: String? = null,
  val serialNumber: String? = null,
  val expiryDate: String? = null, // YYYY-MM-DD
  val batchNumber: String? = null,
  val rawString: String,
  val isValid: Boolean,
  val errorMessage: String? = null
)

object ITSBarcodeParser {

  /**
   * Parses Turkey ITS (İlaç Takip Sistemi) GS1 DataMatrix string.
   * Supports GS1 FNC1 / ASCII 29 (\u001D) separators and parenthesis format e.g. (01)...(17)...
   */
  fun parseITSBarcode(rawString: String): ITSBarcodeResult {
    if (rawString.isBlank()) {
      return ITSBarcodeResult(rawString = rawString, isValid = false, errorMessage = "Barkod boş olamaz.")
    }

    try {
      // Normalize ASCII 29 or FNC1
      val cleanString = rawString.replace("\u001D", " ").replace(29.toChar().toString(), " ")

      var gtin: String? = null
      var serialNumber: String? = null
      var expiryDate: String? = null
      var batchNumber: String? = null

      // Check if parentheses format e.g. (01)08699508090014(17)261031(10)ABC(21)123
      if (cleanString.contains("(")) {
        val aiRegex = Regex("""\((\d{2})\)([^()]+)""")
        val matches = aiRegex.findAll(cleanString)
        for (match in matches) {
          val ai = match.groupValues[1]
          val value = match.groupValues[2]
          when (ai) {
            "01" -> gtin = value.trim()
            "17" -> expiryDate = parseYYMMDD(value.trim())
            "10" -> batchNumber = value.trim()
            "21" -> serialNumber = value.trim()
          }
        }
      } else {
        // Standard GS1 parsing with AI prefixes (01, 17, 10, 21)
        var remaining = cleanString.trim()
        
        while (remaining.isNotEmpty()) {
          when {
            remaining.startsWith("01") && remaining.length >= 16 -> {
              gtin = remaining.substring(2, 16)
              remaining = remaining.substring(16)
            }
            remaining.startsWith("17") && remaining.length >= 8 -> {
              val yyMMdd = remaining.substring(2, 8)
              expiryDate = parseYYMMDD(yyMMdd)
              remaining = remaining.substring(8)
            }
            remaining.startsWith("10") -> {
              // AI 10 (Batch) is variable length, terminated by space or end of string or next AI
              val nextIdx = findNextAI(remaining.substring(2))
              val batchEnd = if (nextIdx == -1) remaining.length else nextIdx + 2
              batchNumber = remaining.substring(2, batchEnd).trim()
              remaining = remaining.substring(batchEnd)
            }
            remaining.startsWith("21") -> {
              val nextIdx = findNextAI(remaining.substring(2))
              val serialEnd = if (nextIdx == -1) remaining.length else nextIdx + 2
              serialNumber = remaining.substring(2, serialEnd).trim()
              remaining = remaining.substring(serialEnd)
            }
            else -> {
              // Skip unknown char or separator
              remaining = remaining.drop(1)
            }
          }
        }
      }

      val isValid = !gtin.isNullOrBlank() && !expiryDate.isNullOrBlank()
      val error = if (!isValid) "Zorunlu alanlar (GTIN veya SKT) eksik veya hatalı." else null

      return ITSBarcodeResult(
        gtin = gtin,
        serialNumber = serialNumber,
        expiryDate = expiryDate,
        batchNumber = batchNumber,
        rawString = rawString,
        isValid = isValid,
        errorMessage = error
      )
    } catch (e: Exception) {
      return ITSBarcodeResult(
        rawString = rawString,
        isValid = false,
        errorMessage = "Karekod ayrıştırma hatası: ${e.message}"
      )
    }
  }

  private fun parseYYMMDD(yymmdd: String): String? {
    if (yymmdd.length != 6) return null
    try {
      val yy = yymmdd.substring(0, 2).toInt()
      val mm = yymmdd.substring(2, 4).toInt()
      val dd = yymmdd.substring(4, 6).toInt()

      val year = 2000 + yy
      val day = if (dd == 0) 28 else dd // If day is 00 (e.g. end of month), use 28 or last day

      return String.format("%04d-%02d-%02d", year, mm, day)
    } catch (e: Exception) {
      return null
    }
  }

  private fun findNextAI(str: String): Int {
    // Look for known AIs preceded by space or start: 01, 17, 10, 21
    val ais = listOf("01", "17", "10", "21")
    var minIdx = -1
    for (ai in ais) {
      val idx = str.indexOf(ai)
      if (idx != -1 && (minIdx == -1 || idx < minIdx)) {
        minIdx = idx
      }
    }
    return minIdx
  }
}
