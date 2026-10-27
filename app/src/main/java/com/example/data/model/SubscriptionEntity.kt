package com.example.data.model

data class SubscriptionEntity(
  val id: Long = 0,
  val name: String,
  val category: String, // Dijital Platform, İnternet & Telefon, Ev Faturası, Spor & Sağlık, Yazılım & Bulut, Diğer
  val billingCycle: String = "Aylık", // Aylık, Yıllık
  val price: String = "",
  val nextPaymentDate: String, // YYYY-MM-DD
  val autoRenew: Boolean = true,
  val notes: String = ""
)
