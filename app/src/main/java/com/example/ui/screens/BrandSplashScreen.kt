package com.example.ui.screens

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.config.AppConfig
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BrandSplashScreen"
private const val SAFETY_TIMEOUT_MS = 6000L
private const val FADE_OUT_DURATION_MS = 600

@OptIn(UnstableApi::class)
@Composable
fun BrandSplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentOnFinished = rememberUpdatedState(onFinished)
    val hasFinished = remember { AtomicBoolean(false) }
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun triggerFinish() {
        if (hasFinished.compareAndSet(false, true)) {
            // Initiate graceful fade-out transition
            isVisible = false
            scope.launch {
                delay(FADE_OUT_DURATION_MS.toLong())
                currentOnFinished.value()
            }
        }
    }

    // Fallback safety timer: if video doesn't end or fails to load, continue after 6s
    LaunchedEffect(Unit) {
        delay(SAFETY_TIMEOUT_MS)
        Log.d(TAG, "Safety timeout reached ($SAFETY_TIMEOUT_MS ms), proceeding to main app.")
        triggerFinish()
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val rawUri = Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
                val mediaItem = MediaItem.fromUri(rawUri)
                setMediaItem(mediaItem)

                // Reproducción sin sonido por defecto (0f).
                // Para activar audio en el video, cambia el volumen a 1f:
                volume = 0f

                // No repetir en bucle: se reproduce una sola vez
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            Log.d(TAG, "Video splash finalizado, iniciando transición suave.")
                            triggerFinish()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w(TAG, "Error reproduciendo splash_video (${error.message}). Continuando.", error)
                        triggerFinish()
                    }
                })

                prepare()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo inicializar splash_video.mp4: ${e.message}", e)
                triggerFinish()
            }
        }
    }

    // Libera los recursos del reproductor al salir del Composable
    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.stop()
                exoPlayer.release()
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
        ) {
            // Video a pantalla completa sin controles y rellenando la vista con RESIZE_MODE_ZOOM
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        useArtwork = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Degradado oscuro superpuesto en la parte inferior para destacar la marca
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
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
                    .padding(horizontal = 24.dp, vertical = 40.dp),
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
