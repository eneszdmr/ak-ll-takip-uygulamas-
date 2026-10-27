package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.ProductReminderEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class SoundToneOption(
  val key: String,
  val label: String,
  val description: String
)

object NotificationHelper {

  val soundOptions = listOf(
    SoundToneOption(
      key = "DEFAULT",
      label = "Klasik Bildirim Sesi",
      description = "Standart sistem bildirim melodisi"
    ),
    SoundToneOption(
      key = "ALARM",
      label = "Alarm Melodisi",
      description = "Dikkat çekici uzun alarm müziği"
    ),
    SoundToneOption(
      key = "CHIME",
      label = "Yumuşak Zil Melodisi",
      description = "Telefon zil sesi tonu"
    ),
    SoundToneOption(
      key = "URGENT",
      label = "Acil Kritik Uyarı",
      description = "Hızlı çift sinyal uyarısı"
    )
  )

  val daysBeforeOptions = listOf(
    1 to "1 Gün Önce",
    7 to "1 Hafta Önce",
    14 to "2 Hafta Önce",
    30 to "1 Ay Önce"
  )

  private var activeRingtone: Ringtone? = null

  fun getSoundLabel(key: String): String {
    return soundOptions.firstOrNull { it.key == key }?.label ?: "Klasik Bildirim Sesi"
  }

  fun getDaysBeforeLabel(days: Int): String {
    return when (days) {
      1 -> "1 Gün Önce"
      7 -> "1 Hafta Önce"
      14 -> "2 Hafta Önce"
      30 -> "1 Ay Önce"
      else -> "$days Gün Önce"
    }
  }

  fun getSoundUri(soundTone: String): Uri {
    return when (soundTone) {
      "ALARM" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      "CHIME" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
  }

  /**
   * Plays a preview of the selected notification music/sound so the user can hear it.
   */
  fun previewSound(context: Context, soundTone: String) {
    try {
      activeRingtone?.stop()
      if (soundTone == "URGENT") {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        return
      }
      val uri = getSoundUri(soundTone)
      val ringtone = RingtoneManager.getRingtone(context, uri)
      activeRingtone = ringtone
      ringtone?.play()
    } catch (_: Exception) {
    }
  }

  fun stopPreviewSound() {
    try {
      activeRingtone?.stop()
    } catch (_: Exception) {
    }
  }

  /**
   * Schedules an alarm on the device for (targetExpiryDate - daysBefore) at notifyTime (HH:mm).
   */
  fun scheduleReminderAlarm(context: Context, reminder: ProductReminderEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val requestCode = (reminder.moduleType + "_" + reminder.itemId).hashCode()

    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
      putExtra(ReminderBroadcastReceiver.EXTRA_ITEM_NAME, reminder.itemName)
      putExtra(ReminderBroadcastReceiver.EXTRA_MODULE_TYPE, reminder.moduleType)
      putExtra(ReminderBroadcastReceiver.EXTRA_DAYS_BEFORE, reminder.daysBefore)
      putExtra(ReminderBroadcastReceiver.EXTRA_SOUND_TONE, reminder.soundTone)
      putExtra(ReminderBroadcastReceiver.EXTRA_TARGET_DATE, reminder.targetExpiryDate)
      putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (!reminder.isEnabled) {
      alarmManager.cancel(pendingIntent)
      return
    }

    try {
      val expiryDate = LocalDate.parse(reminder.targetExpiryDate.trim())
      val notifyDate = expiryDate.minusDays(reminder.daysBefore.toLong())
      val timeParts = reminder.notifyTime.split(":")
      val hour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
      val minute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

      var triggerDateTime = LocalDateTime.of(notifyDate, LocalTime.of(hour, minute))
      val now = LocalDateTime.now()

      // If the calculated reminder date/time is already in the past, schedule for today/tomorrow at the chosen hour:minute
      if (triggerDateTime.isBefore(now)) {
        val todayAtTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
        triggerDateTime = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
      }

      val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
          alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }
    } catch (_: Exception) {
    }
  }

  fun showNotificationNow(
    context: Context,
    notificationId: Int,
    itemName: String,
    moduleType: String,
    daysBefore: Int,
    targetDate: String,
    soundTone: String
  ) {
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val channelId = "eczatakip_channel_$soundTone"
    val soundUri = getSoundUri(soundTone)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Ürün & Süre Hatırlatmaları (${getSoundLabel(soundTone)})",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "SKT, Garanti, Abonelik ve Araç Sigorta hatırlatma bildirimleri"
        enableVibration(true)
        val audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .build()
        setSound(soundUri, audioAttributes)
      }
      notificationManager.createNotificationChannel(channel)
    }

    // Play the selected sound preview as well so the user hears it clearly
    previewSound(context, soundTone)

    val moduleTitle = when (moduleType) {
      "GARANTI" -> "Garanti & Fatura Takip"
      "ABONELIK" -> "Abonelik & Ödeme Takip"
      "ARAC" -> "Araç & Sigorta Takip"
      else -> "Ecza & Kozmetik Takip"
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("$moduleTitle • $itemName")
      .setContentText("Son tarihe (${targetDate}) ${getDaysBeforeLabel(daysBefore).lowercase()} hatırlatması!")
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(
          "$itemName için belirlediğiniz hatırlatma zamanı geldi. Son tarih: $targetDate (${getDaysBeforeLabel(daysBefore)} uyarısı)."
        )
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setSound(soundUri)
      .setContentIntent(contentPendingIntent)
      .build()

    try {
      notificationManager.notify(notificationId, notification)
    } catch (_: SecurityException) {
    }
  }
}
