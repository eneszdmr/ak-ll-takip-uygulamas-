package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReminderEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.VehicleInsuranceEntity
import com.example.data.model.WarrantyEntity
import com.example.notifications.NotificationHelper

data class UnifiedTrackableItem(
  val moduleType: String, // "ECZA", "GARANTI", "ABONELIK", "ARAC"
  val moduleLabel: String,
  val itemId: Long,
  val title: String,
  val subtitle: String,
  val targetExpiryDate: String,
  val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  products: List<ProductEntity>,
  warranties: List<WarrantyEntity>,
  subscriptions: List<SubscriptionEntity>,
  vehicles: List<VehicleInsuranceEntity>,
  reminders: List<ProductReminderEntity>,
  onSaveReminder: (ProductReminderEntity) -> Unit,
  onBackToHub: () -> Unit
) {
  val context = LocalContext.current
  var selectedModuleFilter by remember { mutableStateOf("Tümü") }
  var defaultNotifyTime by remember { mutableStateOf("09:00") }
  var defaultSoundTone by remember { mutableStateOf("DEFAULT") }
  var showGlobalTimePicker by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ -> }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      NotificationHelper.stopPreviewSound()
    }
  }

  val allItems = remember(products, warranties, subscriptions, vehicles) {
    buildList {
      products.forEach { p ->
        add(
          UnifiedTrackableItem(
            moduleType = "ECZA",
            moduleLabel = "Ecza & Kozmetik",
            itemId = p.id,
            title = p.name,
            subtitle = "${p.category} • ${p.storageLocation}",
            targetExpiryDate = p.expiryDate,
            accentColor = Color(0xFF00C7BE)
          )
        )
      }
      warranties.forEach { w ->
        add(
          UnifiedTrackableItem(
            moduleType = "GARANTI",
            moduleLabel = "Garanti Takip",
            itemId = w.id,
            title = w.name,
            subtitle = "${w.brandModel} • ${w.category}",
            targetExpiryDate = w.warrantyEndDate,
            accentColor = Color(0xFF007AFF)
          )
        )
      }
      subscriptions.forEach { s ->
        add(
          UnifiedTrackableItem(
            moduleType = "ABONELIK",
            moduleLabel = "Abonelik Takip",
            itemId = s.id,
            title = s.name,
            subtitle = "${s.category} • ${s.billingCycle}",
            targetExpiryDate = s.nextPaymentDate,
            accentColor = Color(0xFF5856D6)
          )
        )
      }
      vehicles.forEach { v ->
        add(
          UnifiedTrackableItem(
            moduleType = "ARAC",
            moduleLabel = "Araç & Sigorta",
            itemId = v.id,
            title = v.plateAndVehicle,
            subtitle = "${v.policyType} • ${v.company}",
            targetExpiryDate = v.expiryDate,
            accentColor = Color(0xFFFF9500)
          )
        )
      }
    }
  }

  val filteredItems = remember(allItems, selectedModuleFilter) {
    if (selectedModuleFilter == "Tümü") {
      allItems
    } else {
      allItems.filter { it.moduleLabel == selectedModuleFilter }
    }
  }

  val moduleFilters = listOf("Tümü", "Ecza & Kozmetik", "Garanti Takip", "Abonelik Takip", "Araç & Sigorta")

  if (showGlobalTimePicker) {
    AppleStyleTimePickerDialog(
      initialTime = defaultNotifyTime,
      onTimeSelected = { newTime ->
        defaultNotifyTime = newTime
        showGlobalTimePicker = false
      },
      onDismiss = { showGlobalTimePicker = false }
    )
  }

  Scaffold(
    containerColor = Color(0xFFF2F2F7),
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFF2D55).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Bildirim & Alarm Merkezi", fontWeight = FontWeight.Bold, fontSize = 19.sp)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackToHub) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana Menü")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 84.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Global Sound & Time Quick Studio Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Bildirim Müziği & Varsayılan Saat",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color(0xFF1C1C1E)
                )
                Text(
                  text = "Müzik seçip dinleyebilir ve her ürüne özel alarm kurabilirsiniz",
                  fontSize = 12.sp,
                  color = Color(0xFF8E8E93)
                )
              }
              FilledTonalButton(
                onClick = { showGlobalTimePicker = true },
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(defaultNotifyTime, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(NotificationHelper.soundOptions) { sound ->
                FilterChip(
                  selected = defaultSoundTone == sound.key,
                  onClick = {
                    defaultSoundTone = sound.key
                    NotificationHelper.previewSound(context, sound.key)
                  },
                  label = { Text(sound.label, fontSize = 12.sp) },
                  leadingIcon = {
                    Icon(
                      Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                )
              }
            }
          }
        }
      }

      // Module Filter Chips
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(moduleFilters) { filter ->
            FilterChip(
              selected = selectedModuleFilter == filter,
              onClick = { selectedModuleFilter = filter },
              label = { Text(filter, fontSize = 13.sp) }
            )
          }
        }
      }

      if (filteredItems.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(44.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text("Bu kategoride henüz kayıtlı ürün yok", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text(
                "Modüllerden ürün eklediğinizde her ürün için özel bildirim zamanı ayarlayabilirsiniz.",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
              )
            }
          }
        }
      } else {
        items(filteredItems, key = { "${it.moduleType}_${it.itemId}" }) { item ->
          val existingReminder = reminders.firstOrNull {
            it.moduleType == item.moduleType && it.itemId == item.itemId
          }

          ProductReminderCard(
            item = item,
            existingReminder = existingReminder,
            fallbackTime = defaultNotifyTime,
            fallbackSound = defaultSoundTone,
            onSave = { updatedReminder ->
              onSaveReminder(updatedReminder)
              NotificationHelper.scheduleReminderAlarm(context, updatedReminder)
            },
            onTestNotification = { reminderToTest ->
              NotificationHelper.showNotificationNow(
                context = context,
                notificationId = (item.moduleType + "_" + item.itemId).hashCode(),
                itemName = item.title,
                moduleType = item.moduleType,
                daysBefore = reminderToTest.daysBefore,
                targetDate = item.targetExpiryDate,
                soundTone = reminderToTest.soundTone
              )
            }
          )
        }
      }
    }
  }
}

