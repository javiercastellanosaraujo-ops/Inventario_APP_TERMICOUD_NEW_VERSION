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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.TextSecondary

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val adminOnly: Boolean = false
)

val rawNavItems = listOf(
    NavItem("Inicio", Icons.Default.Home, "nav_inicio"),
    NavItem("Inventario", Icons.Default.Inventory2, "nav_inventario"),
    NavItem("Salida", Icons.Default.PointOfSale, "nav_salida"),
    NavItem("Entrada", Icons.Default.AddBox, "nav_entrada"),
    NavItem("Combos", Icons.Default.Layers, "nav_combos"),
    NavItem("Ganancias", Icons.Default.TrendingUp, "nav_ganancias", adminOnly = true),
    NavItem("Tasa", Icons.Default.Savings, "nav_tasa")
)

@Composable
fun BottomNavBar(
    selectedTab: Int,
    isAdmin: Boolean = false,
    onTabSelected: (Int) -> Unit
) {
    // Preserve original index by calling withIndex() before filtering
    val visibleItems = remember(isAdmin) {
        rawNavItems.withIndex()
            .filter { (_, item) ->
                if (item.adminOnly) isAdmin else true
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraphiteSurface)
            .border(width = 1.dp, color = GraphiteBorder)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleItems.forEach { (originalIndex, item) ->
                val isSelected = selectedTab == originalIndex
                val tint = if (isSelected) ElectricLime else TextSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(originalIndex) }
                        .padding(vertical = 4.dp, horizontal = 1.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricLime.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = tint,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.title,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
