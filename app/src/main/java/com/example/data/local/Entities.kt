package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val fila: Int,
    val id: String = "",
    val producto: String,
    val cantidad: Int,
    val precioUsd: Double,
    val precioCompra: Double = 0.0,
    val precioMayor: Double? = null,
    val cantidadMinimaMayor: Int? = null,
    val catalogo: String = "General",
    val codigo: String = "",
    val codigoBarras: String = "",
    val marca: String = "",
    val modelo: String = "",
    val ubicacion: String = "",
    val minStock: Int = 5,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): Product = Product(
        fila = fila,
        id = id.ifBlank { "prod_$fila" },
        producto = producto,
        cantidad = cantidad,
        precioUsd = precioUsd,
        precioCompra = precioCompra,
        precioMayor = precioMayor,
        cantidadMinimaMayor = cantidadMinimaMayor,
        catalogo = catalogo,
        codigo = codigo,
        codigoBarras = codigoBarras,
        marca = marca,
        modelo = modelo,
        ubicacion = ubicacion,
        minStock = minStock
    )
}

fun Product.toEntity(): ProductEntity = ProductEntity(
    fila = fila,
    id = id,
    producto = producto,
    cantidad = cantidad,
    precioUsd = precioUsd,
    precioCompra = precioCompra,
    precioMayor = precioMayor,
    cantidadMinimaMayor = cantidadMinimaMayor,
    catalogo = catalogo,
    codigo = codigo,
    codigoBarras = codigoBarras,
    marca = marca,
    modelo = modelo,
    ubicacion = ubicacion,
    minStock = minStock
)

@Entity(tableName = "sales_history")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val folio: String = "",
    val usuario: String,
    val usuarioEmail: String = "",
    val clienteNombre: String = "",
    val clienteCedula: String = "",
    val metodoPago: String = "",
    val timestamp: Long,
    val totalUsd: Double,
    val totalBs: Double,
    val totalGananciaUsd: Double = 0.0,
    val tasaBcv: Double = 0.0,
    val itemsJson: String,
    val synced: Boolean = true
)
