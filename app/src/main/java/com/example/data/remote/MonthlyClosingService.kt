package com.example.data.remote

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import com.example.config.AppConfig
import com.example.data.model.CierreMensualInfo
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.UsuarioGanancia
import com.example.util.MonthlyReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parámetros para la ejecución del Cierre Mensual de Inventario y Ventas.
 */
data class MonthlyClosingParams(
    val context: Context? = null,
    val backendDriveUrl: String = "",
    val mesKey: String = "", // ej. "2026-09" o "Ventas_2026-09"
    val cerradoPorNombre: String = "",
    val cerradoPorEmail: String = "",
    val ventas: List<Sale> = emptyList(),
    val inventario: List<Product> = emptyList(),
    val usuariosGanancias: List<UsuarioGanancia> = emptyList(),
    val tasaCambio: Double = 1.0,
    val guardarCopiaLocal: Boolean = true
)

/**
 * Resultado detallado del Cierre Mensual consolidado.
 */
data class MonthlyClosingResult(
    val isSuccess: Boolean,
    val mes: String,
    val mesKey: String,
    val fileName: String,
    val driveUploadSuccess: Boolean,
    val driveFileId: String? = null,
    val driveResponse: String? = null,
    val localFilePath: String? = null,
    val totalVentasUsd: Double,
    val totalVentasBs: Double,
    val totalCostoVentasUsd: Double,
    val gananciaNetaUsd: Double,
    val margenVentasPorcentaje: Double,
    val transaccionesCount: Int,
    val unidadesVendidasCount: Int,
    val totalProductosInventario: Int,
    val unidadesStockTotal: Int,
    val valorInventarioCostoUsd: Double,
    val valorInventarioVentaUsd: Double,
    val gananciaProyectadaInventarioUsd: Double,
    val productosStockCritico: Int,
    val cierreInfo: CierreMensualInfo,
    val csvContent: String,
    val errorMessage: String? = null
)

/**
 * Servicio centralizado para ejecutar el Cierre Mensual de Inventario y Ventas.
 * Genera el reporte consolidado (KPIs de ventas, detalle de transacciones, auditoría de stock físico
 * y valoración a costo y venta) y lo sube automáticamente a la carpeta de Google Drive configurada
 * en la aplicación, de manera idéntica al flujo de exportación de comprobantes y notas de entrega.
 */
object MonthlyClosingService {

    private const val TAG = "MonthlyClosingService"

