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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MonoDataMedium
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.StatusAgotado
import com.example.ui.theme.StatusAgotadoBg
import com.example.ui.theme.StatusBajo
import com.example.ui.theme.StatusBajoBg
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
    val cardAlpha = if (isOutOfStock) 0.70f else 1.0f
    val priceBs = product.precioUsd * exchangeRate
    val hasWholesale = product.precioMayor != null && product.precioMayor > 0 &&
            product.cantidadMinimaMayor != null && product.cantidadMinimaMayor > 0

    val borderColor = when {
        isOutOfStock -> StatusAgotadoBg
        isLowStock -> StatusBajoBg
        else -> GraphiteBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("product_card_${product.id.ifBlank { product.fila.toString() }}"),
        color = GraphiteSurface.copy(alpha = cardAlpha),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GraphiteBorder)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.catalogo.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    if (product.codigoBarras.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricLime.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EAN: ${product.codigoBarras}",
                                style = MonoDataSmall.copy(
                                    fontSize = 9.sp,
                                    color = ElectricLime
                                )
                            )
                        }
                    }
                }

                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAgotadoBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AGOTADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusAgotado
                        )
                    }
                } else if (isLowStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusBajoBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STOCK BAJO (${product.cantidad} un.)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusBajo
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Stock: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "${product.cantidad} un.",
                            style = MonoDataSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.producto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "$ %.2f USD", product.precioUsd),
                        style = MonoDataMedium.copy(
                            fontSize = 17.sp,
                            color = ElectricLime
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "Bs %.2f", priceBs),
                        style = MonoDataSmall,
                        color = TextSecondary
                    )

                    if (hasWholesale) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Al mayor: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", product.precioMayor),
                                style = MonoDataSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricLime
                                )
                            )
                            Text(
                                text = " desde ${product.cantidadMinimaMayor} un.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                        }
                    }
                }

                if (onQuickAdd != null && !isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricLime)
                            .clickable { onQuickAdd() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Vender",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GraphiteSurface
                        )
                    }
                }
            }
        }
    }
}
