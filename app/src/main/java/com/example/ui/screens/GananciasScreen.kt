package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GananciasMes
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ProductSaleSummary(
    val productoNombre: String,
    val tipo: String = "producto",
    val cantidadTotal: Int = 0,
    val totalUsd: Double = 0.0,
    val totalCostoUsd: Double = 0.0,
    val gananciaNetaUsd: Double = 0.0,
    val costoUnitario: Double = 0.0,
    val precioUnitarioPromedio: Double = 0.0,
    val margenPorcentaje: Double = 0.0,
    val vendedores: List<String> = emptyList()
)

data class SellerDetailedBreakdown(
    val usuario: String,
    val ventas: Int,
    val unidades: Int,
    val totalUsd: Double,
    val totalBs: Double,
    val totalCostoUsd: Double = 0.0,
    val gananciaNetaUsd: Double = 0.0,
    val margenPorcentaje: Double = 0.0,
    val productosVendidos: List<ProductSaleSummary>,
    val sales: List<Sale>
)

@Composable
fun GananciasScreen(
    gananciasActuales: GananciasMes?,
    historialMeses: List<String>,
    gananciasMesArchivado: GananciasMes?,
    selectedArchivedMonth: String?,
    salesHistory: List<Sale> = emptyList(),
    exchangeRate: Double = 1.0,
    isLoading: Boolean,
    isClosingMonth: Boolean = false,
    onRefresh: () -> Unit,
    onSelectArchivedMonth: (String) -> Unit,
    onClearSelectedArchivedMonth: () -> Unit,
    onCerrarMes: () -> Unit = {}
) {
    val context = LocalContext.current

    // Top Tabs: 0 -> "Este mes", 1 -> "Historial"
    var activeSubTab by remember { mutableIntStateOf(0) }

    // Section view mode: 0 -> "Por Vendedor", 1 -> "Productos Vendidos", 2 -> "Tickets de Venta"
    var viewMode by remember { mutableIntStateOf(0) }
    var productSearchQuery by remember { mutableStateOf("") }
    var sortByUnits by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showCerrarMesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    // Filter sales for the active period
    val activeMonthSales = remember(activeSubTab, selectedArchivedMonth, salesHistory) {
        val nonReverted = salesHistory.filter { !it.esReversado }
        if (activeSubTab == 0) {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH) // 0-indexed
            nonReverted.filter { sale ->
                if (sale.timestamp <= 0L) true else {
                    val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                    cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                }
            }
        } else if (selectedArchivedMonth != null) {
            val clean = selectedArchivedMonth.removePrefix("Ventas_").removePrefix("ventas_").trim()
            val parts = clean.split("-")
            if (parts.size == 2) {
                val targetYear = parts[0].toIntOrNull() ?: 0
                val targetMonth = (parts[1].toIntOrNull() ?: 1) - 1
                nonReverted.filter { sale ->
                    val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                    cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // Aggregate products sold in this period ("Lo vendido")
    val aggregatedProductsSold = remember(activeMonthSales) {
        val productMap = mutableMapOf<String, Triple<SaleItem, MutableList<String>, Double>>() // item, sellers, totalCost
        for (sale in activeMonthSales) {
            val seller = sale.usuario.ifBlank { sale.usuarioEmail }.ifBlank { "Operador" }
            for (item in sale.items) {
                val key = item.producto.ifBlank { "Producto" }.trim()
                val existing = productMap[key]
                val itemCost = item.costoUnitario * item.cantidad
                val itemSale = item.precioUsd * item.cantidad
                if (existing != null) {
                    val updatedItem = existing.first.copy(
                        cantidad = existing.first.cantidad + item.cantidad,
                        precioUsd = existing.first.precioUsd + itemSale,
                        precioCompra = if (existing.first.precioCompra > 0) existing.first.precioCompra else item.costoUnitario
                    )
                    existing.second.add(seller)
                    productMap[key] = Triple(updatedItem, existing.second, existing.third + itemCost)
                } else {
                    val initialItem = item.copy(
                        precioUsd = itemSale
                    )
                    productMap[key] = Triple(initialItem, mutableListOf(seller), itemCost)
                }
            }
        }

        productMap.map { (nombre, triple) ->
            val totalVenta = triple.first.precioUsd
            val totalCosto = triple.third
            val cant = triple.first.cantidad
            val ganancia = (totalVenta - totalCosto).coerceAtLeast(0.0)
            val margen = if (totalVenta > 0) (ganancia / totalVenta) * 100.0 else 0.0
            val precioUnit = if (cant > 0) totalVenta / cant else 0.0
            val costoUnit = if (cant > 0) totalCosto / cant else 0.0

            ProductSaleSummary(
                productoNombre = nombre,
                tipo = triple.first.tipo,
                cantidadTotal = cant,
                totalUsd = totalVenta,
                totalCostoUsd = totalCosto,
                gananciaNetaUsd = ganancia,
                costoUnitario = costoUnit,
                precioUnitarioPromedio = precioUnit,
                margenPorcentaje = margen,
                vendedores = triple.second.distinct()
            )
        }
    }

    // Detailed sellers with their sold products ("Quién lo vendió")
    val sellersDetailedList = remember(activeMonthSales, gananciasActuales, gananciasMesArchivado, activeSubTab, exchangeRate) {
        val currentRate = exchangeRate.coerceAtLeast(1.0)
        val grouped = activeMonthSales.groupBy { it.usuario.ifBlank { it.usuarioEmail }.ifBlank { "Operador" } }
        
        val detailedFromSales = grouped.map { (sellerName, sales) ->
            val totalVentas = sales.size
            val totalUnidades = sales.sumOf { sale ->
                if (sale.items.isNotEmpty()) sale.items.sumOf { it.cantidad } else 1
            }
            val totalUsd = sales.sumOf { it.totalUsd }
            val totalBs = sales.sumOf {
                if (it.totalBs > 0) it.totalBs else it.totalUsd * (if (it.tasaBcv > 0) it.tasaBcv else currentRate)
            }
            val totalCostoUsd = sales.sumOf { it.costoTotalUsd }
            val gananciaNetaUsd = (totalUsd - totalCostoUsd).coerceAtLeast(0.0)
            val margenPorcentaje = if (totalUsd > 0) (gananciaNetaUsd / totalUsd) * 100.0 else 0.0

            // Products sold by this seller
            val sellerProdMap = mutableMapOf<String, Triple<SaleItem, Int, Double>>()
            for (sale in sales) {
                for (item in sale.items) {
                    val key = item.producto.ifBlank { "Producto" }.trim()
                    val existing = sellerProdMap[key]
                    val itemCost = item.costoUnitario * item.cantidad
                    val itemSale = item.precioUsd * item.cantidad
                    if (existing != null) {
                        val newQty = existing.second + item.cantidad
                        val newTotalUsd = existing.first.precioUsd + itemSale
                        val newTotalCost = existing.third + itemCost
                        sellerProdMap[key] = Triple(existing.first.copy(precioUsd = newTotalUsd), newQty, newTotalCost)
                    } else {
                        sellerProdMap[key] = Triple(item.copy(precioUsd = itemSale), item.cantidad, itemCost)
                    }
                }
            }

            val prodList = sellerProdMap.map { (nombre, triple) ->
                val vTotal = triple.first.precioUsd
                val cTotal = triple.third
                val q = triple.second
                val gNeta = (vTotal - cTotal).coerceAtLeast(0.0)
                val mPct = if (vTotal > 0) (gNeta / vTotal) * 100.0 else 0.0
                val pUnit = if (q > 0) vTotal / q else 0.0
                val cUnit = if (q > 0) cTotal / q else 0.0

                ProductSaleSummary(
                    productoNombre = nombre,
                    tipo = triple.first.tipo,
                    cantidadTotal = q,
                    totalUsd = vTotal,
                    totalCostoUsd = cTotal,
                    gananciaNetaUsd = gNeta,
                    costoUnitario = cUnit,
                    precioUnitarioPromedio = pUnit,
                    margenPorcentaje = mPct,
                    vendedores = listOf(sellerName)
                )
            }.sortedByDescending { it.totalUsd }

            SellerDetailedBreakdown(
                usuario = sellerName,
                ventas = totalVentas,
                unidades = totalUnidades,
                totalUsd = totalUsd,
                totalBs = totalBs,
                totalCostoUsd = totalCostoUsd,
                gananciaNetaUsd = gananciaNetaUsd,
                margenPorcentaje = margenPorcentaje,
                productosVendidos = prodList,
                sales = sales.sortedByDescending { it.timestamp }
            )
        }.sortedByDescending { it.totalUsd }

        if (detailedFromSales.isNotEmpty()) {
            detailedFromSales
        } else {
            // Fallback to remote metadata if local list is empty
            val remoteGanancias = if (activeSubTab == 0) gananciasActuales else gananciasMesArchivado
            remoteGanancias?.usuarios?.map { u ->
                SellerDetailedBreakdown(
                    usuario = u.usuario,
                    ventas = u.ventas,
                    unidades = u.unidades,
                    totalUsd = u.totalUsd,
                    totalBs = u.totalBs,
                    totalCostoUsd = u.totalCostoUsd,
                    gananciaNetaUsd = u.gananciaNetaUsd,
                    margenPorcentaje = u.margenPorcentaje,
                    productosVendidos = emptyList(),
                    sales = emptyList()
                )
            } ?: emptyList()
        }
    }

    val totalMonthUsd = remember(sellersDetailedList, gananciasActuales, gananciasMesArchivado, activeSubTab) {
        val sumFromDetailed = sellersDetailedList.sumOf { it.totalUsd }
        if (sumFromDetailed > 0.0) sumFromDetailed else {
            val remote = if (activeSubTab == 0) gananciasActuales else gananciasMesArchivado
            remote?.totalUsd ?: 0.0
        }
    }

    val totalMonthBs = remember(sellersDetailedList, gananciasActuales, gananciasMesArchivado, activeSubTab, exchangeRate) {
        val sumFromDetailed = sellersDetailedList.sumOf { it.totalBs }
        if (sumFromDetailed > 0.0) sumFromDetailed else {
            val remote = if (activeSubTab == 0) gananciasActuales else gananciasMesArchivado
            if ((remote?.totalBs ?: 0.0) > 0) remote?.totalBs ?: 0.0 else totalMonthUsd * exchangeRate
        }
    }

    val totalMonthCostoUsd = remember(sellersDetailedList, activeMonthSales, gananciasActuales, gananciasMesArchivado, activeSubTab) {
        val sumFromSales = activeMonthSales.sumOf { it.costoTotalUsd }
        if (sumFromSales > 0.0) sumFromSales else {
            val sumFromDetailed = sellersDetailedList.sumOf { it.totalCostoUsd }
            if (sumFromDetailed > 0.0) sumFromDetailed else {
                val remote = if (activeSubTab == 0) gananciasActuales else gananciasMesArchivado
                remote?.totalCostoUsd ?: 0.0
            }
        }
    }

    val totalMonthGananciaUsd = remember(totalMonthUsd, totalMonthCostoUsd) {
        (totalMonthUsd - totalMonthCostoUsd).coerceAtLeast(0.0)
    }

    val totalMonthMargenPorcentaje = remember(totalMonthUsd, totalMonthGananciaUsd) {
        if (totalMonthUsd > 0.0) (totalMonthGananciaUsd / totalMonthUsd) * 100.0 else 0.0
    }

    // Top Selling Products (Sorted by Units or Revenue)
    val topSellingProducts = remember(aggregatedProductsSold) {
        aggregatedProductsSold.sortedByDescending { it.cantidadTotal }.take(5)
    }

    val sortedAndFilteredProducts = remember(aggregatedProductsSold, productSearchQuery, sortByUnits) {
        val filtered = if (productSearchQuery.isBlank()) {
            aggregatedProductsSold
        } else {
            aggregatedProductsSold.filter {
                it.productoNombre.contains(productSearchQuery.trim(), ignoreCase = true) ||
                it.vendedores.any { v -> v.contains(productSearchQuery.trim(), ignoreCase = true) }
            }
        }
        if (sortByUnits) {
            filtered.sortedByDescending { it.cantidadTotal }
        } else {
            filtered.sortedByDescending { it.totalUsd }
        }
    }

    val activePeriodLabel = if (activeSubTab == 0) {
        formatMonthName(gananciasActuales?.mes ?: "")
    } else {
        formatMonthName(selectedArchivedMonth ?: "Historial")
    }

    // Export Dialog
    if (showExportDialog) {
        ExportGananciasDialog(
            periodo = activePeriodLabel,
            totalUsd = totalMonthUsd,
            totalBs = totalMonthBs,
            totalCostoUsd = totalMonthCostoUsd,
            gananciaNetaUsd = totalMonthGananciaUsd,
            margenPorcentaje = totalMonthMargenPorcentaje,
            sellers = sellersDetailedList,
            productsSold = aggregatedProductsSold,
            sales = activeMonthSales,
            exchangeRate = exchangeRate,
            onDismiss = { showExportDialog = false },
            onShare = { formattedReport ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de Ganancias - $activePeriodLabel")
                    putExtra(Intent.EXTRA_TEXT, formattedReport)
                }
                context.startActivity(Intent.createChooser(intent, "Exportar reporte vía"))
                showExportDialog = false
            },
            onCopyToClipboard = { formattedReport ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Reporte Ganancias", formattedReport)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Reporte copiado al portapapeles", Toast.LENGTH_SHORT).show()
                showExportDialog = false
            }
        )
    }

    // Confirmation Dialog for Cerrar Mes
    if (showCerrarMesDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClosingMonth) showCerrarMesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar mes y respaldar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta acción realizará el cierre del periodo actual ($activePeriodLabel):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Genera el reporte CSV con el resumen de ganancias y el detalle de ventas.\n" +
                               "• Sube el archivo automáticamente a tu Google Drive (carpeta 'Termicoud - Cierres Mensuales').\n" +
                               "• Guarda un resumen permanente en el historial del sistema.\n" +
                               "• Borra las ventas del mes para iniciar en limpio el nuevo periodo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Esta acción no se puede deshacer. ¿Deseas continuar?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCerrarMesDialog = false
                        onCerrarMes()
                    },
                    enabled = !isClosingMonth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = GraphiteSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirmar_cerrar_mes")
                ) {
                    Text("Cerrar mes y respaldar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCerrarMesDialog = false },
                    enabled = !isClosingMonth,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.testTag("btn_cancelar_cerrar_mes")
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = GraphiteSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Main Header with Export and Refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GANANCIAS Y VENTAS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Control de ventas y productos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GraphiteSurface,
                            contentColor = ElectricLime
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                            .testTag("btn_export_ganancias")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportar",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Exportar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                            .testTag("btn_refresh_ganancias")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ElectricLime,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar ganancias",
                                tint = ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Sub-Tabs: "Este mes" vs "Historial"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                // Tab 0: Este mes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 0) ElectricLime else Color.Transparent)
                        .clickable {
                            activeSubTab = 0
                            onClearSelectedArchivedMonth()
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_ganancias_este_mes"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Este mes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSubTab == 0) Color.Black else TextSecondary
                    )
                }

                // Tab 1: Historial
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 1) ElectricLime else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 10.dp)
                        .testTag("tab_ganancias_historial"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Historial",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSubTab == 1) Color.Black else TextSecondary
                    )
                }
            }
        }

        // CONTENT ROUTING
        if (activeSubTab == 0) {
            // ==================== ESTE MES ====================
            val rawMes = gananciasActuales?.mes ?: ""
            val displayMonth = formatMonthName(rawMes)

            renderGananciasItems(
                displayMonth = displayMonth,
                isArchived = false,
                totalUsd = totalMonthUsd,
                totalBs = totalMonthBs,
                totalCostoUsd = totalMonthCostoUsd,
                gananciaNetaUsd = totalMonthGananciaUsd,
                margenPorcentaje = totalMonthMargenPorcentaje,
                sellers = sellersDetailedList,
                productsSold = sortedAndFilteredProducts,
                topSellingProducts = topSellingProducts,
                sales = activeMonthSales,
                isClosingMonth = isClosingMonth,
                canCerrarMes = totalMonthUsd > 0.0 || activeMonthSales.isNotEmpty() || (gananciasActuales?.usuarios?.isNotEmpty() == true),
                onOpenCerrarMesDialog = { showCerrarMesDialog = true },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                searchQuery = productSearchQuery,
                onSearchQueryChange = { productSearchQuery = it },
                sortByUnits = sortByUnits,
                onToggleSortByUnits = { sortByUnits = !sortByUnits }
            )
        } else {
            // ==================== HISTORIAL ====================
            if (selectedArchivedMonth != null) {
                // Detailed view of selected archived month
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onClearSelectedArchivedMonth,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GraphiteSurface,
                                contentColor = ElectricLime
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                                .testTag("btn_back_historial_list")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Volver al Historial", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Read-only Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningAmber.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mes archivado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                renderGananciasItems(
                    displayMonth = formatMonthName(selectedArchivedMonth),
                    isArchived = true,
                    totalUsd = totalMonthUsd,
                    totalBs = totalMonthBs,
                    totalCostoUsd = totalMonthCostoUsd,
                    gananciaNetaUsd = totalMonthGananciaUsd,
                    margenPorcentaje = totalMonthMargenPorcentaje,
                    sellers = sellersDetailedList,
                    productsSold = sortedAndFilteredProducts,
                    topSellingProducts = topSellingProducts,
                    sales = activeMonthSales,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    searchQuery = productSearchQuery,
                    onSearchQueryChange = { productSearchQuery = it },
                    sortByUnits = sortByUnits,
                    onToggleSortByUnits = { sortByUnits = !sortByUnits }
                )
            } else {
                // List of archived months
                item {
                    Text(
                        text = "MESES ANTERIORES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isLoading && historialMeses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricLime)
                        }
                    }
                } else if (historialMeses.isEmpty()) {
                    item {
                        EmptyGananciasCard(
                            icon = Icons.Default.History,
                            title = "Sin meses archivados",
                            message = "No se encontraron periodos mensuales cerrados en el historial. Los meses anteriores archivados aparecerán aquí para su consulta permanente."
                        )
                    }
                } else {
                    items(historialMeses) { mesKey ->
                        ArchivedMonthCard(
                            mesKey = mesKey,
                            onClick = { onSelectArchivedMonth(mesKey) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

/**
 * Common content builder for the LazyColumn
 */
private fun LazyListScope.renderGananciasItems(
    displayMonth: String,
    isArchived: Boolean,
    totalUsd: Double,
    totalBs: Double,
    totalCostoUsd: Double,
    gananciaNetaUsd: Double,
    margenPorcentaje: Double,
    sellers: List<SellerDetailedBreakdown>,
    productsSold: List<ProductSaleSummary>,
    topSellingProducts: List<ProductSaleSummary>,
    sales: List<Sale>,
    isClosingMonth: Boolean = false,
    canCerrarMes: Boolean = false,
    onOpenCerrarMesDialog: () -> Unit = {},
    viewMode: Int,
    onViewModeChange: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortByUnits: Boolean,
    onToggleSortByUnits: () -> Unit
) {
    // Month Indicator Badge
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isArchived) WarningAmber else ElectricLime)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArchived) "PERIODO CERRADO: $displayMonth" else "PERIODO EN CURSO: $displayMonth",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isArchived) WarningAmber else ElectricLime,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (!isArchived) {
                Text(
                    text = "Se reinicia el 1ro de cada mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }

    // Action Card for Cerrar Mes (Only in current month when there are sales)
    if (!isArchived && canCerrarMes) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("card_cerrar_mes"),
                color = GraphiteSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricLime.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cierre Mensual",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Respaldar reporte en Drive y limpiar mes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenCerrarMesDialog,
                        enabled = !isClosingMonth,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black,
                            disabledContainerColor = GraphiteSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_cerrar_mes")
                    ) {
                        if (isClosingMonth) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cerrando...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cerrar mes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Hero Total Card
    item {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, ElectricLime, RoundedCornerShape(12.dp))
                .testTag("ganancias_hero_card"),
            color = GraphiteSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArchived) "TOTAL FACTURADO EN EL MES" else "TOTAL VENTAS DEL MES",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLime,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isArchived) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GraphiteSurfaceVariant
                        ) {
                            Text(
                                text = "Solo lectura",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total USD Large Display
                Text(
                    text = String.format(Locale.US, "$%,.2f", totalUsd),
                    style = MonoDataLarge.copy(fontSize = 32.sp, color = ElectricLime),
                    modifier = Modifier.testTag("ganancias_hero_total_usd")
                )

                // Total Bs Subtitle
                Text(
                    text = String.format(Locale.US, "≈ Bs %,.2f", totalBs),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    ),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("ganancias_hero_total_bs")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mini Metrics Bar (Ventas, Unidades, Vendedores)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalVentasCount = if (sales.isNotEmpty()) sales.size else sellers.sumOf { it.ventas }
                    val totalUnidadesCount = if (productsSold.isNotEmpty()) productsSold.sumOf { it.cantidadTotal } else sellers.sumOf { it.unidades }

                    Column {
                        Text(
                            text = "Ventas",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$totalVentasCount",
                            style = MonoDataMedium.copy(color = TextPrimary, fontSize = 16.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(GraphiteBorder)
                    )

                    Column {
                        Text(
                            text = "Unidades",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$totalUnidadesCount",
                            style = MonoDataMedium.copy(color = TextPrimary, fontSize = 16.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(GraphiteBorder)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Operadores",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${sellers.size}",
                            style = MonoDataMedium.copy(color = ElectricLime, fontSize = 16.sp)
                        )
                    }
                }

                // Prominent Real Profit & Cost Breakdown Card
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElectricLime.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GANANCIA NETA REAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "Costo base: $%,.2f", totalCostoUsd),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "+$%,.2f", gananciaNetaUsd),
                                    style = MonoDataLarge.copy(
                                        fontSize = 20.sp,
                                        color = ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = String.format(Locale.US, "Margen: %.1f%%", margenPorcentaje),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricLime.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Top 3 Más Vendidos Podium Highlight Card
    if (topSellingProducts.isNotEmpty()) {
        item {
            TopSellingPodiumCard(topProducts = topSellingProducts, totalMonthUsd = totalUsd)
        }
    }

    // View Selector (Por Vendedor / Por Producto / Tickets)
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GraphiteSurface)
                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Option 0: Por Vendedor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewMode == 0) ElectricLime else Color.Transparent)
                    .clickable { onViewModeChange(0) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (viewMode == 0) Color.Black else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vendedores",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == 0) Color.Black else TextSecondary
                    )
                }
            }

            // Option 1: Por Producto
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewMode == 1) ElectricLime else Color.Transparent)
                    .clickable { onViewModeChange(1) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (viewMode == 1) Color.Black else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lo Vendido",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == 1) Color.Black else TextSecondary
                    )
                }
            }

            // Option 2: Tickets
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewMode == 2) ElectricLime else Color.Transparent)
                    .clickable { onViewModeChange(2) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = if (viewMode == 2) Color.Black else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tickets (${sales.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (viewMode == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == 2) Color.Black else TextSecondary
                    )
                }
            }
        }
    }

    // VIEW MODE 0: POR VENDEDOR (Quién vendió)
    if (viewMode == 0) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VENTAS POR VENDEDOR (${sellers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Toca para ver desglose y ganancia",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (sellers.isEmpty() || (totalUsd <= 0.0 && sellers.all { it.totalUsd <= 0.0 && it.ventas <= 0 })) {
            item {
                EmptyGananciasCard(
                    icon = Icons.Default.PointOfSale,
                    title = if (isArchived) "Sin ventas en este mes archivado" else "Sin ventas registradas este mes",
                    message = if (isArchived) {
                        "No se registraron transacciones de venta durante el periodo $displayMonth."
                    } else {
                        "Aún no hay ventas registradas en el mes en curso. Cuando los operadores completen ventas desde el catálogo, su recaudación, costos y ganancias reales aparecerán clasificados aquí."
                    }
                )
            }
        } else {
            itemsIndexed(sellers) { index, seller ->
                SellerPerformanceDetailedCard(
                    rank = index + 1,
                    seller = seller,
                    monthTotalUsd = totalUsd
                )
            }
        }
    }

    // VIEW MODE 1: PRODUCTOS VENDIDOS (Qué se vendió)
    if (viewMode == 1) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_search_sold_products"),
                    placeholder = { Text("Buscar producto vendido...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = GraphiteBorder,
                        focusedContainerColor = GraphiteSurface,
                        unfocusedContainerColor = GraphiteSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Toggle Sort Button
                Button(
                    onClick = onToggleSortByUnits,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sortByUnits) ElectricLime else GraphiteSurface,
                        contentColor = if (sortByUnits) Color.Black else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (sortByUnits) "Por Unidades" else "Por Total $",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATÁLOGO DE PRODUCTOS VENDIDOS (${productsSold.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Total unidades: ${productsSold.sumOf { it.cantidadTotal }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (productsSold.isEmpty()) {
            item {
                EmptyGananciasCard(
                    icon = Icons.Default.Inventory2,
                    title = "Sin productos vendidos",
                    message = if (searchQuery.isNotBlank()) "No se encontraron productos que coincidan con '$searchQuery'." else "No hay registro de artículos despachados en este periodo."
                )
            }
        } else {
            itemsIndexed(productsSold) { index, itemSummary ->
                ProductSoldCard(
                    summary = itemSummary,
                    rank = index + 1,
                    monthTotalUsd = totalUsd
                )
            }
        }
    }

    // VIEW MODE 2: TICKETS DE VENTA (Registro de transacciones)
    if (viewMode == 2) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TICKETS REGISTRADOS (${sales.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (sales.isEmpty()) {
            item {
                EmptyGananciasCard(
                    icon = Icons.Default.Receipt,
                    title = "Sin tickets de venta",
                    message = "No hay transacciones registradas para este periodo."
                )
            }
        } else {
            items(sales) { sale ->
                SaleTicketCompactCard(sale = sale)
            }
        }
    }
}

@Composable
fun TopSellingPodiumCard(
    topProducts: List<ProductSaleSummary>,
    totalMonthUsd: Double
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
        color = GraphiteSurface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LO QUE MÁS SE VENDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Top ${topProducts.size} Artículos",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            topProducts.forEachIndexed { index, prod ->
                val badgeColor = when (index) {
                    0 -> WarningAmber
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> ElectricLime
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = badgeColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = prod.productoNombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${prod.cantidadTotal} unidades vendidas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f", prod.totalUsd),
                            style = MonoDataMedium.copy(color = ElectricLime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        if (totalMonthUsd > 0.0) {
                            val percent = (prod.totalUsd / totalMonthUsd * 100).toInt()
                            Text(
                                text = "$percent% del total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerPerformanceDetailedCard(
    rank: Int,
    seller: SellerDetailedBreakdown,
    monthTotalUsd: Double
) {
    var isExpanded by remember { mutableStateOf(false) }

    val shareRatio = if (monthTotalUsd > 0.0) {
        (seller.totalUsd / monthTotalUsd).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = shareRatio,
        label = "seller_progress"
    )
    val percentage = (shareRatio * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (rank == 1) ElectricLime.copy(alpha = 0.6f) else GraphiteBorder, RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("seller_card_${seller.usuario}"),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val rankBg = when (rank) {
                        1 -> ElectricLime
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> GraphiteSurfaceVariant
                    }
                    val rankColor = when (rank) {
                        1, 2, 3 -> Color.Black
                        else -> TextSecondary
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(rankBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = seller.usuario.ifBlank { "Operador" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "${seller.ventas} ventas · ${seller.unidades} unidades vendidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Total USD & Bs & Profit
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", seller.totalUsd),
                        style = MonoDataMedium.copy(
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    if (seller.gananciaNetaUsd > 0.0) {
                        Text(
                            text = String.format(Locale.US, "Ganancia: +$%,.2f", seller.gananciaNetaUsd),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricLime.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    } else if (seller.totalBs > 0) {
                        Text(
                            text = String.format(Locale.US, "Bs %,.2f", seller.totalBs),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thin Horizontal Progress Bar showing proportional share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(GraphiteSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (rank == 1) ElectricLime else ElectricLime.copy(alpha = 0.75f))
                    )
                }

                Text(
                    text = "$percentage% del total",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rank == 1) ElectricLime else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // EXPANDED SECTION: Products sold and cost/profit breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Operator metrics banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GraphiteSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "COSTO DE MERCANCÍA:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", seller.totalCostoUsd),
                                    style = MonoDataSmall.copy(color = TextSecondary, fontSize = 13.sp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "GANANCIA NETA:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "+$%,.2f", seller.gananciaNetaUsd),
                                    style = MonoDataSmall.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "MARGEN:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", seller.margenPorcentaje),
                                    style = MonoDataSmall.copy(color = TextPrimary, fontSize = 13.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GraphiteBorder)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ARTÍCULOS VENDIDOS POR ESTE OPERADOR:",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLime,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (seller.productosVendidos.isEmpty()) {
                        Text(
                            text = "No hay desglose individual de artículos registrado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        seller.productosVendidos.forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (prod.tipo == "combo") ElectricLime.copy(alpha = 0.15f) else GraphiteSurfaceVariant
                                    ) {
                                        Text(
                                            text = "${prod.cantidadTotal}x",
                                            color = if (prod.tipo == "combo") ElectricLime else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = prod.productoNombre,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (prod.gananciaNetaUsd > 0.0) {
                                            Text(
                                                text = String.format(Locale.US, "Ganancia: +$%,.2f (%.0f%%)", prod.gananciaNetaUsd, prod.margenPorcentaje),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricLime.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "$%,.2f", prod.totalUsd),
                                        style = MonoDataSmall.copy(color = ElectricLime, fontWeight = FontWeight.SemiBold)
                                    )
                                    if (prod.totalCostoUsd > 0.0) {
                                        Text(
                                            text = String.format(Locale.US, "Costo: $%,.2f", prod.totalCostoUsd),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (seller.sales.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ÚLTIMOS TICKETS (${seller.sales.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
                        seller.sales.take(4).forEach { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sdf.format(Date(sale.timestamp)) + if (sale.clienteNombre.isNotBlank()) " · ${sale.clienteNombre}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", sale.totalUsd),
                                    style = MonoDataSmall.copy(color = TextPrimary, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductSoldCard(
    summary: ProductSaleSummary,
    rank: Int = 0,
    monthTotalUsd: Double
) {
    val share = if (monthTotalUsd > 0.0) (summary.totalUsd / monthTotalUsd).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (summary.tipo == "combo") ElectricLime.copy(alpha = 0.15f) else GraphiteSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (summary.tipo == "combo") Icons.Default.Layers else Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (summary.tipo == "combo") ElectricLime else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (rank > 0) {
                                Text(
                                    text = "#$rank ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricLime
                                )
                            }
                            Text(
                                text = summary.productoNombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${summary.cantidadTotal} unidades vendidas" + if (summary.vendedores.isNotEmpty()) " · Por: ${summary.vendedores.joinToString(", ")}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", summary.totalUsd),
                        style = MonoDataMedium.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%% del mes", share * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Real Cost and Profit Breakdown for this product
            if (summary.totalCostoUsd > 0.0 || summary.gananciaNetaUsd > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GraphiteSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "Costo base: $%,.2f (Unit: $%,.2f)", summary.totalCostoUsd, summary.costoUnitario),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = String.format(Locale.US, "Ganancia: +$%,.2f (%.1f%%)", summary.gananciaNetaUsd, summary.margenPorcentaje),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaleTicketCompactCard(
    sale: Sale
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())
    val seller = sale.usuario.ifBlank { sale.usuarioEmail }.ifBlank { "Operador" }
    val costoTotal = sale.costoTotalUsd
    val gananciaNeta = (sale.totalUsd - costoTotal).coerceAtLeast(0.0)
    val margen = if (sale.totalUsd > 0) (gananciaNeta / sale.totalUsd) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricLime.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (sale.folio.isNotBlank()) "#${sale.folio}" else "#${sale.id.takeLast(6).uppercase()}",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sdf.format(Date(sale.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", sale.totalUsd),
                        style = MonoDataMedium.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    if (gananciaNeta > 0.0) {
                        Text(
                            text = String.format(Locale.US, "+$%,.2f (%.0f%%)", gananciaNeta, margen),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricLime.copy(alpha = 0.9f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Seller and Customer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vendido por: $seller",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )

                if (sale.clienteNombre.isNotBlank()) {
                    Text(
                        text = "Cliente: ${sale.clienteNombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (sale.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val itemsSummary = sale.items.joinToString(", ") { "${it.cantidad}x ${it.producto.ifBlank { "Item" }}" }
                Text(
                    text = itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun ArchivedMonthCard(
    mesKey: String,
    onClick: () -> Unit
) {
    val formatted = formatMonthName(mesKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("month_item_$mesKey"),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Periodo archivado ($mesKey)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalle",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyGananciasCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
        color = GraphiteSurface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ExportGananciasDialog(
    periodo: String,
    totalUsd: Double,
    totalBs: Double,
    totalCostoUsd: Double = 0.0,
    gananciaNetaUsd: Double = 0.0,
    margenPorcentaje: Double = 0.0,
    sellers: List<SellerDetailedBreakdown>,
    productsSold: List<ProductSaleSummary>,
    sales: List<Sale>,
    exchangeRate: Double,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    val computedCostoUsd = remember(sales, sellers, totalCostoUsd) {
        if (totalCostoUsd > 0.0) totalCostoUsd
        else if (sales.isNotEmpty()) sales.sumOf { it.costoTotalUsd }
        else sellers.sumOf { it.totalCostoUsd }
    }
    val computedGananciaNetaUsd = remember(totalUsd, computedCostoUsd, gananciaNetaUsd) {
        if (gananciaNetaUsd > 0.0) gananciaNetaUsd else (totalUsd - computedCostoUsd).coerceAtLeast(0.0)
    }
    val computedMargenPorcentaje = remember(totalUsd, computedGananciaNetaUsd, margenPorcentaje) {
        if (margenPorcentaje > 0.0) margenPorcentaje
        else if (totalUsd > 0.0) (computedGananciaNetaUsd / totalUsd) * 100.0 else 0.0
    }

    val reportText = remember(periodo, totalUsd, totalBs, computedCostoUsd, computedGananciaNetaUsd, computedMargenPorcentaje, sellers, productsSold, sales) {
        buildString {
            appendLine("═════════════════════════════════════")
            appendLine(" 📊 REPORTE DE VENTAS Y GANANCIAS")
            appendLine(" Periodo: $periodo")
            appendLine("═════════════════════════════════════")
            appendLine("💰 Total Facturado (USD): $${String.format(Locale.US, "%,.2f", totalUsd)}")
            appendLine("🇻🇪 Total Estimado (Bs):  Bs ${String.format(Locale.US, "%,.2f", totalBs)}")
            appendLine("📦 Costo Base Total:     $${String.format(Locale.US, "%,.2f", computedCostoUsd)}")
            appendLine("✨ GANANCIA NETA REAL:   +$${String.format(Locale.US, "%,.2f", computedGananciaNetaUsd)} (${String.format(Locale.US, "%.1f", computedMargenPorcentaje)}%)")
            appendLine("🧾 Transacciones registradas: ${sales.size}")
            val totalUnits = if (productsSold.isNotEmpty()) productsSold.sumOf { it.cantidadTotal } else sellers.sumOf { it.unidades }
            appendLine("📦 Total artículos despachados: $totalUnits")
            appendLine()
            appendLine("─────────────────────────────────────")
            appendLine(" 👥 RENDIMIENTO POR OPERADOR:")
            appendLine("─────────────────────────────────────")
            if (sellers.isEmpty()) {
                appendLine(" (Sin ventas registradas)")
            } else {
                sellers.forEachIndexed { i, s ->
                    appendLine("${i + 1}. ${s.usuario.ifBlank { "Operador" }}")
                    appendLine("   • Ventas: ${s.ventas} | Unidades: ${s.unidades}")
                    appendLine("   • Total Venta: $${String.format(Locale.US, "%,.2f", s.totalUsd)} (Bs ${String.format(Locale.US, "%,.2f", s.totalBs)})")
                    appendLine("   • Costo Base:  $${String.format(Locale.US, "%,.2f", s.totalCostoUsd)}")
                    appendLine("   • Ganancia:    +$${String.format(Locale.US, "%,.2f", s.gananciaNetaUsd)} (${String.format(Locale.US, "%.1f", s.margenPorcentaje)}%)")
                    if (s.productosVendidos.isNotEmpty()) {
                        val topItems = s.productosVendidos.take(3).joinToString(", ") { "${it.cantidadTotal}x ${it.productoNombre}" }
                        appendLine("   • Artículos principales: $topItems")
                    }
                }
            }
            appendLine()
            appendLine("─────────────────────────────────────")
            appendLine(" 🔥 LO QUE MÁS SE VENDE (TOP PRODUCTOS):")
            appendLine("─────────────────────────────────────")
            val topList = productsSold.sortedByDescending { it.cantidadTotal }.take(10)
            if (topList.isEmpty()) {
                appendLine(" (Sin artículos registrados)")
            } else {
                topList.forEachIndexed { i, p ->
                    appendLine("${i + 1}. ${p.productoNombre}")
                    appendLine("   • Cantidad vendida: ${p.cantidadTotal} unid.")
                    appendLine("   • Venta Total: $${String.format(Locale.US, "%,.2f", p.totalUsd)}")
                    appendLine("   • Costo Base:  $${String.format(Locale.US, "%,.2f", p.totalCostoUsd)}")
                    appendLine("   • Ganancia:    +$${String.format(Locale.US, "%,.2f", p.gananciaNetaUsd)} (${String.format(Locale.US, "%.1f", p.margenPorcentaje)}%)")
                    if (p.vendedores.isNotEmpty()) {
                        appendLine("   • Vendido por: ${p.vendedores.joinToString(", ")}")
                    }
                }
            }
            appendLine()
            appendLine("═════════════════════════════════════")
            appendLine(" Generado por Sistema de Inventario")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exportar Reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Periodo: $periodo",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricLime,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El reporte incluye totales facturados, costos base, ganancias netas reales, rendimiento por operador y ranking de productos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
                    color = GraphiteSurfaceVariant
                ) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(reportText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricLime,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onCopyToClipboard(reportText) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = GraphiteSurface,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Transforms keys like "Ventas_2026-07" or "2026-07" into "Julio 2026".
 */
fun formatMonthName(raw: String): String {
    if (raw.isBlank()) return "Mes actual"
    val clean = raw.removePrefix("Ventas_").removePrefix("ventas_").trim()
    val parts = clean.split("-")
    if (parts.size == 2) {
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: 0
        val monthName = when (monthNum) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> null
        }
        if (monthName != null) {
            return "$monthName $year"
        }
    }
    return if (clean.isNotBlank()) clean else "Mes actual"
}
