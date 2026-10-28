package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ProductEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ProductViewModel
import com.example.utils.ExpiryCalculator
import java.time.LocalDate

sealed class Screen(val depth: Int) {
  object MainHub : Screen(0)
  object Notifications : Screen(1)
  object EczaDashboard : Screen(1)
  data class AddEditProduct(val product: ProductEntity? = null) : Screen(2)
  object WarrantyDashboard : Screen(1)
  data class AddEditWarranty(val warranty: WarrantyEntity? = null) : Screen(2)
  object SubscriptionDashboard : Screen(1)
  data class AddEditSubscription(val subscription: SubscriptionEntity? = null) : Screen(2)
  object VehicleDashboard : Screen(1)
  data class AddEditVehicle(val vehicle: VehicleInsuranceEntity? = null) : Screen(2)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ProductViewModel = viewModel()
        val currentUserPhone by viewModel.currentUserPhone.collectAsState()
        val isLoggingIn by viewModel.isLoggingIn.collectAsState()
        val loginError by viewModel.loginError.collectAsState()
        val isInitialLoading by viewModel.isInitialLoading.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()

        var currentScreen by remember { mutableStateOf<Screen>(Screen.MainHub) }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, currentUserPhone) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !currentUserPhone.isNullOrBlank()) {
              viewModel.refreshAllData()
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
          }
        }

        LaunchedEffect(currentScreen, currentUserPhone) {
          if (!currentUserPhone.isNullOrBlank()) {
            when (currentScreen) {
              is Screen.MainHub -> viewModel.refreshAllData()
              is Screen.EczaDashboard -> viewModel.refreshProducts()
              is Screen.WarrantyDashboard -> viewModel.refreshWarranties()
              is Screen.SubscriptionDashboard -> viewModel.refreshSubscriptions()
              is Screen.VehicleDashboard -> viewModel.refreshVehicleInsurances()
              is Screen.Notifications -> viewModel.refreshReminders()
              else -> {}
            }
          }
        }

        if (currentUserPhone == null) {
          LoginScreen(
            isLoading = isLoggingIn,
            externalError = loginError,
            onLoginClicked = { phone, password ->
              viewModel.loginWithPhoneAndPassword(phone, password) { success, _ ->
                if (success) {
                  currentScreen = Screen.MainHub
                }
              }
            },
            onLoadSecuritySummary = { phone -> viewModel.loadSecuritySummaryForPhone(phone) },
            onResetWithFirebase = { phone, verifiedEmail, firebaseUid, newPassword ->
              viewModel.resetPasswordWithFirebase(phone, verifiedEmail, firebaseUid, newPassword)
            },
            onResetWithSecurityOrCode = { phone, securityAnswer, recoveryCode, newPassword, selectedQuestionIfNew ->
              viewModel.resetPasswordWithSecurityOrCode(
                phone,
                securityAnswer,
                recoveryCode,
                newPassword,
                selectedQuestionIfNew
              )
            }
          )
        } else {
          val rawProducts by viewModel.rawProducts.collectAsState()
          val products by viewModel.filteredProducts.collectAsState()
          val stats by viewModel.statistics.collectAsState()
          val warranties by viewModel.warranties.collectAsState()
          val subscriptions by viewModel.subscriptions.collectAsState()
          val vehicles by viewModel.vehicleInsurances.collectAsState()
          val reminders by viewModel.reminders.collectAsState()
          val userSecuritySummary by viewModel.userSecuritySummary.collectAsState()

          val searchQuery by viewModel.searchQuery.collectAsState()
          val selectedCategory by viewModel.selectedCategory.collectAsState()
          val selectedFamilyMember by viewModel.selectedFamilyMember.collectAsState()

          val expiringWarrantiesCount = remember(warranties) {
            val today = LocalDate.now()
            warranties.count { w ->
              try {
                ExpiryCalculator.getDaysLeft(w.warrantyEndDate, today) <= 30
              } catch (_: Exception) {
                false
              }
            }
          }

          val totalMonthlySubscriptionCost = remember(subscriptions) {
            subscriptions.sumOf { s ->
              val numericPrice = s.price
                .replace(",", ".")
                .filter { it.isDigit() || it == '.' }
                .toDoubleOrNull() ?: 0.0
              when (s.billingCycle.lowercase()) {
                "yıllık" -> numericPrice / 12.0
                "haftalık" -> numericPrice * 4.0
                else -> numericPrice
              }
            }
          }

          val expiringVehiclesCount = remember(vehicles) {
            val today = LocalDate.now()
            vehicles.count { v ->
              try {
                ExpiryCalculator.getDaysLeft(v.expiryDate, today) <= 30
              } catch (_: Exception) {
                false
              }
            }
          }

          val activeRemindersCount = remember(reminders) {
            reminders.count { it.isEnabled }
          }

          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
              val isForward = targetState.depth >= initialState.depth
              val slideIn = slideInHorizontally(
                animationSpec = tween(durationMillis = 260),
                initialOffsetX = { fullWidth -> if (isForward) fullWidth / 4 else -fullWidth / 4 }
              ) + fadeIn(animationSpec = tween(durationMillis = 220))

              val slideOut = slideOutHorizontally(
                animationSpec = tween(durationMillis = 220),
                targetOffsetX = { fullWidth -> if (isForward) -fullWidth / 5 else fullWidth / 5 }
              ) + fadeOut(animationSpec = tween(durationMillis = 180))

              slideIn.togetherWith(slideOut)
            },
            label = "screen_transition"
          ) { screen ->
            when (screen) {
              is Screen.MainHub -> {
                MainHubScreen(
                  userPhone = currentUserPhone ?: "",
                  productStats = stats,
                  totalWarranties = warranties.size,
                  expiringWarranties = expiringWarrantiesCount,
                  totalSubscriptions = subscriptions.size,
                  totalMonthlyCost = totalMonthlySubscriptionCost,
                  totalVehicles = vehicles.size,
                  expiringVehicles = expiringVehiclesCount,
                  activeRemindersCount = activeRemindersCount,
                  userSecuritySummary = userSecuritySummary,
                  isInitialLoading = isInitialLoading,
                  isSyncing = isSyncing,
                  onRefresh = { viewModel.refreshAllData() },
                  onOpenEczaTakip = { currentScreen = Screen.EczaDashboard },
                  onOpenGarantiTakip = { currentScreen = Screen.WarrantyDashboard },
                  onOpenAbonelikTakip = { currentScreen = Screen.SubscriptionDashboard },
                  onOpenAracTakip = { currentScreen = Screen.VehicleDashboard },
                  onOpenNotifications = { currentScreen = Screen.Notifications },
                  onSaveRecoverySettings = { email, uid, question, answer, code ->
                    viewModel.saveUserRecoverySettings(email, uid, question, answer, code)
                  },
                  onLogout = {
                    viewModel.logout()
                    currentScreen = Screen.MainHub
                  }
                )
              }

              is Screen.Notifications -> {
                BackHandler {
                  currentScreen = Screen.MainHub
                }
                NotificationsScreen(
                  products = rawProducts,
                  warranties = warranties,
                  subscriptions = subscriptions,
                  vehicles = vehicles,
                  reminders = reminders,
                  onSaveReminder = { reminder -> viewModel.saveReminder(reminder) },
                  onBackToHub = { currentScreen = Screen.MainHub }
                )
              }

              is Screen.EczaDashboard -> {
                BackHandler {
                  currentScreen = Screen.MainHub
                }
                DashboardScreen(
                  products = products,
                  stats = stats,
                  searchQuery = searchQuery,
                  onSearchQueryChanged = { viewModel.searchQuery.value = it },
                  selectedCategory = selectedCategory,
                  onCategorySelected = { viewModel.selectedCategory.value = it },
                  selectedFamilyMember = selectedFamilyMember,
                  onFamilyMemberSelected = { viewModel.selectedFamilyMember.value = it },
                  onOpenToday = { product -> viewModel.markAsOpenedToday(product) },
                  onEdit = { product -> currentScreen = Screen.AddEditProduct(product) },
                  onDelete = { product -> viewModel.deleteProduct(product) },
                  onAddProductClicked = { currentScreen = Screen.AddEditProduct(null) },
                  isInitialLoading = isInitialLoading,
                  isSyncing = isSyncing,
                  onRefresh = { viewModel.refreshProducts() },
                  onBackToHub = { currentScreen = Screen.MainHub },
                  onLogout = {
                    viewModel.logout()
                    currentScreen = Screen.MainHub
                  }
                )
              }

              is Screen.AddEditProduct -> {
                BackHandler {
                  currentScreen = Screen.EczaDashboard
                }
                var remEnabled by remember { mutableStateOf(true) }
                var remDays by remember { mutableIntStateOf(7) }
                var remTime by remember { mutableStateOf("09:00") }
                var remSound by remember { mutableStateOf("DEFAULT") }

                AddEditProductScreen(
                  productToEdit = screen.product,
                  onSaveReminderConfig = { en, d, t, s ->
                    remEnabled = en
                    remDays = d
                    remTime = t
                    remSound = s
                  },
                  onSave = { name, category, storageLocation, familyMember, expiryDate, openedDate, paoMonths, notes, isOpened ->
                    if (screen.product != null) {
                      viewModel.updateProduct(
                        product = screen.product.copy(
                          name = name,
                          category = category,
                          storageLocation = storageLocation,
                          familyMember = familyMember,
                          expiryDate = expiryDate,
                          openedDate = openedDate,
                          paoMonths = paoMonths,
                          notes = notes,
                          isOpened = isOpened
                        ),
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.EczaDashboard
                      }
                    } else {
                      viewModel.addProduct(
                        name = name,
                        category = category,
                        storageLocation = storageLocation,
                        familyMember = familyMember,
                        expiryDate = expiryDate,
                        openedDate = openedDate,
                        paoMonths = paoMonths,
                        notes = notes,
                        isOpened = isOpened,
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.EczaDashboard
                      }
                    }
                  },
                  onBack = {
                    currentScreen = Screen.EczaDashboard
                  }
                )
              }

              is Screen.WarrantyDashboard -> {
                BackHandler {
                  currentScreen = Screen.MainHub
                }
                WarrantyDashboardScreen(
                  warranties = warranties,
                  onAddWarrantyClicked = { currentScreen = Screen.AddEditWarranty(null) },
                  onEditWarranty = { w -> currentScreen = Screen.AddEditWarranty(w) },
                  onDeleteWarranty = { w -> viewModel.deleteWarranty(w) },
                  isInitialLoading = isInitialLoading,
                  isSyncing = isSyncing,
                  onRefresh = { viewModel.refreshWarranties() },
                  onBackToHub = { currentScreen = Screen.MainHub }
                )
              }

              is Screen.AddEditWarranty -> {
                BackHandler {
                  currentScreen = Screen.WarrantyDashboard
                }
                var remEnabled by remember { mutableStateOf(true) }
                var remDays by remember { mutableIntStateOf(14) }
                var remTime by remember { mutableStateOf("09:00") }
                var remSound by remember { mutableStateOf("DEFAULT") }

                AddEditWarrantyScreen(
                  warrantyToEdit = screen.warranty,
                  onSaveReminderConfig = { en, d, t, s ->
                    remEnabled = en
                    remDays = d
                    remTime = t
                    remSound = s
                  },
                  onSave = { name, brandModel, category, purchaseDate, warrantyMonths, warrantyEndDate, storeInvoice, notes ->
                    if (screen.warranty != null) {
                      viewModel.updateWarranty(
                        warranty = screen.warranty.copy(
                          name = name,
                          brandModel = brandModel,
                          category = category,
                          purchaseDate = purchaseDate,
                          warrantyMonths = warrantyMonths,
                          warrantyEndDate = warrantyEndDate,
                          storeInvoice = storeInvoice,
                          notes = notes
                        ),
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.WarrantyDashboard
                      }
                    } else {
                      viewModel.addWarranty(
                        name = name,
                        brandModel = brandModel,
                        category = category,
                        purchaseDate = purchaseDate,
                        warrantyMonths = warrantyMonths,
                        warrantyEndDate = warrantyEndDate,
                        storeInvoice = storeInvoice,
                        notes = notes,
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.WarrantyDashboard
                      }
                    }
                  },
                  onBack = {
                    currentScreen = Screen.WarrantyDashboard
                  }
                )
              }

              is Screen.SubscriptionDashboard -> {
                BackHandler {
                  currentScreen = Screen.MainHub
                }
                SubscriptionDashboardScreen(
                  subscriptions = subscriptions,
                  onAddSubscriptionClicked = { currentScreen = Screen.AddEditSubscription(null) },
                  onEditSubscription = { s -> currentScreen = Screen.AddEditSubscription(s) },
                  onDeleteSubscription = { s -> viewModel.deleteSubscription(s) },
                  isInitialLoading = isInitialLoading,
                  isSyncing = isSyncing,
                  onRefresh = { viewModel.refreshSubscriptions() },
                  onBackToHub = { currentScreen = Screen.MainHub }
                )
              }

              is Screen.AddEditSubscription -> {
                BackHandler {
                  currentScreen = Screen.SubscriptionDashboard
                }
                AddEditSubscriptionScreen(
                  subscriptionToEdit = screen.subscription,
                  onSave = { name, category, billingCycle, price, nextPaymentDate, autoRenew, notes, remEnabled, remDays, remTime, remSound ->
                    if (screen.subscription != null) {
                      viewModel.updateSubscription(
                        subscription = screen.subscription.copy(
                          name = name,
                          category = category,
                          billingCycle = billingCycle,
                          price = price,
                          nextPaymentDate = nextPaymentDate,
                          autoRenew = autoRenew,
                          notes = notes
                        ),
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.SubscriptionDashboard
                      }
                    } else {
                      viewModel.addSubscription(
                        name = name,
                        category = category,
                        billingCycle = billingCycle,
                        price = price,
                        nextPaymentDate = nextPaymentDate,
                        autoRenew = autoRenew,
                        notes = notes,
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.SubscriptionDashboard
                      }
                    }
                  },
                  onBack = {
                    currentScreen = Screen.SubscriptionDashboard
                  }
                )
              }

              is Screen.VehicleDashboard -> {
                BackHandler {
                  currentScreen = Screen.MainHub
                }
                VehicleInsuranceDashboardScreen(
                  vehicles = vehicles,
                  onAddVehicleClicked = { currentScreen = Screen.AddEditVehicle(null) },
                  onEditVehicle = { v -> currentScreen = Screen.AddEditVehicle(v) },
                  onDeleteVehicle = { v -> viewModel.deleteVehicleInsurance(v) },
                  isInitialLoading = isInitialLoading,
                  isSyncing = isSyncing,
                  onRefresh = { viewModel.refreshVehicleInsurances() },
                  onBackToHub = { currentScreen = Screen.MainHub }
                )
              }

              is Screen.AddEditVehicle -> {
                BackHandler {
                  currentScreen = Screen.VehicleDashboard
                }
                AddEditVehicleInsuranceScreen(
                  vehicleToEdit = screen.vehicle,
                  onSave = { plateAndVehicle, policyType, company, startDate, expiryDate, notes, remEnabled, remDays, remTime, remSound ->
                    if (screen.vehicle != null) {
                      viewModel.updateVehicleInsurance(
                        item = screen.vehicle.copy(
                          plateAndVehicle = plateAndVehicle,
                          policyType = policyType,
                          company = company,
                          startDate = startDate,
                          expiryDate = expiryDate,
                          notes = notes
                        ),
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.VehicleDashboard
                      }
                    } else {
                      viewModel.addVehicleInsurance(
                        plateAndVehicle = plateAndVehicle,
                        policyType = policyType,
                        company = company,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        notes = notes,
                        reminderEnabled = remEnabled,
                        reminderDaysBefore = remDays,
                        reminderTime = remTime,
                        reminderSound = remSound
                      ) { success ->
                        if (success) currentScreen = Screen.VehicleDashboard
                      }
                    }
                  },
                  onBack = {
                    currentScreen = Screen.VehicleDashboard
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}
