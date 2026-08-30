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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.Combo
import com.example.data.model.PriceMode
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerDialog
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
import java.util.Locale

@Composable
fun SaleScreen(
    products: List<Product>,
    combos: List<Combo> = emptyList(),
    cart: List<CartItem>,
    exchangeRate: Double,
    searchQuery: String,
    isSyncing: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onAddToCart: (product: Product, quantity: Int, priceMode: PriceMode) -> Unit,
    onAddComboToCart: (combo: Combo, quantity: Int) -> Unit = { _, _ -> },
    onUpdateCartQuantity: (fila: Int, newQuantity: Int) -> Unit,
    onUpdateCartPriceMode: ((fila: Int, mode: PriceMode) -> Unit)? = null,
    onRemoveFromCart: (fila: Int) -> Unit,
    onClearCart: () -> Unit,
    onConfirmSale: () -> Unit,
    onOpenQuickScan: (() -> Unit)? = null
) {
    var selectedProductForSale by remember { mutableStateOf<Product?>(null) }
    var selectedComboForSale by remember { mutableStateOf<Combo?>(null) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    var activePriceMode by remember { mutableStateOf(PriceMode.AUTOMATICO) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val totalUsd = cart.sumOf { it.subtotalUsd }
    val totalBs = totalUsd * exchangeRate
    val totalUnits = cart.sumOf { it.cantidadSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Section Title & Header Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = ElectricLime)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SALIDA / VENTA RÁPIDA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onOpenQuickScan != null) {
                    Surface(
                        onClick = onOpenQuickScan,
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("btn_goto_quick_scan_sale"),
                        color = GraphiteSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Escaneo",
                                color = ElectricLime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (cart.isNotEmpty()) {
                    Surface(
                        onClick = onClearCart,
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("btn_cancel_sale"),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Cancelar",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Product Selector / Search Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchQueryChange(it)
                        if (selectedProductForSale != null && it.isNotBlank()) {
                            selectedProductForSale = null
                        }
                    },
                    placeholder = { Text("Buscar producto o código...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sale_product_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = GraphiteBorder,
                        focusedContainerColor = GraphiteSurface,
                        unfocusedContainerColor = GraphiteSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, ElectricLime, RoundedCornerShape(8.dp))
                        .testTag("btn_scan_barcode_sale")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear Código de Barras",
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Horizontal row of matching products & combos
            if (searchQuery.isNotBlank()) {
                val matchingProducts = remember(products, searchQuery) {
                    products.filter {
                        it.producto.contains(searchQuery, ignoreCase = true) ||
                                it.catalogo.contains(searchQuery, ignoreCase = true) ||
                                it.codigo.contains(searchQuery, ignoreCase = true) ||
                                it.codigoBarras.contains(searchQuery, ignoreCase = true)
                    }.take(30)
                }

                val matchingCombos = remember(combos, searchQuery) {
                    combos.filter {
                        it.nombre.contains(searchQuery, ignoreCase = true) ||
                                it.categoria.contains(searchQuery, ignoreCase = true) ||
                                it.componentes.any { c -> c.nombre.contains(searchQuery, ignoreCase = true) }
                    }.take(30)
                }

                if (matchingProducts.isNotEmpty() || matchingCombos.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Combos first with prominent COMBO badge
                        items(matchingCombos, key = { "combo_${it.fila}" }) { combo ->
                            val isSelected = selectedComboForSale?.fila == combo.fila
                            val borderColor = if (isSelected) ElectricLime else GraphiteBorder
                            val bgColor = if (isSelected) ElectricLime.copy(alpha = 0.15f) else GraphiteSurface

                            Surface(
                                modifier = Modifier
                                    .width(190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedComboForSale = combo
                                        selectedProductForSale = null
                                        selectedQuantity = 1
                                    }
                                    .testTag("sale_search_combo_${combo.fila}"),
                                color = bgColor
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ElectricLime.copy(alpha = 0.18f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("COMBO", color = ElectricLime, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                        Text(
                                            text = if (combo.disponibles > 0) "${combo.disponibles} disp." else "Agotado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (combo.disponibles > 0) ElectricLime else MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = combo.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "$ %.2f", combo.precioUsd),
                                        style = MonoDataSmall.copy(color = ElectricLime)
                                    )
                                }
                            }
                        }

                        // Regular Products
                        items(matchingProducts, key = { "prod_${it.fila}" }) { product ->
                            val isSelected = selectedProductForSale?.fila == product.fila
                            val borderColor = if (isSelected) ElectricLime else GraphiteBorder
                            val bgColor = if (isSelected) ElectricLime.copy(alpha = 0.15f) else GraphiteSurface

                            Surface(
                                modifier = Modifier
                                    .width(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedProductForSale = product
                                        selectedComboForSale = null
                                        selectedQuantity = 1
                                    }
                                    .testTag("sale_search_item_${product.fila}"),
                                color = bgColor
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = product.producto,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "$ %.2f", product.precioUsd),
                                            style = MonoDataSmall.copy(color = ElectricLime)
                                        )
                                        Text(
                                            text = "Stock: ${product.cantidad}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Selection & Quick Quantity Buttons (Product OR Combo)
        val activeCombo = selectedComboForSale
        val activeProduct = if (activeCombo == null) (selectedProductForSale ?: products.firstOrNull { it.cantidad > 0 }) else null

        // Reset price mode to AUTOMATICO and quantity to 1 when activeProduct or activeCombo changes
        LaunchedEffect(activeProduct?.fila, activeProduct?.id, activeCombo?.fila, activeCombo?.id) {
            activePriceMode = PriceMode.AUTOMATICO
            selectedQuantity = 1
        }

        if (activeCombo != null) {
            // Active COMBO Selection Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, ElectricLime.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricLime.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("COMBO SELECCIONADO", color = ElectricLime, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = String.format(Locale.US, "$ %.2f", activeCombo.precioUsd),
                            style = MonoDataMedium.copy(color = ElectricLime)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = activeCombo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Componentes: ${activeCombo.componentes.joinToString(", ") { "${it.cantidadPorCombo}x ${it.nombre}" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stepper: Minus - Value - Plus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                            enabled = selectedQuantity > 1,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedQuantity > 1) GraphiteSurfaceVariant else GraphiteSurfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar", tint = if (selectedQuantity > 1) TextPrimary else TextMuted)
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ElectricLime.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                            color = GraphiteSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$selectedQuantity",
                                    style = MonoDataLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, color = ElectricLime)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("combos", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                if (activeCombo.disponibles <= 0 || selectedQuantity < activeCombo.disponibles) {
                                    selectedQuantity++
                                }
                            },
                            enabled = activeCombo.disponibles <= 0 || selectedQuantity < activeCombo.disponibles,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeCombo.disponibles <= 0 || selectedQuantity < activeCombo.disponibles) ElectricLime else GraphiteSurfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Sumar", tint = if (activeCombo.disponibles <= 0 || selectedQuantity < activeCombo.disponibles) Color.Black else TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onAddComboToCart(activeCombo, selectedQuantity)
                            selectedQuantity = 1
                            onSearchQueryChange("")
                        },
                        enabled = activeCombo.disponibles > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_add_combo_to_cart"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar $selectedQuantity combo(s) al Carrito ($ ${String.format(Locale.US, "%.2f", activeCombo.precioUsd * selectedQuantity)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else if (activeProduct != null) {
            val hasWholesale = activeProduct.precioMayor != null && activeProduct.precioMayor > 0
            val minMayorQty = if ((activeProduct.cantidadMinimaMayor ?: 0) > 0) activeProduct.cantidadMinimaMayor!! else 1
            val isMayorApplied = hasWholesale && when (activePriceMode) {
                PriceMode.AUTOMATICO -> selectedQuantity >= minMayorQty
                PriceMode.DETAL -> false
                PriceMode.MAYOR -> true
            }
            val effectiveUnitPrice = if (isMayorApplied && activeProduct.precioMayor != null) activeProduct.precioMayor else activeProduct.precioUsd
            val effectiveSubtotal = effectiveUnitPrice * selectedQuantity

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(
                        1.dp,
                        if (isMayorApplied) ElectricLime.copy(alpha = 0.8f) else GraphiteBorder,
                        RoundedCornerShape(8.dp)
                    ),
                color = GraphiteSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PRODUCTO SELECCIONADO:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeProduct.producto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "$ %.2f", effectiveUnitPrice),
                                style = MonoDataMedium.copy(
                                    color = if (isMayorApplied) ElectricLime else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (isMayorApplied) {
                                Text(
                                    text = "Mayor aplicado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricLime,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quantity Stepper and Assist Chips
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CANTIDAD A VENDER:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Text(
                                text = "Stock: ${activeProduct.cantidad} un.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeProduct.cantidad > 0) ElectricLime else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }

                        // Stepper: Minus - Large Number - Plus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Minus Button (48dp+ thumb-friendly touch target)
                            IconButton(
                                onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                                enabled = selectedQuantity > 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedQuantity > 1) GraphiteSurfaceVariant else GraphiteSurfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                                    .testTag("btn_stepper_minus")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Restar 1",
                                    tint = if (selectedQuantity > 1) TextPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Center Value Display Card
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, ElectricLime.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                color = GraphiteSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$selectedQuantity",
                                        style = MonoDataLarge.copy(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ElectricLime
                                        ),
                                        modifier = Modifier.testTag("text_stepper_value")
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

                            // Plus Button (48dp+ thumb-friendly touch target)
                            IconButton(
                                onClick = {
                                    if (activeProduct.cantidad <= 0 || selectedQuantity < activeProduct.cantidad) {
                                        selectedQuantity++
                                    }
                                },
                                enabled = activeProduct.cantidad <= 0 || selectedQuantity < activeProduct.cantidad,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (activeProduct.cantidad <= 0 || selectedQuantity < activeProduct.cantidad)
                                            ElectricLime
                                        else
                                            GraphiteSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .testTag("btn_stepper_plus")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Sumar 1",
                                    tint = if (activeProduct.cantidad <= 0 || selectedQuantity < activeProduct.cantidad) Color.Black else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Secondary Discrete Assist Chips (+5, +10, Máx)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val assistDeltas = listOf(5, 10)
                            assistDeltas.forEach { delta ->
                                val targetQty = selectedQuantity + delta
                                val isAvailable = activeProduct.cantidad <= 0 || targetQty <= activeProduct.cantidad

                                Surface(
                                    onClick = {
                                        val newQty = if (activeProduct.cantidad > 0) targetQty.coerceAtMost(activeProduct.cantidad) else targetQty
                                        selectedQuantity = newQty
                                    },
                                    enabled = isAvailable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .testTag("chip_assist_plus_$delta"),
                                    color = GraphiteSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+$delta",
                                            style = MonoDataSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAvailable) TextPrimary else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            if (activeProduct.cantidad > 0) {
                                val isMax = selectedQuantity == activeProduct.cantidad
                                Surface(
                                    onClick = { selectedQuantity = activeProduct.cantidad },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .testTag("chip_assist_max"),
                                    color = if (isMax) ElectricLime.copy(alpha = 0.2f) else GraphiteSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (isMax) ElectricLime else GraphiteBorder
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Máx (${activeProduct.cantidad})",
                                            style = MonoDataSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMax) ElectricLime else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // WHOLESALE SECTION: Exactly between Stepper and "Agregar al Carrito"
                    if (hasWholesale) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. 3-Button Segmented Control in a single row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val modes = listOf(
                                PriceMode.AUTOMATICO to "Auto",
                                PriceMode.DETAL to "Detal",
                                PriceMode.MAYOR to "Mayor"
                            )
                            modes.forEach { (mode, label) ->
                                val isSelected = activePriceMode == mode
                                Surface(
                                    onClick = { activePriceMode = mode },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .testTag("btn_mode_${mode.name.lowercase()}"),
                                    color = if (isSelected) ElectricLime else GraphiteSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ElectricLime else GraphiteBorder
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else TextSecondary,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Indicator of applied price
                        val indicatorText = when (activePriceMode) {
                            PriceMode.AUTOMATICO -> {
                                if (selectedQuantity >= minMayorQty) {
                                    "✓ Aplicando: Mayor ($${String.format(Locale.US, "%.2f", activeProduct.precioMayor ?: effectiveUnitPrice)} c/u) por llevar $minMayorQty o más unidades"
                                } else {
                                    val faltan = minMayorQty - selectedQuantity
                                    "Aplicando: Detal ($${String.format(Locale.US, "%.2f", activeProduct.precioUsd)} c/u) • Agrega $faltan más para precio al mayor ($${String.format(Locale.US, "%.2f", activeProduct.precioMayor ?: 0.0)})"
                                }
                            }
                            PriceMode.DETAL -> "Aplicando: Detal ($${String.format(Locale.US, "%.2f", activeProduct.precioUsd)} c/u) [Forzado manual]"
                            PriceMode.MAYOR -> "✓ Aplicando: Mayor ($${String.format(Locale.US, "%.2f", activeProduct.precioMayor ?: effectiveUnitPrice)} c/u) [Forzado manual]"
                        }

                        Text(
                            text = indicatorText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMayorApplied) ElectricLime else TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button to Add to Cart
                    Button(
                        onClick = {
                            onAddToCart(activeProduct, selectedQuantity, activePriceMode)
                            selectedQuantity = 1
                            activePriceMode = PriceMode.AUTOMATICO
                            onSearchQueryChange("")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_add_to_cart"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar $selectedQuantity un. al Carrito ($ ${String.format(Locale.US, "%.2f", effectiveSubtotal)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cart Items List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "El carrito de venta está vacío",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Selecciona o busca un producto arriba para agregarlo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = cart,
                        key = { item ->
                            if (item.isCombo && item.combo != null) {
                                item.combo.id.ifBlank { "cart_combo_${item.combo.fila}_${item.combo.nombre}" }
                            } else {
                                item.product.id.ifBlank { "cart_prod_${item.product.fila}_${item.product.producto}" }
                            }
                        }
                    ) { item ->
                        CartItemRow(
                            item = item,
                            exchangeRate = exchangeRate,
                            onQtyChange = { newQty -> onUpdateCartQuantity(item.itemFila, newQty) },
                            onPriceModeChange = { newMode -> onUpdateCartPriceMode?.invoke(item.itemFila, newMode) },
                            onRemove = { onRemoveFromCart(item.itemFila) }
                        )
                    }
                }
            }
        }

        // Persistent Sticky Cart Summary Footer & Cobrar Button
        if (cart.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricLime, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalUnits UNIDADES EN TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = String.format(Locale.US, "Bs %.2f", totalBs),
                                style = MonoDataMedium.copy(color = TextPrimary, fontSize = 15.sp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL A COBRAR:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = String.format(Locale.US, "$ %.2f USD", totalUsd),
                                style = MonoDataLarge.copy(fontSize = 24.sp, color = ElectricLime)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onConfirmSale,
                        enabled = !isSyncing && cart.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_confirm_sale"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black,
                            disabledContainerColor = ElectricLime.copy(alpha = 0.4f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REGISTRANDO VENTA...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        } else {
                            Text(
                                text = "COBRAR Y CONFIRMAR VENTA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Escanear para Venta",
            subtitle = "Apunta al código de barras del producto a vender",
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { scannedCode ->
                val clean = scannedCode.trim()
                val matched = products.firstOrNull {
                    it.codigoBarras.equals(clean, ignoreCase = true)
                } ?: products.firstOrNull {
                    it.codigo.equals(clean, ignoreCase = true)
                }

                if (matched != null) {
                    onAddToCart(matched, 1, PriceMode.AUTOMATICO)
                    showBarcodeScanner = false
                } else {
                    onSearchQueryChange(clean)
                    showBarcodeScanner = false
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    exchangeRate: Double,
    onQtyChange: (Int) -> Unit,
    onPriceModeChange: ((PriceMode) -> Unit)? = null,
    onRemove: () -> Unit
) {
    val subtotalBs = item.subtotalUsd * exchangeRate
    val isCombo = item.isCombo && item.combo != null
    val itemName = if (isCombo) item.combo!!.nombre else item.product.producto
    val unitPriceUsd = item.precioUnitarioAplicado
    val maxStock = if (isCombo) item.combo!!.disponibles else item.product.cantidad
    val hasWholesale = !isCombo && item.product.precioMayor != null && item.product.precioMayor > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (isCombo) ElectricLime.copy(alpha = 0.4f)
                else if (item.esPrecioMayorAplicado) ElectricLime.copy(alpha = 0.7f)
                else GraphiteBorder,
                RoundedCornerShape(10.dp)
            ),
        color = GraphiteSurface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LÍNEA 1 (arriba): Nombre a la izquierda, precio unitario + subtotal a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCombo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricLime.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("COMBO", color = ElectricLime, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        } else if (item.esPrecioMayorAplicado) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricLime.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("MAYOR", color = ElectricLime, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            text = itemName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isCombo && item.combo != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.combo.componentes.joinToString(", ") { "${it.cantidadPorCombo}x ${it.nombre}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Precios a la derecha (subtotal destacado y precio unitario + Bs)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format(Locale.US, "$ %.2f", item.subtotalUsd),
                        style = MonoDataMedium.copy(
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasWholesale) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (item.esPrecioMayorAplicado) ElectricLime.copy(alpha = 0.2f) else GraphiteSurfaceVariant)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (item.esPrecioMayorAplicado) "Mayor" else "Detal",
                                    color = if (item.esPrecioMayorAplicado) ElectricLime else TextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "$ %.2f c/u", unitPriceUsd),
                            style = MonoDataSmall.copy(
                                color = if (item.esPrecioMayorAplicado) ElectricLime else TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = String.format(Locale.US, "Bs %.2f", subtotalBs),
                            style = MonoDataSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Optional Wholesale Price Mode Selector inside cart item
            if (hasWholesale && onPriceModeChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modo:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    val modes = listOf(
                        PriceMode.AUTOMATICO to "Auto",
                        PriceMode.DETAL to "Detal",
                        PriceMode.MAYOR to "Mayor"
                    )
                    modes.forEach { (mode, label) ->
                        val isSelected = item.priceMode == mode
                        Surface(
                            onClick = { onPriceModeChange(mode) },
                            modifier = Modifier
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isSelected) ElectricLime else GraphiteSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                0.6.dp,
                                if (isSelected) ElectricLime else GraphiteBorder
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // LÍNEA 2 (abajo): Stepper a la izquierda, basura a la derecha (con espacio de sobra)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stepper: [-] [ número ] [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Botón "−": fijo 40x40dp mínimo, no se encoge
                    IconButton(
                        onClick = { onQtyChange(item.cantidadSelected - 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Restar 1",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Número central: ancho fijo mínimo 44dp, centrado, monoespaciado
                    Box(
                        modifier = Modifier
                            .widthIn(min = 44.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.cantidadSelected}",
                            style = MonoDataMedium.copy(
                                color = ElectricLime,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }

                    // Botón "+": fijo 40x40dp mínimo, no se encoge
                    val canIncrease = maxStock <= 0 || item.cantidadSelected < maxStock
                    IconButton(
                        onClick = {
                            if (canIncrease) {
                                onQtyChange(item.cantidadSelected + 1)
                            }
                        },
                        enabled = canIncrease,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canIncrease) GraphiteSurfaceVariant else GraphiteSurfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, if (canIncrease) GraphiteBorder else GraphiteBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sumar 1",
                            tint = if (canIncrease) TextPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Ícono de basura: separado por al menos 16dp del stepper, 40x40dp fijo
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar de la venta",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
