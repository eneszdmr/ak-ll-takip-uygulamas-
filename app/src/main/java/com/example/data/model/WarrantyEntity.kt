package com.example.data.model

data class WarrantyEntity(
  val id: Long = 0,
  val name: String,
  val brandModel: String,
  val category: String,
  val purchaseDate: String, // ISO: YYYY-MM-DD
  val warrantyMonths: Int = 24,
  val warrantyEndDate: String, // ISO: YYYY-MM-DD
  val storeInvoice: String = "",
  val notes: String = ""
)
