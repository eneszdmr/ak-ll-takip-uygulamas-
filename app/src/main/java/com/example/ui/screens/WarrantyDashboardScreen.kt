package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.WarrantyEntity
import com.example.ui.components.DatabaseConnectionTester
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyDashboardScreen(
  warranties: List<WarrantyEntity>,
  onAddWarrantyClicked: () -> Unit,
  onEditWarranty: (WarrantyEntity) -> Unit,
  onDeleteWarranty: (WarrantyEntity) -> Unit,
  onBackToHub: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Garanti & Fatura Takip", fontWeight = FontWeight.Bold, fontSize = 19.sp)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackToHub) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana Menü")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddWarrantyClicked,
        containerColor = Color(0xFF2563EB),
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_warranty")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Yeni Garanti Ekle")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 84.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        DatabaseConnectionTester()
      }

      if (warranties.isEmpty()) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFDBEAFE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(36.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Henüz Garanti Kaydı Yok", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Sağ alttaki '+' butonuna basarak elektronik, beyaz eşya veya cihazlarınızın garanti sürelerini takip etmeye başlayın.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        items(warranties, key = { it.id }) { item ->
          WarrantyItemCard(
            warranty = item,
            onEdit = { onEditWarranty(item) },
            onDelete = { onDeleteWarranty(item) }
          )
        }
      }
    }
  }
}

@Composable
private fun WarrantyItemCard(
  warranty: WarrantyEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val today = LocalDate.now()
  val daysLeft: Int = try {
    ExpiryCalculator.getDaysLeft(warranty.warrantyEndDate, today)
  } catch (_: Exception) {
    0
  }
  val status = ExpiryCalculator.getStatus(daysLeft)
  val badgeColor = when (status) {
    ExpiryStatus.CRITICAL -> Color(0xFFDC2626)
    ExpiryStatus.WARNING -> Color(0xFFD97706)
    ExpiryStatus.SAFE -> Color(0xFF059669)
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(warranty.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          if (warranty.brandModel.isNotBlank()) {
            Text(
              warranty.brandModel,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "${warranty.category} • ${warranty.warrantyMonths} Ay Garanti",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Surface(
          color = badgeColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = if (daysLeft < 0) "Garanti Bitti (${-daysLeft}g)" else "$daysLeft Gün Kaldı",
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Başlangıç: ${warranty.purchaseDate}  •  Garanti Bitiş: ${warranty.warrantyEndDate}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (warranty.storeInvoice.isNotBlank()) {
        Text(
          text = "Mağaza / Fatura: ${warranty.storeInvoice}",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (warranty.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = warranty.notes,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        IconButton(onClick = onEdit) {
          Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Sil",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
