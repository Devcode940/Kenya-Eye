package com.kenyaeye.eclipse.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenyaeye.eclipse.data.Chapter
import com.kenyaeye.eclipse.data.MediaTrack
import com.kenyaeye.eclipse.data.RelatedVideo
import com.kenyaeye.eclipse.ui.theme.EclipseCyan
import com.kenyaeye.eclipse.ui.theme.EclipseMagenta
import com.kenyaeye.eclipse.ui.theme.EclipseSurface

// ---------------------------------------------------------------------------
// Razor-thin cyan→magenta gradient progress track with hover + scrubber.
// ---------------------------------------------------------------------------

@Composable
internal fun GradientProgressTrack(
    progress: Float,
    buffered: Float,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var hoverFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: progress.coerceIn(0f, 1f)

    Box(
        modifier
            .height(30.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Exit) {
                            hoverFraction = null
                        } else if (event.type == PointerEventType.Move &&
                            !event.buttons.isPrimaryPressed
                        ) {
                            event.changes.firstOrNull()?.let { change ->
                                hoverFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val target = (offset.x / size.width).coerceIn(0f, 1f)
                    onScrubStart()
                    onScrubEnd(target)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrubStart()
                    },
                    onDragEnd = {
                        dragFraction?.let(onScrubEnd)
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                ) { change, _ ->
                    change.consume()
                    val target = (change.position.x / size.width).coerceIn(0f, 1f)
                    dragFraction = target
                    onScrub(target)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2f
            val width = size.width
            val dragging = dragFraction != null
            val strokeWidth = if (dragging) 3.dp.toPx() else 1.dp.toPx()

            // Base track.
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            // Buffered range.
            drawLine(
                color = Color.White.copy(alpha = 0.26f),
                start = Offset(0f, y),
                end = Offset(width * buffered.coerceIn(0f, 1f), y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            // Played range: vibrant cyan → magenta Material You gradient.
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(EclipseCyan, EclipseMagenta),
                    startX = 0f,
                    endX = width,
                ),
                start = Offset(0f, y),
                end = Offset(width * fraction, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            // Hover position hairline.
            hoverFraction?.let { hover ->
                if (!dragging) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.30f),
                        start = Offset(width * hover, y - 9.dp.toPx()),
                        end = Offset(width * hover, y + 9.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }

            // Scrubber — grows on hover/drag.
            val handleX = width * fraction
            val handleRadius = if (dragging || hoverFraction != null) 7.dp.toPx() else 4.5.dp.toPx()
            drawCircle(
                color = EclipseCyan.copy(alpha = 0.32f),
                radius = handleRadius * 2.1f,
                center = Offset(handleX, y),
            )
            drawCircle(Color.White, radius = handleRadius, center = Offset(handleX, y))
            drawCircle(
                color = EclipseMagenta.copy(alpha = 0.85f),
                radius = handleRadius,
                center = Offset(handleX, y),
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Glassmorphic bottom control bar — timestamps, seek bar, cast, next, avatar.
// ---------------------------------------------------------------------------

@Composable
internal fun GlassBottomBar(
    positionMs: Long,
    durationMs: Long,
    buffered: Float,
    onSeekFraction: (Float) -> Unit,
    onNext: () -> Unit,
    onCast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xD9070B12),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(EclipseCyan.copy(alpha = 0.32f), EclipseMagenta.copy(alpha = 0.32f)),
            ),
        ),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTimestamp(positionMs),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = " / " + formatTimestamp(durationMs),
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(12.dp))
            GradientProgressTrack(
                progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                buffered = buffered,
                onScrubStart = { },
                onScrub = { },
                onScrubEnd = onSeekFraction,
                modifier = Modifier.weight(1f),
            )
            MiniIconButton(Icons.Filled.Cast, "Cast", onCast)
            MiniIconButton(Icons.Filled.SkipNext, "Next episode", onNext)
            Spacer(Modifier.width(4.dp))
            AvatarCy()
        }
    }
}

@Composable
internal fun MiniIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun AvatarCy(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "avatarRing").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "ring",
    )
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        // Pulsing neon cyan ring.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = EclipseCyan,
                radius = size.minDimension / 2f * (0.94f + 0.06f * pulse),
                style = Stroke(width = 2.dp.toPx()),
                alpha = (0.9f - 0.55f * pulse).coerceIn(0.2f, 1f),
            )
            drawCircle(
                color = EclipseCyan.copy(alpha = 0.30f),
                radius = size.minDimension / 2f * 0.97f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Box(
            Modifier
                .size(31.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(EclipseCyan, EclipseMagenta))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CY",
                color = Color(0xFF06131B),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Right-side swipeable panel — chapter list + related videos grid.
// ---------------------------------------------------------------------------

@Composable
internal fun SidePanel(
    visible: Boolean,
    onClose: () -> Unit,
    chapters: List<Chapter>,
    related: List<RelatedVideo>,
    currentTrack: MediaTrack,
    positionMs: Long,
    durationMs: Long,
    @DrawableRes posterRes: Int,
    onChapterClick: (Chapter) -> Unit,
    onRelatedClick: (RelatedVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(260)),
        exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240)),
        modifier = modifier.fillMaxHeight(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(324.dp),
            shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
            color = Color(0xF70A0E16),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(EclipseCyan.copy(alpha = 0.28f), EclipseMagenta.copy(alpha = 0.28f)),
                ),
            ),
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("NOW PLAYING")
                        Text(
                            text = currentTrack.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close panel",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("CHAPTER LIST")
                Spacer(Modifier.height(8.dp))
                chapters.forEachIndexed { index, chapter ->
                    val startMs = chapter.startSeconds * 1000
                    val endMs = chapters.getOrNull(index + 1)?.startSeconds?.times(1000) ?: durationMs
                    val isCurrent = positionMs >= startMs && (endMs <= startMs || positionMs < endMs)
                    ChapterRow(
                        chapter = chapter,
                        isCurrent = isCurrent,
                        posterRes = posterRes,
                        onClick = { onChapterClick(chapter) },
                    )
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("RELATED VIDEOS")
                Spacer(Modifier.height(10.dp))
                related.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        rowItems.forEach { item ->
                            PosterTile(item, Modifier.weight(1f)) { onRelatedClick(item) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
internal fun ChapterRow(
    chapter: Chapter,
    isCurrent: Boolean,
    @DrawableRes posterRes: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 66.dp, height = 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EclipseSurface),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(posterRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Now playing",
                        tint = EclipseCyan,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatChapterTime(chapter.startSeconds),
                color = EclipseCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (isCurrent) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(EclipseMagenta),
            )
        }
    }
}

@Composable
internal fun PosterTile(
    video: RelatedVideo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(EclipseCyan.copy(alpha = 0.35f), EclipseMagenta.copy(alpha = 0.35f)),
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick),
        ) {
            Image(
                painter = painterResource(video.posterRes),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))),
                    ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = video.title,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(EclipseCyan, EclipseMagenta))),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
    }
}
