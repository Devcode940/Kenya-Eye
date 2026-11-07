package com.kenyaeye.eclipse.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenyaeye.eclipse.data.MediaTrack
import com.kenyaeye.eclipse.ui.theme.EclipseCyan
import com.kenyaeye.eclipse.ui.theme.EclipseMagenta
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Top overlay — integrated status row, 4K HDR badge and the video title.
// ---------------------------------------------------------------------------

@Composable
internal fun TopBarOverlay(
    track: MediaTrack,
    clock: String,
    batteryPct: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = clock,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(10.dp))
            BatteryIndicator(pct = batteryPct)
            Spacer(Modifier.weight(1f))
            QualityPill(text = track.quality)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = track.subtitle,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BatteryIndicator(pct: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
            val stroke = 1.2.dp.toPx()
            val bodyWidth = size.width - 3.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                size = Size(bodyWidth, size.height),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = stroke),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height * 0.3f),
                size = Size(2.dp.toPx(), size.height * 0.4f),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
            val fillWidth =
                (bodyWidth - stroke * 2 - 2.dp.toPx()).coerceAtLeast(0f) * pct.coerceIn(0, 100) / 100f
            drawRoundRect(
                color = EclipseCyan,
                topLeft = Offset(stroke + 1.dp.toPx(), stroke + 1.dp.toPx()),
                size = Size(fillWidth, (size.height - (stroke + 1.dp.toPx()) * 2).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$pct%",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun QualityPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(EclipseCyan.copy(alpha = 0.75f), EclipseMagenta.copy(alpha = 0.75f)),
            ),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Center controls — glowing play button, 3D volume arc, icon row.
// ---------------------------------------------------------------------------

@Composable
internal fun CenterControls(
    isPlaying: Boolean,
    volume: Float,
    captionsEnabled: Boolean,
    isFullscreen: Boolean,
    onTogglePlay: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onPip: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlowPlayButton(isPlaying = isPlaying, onToggle = onTogglePlay)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            VolumeArc(volume = volume, onVolumeChange = onVolumeChange)
            Spacer(Modifier.width(18.dp))
            Box(
                Modifier
                    .width(1.dp)
                    .height(46.dp)
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.width(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillIconButton(
                    icon = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    label = "Fullscreen",
                    onClick = onToggleFullscreen,
                )
                PillIconButton(
                    icon = Icons.Filled.PictureInPictureAlt,
                    label = "Picture in picture",
                    onClick = onPip,
                )
                PillIconButton(
                    icon = if (captionsEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                    label = "Subtitles",
                    onClick = onToggleCaptions,
                    active = captionsEnabled,
                )
                PillIconButton(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    onClick = onSettings,
                )
            }
        }
    }
}

@Composable
internal fun GlowPlayButton(isPlaying: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "playGlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "pulse",
    )
    val glowAlpha = if (isPlaying) 0.40f + 0.24f * pulse else 0.58f

    Box(modifier.size(100.dp), contentAlignment = Alignment.Center) {
        // Neon cyan halo (drawn outside the button circle for a real glow).
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EclipseCyan.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = size.maxDimension / 2f,
                ),
                radius = size.maxDimension / 2f,
                center = center,
            )
            drawCircle(
                color = EclipseCyan.copy(alpha = glowAlpha * 0.5f),
                radius = size.maxDimension / 3.4f,
                center = center,
            )
        }
        Box(
            Modifier
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = EclipseCyan.copy(alpha = 0.5f),
                    spotColor = EclipseMagenta.copy(alpha = 0.5f),
                )
                .clip(CircleShape)
                .background(Color(0xF20D1219))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(EclipseCyan, EclipseMagenta)),
                    shape = CircleShape,
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(if (isPlaying) 30.dp else 36.dp),
            )
        }
    }
}

@Composable
internal fun VolumeArc(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        onVolumeChange(volumeValueAt(size, position))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onVolumeChange(volumeValueAt(size, change.position))
                    }
                },
        ) {
            val inset = 16.dp.toPx()
            val arcRadius = size.minDimension / 2f - inset
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            // 3D bevel base + static track.
            drawArc(
                color = Color.Black.copy(alpha = 0.45f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
            )
            if (volume > 0.01f) {
                drawArc(
                    color = lerp(EclipseCyan, EclipseMagenta, volume),
                    startAngle = 135f,
                    sweepAngle = 270f * volume,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                )
                // Inner specular highlight for the 3D look.
                val innerTopLeft = arcTopLeft + Offset(2.5f, 2.5f)
                val innerSize = Size(arcSize.width - 5f, arcSize.height - 5f)
                drawArc(
                    color = Color.White.copy(alpha = 0.20f),
                    startAngle = 135f,
                    sweepAngle = 270f * volume,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Thumb.
            val thumbAngle = Math.toRadians(135.0 + 270.0 * volume).toFloat()
            val thumbCenter = Offset(
                center.x + arcRadius * cos(thumbAngle),
                center.y + arcRadius * sin(thumbAngle),
            )
            drawCircle(EclipseCyan.copy(alpha = 0.35f), radius = 13.dp.toPx(), center = thumbCenter)
            drawCircle(Color.White, radius = 6.dp.toPx(), center = thumbCenter)
            drawCircle(
                color = EclipseMagenta.copy(alpha = 0.9f),
                radius = 6.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(volume * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "VOLUME",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
        }
    }
}

private fun volumeValueAt(size: IntSize, position: Offset): Float {
    val dx = position.x - size.width / 2f
    val dy = position.y - size.height / 2f
    var degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    if (degrees < 0f) degrees += 360f
    val adjusted = if (degrees < 135f) degrees + 360f else degrees
    return ((adjusted - 135f) / 270f).coerceIn(0f, 1f)
}

@Composable
internal fun PillIconButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) EclipseCyan.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) EclipseCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.14f),
        ),
        shadowElevation = 6.dp,
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) EclipseCyan else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs — real playback settings, decorative cast scanner.
// ---------------------------------------------------------------------------

@Composable
internal fun SettingsDialog(
    quality: String,
    captionsEnabled: Boolean,
    onQuality: (String) -> Unit,
    onToggleCaptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Playback settings", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    text = "QUALITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                listOf("Auto", "1080p", "720p", "480p").forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuality(option) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = quality == option,
                            onClick = { onQuality(option) },
                        )
                        Text(option, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Captions", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = captionsEnabled, onCheckedChange = { onToggleCaptions() })
                }
            }
        },
    )
}

@Composable
internal fun CastDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Cast to", fontWeight = FontWeight.SemiBold) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    "Searching for nearby devices on your network…",
                    fontSize = 13.sp,
                )
            }
        },
    )
}
