package com.kenyaeye.eclipse.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kenyaeye.eclipse.data.DemoContent
import com.kenyaeye.eclipse.data.MediaTrack
import com.kenyaeye.eclipse.ui.theme.EclipseCyan
import com.kenyaeye.eclipse.ui.theme.EclipseMagenta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Eclipse Protocol — a premium, Material You-native video player.
 *
 * Portrait: 16:9 cinematic frame + floating glass control bar.
 * Landscape / fullscreen: edge-to-edge player with overlay chrome.
 * PiP: PlayerView only (system renders the mini window).
 */
@Composable
fun PlayerScreen(
    isInPip: Boolean,
    onEnterPip: () -> Unit,
    onAutoPipChanged: (Boolean) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var trackIndex by rememberSaveable { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.85f) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionEpoch by remember { mutableIntStateOf(0) }
    var panelOpen by remember { mutableStateOf(false) }
    var captionsEnabled by remember { mutableStateOf(true) }
    var quality by rememberSaveable { mutableStateOf("Auto") }
    var showSettings by remember { mutableStateOf(false) }
    var showCast by remember { mutableStateOf(false) }
    var clockText by remember { mutableStateOf("--:--") }
    var batteryPct by remember { mutableIntStateOf(88) }
    var edgeAccumulator by remember { mutableFloatStateOf(0f) }

    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerView.player = null
            player.release()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayingChanged(playing)
                onAutoPipChanged(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.duration > 0) {
                    durationMs = player.duration
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = DemoContent.playlist.indexOfFirst { it.title == mediaItem?.mediaId }
                if (index >= 0) trackIndex = index
                positionMs = 0L
                durationMs = 0L
            }

            override fun onTracksChanged(tracks: Tracks) {
                captionsEnabled = !tracks.isTypeDisabled(C.TRACK_TYPE_TEXT)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            bufferedMs = player.bufferedPosition
            if (player.duration > 0) durationMs = player.duration
            delay(100)
        }
    }

    LaunchedEffect(Unit) {
        player.setMediaItems(DemoContent.playlist.map { it.toMediaItem() }, 0, 0L)
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(volume) { player.volume = volume }

    LaunchedEffect(captionsEnabled) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .build()
    }

    LaunchedEffect(isPlaying) {
        context.findActivity()?.window?.let { window ->
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, interactionEpoch) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Live clock + battery for the integrated status row (real device values).
    LaunchedEffect(Unit) {
        while (true) {
            clockText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (capacity in 1..100) batteryPct = capacity
            delay(15_000)
        }
    }

    val selectTrack: (Int) -> Unit = { index ->
        if (index in DemoContent.playlist.indices) {
            player.seekTo(index, 0L)
            player.playWhenReady = true
        }
    }

    val seekToFraction: (Float) -> Unit = { fraction ->
        if (durationMs > 0) player.seekTo((fraction.coerceIn(0f, 1f) * durationMs.toFloat()).toLong())
    }

    val applyQuality: (String) -> Unit = { option ->
        quality = option
        val (maxWidth, maxHeight) = when (option) {
            "1080p" -> 1920 to 1080
            "720p" -> 1280 to 720
            "480p" -> 1024 to 576
            else -> Int.MAX_VALUE to Int.MAX_VALUE
        }
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(maxWidth, maxHeight)
            .build()
    }

    val toggleFullscreen: (Boolean) -> Unit = { on ->
        isFullscreen = on
        context.findActivity()?.let { activity ->
            activity.requestedOrientation = if (on) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (on) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) { toggleFullscreen(false) }
    BackHandler(enabled = panelOpen) { panelOpen = false }

    val currentTrack = DemoContent.playlist[trackIndex.coerceIn(0, DemoContent.playlist.lastIndex)]
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
    val buffered = if (durationMs > 0) (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060B))
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF12081C), Color(0xFF04070D))))
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(EclipseCyan.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.maxDimension * 0.6f,
                    ),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                )
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(EclipseMagenta.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.08f, size.height * 0.92f),
                        radius = size.maxDimension * 0.55f,
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.08f, size.height * 0.92f),
                )
            },
    ) {
        val playerShape = RoundedCornerShape(20.dp)
        val playerBoxModifier = if (isInPip || isLandscape) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 98.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shadow(
                    elevation = 16.dp,
                    shape = playerShape,
                    clip = true,
                    ambientColor = EclipseCyan.copy(alpha = 0.35f),
                    spotColor = EclipseMagenta.copy(alpha = 0.35f),
                )
        }

        Box(
            playerBoxModifier
                .align(Alignment.TopCenter)
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { playerView },
                update = { view -> if (view.player !== player) view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
            LaunchedEffect(currentTrack.posterRes) {
                ContextCompat.getDrawable(context, currentTrack.posterRes)?.let { playerView.defaultArtwork = it }
            }

            if (!isInPip) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.38f),
                                0.28f to Color.Transparent,
                                0.70f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                )

                // Tap layer: single tap toggles chrome, double tap seeks ±10s.
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    controlsVisible = !controlsVisible
                                    interactionEpoch++
                                },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2f) player.seekBack() else player.seekForward()
                                    controlsVisible = true
                                    interactionEpoch++
                                },
                            )
                        },
                )

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(220)),
                    modifier = Modifier.matchParentSize(),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        TopBarOverlay(
                            track = currentTrack,
                            clock = clockText,
                            batteryPct = batteryPct,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        CenterControls(
                            isPlaying = isPlaying,
                            volume = volume,
                            captionsEnabled = captionsEnabled,
                            isFullscreen = isFullscreen,
                            onTogglePlay = {
                                if (isPlaying) player.pause() else player.play()
                                interactionEpoch++
                            },
                            onVolumeChange = {
                                volume = it
                                interactionEpoch++
                            },
                            onToggleCaptions = {
                                captionsEnabled = !captionsEnabled
                                interactionEpoch++
                            },
                            onToggleFullscreen = {
                                toggleFullscreen(!isFullscreen)
                                interactionEpoch++
                            },
                            onPip = onEnterPip,
                            onSettings = {
                                showSettings = true
                                interactionEpoch++
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                        GradientProgressTrack(
                            progress = progress,
                            buffered = buffered,
                            onScrubStart = {
                                controlsVisible = true
                                interactionEpoch++
                            },
                            onScrub = { },
                            onScrubEnd = seekToFraction,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        if (!isInPip) {
            GlassBottomBar(
                positionMs = positionMs,
                durationMs = durationMs,
                buffered = buffered,
                onSeekFraction = seekToFraction,
                onNext = {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekTo(0, 0L)
                },
                onCast = { showCast = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )

            // Right-edge swipe zone opens the chapter / related panel.
            if (!panelOpen) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(24.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                edgeAccumulator += dragAmount
                                if (edgeAccumulator > 48f) {
                                    edgeAccumulator = 0f
                                    panelOpen = true
                                }
                            }
                        },
                )
            }

            AnimatedVisibility(
                visible = panelOpen,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.matchParentSize(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) { detectTapGestures { panelOpen = false } },
                )
            }

            SidePanel(
                visible = panelOpen,
                onClose = { panelOpen = false },
                chapters = DemoContent.chapters,
                related = DemoContent.related,
                currentTrack = currentTrack,
                positionMs = positionMs,
                durationMs = durationMs,
                posterRes = currentTrack.posterRes,
                onChapterClick = { chapter ->
                    player.seekTo(chapter.startSeconds * 1000)
                    player.play()
                },
                onRelatedClick = { related ->
                    selectTrack(related.playlistIndex)
                    panelOpen = false
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            if (showSettings) {
                SettingsDialog(
                    quality = quality,
                    captionsEnabled = captionsEnabled,
                    onQuality = applyQuality,
                    onToggleCaptions = { captionsEnabled = !captionsEnabled },
                    onDismiss = { showSettings = false },
                )
            }
            if (showCast) {
                CastDialog(onDismiss = { showCast = false })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers shared across the player package.
// ---------------------------------------------------------------------------

internal fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal fun MediaTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(title)
        .setUri(videoUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .build(),
        )
        .build()

internal fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

internal fun formatChapterTime(seconds: Long): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
