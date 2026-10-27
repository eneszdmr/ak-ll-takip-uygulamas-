package com.example.data.repository

import com.example.data.dao.ProductDao
import com.example.data.model.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
  val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

  suspend fun insert(product: ProductEntity) {
    productDao.insertProduct(product)
  }

  suspend fun update(product: ProductEntity) {
    productDao.updateProduct(product)
  }

  suspend fun delete(product: ProductEntity) {
    productDao.deleteProduct(product)
  }
}
