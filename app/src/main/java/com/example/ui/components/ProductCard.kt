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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import com.example.utils.ExpiryCalculator
import com.example.utils.ExpiryStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    ExpiryStatus.CRITICAL -> Triple(
      DangerRedBg,
      DangerRedText,
      if (daysLeft < 0) "${-daysLeft} gün geçti!" else "$daysLeft gün kaldı"
    )
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
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .semantics { contentDescription = "${product.name}, ${product.category}, $statusLabel" },
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0xFF00C7BE).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = product.category,
              tint = Color(0xFF0D9488)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = product.name,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color(0xFF1C1C1E),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              BadgeText(text = product.category)
              BadgeText(text = product.storageLocation)
              BadgeText(text = product.familyMember)
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(statusBg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = statusLabel,
            color = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }
      }

      if (product.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = product.notes,
          fontSize = 13.sp,
          color = Color(0xFF4B5563),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = Color(0xFFF2F2F7))
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Son Tarih: $effectiveExpiry",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF6B7280),
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          if (!product.isOpened) {
            TextButton(
              onClick = { onOpenToday(product) },
              modifier = Modifier
                .pressClickEffect()
                .semantics { contentDescription = "${product.name} ürününü bugün açtım olarak işaretle" },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Bugün Açtım", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          IconButton(
            onClick = { onEdit(product) },
            modifier = Modifier
              .size(48.dp)
              .pressClickEffect()
              .semantics { contentDescription = "${product.name} ürününü düzenle" }
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "Düzenle",
              modifier = Modifier.size(20.dp),
              tint = Color(0xFF007AFF)
            )
          }

          IconButton(
            onClick = { onDelete(product) },
            modifier = Modifier
              .size(48.dp)
              .pressClickEffect()
              .semantics { contentDescription = "${product.name} ürününü sil" }
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Sil",
              modifier = Modifier.size(20.dp),
              tint = DangerRedText
            )
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
      .background(Color(0xFFF2F2F7))
      .padding(horizontal = 7.dp, vertical = 3.dp)
  ) {
    Text(
      text = text,
      fontSize = 11.sp,
      color = Color(0xFF3A3A3C),
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