@Composable
private fun ProductReminderCard(
  item: UnifiedTrackableItem,
  existingReminder: ProductReminderEntity?,
  fallbackTime: String,
  fallbackSound: String,
  onSave: (ProductReminderEntity) -> Unit,
  onTestNotification: (ProductReminderEntity) -> Unit
) {
  val context = LocalContext.current
  var isEnabled by remember(existingReminder) {
    mutableStateOf(existingReminder?.isEnabled ?: true)
  }
  var daysBefore by remember(existingReminder) {
    mutableIntStateOf(existingReminder?.daysBefore ?: 7)
  }
  var notifyTime by remember(existingReminder, fallbackTime) {
    mutableStateOf(existingReminder?.notifyTime ?: fallbackTime)
  }
  var soundTone by remember(existingReminder, fallbackSound) {
    mutableStateOf(existingReminder?.soundTone ?: fallbackSound)
  }
  var showItemTimePicker by remember { mutableStateOf(false) }
  var savedConfirmation by remember { mutableStateOf(false) }

  fun currentEntity(): ProductReminderEntity {
    return ProductReminderEntity(
      id = existingReminder?.id ?: 0L,
      moduleType = item.moduleType,
      itemId = item.itemId,
      itemName = item.title,
      targetExpiryDate = item.targetExpiryDate,
      daysBefore = daysBefore,
      notifyTime = notifyTime,
      soundTone = soundTone,
      isEnabled = isEnabled
    )
  }

  if (showItemTimePicker) {
    AppleStyleTimePickerDialog(
      initialTime = notifyTime,
      onTimeSelected = { selected ->
        notifyTime = selected
        showItemTimePicker = false
        val entity = currentEntity().copy(notifyTime = selected)
        onSave(entity)
        savedConfirmation = true
      },
      onDismiss = { showItemTimePicker = false }
    )
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(item.accentColor)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = item.title,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color(0xFF1C1C1E)
            )
            Text(
              text = "${item.moduleLabel} • Son Tarih: ${item.targetExpiryDate}",
              fontSize = 12.sp,
              color = Color(0xFF8E8E93)
            )
          }
        }

        Switch(
          checked = isEnabled,
          onCheckedChange = { checked ->
            isEnabled = checked
            onSave(currentEntity().copy(isEnabled = checked))
            savedConfirmation = true
          }
        )
      }

      if (isEnabled) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Ne Zaman Bildirim Gelsin? (Son Tarihten Önce)",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF3A3A3C)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          NotificationHelper.daysBeforeOptions.forEach { (days, label) ->
            FilterChip(
              selected = daysBefore == days,
              onClick = {
                daysBefore = days
                onSave(currentEntity().copy(daysBefore = days))
                savedConfirmation = true
              },
              label = { Text(label.removeSuffix(" Önce"), fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sound selection row for this product
        Text(
          text = "Bildirim Müziği & Saati",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF3A3A3C)
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(NotificationHelper.soundOptions) { sound ->
            FilterChip(
              selected = soundTone == sound.key,
              onClick = {
                soundTone = sound.key
                NotificationHelper.previewSound(context, sound.key)
                onSave(currentEntity().copy(soundTone = sound.key))
                savedConfirmation = true
              },
              label = { Text(sound.label, fontSize = 11.sp) },
              leadingIcon = {
                Icon(
                  Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp)
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { showItemTimePicker = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Saat: $notifyTime", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
              onClick = { onTestNotification(currentEntity()) },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Test Et", fontSize = 12.sp)
            }

            Button(
              onClick = {
                onSave(currentEntity())
                savedConfirmation = true
              },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (savedConfirmation) "Kuruldu" else "Kaydet", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleStyleTimePickerDialog(
  initialTime: String,
  onTimeSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val parts = initialTime.split(":")
  val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
  val initMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

  val timePickerState = rememberTimePickerState(
    initialHour = initHour,
    initialMinute = initMinute,
    is24Hour = true
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Bildirim Saatini Seçin", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
    text = {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TimePicker(state = timePickerState)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val formatted = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
          onTimeSelected(formatted)
        }
      ) {
        Text("Tamam", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal")
      }
    }
  )
}
