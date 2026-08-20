package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserApprovalLockScreen(
    user: AppUser,
    isCheckingStatus: Boolean,
    onRefreshStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    val isRejected = user.estado.equals("rechazado", ignoreCase = true)
    val statusTitle = if (isRejected) "Acceso Denegado" else "Acceso en Revisión"
    val statusDesc = if (isRejected) {
        "Tu solicitud de acceso para este inventario ha sido rechazada por el administrador. Comunícate con soporte si crees que esto es un error."
    } else {
        "Tu cuenta de Google fue registrada exitosamente, pero tu acceso al inventario está pendiente de aprobación por el administrador."
    }

    val primaryColor = if (isRejected) AlertRed else Color(0xFFFFB800)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GraphiteBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Status Badge Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = statusTitle,
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = statusTitle.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // User Details Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(8.dp)),
                        color = GraphiteBackground,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usuario:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                Text(user.nombre.ifBlank { "Operador" }, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Correo:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                Text(user.email, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fecha Solicitud:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                Text(dateFormat.format(Date(user.fechaSolicitud)), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estado actual:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = user.estado.uppercase(),
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Refresh / Verify Access Button (High Contrast & Legible)
                    Button(
                        onClick = onRefreshStatus,
                        enabled = !isCheckingStatus,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricLime,
                            contentColor = Color(0xFF121212),
                            disabledContainerColor = ElectricLime.copy(alpha = 0.6f),
                            disabledContentColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_refresh_approval_status")
                    ) {
                        if (isCheckingStatus) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF121212),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verificando...",
                                color = Color(0xFF121212),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF121212),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verificar Acceso / Aprobación",
                                color = Color(0xFF121212),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sign Out Button
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary,
                            containerColor = GraphiteSurfaceVariant
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(GraphiteBorder)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_logout_from_lock_screen")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cerrar Sesión / Cambiar Cuenta",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
