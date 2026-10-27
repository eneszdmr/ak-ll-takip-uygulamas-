package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.DatePickerField
import com.example.ui.components.InlineReminderSetupCard
import com.example.ui.components.LiveProductScannerDialog
import com.example.ui.components.ScanStep
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
  productToEdit: ProductEntity? = null,
  onSave: (
    name: String,
    category: String,
    storageLocation: String,
    familyMember: String,
    expiryDate: String,
    openedDate: String?,
    paoMonths: Int?,
    notes: String,
    isOpened: Boolean
  ) -> Unit,
  onSaveReminderConfig: (
    enabled: Boolean,
    daysBefore: Int,
    notifyTime: String,
    soundTone: String
  ) -> Unit = { _, _, _, _ -> },
  onBack: () -> Unit
) {
  var name by remember { mutableStateOf(productToEdit?.name ?: "") }
  var category by remember { mutableStateOf(productToEdit?.category ?: "İlaç") }
  var storageLocation by remember { mutableStateOf(productToEdit?.storageLocation ?: "Banyo Dolabı") }
  var familyMember by remember { mutableStateOf(productToEdit?.familyMember ?: "Ortak") }
  var expiryDate by remember {
    mutableStateOf(productToEdit?.expiryDate ?: LocalDate.now().plusMonths(6).toString())
  }
  var isOpened by remember { mutableStateOf(productToEdit?.isOpened ?: false) }
  var openedDate by remember {
    mutableStateOf(productToEdit?.openedDate ?: LocalDate.now().toString())
  }
  var paoMonths by remember { mutableStateOf(productToEdit?.paoMonths?.toString() ?: "6") }
  var notes by remember { mutableStateOf(productToEdit?.notes ?: "") }

  var reminderEnabled by remember { mutableStateOf(true) }
  var reminderDaysBefore by remember { mutableIntStateOf(7) }
  var reminderTime by remember { mutableStateOf("09:00") }
  var reminderSound by remember { mutableStateOf("DEFAULT") }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  // Step-by-step live camera scanner state
  var activeScannerStep by remember { mutableStateOf<ScanStep?>(null) }
  var step1Done by remember { mutableStateOf(false) }
  var step2Done by remember { mutableStateOf(false) }

  val categories = listOf("İlaç", "Şurup", "Güneş Kremi", "Cilt Bakımı", "Vitamin", "Diğer")
  val storageLocations = listOf("Banyo Dolabı", "Buzdolabı", "Çocuk Odası", "Çanta")
  val familyMembers = listOf("Anne", "Baba", "Çocuk 1", "Ortak")
  val paoOptions = listOf(3, 6, 12, 24)

  if (activeScannerStep != null) {
    LiveProductScannerDialog(
      initialStep = activeScannerStep!!,
      onProductScanned = { info ->
        if (info.name.isNotBlank()) {
          name = info.name
        }
        category = info.category
        if (info.whatItDoes.isNotBlank()) {
          notes = info.whatItDoes
        }
        if (info.suggestedPaoMonths != null) {
          paoMonths = info.suggestedPaoMonths.toString()
        }
        step1Done = true

        if (!info.expiryDate.isNullOrBlank()) {
          expiryDate = info.expiryDate
          step2Done = true
          activeScannerStep = null
          successMessage = "Adım 1 & Adım 2 tamamlandı! Ürün bilgileri ve SKT (${info.expiryDate}) kutudan otomatik alındı."
          errorMessage = null
        } else {
          // Automatically prompt Step 2 to scan Expiry Date via OCR from the box
          activeScannerStep = null
          successMessage = "Adım 1 tamamlandı (Ürün bilgisi alındı). Şimdi Adım 2 ile kutu üzerindeki Son Kullanma Tarihini (SKT) okutabilirsiniz."
          errorMessage = null
        }
      },
      onExpiryDateScanned = { scannedDate ->
        expiryDate = scannedDate
        step2Done = true
        activeScannerStep = null
        successMessage = "Adım 2 tamamlandı! Son Kullanma Tarihi ($scannedDate) kutu üzerinden OCR ile alındı."
        errorMessage = null
      },
      onDismiss = {
        activeScannerStep = null
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (productToEdit == null) "Yeni Ürün Ekle" else "Ürünü Düzenle") },
        navigationIcon = {
          IconButton(onClick = onBack) {
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

      if (successMessage != null) {
        Surface(
          color = Color(0xFFECFDF5),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF059669),
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = successMessage!!,
              color = Color(0xFF065F46),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Live Step-by-Step ITS Barcode / QR & Box OCR Scanner Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "Canlı Akıllı Kutu Tarama (Adım Adım)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                "Kutu üzerindeki İTS Karekod/Barkod ve SKT bilgisini kamerayla okutun",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { activeScannerStep = ScanStep.STEP_1_BARCODE_AND_BOX },
              modifier = Modifier
                .weight(1f)
                .testTag("scan_step1_button"),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) {
              Icon(
                if (step1Done) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("1. İTS / Kutu Tara", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            FilledTonalButton(
              onClick = { activeScannerStep = ScanStep.STEP_2_EXPIRY_OCR },
              modifier = Modifier
                .weight(1f)
                .testTag("scan_step2_button"),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) {
              Icon(
                if (step2Done) Icons.Default.CheckCircle else Icons.Default.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("2. SKT OCR Tara", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Ürün Adı *") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_name_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      // Category Chips
      Text("Kategori", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = categories, selectedItem = category) { category = it }

      // Storage Location
      Text("Saklama Konumu", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = storageLocations, selectedItem = storageLocation) {
        storageLocation = it
      }

      // Family Member
      Text("Kime Ait?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      ScrollableChipRow(items = familyMembers, selectedItem = familyMember) { familyMember = it }

      // Expiry Date with Calendar DatePicker
      DatePickerField(
        value = expiryDate,
        onValueChange = { expiryDate = it },
        label = "Son Kullanma Tarihi (YYYY-MM-DD) *",
        inputTestTag = "expiry_date_input"
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Bu ürün daha önce açıldı mı?", fontWeight = FontWeight.Medium)
            Switch(
              checked = isOpened,
              onCheckedChange = { isOpened = it },
              modifier = Modifier.testTag("opened_switch")
            )
          }

          if (isOpened) {
            Spacer(modifier = Modifier.height(12.dp))
            DatePickerField(
              value = openedDate,
              onValueChange = { openedDate = it },
              label = "Açılış Tarihi (YYYY-MM-DD)",
              inputTestTag = "opened_date_input"
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("PAO Süresi (Açıldıktan Sonra Kaç Ay?)", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(paoOptions) { months ->
                FilterChip(
                  selected = paoMonths == months.toString(),
                  onClick = { paoMonths = months.toString() },
                  label = { Text("$months Ay") }
                )
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Ne İşe Yarar / Açıklama ve Notlar") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_notes_input"),
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

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = {
          if (name.isBlank()) {
            errorMessage = "Ürün adı boş olamaz."
            return@Button
          }
          try {
            LocalDate.parse(expiryDate.trim())
          } catch (_: Exception) {
            errorMessage = "Geçersiz son kullanma tarihi formatı (YYYY-MM-DD olmalı)."
            return@Button
          }

          val parsedPao = paoMonths.toIntOrNull()
          onSaveReminderConfig(reminderEnabled, reminderDaysBefore, reminderTime, reminderSound)
          onSave(
            name.trim(),
            category,
            storageLocation,
            familyMember,
            expiryDate.trim(),
            if (isOpened) openedDate.trim() else null,
            parsedPao,
            notes.trim(),
            isOpened
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_product_button"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun ScrollableChipRow(items: List<String>, selectedItem: String, onSelect: (String) -> Unit) {
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    items(items) { item ->
      FilterChip(
        selected = selectedItem == item,
        onClick = { onSelect(item) },
        label = { Text(item) }
      )
    }
  }
}
