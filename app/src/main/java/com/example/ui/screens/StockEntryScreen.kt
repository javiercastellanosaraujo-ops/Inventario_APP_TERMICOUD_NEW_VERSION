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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import android.widget.Toast
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.UpcItemDbService
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun StockEntryScreen(
    products: List<Product>,
    categories: List<String>,
    onAddStockToExisting: (product: Product, quantityToAdd: Int) -> Unit,
    onCreateNewProduct: (producto: String, cantidad: Int, precioUsd: Double, categoria: String, codigoBarras: String, precioMayor: Double?, cantidadMinimaMayor: Int?, precioCompra: Double) -> Unit,
    onOpenQuickScan: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchOrNameInput by remember { mutableStateOf("") }
    var selectedExistingProduct by remember { mutableStateOf<Product?>(null) }
    var isLookingUpBarcode by remember { mutableStateOf(false) }

    var addQuantityInput by remember { mutableStateOf("1") }

    // New product fields
    var newProductPrecioCompraInput by remember { mutableStateOf("") }
    var newProductPriceInput by remember { mutableStateOf("") }
    var newProductCategoryInput by remember { mutableStateOf("General") }
    var newProductBarcode by remember { mutableStateOf("") }
    var hasWholesale by remember { mutableStateOf(false) }
    var newProductPrecioMayorInput by remember { mutableStateOf("") }
    var newProductCantMinimaMayorInput by remember { mutableStateOf("") }

    var showBarcodeScannerForSearch by remember { mutableStateOf(false) }
    var showBarcodeScannerForNewProduct by remember { mutableStateOf(false) }

    val matchingProducts = if (searchOrNameInput.isBlank()) {
        emptyList()
    } else {
        products.filter {
            it.producto.contains(searchOrNameInput, ignoreCase = true) ||
                    it.catalogo.contains(searchOrNameInput, ignoreCase = true) ||
                    it.codigo.contains(searchOrNameInput, ignoreCase = true) ||
                    it.codigoBarras.contains(searchOrNameInput, ignoreCase = true)
        }
    }

    val exactMatch = products.any {
        it.producto.equals(searchOrNameInput.trim(), ignoreCase = true) ||
                (it.codigoBarras.isNotBlank() && it.codigoBarras.equals(searchOrNameInput.trim(), ignoreCase = true))
    }
    val isNewProductMode = searchOrNameInput.isNotBlank() && matchingProducts.isEmpty() && !exactMatch

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Header Title
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
                    Icon(Icons.Default.AddBox, contentDescription = null, tint = ElectricLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENTRADA DE STOCK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (onOpenQuickScan != null) {
                    Button(
                        onClick = onOpenQuickScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_goto_quick_scan_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escaneo Rápido", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Product Name Search / Entry Input
        item {
            Column {
                Text(
                    text = "NOMBRE DEL PRODUCTO O CÓDIGO",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchOrNameInput,
                        onValueChange = {
                            searchOrNameInput = it
                            selectedExistingProduct = null
                        },
                        placeholder = { Text("Escribe nombre o código...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = {
                            if (searchOrNameInput.isNotBlank()) {
                                IconButton(onClick = {
                                    searchOrNameInput = ""
                                    selectedExistingProduct = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("entry_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedContainerColor = GraphiteSurface,
                            unfocusedContainerColor = GraphiteSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    IconButton(
                        onClick = { showBarcodeScannerForSearch = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, ElectricLime, RoundedCornerShape(8.dp))
                            .testTag("btn_scan_barcode_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear para buscar",
                            tint = ElectricLime,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Matching existing products suggestions
        if (searchOrNameInput.isNotBlank() && matchingProducts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "COINCIDENCIAS EN CATALOGO (${matchingProducts.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLime
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = matchingProducts,
                            key = { it.id.ifBlank { "entry_prod_${it.fila}_${it.producto}" } }
                        ) { prod ->
                            val isSelected = (selectedExistingProduct?.id?.isNotBlank() == true && selectedExistingProduct?.id == prod.id) ||
                                (prod.fila > 0 && selectedExistingProduct?.fila == prod.fila)
                            val borderColor = if (isSelected) ElectricLime else GraphiteBorder
                            val bgColor = if (isSelected) ElectricLime.copy(alpha = 0.15f) else GraphiteSurface

                            Surface(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedExistingProduct = prod
                                        searchOrNameInput = prod.producto
                                    }
                                    .testTag("entry_suggestion_${prod.id.ifBlank { prod.fila.toString() }}"),
                                color = bgColor
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = prod.producto,
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
                                        Text(text = "Stock: ${prod.cantidad}", style = MonoDataSmall)
                                        Text(
                                            text = String.format(Locale.US, "$ %.2f", prod.precioUsd),
                                            style = MonoDataSmall.copy(color = ElectricLime)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MODE 1: Adding stock to existing product
        val targetProduct = selectedExistingProduct
            ?: products.find { it.producto.equals(searchOrNameInput.trim(), ignoreCase = true) }

        if (targetProduct != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ElectricLime, RoundedCornerShape(10.dp)),
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRODUCTO ENCONTRADO EN CATALOGO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = targetProduct.producto,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "Stock Actual: ${targetProduct.cantidad} un. • Precio: $ ${String.format(Locale.US, "%.2f", targetProduct.precioUsd)} • Catálogo: ${targetProduct.catalogo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        Text(
                            text = "CANTIDAD DE UNIDADES A SUMAR AL STOCK:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = addQuantityInput,
                            onValueChange = { addQuantityInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_stock_qty_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = GraphiteBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val qtyToAdd = addQuantityInput.toIntOrNull() ?: 1
                                onAddStockToExisting(targetProduct, qtyToAdd)
                                searchOrNameInput = ""
                                selectedExistingProduct = null
                                addQuantityInput = "1"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_add_stock"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("AGREGAR AL STOCK", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // MODE 2: Creating a brand new product
        if (isNewProductMode || (searchOrNameInput.isBlank() && targetProduct == null)) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isNewProductMode) "ESTE PRODUCTO NO EXISTE EN EL CATÁLOGO" else "CREAR NUEVO PRODUCTO EN CATÁLOGO",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNewProductMode) ElectricLime else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        if (isNewProductMode) {
                            Text(
                                text = "¿Deseas crearlo como producto nuevo?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (!isNewProductMode) {
                            Text(
                                text = "Nombre:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = searchOrNameInput,
                                onValueChange = { searchOrNameInput = it },
                                placeholder = { Text("Ej. Cable USB-C a HDMI 1.8m", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_product_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cantidad Inicial:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = addQuantityInput,
                                    onValueChange = { addQuantityInput = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_product_qty_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Precio Compra (Costo):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = newProductPrecioCompraInput,
                                    onValueChange = { newProductPrecioCompraInput = it },
                                    prefix = { Text("$ ", color = TextSecondary) },
                                    placeholder = { Text("0.00", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_product_precio_compra_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = GraphiteBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Precio Venta (Detal USD):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newProductPriceInput,
                                onValueChange = { newProductPriceInput = it },
                                prefix = { Text("$ ", color = ElectricLime) },
                                placeholder = { Text("0.00", color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_product_price_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder
                                )
                            )
                        }

                        // Live Profit Preview for new product
                        val cost = newProductPrecioCompraInput.toDoubleOrNull() ?: 0.0
                        val price = newProductPriceInput.toDoubleOrNull() ?: 0.0
                        if (price > 0 || cost > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val profit = price - cost
                            val margin = if (price > 0) (profit / price) * 100.0 else 0.0
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = GraphiteSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (profit >= 0) ElectricLime.copy(alpha = 0.3f) else com.example.ui.theme.AlertRed.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ganancia estimada por unidad:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%s$%.2f (%.1f%%)", if (profit >= 0) "+" else "", profit, margin),
                                        style = MonoDataSmall.copy(
                                            color = if (profit >= 0) ElectricLime else com.example.ui.theme.AlertRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Catálogo / Categoría:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Category suggestion chips
                        val categorySuggestions = categories.filter { it != "Todos" }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(categorySuggestions) { cat ->
                                val isSelected = newProductCategoryInput.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) ElectricLime else GraphiteBorder)
                                        .clickable { newProductCategoryInput = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newProductCategoryInput,
                            onValueChange = { newProductCategoryInput = it },
                            placeholder = { Text("Categoría personalizada...", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_product_category_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = GraphiteBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Código de Barras (Opcional):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newProductBarcode,
                                onValueChange = { newProductBarcode = it },
                                placeholder = { Text("Escanear o escribir...", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_product_barcode_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder
                                )
                            )

                            IconButton(
                                onClick = { showBarcodeScannerForNewProduct = true },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GraphiteSurface)
                                    .border(1.dp, ElectricLime, RoundedCornerShape(8.dp))
                                    .testTag("btn_scan_barcode_new_prod")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Escanear Código",
                                    tint = ElectricLime,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Wholesale pricing box
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
                                .padding(12.dp)
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
                                                newProductPrecioMayorInput = ""
                                                newProductCantMinimaMayorInput = ""
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = ElectricLime,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = GraphiteSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("switch_new_product_wholesale")
                                    )
                                }

                                if (hasWholesale) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Precio al Mayor (USD):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = newProductPrecioMayorInput,
                                                onValueChange = { newProductPrecioMayorInput = it },
                                                prefix = { Text("$ ", color = ElectricLime) },
                                                placeholder = { Text("0.00", color = TextMuted) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("new_product_precio_mayor_input"),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = ElectricLime,
                                                    unfocusedBorderColor = GraphiteBorder
                                                )
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Cant. Mínima Mayor:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = newProductCantMinimaMayorInput,
                                                onValueChange = { newProductCantMinimaMayorInput = it },
                                                placeholder = { Text("Ej. 3", color = TextMuted) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("new_product_cant_minima_mayor_input"),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val name = searchOrNameInput.trim()
                                val qty = addQuantityInput.toIntOrNull() ?: 0
                                val price = newProductPriceInput.toDoubleOrNull() ?: 0.0
                                val precioMayor = if (hasWholesale) newProductPrecioMayorInput.toDoubleOrNull() else null
                                val cantMinima = if (hasWholesale) newProductCantMinimaMayorInput.toIntOrNull() else null

                                val cost = newProductPrecioCompraInput.toDoubleOrNull() ?: 0.0

                                if (name.isNotBlank() && price >= 0) {
                                    onCreateNewProduct(name, qty, price, newProductCategoryInput, newProductBarcode.trim(), precioMayor, cantMinima, cost)
                                    searchOrNameInput = ""
                                    newProductPrecioCompraInput = ""
                                    newProductPriceInput = ""
                                    newProductBarcode = ""
                                    hasWholesale = false
                                    newProductPrecioMayorInput = ""
                                    newProductCantMinimaMayorInput = ""
                                    addQuantityInput = "1"
                                }
                            },
                            enabled = searchOrNameInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_create_new_product"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = Color.Black,
                                disabledContainerColor = ElectricLime.copy(alpha = 0.4f),
                                disabledContentColor = Color.Black.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CREAR Y REGISTRAR PRODUCTO", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (showBarcodeScannerForSearch) {
        BarcodeScannerDialog(
            title = "Escanear para Entrada",
            subtitle = "Apunta al código de barras del producto",
            onDismiss = { showBarcodeScannerForSearch = false },
            onBarcodeScanned = { scannedCode ->
                val clean = scannedCode.trim()
                showBarcodeScannerForSearch = false

                // 1. Check local catalog
                val matched = products.firstOrNull {
                    it.codigoBarras.equals(clean, ignoreCase = true)
                } ?: products.firstOrNull {
                    it.codigo.equals(clean, ignoreCase = true)
                } ?: products.firstOrNull {
                    it.producto.equals(clean, ignoreCase = true)
                }

                if (matched != null) {
                    selectedExistingProduct = matched
                    searchOrNameInput = matched.producto
                    Toast.makeText(context, "Producto encontrado: ${matched.producto}", Toast.LENGTH_SHORT).show()
                } else {
                    // 2. Query UPCItemDB online
                    coroutineScope.launch {
                        isLookingUpBarcode = true
                        val onlineResult = UpcItemDbService.lookupBarcode(clean)
                        isLookingUpBarcode = false

                        if (onlineResult.found) {
                            searchOrNameInput = onlineResult.title
                            newProductBarcode = clean
                            if (onlineResult.category.isNotBlank()) {
                                newProductCategoryInput = onlineResult.category
                            }
                            Toast.makeText(context, "Identificado vía UPCItemDB: ${onlineResult.title}", Toast.LENGTH_LONG).show()
                        } else {
                            searchOrNameInput = ""
                            newProductBarcode = clean
                            val msg = if (onlineResult.errorMessage != null) {
                                "No se pudo buscar en línea (${onlineResult.errorMessage}), completa los datos manualmente"
                            } else {
                                "Código '$clean' no encontrado en catálogo ni en UPCItemDB. Ingresa los datos."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    if (showBarcodeScannerForNewProduct) {
        BarcodeScannerDialog(
            title = "Asignar Código de Barras",
            subtitle = "Apunta al código para registrar este nuevo producto",
            onDismiss = { showBarcodeScannerForNewProduct = false },
            onBarcodeScanned = { scannedCode ->
                val clean = scannedCode.trim()
                newProductBarcode = clean
                showBarcodeScannerForNewProduct = false
                
                // If title is currently blank, attempt lookup
                if (searchOrNameInput.isBlank()) {
                    coroutineScope.launch {
                        val onlineResult = UpcItemDbService.lookupBarcode(clean)
                        if (onlineResult.found) {
                            searchOrNameInput = onlineResult.title
                            if (onlineResult.category.isNotBlank()) {
                                newProductCategoryInput = onlineResult.category
                            }
                            Toast.makeText(context, "Datos autocompletados desde UPCItemDB", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
