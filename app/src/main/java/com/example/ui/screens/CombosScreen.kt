package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Combo
import com.example.data.model.ComboComponente
import com.example.data.model.Product
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
fun CombosScreen(
    combos: List<Combo>,
    products: List<Product>,
    exchangeRate: Double,
    isLoading: Boolean = false,
    isAdmin: Boolean = true,
    onRefresh: () -> Unit,
    onAddComboToCart: (Combo, Int) -> Unit,
    onCreateCombo: (nombre: String, precioUsd: Double, categoria: String, componentes: List<Pair<Product, Int>>, onSuccess: () -> Unit) -> Unit,
    onDeleteCombo: (Combo) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var comboToDelete by remember { mutableStateOf<Combo?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCombos = remember(combos, searchQuery) {
        if (searchQuery.isBlank()) {
            combos
        } else {
            combos.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.categoria.contains(searchQuery, ignoreCase = true) ||
                it.componentes.any { comp -> comp.nombre.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "COMBOS Y RECETAS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${combos.size} combos configurados",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, CircleShape)
                            .testTag("btn_refresh_combos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar",
                            tint = ElectricLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_create_combo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nuevo Combo",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar combo o ingrediente...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("combos_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = GraphiteBorder,
                    focusedContainerColor = GraphiteSurface,
                    unfocusedContainerColor = GraphiteSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main List
            if (isLoading && combos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricLime)
                }
            } else if (filteredCombos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No hay combos registrados" else "No se encontraron combos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Crea tu primer combo combinando productos del inventario" else "Prueba con otro término de búsqueda",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricLime,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_empty_create_combo")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crear Primer Combo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCombos, key = { it.id.ifBlank { "combo_${it.fila}" } }) { combo ->
                        ComboCardItem(
                            combo = combo,
                            exchangeRate = exchangeRate,
                            isAdmin = isAdmin,
                            onAddToCart = { qty -> onAddComboToCart(combo, qty) },
                            onDelete = { comboToDelete = combo }
                        )
                    }
                }
            }
        }

        // Dialog for Creating a New Combo
        if (showCreateDialog) {
            CreateComboDialog(
                availableProducts = products,
                onDismiss = { showCreateDialog = false },
                onConfirm = { nombre, precio, cat, compList ->
                    onCreateCombo(nombre, precio, cat, compList) {
                        showCreateDialog = false
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        comboToDelete?.let { combo ->
            AlertDialog(
                onDismissRequest = { comboToDelete = null },
                containerColor = GraphiteSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = { Text("Eliminar Combo", fontWeight = FontWeight.Bold) },
                text = {
                    Text("¿Estás seguro de que deseas eliminar el combo \"${combo.nombre}\"? Esta acción no afectará el stock de sus productos individuales.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCombo(combo)
                            comboToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_confirm_delete_combo")
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { comboToDelete = null }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun ComboCardItem(
    combo: Combo,
    exchangeRate: Double,
    isAdmin: Boolean,
    onAddToCart: (quantity: Int) -> Unit,
    onDelete: () -> Unit
) {
    var expandedComponents by remember { mutableStateOf(true) }
    var saleQuantity by remember { mutableIntStateOf(1) }
    val isAvailable = combo.disponibles > 0
    val precioBs = combo.precioUsd * exchangeRate

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (isAvailable) ElectricLime.copy(alpha = 0.5f) else GraphiteBorder,
                RoundedCornerShape(10.dp)
            )
            .testTag("combo_card_${combo.fila}"),
        color = GraphiteSurface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge & Availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricLime.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "COMBO",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (combo.categoria.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GraphiteSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = combo.categoria,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Availability Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isAvailable) ElectricLime.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isAvailable) "Disponibles: ${combo.disponibles}" else "Agotado",
                        color = if (isAvailable) ElectricLime else MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Combo Title and Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = combo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$ %.2f", combo.precioUsd),
                        style = MonoDataLarge.copy(fontSize = 18.sp, color = ElectricLime, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = String.format(Locale.US, "Bs. %.2f", precioBs),
                        style = MonoDataSmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                    if (combo.costoTotal > 0) {
                        Text(
                            text = String.format(Locale.US, "Ganancia: +$%.2f", combo.gananciaNeta),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ElectricLime
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recipe / Components Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.8.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
                color = GraphiteSurfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedComponents = !expandedComponents },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Componentes de la Receta (${combo.componentes.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (expandedComponents) "Ocultar ▲" else "Mostrar ▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricLime,
                            fontSize = 10.sp
                        )
                    }

                    if (expandedComponents) {
                        Spacer(modifier = Modifier.height(6.dp))
                        combo.componentes.forEach { comp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "•",
                                        color = ElectricLime,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = comp.nombre,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "x${comp.cantidadPorCombo} un.",
                                        style = MonoDataSmall.copy(fontSize = 11.sp, color = ElectricLime, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "(Stock: ${comp.stockDisponible})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (comp.stockDisponible >= comp.cantidadPorCombo) TextSecondary else MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Add to Cart + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAvailable) {
                    // Quick quantity selector with fixed size buttons and fixed-width number
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { if (saleQuantity > 1) saleQuantity-- },
                            enabled = saleQuantity > 1,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (saleQuantity > 1) GraphiteSurfaceVariant else GraphiteSurfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Restar",
                                tint = if (saleQuantity > 1) TextPrimary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

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
                                text = "$saleQuantity",
                                style = MonoDataMedium.copy(color = ElectricLime, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { if (saleQuantity < combo.disponibles) saleQuantity++ },
                            enabled = saleQuantity < combo.disponibles,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (saleQuantity < combo.disponibles) GraphiteSurfaceVariant else GraphiteSurfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Sumar",
                                tint = if (saleQuantity < combo.disponibles) TextPrimary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onAddToCart(saleQuantity) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_add_combo_cart_${combo.fila}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar a Venta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin stock suficiente para armar combo",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .testTag("btn_delete_combo_${combo.fila}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Combo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateComboDialog(
    availableProducts: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, precioUsd: Double, categoria: String, componentes: List<Pair<Product, Int>>) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var precioUsdStr by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Combos") }

    val selectedComponents = remember { mutableStateListOf<Pair<Product, Int>>() }
    var productSearchQuery by remember { mutableStateOf("") }
    var showProductPicker by remember { mutableStateOf(false) }

    val estimatedAvailability = remember(selectedComponents) {
        if (selectedComponents.isEmpty()) 0 else {
            selectedComponents.minOfOrNull { (prod, neededQty) ->
                if (neededQty > 0) prod.cantidad / neededQty else 0
            } ?: 0
        }
    }

    val sumOfIndividualPrices = remember(selectedComponents) {
        selectedComponents.sumOf { (prod, qty) -> prod.precioUsd * qty }
    }

    val sumOfCostPrices = remember(selectedComponents) {
        selectedComponents.sumOf { (prod, qty) -> prod.precioCompra * qty }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, ElectricLime, RoundedCornerShape(14.dp)),
            color = GraphiteSurface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = ElectricLime)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear Nuevo Combo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Define la receta vinculando productos del inventario y asigna un precio especial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nombre del Combo
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Combo *", color = TextSecondary) },
                    placeholder = { Text("Ej: Combo Cambio de Aceite Completo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_combo_nombre"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = GraphiteBorder,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Precio USD & Categoria Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = precioUsdStr,
                        onValueChange = { precioUsdStr = it },
                        label = { Text("Precio USD *", color = TextSecondary) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_combo_precio"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = categoria,
                        onValueChange = { categoria = it },
                        label = { Text("Categoría", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_combo_categoria"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                val parsedPrice = precioUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (sumOfIndividualPrices > 0 || sumOfCostPrices > 0 || parsedPrice > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Suma venta individual:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "$ %.2f USD", sumOfIndividualPrices),
                                    style = MonoDataSmall.copy(fontSize = 11.sp, color = TextPrimary)
                                )
                            }
                            if (sumOfCostPrices > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Costo total de insumos:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$ %.2f USD", sumOfCostPrices),
                                        style = MonoDataSmall.copy(fontSize = 11.sp, color = TextSecondary)
                                    )
                                }
                            }
                            if (parsedPrice > 0) {
                                val profit = parsedPrice - sumOfCostPrices
                                val margin = (profit / parsedPrice) * 100.0
                                val isPositive = profit >= 0
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ganancia neta por combo:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%s$%.2f (%.1f%%)", if (isPositive) "+" else "", profit, margin),
                                        style = MonoDataSmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositive) ElectricLime else MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recipe Components Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMPONENTES DE LA RECETA (${selectedComponents.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { showProductPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GraphiteSurfaceVariant,
                            contentColor = ElectricLime
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.border(1.dp, ElectricLime, RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Agregar Producto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedComponents.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Aún no has agregado ningún componente a la receta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedComponents.forEachIndexed { index, (prod, qty) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GraphiteBorder, RoundedCornerShape(6.dp)),
                                color = GraphiteSurfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.producto,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Stock actual: ${prod.cantidad} un. • $ ${String.format(Locale.US, "%.2f", prod.precioUsd)} c/u",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (qty > 1) {
                                                    selectedComponents[index] = Pair(prod, qty - 1)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurface)
                                                .border(1.dp, GraphiteBorder, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Restar", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 36.dp)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurface)
                                                .border(1.dp, ElectricLime.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$qty",
                                                style = MonoDataMedium.copy(color = ElectricLime, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedComponents[index] = Pair(prod, qty + 1)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurface)
                                                .border(1.dp, GraphiteBorder, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Sumar", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = { selectedComponents.removeAt(index) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Estimated availability indicator
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (estimatedAvailability > 0) ElectricLime.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(8.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disponibilidad calculada:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = if (estimatedAvailability > 0) "$estimatedAvailability combos disponibles" else "0 combos (stock insuficiente)",
                                style = MonoDataSmall.copy(
                                    color = if (estimatedAvailability > 0) ElectricLime else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }

                    val parsedPrice = precioUsdStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val isValid = nombre.isNotBlank() && parsedPrice > 0.0 && selectedComponents.isNotEmpty()

                    Button(
                        onClick = {
                            onConfirm(nombre, parsedPrice, categoria, selectedComponents.toList())
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black,
                            disabledContainerColor = ElectricLime.copy(alpha = 0.3f),
                            disabledContentColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_save_new_combo")
                    ) {
                        Text("Guardar Combo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal to pick a product to add to recipe
    if (showProductPicker) {
        Dialog(onDismissRequest = { showProductPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seleccionar Producto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showProductPicker = false }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        placeholder = { Text("Buscar en inventario...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val pickerList = remember(availableProducts, productSearchQuery) {
                        if (productSearchQuery.isBlank()) availableProducts
                        else availableProducts.filter {
                            it.producto.contains(productSearchQuery, ignoreCase = true) ||
                            it.codigo.contains(productSearchQuery, ignoreCase = true) ||
                            it.catalogo.contains(productSearchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pickerList) { prod ->
                            val alreadySelected = selectedComponents.any { it.first.fila == prod.fila }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (!alreadySelected) {
                                            selectedComponents.add(Pair(prod, 1))
                                            showProductPicker = false
                                        }
                                    },
                                color = if (alreadySelected) GraphiteSurfaceVariant.copy(alpha = 0.5f) else GraphiteSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.producto,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (alreadySelected) TextMuted else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prod.catalogo} • Stock: ${prod.cantidad} un. • $ ${String.format(Locale.US, "%.2f", prod.precioUsd)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (alreadySelected) {
                                        Text("Ya agregado", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricLime)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
