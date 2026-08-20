package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.config.AppConfig
import com.example.data.model.Sale
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private const val TAG = "InvoicePdfGenerator"
    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points

    fun generateInvoicePdf(
        context: Context,
        sale: Sale,
        businessName: String = AppConfig.BUSINESS_NAME,
        rif: String = AppConfig.BUSINESS_RIF
    ): File? {
        val document = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawInvoiceContent(
                canvas = canvas,
                sale = sale,
                businessName = businessName,
                rif = rif
            )

            document.finishPage(page)

            // Save in App Documents folder
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(context.filesDir, "notas_entrega")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val fileName = "Nota_Entrega_${sale.folio.ifBlank { sale.id.take(8).uppercase() }}.pdf"
            val pdfFile = File(storageDir, fileName)

            FileOutputStream(pdfFile).use { outputStream ->
                document.writeTo(outputStream)
            }

            Log.d(TAG, "Nota de Entrega PDF guardada exitosamente: ${pdfFile.absolutePath}")
            return pdfFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generando PDF de Nota de Entrega: ${e.message}", e)
            return null
        } finally {
            document.close()
        }
    }

    private fun drawInvoiceContent(
        canvas: Canvas,
        sale: Sale,
        businessName: String,
        rif: String
    ) {
        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(30, 30, 30)
        }

        val paintAccent = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(20, 90, 50) // Forest green / dark lime
        }

        val paintGray = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(110, 110, 110)
        }

        val paintLine = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(210, 210, 210)
            strokeWidth = 1f
        }

        val paintTableBg = Paint().apply {
            color = Color.rgb(245, 247, 245)
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val fechaStr = dateFormat.format(Date(sale.timestamp))

        var currentY = 50f

        // --- 1. HEADER ---
        paintText.textSize = 20f
        paintText.isFakeBoldText = true
        canvas.drawText(businessName, 40f, currentY, paintText)

        paintText.textSize = 10f
        paintText.isFakeBoldText = false
        paintText.color = Color.rgb(80, 80, 80)
        currentY += 16f
        canvas.drawText("RIF: $rif • Control de Despacho y Entregas", 40f, currentY, paintText)

        // Folio badge on the right
        val folio = sale.folio.ifBlank { "NE-${sale.id.take(6).uppercase()}" }
        paintAccent.textSize = 14f
        paintAccent.isFakeBoldText = true
        val folioText = "NOTA DE ENTREGA: $folio"
        val folioWidth = paintAccent.measureText(folioText)
        canvas.drawText(folioText, PAGE_WIDTH - 40f - folioWidth, 50f, paintAccent)

        paintGray.textSize = 10f
        val fechaText = "Fecha: $fechaStr"
        val fechaWidth = paintGray.measureText(fechaText)
        canvas.drawText(fechaText, PAGE_WIDTH - 40f - fechaWidth, 68f, paintGray)

        currentY += 25f
        canvas.drawLine(40f, currentY, PAGE_WIDTH - 40f, currentY, paintLine)

        // --- 2. METADATA SECTION ---
        currentY += 20f
        paintText.textSize = 10f
        paintText.isFakeBoldText = true
        paintText.color = Color.rgb(40, 40, 40)
        canvas.drawText("DATOS DEL DESPACHO / ENTREGA", 40f, currentY, paintText)

        currentY += 15f
        paintText.isFakeBoldText = false

        // Left column
        val cliente = sale.clienteNombre.ifBlank { "Consumidor Final / Cliente General" }
        canvas.drawText("Cliente: $cliente", 40f, currentY, paintText)
        currentY += 14f
        val operador = if (sale.usuario.isNotBlank()) "${sale.usuario} (${sale.usuarioEmail})" else sale.usuarioEmail.ifBlank { "Operador" }
        canvas.drawText("Atendido por: $operador", 40f, currentY, paintText)

        // Right column (Tasa BCV)
        val tasaText = String.format(Locale.US, "Tasa BCV Aplicada: Bs %.2f / USD", sale.tasaBcv)
        val tasaWidth = paintText.measureText(tasaText)
        canvas.drawText(tasaText, PAGE_WIDTH - 40f - tasaWidth, currentY - 14f, paintText)

        currentY += 20f

        // --- 3. ITEMS TABLE HEADER ---
        val tableTop = currentY
        val tableBottom = tableTop + 24f
        canvas.drawRect(40f, tableTop, PAGE_WIDTH - 40f, tableBottom, paintTableBg)

        paintText.isFakeBoldText = true
        paintText.textSize = 10f
        paintText.color = Color.rgb(30, 30, 30)

        canvas.drawText("DESCRIPCIÓN / PRODUCTO", 48f, tableTop + 16f, paintText)
        canvas.drawText("CANT.", 330f, tableTop + 16f, paintText)
        canvas.drawText("PRECIO USD", 385f, tableTop + 16f, paintText)
        canvas.drawText("SUBTOTAL USD", 480f, tableTop + 16f, paintText)

        currentY = tableBottom + 16f
        paintText.isFakeBoldText = false

        // --- 4. ITEMS LIST ---
        for (item in sale.items) {
            val itemSubtotal = item.precioUsd * item.cantidad

            // Product name (trimmed if too long)
            var pName = item.producto
            if (paintText.measureText(pName) > 270f) {
                while (paintText.measureText("$pName...") > 270f && pName.length > 5) {
                    pName = pName.dropLast(1)
                }
                pName = "$pName..."
            }
            canvas.drawText(pName, 48f, currentY, paintText)

            // Cantidad
            canvas.drawText("${item.cantidad}", 340f, currentY, paintText)

            // Precio USD
            canvas.drawText(String.format(Locale.US, "$ %.2f", item.precioUsd), 395f, currentY, paintText)

            // Subtotal USD
            val subtotalStr = String.format(Locale.US, "$ %.2f", itemSubtotal)
            val subtotalWidth = paintText.measureText(subtotalStr)
            canvas.drawText(subtotalStr, PAGE_WIDTH - 48f - subtotalWidth, currentY, paintText)

            currentY += 18f
            canvas.drawLine(40f, currentY - 6f, PAGE_WIDTH - 40f, currentY - 6f, paintLine)
        }

        // --- 5. TOTALS SUMMARY BOX ---
        currentY += 15f
        val totalsLeft = 320f
        val totalsBoxTop = currentY
        val totalsBoxBottom = totalsBoxTop + 90f

        val totalsBg = Paint().apply {
            color = Color.rgb(240, 245, 240)
        }
        canvas.drawRoundRect(
            RectF(totalsLeft, totalsBoxTop, PAGE_WIDTH - 40f, totalsBoxBottom),
            8f,
            8f,
            totalsBg
        )

        val totalsBorder = Paint().apply {
            color = Color.rgb(180, 210, 180)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(
            RectF(totalsLeft, totalsBoxTop, PAGE_WIDTH - 40f, totalsBoxBottom),
            8f,
            8f,
            totalsBorder
        )

        paintText.isFakeBoldText = true
        paintText.textSize = 12f
        paintText.color = Color.rgb(20, 60, 20)

        // USD Total
        canvas.drawText("TOTAL USD:", totalsLeft + 16f, totalsBoxTop + 30f, paintText)
        val totalUsdStr = String.format(Locale.US, "$ %.2f", sale.totalUsd)
        val totalUsdWidth = paintText.measureText(totalUsdStr)
        canvas.drawText(totalUsdStr, PAGE_WIDTH - 56f - totalUsdWidth, totalsBoxTop + 30f, paintText)

        // Bs Total
        paintText.textSize = 13f
        canvas.drawText("TOTAL BS:", totalsLeft + 16f, totalsBoxTop + 65f, paintText)
        val totalBsStr = String.format(Locale.US, "Bs %.2f", sale.totalBs)
        val totalBsWidth = paintText.measureText(totalBsStr)
        canvas.drawText(totalBsStr, PAGE_WIDTH - 56f - totalBsWidth, totalsBoxTop + 65f, paintText)

        // --- 6. FOOTER ---
        val footerY = PAGE_HEIGHT - 60f
        canvas.drawLine(40f, footerY - 15f, PAGE_WIDTH - 40f, footerY - 15f, paintLine)

        paintGray.textSize = 9f
        paintGray.isFakeBoldText = false
        val footer1 = AppConfig.PDF_FOOTER_NOTE
        val footer2 = "Conserve este comprobante como respaldo de su compra."
        canvas.drawText(footer1, 40f, footerY, paintGray)
        canvas.drawText(footer2, 40f, footerY + 12f, paintGray)

        // --- 7. ANULADA WATERMARK (If reversed) ---
        if (sale.esReversado) {
            canvas.save()
            val paintWatermark = Paint().apply {
                isAntiAlias = true
                color = Color.argb(120, 220, 40, 40) // Translucent bold red
                textSize = 72f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            canvas.rotate(-35f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            canvas.drawText("ANULADA", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f - 20f, paintWatermark)

            paintWatermark.textSize = 22f
            val reversoInfo = "Reversado: ${if (sale.fechaReverso != null) dateFormat.format(Date(sale.fechaReverso)) else ""}"
            canvas.drawText(reversoInfo, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f + 25f, paintWatermark)

            canvas.restore()
        }
    }

    fun sharePdfFile(
        context: Context,
        pdfFile: File,
        recipientEmail: String = AppConfig.ADMIN_EMAIL
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Nota de Entrega ${AppConfig.APP_BRAND_NAME} - ${pdfFile.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "Adjunto comprobante digital de Nota de Entrega ${AppConfig.APP_BRAND_NAME} para el registro del Administrador.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Enviar Nota de Entrega a: $recipientEmail")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir PDF: ${e.message}", e)
        }
    }
}
