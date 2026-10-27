package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val itemName = intent.getStringExtra(EXTRA_ITEM_NAME) ?: "Ürün Hatırlatması"
    val moduleType = intent.getStringExtra(EXTRA_MODULE_TYPE) ?: "ECZA"
    val daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 7)
    val soundTone = intent.getStringExtra(EXTRA_SOUND_TONE) ?: "DEFAULT"
    val targetDate = intent.getStringExtra(EXTRA_TARGET_DATE) ?: ""
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, itemName.hashCode())

    NotificationHelper.showNotificationNow(
      context = context,
      notificationId = notificationId,
      itemName = itemName,
      moduleType = moduleType,
      daysBefore = daysBefore,
      targetDate = targetDate,
      soundTone = soundTone
    )
  }

  companion object {
    const val EXTRA_ITEM_NAME = "extra_item_name"
    const val EXTRA_MODULE_TYPE = "extra_module_type"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_SOUND_TONE = "extra_sound_tone"
    const val EXTRA_TARGET_DATE = "extra_target_date"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
  }
}
