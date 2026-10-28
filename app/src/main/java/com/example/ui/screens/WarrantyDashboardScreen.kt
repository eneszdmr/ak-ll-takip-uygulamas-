package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.WarrantyEntity
import com.example.ui.components.*
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
  isInitialLoading: Boolean = false,
  isSyncing: Boolean = false,
  onRefresh: () -> Unit = {},
  onBackToHub: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var warrantyPendingDelete by remember { mutableStateOf<WarrantyEntity?>(null) }

  val filteredWarranties = remember(warranties, searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) warranties
    else warranties.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.brandModel.contains(q, ignoreCase = true) ||
        it.category.contains(q, ignoreCase = true) ||
        it.storeInvoice.contains(q, ignoreCase = true) ||
        it.notes.contains(q, ignoreCase = true)
    }
  }

  if (warrantyPendingDelete != null) {
    DeleteConfirmationDialog(
      itemName = warrantyPendingDelete!!.name,
      onConfirm = {
        onDeleteWarranty(warrantyPendingDelete!!)
        warrantyPendingDelete = null
      },
      onDismiss = { warrantyPendingDelete = null }
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
                .background(Color(0xFF007AFF).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              "Garanti & Fatura Takip",
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
              .semantics { contentDescription = "Garantileri Yenile" }
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF007AFF)
              )
            } else {
              Icon(Icons.Default.Sync, contentDescription = "Yenile", tint = Color(0xFF007AFF))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddWarrantyClicked,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Yeni Garanti Ekle", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF007AFF),
        contentColor = Color.White,
        modifier = Modifier
          .pressClickEffect()
          .testTag("fab_add_warranty")
          .semantics { contentDescription = "Yeni Garanti Ekle" }
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
            placeholder = "Cihaz adı, marka/model veya fatura ara...",
            testTag = "warranty_search_input"
          )
        }

        if (isInitialLoading && warranties.isEmpty()) {
          item {
            SkeletonLoadingList(count = 3)
          }
        } else if (filteredWarranties.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.VerifiedUser,
              title = if (searchQuery.isNotBlank()) "Eşleşen Garanti Kaydı Yok" else "Henüz Garanti Kaydı Yok",
              subtitle = if (searchQuery.isNotBlank()) {
                "Arama filtresini temizleyerek tüm cihazlarınızı görüntüleyebilirsiniz."
              } else {
                "Elektronik, beyaz eşya veya ev cihazlarınızın garanti sürelerini ve fatura bilgilerini buradan takip edin."
              },
              accentColor = Color(0xFF007AFF),
              actionLabel = if (searchQuery.isNotBlank()) "Aramayı Temizle" else "İlk Garantiyi Ekle",
              onActionClick = {
                if (searchQuery.isNotBlank()) searchQuery = "" else onAddWarrantyClicked()
              }
            )
          }
        } else {
          items(filteredWarranties, key = { it.id }) { item ->
            WarrantyItemCard(
              warranty = item,
              onEdit = { onEditWarranty(item) },
              onDelete = { warrantyPendingDelete = item }
            )
          }
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
    modifier = Modifier
      .fillMaxWidth()
      .semantics { contentDescription = "${warranty.name}, ${warranty.brandModel}" },
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
            warranty.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1C1C1E),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          if (warranty.brandModel.isNotBlank()) {
            Text(
              warranty.brandModel,
              fontSize = 13.sp,
              color = Color(0xFF6B7280),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "${warranty.category} • ${warranty.warrantyMonths} Ay Garanti",
            fontSize = 12.sp,
            color = Color(0xFF007AFF),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          color = badgeColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = if (daysLeft < 0) "Garanti Bitti (${-daysLeft}g)" else "$daysLeft Gün Kaldı",
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Başlangıç: ${warranty.purchaseDate}  •  Garanti Bitiş: ${warranty.warrantyEndDate}",
        fontSize = 12.sp,
        color = Color(0xFF4B5563),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      if (warranty.storeInvoice.isNotBlank()) {
        Text(
          text = "Mağaza / Fatura: ${warranty.storeInvoice}",
          fontSize = 12.sp,
          color = Color(0xFF6B7280),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (warranty.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = warranty.notes,
          fontSize = 12.sp,
          color = Color(0xFF1C1C1E),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier
            .size(48.dp)
            .pressClickEffect()
            .semantics { contentDescription = "${warranty.name} garantisini düzenle" }
        ) {
          Icon(
            Icons.Default.Edit,
            contentDescription = "Düzenle",
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(20.dp)
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .pressClickEffect()
            .semantics { contentDescription = "${warranty.name} garantisini sil" }
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Sil",
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
