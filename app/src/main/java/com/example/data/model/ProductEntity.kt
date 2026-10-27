package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val category: String,          // İlaç, Şurup, Güneş Kremi, Cilt Bakımı, Vitamin, Diğer
  val storageLocation: String,   // Banyo Dolabı, Buzdolabı, Çocuk Odası, Çanta
  val familyMember: String,      // Anne, Baba, Çocuk 1, Ortak
  val expiryDate: String,        // YYYY-MM-DD (Classic SKT)
  val openedDate: String? = null, // YYYY-MM-DD (Opening Date)
  val paoMonths: Int? = null,    // Period After Opening in months
  val notes: String = "",        // Ne İşe Yarar / Notlar
  val isOpened: Boolean = false
)
