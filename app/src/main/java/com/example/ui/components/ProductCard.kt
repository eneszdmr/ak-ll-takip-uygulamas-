package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
  product: ProductEntity,
  onOpenToday: (ProductEntity) -> Unit,
  onEdit: (ProductEntity) -> Unit,
  onDelete: (ProductEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val today = LocalDate.now()
  val effectiveExpiry = ExpiryCalculator.calculateEffectiveExpiryDate(
    product.expiryDate,
    product.openedDate,
    product.paoMonths,
    today
  )
  val daysLeft = ExpiryCalculator.getDaysLeft(effectiveExpiry, today)
  val status = ExpiryCalculator.getStatus(daysLeft)

  val (statusBg, statusText, statusLabel) = when (status) {
    ExpiryStatus.CRITICAL -> Triple(DangerRedBg, DangerRedText, if (daysLeft < 0) "${-daysLeft} gün geçti!" else "$daysLeft gün kaldı")
    ExpiryStatus.WARNING -> Triple(WarningAmberBg, WarningAmberText, "$daysLeft gün kaldı")
    ExpiryStatus.SAFE -> Triple(SafeGreenBg, SafeGreenText, "$daysLeft gün kaldı")
  }

  val categoryIcon = when (product.category) {
    "İlaç" -> Icons.Default.Medication
    "Şurup" -> Icons.Default.LocalDrink
    "Güneş Kremi" -> Icons.Default.WbSunny
    "Cilt Bakımı" -> Icons.Default.Face
    "Vitamin" -> Icons.Default.HealthAndSafety
    else -> Icons.Default.MedicalServices
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = product.category,
              tint = MaterialTheme.colorScheme.primary
            )
          }

          Column {
            Text(
              text = product.name,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              BadgeText(text = product.category)
              BadgeText(text = product.storageLocation)
              BadgeText(text = product.familyMember)
            }
          }
        }

        // Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusBg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = statusLabel,
            color = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (product.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = product.notes,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Son Tarih: $effectiveExpiry",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (!product.isOpened) {
            TextButton(
              onClick = { onOpenToday(product) },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Bugün Açtım", fontSize = 12.sp)
            }
          }

          IconButton(onClick = { onEdit(product) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
          }

          IconButton(onClick = { onDelete(product) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(18.dp), tint = DangerRedText)
          }
        }
      }
    }
  }
}

@Composable
fun BadgeText(text: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = text,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Medium
    )
  }
}
