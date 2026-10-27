package com.example

import com.example.utils.ITSBarcodeParser
import org.junit.Assert.*
import org.junit.Test

class ITSBarcodeParserTest {

  @Test
  fun testValidITSBarcodeGS1Standard() {
    // 01: GTIN (14 digits) 08699508090014
    // 17: Expiry date YYMMDD -> 261031 (2026-10-31)
    // 10: Batch -> ABC123
    // 21: Serial -> SER456
    val rawBarcode = "01086995080900141726103110ABC12321SER456"
    val result = ITSBarcodeParser.parseITSBarcode(rawBarcode)

    assertTrue(result.isValid)
    assertEquals("08699508090014", result.gtin)
    assertEquals("2026-10-31", result.expiryDate)
    assertEquals("ABC123", result.batchNumber)
    assertEquals("SER456", result.serialNumber)
    assertNull(result.errorMessage)
  }

  @Test
  fun testValidITSBarcodeParenthesesFormat() {
    val parenthesized = "(01)08699508090014(17)261031(10)LOT789(21)SER999"
    val result = ITSBarcodeParser.parseITSBarcode(parenthesized)

    assertTrue(result.isValid)
    assertEquals("08699508090014", result.gtin)
    assertEquals("2026-10-31", result.expiryDate)
    assertEquals("LOT789", result.batchNumber)
    assertEquals("SER999", result.serialNumber)
  }

  @Test
  fun testMalformedOrBlankBarcodeGracefulError() {
    val blankResult = ITSBarcodeParser.parseITSBarcode("")
    assertFalse(blankResult.isValid)
    assertNotNull(blankResult.errorMessage)

    val incompleteResult = ITSBarcodeParser.parseITSBarcode("INVALID_BARCODE_STRING")
    assertFalse(incompleteResult.isValid)
    assertNotNull(incompleteResult.errorMessage)
  }
}
