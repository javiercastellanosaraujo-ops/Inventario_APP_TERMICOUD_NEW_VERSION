package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppUser
import com.example.data.model.Product
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomSheetProductActions
import com.example.ui.components.SaleSuccessDialog
import com.example.ui.screens.BrandSplashScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExchangeRateScreen
import com.example.ui.screens.GananciasScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QuickScanScreen
import com.example.ui.screens.SaleScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.ScanMode
import com.example.ui.screens.StockEntryScreen
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.TermicoudTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TermicoudTheme {
                TermicoudApp()
            }
        }
    }
}

@Composable
fun TermicoudApp(viewModel: InventoryViewModel = viewModel()) {
    var showingBrandSplash by remember { mutableStateOf(true) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val exchangeRate by viewModel.exchangeRate.collectAsStateWithLifecycle()
    val isUserSelected by viewModel.isUserSelected.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val stockFilter by viewModel.stockFilter.collectAsStateWithLifecycle()

    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val combos by viewModel.combos.collectAsStateWithLifecycle()
    val salesHistory by viewModel.salesHistory.collectAsStateWithLifecycle()
    val movimientos by viewModel.movimientos.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val tasaActualizada by viewModel.tasaActualizada.collectAsStateWithLifecycle()
    val tasaUsuario by viewModel.tasaUsuario.collectAsStateWithLifecycle()

    val appUser by viewModel.appUser.collectAsStateWithLifecycle()
    val isUserApproved by viewModel.isUserApproved.collectAsStateWithLifecycle()
    val isCurrentUserAdmin by viewModel.isCurrentUserAdmin.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val isCheckingUserApproval by viewModel.isCheckingUserApproval.collectAsStateWithLifecycle()

    val gananciasActuales by viewModel.gananciasActuales.collectAsStateWithLifecycle()
    val historialMeses by viewModel.historialMeses.collectAsStateWithLifecycle()
    val gananciasMesArchivado by viewModel.gananciasMesArchivado.collectAsStateWithLifecycle()
    val selectedArchivedMonth by viewModel.selectedArchivedMonth.collectAsStateWithLifecycle()
    val isLoadingGanancias by viewModel.isLoadingGanancias.collectAsStateWithLifecycle()
    val backendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()

    val bottomSheetProduct by viewModel.selectedProductForBottomSheet.collectAsStateWithLifecycle()
    val completedSale by viewModel.completedSale.collectAsStateWithLifecycle()

    var showingFullHistoryScreen by remember { mutableStateOf(false) }
    var showingUserMgmtScreen by remember { mutableStateOf(false) }
    var showingQuickScanScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Success message toast
    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dismissSuccess()
        }
    }

    // Lazy load data for sub-screens on demand
    LaunchedEffect(showingFullHistoryScreen) {
        if (showingFullHistoryScreen) {
            viewModel.ensureSalesLoaded(limit = 100)
            viewModel.ensureMovementsLoaded(limit = 150)
        }
    }

    LaunchedEffect(showingUserMgmtScreen) {
        if (showingUserMgmtScreen) {
            viewModel.ensureAllUsersLoaded()
        }
    }

    // 0. Main Container with Animated Crossfade when Video Splash finishes
    Box(modifier = Modifier.fillMaxSize()) {
        // App Content (Login, Lock, or Main Dashboard)
        if (currentUser == null) {
            AnimatedVisibility(
                visible = !showingBrandSplash,
                enter = fadeIn(animationSpec = tween(700)),
                modifier = Modifier.fillMaxSize()
            ) {
                LoginScreen(
                    currentUser = currentUser,
                    onLoginSuccess = { session ->
                        viewModel.setUserSession(session)
                    },
                    onSignOut = {
                        viewModel.signOut()
                    }
                )
            }
        } else if (!isUserApproved) {
            AnimatedVisibility(
                visible = !showingBrandSplash,
                enter = fadeIn(animationSpec = tween(700)),
                modifier = Modifier.fillMaxSize()
            ) {
                com.example.ui.screens.UserApprovalLockScreen(
                    user = appUser ?: com.example.data.model.AppUser(
                        email = currentUser?.email ?: "",
                        nombre = currentUser?.displayName ?: "Operador",
                        estado = "pendiente"
                    ),
                    isCheckingStatus = isCheckingUserApproval,
                    onRefreshStatus = { viewModel.refreshUserStatus() },
                    onSignOut = { viewModel.signOut() }
                )
            }
        } else {
            AnimatedVisibility(
                visible = !showingBrandSplash,
                enter = fadeIn(animationSpec = tween(700)),
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!showingFullHistoryScreen && !showingUserMgmtScreen && !showingQuickScanScreen) {
                            BottomNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    viewModel.selectTab(tab)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showingUserMgmtScreen) {
                            com.example.ui.screens.UserManagementScreen(
                                users = allUsers,
                                onApproveUser = { targetEmail -> viewModel.approveUser(targetEmail) },
                                onRejectUser = { targetEmail -> viewModel.rejectUser(targetEmail) },
                                onBackClick = { showingUserMgmtScreen = false }
                            )
                        } else if (showingFullHistoryScreen) {
                            SalesHistoryScreen(
                                sales = salesHistory,
                                movimientos = movimientos,
                                onBackClick = { showingFullHistoryScreen = false },
                                onRevertSale = { ventaId ->
                                    viewModel.reversarVenta(ventaId)
                                },
                                onRevertMovimiento = { movId ->
                                    viewModel.reversarMovimiento(movId)
                                }
                            )
                        } else if (showingQuickScanScreen) {
                            QuickScanScreen(
                                products = products,
                                exchangeRate = exchangeRate,
                                onConfirmBatch = { mode, items ->
                                    val isEntrada = mode == ScanMode.ENTRADA
                                    viewModel.processQuickScanBatch(isEntrada, items)
                                    showingQuickScanScreen = false
                                },
                                onAddStock = { prod, qty -> viewModel.addStockToProduct(prod, qty) },
                                onDeductStock = { prod, qty -> viewModel.deductStockFromProduct(prod, qty) },
                                onAddBarcodeAlias = { prod, barcode -> viewModel.addBarcodeAliasToProduct(prod, barcode) },
                                onCreateNewProduct = { name, qty, price, cat, barcode -> viewModel.createNewProduct(name, qty, price, cat, barcode) },
                                onLookupOpenFoodFacts = { barcode, cb -> viewModel.lookupOpenFoodFacts(barcode, cb) },
                                onBackClick = { showingQuickScanScreen = false }
                            )
                        } else {
                            when (selectedTab) {
                                0 -> DashboardScreen(
                                    activeUser = currentUser?.displayName?.ifBlank { activeUser } ?: activeUser,
                                    currentUserEmail = currentUser?.email ?: "",
                                    exchangeRate = exchangeRate,
                                    products = products,
                                    categoriesCount = (categories.size - 1).coerceAtLeast(1),
                                    salesHistory = salesHistory,
                                    isSyncing = isSyncing,
                                    isAdmin = isCurrentUserAdmin,
                                    pendingUsersCount = allUsers.count { it.estado.equals("pendiente", ignoreCase = true) },
                                    onNavigateToTab = { tab -> viewModel.selectTab(tab) },
                                    onUpdateProfile = { newName, newEmail ->
                                        viewModel.updateUserProfile(newName, newEmail)
                                    },
                                    onSignOut = { viewModel.signOut() },
                                    onOpenUserManagement = { showingUserMgmtScreen = true },
                                    onOpenQuickScan = { showingQuickScanScreen = true },
                                    onSyncClick = { viewModel.syncFromRemote() },
                                    onViewFullHistory = { showingFullHistoryScreen = true }
                                )

                                1 -> InventoryScreen(
                                    products = filteredProducts,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    stockFilter = stockFilter,
                                    searchQuery = searchQuery,
                                    exchangeRate = exchangeRate,
                                    isSyncing = isSyncing,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onCategorySelect = { viewModel.setCategory(it) },
                                    onStockFilterSelect = { viewModel.setStockFilter(it) },
                                    onProductClick = { viewModel.openProductBottomSheet(it) },
                                    onQuickAddToCart = {
                                        viewModel.addToCart(it, 1)
                                        viewModel.selectTab(2) // Jump to Salida tab
                                    },
                                    onRefresh = { viewModel.syncInventoryCatalog() }
                                )

                                2 -> SaleScreen(
                                    products = products,
                                    combos = combos,
                                    cart = cart,
                                    exchangeRate = exchangeRate,
                                    searchQuery = searchQuery,
                                    isSyncing = isSyncing,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onAddToCart = { prod, qty, mode -> viewModel.addToCart(prod, qty, mode) },
                                    onAddComboToCart = { combo, qty -> viewModel.addComboToCart(combo, qty) },
                                    onUpdateCartQuantity = { fila, qty -> viewModel.updateCartQuantity(fila, qty) },
                                    onUpdateCartPriceMode = { fila, mode -> viewModel.updateCartItemPriceMode(fila, mode) },
                                    onRemoveFromCart = { fila -> viewModel.removeFromCart(fila) },
                                    onClearCart = { viewModel.clearCart() },
                                    onConfirmSale = { viewModel.confirmSale() },
                                    onOpenQuickScan = { showingQuickScanScreen = true }
                                )

                                3 -> StockEntryScreen(
                                    products = products,
                                    categories = categories,
                                    onAddStockToExisting = { prod, qty -> viewModel.addStockToProduct(prod, qty) },
                                    onCreateNewProduct = { name, qty, price, cat, barcode, precioMayor, cantMinima, precioCompra ->
                                        viewModel.createNewProduct(name, qty, price, cat, barcode, precioMayor, cantMinima, precioCompra)
                                    },
                                    onOpenQuickScan = { showingQuickScanScreen = true }
                                )

                                4 -> com.example.ui.screens.CombosScreen(
                                    combos = combos,
                                    products = products,
                                    exchangeRate = exchangeRate,
                                    isLoading = isSyncing,
                                    isAdmin = isCurrentUserAdmin,
                                    onRefresh = {
                                        viewModel.fetchCombos()
                                    },
                                    onAddComboToCart = { combo, qty ->
                                        viewModel.addComboToCart(combo, qty)
                                        viewModel.selectTab(2) // Jump to Salida
                                    },
                                    onCreateCombo = { nombre, precioUsd, categoria, componentes, onSuccess ->
                                        viewModel.crearCombo(nombre, precioUsd, categoria, componentes, onSuccess)
                                    },
                                    onDeleteCombo = { combo ->
                                        viewModel.eliminarCombo(combo)
                                    }
                                )

                                5 -> GananciasScreen(
                                    gananciasActuales = gananciasActuales,
                                    historialMeses = historialMeses,
                                    gananciasMesArchivado = gananciasMesArchivado,
                                    selectedArchivedMonth = selectedArchivedMonth,
                                    salesHistory = salesHistory,
                                    exchangeRate = exchangeRate,
                                    isLoading = isLoadingGanancias,
                                    onRefresh = {
                                        viewModel.fetchGanancias()
                                        viewModel.fetchHistorialMeses()
                                    },
                                    onSelectArchivedMonth = { mesKey -> viewModel.selectArchivedMonth(mesKey) },
                                    onClearSelectedArchivedMonth = { viewModel.clearSelectedArchivedMonth() }
                                )

                                6 -> ExchangeRateScreen(
                                    exchangeRate = exchangeRate,
                                    activeUser = currentUser?.displayName?.ifBlank { activeUser } ?: activeUser,
                                    userEmail = currentUser?.email ?: "",
                                    backendUrl = backendUrl,
                                    isSyncing = isSyncing,
                                    tasaActualizada = tasaActualizada,
                                    tasaUsuario = tasaUsuario,
                                    onRefreshTasa = { viewModel.fetchExchangeRateFromBackend() },
                                    onSaveExchangeRate = { viewModel.setExchangeRate(it) },
                                    onSaveBackendUrl = { viewModel.setBackendUrl(it) },
                                    onSyncAll = { viewModel.syncFromRemote() },
                                    onSignOut = { viewModel.signOut() }
                                )
                            }
                        }

                        // Error Banner with Retry button
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            errorMessage?.let { msg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row {
                                            TextButton(
                                                onClick = { viewModel.syncFromRemote() },
                                                modifier = Modifier.testTag("btn_error_retry")
                                            ) {
                                                Text("Reintentar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = { viewModel.dismissError() },
                                                modifier = Modifier.testTag("btn_error_dismiss")
                                            ) {
                                                Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom sheet for editing product stock/price
                        bottomSheetProduct?.let { product ->
                            BottomSheetProductActions(
                                product = product,
                                exchangeRate = exchangeRate,
                                isAdmin = isCurrentUserAdmin,
                                onDismiss = { viewModel.dismissBottomSheet() },
                                onUpdateProduct = { prod, cant, price, barcode, precioMayor, cantMinima, precioCompra ->
                                    viewModel.updateProductStockPriceAndBarcode(prod, cant, price, barcode, precioMayor, cantMinima, precioCompra)
                                },
                                onAddToCartAndGoToSale = { prod, qty ->
                                    viewModel.addToCart(prod, qty)
                                    viewModel.selectTab(2) // Jump to Salida tab
                                },
                                onDeleteProduct = { prod ->
                                    viewModel.deleteProduct(prod)
                                }
                            )
                        }

                        // Sale Completed Confirmation Overlay
                        completedSale?.let { sale ->
                            SaleSuccessDialog(
                                sale = sale,
                                onDismiss = { viewModel.dismissCompletedSale() }
                            )
                        }
                    }
                }
            }
        }

        // Overlay Splash Video on top with smooth fade-out
        if (showingBrandSplash) {
            BrandSplashScreen(onFinished = { showingBrandSplash = false })
        }
    }
}
