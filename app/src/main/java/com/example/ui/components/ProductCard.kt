package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.CardHighlightColor
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
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
import java.util.Locale

@Composable
fun ProductCard(
    product: Product,
    exchangeRate: Double,
    onClick: () -> Unit,
    onQuickAdd: (() -> Unit)? = null
) {
    val isOutOfStock = product.cantidad <= 0
    val isLowStock = product.cantidad in 1..product.minStock
    val cardAlpha = if (isOutOfStock) 0.85f else 1.0f
    val priceBs = product.precioUsd * exchangeRate
    val hasWholesale = product.precioMayor != null && product.precioMayor > 0 &&
            product.cantidadMinimaMayor != null && product.cantidadMinimaMayor > 0

    val borderColor = when {
        isOutOfStock -> StatusAgotado.copy(alpha = 0.75f)
        isLowStock -> StatusBajo.copy(alpha = 0.75f)
        else -> GraphiteBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isOutOfStock || isLowStock) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("product_card_${product.id.ifBlank { product.fila.toString() }}"),
        color = GraphiteSurface.copy(alpha = cardAlpha),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge, Barcode & Stock Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, GraphiteBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = product.catalogo.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (product.codigoBarras.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricLime.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = product.codigoBarras,
                                style = MonoDataSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricLime
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Prominent Stock Status Pill
                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StatusAgotadoBg)
                            .border(1.dp, StatusAgotado.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✕ AGOTADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusAgotado
                        )
                    }
                } else if (isLowStock) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StatusBajoBg)
                            .border(1.dp, StatusBajo.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusBajo,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${product.cantidad} un. (Bajo)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusBajo
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StatusOkBg)
                            .border(1.dp, StatusOk.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusOk,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${product.cantidad} un.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Name - High Legibility
            Text(
                text = product.producto,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price Row with Highlighted Price Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Container
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = GraphiteSurfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "$%.2f", product.precioUsd),
                            style = MonoDataMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricLime
                            )
                        )

                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                                .background(GraphiteBorder)
                        )

                        Text(
                            text = String.format(Locale.US, "Bs %.2f", priceBs),
                            style = MonoDataSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        )
                    }
                }

                if (onQuickAdd != null && !isOutOfStock) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickAdd() }
                            .testTag("btn_quick_add_${product.id.ifBlank { product.fila.toString() }}"),
                        color = ElectricLime,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = OnElectricLime,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "+ Vender",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = OnElectricLime
                            )
                        }
                    }
                }
            }

            // Wholesale Price Badge
            if (hasWholesale) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardHighlightColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Precio Mayor: ",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "$%.2f USD", product.precioMayor),
                        style = MonoDataSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricLime
                        )
                    )
                    Text(
                        text = " (a partir de ${product.cantidadMinimaMayor} un.)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
