package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.config.AppConfig
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TermiCoudDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.2.dp, GraphiteBorder, RoundedCornerShape(20.dp)),
            color = GraphiteSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ElectricLime.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = AppConfig.APP_BRAND_NAME,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricLime
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = ElectricLime.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "V 2.0",
                                        color = ElectricLime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Términos y Condiciones de Uso",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GraphiteSurfaceVariant)
                            .testTag("btn_close_termicoud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = GraphiteBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Terms Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Introduction banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GraphiteBackground),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bienvenido a TermiCoud",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Al acceder y operar el sistema de inventario y punto de venta (${AppConfig.APP_FULL_TITLE}), el usuario acepta los siguientes términos y condiciones de servicio que regulan el uso correcto, seguro y transparente de la plataforma.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    TermSection(
                        icon = Icons.Default.VerifiedUser,
                        title = "1. Cuentas, Identidad y Roles",
                        content = "El acceso a la aplicación se gestiona mediante autenticación segura con Google Sign-In o credenciales de operador. Existen dos niveles de acceso:\n" +
                                "• Administrador: Control total sobre catálogo de productos, compras, costos, márgenes, reversos de ventas y aprobación/gestión de usuarios.\n" +
                                "• Operador: Registro de ventas, salidas, entradas autorizadas y consulta de inventario.\n\n" +
                                "Cada usuario es responsable de la confidencialidad de su sesión y de las acciones realizadas bajo su identificador."
                    )

                    TermSection(
                        icon = Icons.Default.Description,
                        title = "2. Gestión de Inventario, Costos y Precios",
                        content = "La información de existencias, costos unitarios de compra (precioCompra), precios de venta en divisas (USD) y su conversión a bolívares (VES) debe ser registrada con exactitud y honestidad. Las tasas de cambio se sincronizan con los canales oficiales correspondientes (BCV) o valores fijados por el comercio."
                    )

                    TermSection(
                        icon = Icons.Default.Security,
                        title = "3. Trazabilidad, Auditoría y Reversos",
                        content = "Para garantizar la transparencia contable:\n" +
                                "• Todo movimiento de entrada, salida, venta o modificación de precio queda registrado con marca de tiempo, usuario responsable y motivo.\n" +
                                "• La anulación o reverso de una venta restaura automáticamente el stock al inventario y genera un registro de auditoría irrevocable con los datos del administrador que autorizó la operación."
                    )

                    TermSection(
                        icon = Icons.Default.Check,
                        title = "4. Almacenamiento y Protección de Datos",
                        content = "Los datos del catálogo, transacciones y sesiones se almacenan de forma segura en la infraestructura en la nube de Google Firebase Firestore. El sistema aplica reglas de seguridad que impiden accesos o modificaciones no autorizadas."
                    )

                    TermSection(
                        icon = Icons.Default.Speed,
                        title = "5. Límites de Consultas y Uso Justo (Rate Limiting)",
                        content = "Para preservar la estabilidad, velocidad y disponibilidad de la plataforma en la nube, el sistema implementa políticas estrictas de cuotas y uso justo:\n\n" +
                                "• Frecuencia de Peticiones y API: Las consultas automáticas hacia los servicios externos (incluyendo obtención de tasas BCV, scripts de backend y sincronización en tiempo real) están sujetas a un límite de tasa por dispositivo y por sesión. Se prohíben llamadas concurrentes o disparos repetitivos innecesarios.\n" +
                                "• Paginación y Carga por Lotes: Las consultas masivas de inventario, ventas e historial se despachan en bloques paginados (por ejemplo, lotes de 20 a 50 transacciones) para evitar saturación de memoria y consumo desmedido de ancho de banda.\n" +
                                "• Caché Inteligente y Respaldo Local: Los datos consultados recientemente se conservan en memoria caché local. Si se alcanza un umbral temporal de consultas, el sistema responderá desde el almacenamiento local seguro hasta que expire la ventana de espera.\n" +
                                "• Restricción por Abuso: El envío automatizado de peticiones no autorizadas o intentos de sobrecargar los endpoints de datos podrá acarrear la suspensión preventiva temporal del acceso."
                    )

                    TermSection(
                        icon = Icons.Default.Security,
                        title = "6. Políticas de Privacidad del Usuario",
                        content = "En Termicoud la privacidad de usuarios y operadores es prioritaria:\n\n" +
                                "• Protección de Datos Operativos: Los correos electrónicos, nombres y roles asignados se emplean con el fin exclusivo de control de acceso y registro de auditoría interna.\n" +
                                "• No Compartición con Terceros: La información financiera, compras a proveedores y márgenes de ganancia no se comparten ni comercializan con entidades externas.\n" +
                                "• Cifrado de Comunicaciones: Las transacciones se transmiten bajo protocolos seguros HTTPS/TLS 1.3 con Google Firestore."
                    )

                    TermSection(
                        icon = Icons.Default.Gavel,
                        title = "7. Modificaciones y Vigencia de Termicoud",
                        content = "Termicoud puede ser actualizado periódicamente para incorporar nuevas funcionalidades, mejoras de seguridad o ajustes legales. El uso continuo del sistema tras una actualización implica la aceptación plena de los términos vigentes."
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Última actualización: Agosto 2026 • TermiCoud Security & Governance",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = GraphiteBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_accept_termicoud"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entendido y Aceptado",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TermSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GraphiteSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, GraphiteBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 17.sp,
                fontSize = 12.sp
            )
        }
    }
}
