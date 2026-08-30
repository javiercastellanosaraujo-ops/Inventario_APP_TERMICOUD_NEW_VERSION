package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetProductActions(
    product: Product,
    exchangeRate: Double,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onUpdateProduct: (product: Product, cantidad: Int, precioUsd: Double, codigoBarras: String, precioMayor: Double?, cantidadMinimaMayor: Int?, precioCompra: Double?) -> Unit,
    onAddToCartAndGoToSale: (product: Product, quantity: Int) -> Unit,
    onDeleteProduct: ((Product) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentQty by remember(product) { mutableIntStateOf(product.cantidad) }
    var priceInput by remember(product) { mutableDoubleStateOf(product.precioUsd) }
    var priceText by remember(product) { mutableStateOf(String.format(Locale.US, "%.2f", product.precioUsd)) }
    var precioCompraInput by remember(product) { mutableDoubleStateOf(product.precioCompra) }
    var precioCompraText by remember(product) { mutableStateOf(if (product.precioCompra > 0) String.format(Locale.US, "%.2f", product.precioCompra) else "") }
    var barcodeText by remember(product) { mutableStateOf(product.codigoBarras) }
    var hasWholesale by remember(product) { mutableStateOf(product.precioMayor != null && product.precioMayor > 0) }
    var precioMayorText by remember(product) { mutableStateOf(product.precioMayor?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var cantMinimaMayorText by remember(product) { mutableStateOf(product.cantidadMinimaMayor?.toString() ?: "") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GraphiteSurface,
        scrimColor = GraphiteSurface.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Info
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GraphiteSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ID FILA: ${product.fila} • ${product.catalogo.uppercase(Locale.getDefault())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricLime
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.producto,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action 1: Modify Quantity Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "CANTIDAD DE STOCK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentQty > 0) currentQty-- },
                            enabled = currentQty > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GraphiteSurfaceVariant)
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                                .testTag("btn_decrement_qty")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Restar 1",
                                tint = if (currentQty > 0) TextPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            color = GraphiteSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$currentQty",
                                    style = MonoDataLarge.copy(fontSize = 22.sp, color = ElectricLime, fontWeight = FontWeight.Black),
                                    modifier = Modifier.testTag("text_qty_value")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "un.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { currentQty++ },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricLime)
                                .testTag("btn_increment_qty")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Sumar 1",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Discrete assist chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(5, 10, 20).forEach { delta ->
                            Surface(
                                onClick = { currentQty += delta },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = GraphiteSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+$delta",
                                        style = MonoDataSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: Cost Price & Sale Price (Ganancias - Admin Only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = if (isAdmin) "PRECIOS Y GANANCIA" else "PRECIO DE VENTA",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Precio de compra (Costo)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Precio Compra (Costo):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = precioCompraText,
                                    onValueChange = { input ->
                                        precioCompraText = input
                                        val parsed = input.toDoubleOrNull()
                                        precioCompraInput = if (parsed != null && parsed >= 0) parsed else 0.0
                                    },
                                    prefix = { Text("$ ", color = TextSecondary) },
                                    placeholder = { Text("0.00", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_precio_compra"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }

                            // Precio de venta (Detal)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Precio Venta (Detal):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { input ->
                                        priceText = input
                                        val parsed = input.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0) {
                                            priceInput = parsed
                                        }
                                    },
                                    prefix = { Text("$ ", color = ElectricLime) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_price_usd"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Profit Preview Card
                        val gananciaDetal = (priceInput - precioCompraInput).coerceAtLeast(-precioCompraInput)
                        val margenDetal = if (priceInput > 0) ((gananciaDetal / priceInput) * 100.0) else 0.0
                        val isProfitPositive = gananciaDetal >= 0

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = GraphiteSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (isProfitPositive) ElectricLime.copy(alpha = 0.3f) else AlertRed.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Ganancia neta por unidad:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Bs %.2f al cambio", priceInput * exchangeRate),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "%s$%.2f", if (isProfitPositive) "+" else "", gananciaDetal),
                                        style = MonoDataLarge.copy(
                                            fontSize = 15.sp,
                                            color = if (isProfitPositive) ElectricLime else AlertRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Margen: %.1f%%", margenDetal),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isProfitPositive) ElectricLime.copy(alpha = 0.8f) else AlertRed.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        // For Operator: Only show & edit Sale Price (Detal)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Precio Venta (Detal):",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { input ->
                                    priceText = input
                                    val parsed = input.toDoubleOrNull()
                                    if (parsed != null && parsed >= 0) {
                                        priceInput = parsed
                                    }
                                },
                                prefix = { Text("$ ", color = ElectricLime) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_price_usd"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "Equivalente: Bs %.2f", priceInput * exchangeRate),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Barcode Field & Camera Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "CÓDIGO DE BARRAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcodeText,
                            onValueChange = { barcodeText = it },
                            placeholder = { Text("Escanea o escribe código", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_product_barcode"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = GraphiteBorder
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricLime.copy(alpha = 0.15f))
                                .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .testTag("btn_scan_barcode_camera")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear Código de Barras",
                                tint = ElectricLime
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Wholesale Pricing (Precio al Mayor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (hasWholesale) ElectricLime.copy(alpha = 0.6f) else GraphiteBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .background(if (hasWholesale) ElectricLime.copy(alpha = 0.05f) else Color.Transparent)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRECIO AL MAYOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasWholesale) ElectricLime else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Este producto maneja precio al mayor",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = hasWholesale,
                            onCheckedChange = { checked ->
                                hasWholesale = checked
                                if (!checked) {
                                    precioMayorText = ""
                                    cantMinimaMayorText = ""
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ElectricLime,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = GraphiteSurfaceVariant
                            ),
                            modifier = Modifier.testTag("switch_has_wholesale")
                        )
                    }

                    if (hasWholesale) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Precio Mayor (USD):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = precioMayorText,
                                    onValueChange = { precioMayorText = it },
                                    prefix = { Text("$ ", color = ElectricLime) },
                                    placeholder = { Text("0.00", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_precio_mayor"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cant. Mínima Mayor:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = cantMinimaMayorText,
                                    onValueChange = { cantMinimaMayorText = it },
                                    placeholder = { Text("Ej. 3", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_cant_minima_mayor"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CTA Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sacar / Ir a venta
                Button(
                    onClick = {
                        onAddToCartAndGoToSale(product, 1)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_sacar_venta"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphiteSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Sacar (Venta)")
                }

                // Guardar cambios (cantidad / precio / código de barras / precio mayor)
                Button(
                    onClick = {
                        val parsedMayor = if (hasWholesale) precioMayorText.toDoubleOrNull() else null
                        val parsedCantMin = if (hasWholesale) cantMinimaMayorText.toIntOrNull() else null
                        onUpdateProduct(product, currentQty, priceInput, barcodeText, parsedMayor, parsedCantMin, precioCompraInput)
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("btn_save_product_changes"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp), tint = Color.Black)
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Admin Only: Delete Product Button
            if (isAdmin && onDeleteProduct != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_admin_delete_product"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AlertRed.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar del Catálogo (Solo Admin)", color = AlertRed, fontSize = 13.sp)
                }
            }
        }
    }

    if (showDeleteConfirmDialog && onDeleteProduct != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = GraphiteSurface,
            title = {
                Text("¿Eliminar producto?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Esta acción eliminará '${product.producto}' permanentemente del inventario en Firestore. Solo administradores pueden realizar esta operación.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProduct(product)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                    modifier = Modifier.testTag("btn_confirm_delete_product")
                ) {
                    Text("Eliminar Definitivamente")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_delete_product")
                ) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { scannedCode ->
                barcodeText = scannedCode
                showBarcodeScanner = false
            }
        )
    }
}
