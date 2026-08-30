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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.config.AppConfig
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Términos y Condiciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ElectricLime.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = AppConfig.APP_BRAND_NAME,
                                    color = ElectricLime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Marco legal, límites de consulta y privacidad",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_terms_screen")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraphiteBackground
                )
            )
        },
        containerColor = GraphiteBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Banner Hero Termicoud
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricLime.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acuerdo de Servicio ${AppConfig.APP_BRAND_NAME}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricLime
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Este documento regula el uso operativo, los límites de consultas en servidor y la protección de datos personales y comerciales en ${AppConfig.APP_FULL_TITLE}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 17.sp,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Sección 1: Identidad y Aceptación
            TermsSectionItem(
                icon = Icons.Default.Description,
                title = "1. Nombre y Ámbito del Servicio (Termicoud)",
                content = "La plataforma opera bajo el protocolo de gobernanza y control denominado Termicoud. Al registrarse, iniciar sesión u operar cualquier terminal de punto de venta e inventario, el usuario acepta de manera vinculante estos términos, comprometiéndose a un uso transparente, lícito y conforme a las leyes mercantiles vigentes."
            )

            // Sección 2: Límites de Uso del Sistema de Consultas (DEDICADO)
            TermsSectionItem(
                icon = Icons.Default.Speed,
                title = "2. Límites de Uso del Sistema de Consultas (Rate Limiting y Cuotas)",
                content = "Con el fin de garantizar la disponibilidad ininterrumpida del servicio y evitar la sobrecarga o saturación de los servidores en la nube:\n\n" +
                        "• Tasa de Peticiones Controlada: Las consultas directas hacia endpoints de sincronización (tasas de cambio BCV, webhooks y lectura de catálogos) cuentan con protección de frecuencia. Se prohíben las llamadas simultáneas repetitivas o ráfagas continuas desde un mismo cliente.\n\n" +
                        "• Descarga Paginada de Datos: Las operaciones sobre registros históricos (ventas, movimientos, auditoría de reversos) se ejecutan estrictamente en lotes paginados (lotes de 20 a 50 registros por consulta) para optimizar el consumo de memoria y ancho de banda móvil.\n\n" +
                        "• Almacenamiento en Caché Local: Los datos consultados recientemente se preservan en memoria local segura del dispositivo. Mientras los datos locales sean válidos, las consultas se resuelven en el dispositivo sin generar peticiones redundantes a la base de datos central.\n\n" +
                        "• Prevención de Abuso y Bots: El uso de emuladores automatizados, scripts de scraping o ataques de denegación de servicio acarreará la limitación preventiva inmediata de la sesión y el reporte de auditoría."
            )

            // Sección 3: Políticas de Privacidad del Usuario (DEDICADO)
            TermsSectionItem(
                icon = Icons.Default.PrivacyTip,
                title = "3. Políticas de Privacidad y Tratamiento de Datos",
                content = "La confidencialidad y resguardo de la información de usuarios y clientes constituye un pilar esencial en Termicoud:\n\n" +
                        "• Datos Recopilados: Únicamente se gestionan datos indispensables para la operación comercial, tales como correo electrónico del operador, nombre visible, rol asignado (Admin / Operador), registros de transacciones de venta e historial de stock.\n\n" +
                        "• No Comercialización: La información comercial, precios de costo (precioCompra), márgenes de ganancia y volúmenes de venta jamás serán vendidos, cedidos ni compartidos con terceros con fines publicitarios o comerciales.\n\n" +
                        "• Cifrado y Aislamiento: Toda la información viaja mediante canales de comunicación seguros cifrados bajo el protocolo TLS 1.3/HTTPS y se almacena en la infraestructura de Google Firebase Firestore con reglas de acceso por rol estricto.\n\n" +
                        "• Derecho de Acceso y Cancelación: Los usuarios pueden solicitar la revisión o desactivación de su cuenta a través del Administrador maestro del sistema."
            )

            // Sección 4: Roles y Responsabilidades
            TermsSectionItem(
                icon = Icons.Default.VerifiedUser,
                title = "4. Roles de Acceso y Control de Autorizaciones",
                content = "• Administrador: Posee facultades para modificar costos, autorizar nuevos usuarios, anular ventas con restauración de existencias y gestionar combos promocionales.\n" +
                        "• Operador: Autorizado para realizar búsquedas, escanear códigos de barra, despachar salidas y registrar ventas autorizadas.\n\n" +
                        "Cada operador es responsable exclusivo de la custodia de sus credenciales y de las acciones efectuadas en su sesión."
            )

            // Sección 5: Seguridad y Auditoría
            TermsSectionItem(
                icon = Icons.Default.Lock,
                title = "5. Seguridad y Registros de Auditoría Irrevocables",
                content = "Toda transacción crítica —incluyendo ajustes de existencias, cambios en la tasa de cambio y anulación de comprobantes— genera una huella de auditoría inmutable con fecha, hora exacta y correo del usuario que autorizó la operación."
            )

            // Sección 6: Vigencia
            TermsSectionItem(
                icon = Icons.Default.Security,
                title = "6. Modificaciones y Vigencia",
                content = "Termicoud se reserva el derecho de actualizar estos términos para incorporar mejoras de seguridad o cumplimiento normativo. Las actualizaciones entrarán en vigencia inmediatamente tras su publicación en esta pantalla."
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_accept_terms_screen"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aceptar y Continuar",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TermsSectionItem(
    icon: ImageVector,
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GraphiteSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricLime.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = GraphiteBorder, thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 19.sp,
                fontSize = 12.5.sp
            )
        }
    }
}
