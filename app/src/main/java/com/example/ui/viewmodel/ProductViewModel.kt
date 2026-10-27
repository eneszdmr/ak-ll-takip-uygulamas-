package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReminderEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.data.remote.MariaDBRepository
import com.example.notifications.NotificationHelper
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import com.example.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProductViewModel(application: Application) : AndroidViewModel(application) {
  private val mariaDBRepository = MariaDBRepository()
  private val sessionManager = SessionManager(application.applicationContext)

  val currentUserPhone = MutableStateFlow<String?>(sessionManager.getSavedPhone())
  private val _rawProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
  private val _warranties = MutableStateFlow<List<WarrantyEntity>>(emptyList())
  private val _subscriptions = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
  private val _vehicleInsurances = MutableStateFlow<List<VehicleInsuranceEntity>>(emptyList())
  private val _reminders = MutableStateFlow<List<ProductReminderEntity>>(emptyList())

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

  init {
    val savedPhone = sessionManager.getSavedPhone()
    if (!savedPhone.isNullOrBlank()) {
      refreshAllData()
    }
  }

  val filteredProducts: StateFlow<List<ProductEntity>> = combine(
    _rawProducts,
    searchQuery,
    selectedCategory,
    selectedFamilyMember
  ) { products, query, category, familyMember ->
    products.filter { product ->
      val matchesQuery = query.isBlank() ||
        product.name.contains(query, ignoreCase = true) ||
        product.notes.contains(query, ignoreCase = true) ||
        product.category.contains(query, ignoreCase = true)

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
    isLoggingIn.value = true
    loginError.value = null
    viewModelScope.launch {
      val (authenticated, errMessage) = mariaDBRepository.authenticateOrRegisterUser(
        phoneNumber = phoneNumber,
        rawPassword = password
      )
      if (!authenticated) {
        isLoggingIn.value = false
        val err = errMessage ?: "Giriş yapılamadı."
        loginError.value = err
        onResult(false, err)
        return@launch
      }

      sessionManager.saveSession(phoneNumber.trim())
      currentUserPhone.value = phoneNumber.trim()
      refreshAllData()
      isLoggingIn.value = false
      onResult(true, null)
    }
  }

  fun logout() {
    sessionManager.clearSession()
    currentUserPhone.value = null
    _rawProducts.value = emptyList()
    _warranties.value = emptyList()
    _subscriptions.value = emptyList()
    _vehicleInsurances.value = emptyList()
    _reminders.value = emptyList()
    loginError.value = null
  }

  fun refreshAllData() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _rawProducts.value = mariaDBRepository.getProducts(phone)
      _warranties.value = mariaDBRepository.getWarranties(phone)
      _subscriptions.value = mariaDBRepository.getSubscriptions(phone)
      _vehicleInsurances.value = mariaDBRepository.getVehicleInsurances(phone)
      val loadedReminders = mariaDBRepository.getReminders(phone)
      _reminders.value = loadedReminders
      val ctx = getApplication<Application>().applicationContext
      loadedReminders.forEach { reminder ->
        NotificationHelper.scheduleReminderAlarm(ctx, reminder)
      }
    }
  }

  fun refreshProducts() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _rawProducts.value = mariaDBRepository.getProducts(phone)
    }
  }

  fun refreshWarranties() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _warranties.value = mariaDBRepository.getWarranties(phone)
    }
  }

  fun refreshSubscriptions() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _subscriptions.value = mariaDBRepository.getSubscriptions(phone)
    }
  }

  fun refreshVehicleInsurances() {
    val phone = currentUserPhone.value ?: return
    viewModelScope.launch {
      _vehicleInsurances.value = mariaDBRepository.getVehicleInsurances(phone)
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
    val phone = currentUserPhone.value ?: "guest"
    viewModelScope.launch {
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
