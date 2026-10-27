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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionEntity
import com.example.ui.components.DatabaseConnectionTester
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
            Text("Abonelik & Fatura Takip", fontWeight = FontWeight.Bold, fontSize = 19.sp)
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
      FloatingActionButton(
        onClick = onAddSubscriptionClicked,
        containerColor = Color(0xFF5856D6),
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_subscription")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Yeni Abonelik Ekle")
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

      if (subscriptions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(Color(0xFF5856D6).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Autorenew,
                  contentDescription = null,
                  tint = Color(0xFF5856D6),
                  modifier = Modifier.size(34.dp)
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text("Henüz Abonelik Kaydı Yok", fontWeight = FontWeight.Bold, fontSize = 17.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "Dijital platform, internet, spor salonu veya aylık düzenli faturalarınızı ekleyerek ödeme günlerini takip edin.",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
              )
            }
          }
        }
      } else {
        items(subscriptions, key = { it.id }) { sub ->
          SubscriptionItemCard(
            subscription = sub,
            onEdit = { onEditSubscription(sub) },
            onDelete = { onDeleteSubscription(sub) }
          )
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
            subscription.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1C1C1E)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            "${subscription.category} • ${subscription.billingCycle}" +
              if (subscription.price.isNotBlank()) " • ${subscription.price} ₺" else "",
            fontSize = 13.sp,
            color = Color(0xFF5856D6),
            fontWeight = FontWeight.Medium
          )
        }

        Surface(
          color = badgeColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = if (daysLeft < 0) "Ödeme Geçti (${-daysLeft}g)" else "$daysLeft Gün Kaldı",
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Sonraki Ödeme / Yenileme: ${subscription.nextPaymentDate}" +
          if (subscription.autoRenew) " (Otomatik Yenileme Açık)" else " (Manuel Ödeme)",
        fontSize = 12.sp,
        color = Color(0xFF8E8E93)
      )

      if (subscription.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(subscription.notes, fontSize = 12.sp, color = Color(0xFF3A3A3C))
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
            tint = Color(0xFFFF3B30),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
