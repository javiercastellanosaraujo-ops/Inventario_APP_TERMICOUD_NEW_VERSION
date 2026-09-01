package com.example.util

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
}
