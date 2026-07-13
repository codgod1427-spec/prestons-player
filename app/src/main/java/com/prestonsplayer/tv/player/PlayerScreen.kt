package com.prestonsplayer.tv.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.prestonsplayer.tv.data.Channel

@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current

    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    BackHandler { onBack() }

    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                controllerAutoShow = false
                setShowNextButton(false)
                setShowPreviousButton(false)
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
                // D-pad center toggles the controller overlay
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        if (!isControllerFullyVisible) { showController(); true } else false
                    } else false
                }
            }
        }
    )
}
