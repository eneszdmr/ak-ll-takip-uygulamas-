package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
import com.example.data.model.VehicleInsuranceEntity
import com.example.ui.components.DatePickerField
import com.example.ui.components.InlineReminderSetupCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleInsuranceScreen(
  vehicleToEdit: VehicleInsuranceEntity? = null,
  onSave: (
    plateAndVehicle: String,
    policyType: String,
    company: String,
    startDate: String,
    expiryDate: String,
    notes: String,
    reminderEnabled: Boolean,
    reminderDaysBefore: Int,
    reminderTime: String,
    reminderSound: String
  ) -> Unit,
  onBack: () -> Unit
) {
  var plateAndVehicle by remember { mutableStateOf(vehicleToEdit?.plateAndVehicle ?: "") }
  var policyType by remember { mutableStateOf(vehicleToEdit?.policyType ?: "Trafik Sigortası") }
  var company by remember { mutableStateOf(vehicleToEdit?.company ?: "") }
  var startDate by remember {
    mutableStateOf(vehicleToEdit?.startDate ?: LocalDate.now().toString())
  }
  var expiryDate by remember {
    mutableStateOf(vehicleToEdit?.expiryDate ?: LocalDate.now().plusYears(1).toString())
  }
  var notes by remember { mutableStateOf(vehicleToEdit?.notes ?: "") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  var reminderEnabled by remember { mutableStateOf(true) }
  var reminderDaysBefore by remember { mutableIntStateOf(14) }
  var reminderTime by remember { mutableStateOf("09:00") }
  var reminderSound by remember { mutableStateOf("DEFAULT") }

  val policyTypes = listOf(
    "Trafik Sigortası",
    "Kasko",
    "TÜVTÜRK Muayene",
    "Periyodik Bakım",
    "Egzoz Muayenesi",
    "Diğer"
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            if (vehicleToEdit == null) "Yeni Araç / Sigorta Ekle" else "Araç / Sigorta Düzenle",
            fontWeight = FontWeight.Bold
          )
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
        value = plateAndVehicle,
        onValueChange = { plateAndVehicle = it },
        label = { Text("Plaka ve Araç Modeli * (örn: 34 ABC 123 - Megane)") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vehicle_plate_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      Text("İşlem / Poliçe Türü", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = policyTypes, selectedItem = policyType) { policyType = it }

      OutlinedTextField(
        value = company,
        onValueChange = { company = it },
        label = { Text("Sigorta Şirketi / Servis (örn: Allianz, Anadolu Sigorta)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      DatePickerField(
        value = startDate,
        onValueChange = {
          startDate = it
          try {
            val parsed = LocalDate.parse(it.trim())
            expiryDate = parsed.plusYears(1).toString()
          } catch (_: Exception) {
          }
        },
        label = "Başlangıç / İşlem Tarihi (YYYY-MM-DD) *",
        inputTestTag = "vehicle_start_date_input"
      )

      DatePickerField(
        value = expiryDate,
        onValueChange = { expiryDate = it },
        label = "Bitiş / Yenileme Tarihi (YYYY-MM-DD) *",
        inputTestTag = "vehicle_expiry_date_input"
      )

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Poliçe No / Ekspertiz ve Bakım Notları") },
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
          if (plateAndVehicle.isBlank()) {
            errorMessage = "Plaka ve araç bilgisi boş olamaz."
            return@Button
          }
          try {
            LocalDate.parse(startDate.trim())
            LocalDate.parse(expiryDate.trim())
          } catch (_: Exception) {
            errorMessage = "Geçersiz tarih formatı (YYYY-MM-DD olmalı)."
            return@Button
          }

          isSubmitting = true
          errorMessage = null
          onSave(
            plateAndVehicle.trim(),
            policyType,
            company.trim(),
            startDate.trim(),
            expiryDate.trim(),
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
          .testTag("save_vehicle_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Kaydediliyor...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
          Text("Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
