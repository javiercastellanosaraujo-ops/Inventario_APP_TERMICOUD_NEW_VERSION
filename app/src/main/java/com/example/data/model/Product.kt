package com.example.data.model

import java.util.UUID

data class Product(
    val fila: Int = 0,
    val id: String = UUID.randomUUID().toString(),
    val producto: String = "",
    val cantidad: Int = 0,
    val precioUsd: Double = 0.0,
    val precioCompra: Double = 0.0,
    val precioMayor: Double? = null,
    val cantidadMinimaMayor: Int? = null,
    val catalogo: String = "General",
    val codigo: String = "",
    val codigoBarras: String = "",
    val marca: String = "",
    val modelo: String = "",
    val ubicacion: String = "",
    val minStock: Int = 5
) {
    val costoUnitario: Double
        get() = precioCompra

    val gananciaDetal: Double
        get() = if (precioCompra > 0) (precioUsd - precioCompra).coerceAtLeast(0.0) else precioUsd

    val margenDetalPorcentaje: Double
        get() = if (precioUsd > 0 && precioCompra > 0) {
            ((precioUsd - precioCompra) / precioUsd) * 100.0
        } else 0.0

    val gananciaMayor: Double
        get() {
            val pm = precioMayor ?: precioUsd
            return if (precioCompra > 0) (pm - precioCompra).coerceAtLeast(0.0) else pm
        }

    val margenMayorPorcentaje: Double
        get() {
            val pm = precioMayor ?: precioUsd
            return if (pm > 0 && precioCompra > 0) {
                ((pm - precioCompra) / pm) * 100.0
            } else 0.0
        }
}

enum class PriceMode {
    AUTOMATICO,
    DETAL,
    MAYOR
}

data class CartItem(
    val product: Product = Product(),
    val cantidadSelected: Int = 0,
    val isCombo: Boolean = false,
    val combo: Combo? = null,
    val priceMode: PriceMode = PriceMode.AUTOMATICO
) {
    val tienePrecioMayorConfigurado: Boolean
        get() = !isCombo && product.precioMayor != null && product.precioMayor > 0

    val esPrecioMayorAplicado: Boolean
        get() = when {
            isCombo -> false
            !tienePrecioMayorConfigurado -> false
            priceMode == PriceMode.MAYOR -> true
            priceMode == PriceMode.DETAL -> false
            else -> {
                val minQty = if ((product.cantidadMinimaMayor ?: 0) > 0) product.cantidadMinimaMayor!! else 1
                cantidadSelected >= minQty
            }
        }

    val precioUnitarioAplicado: Double
        get() = when {
            isCombo && combo != null -> combo.precioUsd
            esPrecioMayorAplicado && product.precioMayor != null -> product.precioMayor!!
            else -> product.precioUsd
        }

    val subtotalUsd: Double
        get() = precioUnitarioAplicado * cantidadSelected

    val nombreItem: String
        get() = if (isCombo && combo != null) combo.nombre else product.producto

    val maxDisponible: Int
        get() = if (isCombo && combo != null) combo.disponibles else product.cantidad

    val itemFila: Int
        get() = if (isCombo && combo != null) combo.fila else product.fila
}

data class SaleItem(
    val fila: Int = 0,
    val productoId: String = "",
    val producto: String = "",
    val cantidad: Int = 0,
    val precioUsd: Double = 0.0,
    val precioCompra: Double = 0.0,
    val codigoBarras: String = "",
    val tipo: String = "producto", // "producto" o "combo"
    val componentes: List<ComboComponente> = emptyList(),
    val esPrecioMayor: Boolean = false
) {
    val costoUnitario: Double
        get() = if (tipo == "combo" && componentes.isNotEmpty()) {
            componentes.sumOf { it.precioCompraUnitario * it.cantidadPorCombo }
        } else {
            precioCompra
        }

    val gananciaNetaUnitaria: Double
        get() = (precioUsd - costoUnitario).coerceAtLeast(0.0)

    val gananciaNetaTotal: Double
        get() = gananciaNetaUnitaria * cantidad
}

