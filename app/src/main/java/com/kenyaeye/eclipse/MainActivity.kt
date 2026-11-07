package com.kenyaeye.eclipse

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.kenyaeye.eclipse.player.PlayerScreen
import com.kenyaeye.eclipse.ui.theme.EclipseTheme

class MainActivity : ComponentActivity() {

    private val isInPipMode = mutableStateOf(false)

    @Volatile
    private var isPlaybackPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EclipseTheme {
                PlayerScreen(
                    isInPip = isInPipMode.value,
                    onEnterPip = ::enterPipMode,
                    onAutoPipChanged = ::updateAutoPip,
                    onPlayingChanged = { isPlaybackPlaying = it },
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Legacy auto-PiP path (Android 8–11); Android 12+ uses setAutoEnterEnabled.
        if (isPlaybackPlaying && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(buildPipParams(isPlaybackPlaying))
            } catch (_: IllegalStateException) {
                // PiP unavailable (e.g. not in a resumable lifecycle state).
            }
        }
    }

    private fun updateAutoPip(playing: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                setPictureInPictureParams(buildPipParams(playing))
            } catch (_: IllegalStateException) {
                // Ignore — params will be refreshed on the next playback change.
            }
        }
    }

    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }
}
