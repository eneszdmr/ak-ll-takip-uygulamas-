package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleInsuranceEntity
import com.example.ui.components.DatabaseConnectionTester
import com.example.utils.ExpiryCalculator
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInsuranceDashboardScreen(
  vehicles: List<VehicleInsuranceEntity>,
  onAddVehicleClicked: () -> Unit,
  onEditVehicle: (VehicleInsuranceEntity) -> Unit,
  onDeleteVehicle: (VehicleInsuranceEntity) -> Unit,
  onBackToHub: () -> Unit
) {
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
            Text("Araç & Sigorta Takip", fontWeight = FontWeight.Bold, fontSize = 19.sp)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackToHub) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana Menü")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddVehicleClicked,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Poliçe / Muayene Ekle") },
        containerColor = Color(0xFFFF9500),
        contentColor = Color.White,
        modifier = Modifier.testTag("add_vehicle_fab")
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        DatabaseConnectionTester()
      }

      if (vehicles.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFF9500)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Henüz Araç / Sigorta Kaydı Yok",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Trafik sigortası, kasko, TÜVTÜRK muayene ve periyodik bakım tarihlerini buradan takip edin.",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
              )
            }
          }
        }
      } else {
        items(vehicles, key = { it.id }) { item ->
          VehicleInsuranceItemCard(
            item = item,
            onEdit = { onEditVehicle(item) },
            onDelete = { onDeleteVehicle(item) }
          )
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
    modifier = Modifier.fillMaxWidth(),
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
            color = Color(0xFF1C1C1E)
          )
          Text(
            text = "${item.policyType}${if (item.company.isNotBlank()) " • ${item.company}" else ""}",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93)
          )
        }
        Surface(
          color = badgeColor,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = statusText,
            color = badgeTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
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
        Column {
          Text(
            text = "Başlangıç: ${item.startDate}  •  Bitiş: ${item.expiryDate}",
            fontSize = 12.sp,
            color = Color(0xFF3A3A3C),
            fontWeight = FontWeight.Medium
          )
          if (item.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.notes,
              fontSize = 12.sp,
              color = Color(0xFF8E8E93)
            )
          }
        }

        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFFFF9500))
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
          }
        }
      }
    }
  }
}
