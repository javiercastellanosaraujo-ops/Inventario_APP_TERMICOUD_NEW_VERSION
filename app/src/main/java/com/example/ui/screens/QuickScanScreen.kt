package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.findSimilarProductByName
import com.example.data.model.getAllBarcodesList
import com.example.data.model.matchesBarcode
import com.example.data.remote.OpenFoodFactsResult
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.AlertRed
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
import kotlinx.coroutines.launch
import java.util.Locale

enum class ScanMode {
    ENTRADA, // Sumar al inventario
    SALIDA   // Restar del inventario
}

data class ScannedItemState(
    val product: Product,
    var quantity: Int = 1
)

@Composable
fun QuickScanScreen(
    products: List<Product>,
    exchangeRate: Double,
    onConfirmBatch: (mode: ScanMode, items: List<Pair<Product, Int>>) -> Unit,
    onAddStock: (product: Product, quantity: Int) -> Unit = { _, _ -> },
    onDeductStock: (product: Product, quantity: Int) -> Unit = { _, _ -> },
    onAddBarcodeAlias: (product: Product, barcode: String) -> Unit = { _, _ -> },
    onCreateNewProduct: (name: String, quantity: Int, price: Double, category: String, barcode: String) -> Unit = { _, _, _, _, _ -> },
    onLookupOpenFoodFacts: (barcode: String, callback: (OpenFoodFactsResult) -> Unit) -> Unit = { _, _ -> },
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: Escanear Producto Directo (con flujo inteligente de alias y Open Food Facts), 1: Modo Lote / Ráfaga
    var activeTab by remember { mutableIntStateOf(0) }

    // Live Scanner overlay
    var showCameraScanner by remember { mutableStateOf(false) }
    var currentScannedCode by remember { mutableStateOf("") }

    // Active Card Modal State after scanning
    var scannedProductMatch by remember { mutableStateOf<Product?>(null) }
    var similarProductCandidate by remember { mutableStateOf<Product?>(null) }
    var isLookingUpOpenFoodFacts by remember { mutableStateOf(false) }
    var openFoodFactsResult by remember { mutableStateOf<OpenFoodFactsResult?>(null) }
    var showCreateProductModal by remember { mutableStateOf(false) }

    // Direct Action Quantity
    var actionQuantity by remember { mutableIntStateOf(1) }

    // Batch scan states
    var batchScanMode by remember { mutableStateOf(ScanMode.SALIDA) }
    var batchScannedQueue by remember { mutableStateOf<List<ScannedItemState>>(emptyList()) }
    var batchLastMessage by remember { mutableStateOf<String?>(null) }
    var batchLastIsError by remember { mutableStateOf(false) }

    fun triggerFeedback(isSuccess: Boolean) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            if (isSuccess) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } else {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            }
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(if (isSuccess) 80 else 250, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(if (isSuccess) 80 else 250, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(if (isSuccess) 80 else 250)
                }
            }
        } catch (_: Exception) {}
    }

    // Process Direct Scan Workflow
    fun handleDirectBarcodeScanned(rawCode: String) {
        val cleanCode = rawCode.trim()
        if (cleanCode.isBlank()) return
        currentScannedCode = cleanCode
        actionQuantity = 1

        // 1. Check if scanned code is in any product's comma-separated barcodes or codigo
        val match = products.firstOrNull { it.matchesBarcode(cleanCode) }

        if (match != null) {
            triggerFeedback(true)
            scannedProductMatch = match
            similarProductCandidate = null
            showCameraScanner = false
        } else {
            // 2. Not found in barcodes: Check if a similar product by name exists in inventory
            val candidate = products.findSimilarProductByName(cleanCode)
            if (candidate != null) {
                triggerFeedback(true)
                similarProductCandidate = candidate
                scannedProductMatch = null
                showCameraScanner = false
            } else {
                // 3. No candidate in inventory: Fetch Open Food Facts
                triggerFeedback(false)
                showCameraScanner = false
                isLookingUpOpenFoodFacts = true
                similarProductCandidate = null
                scannedProductMatch = null

                onLookupOpenFoodFacts(cleanCode) { result ->
                    isLookingUpOpenFoodFacts = false
                    openFoodFactsResult = result
                    showCreateProductModal = true
                }
            }
        }
    }

    // Process Batch Scan Workflow
    fun handleBatchBarcodeScanned(rawCode: String) {
        val cleanCode = rawCode.trim()
        if (cleanCode.isBlank()) return

        val matchedProduct = products.firstOrNull { it.matchesBarcode(cleanCode) }
            ?: products.firstOrNull { it.producto.equals(cleanCode, ignoreCase = true) }

        if (matchedProduct != null) {
            triggerFeedback(true)
            batchLastIsError = false
            batchLastMessage = "✓ ${matchedProduct.producto} (+1)"

            val existingIndex = batchScannedQueue.indexOfFirst {
                (it.product.id.isNotBlank() && it.product.id == matchedProduct.id) ||
                (it.product.fila > 0 && it.product.fila == matchedProduct.fila)
            }

            if (existingIndex >= 0) {
                val current = batchScannedQueue[existingIndex]
                val newQty = current.quantity + 1
                if (batchScanMode == ScanMode.SALIDA && matchedProduct.cantidad > 0 && newQty > matchedProduct.cantidad) {
                    batchLastIsError = true
                    batchLastMessage = "⚠️ Stock máximo alcanzado (${matchedProduct.cantidad} un.)"
                    triggerFeedback(false)
                } else {
                    batchScannedQueue = batchScannedQueue.toMutableList().also {
                        it[existingIndex] = current.copy(quantity = newQty)
                    }
                }
            } else {
                batchScannedQueue = batchScannedQueue + ScannedItemState(product = matchedProduct, quantity = 1)
            }
        } else {
            triggerFeedback(false)
            batchLastIsError = true
            batchLastMessage = "❌ Código '$cleanCode' no encontrado en catálogo"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = GraphiteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_back_quick_scan")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CENTRO DE ESCANEO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Reconocimiento óptico y gestión por códigos de barra",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricLime
                        )
                    }

                    Button(
                        onClick = { showCameraScanner = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_camera_scanner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Escanear",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = GraphiteSurface,
                    contentColor = ElectricLime,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = ElectricLime,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "ESCANEO DIRECTO",
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 0) ElectricLime else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "MODO LOTE / RÁFAGA",
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 1) ElectricLime else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // Main Tab Content
        if (activeTab == 0) {
            // TAB 0: ESCANEO DIRECTO / PRODUCT CARD / ALIAS WORKFLOW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(ElectricLime.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricLime.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = ElectricLime,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Escáner Inteligente con Alias",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reconoce múltiples códigos por producto (lotes). Si no existe, sugiere alias similares o consulta Open Food Facts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Quick Launch Card
                item {
                    Button(
                        onClick = { showCameraScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_start_direct_scan"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ABRIR CÁMARA PARA ESCANEAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }

                // If a product is currently displayed / matched
                if (scannedProductMatch != null) {
                    val prod = scannedProductMatch!!
                    item {
                        DirectProductCardView(
                            product = prod,
                            exchangeRate = exchangeRate,
                            quantity = actionQuantity,
                            onQuantityChange = { actionQuantity = it },
                            onDeductClick = {
                                onDeductStock(prod, actionQuantity)
                                scannedProductMatch = prod.copy(cantidad = (prod.cantidad - actionQuantity).coerceAtLeast(0))
                            },
                            onAddStockClick = {
                                onAddStock(prod, actionQuantity)
                                scannedProductMatch = prod.copy(cantidad = prod.cantidad + actionQuantity)
                            },
                            onScanNext = {
                                scannedProductMatch = null
                                showCameraScanner = true
                            },
                            onDismiss = { scannedProductMatch = null }
                        )
                    }
                }

                // Inventory Quick Overview
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRODUCTOS CON CÓDIGOS ASOCIADOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${products.count { it.codigoBarras.isNotBlank() || it.codigo.isNotBlank() }} registrados",
                            style = MonoDataSmall.copy(color = ElectricLime)
                        )
                    }
                }

                val prodsWithBarcodes = products.filter { it.codigoBarras.isNotBlank() || it.codigo.isNotBlank() }.take(10)
                items(prodsWithBarcodes) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scannedProductMatch = prod
                                actionQuantity = 1
                            },
                        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.producto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val barcodes = prod.getAllBarcodesList()
                                Text(
                                    text = if (barcodes.size > 1) "${barcodes.size} códigos: ${barcodes.joinToString(", ")}" else "Cód: ${barcodes.firstOrNull() ?: prod.codigoBarras}",
                                    style = MonoDataSmall.copy(color = TextMuted, fontSize = 11.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${prod.cantidad} un.",
                                    style = MonoDataMedium.copy(
                                        color = if (prod.cantidad <= 0) AlertRed else if (prod.cantidad <= 5) Color(0xFFFFB300) else ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = String.format(Locale.US, "$ %.2f", prod.precioUsd),
                                    style = MonoDataSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: MODO LOTE / RÁFAGA
            Column(modifier = Modifier.fillMaxSize()) {
                // Mode selector (Entrada vs Salida)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { batchScanMode = ScanMode.SALIDA },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_scan_mode_salida"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (batchScanMode == ScanMode.SALIDA) AlertRed else Color.White,
                            contentColor = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "LOTE: SALIDA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        )
                    }

                    Button(
                        onClick = { batchScanMode = ScanMode.ENTRADA },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_scan_mode_entrada"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (batchScanMode == ScanMode.ENTRADA) ElectricLime else Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "LOTE: ENTRADA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }

                // Launch Camera for batch scan
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { showCameraScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_open_scanner_dialog"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (batchScanMode == ScanMode.SALIDA) AlertRed else ElectricLime,
                            contentColor = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (batchScanMode == ScanMode.SALIDA) "ESCANEAR RÁFAGA (SALIDA)" else "ESCANEAR RÁFAGA (ENTRADA)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                        )
                    }
                }

                // Last scan status feedback
                AnimatedVisibility(
                    visible = batchLastMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (batchLastIsError) AlertRed.copy(alpha = 0.15f) else ElectricLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (batchLastIsError) AlertRed.copy(alpha = 0.5f) else ElectricLime.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (batchLastIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (batchLastIsError) AlertRed else ElectricLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = batchLastMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (batchLastIsError) AlertRed else ElectricLime,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Scanned Queue List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (batchScannedQueue.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Cola de escaneo vacía",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Presiona 'Escanear Ráfaga' para registrar múltiples códigos seguidos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(batchScannedQueue) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.producto,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Stock actual: ${item.product.cantidad} un. • $ ${String.format(Locale.US, "%.2f", item.product.precioUsd)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    // Quantity Selector
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (item.quantity > 1) {
                                                    batchScannedQueue = batchScannedQueue.map {
                                                        if (it.product.id == item.product.id) it.copy(quantity = it.quantity - 1) else it
                                                    }
                                                } else {
                                                    batchScannedQueue = batchScannedQueue.filter { it.product.id != item.product.id }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurfaceVariant)
                                                .border(1.dp, GraphiteBorder, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                                contentDescription = "Restar",
                                                tint = if (item.quantity == 1) AlertRed else TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 44.dp)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurfaceVariant)
                                                .border(1.dp, ElectricLime.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${item.quantity}",
                                                style = MonoDataMedium.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (batchScanMode == ScanMode.SALIDA && item.product.cantidad > 0 && item.quantity >= item.product.cantidad) {
                                                    // Max stock reached
                                                } else {
                                                    batchScannedQueue = batchScannedQueue.map {
                                                        if (it.product.id == item.product.id) it.copy(quantity = it.quantity + 1) else it
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GraphiteSurfaceVariant)
                                                .border(1.dp, GraphiteBorder, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Sumar",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Batch Action Bar
                if (batchScannedQueue.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (batchScanMode == ScanMode.SALIDA) AlertRed else ElectricLime, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val totalUnits = batchScannedQueue.sumOf { it.quantity }
                            val totalUsd = batchScannedQueue.sumOf { it.product.precioUsd * it.quantity }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "TOTAL REGISTRO:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(text = "$totalUnits unidades • ${batchScannedQueue.size} productos", style = MonoDataMedium.copy(color = TextPrimary))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "VALOR REF:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        text = String.format(Locale.US, "$ %.2f USD", totalUsd),
                                        style = MonoDataLarge.copy(color = ElectricLime, fontSize = 20.sp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val items = batchScannedQueue.map { it.product to it.quantity }
                                    onConfirmBatch(batchScanMode, items)
                                    batchScannedQueue = emptyList()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_confirm_quick_scan_batch"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (batchScanMode == ScanMode.SALIDA) AlertRed else ElectricLime,
                                    contentColor = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (batchScanMode == ScanMode.SALIDA) "CONFIRMAR SALIDA / DESPACHO ($totalUnits un.)" else "CONFIRMAR ENTRADA / REPOSICIÓN ($totalUnits un.)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (batchScanMode == ScanMode.SALIDA) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Camera Live Scanner Dialog
    if (showCameraScanner) {
        BarcodeScannerDialog(
            title = if (activeTab == 0) "Escanear Producto" else if (batchScanMode == ScanMode.SALIDA) "Escaneo Lote - Salida" else "Escaneo Lote - Entrada",
            subtitle = "Apunta el recuadro hacia el código de barras o QR",
            onDismiss = { showCameraScanner = false },
            onBarcodeScanned = { scannedCode ->
                if (activeTab == 0) {
                    handleDirectBarcodeScanned(scannedCode)
                } else {
                    handleBatchBarcodeScanned(scannedCode)
                }
            }
        )
    }

    // Modal: Similar Product Confirmation (Alias suggestion)
    if (similarProductCandidate != null) {
        val candidate = similarProductCandidate!!
        Dialog(
            onDismissRequest = { similarProductCandidate = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = GraphiteSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ElectricLime.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Producto Parecido Encontrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Encontré un producto parecido en inventario:\n\"${candidate.producto}\"\n\n¿Es el mismo producto con otro código de lote o presentación?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Código escaneado:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(currentScannedCode, style = MonoDataMedium.copy(color = ElectricLime))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // User said NO -> Trigger Open Food Facts lookup
                                similarProductCandidate = null
                                isLookingUpOpenFoodFacts = true
                                onLookupOpenFoodFacts(currentScannedCode) { result ->
                                    isLookingUpOpenFoodFacts = false
                                    openFoodFactsResult = result
                                    showCreateProductModal = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("No, es otro", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                // User said YES -> Associate alias and open product card
                                onAddBarcodeAlias(candidate, currentScannedCode)
                                val updatedBarcodes = if (candidate.codigoBarras.isBlank()) currentScannedCode else "${candidate.codigoBarras}, $currentScannedCode"
                                val updatedProd = candidate.copy(codigoBarras = updatedBarcodes)
                                similarProductCandidate = null
                                scannedProductMatch = updatedProd
                                actionQuantity = 1
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sí, asociar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // Modal: Open Food Facts Lookup Loading
    if (isLookingUpOpenFoodFacts) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = GraphiteSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = ElectricLime, modifier = Modifier.size(40.dp))
                    Text(
                        text = "Consultando Open Food Facts...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Buscando información global para el código $currentScannedCode",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modal: Create New Product Form (with Open Food Facts prefill or manual note)
    if (showCreateProductModal) {
        CreateProductModalDialog(
            barcode = currentScannedCode,
            openFoodFactsResult = openFoodFactsResult,
            onDismiss = {
                showCreateProductModal = false
                openFoodFactsResult = null
            },
            onSaveProduct = { name, qty, price, cat ->
                onCreateNewProduct(name, qty, price, cat, currentScannedCode)
                showCreateProductModal = false
                openFoodFactsResult = null
                // Open newly created product card directly
                scannedProductMatch = Product(
                    producto = name,
                    cantidad = qty,
                    precioUsd = price,
                    catalogo = cat,
                    codigoBarras = currentScannedCode
                )
                actionQuantity = 1
            }
        )
    }
}

/**
 * High-Impact Product Card View with Direct "Sacar" and "Agregar stock" Actions
 */
@Composable
private fun DirectProductCardView(
    product: Product,
    exchangeRate: Double,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onDeductClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onScanNext: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricLime)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = ElectricLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.catalogo.uppercase(),
                            style = MonoDataSmall.copy(color = ElectricLime, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.producto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GraphiteSurfaceVariant)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Price and Stock Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PRECIO UNITARIO:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "$ %.2f", product.precioUsd),
                            style = MonoDataLarge.copy(color = ElectricLime, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "/ Bs %.2f", product.precioUsd * exchangeRate),
                            style = MonoDataSmall.copy(color = TextSecondary),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("DISPONIBILIDAD:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Surface(
                        color = if (product.cantidad <= 0) AlertRed.copy(alpha = 0.2f) else if (product.cantidad <= 5) Color(0xFFFFB300).copy(alpha = 0.2f) else ElectricLime.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${product.cantidad} un. en stock",
                            style = MonoDataMedium.copy(
                                color = if (product.cantidad <= 0) AlertRed else if (product.cantidad <= 5) Color(0xFFFFB300) else ElectricLime,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Barcodes Info Chips
            val allCodes = product.getAllBarcodesList()
            if (allCodes.isNotEmpty()) {
                Column {
                    Text("CÓDIGOS ASOCIADOS (${allCodes.size}):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allCodes) { code ->
                            Surface(
                                color = GraphiteSurfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                            ) {
                                Text(
                                    text = code,
                                    style = MonoDataSmall.copy(color = TextSecondary, fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = GraphiteBorder, thickness = 1.dp)

            // Quantity Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cantidad de la operación:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange((quantity - 1).coerceAtLeast(1)) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$quantity",
                            style = MonoDataMedium.copy(color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = { onQuantityChange(quantity + 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Action Buttons: "Sacar" & "Agregar stock"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDeductClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_direct_deduct_stock"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SACAR ($quantity)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = onAddStockClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_direct_add_stock"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AGREGAR STOCK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                }
            }

            // Scan Next Button
            Button(
                onClick = onScanNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear Otro Producto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Modal Dialog to Create a New Product (with Open Food Facts prefill or manual note)
 */
@Composable
private fun CreateProductModalDialog(
    barcode: String,
    openFoodFactsResult: OpenFoodFactsResult?,
    onDismiss: () -> Unit,
    onSaveProduct: (name: String, quantity: Int, price: Double, category: String) -> Unit
) {
    val isFoundOnline = openFoodFactsResult?.found == true && openFoodFactsResult.productName.isNotBlank()

    var nameInput by remember { mutableStateOf(if (isFoundOnline) openFoodFactsResult!!.productName else "") }
    var quantityInput by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf(if (isFoundOnline && openFoodFactsResult!!.categories.isNotBlank()) openFoodFactsResult.categories.split(",").first().trim() else "General") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = GraphiteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFoundOnline) Icons.Default.AutoAwesome else Icons.Default.AddBox,
                            contentDescription = null,
                            tint = ElectricLime
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear Producto Nuevo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextPrimary)
                    }
                }

                // Info Banner
                if (isFoundOnline) {
                    Surface(
                        color = ElectricLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Nombre sugerido por Open Food Facts. Puedes editarlo antes de guardar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricLime,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = "No lo encontré en internet, escribe el nombre tú mismo",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Barcode Chip
                Surface(
                    color = GraphiteSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Código de barras:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(barcode, style = MonoDataMedium.copy(color = ElectricLime))
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre del Producto *", color = TextSecondary) },
                    placeholder = { Text("Ej. Harina PAN 1kg", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = GraphiteBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quantity and Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Cantidad", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Precio USD", color = TextSecondary) },
                        placeholder = { Text("0.00", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category
                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Catálogo / Categoría", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = GraphiteBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Cancelar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                val qty = quantityInput.toIntOrNull() ?: 1
                                val price = priceInput.toDoubleOrNull() ?: 0.0
                                onSaveProduct(nameInput.trim(), qty, price, categoryInput.trim())
                            }
                        },
                        enabled = nameInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
