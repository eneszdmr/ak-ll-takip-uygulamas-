package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReminderEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.UserSecuritySummary
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.data.remote.MariaDBWireClient.Companion.sqlEscape
import com.example.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MariaDBRepository {
  private val rawUrl = BuildConfig.DB_URL.ifBlank { "jdbc:mariadb://31.186.11.44:3306/mar220aksesucom_mobildb" }
  private val defaultDb = BuildConfig.DB_NAME.ifBlank { "mar220aksesucom_mobildb" }
  private val user = BuildConfig.DB_USER.ifBlank { "mar220aksesucom_mobiluser" }
  private val password = BuildConfig.DB_PASSWORD.ifBlank { "J!@8z=zt.r2%5mp9" }

  @Volatile
  var lastErrorMessage: String? = null
    private set

  @Volatile
  private var schemaInitialized = false

  private val client: MariaDBWireClient by lazy {
    val (host, port, db) = MariaDBWireClient.parseJdbcUrl(rawUrl, defaultDb)
    MariaDBWireClient(
      host = host,
      port = port,
      database = db,
      user = user,
      password = password
    )
  }

  companion object {
    private const val TAG = "MariaDBRepository"
    private fun safeLogError(msg: String, e: Throwable) {
      try {
        Log.e(TAG, msg, e)
      } catch (_: Throwable) {
        println("[$TAG] $msg: ${e.message}")
      }
    }
  }

  suspend fun testDbQueryDetailed(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
      val result = client.withConnection { session ->
        val rows = session.executeQuery("SELECT 1 AS test_val")
        rows.firstOrNull()?.get("test_val")
      }
      if (result == "1") {
        lastErrorMessage = null
        true to "Veritabanı bağlantısı başarılı"
      } else {
        val msg = "Veritabanı yanıt vermedi"
        lastErrorMessage = msg
        false to msg
      }
    } catch (e: Exception) {
      safeLogError("MariaDB test query failed", e)
      val msg = "Veritabanı bağlantısı kurulamadı"
      lastErrorMessage = e.message ?: msg
      false to msg
    }
  }

  suspend fun testConnection(): Boolean = testDbQueryDetailed().first

  suspend fun testDbQuery(): Boolean = testDbQueryDetailed().first

  private fun ensureTablesInSession(session: MariaDBWireClient.Session) {
    session.executeUpdate("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci")
    if (schemaInitialized) return

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        phone_number VARCHAR(20) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NULL,
        recovery_email VARCHAR(255) NULL,
        firebase_uid VARCHAR(128) NULL,
        security_question VARCHAR(255) NULL,
        security_answer_hash VARCHAR(255) NULL,
        recovery_code_hash VARCHAR(255) NULL,
        recovery_code_hint VARCHAR(64) NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS recovery_email VARCHAR(255) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS security_question VARCHAR(255) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS security_answer_hash VARCHAR(255) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS recovery_code_hash VARCHAR(255) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users ADD COLUMN IF NOT EXISTS recovery_code_hint VARCHAR(64) NULL"
    )
    session.executeUpdate(
      "ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS products (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_phone VARCHAR(20) NOT NULL,
        name VARCHAR(255) NOT NULL,
        category VARCHAR(100) NOT NULL,
        storage_location VARCHAR(100) NOT NULL,
        family_member VARCHAR(100) NOT NULL,
        expiry_date DATE NOT NULL,
        opened_date DATE NULL,
        pao_months INT NULL,
        notes TEXT NULL,
        is_opened TINYINT(1) DEFAULT 0,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_user_phone (user_phone)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE products CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS warranties (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_phone VARCHAR(20) NOT NULL,
        name VARCHAR(255) NOT NULL,
        brand_model VARCHAR(255) NOT NULL,
        category VARCHAR(100) NOT NULL,
        purchase_date DATE NOT NULL,
        warranty_months INT DEFAULT 24,
        warranty_end_date DATE NOT NULL,
        store_invoice VARCHAR(255) NULL,
        notes TEXT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_warranty_user_phone (user_phone)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE warranties CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    // Repair any row in warranties that had '?' due to previous latin1 table collation
    session.executeUpdate(
      """
      UPDATE warranties
      SET name = 'Dyson V15 Süpürge',
          category = 'Küçük Ev Aleti',
          notes = '2 yıl resmi distribütör garantili'
      WHERE name LIKE 'Dyson V15 S%p%rge' OR category LIKE 'K%k Ev Aleti'
      """.trimIndent()
    )

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS subscriptions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_phone VARCHAR(20) NOT NULL,
        name VARCHAR(255) NOT NULL,
        category VARCHAR(100) NOT NULL,
        billing_cycle VARCHAR(50) NOT NULL DEFAULT 'Aylık',
        price VARCHAR(50) NULL,
        next_payment_date DATE NOT NULL,
        auto_renew TINYINT(1) DEFAULT 1,
        notes TEXT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_sub_user_phone (user_phone)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE subscriptions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS vehicle_insurances (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_phone VARCHAR(20) NOT NULL,
        plate_and_vehicle VARCHAR(255) NOT NULL,
        policy_type VARCHAR(100) NOT NULL,
        company VARCHAR(255) NULL,
        start_date DATE NOT NULL,
        expiry_date DATE NOT NULL,
        notes TEXT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_vehicle_user_phone (user_phone)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE vehicle_insurances CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    session.executeUpdate(
      """
      CREATE TABLE IF NOT EXISTS product_reminders (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_phone VARCHAR(20) NOT NULL,
        module_type VARCHAR(50) NOT NULL,
        item_id INT NOT NULL,
        item_name VARCHAR(255) NOT NULL,
        target_expiry_date DATE NOT NULL,
        days_before INT NOT NULL DEFAULT 7,
        notify_time VARCHAR(10) NOT NULL DEFAULT '09:00',
        sound_tone VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
        is_enabled TINYINT(1) NOT NULL DEFAULT 1,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_user_module_item (user_phone, module_type, item_id),
        INDEX idx_reminder_user_phone (user_phone)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      """.trimIndent()
    )
    session.executeUpdate(
      "ALTER TABLE product_reminders CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )

    schemaInitialized = true
  }

  private fun canonicalPhone(raw: String): String {
    val norm = SessionManager.normalizePhone(raw)
    return norm.ifBlank { raw.trim() }
  }

  private fun phoneInClause(column: String, rawPhone: String): String {
    val canonical = canonicalPhone(rawPhone)
    val withZero = "0$canonical"
    return "$column IN (${sqlEscape(canonical)}, ${sqlEscape(withZero)})"
  }

  private fun ensureUserRowInSession(session: MariaDBWireClient.Session, rawPhone: String): String {
    val canonical = canonicalPhone(rawPhone)
    val rows = session.executeQuery(
      "SELECT phone_number FROM users WHERE ${phoneInClause("phone_number", canonical)} LIMIT 1"
    )
    return if (rows.isNotEmpty()) {
      rows.first()["phone_number"] ?: canonical
    } else {
      session.executeUpdate("INSERT IGNORE INTO users (phone_number) VALUES (${sqlEscape(canonical)})")
      canonical
    }
  }

  suspend fun authenticateOrRegisterUser(
    phoneNumber: String,
    rawPassword: String
  ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    val hashedInput = SessionManager.hashPassword(rawPassword)

    try {
      client.withConnection { session ->
        ensureTablesInSession(session)

        val rows = session.executeQuery(
          "SELECT id, phone_number, password_hash FROM users WHERE ${phoneInClause("phone_number", cleanPhone)} LIMIT 1"
        )

        if (rows.isEmpty()) {
          session.executeUpdate(
            "INSERT INTO users (phone_number, password_hash) VALUES (${sqlEscape(cleanPhone)}, ${sqlEscape(hashedInput)})"
          )
          lastErrorMessage = null
          true to null
        } else {
          val matchedRow = rows.first()
          val existingHash = matchedRow["password_hash"]
          val matchedPhone = matchedRow["phone_number"] ?: cleanPhone
          if (existingHash.isNullOrBlank()) {
            session.executeUpdate(
              "UPDATE users SET password_hash = ${sqlEscape(hashedInput)}, phone_number = ${sqlEscape(cleanPhone)} WHERE phone_number = ${sqlEscape(matchedPhone)}"
            )
            lastErrorMessage = null
            true to null
          } else if (existingHash.equals(hashedInput, ignoreCase = true)) {
            lastErrorMessage = null
            true to null
          } else {
            val err = "Girdiğiniz şifre bu telefon numarası için hatalı. Şifrenizi unuttuysanız aşağıdaki 'Şifremi Unuttum' seçeneğini kullanabilirsiniz."
            lastErrorMessage = err
            false to err
          }
        }
      }
    } catch (e: Exception) {
      safeLogError("Error authenticating/registering user", e)
      val err = "Sunucu bağlantı hatası: ${e.message ?: "Bilinmeyen hata"}"
      lastErrorMessage = err
      false to err
    }
  }

  suspend fun getUserSecuritySummary(phoneNumber: String): UserSecuritySummary? = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    if (cleanPhone.length < 10) return@withContext null
    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val rows = session.executeQuery(
          "SELECT phone_number, recovery_email, firebase_uid, security_question, " +
            "security_answer_hash, recovery_code_hash, recovery_code_hint " +
            "FROM users WHERE ${phoneInClause("phone_number", cleanPhone)} LIMIT 1"
        )
        val row = rows.firstOrNull() ?: return@withConnection null
        UserSecuritySummary(
          phoneNumber = cleanPhone,
          recoveryEmail = row["recovery_email"]?.takeIf { it.isNotBlank() },
          firebaseUid = row["firebase_uid"]?.takeIf { it.isNotBlank() },
          securityQuestion = row["security_question"]?.takeIf { it.isNotBlank() },
          hasSecurityAnswer = !row["security_answer_hash"].isNullOrBlank(),
          hasRecoveryCode = !row["recovery_code_hash"].isNullOrBlank(),
          recoveryCodeHint = row["recovery_code_hint"]?.takeIf { it.isNotBlank() }
        )
      }
    } catch (e: Exception) {
      safeLogError("Error fetching user security summary", e)
      null
    }
  }

  suspend fun saveUserRecoveryConfig(
    phoneNumber: String,
    recoveryEmail: String?,
    firebaseUid: String?,
    securityQuestion: String?,
    securityAnswerRaw: String?,
    rawRecoveryCode: String?
  ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    if (cleanPhone.length != 10) {
      return@withContext false to "Geçerli bir 10 haneli telefon numarası gereklidir."
    }
    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectivePhone = ensureUserRowInSession(session, cleanPhone)
        val updates = mutableListOf<String>()

        if (!recoveryEmail.isNullOrBlank()) {
          updates.add("recovery_email = ${sqlEscape(recoveryEmail.trim())}")
        }
        if (!firebaseUid.isNullOrBlank()) {
          updates.add("firebase_uid = ${sqlEscape(firebaseUid.trim())}")
        }
        if (!securityQuestion.isNullOrBlank()) {
          updates.add("security_question = ${sqlEscape(securityQuestion.trim())}")
        }
        if (!securityAnswerRaw.isNullOrBlank()) {
          val answerHash = SessionManager.hashSecurityAnswer(securityAnswerRaw)
          updates.add("security_answer_hash = ${sqlEscape(answerHash)}")
        }
        if (!rawRecoveryCode.isNullOrBlank()) {
          val codeHash = SessionManager.hashRecoveryCode(rawRecoveryCode)
          val codeHint = SessionManager.maskRecoveryCodeHint(rawRecoveryCode)
          updates.add("recovery_code_hash = ${sqlEscape(codeHash)}")
          updates.add("recovery_code_hint = ${sqlEscape(codeHint)}")
        }

        if (updates.isNotEmpty()) {
          val sql = "UPDATE users SET ${updates.joinToString(", ")} WHERE phone_number = ${sqlEscape(effectivePhone)}"
          session.executeUpdate(sql)
        }
        true to "Güvenlik ve şifre kurtarma ayarlarınız başarıyla kaydedildi."
      }
    } catch (e: Exception) {
      safeLogError("Error saving user recovery config", e)
      false to "Kurtarma ayarları kaydedilemedi: ${e.message ?: "Bilinmeyen hata"}"
    }
  }

  suspend fun resetPasswordBySecurityAnswerOrCode(
    phoneNumber: String,
    securityAnswerRaw: String?,
    recoveryCodeRaw: String?,
    newRawPassword: String,
    newSecurityQuestion: String? = null
  ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    if (cleanPhone.length != 10) {
      return@withContext false to "Lütfen telefon numaranızı başında 0 olmadan 10 hane olarak giriniz."
    }
    if (newRawPassword.trim().length < 4) {
      return@withContext false to "Yeni şifreniz en az 4 karakter olmalıdır."
    }
    val newPasswordHash = SessionManager.hashPassword(newRawPassword)

    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val rows = session.executeQuery(
          "SELECT phone_number, security_question, security_answer_hash, recovery_code_hash " +
            "FROM users WHERE ${phoneInClause("phone_number", cleanPhone)} LIMIT 1"
        )
        if (rows.isEmpty()) {
          return@withConnection false to "Bu telefon numarasına ait kayıtlı bir hesap bulunamadı."
        }
        val row = rows.first()
        val matchedPhone = row["phone_number"] ?: cleanPhone
        val storedAnswerHash = row["security_answer_hash"]?.takeIf { it.isNotBlank() }
        val storedCodeHash = row["recovery_code_hash"]?.takeIf { it.isNotBlank() }

        // If account has neither security answer nor recovery code configured yet (legacy user),
        // allow setting security question + answer while resetting password so user isn't locked out.
        if (storedAnswerHash == null && storedCodeHash == null) {
          if (securityAnswerRaw.isNullOrBlank()) {
            return@withConnection false to "Bu hesapta henüz güvenlik cevabı tanımlanmamış. Lütfen yeni bir güvenlik cevabı veya Google E-posta doğrulaması giriniz."
          }
          val newAnswerHash = SessionManager.hashSecurityAnswer(securityAnswerRaw)
          val questionToSave = newSecurityQuestion?.takeIf { it.isNotBlank() } ?: "İlkokul öğretmeninizin adı nedir?"
          session.executeUpdate(
            "UPDATE users SET password_hash = ${sqlEscape(newPasswordHash)}, " +
              "security_question = ${sqlEscape(questionToSave)}, " +
              "security_answer_hash = ${sqlEscape(newAnswerHash)} " +
              "WHERE phone_number = ${sqlEscape(matchedPhone)}"
          )
          return@withConnection true to "Şifreniz sıfırlandı ve güvenlik sorunuz tanımlandı!"
        }

        val answerMatched = !securityAnswerRaw.isNullOrBlank() &&
          storedAnswerHash != null &&
          storedAnswerHash.equals(SessionManager.hashSecurityAnswer(securityAnswerRaw), ignoreCase = true)

        val codeMatched = !recoveryCodeRaw.isNullOrBlank() &&
          storedCodeHash != null &&
          storedCodeHash.equals(SessionManager.hashRecoveryCode(recoveryCodeRaw), ignoreCase = true)

        if (!answerMatched && !codeMatched) {
          return@withConnection false to "Girdiğiniz güvenlik cevabı veya kurtarma kodu doğrulanamadı."
        }

        session.executeUpdate(
          "UPDATE users SET password_hash = ${sqlEscape(newPasswordHash)} WHERE phone_number = ${sqlEscape(matchedPhone)}"
        )
        true to "Şifreniz başarıyla sıfırlandı! Yeni şifrenizle giriş yapabilirsiniz."
      }
    } catch (e: Exception) {
      safeLogError("Error resetting password with security answer/code", e)
      false to "Şifre sıfırlama hatası: ${e.message ?: "Bağlantı kurulamadı"}"
    }
  }

  suspend fun resetPasswordByVerifiedFirebaseAccount(
    phoneNumber: String,
    verifiedEmail: String,
    firebaseUid: String,
    newRawPassword: String,
    firestoreLinkedPhone: String? = null
  ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    if (cleanPhone.length != 10) {
      return@withContext false to "Lütfen 10 haneli telefon numaranızı giriniz."
    }
    if (newRawPassword.trim().length < 4) {
      return@withContext false to "Yeni şifreniz en az 4 karakter olmalıdır."
    }
    val newPasswordHash = SessionManager.hashPassword(newRawPassword)

    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val rows = session.executeQuery(
          "SELECT phone_number, recovery_email, firebase_uid FROM users WHERE ${phoneInClause("phone_number", cleanPhone)} LIMIT 1"
        )
        if (rows.isEmpty()) {
          // Create the user account with this verified Firebase identity and new password
          session.executeUpdate(
            "INSERT INTO users (phone_number, password_hash, recovery_email, firebase_uid) VALUES (" +
              "${sqlEscape(cleanPhone)}, ${sqlEscape(newPasswordHash)}, ${sqlEscape(verifiedEmail.trim())}, ${sqlEscape(firebaseUid.trim())})"
          )
          return@withConnection true to "Firebase E-posta doğrulaması ile hesabınız ve yeni şifreniz oluşturuldu!"
        }

        val row = rows.first()
        val matchedPhone = row["phone_number"] ?: cleanPhone
        val existingEmail = row["recovery_email"]?.takeIf { it.isNotBlank() }
        val existingUid = row["firebase_uid"]?.takeIf { it.isNotBlank() }

        val emailMatches = existingEmail == null || existingEmail.equals(verifiedEmail.trim(), ignoreCase = true)
        val uidMatches = existingUid == null || existingUid == firebaseUid.trim()
        val firestorePhoneMatches = firestoreLinkedPhone != null && SessionManager.normalizePhone(firestoreLinkedPhone) == cleanPhone

        if (!emailMatches && !uidMatches && !firestorePhoneMatches) {
          return@withConnection false to "Bu telefon numarası farklı bir kurtarma e-postası ile eşleştirilmiş ($existingEmail)."
        }

        session.executeUpdate(
          "UPDATE users SET password_hash = ${sqlEscape(newPasswordHash)}, " +
            "recovery_email = ${sqlEscape(verifiedEmail.trim())}, " +
            "firebase_uid = ${sqlEscape(firebaseUid.trim())} " +
            "WHERE phone_number = ${sqlEscape(matchedPhone)}"
        )
        true to "Firebase / Google E-posta doğrulaması ile MariaDB şifreniz başarıyla sıfırlandı!"
      }
    } catch (e: Exception) {
      safeLogError("Error resetting password with Firebase account", e)
      false to "Şifre sıfırlama hatası: ${e.message ?: "Bağlantı kurulamadı"}"
    }
  }

  suspend fun ensureUserExists(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
    val cleanPhone = canonicalPhone(phoneNumber)
    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeUpdate(
          "INSERT IGNORE INTO users (phone_number) VALUES (${sqlEscape(cleanPhone)})"
        )
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error ensuring user exists", e)
      lastErrorMessage = e.message
      false
    }
  }

  // --- 1. Ecza & Kozmetik Takip (products) ---

  suspend fun getProducts(phoneNumber: String): List<ProductEntity> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ProductEntity>()
    try {
      val rows = client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeQuery(
          "SELECT id, user_phone, name, category, storage_location, family_member, " +
            "expiry_date, opened_date, pao_months, notes, is_opened " +
            "FROM products WHERE ${phoneInClause("user_phone", phoneNumber)} " +
            "ORDER BY expiry_date ASC, id DESC"
        )
      }
      for (row in rows) {
        val isOpenedRaw = row["is_opened"] ?: "0"
        val isOpened = isOpenedRaw == "1" || isOpenedRaw.equals("true", ignoreCase = true)
        list.add(
          ProductEntity(
            id = row["id"]?.toLongOrNull() ?: 0L,
            name = row["name"] ?: "",
            category = row["category"] ?: "İlaç",
            storageLocation = row["storage_location"] ?: "Banyo Dolabı",
            familyMember = row["family_member"] ?: "Ortak",
            expiryDate = row["expiry_date"] ?: "",
            openedDate = row["opened_date"]?.takeIf { it.isNotBlank() },
            paoMonths = row["pao_months"]?.toIntOrNull(),
            notes = row["notes"] ?: "",
            isOpened = isOpened
          )
        )
      }
      lastErrorMessage = null
    } catch (e: Exception) {
      safeLogError("Error fetching products", e)
      lastErrorMessage = e.message
    }
    list
  }

  suspend fun saveProduct(phoneNumber: String, product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
    try {
      val openedDateSql = if (product.isOpened && !product.openedDate.isNullOrBlank()) {
        sqlEscape(product.openedDate)
      } else {
        "NULL"
      }
      val paoSql = if (product.isOpened && product.paoMonths != null) {
        product.paoMonths.toString()
      } else {
        "NULL"
      }
      val isOpenedInt = if (product.isOpened) 1 else 0

      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectiveUserPhone = ensureUserRowInSession(session, phoneNumber)
        if (product.id == 0L) {
          val sql = "INSERT INTO products " +
            "(user_phone, name, category, storage_location, family_member, expiry_date, opened_date, pao_months, notes, is_opened) " +
            "VALUES (" +
            "${sqlEscape(effectiveUserPhone)}, " +
            "${sqlEscape(product.name)}, " +
            "${sqlEscape(product.category)}, " +
            "${sqlEscape(product.storageLocation)}, " +
            "${sqlEscape(product.familyMember)}, " +
            "${sqlEscape(product.expiryDate)}, " +
            "$openedDateSql, " +
            "$paoSql, " +
            "${sqlEscape(product.notes)}, " +
            "$isOpenedInt)"
          session.executeUpdate(sql)
        } else {
          val sql = "UPDATE products SET " +
            "name = ${sqlEscape(product.name)}, " +
            "category = ${sqlEscape(product.category)}, " +
            "storage_location = ${sqlEscape(product.storageLocation)}, " +
            "family_member = ${sqlEscape(product.familyMember)}, " +
            "expiry_date = ${sqlEscape(product.expiryDate)}, " +
            "opened_date = $openedDateSql, " +
            "pao_months = $paoSql, " +
            "notes = ${sqlEscape(product.notes)}, " +
            "is_opened = $isOpenedInt " +
            "WHERE id = ${product.id}"
          session.executeUpdate(sql)
        }
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error saving product", e)
      lastErrorMessage = e.message
      false
    }
  }

  suspend fun deleteProduct(productId: Long): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        session.executeUpdate("DELETE FROM products WHERE id = $productId")
        session.executeUpdate("DELETE FROM product_reminders WHERE module_type = 'ECZA' AND item_id = $productId")
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error deleting product", e)
      lastErrorMessage = e.message
      false
    }
  }

  // --- 2. Garanti & Fatura Takip (warranties) ---

  suspend fun getWarranties(phoneNumber: String): List<WarrantyEntity> = withContext(Dispatchers.IO) {
    val list = mutableListOf<WarrantyEntity>()
    try {
      val rows = client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeQuery(
          "SELECT id, user_phone, name, brand_model, category, purchase_date, " +
            "warranty_months, warranty_end_date, store_invoice, notes " +
            "FROM warranties WHERE ${phoneInClause("user_phone", phoneNumber)} " +
            "ORDER BY warranty_end_date ASC, id DESC"
        )
      }
      for (row in rows) {
        list.add(
          WarrantyEntity(
            id = row["id"]?.toLongOrNull() ?: 0L,
            name = row["name"] ?: "",
            brandModel = row["brand_model"] ?: "",
            category = row["category"] ?: "Elektronik",
            purchaseDate = row["purchase_date"] ?: "",
            warrantyMonths = row["warranty_months"]?.toIntOrNull() ?: 24,
            warrantyEndDate = row["warranty_end_date"] ?: "",
            storeInvoice = row["store_invoice"] ?: "",
            notes = row["notes"] ?: ""
          )
        )
      }
      lastErrorMessage = null
    } catch (e: Exception) {
      safeLogError("Error fetching warranties", e)
      lastErrorMessage = e.message
    }
    list
  }

  suspend fun saveWarranty(phoneNumber: String, warranty: WarrantyEntity): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectiveUserPhone = ensureUserRowInSession(session, phoneNumber)
        if (warranty.id == 0L) {
          val sql = "INSERT INTO warranties " +
            "(user_phone, name, brand_model, category, purchase_date, warranty_months, warranty_end_date, store_invoice, notes) " +
            "VALUES (" +
            "${sqlEscape(effectiveUserPhone)}, " +
            "${sqlEscape(warranty.name)}, " +
            "${sqlEscape(warranty.brandModel)}, " +
            "${sqlEscape(warranty.category)}, " +
            "${sqlEscape(warranty.purchaseDate)}, " +
            "${warranty.warrantyMonths}, " +
            "${sqlEscape(warranty.warrantyEndDate)}, " +
            "${sqlEscape(warranty.storeInvoice)}, " +
            "${sqlEscape(warranty.notes)})"
          session.executeUpdate(sql)
        } else {
          val sql = "UPDATE warranties SET " +
            "name = ${sqlEscape(warranty.name)}, " +
            "brand_model = ${sqlEscape(warranty.brandModel)}, " +
            "category = ${sqlEscape(warranty.category)}, " +
            "purchase_date = ${sqlEscape(warranty.purchaseDate)}, " +
            "warranty_months = ${warranty.warrantyMonths}, " +
            "warranty_end_date = ${sqlEscape(warranty.warrantyEndDate)}, " +
            "store_invoice = ${sqlEscape(warranty.storeInvoice)}, " +
            "notes = ${sqlEscape(warranty.notes)} " +
            "WHERE id = ${warranty.id}"
          session.executeUpdate(sql)
        }
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error saving warranty", e)
      lastErrorMessage = e.message
      false
    }
  }

  suspend fun deleteWarranty(warrantyId: Long): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        session.executeUpdate("DELETE FROM warranties WHERE id = $warrantyId")
        session.executeUpdate("DELETE FROM product_reminders WHERE module_type = 'GARANTI' AND item_id = $warrantyId")
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error deleting warranty", e)
      lastErrorMessage = e.message
      false
    }
  }

  // --- 3. Abonelik & Fatura Takip (subscriptions) ---

  suspend fun getSubscriptions(phoneNumber: String): List<SubscriptionEntity> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SubscriptionEntity>()
    try {
      val rows = client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeQuery(
          "SELECT id, user_phone, name, category, billing_cycle, price, next_payment_date, auto_renew, notes " +
            "FROM subscriptions WHERE ${phoneInClause("user_phone", phoneNumber)} " +
            "ORDER BY next_payment_date ASC, id DESC"
        )
      }
      for (row in rows) {
        val autoRenewRaw = row["auto_renew"] ?: "1"
        list.add(
          SubscriptionEntity(
            id = row["id"]?.toLongOrNull() ?: 0L,
            name = row["name"] ?: "",
            category = row["category"] ?: "Dijital Platform",
            billingCycle = row["billing_cycle"] ?: "Aylık",
            price = row["price"] ?: "",
            nextPaymentDate = row["next_payment_date"] ?: "",
            autoRenew = autoRenewRaw == "1" || autoRenewRaw.equals("true", ignoreCase = true),
            notes = row["notes"] ?: ""
          )
        )
      }
      lastErrorMessage = null
    } catch (e: Exception) {
      safeLogError("Error fetching subscriptions", e)
      lastErrorMessage = e.message
    }
    list
  }

  suspend fun saveSubscription(phoneNumber: String, sub: SubscriptionEntity): Boolean = withContext(Dispatchers.IO) {
    try {
      val autoRenewInt = if (sub.autoRenew) 1 else 0
      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectiveUserPhone = ensureUserRowInSession(session, phoneNumber)
        if (sub.id == 0L) {
          val sql = "INSERT INTO subscriptions " +
            "(user_phone, name, category, billing_cycle, price, next_payment_date, auto_renew, notes) " +
            "VALUES (" +
            "${sqlEscape(effectiveUserPhone)}, " +
            "${sqlEscape(sub.name)}, " +
            "${sqlEscape(sub.category)}, " +
            "${sqlEscape(sub.billingCycle)}, " +
            "${sqlEscape(sub.price)}, " +
            "${sqlEscape(sub.nextPaymentDate)}, " +
            "$autoRenewInt, " +
            "${sqlEscape(sub.notes)})"
          session.executeUpdate(sql)
        } else {
          val sql = "UPDATE subscriptions SET " +
            "name = ${sqlEscape(sub.name)}, " +
            "category = ${sqlEscape(sub.category)}, " +
            "billing_cycle = ${sqlEscape(sub.billingCycle)}, " +
            "price = ${sqlEscape(sub.price)}, " +
            "next_payment_date = ${sqlEscape(sub.nextPaymentDate)}, " +
            "auto_renew = $autoRenewInt, " +
            "notes = ${sqlEscape(sub.notes)} " +
            "WHERE id = ${sub.id}"
          session.executeUpdate(sql)
        }
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error saving subscription", e)
      lastErrorMessage = e.message
      false
    }
  }

  suspend fun deleteSubscription(subscriptionId: Long): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        session.executeUpdate("DELETE FROM subscriptions WHERE id = $subscriptionId")
        session.executeUpdate("DELETE FROM product_reminders WHERE module_type = 'ABONELIK' AND item_id = $subscriptionId")
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error deleting subscription", e)
      lastErrorMessage = e.message
      false
    }
  }

  // --- 4. Araç & Sigorta Takip (vehicle_insurances) ---

  suspend fun getVehicleInsurances(phoneNumber: String): List<VehicleInsuranceEntity> = withContext(Dispatchers.IO) {
    val list = mutableListOf<VehicleInsuranceEntity>()
    try {
      val rows = client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeQuery(
          "SELECT id, user_phone, plate_and_vehicle, policy_type, company, start_date, expiry_date, notes " +
            "FROM vehicle_insurances WHERE ${phoneInClause("user_phone", phoneNumber)} " +
            "ORDER BY expiry_date ASC, id DESC"
        )
      }
      for (row in rows) {
        list.add(
          VehicleInsuranceEntity(
            id = row["id"]?.toLongOrNull() ?: 0L,
            plateAndVehicle = row["plate_and_vehicle"] ?: "",
            policyType = row["policy_type"] ?: "Trafik Sigortası",
            company = row["company"] ?: "",
            startDate = row["start_date"] ?: "",
            expiryDate = row["expiry_date"] ?: "",
            notes = row["notes"] ?: ""
          )
        )
      }
      lastErrorMessage = null
    } catch (e: Exception) {
      safeLogError("Error fetching vehicle insurances", e)
      lastErrorMessage = e.message
    }
    list
  }

  suspend fun saveVehicleInsurance(phoneNumber: String, item: VehicleInsuranceEntity): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectiveUserPhone = ensureUserRowInSession(session, phoneNumber)
        if (item.id == 0L) {
          val sql = "INSERT INTO vehicle_insurances " +
            "(user_phone, plate_and_vehicle, policy_type, company, start_date, expiry_date, notes) " +
            "VALUES (" +
            "${sqlEscape(effectiveUserPhone)}, " +
            "${sqlEscape(item.plateAndVehicle)}, " +
            "${sqlEscape(item.policyType)}, " +
            "${sqlEscape(item.company)}, " +
            "${sqlEscape(item.startDate)}, " +
            "${sqlEscape(item.expiryDate)}, " +
            "${sqlEscape(item.notes)})"
          session.executeUpdate(sql)
        } else {
          val sql = "UPDATE vehicle_insurances SET " +
            "plate_and_vehicle = ${sqlEscape(item.plateAndVehicle)}, " +
            "policy_type = ${sqlEscape(item.policyType)}, " +
            "company = ${sqlEscape(item.company)}, " +
            "start_date = ${sqlEscape(item.startDate)}, " +
            "expiry_date = ${sqlEscape(item.expiryDate)}, " +
            "notes = ${sqlEscape(item.notes)} " +
            "WHERE id = ${item.id}"
          session.executeUpdate(sql)
        }
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error saving vehicle insurance", e)
      lastErrorMessage = e.message
      false
    }
  }

  suspend fun deleteVehicleInsurance(id: Long): Boolean = withContext(Dispatchers.IO) {
    try {
      client.withConnection { session ->
        session.executeUpdate("DELETE FROM vehicle_insurances WHERE id = $id")
        session.executeUpdate("DELETE FROM product_reminders WHERE module_type = 'ARAC' AND item_id = $id")
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error deleting vehicle insurance", e)
      lastErrorMessage = e.message
      false
    }
  }

  // --- 5. Bildirim Merkezi (product_reminders) ---

  suspend fun getReminders(phoneNumber: String): List<ProductReminderEntity> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ProductReminderEntity>()
    try {
      val rows = client.withConnection { session ->
        ensureTablesInSession(session)
        session.executeQuery(
          "SELECT id, user_phone, module_type, item_id, item_name, target_expiry_date, " +
            "days_before, notify_time, sound_tone, is_enabled " +
            "FROM product_reminders WHERE ${phoneInClause("user_phone", phoneNumber)} " +
            "ORDER BY target_expiry_date ASC"
        )
      }
      for (row in rows) {
        val enabledRaw = row["is_enabled"] ?: "1"
        list.add(
          ProductReminderEntity(
            id = row["id"]?.toLongOrNull() ?: 0L,
            moduleType = row["module_type"] ?: "ECZA",
            itemId = row["item_id"]?.toLongOrNull() ?: 0L,
            itemName = row["item_name"] ?: "",
            targetExpiryDate = row["target_expiry_date"] ?: "",
            daysBefore = row["days_before"]?.toIntOrNull() ?: 7,
            notifyTime = row["notify_time"] ?: "09:00",
            soundTone = row["sound_tone"] ?: "DEFAULT",
            isEnabled = enabledRaw == "1" || enabledRaw.equals("true", ignoreCase = true)
          )
        )
      }
      lastErrorMessage = null
    } catch (e: Exception) {
      safeLogError("Error fetching reminders", e)
      lastErrorMessage = e.message
    }
    list
  }

  suspend fun saveReminder(phoneNumber: String, reminder: ProductReminderEntity): Boolean = withContext(Dispatchers.IO) {
    try {
      val enabledInt = if (reminder.isEnabled) 1 else 0
      client.withConnection { session ->
        ensureTablesInSession(session)
        val effectiveUserPhone = ensureUserRowInSession(session, phoneNumber)
        val sql = "INSERT INTO product_reminders " +
          "(user_phone, module_type, item_id, item_name, target_expiry_date, days_before, notify_time, sound_tone, is_enabled) " +
          "VALUES (" +
          "${sqlEscape(effectiveUserPhone)}, " +
          "${sqlEscape(reminder.moduleType)}, " +
          "${reminder.itemId}, " +
          "${sqlEscape(reminder.itemName)}, " +
          "${sqlEscape(reminder.targetExpiryDate)}, " +
          "${reminder.daysBefore}, " +
          "${sqlEscape(reminder.notifyTime)}, " +
          "${sqlEscape(reminder.soundTone)}, " +
          "$enabledInt) " +
          "ON DUPLICATE KEY UPDATE " +
          "item_name = VALUES(item_name), " +
          "target_expiry_date = VALUES(target_expiry_date), " +
          "days_before = VALUES(days_before), " +
          "notify_time = VALUES(notify_time), " +
          "sound_tone = VALUES(sound_tone), " +
          "is_enabled = VALUES(is_enabled)"
        session.executeUpdate(sql)
      }
      lastErrorMessage = null
      true
    } catch (e: Exception) {
      safeLogError("Error saving reminder", e)
      lastErrorMessage = e.message
      false
    }
  }
}
