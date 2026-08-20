package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserManagementScreen(
    users: List<AppUser>,
    onApproveUser: (String) -> Unit,
    onRejectUser: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pendientes, 1: Aprobados, 2: Rechazados

    val pendingUsers = users.filter { it.estado.equals("pendiente", ignoreCase = true) }
    val approvedUsers = users.filter { it.estado.equals("aprobado", ignoreCase = true) }
    val rejectedUsers = users.filter { it.estado.equals("rechazado", ignoreCase = true) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("btn_back_user_mgmt")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADMINISTRACIÓN DE USUARIOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Control de accesos a Firestore y permisos de operadores",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GraphiteSurface,
            contentColor = ElectricLime,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricLime
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Pendientes (${pendingUsers.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) ElectricLime else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_pending_users")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Aprobados (${approvedUsers.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) ElectricLime else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_approved_users")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "Rechazados (${rejectedUsers.size})",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 2) ElectricLime else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_rejected_users")
            )
        }

        val currentList = when (selectedTab) {
            0 -> pendingUsers
            1 -> approvedUsers
            else -> rejectedUsers
        }

        if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (selectedTab) {
                            0 -> "No hay solicitudes pendientes"
                            1 -> "No hay usuarios aprobados"
                            else -> "No hay usuarios rechazados"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (selectedTab) {
                            0 -> "Cuando nuevos operadores inicien sesión con Google, aparecerán aquí para tu aprobación"
                            1 -> "Los usuarios aprobados tienen acceso total al inventario y ventas"
                            else -> "Los usuarios bloqueados no pueden sincronizar datos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentList, key = { it.email }) { user ->
                    UserApprovalCard(
                        user = user,
                        dateFormat = dateFormat,
                        onApprove = { onApproveUser(user.email) },
                        onReject = { onRejectUser(user.email) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserApprovalCard(
    user: AppUser,
    dateFormat: SimpleDateFormat,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isAdmin = user.rol.equals("admin", ignoreCase = true)
    val isPending = user.estado.equals("pendiente", ignoreCase = true)
    val isApproved = user.estado.equals("aprobado", ignoreCase = true)
    val isRejected = user.estado.equals("rechazado", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAdmin) ElectricLime.copy(alpha = 0.2f)
                                else if (isApproved) Color(0xFF38BDF8).copy(alpha = 0.2f)
                                else if (isRejected) AlertRed.copy(alpha = 0.2f)
                                else Color(0xFFFFB800).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Lock else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isAdmin) ElectricLime else if (isApproved) Color(0xFF38BDF8) else if (isRejected) AlertRed else Color(0xFFFFB800),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.nombre.ifBlank { "Operador" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricLime.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = ElectricLime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Estado badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isApproved -> ElectricLime.copy(alpha = 0.15f)
                                isRejected -> AlertRed.copy(alpha = 0.15f)
                                else -> Color(0xFFFFB800).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = user.estado.uppercase(),
                        color = when {
                            isApproved -> ElectricLime
                            isRejected -> AlertRed
                            else -> Color(0xFFFFB800)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timestamps info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Solicitado: ${dateFormat.format(Date(user.fechaSolicitud))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                if (user.fechaAprobacion != null) {
                    Text(
                        text = "Aprobado: ${dateFormat.format(Date(user.fechaAprobacion))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            if (user.aprobadoPorEmail != null && user.aprobadoPorEmail.isNotBlank()) {
                Text(
                    text = "Por: ${user.aprobadoPorEmail}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            if (!isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPending) {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier.testTag("btn_reject_user_${user.email}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rechazar", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("btn_approve_user_${user.email}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aprobar Acceso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isApproved) {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier.testTag("btn_revoke_user_${user.email}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revocar Acceso", fontSize = 12.sp)
                        }
                    } else if (isRejected) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("btn_reapprove_user_${user.email}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-Aprobar Acceso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
