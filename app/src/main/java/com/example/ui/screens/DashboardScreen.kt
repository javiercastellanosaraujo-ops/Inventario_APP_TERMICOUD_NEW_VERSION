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
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppConfig
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.StockFilter
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.StatusAgotado
import com.example.ui.theme.StatusAgotadoBg
import com.example.ui.theme.StatusBajo
import com.example.ui.theme.StatusBajoBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    activeUser: String,
    currentUserEmail: String = "",
    exchangeRate: Double,
    products: List<Product>,
    categoriesCount: Int,
    salesHistory: List<Sale>,
    isSyncing: Boolean,
    isAdmin: Boolean = false,
    pendingUsersCount: Int = 0,
    onNavigateToTab: (Int) -> Unit,
    onNavigateToStockAlerts: ((StockFilter) -> Unit)? = null,
    onUpdateProfile: (newName: String, newEmail: String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onOpenUserManagement: (() -> Unit)? = null,
    onOpenQuickScan: (() -> Unit)? = null,
    onSyncClick: () -> Unit,
    onViewFullHistory: () -> Unit
) {
    val totalProducts = products.size
    val totalStockUnits = products.sumOf { it.cantidad }
    val outOfStockCount = products.count { it.cantidad <= 0 }
    val lowStockCount = products.count { it.cantidad in 1..it.minStock }
    var showUserSessionDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var editName by remember(activeUser) { mutableStateOf(activeUser) }
    var editEmail by remember(currentUserEmail) { mutableStateOf(currentUserEmail) }

    // Dialog for Editing Profile
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Actualizar Perfil",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAdmin) "Rol: Administrador (Acceso Total)" else "Rol: Operador de Inventario",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre / Operador") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateProfile(editName.trim(), editEmail.trim())
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_save_profile")
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = GraphiteSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showUserSessionDialog) {
        AlertDialog(
            onDismissRequest = { showUserSessionDialog = false },
            title = {
                Text(
                    text = "Sesión Activa",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Usuario: $activeUser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentUserEmail.isNotBlank()) {
                        Text(
                            text = "Correo: $currentUserEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAdmin) "Rol: Administrador (Acceso Total)" else "Rol: Operador de Inventario",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricLime
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Deseas cerrar la sesión actual y salir?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUserSessionDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("btn_confirm_signout_dialog")
                ) {
                    Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserSessionDialog = false }) {
                    Text("Continuar", color = ElectricLime)
                }
            },
            containerColor = GraphiteSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Top Header: User Greeting & Action Bar with 3-Dots Menu
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = AppConfig.APP_FULL_TITLE,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hola, $activeUser",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 22.sp,
                            lineHeight = 26.sp
                        ),
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdmin && onOpenUserManagement != null) {
                        IconButton(
                            onClick = onOpenUserManagement,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GraphiteSurface)
                                .border(1.dp, if (pendingUsersCount > 0) ElectricLime else GraphiteBorder, RoundedCornerShape(10.dp))
                                .testTag("btn_user_mgmt_dashboard")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Gestión de Usuarios",
                                    tint = ElectricLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (pendingUsersCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(com.example.ui.theme.AlertRed)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_sync_dashboard")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ElectricLime,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Sincronizar",
                                tint = ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 3-Dots Menu Button for Profile & Options
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GraphiteSurface)
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                                .testTag("btn_more_menu_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más Opciones",
                                tint = ElectricLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(GraphiteSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Actualizar Perfil", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = ElectricLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    editName = activeUser
                                    editEmail = currentUserEmail
                                    showEditProfileDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_update_profile")
                            )

                            DropdownMenuItem(
                                text = { Text("Ver Sesión Activa", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showUserSessionDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_view_session")
                            )

                            DropdownMenuItem(
                                text = { Text("Sincronizar Catálogo", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onSyncClick()
                                },
                                modifier = Modifier.testTag("menu_item_sync")
                            )

                            if (isAdmin && onOpenUserManagement != null) {
                                DropdownMenuItem(
                                    text = { Text("Gestión de Usuarios", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = ElectricLime,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenUserManagement()
                                    },
                                    modifier = Modifier.testTag("menu_item_user_mgmt")
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onSignOut()
                                },
                                modifier = Modifier.testTag("menu_item_signout")
                            )
                        }
                    }
                }
            }
        }

        // Admin Stock Alert Banner (Only shown for Admin when low/out-of-stock items exist)
        if (isAdmin && (outOfStockCount > 0 || lowStockCount > 0)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (outOfStockCount > 0) AlertRed else WarningAmber, RoundedCornerShape(10.dp))
                        .testTag("admin_stock_alert_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (outOfStockCount > 0) StatusAgotadoBg else StatusBajoBg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (outOfStockCount > 0) AlertRed else WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )

                            Column {
                                val parts = mutableListOf<String>()
                                if (outOfStockCount > 0) parts.add("$outOfStockCount sin stock")
                                if (lowStockCount > 0) parts.add("$lowStockCount con stock bajo")
                                val textAlert = "⚠️ " + parts.joinToString(", ")

                                Text(
                                    text = textAlert,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Requiere reposición inmediata en inventario.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val targetFilter = if (outOfStockCount > 0) StockFilter.AGOTADOS else StockFilter.STOCK_BAJO
                                onNavigateToStockAlerts?.invoke(targetFilter) ?: onNavigateToTab(1)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (outOfStockCount > 0) AlertRed else WarningAmber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_view_stock_alerts")
                        ) {
                            Text("Ver detalle", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Summary Card: Total Products & Categories
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
                        text = "ESTADO DEL INVENTARIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$totalProducts",
                                style = MonoDataLarge
                            )
                            Text(
                                text = "Productos en catálogo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$categoriesCount",
                                style = MonoDataLarge.copy(color = TextPrimary)
                            )
                            Text(
                                text = "Catálogos / Categorías",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(GraphiteSurfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Stock Total: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = "$totalStockUnits un.",
                                style = MonoDataMedium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Agotados: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = "$outOfStockCount",
                                style = MonoDataMedium.copy(color = if (outOfStockCount > 0) MaterialTheme.colorScheme.error else ElectricLime)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Grid (Inventario, Salida rápida, Entrada, Tasa del día)
        item {
            Text(
                text = "ACCESOS RÁPIDOS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Inventario",
                        subtitle = "Ver y editar catálogo",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_inventario",
                        onClick = { onNavigateToTab(1) }
                    )

                    QuickActionCard(
                        title = "Salida Rápida",
                        subtitle = "Cobrar y descontar",
                        icon = Icons.Default.PointOfSale,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_salida",
                        isHighlight = true,
                        onClick = { onNavigateToTab(2) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Entrada",
                        subtitle = "+ Stock / Crear prod.",
                        icon = Icons.Default.AddBox,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_entrada",
                        onClick = { onNavigateToTab(3) }
                    )

                    QuickActionCard(
                        title = "Combos",
                        subtitle = "Recetas y combos",
                        icon = Icons.Default.Layers,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_combos",
                        onClick = { onNavigateToTab(4) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Ganancias",
                        subtitle = "Ventas por vendedor",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_ganancias",
                        onClick = { onNavigateToTab(5) }
                    )

                    QuickActionCard(
                        title = "Tasa del Día",
                        subtitle = String.format(Locale.US, "Bs %.2f", exchangeRate),
                        icon = Icons.Default.Savings,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_tasa",
                        onClick = { onNavigateToTab(6) }
                    )
                }

                if (onOpenQuickScan != null) {
                    QuickActionCard(
                        title = "Escáner",
                        subtitle = "Lector de barras",
                        icon = Icons.Default.QrCodeScanner,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "quick_action_scanner",
                        isHighlight = true,
                        onClick = onOpenQuickScan
                    )
                }
            }
        }

        // Recent Sales Section (Last 3-5 sales)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ÚLTIMAS VENTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                if (salesHistory.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .clickable { onViewFullHistory() }
                            .padding(4.dp),
                        color = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ver Historial",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (salesHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay ventas registradas en esta sesión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            val recentSales = salesHistory.take(5)
            items(recentSales) { sale ->
                RecentSaleItem(sale = sale)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isHighlight) ElectricLime else GraphiteBorder
    val bgColor = if (isHighlight) ElectricLime.copy(alpha = 0.08f) else GraphiteSurface

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isHighlight) ElectricLime else GraphiteBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) GraphiteSurface else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RecentSaleItem(sale: Sale) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(sale.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
        color = GraphiteSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.usuario,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${sale.items.sumOf { it.cantidad }} items: ${sale.items.joinToString(", ") { it.producto }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "$ %.2f", sale.totalUsd),
                    style = MonoDataMedium.copy(color = ElectricLime)
                )
                Text(
                    text = String.format(Locale.US, "Bs %.2f", sale.totalBs),
                    style = MonoDataSmall
                )
            }
        }
    }
}
