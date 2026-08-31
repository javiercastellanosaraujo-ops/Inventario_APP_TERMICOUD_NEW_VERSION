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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import com.example.ui.components.SaleCardSkeleton
import com.example.ui.components.TermiCoudDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.OnElectricLime
import com.example.ui.theme.StatusAgotado
import com.example.ui.theme.StatusAgotadoBg
import com.example.ui.theme.StatusBajo
import com.example.ui.theme.StatusBajoBg
import com.example.ui.theme.StatusOk
import com.example.ui.theme.StatusOkBg
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
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
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
    var showTermiCoudDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    if (showTermiCoudDialog) {
        com.example.ui.components.TermiCoudDialog(
            onDismissRequest = { showTermiCoudDialog = false }
        )
    }

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
                    Text("Guardar Cambios", color = OnElectricLime, fontWeight = FontWeight.Bold)
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

                    // Theme Toggle Button (Light Azul Cielo / Dark Graphite)
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                            .testTag("btn_toggle_theme_dashboard")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro",
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
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
                                text = {
                                    Text(
                                        text = if (isDarkMode) "Modo Claro (Azul Cielo)" else "Modo Oscuro (Grafito)",
                                        color = TextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = ElectricLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleDarkMode()
                                },
                                modifier = Modifier.testTag("menu_item_toggle_theme")
                            )

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
                                text = { Text("Términos (TermiCoud)", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = ElectricLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showTermiCoudDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_termicoud")
                            )

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

        // Summary Card: Total Products & Categories - 4-Stat Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTADO DEL INVENTARIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(StatusOk)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1 USD = Bs. ${String.format(Locale.US, "%.2f", exchangeRate)}",
                            style = MonoDataSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                // 2x2 High-Impact Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Total Productos
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp)),
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElectricLime.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = ElectricLime,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "$categoriesCount cat.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$totalProducts",
                                style = MonoDataLarge.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Productos en catálogo",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Card 2: Total Unidades
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp)),
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusOkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusOk,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$totalStockUnits",
                                style = MonoDataLarge.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.Black,
                                color = StatusOk
                            )
                            Text(
                                text = "Unidades físicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 3: Stock Bajo
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (lowStockCount > 0) StatusBajo.copy(alpha = 0.5f) else GraphiteBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (lowStockCount > 0) {
                                    onNavigateToStockAlerts?.invoke(StockFilter.STOCK_BAJO) ?: onNavigateToTab(1)
                                }
                            },
                        color = if (lowStockCount > 0) StatusBajoBg else GraphiteSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (lowStockCount > 0) StatusBajo.copy(alpha = 0.2f) else GraphiteSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (lowStockCount > 0) StatusBajo else TextMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                if (lowStockCount > 0) {
                                    Text(
                                        text = "Atención",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusBajo
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$lowStockCount",
                                style = MonoDataLarge.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.Black,
                                color = if (lowStockCount > 0) StatusBajo else TextPrimary
                            )
                            Text(
                                text = "Stock Bajo",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Card 4: Agotados
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (outOfStockCount > 0) StatusAgotado.copy(alpha = 0.5f) else GraphiteBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (outOfStockCount > 0) {
                                    onNavigateToStockAlerts?.invoke(StockFilter.AGOTADOS) ?: onNavigateToTab(1)
                                }
                            },
                        color = if (outOfStockCount > 0) StatusAgotadoBg else GraphiteSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (outOfStockCount > 0) StatusAgotado.copy(alpha = 0.2f) else GraphiteSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (outOfStockCount > 0) StatusAgotado else TextMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                if (outOfStockCount > 0) {
                                    Text(
                                        text = "Crítico",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAgotado
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$outOfStockCount",
                                style = MonoDataLarge.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.Black,
                                color = if (outOfStockCount > 0) StatusAgotado else TextPrimary
                            )
                            Text(
                                text = "Sin Existencias",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
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

                if (isAdmin) {
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
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "Tasa del Día",
                            subtitle = String.format(Locale.US, "Bs %.2f", exchangeRate),
                            icon = Icons.Default.Savings,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_tasa",
                            onClick = { onNavigateToTab(6) }
                        )
                    }
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

        if (isSyncing && salesHistory.isEmpty()) {
            items(3) {
                SaleCardSkeleton()
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (salesHistory.isEmpty()) {
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
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isHighlight) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isHighlight) ElectricLime else GraphiteSurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) OnElectricLime else ElectricLime,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp)),
        color = GraphiteSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sale.usuario,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $dateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${sale.items.sumOf { it.cantidad }} items: ${sale.items.joinToString(", ") { it.producto }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sale Total Block
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GraphiteSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = GraphiteSurfaceVariant
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$ %.2f", sale.totalUsd),
                        style = MonoDataMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricLime
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "Bs %.2f", sale.totalBs),
                        style = MonoDataSmall.copy(fontSize = 10.5.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
