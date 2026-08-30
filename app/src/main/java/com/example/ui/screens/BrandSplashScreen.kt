package com.example.ui.screens

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.config.AppConfig
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BrandSplashScreen"
private const val SPLASH_DURATION_MS = 5000L
private const val FADE_OUT_DURATION_MS = 500

@OptIn(UnstableApi::class)
@Composable
fun BrandSplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentOnFinished = rememberUpdatedState(onFinished)
    val hasFinished = remember { AtomicBoolean(false) }
    var isVisible by remember { mutableStateOf(true) }
    var isVideoReady by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun triggerFinish() {
        if (hasFinished.compareAndSet(false, true)) {
            isVisible = false
            scope.launch {
                delay(FADE_OUT_DURATION_MS.toLong())
                currentOnFinished.value()
            }
        }
    }

    // Temporizador principal de presentación
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        triggerFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "brand_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val exoPlayer = remember(context) {
        try {
            ExoPlayer.Builder(context).build().apply {
                try {
                    val rawUri = try {
                        RawResourceDataSource.buildRawResourceUri(R.raw.splash_video)
                    } catch (e: Throwable) {
                        Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
                    }
                    val mediaItem = MediaItem.fromUri(rawUri)
                    setMediaItem(mediaItem)
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                isVideoReady = true
                            } else if (playbackState == Player.STATE_ENDED) {
                                Log.d(TAG, "Video splash finalizado con éxito.")
                                triggerFinish()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.w(TAG, "Aviso ExoPlayer (${error.message}). Mostrando presentación de marca.")
                        }
                    })

                    prepare()
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo inicializar splash_video: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "ExoPlayer builder no disponible: ${e.message}")
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer?.stop()
                exoPlayer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error liberando ExoPlayer: ${e.message}")
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(FADE_OUT_DURATION_MS))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    triggerFinish()
                }
        ) {
            // Capa de video de fondo
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            useArtwork = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Emblema y logo central con halo si el video no cubre completamente o durante la carga
            if (!isVideoReady) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Halo de luz neón
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ElectricLime.copy(alpha = 0.35f),
                                        ElectricLime.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Logo oficial Termicoud
                    Image(
                        painter = painterResource(id = R.drawable.termicoud_logo_official_1787959745763),
                        contentDescription = "Logo Termicoud",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, ElectricLime.copy(alpha = 0.8f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Botón superior derecho para Saltar (Skip)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 20.dp)
                    .clickable { triggerFinish() },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saltar",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Saltar",
                        tint = ElectricLime,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Degradado oscuro superpuesto en la parte inferior para destacar la marca
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GraphiteBackground.copy(alpha = 0.75f),
                                GraphiteBackground.copy(alpha = 0.95f),
                                GraphiteBackground
                            )
                        )
                    )
            )

            // Textos de marca e identidad en la parte inferior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppConfig.APP_BRAND_NAME.uppercase(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        letterSpacing = 3.sp
                    ),
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SISTEMA DE INVENTARIO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    ),
                    color = ElectricLime,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppConfig.APP_SUBTITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

