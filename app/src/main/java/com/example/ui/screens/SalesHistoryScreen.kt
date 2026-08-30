package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Movimiento
import com.example.data.model.Sale
import com.example.data.model.TipoMovimiento
import com.example.ui.components.MovementsListSkeleton
import com.example.ui.components.SalesHistorySkeleton
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.InvoicePdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesHistoryScreen(
    sales: List<Sale>,
    movimientos: List<Movimiento> = emptyList(),
    isLoading: Boolean = false,
    onBackClick: () -> Unit,
    onRevertSale: ((String) -> Unit)? = null,
    onRevertMovimiento: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ventas, 1: Movimientos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("btn_history_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(Icons.Default.History, contentDescription = null, tint = ElectricLime)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "HISTORIAL Y AUDITORÍA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Sub-tabs: Ventas / Movimientos de Stock
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GraphiteSurface,
            contentColor = ElectricLime,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricLime
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Ventas (${sales.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) ElectricLime else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_history_sales")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Movimientos (${movimientos.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) ElectricLime else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_history_movements")
            )
        }

        if (selectedTab == 0) {
            // TAB 0: VENTAS
            if (isLoading && sales.isEmpty()) {
                SalesHistorySkeleton(count = 5)
            } else if (sales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay ventas registradas aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Las ventas que registres aparecerán en este historial sincronizado con Firestore",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sales, key = { it.id }) { sale ->
                        val dateStr = dateFormat.format(Date(sale.timestamp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (sale.esReversado) AlertRed.copy(alpha = 0.5f) else GraphiteBorder,
                                    RoundedCornerShape(8.dp)
                                ),
                            color = GraphiteSurface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricLime, modifier = Modifier.padding(end = 4.dp))
                                        Column {
                                            Text(
                                                text = sale.usuario,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (sale.usuarioEmail.isNotBlank()) {
                                                Text(
                                                    text = sale.usuarioEmail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        if (sale.esReversado) {
                                            Text(
                                                text = "[VENTA ANULADA / REVERSADA]",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AlertRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Itemized summary
                                sale.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${item.cantidad}x ${item.producto}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$ %.2f", item.precioUsd * item.cantidad),
                                            style = MonoDataSmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Totals & Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, sale)
                                                if (pdfFile != null) {
                                                    InvoicePdfGenerator.sharePdfFile(context, pdfFile)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("btn_share_pdf_${sale.id}")
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Nota PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }

                                        if (!sale.esReversado && onRevertSale != null) {
                                            Button(
                                                onClick = { onRevertSale(sale.id) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AlertRed,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("btn_revert_sale_${sale.id}")
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reversar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "$ %.2f USD", sale.totalUsd),
                                            style = MonoDataMedium.copy(color = if (sale.esReversado) TextMuted else ElectricLime)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "Bs %.2f", sale.totalBs),
                                            style = MonoDataSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: MOVIMIENTOS DE INVENTARIO
            if (isLoading && movimientos.isEmpty()) {
                MovementsListSkeleton(count = 6)
            } else if (movimientos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay movimientos registrados",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Entradas, salidas, ajustes de precios y reversos aparecerán aquí",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(movimientos, key = { it.id }) { mov ->
                        MovimientoCardItem(
                            movimiento = mov,
                            dateFormat = dateFormat,
                            onRevert = onRevertMovimiento
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoCardItem(
    movimiento: Movimiento,
    dateFormat: SimpleDateFormat,
    onRevert: ((String) -> Unit)? = null
) {
    val dateStr = dateFormat.format(Date(movimiento.fecha))

    // Distinct styling for each TipoMovimiento
    val (typeColor, typeIcon, typeLabel) = when (movimiento.tipo) {
        TipoMovimiento.ENTRADA -> Triple(ElectricLime, Icons.Default.ArrowDownward, "ENTRADA DE STOCK")
        TipoMovimiento.SALIDA -> Triple(AlertRed, Icons.Default.ArrowUpward, "SALIDA / VENTA")
        TipoMovimiento.CAMBIO_PRECIO -> Triple(Color(0xFF38BDF8), Icons.Default.Edit, "AJUSTE DE PRECIO")
        TipoMovimiento.REVERSO -> Triple(Color(0xFFA78BFA), Icons.Default.Restore, "REVERSO DE OPERACIÓN")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (movimiento.esReversado) AlertRed.copy(alpha = 0.4f) else GraphiteBorder,
                RoundedCornerShape(8.dp)
            ),
        color = GraphiteSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = typeLabel,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                if (movimiento.cantidad > 0) {
                    Text(
                        text = when (movimiento.tipo) {
                            TipoMovimiento.ENTRADA -> "+${movimiento.cantidad}"
                            TipoMovimiento.SALIDA -> "-${movimiento.cantidad}"
                            TipoMovimiento.REVERSO -> "↺ ${movimiento.cantidad}"
                            TipoMovimiento.CAMBIO_PRECIO -> "${movimiento.cantidad}"
                        },
                        style = MonoDataMedium.copy(
                            color = if (movimiento.esReversado) TextMuted else typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movimiento.productoNombre.ifBlank { "Producto #${movimiento.productoFila}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (movimiento.motivo.isNotBlank()) {
                Text(
                    text = movimiento.motivo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Por: ${movimiento.usuarioNombre.ifBlank { movimiento.usuarioEmail.ifBlank { "Operador" } }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                if (movimiento.esReversado) {
                    Text(
                        text = "[MOVIMIENTO ANULADO]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed
                    )
                } else if (onRevert != null && movimiento.tipo != TipoMovimiento.REVERSO) {
                    OutlinedButton(
                        onClick = { onRevert(movimiento.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AlertRed
                        ),
                        modifier = Modifier.testTag("btn_revert_mov_${movimiento.id}")
                    ) {
                        Text("Reversar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
