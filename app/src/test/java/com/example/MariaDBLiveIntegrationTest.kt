package com.example

import com.example.data.model.ProductEntity
import com.example.data.model.ProductReminderEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.data.remote.MariaDBRepository
import com.example.utils.OCRParser
import com.example.utils.SessionManager
import com.example.utils.TurkishProductKnowledgeBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MariaDBLiveIntegrationTest {

  @Test
  fun testDirectMariaDBConnectionAndPasswordAuthAndModules() = runBlocking {
    val repository = MariaDBRepository()

    // 1. Test SELECT 1
    val (connected, detail) = repository.testDbQueryDetailed()
    println("SELECT 1 Result: connected=$connected, detail=$detail")
    assertTrue("MariaDB SELECT 1 failed: $detail", connected)

    // 2. Test SHA-256 Password Auth & Registration on 'users' table
    val testPhone = "05551234567"
    val (authOk, authErr) = repository.authenticateOrRegisterUser(testPhone, "123456")
    assertTrue("Failed to authenticate/register user with password: $authErr", authOk)

    // Verify SHA-256 hash length is 64 hex chars
    val expectedHash = SessionManager.hashPassword("123456")
    assertEquals(64, expectedHash.length)

    // Verify wrong password fails
    val (wrongAuthOk, _) = repository.authenticateOrRegisterUser(testPhone, "yanlis_sifre_999")
    assertFalse("Expected wrong password to be rejected", wrongAuthOk)

    // 3. Test Product CRUD in 'products' table
    val existingProducts = repository.getProducts(testPhone)
    if (existingProducts.isEmpty()) {
      val sampleProduct = ProductEntity(
        name = "Augmentin BID 1000mg",
        category = "İlaç",
        storageLocation = "Banyo Dolabı",
        familyMember = "Ortak",
        expiryDate = "2027-10-31",
        openedDate = "2026-10-26",
        paoMonths = 6,
        notes = "Geniş spektrumlu antibiyotik",
        isOpened = true
      )
      assertTrue(repository.saveProduct(testPhone, sampleProduct))
    }
    val products = repository.getProducts(testPhone)
    assertTrue(products.isNotEmpty())

    // 4. Test Warranty CRUD in 'warranties' table and verify utf8mb4 Turkish characters
    val existingWarranties = repository.getWarranties(testPhone)
    if (existingWarranties.isEmpty()) {
      val sampleWarranty = WarrantyEntity(
        name = "Dyson V15 Süpürge",
        brandModel = "Dyson V15 Detect",
        category = "Küçük Ev Aleti",
        purchaseDate = "2026-10-26",
        warrantyMonths = 24,
        warrantyEndDate = "2028-10-26",
        storeInvoice = "Teknosa Fatura #TK-2026",
        notes = "2 yıl resmi distribütör garantili"
      )
      assertTrue(repository.saveWarranty(testPhone, sampleWarranty))
    }
    val warranties = repository.getWarranties(testPhone)
    assertTrue(warranties.isNotEmpty())
    assertFalse(
      "Turkish characters in warranties should not contain '?'",
      warranties.first().name.contains("?") || warranties.first().category.contains("?")
    )

    // 5. Test Subscription CRUD in 'subscriptions' table
    val existingSubs = repository.getSubscriptions(testPhone)
    if (existingSubs.isEmpty()) {
      val sampleSub = SubscriptionEntity(
        name = "Netflix Özel Paket",
        category = "Dijital Platform",
        billingCycle = "Aylık",
        price = "229.99",
        nextPaymentDate = "2026-11-26",
        autoRenew = true,
        notes = "4K Ultra HD Aile Planı"
      )
      assertTrue(repository.saveSubscription(testPhone, sampleSub))
    }
    val subs = repository.getSubscriptions(testPhone)
    assertTrue(subs.isNotEmpty())

    // 6. Test Vehicle & Insurance CRUD in 'vehicle_insurances' table
    val existingVehicles = repository.getVehicleInsurances(testPhone)
    if (existingVehicles.isEmpty()) {
      val sampleVehicle = VehicleInsuranceEntity(
        plateAndVehicle = "34 ABC 123 - Renault Megane",
        policyType = "Trafik Sigortası",
        company = "Anadolu Sigorta",
        startDate = "2026-10-26",
        expiryDate = "2027-10-26",
        notes = "Yıllık zorunlu trafik sigortası"
      )
      assertTrue(repository.saveVehicleInsurance(testPhone, sampleVehicle))
    }
    val vehicles = repository.getVehicleInsurances(testPhone)
    assertTrue(vehicles.isNotEmpty())

    // 7. Test Per-Product Notification Reminder CRUD in 'product_reminders' table
    val firstProduct = products.first()
    val reminder = ProductReminderEntity(
      moduleType = "ECZA",
      itemId = firstProduct.id,
      itemName = firstProduct.name,
      targetExpiryDate = firstProduct.expiryDate,
      daysBefore = 7, // 1 Hafta
      notifyTime = "09:30",
      soundTone = "CHIME",
      isEnabled = true
    )
    assertTrue(repository.saveReminder(testPhone, reminder))
    val reminders = repository.getReminders(testPhone)
    assertTrue(reminders.isNotEmpty())

    // 8. Test ITS Barcode + Product Knowledge Base resolution
    val scanned = TurkishProductKnowledgeBase.resolveBarcodeOrITS(
      "01086995080900141726103110ABC12321SER456"
    )
    assertEquals("Parol 500 mg 20 Tablet", scanned.name)
    assertEquals("2026-10-31", scanned.expiryDate)
    assertNotNull(scanned.whatItDoes)

    // 9. Test OCRParser with label variations and standalone dates
    assertEquals("2028-08-31", OCRParser.parseExpiryFromOCR("SON KUL. TAR.: 08/2028"))
    assertEquals("2027-11-15", OCRParser.parseExpiryFromOCR("S.K.T 15.11.2027"))
    assertEquals("2029-12-31", OCRParser.parseExpiryFromOCR("12/2029"))
    assertEquals("2028-05-20", OCRParser.parseExpiryFromOCR("20.05.2028"))
  }
}
