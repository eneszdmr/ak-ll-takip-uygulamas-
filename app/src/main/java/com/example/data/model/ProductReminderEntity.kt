package com.example.data.model

data class ProductReminderEntity(
  val id: Long = 0,
  val moduleType: String, // "ECZA", "GARANTI", "ABONELIK", "ARAC"
  val itemId: Long,
  val itemName: String,
  val targetExpiryDate: String, // YYYY-MM-DD
  val daysBefore: Int = 7, // 1 = 1 Gün, 7 = 1 Hafta, 14 = 2 Hafta, 30 = 1 Ay
  val notifyTime: String = "09:00", // HH:mm
  val soundTone: String = "DEFAULT", // "DEFAULT", "ALARM", "CHIME", "URGENT"
  val isEnabled: Boolean = true
)
