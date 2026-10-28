package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WarrantyEntity
import com.example.ui.components.DatePickerField
import com.example.ui.components.InlineReminderSetupCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWarrantyScreen(
  warrantyToEdit: WarrantyEntity? = null,
  onSave: (
    name: String,
    brandModel: String,
    category: String,
    purchaseDate: String,
    warrantyMonths: Int,
    warrantyEndDate: String,
    storeInvoice: String,
    notes: String
  ) -> Unit,
  onSaveReminderConfig: (
    enabled: Boolean,
    daysBefore: Int,
    notifyTime: String,
    soundTone: String
  ) -> Unit = { _, _, _, _ -> },
  onBack: () -> Unit
) {
  var name by remember { mutableStateOf(warrantyToEdit?.name ?: "") }
  var brandModel by remember { mutableStateOf(warrantyToEdit?.brandModel ?: "") }
  var category by remember { mutableStateOf(warrantyToEdit?.category ?: "Elektronik") }
  var purchaseDate by remember {
    mutableStateOf(warrantyToEdit?.purchaseDate ?: LocalDate.now().toString())
  }
  var warrantyMonths by remember { mutableIntStateOf(warrantyToEdit?.warrantyMonths ?: 24) }
  var warrantyEndDate by remember {
    mutableStateOf(
      warrantyToEdit?.warrantyEndDate ?: LocalDate.now().plusMonths(24).toString()
    )
  }
  var storeInvoice by remember { mutableStateOf(warrantyToEdit?.storeInvoice ?: "") }
  var notes by remember { mutableStateOf(warrantyToEdit?.notes ?: "") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  var reminderEnabled by remember { mutableStateOf(true) }
  var reminderDaysBefore by remember { mutableIntStateOf(14) }
  var reminderTime by remember { mutableStateOf("09:00") }
  var reminderSound by remember { mutableStateOf("DEFAULT") }

  val categories = listOf(
    "Elektronik",
    "Beyaz Eşya",
    "Küçük Ev Aleti",
    "Telefon & Bilgisayar",
    "Mobilya",
    "Diğer"
  )
  val monthOptions = listOf(12, 24, 36, 48, 60)

  fun recalculateEndDate(baseDateStr: String, months: Int) {
    try {
      val parsed = LocalDate.parse(baseDateStr.trim())
      warrantyEndDate = parsed.plusMonths(months.toLong()).toString()
    } catch (_: Exception) {
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (warrantyToEdit == null) "Yeni Garanti Ekle" else "Garantiyi Düzenle", fontWeight = FontWeight.Bold) },
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
        label = { Text("Cihaz / Ürün Adı * (örn: Süpürge, Buzdolabı)") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("warranty_name_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      OutlinedTextField(
        value = brandModel,
        onValueChange = { brandModel = it },
        label = { Text("Marka / Model (örn: Dyson V15, Bosch Serie 6)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      Text("Kategori", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = categories, selectedItem = category) { category = it }

      DatePickerField(
        value = purchaseDate,
        onValueChange = {
          purchaseDate = it
          recalculateEndDate(it, warrantyMonths)
        },
        label = "Fatura / Satın Alma Tarihi (YYYY-MM-DD) *",
        inputTestTag = "warranty_purchase_date_input"
      )

      Text("Garanti Süresi (Ay)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(monthOptions) { m ->
          FilterChip(
            selected = warrantyMonths == m,
            onClick = {
              warrantyMonths = m
              recalculateEndDate(purchaseDate, m)
            },
            label = { Text("$m Ay (${m / 12} Yıl)") }
          )
        }
      }

      DatePickerField(
        value = warrantyEndDate,
        onValueChange = { warrantyEndDate = it },
        label = "Garanti Bitiş Tarihi (YYYY-MM-DD) *",
        inputTestTag = "warranty_end_date_input"
      )

      OutlinedTextField(
        value = storeInvoice,
        onValueChange = { storeInvoice = it },
        label = { Text("Mağaza / Fatura No (örn: MediaMarkt #2026-412)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Servis / Ek Garanti Notları") },
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
            errorMessage = "Cihaz / ürün adı boş olamaz."
            return@Button
          }
          try {
            LocalDate.parse(purchaseDate.trim())
            LocalDate.parse(warrantyEndDate.trim())
          } catch (_: Exception) {
            errorMessage = "Geçersiz tarih formatı (YYYY-MM-DD olmalı)."
            return@Button
          }

          isSubmitting = true
          errorMessage = null
          onSaveReminderConfig(reminderEnabled, reminderDaysBefore, reminderTime, reminderSound)
          onSave(
            name.trim(),
            brandModel.trim(),
            category,
            purchaseDate.trim(),
            warrantyMonths,
            warrantyEndDate.trim(),
            storeInvoice.trim(),
            notes.trim()
          )
        },
        enabled = !isSubmitting,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_warranty_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Kaydediliyor...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
          Text("Garantiyi Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
