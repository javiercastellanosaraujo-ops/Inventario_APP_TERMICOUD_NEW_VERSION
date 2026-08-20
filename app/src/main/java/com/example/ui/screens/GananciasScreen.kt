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
    val vendedores: List<String> = emptyList()
)

data class SellerDetailedBreakdown(
    val usuario: String,
    val ventas: Int,
    val unidades: Int,
    val totalUsd: Double,
    val totalBs: Double,
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
    onRefresh: () -> Unit,
    onSelectArchivedMonth: (String) -> Unit,
    onClearSelectedArchivedMonth: () -> Unit
) {
    val context = LocalContext.current

    // Top Tabs: 0 -> "Este mes", 1 -> "Historial"
    var activeSubTab by remember { mutableIntStateOf(0) }

    // Section view mode: 0 -> "Por Vendedor", 1 -> "Productos Vendidos", 2 -> "Tickets de Venta"
    var viewMode by remember { mutableIntStateOf(0) }
    var productSearchQuery by remember { mutableStateOf("") }
    var sortByUnits by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }

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
        val productMap = mutableMapOf<String, Pair<SaleItem, MutableList<String>>>()
        for (sale in activeMonthSales) {
            val seller = sale.usuario.ifBlank { sale.usuarioEmail }.ifBlank { "Operador" }
            for (item in sale.items) {
                val key = item.producto.ifBlank { "Producto" }.trim()
                val existing = productMap[key]
                if (existing != null) {
                    val updatedItem = existing.first.copy(
                        cantidad = existing.first.cantidad + item.cantidad,
                        precioUsd = existing.first.precioUsd + (item.cantidad * item.precioUsd)
                    )
                    existing.second.add(seller)
                    productMap[key] = Pair(updatedItem, existing.second)
                } else {
                    val initialItem = item.copy(
                        precioUsd = item.cantidad * item.precioUsd
                    )
                    productMap[key] = Pair(initialItem, mutableListOf(seller))
                }
            }
        }

        productMap.map { (nombre, pair) ->
            ProductSaleSummary(
                productoNombre = nombre,
                tipo = pair.first.tipo,
                cantidadTotal = pair.first.cantidad,
                totalUsd = pair.first.precioUsd,
                vendedores = pair.second.distinct()
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

            // Products sold by this seller
            val sellerProdMap = mutableMapOf<String, Pair<SaleItem, Int>>()
            for (sale in sales) {
                for (item in sale.items) {
                    val key = item.producto.ifBlank { "Producto" }.trim()
                    val existing = sellerProdMap[key]
                    if (existing != null) {
                        val newQty = existing.second + item.cantidad
                        val newTotalUsd = existing.first.precioUsd + (item.cantidad * item.precioUsd)
                        sellerProdMap[key] = Pair(existing.first.copy(precioUsd = newTotalUsd), newQty)
                    } else {
                        sellerProdMap[key] = Pair(item.copy(precioUsd = item.cantidad * item.precioUsd), item.cantidad)
                    }
                }
            }

            val prodList = sellerProdMap.map { (nombre, pair) ->
                ProductSaleSummary(
                    productoNombre = nombre,
                    tipo = pair.first.tipo,
                    cantidadTotal = pair.second,
                    totalUsd = pair.first.precioUsd,
                    vendedores = listOf(sellerName)
                )
            }.sortedByDescending { it.totalUsd }

            SellerDetailedBreakdown(
                usuario = sellerName,
                ventas = totalVentas,
                unidades = totalUnidades,
                totalUsd = totalUsd,
                totalBs = totalBs,
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
    sellers: List<SellerDetailedBreakdown>,
    productsSold: List<ProductSaleSummary>,
    topSellingProducts: List<ProductSaleSummary>,
    sales: List<Sale>,
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
                        text = if (isArchived) "TOTAL RECAUDADO EN EL MES" else "TOTAL GENERADO EN EL MES",
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

                // Mini Metrics Bar
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
                            text = "Vendedores",
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

                // Cost and Profit Breakdown if costs are registered
                val totalCostoUsd = sales.sumOf { it.costoTotalUsd }
                if (totalCostoUsd > 0 && totalUsd > 0) {
                    val gananciaNeta = (totalUsd - totalCostoUsd).coerceAtLeast(0.0)
                    val margen = (gananciaNeta / totalUsd) * 100.0

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GraphiteSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ganancia Neta Estimada:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "Costo base: $%,.2f", totalCostoUsd),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "+$%,.2f", gananciaNeta),
                                    style = MonoDataSmall.copy(
                                        fontSize = 14.sp,
                                        color = ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = String.format(Locale.US, "Margen: %.1f%%", margen),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricLime.copy(alpha = 0.8f),
                                    fontSize = 10.sp
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
                    text = "Toca para ver detalle",
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
                        "Aún no hay ventas registradas en el mes en curso. Cuando los operadores completen ventas desde el catálogo, su recaudación y productos vendidos aparecerán clasificados aquí."
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

                // Total USD & Bs
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", seller.totalUsd),
                        style = MonoDataMedium.copy(
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    if (seller.totalBs > 0) {
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

            // EXPANDED SECTION: Products sold by this seller
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
                                    Text(
                                        text = prod.productoNombre,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "$%,.2f", prod.totalUsd),
                                    style = MonoDataSmall.copy(color = ElectricLime, fontWeight = FontWeight.SemiBold)
                                )
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
        }
    }
}

@Composable
fun SaleTicketCompactCard(
    sale: Sale
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())
    val seller = sale.usuario.ifBlank { sale.usuarioEmail }.ifBlank { "Operador" }

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

                Text(
                    text = String.format(Locale.US, "$%,.2f", sale.totalUsd),
                    style = MonoDataMedium.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
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
    sellers: List<SellerDetailedBreakdown>,
    productsSold: List<ProductSaleSummary>,
    sales: List<Sale>,
    exchangeRate: Double,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    val reportText = remember(periodo, totalUsd, totalBs, sellers, productsSold, sales) {
        buildString {
            appendLine("═════════════════════════════════════")
            appendLine(" 📊 REPORTE DE VENTAS Y GANANCIAS")
            appendLine(" Periodo: $periodo")
            appendLine("═════════════════════════════════════")
            appendLine("💰 Total Generado USD: $${String.format(Locale.US, "%,.2f", totalUsd)}")
            appendLine("🇻🇪 Total Estimado Bs: Bs ${String.format(Locale.US, "%,.2f", totalBs)}")
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
                    appendLine("   • Recaudado: $${String.format(Locale.US, "%,.2f", s.totalUsd)} (Bs ${String.format(Locale.US, "%,.2f", s.totalBs)})")
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
                    appendLine("   • Total generado: $${String.format(Locale.US, "%,.2f", p.totalUsd)}")
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
                    text = "El reporte incluye el total mensual, el rendimiento individual de tus operadores y el ranking de lo que más se vende en tu inventario.",
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
