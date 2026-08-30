package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant

/**
 * Modifier that applies a smooth, animated shimmer gradient over any Composable.
 */
@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(4.dp),
    baseColor: Color = GraphiteSurfaceVariant,
    highlightColor: Color = Color(0xFF3F3F3F),
    durationMillis: Int = 1300
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.7f),
            highlightColor.copy(alpha = 0.95f),
            baseColor.copy(alpha = 0.7f)
        ),
        start = Offset(translateAnimation - 350f, translateAnimation - 350f),
        end = Offset(translateAnimation, translateAnimation)
    )

    return this
        .clip(shape)
        .background(brush)
}

/**
 * Skeleton placeholder item matching ProductCard layout.
 */
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag("product_card_skeleton"),
        color = GraphiteSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge & Stock Badge placeholders
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
                            .width(64.dp)
                            .height(18.dp)
                            .shimmerEffect(shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(18.dp)
                            .shimmerEffect(shape = RoundedCornerShape(4.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(16.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Name (2 lines placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(16.dp)
                    .shimmerEffect(shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .shimmerEffect(shape = RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price Row & Quick Action Button Placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(20.dp)
                            .shimmerEffect(shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(65.dp)
                            .height(14.dp)
                            .shimmerEffect(shape = RoundedCornerShape(4.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(32.dp)
                        .shimmerEffect(shape = RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

/**
 * List of Product Card Skeletons for InventoryScreen.
 */
@Composable
fun InventoryListSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count) {
            ProductCardSkeleton()
        }
    }
}

/**
 * Skeleton placeholder item matching a Sale card in SalesHistoryScreen.
 */
@Composable
fun SaleCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag("sale_card_skeleton"),
        color = GraphiteSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: User avatar + Seller Name + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shimmerEffect(shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(14.dp)
                                .shimmerEffect(shape = RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(10.dp)
                                .shimmerEffect(shape = RoundedCornerShape(3.dp))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(95.dp)
                        .height(12.dp)
                        .shimmerEffect(shape = RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item rows placeholder (2 items)
            repeat(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(13.dp)
                            .shimmerEffect(shape = RoundedCornerShape(3.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(13.dp)
                            .shimmerEffect(shape = RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: PDF Button & Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(28.dp)
                        .shimmerEffect(shape = RoundedCornerShape(6.dp))
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(18.dp)
                            .shimmerEffect(shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .shimmerEffect(shape = RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

/**
 * List of Sale Card Skeletons for SalesHistoryScreen.
 */
@Composable
fun SalesHistorySkeleton(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count) {
            SaleCardSkeleton()
        }
    }
}

/**
 * Skeleton placeholder item matching an Inventory Movement item.
 */
@Composable
fun MovementCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GraphiteBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag("movement_card_skeleton"),
        color = GraphiteSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(75.dp)
                        .height(18.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                )

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(12.dp)
                        .shimmerEffect(shape = RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmerEffect(shape = RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp)
                        .shimmerEffect(shape = RoundedCornerShape(3.dp))
                )

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * List of Movement Card Skeletons for SalesHistoryScreen (Movements tab).
 */
@Composable
fun MovementsListSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count) {
            MovementCardSkeleton()
        }
    }
}
