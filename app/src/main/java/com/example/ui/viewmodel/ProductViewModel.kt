package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReminderEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.UserSecuritySummary
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.data.remote.MariaDBRepository
import com.example.data.repository.RecoveryProfileRepository
import com.example.notifications.NotificationHelper
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import com.example.utils.SessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
  private val mariaDBRepository = MariaDBRepository()
  private val recoveryProfileRepository by lazy {
    RecoveryProfileRepository(application.applicationContext)
  }
  private val sessionManager = SessionManager(application.applicationContext)
  private val saveInFlight = AtomicBoolean(false)
  private var autoSyncJob: Job? = null

  val currentUserPhone = MutableStateFlow<String?>(sessionManager.getSavedPhone())
  private val _rawProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
  private val _warranties = MutableStateFlow<List<WarrantyEntity>>(emptyList())
  private val _subscriptions = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
  private val _vehicleInsurances = MutableStateFlow<List<VehicleInsuranceEntity>>(emptyList())
  private val _reminders = MutableStateFlow<List<ProductReminderEntity>>(emptyList())
  private val _userSecuritySummary = MutableStateFlow<UserSecuritySummary?>(null)

  val isInitialLoading = MutableStateFlow(false)
  val isSyncing = MutableStateFlow(false)
  val isSaving = MutableStateFlow(false)

  val loginError = MutableStateFlow<String?>(null)
  val isLoggingIn = MutableStateFlow(false)

  val searchQuery = MutableStateFlow("")
  val selectedCategory = MutableStateFlow("Tümü")
  val selectedFamilyMember = MutableStateFlow("Tümü")

  val rawProducts: StateFlow<List<ProductEntity>> = _rawProducts
  val warranties: StateFlow<List<WarrantyEntity>> = _warranties
  val subscriptions: StateFlow<List<SubscriptionEntity>> = _subscriptions
  val vehicleInsurances: StateFlow<List<VehicleInsuranceEntity>> = _vehicleInsurances
  val reminders: StateFlow<List<ProductReminderEntity>> = _reminders
  val userSecuritySummary: StateFlow<UserSecuritySummary?> = _userSecuritySummary

  init {
    val savedPhone = sessionManager.getSavedPhone()
    if (!savedPhone.isNullOrBlank()) {
      refreshAllData(showSkeleton = true)
      startAutoSyncLoop()
    }
  }

  /**
   * Automatically polls MariaDB every 4 seconds while logged in so items added/edited/deleted
   * from another phone with the same phone number appear live without logging out or restarting.
   */
  private fun startAutoSyncLoop() {
    autoSyncJob?.cancel()
    autoSyncJob = viewModelScope.launch {
      while (isActive) {
        delay(4000L)
        val phone = currentUserPhone.value
        if (!phone.isNullOrBlank() && !saveInFlight.get()) {
          syncSilently(phone)
        }
      }
    }
  }

  private suspend fun syncSilently(phone: String) {
    try {
      val latestProducts = mariaDBRepository.getProducts(phone)
      if (latestProducts != _rawProducts.value) {
        _rawProducts.value = latestProducts
      }
      val latestWarranties = mariaDBRepository.getWarranties(phone)
      if (latestWarranties != _warranties.value) {
        _warranties.value = latestWarranties
      }
      val latestSubscriptions = mariaDBRepository.getSubscriptions(phone)
      if (latestSubscriptions != _subscriptions.value) {
        _subscriptions.value = latestSubscriptions
      }
      val latestVehicles = mariaDBRepository.getVehicleInsurances(phone)
      if (latestVehicles != _vehicleInsurances.value) {
        _vehicleInsurances.value = latestVehicles
      }
      val latestReminders = mariaDBRepository.getReminders(phone)
      if (latestReminders != _reminders.value) {
        _reminders.value = latestReminders
      }
    } catch (_: Exception) {
    }
  }

  val filteredProducts: StateFlow<List<ProductEntity>> = combine(
    _rawProducts,
    searchQuery,
    selectedCategory,
    selectedFamilyMember
  ) { products, query, category, familyMember ->
    val trimmedQuery = query.trim()
    products.filter { product ->
      val matchesQuery = trimmedQuery.isBlank() ||
        product.name.contains(trimmedQuery, ignoreCase = true) ||
        product.notes.contains(trimmedQuery, ignoreCase = true) ||
        product.category.contains(trimmedQuery, ignoreCase = true) ||
        product.storageLocation.contains(trimmedQuery, ignoreCase = true) ||
        product.familyMember.contains(trimmedQuery, ignoreCase = true)

      val matchesCategory = category == "Tümü" || product.category.equals(category, ignoreCase = true)
      val matchesFamily = familyMember == "Tümü" || product.familyMember.equals(familyMember, ignoreCase = true)

      matchesQuery && matchesCategory && matchesFamily
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val statistics = combine(_rawProducts) { (products) ->
    val total = products.size
    var critical = 0
    var warning = 0
    var safe = 0

    val today = LocalDate.now()
    for (p in products) {
      val effective = ExpiryCalculator.calculateEffectiveExpiryDate(
        p.expiryDate,
        p.openedDate,
        p.paoMonths,
        today
      )
      val days = ExpiryCalculator.getDaysLeft(effective, today)
      when (ExpiryCalculator.getStatus(days)) {
        ExpiryStatus.CRITICAL -> critical++
        ExpiryStatus.WARNING -> warning++
        ExpiryStatus.SAFE -> safe++
      }
    }
    ProductStats(total, critical, warning, safe)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductStats(0, 0, 0, 0))

  fun loginWithPhoneAndPassword(
    phoneNumber: String,
    password: String,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    if (isLoggingIn.value) return
    isLoggingIn.value = true
    loginError.value = null
    val normalizedPhone = SessionManager.normalizePhone(phoneNumber)
    viewModelScope.launch {
      val (authenticated, errMessage) = mariaDBRepository.authenticateOrRegisterUser(
        phoneNumber = normalizedPhone,
        rawPassword = password
      )
      if (!authenticated) {
        isLoggingIn.value = false
        val err = errMessage ?: "Giriş yapılamadı."
        loginError.value = err
        onResult(false, err)
        return@launch
      }

      sessionManager.saveSession(normalizedPhone)
      currentUserPhone.value = normalizedPhone
      refreshAllData(showSkeleton = true)
      startAutoSyncLoop()
      isLoggingIn.value = false
      onResult(true, null)
    }
  }

  fun logout() {
    autoSyncJob?.cancel()
    sessionManager.clearSession()
    try {
      Firebase.auth.signOut()
    } catch (_: Exception) {
    }
    currentUserPhone.value = null
    _rawProducts.value = emptyList()
    _warranties.value = emptyList()
    _subscriptions.value = emptyList()
    _vehicleInsurances.value = emptyList()
    _reminders.value = emptyList()
    _userSecuritySummary.value = null
    loginError.value = null
  }

  suspend fun loadSecuritySummaryForPhone(phone: String): UserSecuritySummary? {
    return mariaDBRepository.getUserSecuritySummary(phone)
  }

  suspend fun resetPasswordWithFirebase(
    phone: String,
    verifiedEmail: String,
    firebaseUid: String,
    newPassword: String
  ): Pair<Boolean, String?> {
    val firestoreLinkedPhone = try {
      recoveryProfileRepository.getMyProfile().getOrNull()?.phoneNumber
    } catch (_: Exception) {
      null
    }
    val (success, msg) = mariaDBRepository.resetPasswordByVerifiedFirebaseAccount(
      phoneNumber = phone,
      verifiedEmail = verifiedEmail,
      firebaseUid = firebaseUid,
      newRawPassword = newPassword,
      firestoreLinkedPhone = firestoreLinkedPhone
    )
    if (success) {
      try {
        val summary = mariaDBRepository.getUserSecuritySummary(phone)
        recoveryProfileRepository.saveOrUpdateProfile(
          phoneNumber = phone,
          email = verifiedEmail,
          securityQuestion = summary?.securityQuestion ?: "",
          recoveryCodeHint = summary?.recoveryCodeHint ?: ""
        )
      } catch (_: Exception) {
      }
    }
    return success to msg
  }

  suspend fun resetPasswordWithSecurityOrCode(
    phone: String,
    securityAnswer: String?,
    recoveryCode: String?,
    newPassword: String,
    selectedQuestionIfNew: String?
  ): Pair<Boolean, String?> {
    return mariaDBRepository.resetPasswordBySecurityAnswerOrCode(
      phoneNumber = phone,
      securityAnswerRaw = securityAnswer,
      recoveryCodeRaw = recoveryCode,
      newRawPassword = newPassword,
      newSecurityQuestion = selectedQuestionIfNew
    )
  }

  suspend fun saveUserRecoverySettings(
    recoveryEmail: String?,
    firebaseUid: String?,
    securityQuestion: String?,
    securityAnswerRaw: String?,
    rawRecoveryCode: String?
  ): Pair<Boolean, String?> {
    val phone = currentUserPhone.value ?: return false to "Oturum açık değil."
    val (success, msg) = mariaDBRepository.saveUserRecoveryConfig(
      phoneNumber = phone,
      recoveryEmail = recoveryEmail,
      firebaseUid = firebaseUid,
      securityQuestion = securityQuestion,
      securityAnswerRaw = securityAnswerRaw,
      rawRecoveryCode = rawRecoveryCode
    )
    if (success) {
      val updatedSummary = mariaDBRepository.getUserSecuritySummary(phone)
      _userSecuritySummary.value = updatedSummary
      val activeUser = try {
        Firebase.auth.currentUser
      } catch (_: Exception) {
        null
      }
      if (activeUser != null && !recoveryEmail.isNullOrBlank()) {
        try {
          recoveryProfileRepository.saveOrUpdateProfile(
            phoneNumber = phone,
            email = recoveryEmail,
            securityQuestion = updatedSummary?.securityQuestion ?: (securityQuestion ?: ""),
            recoveryCodeHint = updatedSummary?.recoveryCodeHint ?: ""
          )
        } catch (_: Exception) {
        }
      }
    }
    return success to msg
  }

  fun refreshAllData(showSkeleton: Boolean = false) {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      if (showSkeleton && _rawProducts.value.isEmpty() && _warranties.value.isEmpty()) {
        isInitialLoading.value = true
      }
      isSyncing.value = true
      _rawProducts.value = mariaDBRepository.getProducts(phone)
      _warranties.value = mariaDBRepository.getWarranties(phone)
      _subscriptions.value = mariaDBRepository.getSubscriptions(phone)
      _vehicleInsurances.value = mariaDBRepository.getVehicleInsurances(phone)
      _userSecuritySummary.value = mariaDBRepository.getUserSecuritySummary(phone)
      val loadedReminders = mariaDBRepository.getReminders(phone)
      _reminders.value = loadedReminders
      val ctx = getApplication<Application>().applicationContext
      loadedReminders.forEach { reminder ->
        NotificationHelper.scheduleReminderAlarm(ctx, reminder)
      }
      isInitialLoading.value = false
      isSyncing.value = false
    }
  }

  fun refreshProducts() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      isSyncing.value = true
      _rawProducts.value = mariaDBRepository.getProducts(phone)
      isSyncing.value = false
    }
  }

  fun refreshWarranties() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      isSyncing.value = true
      _warranties.value = mariaDBRepository.getWarranties(phone)
      isSyncing.value = false
    }
  }

  fun refreshSubscriptions() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      isSyncing.value = true
      _subscriptions.value = mariaDBRepository.getSubscriptions(phone)
      isSyncing.value = false
    }
  }

  fun refreshVehicleInsurances() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      isSyncing.value = true
      _vehicleInsurances.value = mariaDBRepository.getVehicleInsurances(phone)
      isSyncing.value = false
    }
  }

  fun refreshReminders() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _reminders.value = mariaDBRepository.getReminders(phone)
    }
  }

  fun saveReminder(reminder: ProductReminderEntity) {
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      val success = mariaDBRepository.saveReminder(phone, reminder)
      if (success) {
        NotificationHelper.scheduleReminderAlarm(
          getApplication<Application>().applicationContext,
          reminder
        )
        refreshReminders()
      }
    }
  }

  private suspend fun saveInlineReminderForMatchedItem(
    phone: String,
    moduleType: String,
    itemId: Long,
    itemName: String,
    targetExpiryDate: String,
    enabled: Boolean,
    daysBefore: Int,
    notifyTime: String,
    soundTone: String
  ) {
    val reminder = ProductReminderEntity(
      moduleType = moduleType,
      itemId = itemId,
      itemName = itemName,
      targetExpiryDate = targetExpiryDate,
      daysBefore = daysBefore,
      notifyTime = notifyTime,
      soundTone = soundTone,
      isEnabled = enabled
    )
    mariaDBRepository.saveReminder(phone, reminder)
    NotificationHelper.scheduleReminderAlarm(
      getApplication<Application>().applicationContext,
      reminder
    )
    _reminders.value = mariaDBRepository.getReminders(phone)
  }

  // --- Ecza & Kozmetik Actions ---

  fun addProduct(
    name: String,
    category: String,
    storageLocation: String,
    familyMember: String,
    expiryDate: String,
    openedDate: String?,
    paoMonths: Int?,
    notes: String,
    isOpened: Boolean,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 7,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val newProduct = ProductEntity(
          name = name,
          category = category,
          storageLocation = storageLocation,
          familyMember = familyMember,
          expiryDate = expiryDate,
          openedDate = if (isOpened) openedDate ?: LocalDate.now().toString() else null,
          paoMonths = if (isOpened) paoMonths else null,
          notes = notes,
          isOpened = isOpened
        )
        val success = mariaDBRepository.saveProduct(phone, newProduct)
        if (success) {
          val updatedList = mariaDBRepository.getProducts(phone)
          _rawProducts.value = updatedList
          val created = updatedList.firstOrNull { it.name == name && it.expiryDate == expiryDate }
          if (created != null) {
            saveInlineReminderForMatchedItem(
              phone = phone,
              moduleType = "ECZA",
              itemId = created.id,
              itemName = created.name,
              targetExpiryDate = created.expiryDate,
              enabled = reminderEnabled,
              daysBefore = reminderDaysBefore,
              notifyTime = reminderTime,
              soundTone = reminderSound
            )
          }
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun updateProduct(
    product: ProductEntity,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 7,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val success = mariaDBRepository.saveProduct(phone, product)
        if (success) {
          _rawProducts.value = mariaDBRepository.getProducts(phone)
          saveInlineReminderForMatchedItem(
            phone = phone,
            moduleType = "ECZA",
            itemId = product.id,
            itemName = product.name,
            targetExpiryDate = product.expiryDate,
            enabled = reminderEnabled,
            daysBefore = reminderDaysBefore,
            notifyTime = reminderTime,
            soundTone = reminderSound
          )
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun deleteProduct(product: ProductEntity) {
    viewModelScope.launch {
      val success = mariaDBRepository.deleteProduct(product.id)
      if (success) {
        refreshProducts()
      }
    }
  }

  fun markAsOpenedToday(product: ProductEntity, defaultPaoMonths: Int = 6) {
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      val todayStr = LocalDate.now().toString()
      val updated = product.copy(
        isOpened = true,
        openedDate = todayStr,
        paoMonths = product.paoMonths ?: defaultPaoMonths
      )
      val success = mariaDBRepository.saveProduct(phone, updated)
      if (success) {
        refreshProducts()
      }
    }
  }

  // --- Warranty Tracking (Garanti Takip) Actions ---

  fun addWarranty(
    name: String,
    brandModel: String,
    category: String,
    purchaseDate: String,
    warrantyMonths: Int,
    warrantyEndDate: String,
    storeInvoice: String,
    notes: String,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 14,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val warranty = WarrantyEntity(
          name = name,
          brandModel = brandModel,
          category = category,
          purchaseDate = purchaseDate,
          warrantyMonths = warrantyMonths,
          warrantyEndDate = warrantyEndDate,
          storeInvoice = storeInvoice,
          notes = notes
        )
        val success = mariaDBRepository.saveWarranty(phone, warranty)
        if (success) {
          val list = mariaDBRepository.getWarranties(phone)
          _warranties.value = list
          val created = list.firstOrNull { it.name == name && it.warrantyEndDate == warrantyEndDate }
          if (created != null) {
            saveInlineReminderForMatchedItem(
              phone = phone,
              moduleType = "GARANTI",
              itemId = created.id,
              itemName = created.name,
              targetExpiryDate = created.warrantyEndDate,
              enabled = reminderEnabled,
              daysBefore = reminderDaysBefore,
              notifyTime = reminderTime,
              soundTone = reminderSound
            )
          }
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun updateWarranty(
    warranty: WarrantyEntity,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 14,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val success = mariaDBRepository.saveWarranty(phone, warranty)
        if (success) {
          _warranties.value = mariaDBRepository.getWarranties(phone)
          saveInlineReminderForMatchedItem(
            phone = phone,
            moduleType = "GARANTI",
            itemId = warranty.id,
            itemName = warranty.name,
            targetExpiryDate = warranty.warrantyEndDate,
            enabled = reminderEnabled,
            daysBefore = reminderDaysBefore,
            notifyTime = reminderTime,
            soundTone = reminderSound
          )
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun deleteWarranty(warranty: WarrantyEntity) {
    viewModelScope.launch {
      val success = mariaDBRepository.deleteWarranty(warranty.id)
      if (success) {
        refreshWarranties()
      }
    }
  }

  // --- Subscription Tracking (Abonelik Takip) Actions ---

  fun addSubscription(
    name: String,
    category: String,
    billingCycle: String,
    price: String,
    nextPaymentDate: String,
    autoRenew: Boolean,
    notes: String,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 7,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val sub = SubscriptionEntity(
          name = name,
          category = category,
          billingCycle = billingCycle,
          price = price,
          nextPaymentDate = nextPaymentDate,
          autoRenew = autoRenew,
          notes = notes
        )
        val success = mariaDBRepository.saveSubscription(phone, sub)
        if (success) {
          val list = mariaDBRepository.getSubscriptions(phone)
          _subscriptions.value = list
          val created = list.firstOrNull { it.name == name && it.nextPaymentDate == nextPaymentDate }
          if (created != null) {
            saveInlineReminderForMatchedItem(
              phone = phone,
              moduleType = "ABONELIK",
              itemId = created.id,
              itemName = created.name,
              targetExpiryDate = created.nextPaymentDate,
              enabled = reminderEnabled,
              daysBefore = reminderDaysBefore,
              notifyTime = reminderTime,
              soundTone = reminderSound
            )
          }
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun updateSubscription(
    subscription: SubscriptionEntity,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 7,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val success = mariaDBRepository.saveSubscription(phone, subscription)
        if (success) {
          _subscriptions.value = mariaDBRepository.getSubscriptions(phone)
          saveInlineReminderForMatchedItem(
            phone = phone,
            moduleType = "ABONELIK",
            itemId = subscription.id,
            itemName = subscription.name,
            targetExpiryDate = subscription.nextPaymentDate,
            enabled = reminderEnabled,
            daysBefore = reminderDaysBefore,
            notifyTime = reminderTime,
            soundTone = reminderSound
          )
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun deleteSubscription(subscription: SubscriptionEntity) {
    viewModelScope.launch {
      val success = mariaDBRepository.deleteSubscription(subscription.id)
      if (success) {
        refreshSubscriptions()
      }
    }
  }

  // --- Vehicle & Insurance Tracking (Araç & Sigorta Takip) Actions ---

  fun addVehicleInsurance(
    plateAndVehicle: String,
    policyType: String,
    company: String,
    startDate: String,
    expiryDate: String,
    notes: String,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 14,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val item = VehicleInsuranceEntity(
          plateAndVehicle = plateAndVehicle,
          policyType = policyType,
          company = company,
          startDate = startDate,
          expiryDate = expiryDate,
          notes = notes
        )
        val success = mariaDBRepository.saveVehicleInsurance(phone, item)
        if (success) {
          val list = mariaDBRepository.getVehicleInsurances(phone)
          _vehicleInsurances.value = list
          val created = list.firstOrNull {
            it.plateAndVehicle == plateAndVehicle && it.expiryDate == expiryDate
          }
          if (created != null) {
            saveInlineReminderForMatchedItem(
              phone = phone,
              moduleType = "ARAC",
              itemId = created.id,
              itemName = created.plateAndVehicle,
              targetExpiryDate = created.expiryDate,
              enabled = reminderEnabled,
              daysBefore = reminderDaysBefore,
              notifyTime = reminderTime,
              soundTone = reminderSound
            )
          }
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun updateVehicleInsurance(
    item: VehicleInsuranceEntity,
    reminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 14,
    reminderTime: String = "09:00",
    reminderSound: String = "DEFAULT",
    onComplete: (Boolean) -> Unit
  ) {
    if (!saveInFlight.compareAndSet(false, true)) return
    isSaving.value = true
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
      try {
        val success = mariaDBRepository.saveVehicleInsurance(phone, item)
        if (success) {
          _vehicleInsurances.value = mariaDBRepository.getVehicleInsurances(phone)
          saveInlineReminderForMatchedItem(
            phone = phone,
            moduleType = "ARAC",
            itemId = item.id,
            itemName = item.plateAndVehicle,
            targetExpiryDate = item.expiryDate,
            enabled = reminderEnabled,
            daysBefore = reminderDaysBefore,
            notifyTime = reminderTime,
            soundTone = reminderSound
          )
        }
        onComplete(success)
      } finally {
        isSaving.value = false
        saveInFlight.set(false)
      }
    }
  }

  fun deleteVehicleInsurance(item: VehicleInsuranceEntity) {
    viewModelScope.launch {
      val success = mariaDBRepository.deleteVehicleInsurance(item.id)
      if (success) {
        refreshVehicleInsurances()
      }
    }
  }
}

data class ProductStats(
  val total: Int,
  val critical: Int,
  val warning: Int,
  val safe: Int
)
