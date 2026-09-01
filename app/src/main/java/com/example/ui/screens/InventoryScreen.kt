package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StockFilter
import com.example.ui.components.InventoryListSkeleton
import com.example.ui.components.ProductCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.OnElectricLime
import com.example.ui.theme.StatusAgotado
import com.example.ui.theme.StatusAgotadoBg
import com.example.ui.theme.StatusBajo
import com.example.ui.theme.StatusBajoBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    stockFilter: StockFilter = StockFilter.TODOS,
    exchangeRate: Double,
    isSyncing: Boolean,
    isRenamingCategory: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onStockFilterSelect: (StockFilter) -> Unit = {},
    onRenameCategory: ((String, String) -> Unit)? = null,
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit,
    onRefresh: () -> Unit
) {
    // Preserve scroll state across tab switches
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // Dialog state for renaming/merging catalogs on long press
    var catalogToRename by remember { mutableStateOf<String?>(null) }
    var newCatalogName by remember { mutableStateOf("") }

    // Close rename dialog when operation completes successfully
    var wasRenaming by remember { mutableStateOf(false) }
    LaunchedEffect(isRenamingCategory) {
        if (wasRenaming && !isRenamingCategory) {
            catalogToRename = null
        }
        wasRenaming = isRenamingCategory
    }

    // Progressive pagination in batches of 30
    var visibleItemCount by rememberSaveable(selectedCategory, searchQuery, stockFilter) { mutableIntStateOf(30) }

    val shouldLoadMore by remember(products.size, visibleItemCount) {
        derivedStateOf {
            if (visibleItemCount >= products.size) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (visibleItemCount - 5)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            visibleItemCount = (visibleItemCount + 30).coerceAtMost(products.size)
        }
    }

    val displayedProducts = remember(products, visibleItemCount) {
        products.take(visibleItemCount)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar & Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATÁLOGO DE INVENTARIO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("btn_refresh_inventory")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricLime,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = ElectricLime)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Real-time Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar por nombre, código o EAN...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = GraphiteBorder,
                    focusedContainerColor = GraphiteSurface,
                    unfocusedContainerColor = GraphiteSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Quick Stock Filter Chips (Todos / Agotados / Stock Bajo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODOS
            val isTodos = stockFilter == StockFilter.TODOS
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isTodos) ElectricLime else GraphiteBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onStockFilterSelect(StockFilter.TODOS) }
                    .testTag("stock_filter_todos"),
                color = if (isTodos) ElectricLime else GraphiteSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isTodos) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTodos) OnElectricLime else TextSecondary
                    )
                }
            }

            // AGOTADOS
            val isAgotados = stockFilter == StockFilter.AGOTADOS
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isAgotados) StatusAgotado else GraphiteBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onStockFilterSelect(StockFilter.AGOTADOS) }
                    .testTag("stock_filter_agotados"),
                color = if (isAgotados) StatusAgotadoBg else GraphiteSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(StatusAgotado)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Agotados",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isAgotados) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAgotados) StatusAgotado else TextSecondary
                    )
                }
            }

            // STOCK BAJO
            val isBajo = stockFilter == StockFilter.STOCK_BAJO
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isBajo) StatusBajo else GraphiteBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onStockFilterSelect(StockFilter.STOCK_BAJO) }
                    .testTag("stock_filter_stock_bajo"),
                color = if (isBajo) StatusBajoBg else GraphiteSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(StatusBajo)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Stock Bajo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isBajo) FontWeight.Bold else FontWeight.Medium,
                        color = if (isBajo) StatusBajo else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Categories Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = categories,
                key = { it }
            ) { category ->
                val isSelected = category.trim().equals(selectedCategory.trim(), ignoreCase = true)
                val isTodos = category.trim().equals("Todos", ignoreCase = true)
                val bgColor = if (isSelected) ElectricLime else GraphiteSurface
                val textColor = if (isSelected) OnElectricLime else TextPrimary
                val borderColor = if (isSelected) ElectricLime else GraphiteBorder

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onCategorySelect(category) },
                            onLongClick = if (!isTodos) {
                                {
                                    catalogToRename = category
                                    newCatalogName = category
                                }
                            } else null
                        )
                        .testTag("category_chip_$category"),
                    color = bgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isTodos) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = if (isSelected) OnElectricLime else ElectricLime,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Cards List
        if (isSyncing && products.isEmpty()) {
            InventoryListSkeleton(count = 6)
        } else if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No se encontraron productos",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Intenta buscar con otro término" else "Añade productos en la pestaña Entrada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = displayedProducts,
                    key = { it.id.ifBlank { "prod_${it.fila}_${it.producto}" } }
                ) { product ->
                    ProductCard(
                        product = product,
                        exchangeRate = exchangeRate,
                        onClick = { onProductClick(product) },
                        onQuickAdd = { onQuickAddToCart(product) }
                    )
                }

                if (visibleItemCount < products.size) {
                    item(key = "load_more_footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ElectricLime,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Mostrando ${displayedProducts.size} de ${products.size} productos...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation & Edit Dialog for Renaming / Merging Categories
    if (catalogToRename != null) {
        val currentOldName = catalogToRename ?: ""
        AlertDialog(
            onDismissRequest = {
                if (!isRenamingCategory) catalogToRename = null
            },
            containerColor = GraphiteSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Renombrar / Fusionar Catálogo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Todos los productos que pertenezcan al catálogo \"$currentOldName\" (sin importar mayúsculas o espacios) van a pasar al nuevo nombre que escribas a continuación:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newCatalogName,
                        onValueChange = { newCatalogName = it },
                        label = { Text("Nuevo nombre de catálogo", color = TextMuted) },
                        singleLine = true,
                        enabled = !isRenamingCategory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rename_catalog"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (newCatalogName.trim().isNotBlank() && !newCatalogName.trim().equals(currentOldName.trim(), ignoreCase = true)) {
                        val matchingTarget = categories.find { it.trim().equals(newCatalogName.trim(), ignoreCase = true) && !it.equals("Todos", ignoreCase = true) }
                        if (matchingTarget != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ℹ️ Se fusionará con el catálogo existente \"$matchingTarget\".",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatalogName.trim().isNotBlank()) {
                            onRenameCategory?.invoke(currentOldName, newCatalogName.trim())
                        }
                    },
                    enabled = !isRenamingCategory && newCatalogName.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = Color.Black,
                        disabledContainerColor = ElectricLime.copy(alpha = 0.4f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_rename_catalog")
                ) {
                    if (isRenamingCategory) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Text("Guardando...", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Confirmar y Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { catalogToRename = null },
                    enabled = !isRenamingCategory
                ) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
