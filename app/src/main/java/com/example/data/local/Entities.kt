package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val fila: Int,
    val producto: String,
    val cantidad: Int,
    val precioUsd: Double,
    val catalogo: String,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): Product = Product(
        fila = fila,
        producto = producto,
        cantidad = cantidad,
        precioUsd = precioUsd,
        catalogo = catalogo
    )
}

fun Product.toEntity(): ProductEntity = ProductEntity(
    fila = fila,
    producto = producto,
    cantidad = cantidad,
    precioUsd = precioUsd,
    catalogo = catalogo
)

@Entity(tableName = "sales_history")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuario: String,
    val timestamp: Long,
    val totalUsd: Double,
    val totalBs: Double,
    val itemsJson: String,
    val synced: Boolean = true
)
