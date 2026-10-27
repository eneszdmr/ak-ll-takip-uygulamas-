package com.example

import com.example.utils.OCRParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OCRParserTest {

  @Test
  fun testSunscreenOCRExpFormat() {
    val ocrText = "LOREAL SUN LOTION SPF 50 EXP: 08/27 LOT 40X"
    val parsedDate = OCRParser.parseExpiryFromOCR(ocrText)

    assertNotNull(parsedDate)
    // 08/27 -> August 2027 -> Last day of August 2027 is 2027-08-31
    assertEquals("2027-08-31", parsedDate)
  }

  @Test
  fun testSunscreenOCRSKTFormat() {
    val ocrText = "NIVEA SUN CREAM SKT: 15.06.2028 BATCH 99A"
    val parsedDate = OCRParser.parseExpiryFromOCR(ocrText)

    assertNotNull(parsedDate)
    assertEquals("2028-06-15", parsedDate)
  }

  @Test
  fun testSunscreenOCRBBDFormat() {
    val ocrText = "SUNSCREEN BBD: 2028-12-31"
    val parsedDate = OCRParser.parseExpiryFromOCR(ocrText)

    assertNotNull(parsedDate)
    assertEquals("2028-12-31", parsedDate)
  }
}
