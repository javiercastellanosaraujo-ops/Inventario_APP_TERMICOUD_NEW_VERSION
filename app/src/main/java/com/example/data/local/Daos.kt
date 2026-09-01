package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY catalogo ASC, producto ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(product: ProductEntity)

    @Query("DELETE FROM products WHERE fila = :fila")
    suspend fun deleteByFila(fila: Int)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales_history ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT * FROM sales_history WHERE synced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE sales_history SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM sales_history WHERE firestoreId = :firestoreId")
    suspend fun deleteByFirestoreId(firestoreId: String)

    @Query("DELETE FROM sales_history")
    suspend fun deleteAllSales()
}