    /**
     * Realiza el cierre mensual consolidado de inventario y ventas:
     * 1. Calcula las métricas financieras de ventas y la valoración física del inventario al momento del corte.
     * 2. Genera el documento consolidado en formato CSV (RFC 4180).
     * 3. Guarda opcionalmente una copia local de respaldo en el almacenamiento del dispositivo.
     * 4. Sube automáticamente el archivo a la carpeta de Google Drive configurada en la app a través del Google Apps Script webhook.
     * 5. Retorna un objeto [MonthlyClosingResult] con el balance general y estado de la subida.
     */
    suspend fun realizarCierreMensualInventarioYVentas(
        params: MonthlyClosingParams
    ): Result<MonthlyClosingResult> = withContext(Dispatchers.IO) {
        try {
            val cleanMes = params.mesKey.removePrefix("Ventas_").removePrefix("ventas_").trim().ifBlank {
                val now = java.util.Calendar.getInstance()
                String.format(Locale.US, "%04d-%02d", now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
            }
            val formattedMesKey = if (params.mesKey.startsWith("Ventas_")) params.mesKey else "Ventas_$cleanMes"

            // 1. Filtrar ventas válidas del período
            val nonRevertedSales = params.ventas.filter { !it.esReversado }

            var totalVentasUsd = 0.0
            var totalVentasBs = 0.0
            var totalCostoVentasUsd = 0.0
            var totalUnidadesVendidas = 0

            for (sale in nonRevertedSales) {
                totalVentasUsd += sale.totalUsd
                val rate = if (sale.tasaBcv > 0) sale.tasaBcv else params.tasaCambio.coerceAtLeast(1.0)
                totalVentasBs += if (sale.totalBs > 0) sale.totalBs else (sale.totalUsd * rate)
                totalCostoVentasUsd += sale.costoTotalUsd
                totalUnidadesVendidas += sale.items.sumOf { it.cantidad }
            }

            val gananciaNetaUsd = (totalVentasUsd - totalCostoVentasUsd).coerceAtLeast(0.0)
            val margenVentas = if (totalVentasUsd > 0) (gananciaNetaUsd / totalVentasUsd) * 100.0 else 0.0

            // 2. Métricas de inventario físico
            val totalItemsInventario = params.inventario.size
            var unidadesStockTotal = 0
            var valorInventarioCostoUsd = 0.0
            var valorInventarioVentaUsd = 0.0
            var productosStockCritico = 0

            for (p in params.inventario) {
                val stock = p.cantidad.coerceAtLeast(0)
                unidadesStockTotal += stock
                valorInventarioCostoUsd += (p.precioCompra * stock)
                valorInventarioVentaUsd += (p.precioUsd * stock)
                if (p.minStock > 0 && stock <= p.minStock) {
                    productosStockCritico++
                }
            }

            val gananciaProyectadaInventarioUsd = (valorInventarioVentaUsd - valorInventarioCostoUsd).coerceAtLeast(0.0)

            // 3. Generar el reporte consolidado
            val csvContent = MonthlyReportGenerator.generateConsolidatedClosingReport(
                mesKey = cleanMes,
                usuarios = params.usuariosGanancias,
                ventas = nonRevertedSales,
                inventario = params.inventario,
                tasaCambio = params.tasaCambio,
                cerradoPor = params.cerradoPorNombre.ifBlank { "Administrador" }
            )

            // 4. Nombre del archivo y guardado local de respaldo
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Cierre_Inventario_Ventas_${cleanMes}_$timestampStr.csv"
            var localFilePath: String? = null

            if (params.guardarCopiaLocal && params.context != null) {
                try {
                    val storageDir = params.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: File(params.context.filesDir, "cierres_mensuales")
                    if (!storageDir.exists()) storageDir.mkdirs()
                    val csvFile = File(storageDir, fileName)
                    csvFile.writeText(csvContent, Charsets.UTF_8)
                    localFilePath = csvFile.absolutePath
                    Log.d(TAG, "Respaldo local guardado en: $localFilePath")
                } catch (e: Exception) {
                    Log.w(TAG, "Aviso guardando copia local del reporte: ${e.message}")
                }
            }

            // 5. Subir a Google Drive (carpeta configurada o AppConfig por defecto)
            val effectiveDriveUrl = params.backendDriveUrl.trim().ifBlank {
                AppConfig.GOOGLE_DRIVE_FOLDER_WEBHOOK_URL.trim()
            }

            var driveUploadSuccess = false
            var driveResponseText: String? = null
            var driveFileId: String? = null
            var uploadErrorMsg: String? = null

            if (effectiveDriveUrl.isNotBlank()) {
                val csvBytes = csvContent.toByteArray(Charsets.UTF_8)
                val csvBase64 = Base64.encodeToString(csvBytes, Base64.NO_WRAP)

                val payload = JSONObject().apply {
                    put("accion", "guardar_cierre_drive")
                    put("action", "guardar_cierre_drive")
                    put("tipo", "cierre_mensual_inventario_ventas")
                    put("sale_id", "CIERRE-$cleanMes")
                    put("folio", "CIERRE-$cleanMes")
                    put("cliente", "REPORTE CONSOLIDADO DE INVENTARIO Y VENTAS")
                    put("usuario", params.cerradoPorNombre.ifBlank { "Administrador" })
                    put("total_usd", totalVentasUsd)
                    put("totalUsd", totalVentasUsd)
                    put("mes_anio", cleanMes)
                    put("mesAnio", cleanMes)
                    put("archivo_nombre", fileName)
                    put("fileName", fileName)
                    put("filename", fileName)
                    put("archivo_base64", csvBase64)
                    put("pdfBase64", csvBase64)
                    put("base64", csvBase64)
                    put("fileData", csvBase64)
                    put("mimeType", "text/csv")
                }

                try {
                    var response = GananciasApiService.executeHttpPost(effectiveDriveUrl, payload)
                    Log.d(TAG, "Respuesta guardar_cierre_drive: $response")
                    driveResponseText = response

                    // Si el Google Apps Script solo acepta la acción estándar guardar_nota_drive como en facturas:
                    val json = try { JSONObject(response) } catch (e: Exception) { null }
                    if (json != null && json.has("ok") && !json.optBoolean("ok", true)) {
                        payload.put("accion", "guardar_nota_drive")
                        payload.put("action", "guardar_nota_drive")
                        response = GananciasApiService.executeHttpPost(effectiveDriveUrl, payload)
                        Log.d(TAG, "Fallback respuesta guardar_nota_drive para cierre: $response")
                        driveResponseText = response
                    }

                    // Extraer ID si viene en la respuesta
                    val resJson = try { JSONObject(response) } catch (e: Exception) { null }
                    driveFileId = resJson?.optString("fileId")?.ifBlank { null }
                        ?: resJson?.optString("id")?.ifBlank { null }

                    driveUploadSuccess = true
                } catch (e: Exception) {
                    uploadErrorMsg = e.message ?: "Error de red al conectar con Google Drive"
                    Log.e(TAG, "Error subiendo cierre a Google Drive: $uploadErrorMsg", e)
                }
            } else {
                uploadErrorMsg = "URL de Google Drive no configurada"
            }

            val cierreInfo = CierreMensualInfo(
                id = cleanMes,
                mes = cleanMes,
                mesKey = formattedMesKey,
                totalUsd = totalVentasUsd,
                totalBs = totalVentasBs,
                totalCostoUsd = totalCostoVentasUsd,
                gananciaNetaUsd = gananciaNetaUsd,
                margenPorcentaje = margenVentas,
                usuariosCount = params.usuariosGanancias.size,
                totalItemsInventario = totalItemsInventario,
                unidadesStockTotal = unidadesStockTotal,
                valorInventarioCostoUsd = valorInventarioCostoUsd,
                valorInventarioVentaUsd = valorInventarioVentaUsd,
                cerradoEn = System.currentTimeMillis(),
                cerradoPorEmail = params.cerradoPorEmail,
                cerradoPorNombre = params.cerradoPorNombre,
                archivoDriveNombre = fileName,
                archivoDriveId = driveFileId ?: "",
                driveUrl = effectiveDriveUrl
            )

            val finalResult = MonthlyClosingResult(
                isSuccess = driveUploadSuccess,
                mes = cleanMes,
                mesKey = formattedMesKey,
                fileName = fileName,
                driveUploadSuccess = driveUploadSuccess,
                driveFileId = driveFileId,
                driveResponse = driveResponseText,
                localFilePath = localFilePath,
                totalVentasUsd = totalVentasUsd,
                totalVentasBs = totalVentasBs,
                totalCostoVentasUsd = totalCostoVentasUsd,
                gananciaNetaUsd = gananciaNetaUsd,
                margenVentasPorcentaje = margenVentas,
                transaccionesCount = nonRevertedSales.size,
                unidadesVendidasCount = totalUnidadesVendidas,
                totalProductosInventario = totalItemsInventario,
                unidadesStockTotal = unidadesStockTotal,
                valorInventarioCostoUsd = valorInventarioCostoUsd,
                valorInventarioVentaUsd = valorInventarioVentaUsd,
                gananciaProyectadaInventarioUsd = gananciaProyectadaInventarioUsd,
                productosStockCritico = productosStockCritico,
                cierreInfo = cierreInfo,
                csvContent = csvContent,
                errorMessage = uploadErrorMsg
            )

            if (!driveUploadSuccess) {
                Result.failure(Exception(uploadErrorMsg ?: "No se pudo subir el reporte consolidado a Google Drive"))
            } else {
                Result.success(finalResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error general en cierre mensual de inventario y ventas: ${e.message}", e)
            Result.failure(e)
        }
    }
}
