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
import com.example.data.model.SubscriptionEntity
import com.example.ui.components.*
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDashboardScreen(
  subscriptions: List<SubscriptionEntity>,
  onAddSubscriptionClicked: () -> Unit,
  onEditSubscription: (SubscriptionEntity) -> Unit,
  onDeleteSubscription: (SubscriptionEntity) -> Unit,
  isInitialLoading: Boolean = false,
  isSyncing: Boolean = false,
  onRefresh: () -> Unit = {},
  onBackToHub: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var subscriptionPendingDelete by remember { mutableStateOf<SubscriptionEntity?>(null) }

  val filteredSubscriptions = remember(subscriptions, searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) subscriptions
    else subscriptions.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.category.contains(q, ignoreCase = true) ||
        it.billingCycle.contains(q, ignoreCase = true) ||
        it.notes.contains(q, ignoreCase = true)
    }
  }

  if (subscriptionPendingDelete != null) {
    DeleteConfirmationDialog(
      itemName = subscriptionPendingDelete!!.name,
      onConfirm = {
        onDeleteSubscription(subscriptionPendingDelete!!)
        subscriptionPendingDelete = null
      },
      onDismiss = { subscriptionPendingDelete = null }
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
                .background(Color(0xFF5856D6).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Autorenew,
                contentDescription = null,
                tint = Color(0xFF5856D6),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              "Abonelik & Fatura Takip",
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
              .semantics { contentDescription = "Abonelikleri Yenile" }
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF5856D6)
              )
            } else {
              Icon(Icons.Default.Sync, contentDescription = "Yenile", tint = Color(0xFF5856D6))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddSubscriptionClicked,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Yeni Abonelik Ekle", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF5856D6),
        contentColor = Color.White,
        modifier = Modifier
          .pressClickEffect()
          .testTag("fab_add_subscription")
          .semantics { contentDescription = "Yeni Abonelik Ekle" }
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
            placeholder = "Abonelik adı, kategori veya not ara...",
            testTag = "subscription_search_input"
          )
        }

        if (isInitialLoading && subscriptions.isEmpty()) {
          item {
            SkeletonLoadingList(count = 3)
          }
        } else if (filteredSubscriptions.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.Autorenew,
              title = if (searchQuery.isNotBlank()) "Eşleşen Abonelik Bulunamadı" else "Henüz Abonelik Kaydı Yok",
              subtitle = if (searchQuery.isNotBlank()) {
                "Arama filtresini temizleyerek tüm aboneliklerinizi görüntüleyebilirsiniz."
              } else {
                "Dijital platform, internet, spor salonu veya aylık düzenli faturalarınızı ekleyerek ödeme günlerini takip edin."
              },
              accentColor = Color(0xFF5856D6),
              actionLabel = if (searchQuery.isNotBlank()) "Aramayı Temizle" else "İlk Aboneliği Ekle",
              onActionClick = {
                if (searchQuery.isNotBlank()) searchQuery = "" else onAddSubscriptionClicked()
              }
            )
          }
        } else {
          items(filteredSubscriptions, key = { it.id }) { sub ->
            SubscriptionItemCard(
              subscription = sub,
              onEdit = { onEditSubscription(sub) },
              onDelete = { subscriptionPendingDelete = sub }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SubscriptionItemCard(
  subscription: SubscriptionEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val today = LocalDate.now()
  val daysLeft = try {
    ExpiryCalculator.getDaysLeft(subscription.nextPaymentDate, today)
  } catch (_: Exception) {
    0
  }
  val status = ExpiryCalculator.getStatus(daysLeft)
  val badgeColor = when (status) {
    ExpiryStatus.CRITICAL -> Color(0xFFFF3B30)
    ExpiryStatus.WARNING -> Color(0xFFFF9500)
    ExpiryStatus.SAFE -> Color(0xFF34C759)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .semantics { contentDescription = "${subscription.name}, ${subscription.billingCycle}" },
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
            subscription.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1C1C1E),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            "${subscription.category} • ${subscription.billingCycle}" +
              if (subscription.price.isNotBlank()) " • ${subscription.price} ₺" else "",
            fontSize = 13.sp,
            color = Color(0xFF5856D6),
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
            text = if (daysLeft < 0) "Ödeme Geçti (${-daysLeft}g)" else "$daysLeft Gün Kaldı",
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Sonraki Ödeme / Yenileme: ${subscription.nextPaymentDate}" +
          if (subscription.autoRenew) " (Otomatik Yenileme Açık)" else " (Manuel Ödeme)",
        fontSize = 12.sp,
        color = Color(0xFF8E8E93),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      if (subscription.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          subscription.notes,
          fontSize = 12.sp,
          color = Color(0xFF3A3A3C),
          maxLines = 2,
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
            .semantics { contentDescription = "${subscription.name} aboneliğini düzenle" }
        ) {
          Icon(
            Icons.Default.Edit,
            contentDescription = "Düzenle",
            tint = Color(0xFF5856D6),
            modifier = Modifier.size(20.dp)
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .pressClickEffect()
            .semantics { contentDescription = "${subscription.name} aboneliğini sil" }
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Sil",
            tint = Color(0xFFFF3B30),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
