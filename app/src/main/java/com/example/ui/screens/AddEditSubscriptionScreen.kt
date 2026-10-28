package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionEntity
import com.example.ui.components.DatePickerField
import com.example.ui.components.InlineReminderSetupCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionScreen(
  subscriptionToEdit: SubscriptionEntity? = null,
  onSave: (
    name: String,
    category: String,
    billingCycle: String,
    price: String,
    nextPaymentDate: String,
    autoRenew: Boolean,
    notes: String,
    reminderEnabled: Boolean,
    reminderDaysBefore: Int,
    reminderTime: String,
    reminderSound: String
  ) -> Unit,
  onBack: () -> Unit
) {
  var name by remember { mutableStateOf(subscriptionToEdit?.name ?: "") }
  var category by remember { mutableStateOf(subscriptionToEdit?.category ?: "Dijital Platform") }
  var billingCycle by remember { mutableStateOf(subscriptionToEdit?.billingCycle ?: "Aylık") }
  var priceText by remember { mutableStateOf(subscriptionToEdit?.price ?: "") }
  var nextPaymentDate by remember {
    mutableStateOf(
      subscriptionToEdit?.nextPaymentDate ?: LocalDate.now().plusMonths(1).toString()
    )
  }
  var autoRenew by remember { mutableStateOf(subscriptionToEdit?.autoRenew ?: true) }
  var notes by remember { mutableStateOf(subscriptionToEdit?.notes ?: "") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  var reminderEnabled by remember { mutableStateOf(true) }
  var reminderDaysBefore by remember { mutableIntStateOf(7) }
  var reminderTime by remember { mutableStateOf("09:00") }
  var reminderSound by remember { mutableStateOf("DEFAULT") }

  val categories = listOf(
    "Dijital Platform",
    "İnternet & TV",
    "Yazılım & Bulut",
    "Spor & Sağlık",
    "Müzik & Oyun",
    "Diğer"
  )
  val cycles = listOf("Aylık", "Yıllık", "Haftalık")

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(if (subscriptionToEdit == null) "Yeni Abonelik Ekle" else "Aboneliği Düzenle", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
          IconButton(onClick = { if (!isSubmitting) onBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            fontSize = 13.sp
          )
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Abonelik / Hizmet Adı * (örn: Netflix, Turkcell, iCloud)") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("subscription_name_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      Text("Kategori", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = categories, selectedItem = category) { category = it }

      Text("Ödeme Periyodu", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = cycles, selectedItem = billingCycle) { billingCycle = it }

      OutlinedTextField(
        value = priceText,
        onValueChange = { priceText = it },
        label = { Text("Tutar (₺) (örn: 229.90)") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("subscription_price_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp)
      )

      DatePickerField(
        value = nextPaymentDate,
        onValueChange = { nextPaymentDate = it },
        label = "Sonraki Ödeme / Yenileme Tarihi (YYYY-MM-DD) *",
        inputTestTag = "subscription_next_date_input"
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Otomatik Yenileme Aktif mi?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
          Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Kart / Hesap veya Paket Notları") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(12.dp)
      )

      InlineReminderSetupCard(
        enabled = reminderEnabled,
        onEnabledChange = { reminderEnabled = it },
        daysBefore = reminderDaysBefore,
        onDaysBeforeChange = { reminderDaysBefore = it },
        notifyTime = reminderTime,
        onNotifyTimeChange = { reminderTime = it },
        soundTone = reminderSound,
        onSoundToneChange = { reminderSound = it }
      )

      Button(
        onClick = {
          if (isSubmitting) return@Button
          if (name.isBlank()) {
            errorMessage = "Abonelik adı boş olamaz."
            return@Button
          }
          try {
            LocalDate.parse(nextPaymentDate.trim())
          } catch (_: Exception) {
            errorMessage = "Geçersiz tarih formatı (YYYY-MM-DD olmalı)."
            return@Button
          }
          isSubmitting = true
          errorMessage = null
          onSave(
            name.trim(),
            category,
            billingCycle,
            priceText.trim(),
            nextPaymentDate.trim(),
            autoRenew,
            notes.trim(),
            reminderEnabled,
            reminderDaysBefore,
            reminderTime,
            reminderSound
          )
        },
        enabled = !isSubmitting,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_subscription_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Kaydediliyor...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
          Text("Aboneliği Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
