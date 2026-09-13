package com.example.util

import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.UsuarioGanancia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MonthlyReportGenerator {

    /**
     * Genera un reporte CSV formateado con el resumen de ganancias por usuario y el detalle de ventas del mes.
     * Cumple con los estándares RFC 4180 de CSV: valores de texto entre comillas dobles y comillas internas escapadas.
     */
    fun generateMonthlyReportCsv(
        mesKey: String, // ej. "2026-08" o "Ventas_2026-08"
        usuarios: List<UsuarioGanancia>,
        ventas: List<Sale>,
        tasaCambio: Double = 1.0
    ): String {
        val cleanMes = mesKey.removePrefix("Ventas_").removePrefix("ventas_").trim()
        val sb = StringBuilder()

        fun escape(value: Any?): String {
            val str = value?.toString() ?: ""
            val escaped = str.replace("\"", "\"\"")
            return "\"$escaped\""
        }

        // ==========================================
        // 1. BLOQUE RESUMEN DE GANANCIAS POR USUARIO
        // ==========================================
        sb.append(escape("RESUMEN DE GANANCIAS - $cleanMes")).append("\n")
        sb.append(
            listOf(
                escape("Usuario"),
                escape("Ventas"),
                escape("Unidades"),
                escape("Total USD"),
                escape("Total Bs")
            ).joinToString(",")
        ).append("\n")

        var totalVentasSum = 0
        var totalUnidadesSum = 0
        var totalUsdSum = 0.0
        var totalBsSum = 0.0

        for (u in usuarios) {
            totalVentasSum += u.ventas
            totalUnidadesSum += u.unidades
            totalUsdSum += u.totalUsd
            totalBsSum += u.totalBs

            sb.append(
                listOf(
                    escape(u.usuario.ifBlank { "Operador" }),
                    escape(u.ventas),
                    escape(u.unidades),
                    escape(String.format(Locale.US, "%.2f", u.totalUsd)),
                    escape(String.format(Locale.US, "%.2f", u.totalBs))
                ).joinToString(",")
            ).append("\n")
        }

        // Fila de TOTAL al final del resumen
        sb.append(
            listOf(
                escape("TOTAL"),
                escape(totalVentasSum),
                escape(totalUnidadesSum),
                escape(String.format(Locale.US, "%.2f", totalUsdSum)),
                escape(String.format(Locale.US, "%.2f", totalBsSum))
            ).joinToString(",")
        ).append("\n\n")

        // ==========================================
        // 2. BLOQUE DETALLE DE VENTAS
        // ==========================================
        sb.append(escape("DETALLE DE VENTAS")).append("\n")
        sb.append(
            listOf(
                escape("Fecha"),
                escape("Usuario"),
                escape("Total USD"),
                escape("Total Bs"),
                escape("Productos")
            ).joinToString(",")
        ).append("\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sortedSales = ventas.filter { !it.esReversado }.sortedBy { it.timestamp }

        for (sale in sortedSales) {
            val fechaStr = if (sale.timestamp > 0L) {
                dateFormat.format(Date(sale.timestamp))
            } else {
                "N/A"
            }

            val userStr = sale.usuario.trim().ifBlank { sale.usuarioEmail.trim() }.ifBlank { "Operador" }
            val rate = if (sale.tasaBcv > 0) sale.tasaBcv else tasaCambio.coerceAtLeast(1.0)
            val bsAmount = if (sale.totalBs > 0) sale.totalBs else sale.totalUsd * rate

            val productosStr = if (sale.items.isNotEmpty()) {
                sale.items.joinToString(" | ") { item ->
                    val prodName = item.producto.trim().ifBlank { "Producto" }
                    val qty = item.cantidad
                    val price = String.format(Locale.US, "$%.2f", item.precioUsd)
                    "$prodName (x$qty - $price)"
                }
            } else {
                "Venta mostrador"
            }

            sb.append(
                listOf(
                    escape(fechaStr),
                    escape(userStr),
                    escape(String.format(Locale.US, "%.2f", sale.totalUsd)),
                    escape(String.format(Locale.US, "%.2f", bsAmount)),
                    escape(productosStr)
                ).joinToString(",")
            ).append("\n")
        }

        return sb.toString()
    }

    /**
     * Genera un reporte consolidado completo de Cierre Mensual de INVENTARIO Y VENTAS.
     * Incluye:
     * 1. Resumen Ejecutivo con KPIs de Ventas y Valoración de Inventario al Cierre.
     * 2. Desglose de Rendimiento por Vendedor / Usuario.
     * 3. Auditoría y Valoración detallada del Inventario Físico (Stock, Costos, Venta y Estado).
     * 4. Detalle completo de transacciones de Ventas del período.
     */
    fun generateConsolidatedClosingReport(
        mesKey: String,
        usuarios: List<UsuarioGanancia>,
        ventas: List<Sale>,
        inventario: List<Product>,
        tasaCambio: Double = 1.0,
        cerradoPor: String = "Administrador"
    ): String {
        val cleanMes = mesKey.removePrefix("Ventas_").removePrefix("ventas_").trim()
        val sb = StringBuilder()
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        fun escape(value: Any?): String {
            val str = value?.toString() ?: ""
            val escaped = str.replace("\"", "\"\"")
            return "\"$escaped\""
        }

        // =========================================================================
        // CÁLCULOS GENERALES DE VENTAS E INVENTARIO
        // =========================================================================
        val nonRevertedSales = ventas.filter { !it.esReversado }
        val totalVentasCount = nonRevertedSales.size
        var totalVentasUsd = 0.0
        var totalVentasBs = 0.0
        var totalCostoVentasUsd = 0.0
        var totalUnidadesVendidas = 0

        for (sale in nonRevertedSales) {
            totalVentasUsd += sale.totalUsd
            val rate = if (sale.tasaBcv > 0) sale.tasaBcv else tasaCambio.coerceAtLeast(1.0)
            totalVentasBs += if (sale.totalBs > 0) sale.totalBs else (sale.totalUsd * rate)
            totalCostoVentasUsd += sale.costoTotalUsd
            totalUnidadesVendidas += sale.items.sumOf { it.cantidad }
        }

        val gananciaNetaVentasUsd = (totalVentasUsd - totalCostoVentasUsd).coerceAtLeast(0.0)
        val margenVentasPorcentaje = if (totalVentasUsd > 0) {
            (gananciaNetaVentasUsd / totalVentasUsd) * 100.0
        } else 0.0

        // Métricas de inventario
        val totalProductosCatalogo = inventario.size
        var totalUnidadesStock = 0
        var valorInventarioCostoUsd = 0.0
        var valorInventarioVentaUsd = 0.0
        var productosStockCritico = 0

        for (p in inventario) {
            val stock = p.cantidad.coerceAtLeast(0)
            totalUnidadesStock += stock
            valorInventarioCostoUsd += (p.precioCompra * stock)
            valorInventarioVentaUsd += (p.precioUsd * stock)
            if (p.minStock > 0 && stock <= p.minStock) {
                productosStockCritico++
            }
        }

        val gananciaProyectadaInventarioUsd = (valorInventarioVentaUsd - valorInventarioCostoUsd).coerceAtLeast(0.0)
        val margenProyectadoInventario = if (valorInventarioVentaUsd > 0) {
            (gananciaProyectadaInventarioUsd / valorInventarioVentaUsd) * 100.0
        } else 0.0

        // =========================================================================
        // SECCIÓN 1: RESUMEN EJECUTIVO CONSOLIDADO
        // =========================================================================
        sb.append(escape("REPORTE CONSOLIDADO DE CIERRE MENSUAL - INVENTARIO Y VENTAS")).append("\n")
        sb.append(listOf(escape("Periodo:"), escape(cleanMes), escape("Fecha Cierre:"), escape(nowFormatted)).joinToString(",")).append("\n")
        sb.append(listOf(escape("Responsable:"), escape(cerradoPor), escape("Tasa BCV Aplicada:"), escape(String.format(Locale.US, "%.2f Bs/$", tasaCambio))).joinToString(",")).append("\n\n")

        sb.append(escape("BALANCE FINANCIERO DE VENTAS (PERIODO $cleanMes)")).append("\n")
        sb.append(listOf(escape("Metrica"), escape("Valor USD"), escape("Valor Bs / Cantidad")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Total Facturado (Ventas Brutas)"), escape(String.format(Locale.US, "$%.2f", totalVentasUsd)), escape(String.format(Locale.US, "Bs %.2f", totalVentasBs))).joinToString(",")).append("\n")
        sb.append(listOf(escape("Costo Total de Ventas (COGS)"), escape(String.format(Locale.US, "$%.2f", totalCostoVentasUsd)), escape("N/A")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Ganancia Neta Realizada"), escape(String.format(Locale.US, "$%.2f", gananciaNetaVentasUsd)), escape("N/A")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Margen de Rentabilidad"), escape(String.format(Locale.US, "%.2f%%", margenVentasPorcentaje)), escape("N/A")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Transacciones Realizadas"), escape(totalVentasCount.toString()), escape("Facturas / Notas")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Unidades Totales Vendidas"), escape(totalUnidadesVendidas.toString()), escape("Unidades")).joinToString(",")).append("\n\n")

        sb.append(escape("VALORACIÓN Y ESTADO DE INVENTARIO AL CIERRE")).append("\n")
        sb.append(listOf(escape("Metrica Inventario"), escape("Valor USD"), escape("Detalle / Existencias")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Productos Registrados (SKUs)"), escape(totalProductosCatalogo.toString()), escape("Referencias activas")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Stock Fisico Total"), escape(totalUnidadesStock.toString()), escape("Unidades en existencia")).joinToString(",")).append("\n")
        sb.append(listOf(escape("Inversion en Stock (A Costo)"), escape(String.format(Locale.US, "$%.2f", valorInventarioCostoUsd)), escape(String.format(Locale.US, "Bs %.2f", valorInventarioCostoUsd * tasaCambio))).joinToString(",")).append("\n")
        sb.append(listOf(escape("Valor Comercial en Stock (A Venta)"), escape(String.format(Locale.US, "$%.2f", valorInventarioVentaUsd)), escape(String.format(Locale.US, "Bs %.2f", valorInventarioVentaUsd * tasaCambio))).joinToString(",")).append("\n")
        sb.append(listOf(escape("Ganancia Proyectada Inventario"), escape(String.format(Locale.US, "$%.2f", gananciaProyectadaInventarioUsd)), escape(String.format(Locale.US, "Margen %.1f%%", margenProyectadoInventario))).joinToString(",")).append("\n")
        sb.append(listOf(escape("Productos en Stock Bajo / Critico"), escape(productosStockCritico.toString()), escape("Requieren reposicion")).joinToString(",")).append("\n\n")

        // =========================================================================
        // SECCIÓN 2: RENDIMIENTO DE VENTAS POR USUARIO
        // =========================================================================
        sb.append(escape("RENDIMIENTO DE VENTAS POR USUARIO")).append("\n")
        sb.append(listOf(
            escape("Usuario"),
            escape("Transacciones"),
            escape("Unidades"),
            escape("Total USD"),
            escape("Total Bs"),
            escape("Costo USD"),
            escape("Ganancia Neta USD"),
            escape("Margen %")
        ).joinToString(",")).append("\n")

        for (u in usuarios) {
            sb.append(listOf(
                escape(u.usuario.ifBlank { "Operador" }),
                escape(u.ventas),
                escape(u.unidades),
                escape(String.format(Locale.US, "%.2f", u.totalUsd)),
                escape(String.format(Locale.US, "%.2f", u.totalBs)),
                escape(String.format(Locale.US, "%.2f", u.totalCostoUsd)),
                escape(String.format(Locale.US, "%.2f", u.gananciaNetaUsd)),
                escape(String.format(Locale.US, "%.1f%%", u.margenPorcentaje))
            ).joinToString(",")).append("\n")
        }
        sb.append("\n")

        // =========================================================================
        // SECCIÓN 3: AUDITORÍA Y VALORACIÓN DETALLADA DE INVENTARIO
        // =========================================================================
        sb.append(escape("AUDITORÍA DETALLADA DE INVENTARIO AL CIERRE")).append("\n")
        sb.append(listOf(
            escape("Codigo"),
            escape("Producto"),
            escape("Marca"),
            escape("Categoria"),
            escape("Stock Actual"),
            escape("Stock Minimo"),
            escape("Costo Unit USD"),
            escape("Precio Venta USD"),
            escape("Total Costo USD"),
            escape("Total Venta USD"),
            escape("Margen %"),
            escape("Estado Stock")
        ).joinToString(",")).append("\n")

        val sortedInventory = inventario.sortedWith(compareBy({ it.catalogo }, { it.producto }))
        for (p in sortedInventory) {
            val stock = p.cantidad.coerceAtLeast(0)
            val totalCostoProd = p.precioCompra * stock
            val totalVentaProd = p.precioUsd * stock
            val margenProd = p.margenDetalPorcentaje
            val estado = if (p.minStock > 0 && stock <= p.minStock) "CRÍTICO / BAJO" else "NORMAL"

            sb.append(listOf(
                escape(p.codigo.ifBlank { p.codigoBarras }.ifBlank { "-" }),
                escape(p.producto.ifBlank { "Sin nombre" }),
                escape(p.marca.ifBlank { "-" }),
                escape(p.catalogo.ifBlank { "General" }),
                escape(stock),
                escape(p.minStock),
                escape(String.format(Locale.US, "%.2f", p.precioCompra)),
                escape(String.format(Locale.US, "%.2f", p.precioUsd)),
                escape(String.format(Locale.US, "%.2f", totalCostoProd)),
                escape(String.format(Locale.US, "%.2f", totalVentaProd)),
                escape(String.format(Locale.US, "%.1f%%", margenProd)),
                escape(estado)
            ).joinToString(",")).append("\n")
        }
        sb.append("\n")

        // =========================================================================
        // SECCIÓN 4: DETALLE DE VENTAS DEL MES
        // =========================================================================
        sb.append(escape("DETALLE DE TRANSACCIONES DEL MES")).append("\n")
        sb.append(listOf(
            escape("Fecha y Hora"),
            escape("Folio / ID"),
            escape("Vendedor"),
            escape("Cliente"),
            escape("Total USD"),
            escape("Total Bs"),
            escape("Tasa BCV"),
            escape("Productos Vendidos")
        ).joinToString(",")).append("\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sortedSales = nonRevertedSales.sortedBy { it.timestamp }

        for (sale in sortedSales) {
            val fechaStr = if (sale.timestamp > 0L) dateFormat.format(Date(sale.timestamp)) else "N/A"
            val userStr = sale.usuario.trim().ifBlank { sale.usuarioEmail.trim() }.ifBlank { "Operador" }
            val clientStr = sale.clienteNombre.trim().ifBlank { "Consumidor Final" }
            val folioStr = sale.folio.ifBlank { sale.id.take(8) }
            val rate = if (sale.tasaBcv > 0) sale.tasaBcv else tasaCambio.coerceAtLeast(1.0)
            val bsAmount = if (sale.totalBs > 0) sale.totalBs else (sale.totalUsd * rate)

            val productosStr = if (sale.items.isNotEmpty()) {
                sale.items.joinToString(" | ") { item ->
                    val prodName = item.producto.trim().ifBlank { "Producto" }
                    val qty = item.cantidad
                    val price = String.format(Locale.US, "$%.2f", item.precioUsd)
                    "$prodName (x$qty - $price)"
                }
            } else {
                "Venta mostrador"
            }

            sb.append(listOf(
                escape(fechaStr),
                escape(folioStr),
                escape(userStr),
                escape(clientStr),
                escape(String.format(Locale.US, "%.2f", sale.totalUsd)),
                escape(String.format(Locale.US, "%.2f", bsAmount)),
                escape(String.format(Locale.US, "%.2f", rate)),
                escape(productosStr)
            ).joinToString(",")).append("\n")
        }

        return sb.toString()
    }
}

