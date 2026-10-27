package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ProductDao
import com.example.data.model.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun productDao(): ProductDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "ecza_takip_db"
        )
          .addCallback(DatabaseCallback())
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          CoroutineScope(Dispatchers.IO).launch {
            populateInitialData(database.productDao())
          }
        }
      }

      suspend fun populateInitialData(dao: ProductDao) {
        val today = LocalDate.now()
        dao.insertProduct(
          ProductEntity(
            name = "Parasetamol Şurubu",
            category = "Şurup",
            storageLocation = "Banyo Dolabı",
            familyMember = "Çocuk 1",
            expiryDate = today.plusDays(5).toString(), // Critical (5 days)
            openedDate = today.minusDays(10).toString(),
            paoMonths = 3,
            notes = "Ateş düşürücü ve ağrı kesici",
            isOpened = true
          )
        )
        dao.insertProduct(
          ProductEntity(
            name = "Güneş Kremi SPF 50+",
            category = "Güneş Kremi",
            storageLocation = "Çanta",
            familyMember = "Ortak",
            expiryDate = today.plusDays(20).toString(), // Warning (20 days)
            openedDate = today.minusDays(60).toString(),
            paoMonths = 12,
            notes = "Yüz ve vücut için yüksek koruma",
            isOpened = true
          )
        )
        dao.insertProduct(
          ProductEntity(
            name = "C Vitamini Takviyesi",
            category = "Vitamin",
            storageLocation = "Banyo Dolabı",
            familyMember = "Anne",
            expiryDate = today.plusDays(120).toString(), // Safe (120 days)
            openedDate = null,
            paoMonths = 6,
            notes = "Bağışıklık destekleyici efervesan",
            isOpened = false
          )
        )
        dao.insertProduct(
          ProductEntity(
            name = "Nemlendirici Cilt Kremi",
            category = "Cilt Bakımı",
            storageLocation = "Banyo Dolabı",
            familyMember = "Baba",
            expiryDate = today.plusDays(180).toString(), // Safe
            openedDate = today.minusDays(15).toString(),
            paoMonths = 12,
            notes = "Günlük nemlendirici bakım",
            isOpened = true
          )
        )
      }
    }
  }
}
