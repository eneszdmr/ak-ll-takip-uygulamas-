package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleInsuranceEntity
import com.example.ui.components.*
import com.example.utils.ExpiryCalculator
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInsuranceDashboardScreen(
  vehicles: List<VehicleInsuranceEntity>,
  onAddVehicleClicked: () -> Unit,
  onEditVehicle: (VehicleInsuranceEntity) -> Unit,
  onDeleteVehicle: (VehicleInsuranceEntity) -> Unit,
  isInitialLoading: Boolean = false,
  isSyncing: Boolean = false,
  onRefresh: () -> Unit = {},
  onBackToHub: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var vehiclePendingDelete by remember { mutableStateOf<VehicleInsuranceEntity?>(null) }

  val filteredVehicles = remember(vehicles, searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) vehicles
    else vehicles.filter {
      it.plateAndVehicle.contains(q, ignoreCase = true) ||
        it.policyType.contains(q, ignoreCase = true) ||
        it.company.contains(q, ignoreCase = true) ||
        it.notes.contains(q, ignoreCase = true)
    }
  }

  if (vehiclePendingDelete != null) {
    DeleteConfirmationDialog(
      itemName = "${vehiclePendingDelete!!.plateAndVehicle} (${vehiclePendingDelete!!.policyType})",
      onConfirm = {
        onDeleteVehicle(vehiclePendingDelete!!)
        vehiclePendingDelete = null
      },
      onDismiss = { vehiclePendingDelete = null }
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
                .background(Color(0xFFFF9500).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              "Araç & Sigorta Takip",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackToHub,
            modifier = Modifier.semantics { contentDescription = "Ana Menüye Dön" }
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana Menü")
          }
        },
        actions = {
          IconButton(
            onClick = onRefresh,
            modifier = Modifier
              .pressClickEffect()
              .semantics { contentDescription = "Araç Kayıtlarını Yenile" }
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFF9500)
              )
            } else {
              Icon(Icons.Default.Sync, contentDescription = "Yenile", tint = Color(0xFFFF9500))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddVehicleClicked,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Poliçe / Muayene Ekle", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFFFF9500),
        contentColor = Color.White,
        modifier = Modifier
          .pressClickEffect()
          .testTag("add_vehicle_fab")
          .semantics { contentDescription = "Yeni Poliçe veya Muayene Ekle" }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          DatabaseConnectionTester()
        }

        item {
          AppleSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Plaka, araç modeli, poliçe türü veya şirket ara...",
            testTag = "vehicle_search_input"
          )
        }

        if (isInitialLoading && vehicles.isEmpty()) {
          item {
            SkeletonLoadingList(count = 3)
          }
        } else if (filteredVehicles.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.DirectionsCar,
              title = if (searchQuery.isNotBlank()) "Eşleşen Araç Kaydı Yok" else "Henüz Araç / Sigorta Kaydı Yok",
              subtitle = if (searchQuery.isNotBlank()) {
                "Arama filtresini temizleyerek tüm araç ve sigorta kayıtlarınızı görebilirsiniz."
              } else {
                "Trafik sigortası, kasko, TÜVTÜRK muayene ve periyodik bakım tarihlerini buradan takip edin."
              },
              accentColor = Color(0xFFFF9500),
              actionLabel = if (searchQuery.isNotBlank()) "Aramayı Temizle" else "İlk Poliçeyi Ekle",
              onActionClick = {
                if (searchQuery.isNotBlank()) searchQuery = "" else onAddVehicleClicked()
              }
            )
          }
        } else {
          items(filteredVehicles, key = { it.id }) { item ->
            VehicleInsuranceItemCard(
              item = item,
              onEdit = { onEditVehicle(item) },
              onDelete = { vehiclePendingDelete = item }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VehicleInsuranceItemCard(
  item: VehicleInsuranceEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val today = LocalDate.now()
  val daysLeft = try {
    ExpiryCalculator.getDaysLeft(item.expiryDate, today)
  } catch (_: Exception) {
    0
  }

  val isExpired = daysLeft < 0
  val isUrgent = daysLeft in 0..30

  val badgeColor = when {
    isExpired -> Color(0xFFFEE2E2)
    isUrgent -> Color(0xFFFEF3C7)
    else -> Color(0xFFDCFCE7)
  }
  val badgeTextColor = when {
    isExpired -> Color(0xFFB91C1C)
    isUrgent -> Color(0xFFB45309)
    else -> Color(0xFF15803D)
  }
  val statusText = when {
    isExpired -> "Süresi Doldu (${-daysLeft} gün)"
    daysLeft == 0 -> "Bugün Son Gün!"
    else -> "$daysLeft gün kaldı"
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .semantics { contentDescription = "${item.plateAndVehicle}, ${item.policyType}, $statusText" },
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.plateAndVehicle,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF1C1C1E),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${item.policyType}${if (item.company.isNotBlank()) " • ${item.company}" else ""}",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          color = badgeColor,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = statusText,
            color = badgeTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Başlangıç: ${item.startDate}  •  Bitiş: ${item.expiryDate}",
            fontSize = 12.sp,
            color = Color(0xFF3A3A3C),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (item.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.notes,
              fontSize = 12.sp,
              color = Color(0xFF8E8E93),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row {
          IconButton(
            onClick = onEdit,
            modifier = Modifier
              .size(48.dp)
              .pressClickEffect()
              .semantics { contentDescription = "${item.plateAndVehicle} kaydını düzenle" }
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFFFF9500))
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .pressClickEffect()
              .semantics { contentDescription = "${item.plateAndVehicle} kaydını sil" }
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
          }
        }
      }
    }
  }
}
