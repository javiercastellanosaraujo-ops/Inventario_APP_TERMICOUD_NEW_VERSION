package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.OnElectricLime
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ExchangeRateScreen(
    exchangeRate: Double,
    activeUser: String,
    userEmail: String = "",
    backendUrl: String = "",
    tasaActualizada: String? = null,
    tasaUsuario: String? = null,
    isSyncing: Boolean,
    isDarkMode: Boolean = false,
    onSetDarkMode: (Boolean) -> Unit = {},
    onSaveExchangeRate: (Double) -> Unit,
    onRefreshTasa: () -> Unit = {},
    onSaveBackendUrl: (String) -> Unit = {},
    onSyncAll: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    var rateInputText by remember(exchangeRate) {
        mutableStateOf(String.format(Locale.US, "%.2f", exchangeRate))
    }
    var backendUrlInput by remember(backendUrl) {
        mutableStateOf(backendUrl)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onRefreshTasa()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Section Title
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
                    Icon(Icons.Default.Savings, contentDescription = null, tint = ElectricLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TASA DEL DÍA Y AJUSTES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onRefreshTasa,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("btn_refresh_tasa")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricLime,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar Tasa", tint = ElectricLime)
                    }
                }
            }
        }

        // Section 1: Exchange Rate Large Display & Input
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ElectricLime, RoundedCornerShape(10.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TASA COMPARTIDA (Bs por USD)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricLime,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Sincronizada en la nube",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format(Locale.US, "Bs %.2f", exchangeRate),
                        style = MonoDataLarge.copy(fontSize = 32.sp)
                    )

                    if (!tasaActualizada.isNullOrBlank() || !tasaUsuario.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GraphiteSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = buildString {
                                    append("Última actualización: ")
                                    if (!tasaActualizada.isNullOrBlank()) {
                                        // Try to format nice date if ISO or raw
                                        val displayDate = try {
                                            if (tasaActualizada.contains("T")) {
                                                tasaActualizada.replace("T", " ").take(19)
                                            } else {
                                                tasaActualizada
                                            }
                                        } catch (e: Exception) {
                                            tasaActualizada
                                        }
                                        append(displayDate)
                                    } else {
                                        append("Reciente")
                                    }
                                    if (!tasaUsuario.isNullOrBlank()) {
                                        append(" por $tasaUsuario")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "Esta tasa compartida se utiliza para calcular automáticamente los importes en Bs en todo el inventario, catálogo, salidas y ganancias.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                    )

                    Text(
                        text = "EDITAR TASA DEL DÍA (PARA TODOS):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = rateInputText,
                            onValueChange = { rateInputText = it },
                            prefix = { Text("Bs ", color = ElectricLime) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rate_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = GraphiteBorder,
                                focusedContainerColor = GraphiteSurfaceVariant,
                                unfocusedContainerColor = GraphiteSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                val parsed = rateInputText.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    onSaveExchangeRate(parsed)
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("btn_save_rate"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = OnElectricLime
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = OnElectricLime)
                        }
                    }
                }
            }
        }

        // Section 2: Real-time Cloud Database Status (Firebase Firestore)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BASE DE DATOS EN LA NUBE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Live status indicator dot
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ElectricLime)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Conectado (Firestore)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "El inventario, historial de salidas y entradas se sincronizan en tiempo real directamente con Firebase Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSyncAll,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_sync_firestore_settings"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = OnElectricLime
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OnElectricLime,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando...", color = OnElectricLime, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OnElectricLime
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refrescar Datos de Firestore", color = OnElectricLime, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 2.5: Google Apps Script Backend URL (Ganancias & Sheets API)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "URL DEL BACKEND (GOOGLE APPS SCRIPT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Endpoint Web App para sincronizar ganancias mensuales, tasa compartida y respaldo automático de Notas de Entrega (PDF) en tu Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_backend_url"),
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricLime
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveBackendUrl(backendUrlInput.trim()) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_save_backend_url"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = OnElectricLime
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar URL", fontWeight = FontWeight.Bold, color = OnElectricLime)
                        }

                        if (backendUrlInput.isNotBlank()) {
                            Button(
                                onClick = {
                                    backendUrlInput = ""
                                    onSaveBackendUrl("")
                                },
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("btn_clear_backend_url"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricLime,
                                    contentColor = OnElectricLime
                                )
                            ) {
                                Text("Desconectar", fontWeight = FontWeight.Bold, color = OnElectricLime)
                            }
                        }
                    }
                }
            }
        }

        // Section: Appearance & Theme (Light Azul Cielo vs Dark Grafito)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APARIENCIA Y TEMA",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = if (isDarkMode) "Oscuro" else "Claro (Azul)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Selecciona el tema visual de la aplicación. El modo claro está diseñado en Azul Cielo profesional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Light Mode (Azul Cielo) Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (!isDarkMode) 2.dp else 1.dp,
                                    color = if (!isDarkMode) ElectricLime else GraphiteBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetDarkMode(false) }
                                .testTag("btn_theme_light"),
                            color = if (!isDarkMode) ElectricLime.copy(alpha = 0.12f) else GraphiteSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (!isDarkMode) ElectricLime else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Modo Claro",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isDarkMode) ElectricLime else TextPrimary
                                )
                                Text(
                                    text = "Azul Cielo",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Dark Mode (Grafito) Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isDarkMode) 2.dp else 1.dp,
                                    color = if (isDarkMode) ElectricLime else GraphiteBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetDarkMode(true) }
                                .testTag("btn_theme_dark"),
                            color = if (isDarkMode) ElectricLime.copy(alpha = 0.12f) else GraphiteSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = if (isDarkMode) ElectricLime else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Modo Oscuro",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) ElectricLime else TextPrimary
                                )
                                Text(
                                    text = "Grafito",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Google Account & Session Info
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                color = GraphiteSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SESIÓN DE GOOGLE ACTIVA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeUser,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_signout_settings"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = OnElectricLime
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = OnElectricLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", color = OnElectricLime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