data class Sale(
    val id: String = UUID.randomUUID().toString(),
    val folio: String = "",
    val clienteNombre: String = "",
    val clienteCedula: String = "",
    val metodoPago: String = "",
    val usuario: String = "",
    val usuarioEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0,
    val tasaBcv: Double = 0.0,
    val items: List<SaleItem> = emptyList(),
    val pdfUri: String? = null,
    val esReversado: Boolean = false,
    val fechaReverso: Long? = null,
    val reversadoPorNombre: String? = null,
    val reversadoPorEmail: String? = null
) {
    val costoTotalUsd: Double
        get() = items.sumOf { it.costoUnitario * it.cantidad }

    val gananciaNetaUsd: Double
        get() = (totalUsd - costoTotalUsd).coerceAtLeast(0.0)

    val margenGananciaPorcentaje: Double
        get() = if (totalUsd > 0) (gananciaNetaUsd / totalUsd) * 100.0 else 0.0
}

enum class TipoMovimiento {
    ENTRADA, SALIDA, CAMBIO_PRECIO, REVERSO
}

enum class StockFilter {
    TODOS, AGOTADOS, STOCK_BAJO
}

data class Movimiento(
    val id: String = UUID.randomUUID().toString(),
    val productoId: String = "",
    val productoFila: Int = 0,
    val productoNombre: String = "",
    val tipo: TipoMovimiento = TipoMovimiento.ENTRADA,
    val cantidad: Int = 0,
    val fecha: Long = System.currentTimeMillis(),
    val motivo: String = "",
    val precioUnitarioUsd: Double = 0.0,
    val usuarioEmail: String = "",
    val usuarioNombre: String = "",
    val esReversado: Boolean = false,
    val fechaReverso: Long? = null,
    val reversadoPorEmail: String? = null
)

data class UserSession(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
)

// API Request/Response Models for Google Apps Script fallback
data class AppsScriptListResponse(
    val ok: Boolean = false,
    val productos: List<RawProductRow>? = null,
    val msg: String? = null
)

data class RawProductRow(
    val fila: Any? = null,
    val Producto: String? = null,
    val Cantidad: Any? = null,
    val precioUsd: Any? = null,
    val catalogo: String? = null
)

/**
 * Returns all individual valid barcodes and aliases for this product.
 */
fun Product.getAllBarcodesList(): List<String> {
    val result = mutableSetOf<String>()
    if (codigo.isNotBlank()) result.add(codigo.trim())
    if (codigoBarras.isNotBlank()) {
        val parts = codigoBarras.split(",", ";", "\n", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        result.addAll(parts)
    }
    return result.toList()
}

/**
 * Checks if the scanned barcode matches any of this product's barcodes or aliases.
 */
fun Product.matchesBarcode(scannedCode: String): Boolean {
    val clean = scannedCode.trim()
    if (clean.isBlank()) return false
    val allCodes = getAllBarcodesList()
    return allCodes.any { it.equals(clean, ignoreCase = true) }
}

/**
 * Find candidate similar product by name tokens / levenshtein distance in inventory.
 */
fun List<Product>.findSimilarProductByName(query: String): Product? {
    val clean = query.trim().lowercase()
    if (clean.length < 3) return null

    val tokens = clean.split(" ", "-", "_").filter { it.length > 2 }

    // 1. Direct contains
    val directMatch = firstOrNull { prod ->
        val prodName = prod.producto.lowercase()
        prodName.contains(clean) || (tokens.isNotEmpty() && tokens.all { prodName.contains(it) })
    }
    if (directMatch != null) return directMatch

    // 2. Partial token overlap (at least 2 tokens or >50% tokens)
    if (tokens.isNotEmpty()) {
        val bestTokenMatch = maxByOrNull { prod ->
            val pTokens = prod.producto.lowercase().split(" ", "-", "_").filter { it.length > 2 }
            tokens.count { t -> pTokens.any { pt -> pt.contains(t) || t.contains(pt) } }
        }
        if (bestTokenMatch != null) {
            val pTokens = bestTokenMatch.producto.lowercase().split(" ", "-", "_").filter { it.length > 2 }
            val matchCount = tokens.count { t -> pTokens.any { pt -> pt.contains(t) || t.contains(pt) } }
            if (matchCount >= 2 || (tokens.size == 1 && matchCount == 1)) {
                return bestTokenMatch
            }
        }
    }

    return null
}

