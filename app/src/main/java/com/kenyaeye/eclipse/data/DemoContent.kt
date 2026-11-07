package com.kenyaeye.eclipse.data

import androidx.annotation.DrawableRes
import com.kenyaeye.eclipse.R

data class Chapter(val title: String, val startSeconds: Long)

data class RelatedVideo(
    val title: String,
    @DrawableRes val posterRes: Int,
    val playlistIndex: Int,
)

data class MediaTrack(
    val title: String,
    val subtitle: String,
    val videoUrl: String,
    @DrawableRes val posterRes: Int,
    val quality: String,
)

object DemoContent {
    private const val STREAM_HOST =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/"

    val playlist = listOf(
        MediaTrack(
            title = "Eclipse Protocol",
            subtitle = "Season 1 · Episode 4 · “Signal Burn”",
            videoUrl = STREAM_HOST + "Sintel.mp4",
            posterRes = R.drawable.poster_eclipse_protocol,
            quality = "4K HDR",
        ),
        MediaTrack(
            title = "Nexus Prime",
            subtitle = "Original Film · 2026",
            videoUrl = STREAM_HOST + "TearsOfSteel.mp4",
            posterRes = R.drawable.poster_nexus_prime,
            quality = "4K",
        ),
        MediaTrack(
            title = "Neural Echo",
            subtitle = "Original Film · 2025",
            videoUrl = STREAM_HOST + "ElephantsDream.mp4",
            posterRes = R.drawable.poster_neural_echo,
            quality = "1080p",
        ),
        MediaTrack(
            title = "Cyberspace",
            subtitle = "Original Series · Pilot",
            videoUrl = STREAM_HOST + "BigBuckBunny.mp4",
            posterRes = R.drawable.poster_cyberspace,
            quality = "4K",
        ),
    )

    val chapters = listOf(
        Chapter("City View", 0L),
        Chapter("Chase Sequence", 35L),
        Chapter("Neon District", 62L),
        Chapter("The Drop", 105L),
        Chapter("Eclipse", 148L),
    )

    val related = listOf(
        RelatedVideo("Nexus Prime", R.drawable.poster_nexus_prime, 1),
        RelatedVideo("Neural Echo", R.drawable.poster_neural_echo, 2),
        RelatedVideo("Cyberspace", R.drawable.poster_cyberspace, 3),
        RelatedVideo("Ghost Circuit", R.drawable.poster_nexus_prime, 1),
        RelatedVideo("Static Horizon", R.drawable.poster_neural_echo, 2),
        RelatedVideo("Chrome Dynasty", R.drawable.poster_cyberspace, 3),
    )
}
