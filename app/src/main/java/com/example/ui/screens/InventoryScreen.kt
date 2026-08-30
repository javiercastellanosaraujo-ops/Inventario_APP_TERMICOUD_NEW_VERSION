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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.Product
import com.example.data.model.StockFilter
import com.example.ui.components.InventoryListSkeleton
import com.example.ui.components.ProductCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.StatusAgotado
import com.example.ui.theme.StatusAgotadoBg
import com.example.ui.theme.StatusBajo
import com.example.ui.theme.StatusBajoBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun InventoryScreen(
    products: List<Product>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    stockFilter: StockFilter = StockFilter.TODOS,
    exchangeRate: Double,
    isSyncing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onStockFilterSelect: (StockFilter) -> Unit = {},
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit,
    onRefresh: () -> Unit
) {
    // Preserve scroll state across tab switches
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

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
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODOS
            val isTodos = stockFilter == StockFilter.TODOS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isTodos) ElectricLime else GraphiteSurface)
                    .border(1.dp, if (isTodos) ElectricLime else GraphiteBorder, RoundedCornerShape(6.dp))
                    .clickable { onStockFilterSelect(StockFilter.TODOS) }
                    .padding(vertical = 7.dp)
                    .testTag("stock_filter_todos"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todos",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isTodos) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTodos) Color.Black else TextSecondary
                )
            }

            // AGOTADOS
            val isAgotados = stockFilter == StockFilter.AGOTADOS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAgotados) ElectricLime else GraphiteSurface)
                    .border(1.dp, if (isAgotados) ElectricLime else GraphiteBorder, RoundedCornerShape(6.dp))
                    .clickable { onStockFilterSelect(StockFilter.AGOTADOS) }
                    .padding(vertical = 7.dp)
                    .testTag("stock_filter_agotados"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Agotados",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isAgotados) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAgotados) Color.Black else TextSecondary
                )
            }

            // STOCK BAJO
            val isBajo = stockFilter == StockFilter.STOCK_BAJO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isBajo) ElectricLime else GraphiteSurface)
                    .border(1.dp, if (isBajo) ElectricLime else GraphiteBorder, RoundedCornerShape(6.dp))
                    .clickable { onStockFilterSelect(StockFilter.STOCK_BAJO) }
                    .padding(vertical = 7.dp)
                    .testTag("stock_filter_stock_bajo"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Stock Bajo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isBajo) FontWeight.Bold else FontWeight.Medium,
                    color = if (isBajo) Color.Black else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Categories Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                val bgColor = if (isSelected) ElectricLime else GraphiteSurface
                val textColor = if (isSelected) Color.Black else TextPrimary
                val borderColor = if (isSelected) ElectricLime else GraphiteBorder

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                        .clickable { onCategorySelect(category) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("category_chip_$category"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
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
}
