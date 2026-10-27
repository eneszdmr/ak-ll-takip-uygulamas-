package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.NotificationHelper
import com.example.ui.screens.AppleStyleTimePickerDialog

@Composable
fun InlineReminderSetupCard(
  enabled: Boolean,
  onEnabledChange: (Boolean) -> Unit,
  daysBefore: Int,
  onDaysBeforeChange: (Int) -> Unit,
  notifyTime: String,
  onNotifyTimeChange: (String) -> Unit,
  soundTone: String,
  onSoundToneChange: (String) -> Unit
) {
  val context = LocalContext.current
  var showTimePicker by remember { mutableStateOf(false) }

  if (showTimePicker) {
    AppleStyleTimePickerDialog(
      initialTime = notifyTime,
      onTimeSelected = { selected ->
        onNotifyTimeChange(selected)
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false }
    )
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2).copy(alpha = 0.7f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = Color(0xFFFF2D55),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Bu Ürün İçin Özel Bildirim Kur",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFF1C1C1E)
            )
            Text(
              text = "Son tarihten önce seçtiğiniz gün ve saatte müzikli uyarı",
              fontSize = 11.sp,
              color = Color(0xFF6B7280)
            )
          }
        }
        Switch(
          checked = enabled,
          onCheckedChange = onEnabledChange
        )
      }

      if (enabled) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Ne Zaman Bildirim Gelsin?",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          NotificationHelper.daysBeforeOptions.forEach { (days, label) ->
            FilterChip(
              selected = daysBefore == days,
              onClick = { onDaysBeforeChange(days) },
              label = { Text(label.removeSuffix(" Önce"), fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Bildirim Müziği Seçin (Dinlemek için dokunun)",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(NotificationHelper.soundOptions) { sound ->
            FilterChip(
              selected = soundTone == sound.key,
              onClick = {
                onSoundToneChange(sound.key)
                NotificationHelper.previewSound(context, sound.key)
              },
              label = { Text(sound.label, fontSize = 11.sp) },
              leadingIcon = {
                Icon(
                  Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp)
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = { showTimePicker = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Bildirim Saati: $notifyTime (Değiştirmek için dokunun)", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
