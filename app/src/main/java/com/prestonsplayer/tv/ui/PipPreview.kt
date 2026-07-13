package com.prestonsplayer.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.prestonsplayer.tv.data.Channel
import kotlinx.coroutines.delay

/**
 * Live PiP preview in the guide header.
 * Debounced: only tunes after focus rests on a channel, so scrolling the grid
 * doesn't thrash the network on a Firestick.
 */
@Composable
fun PipPreview(
    channel: Channel?,
    modifier: Modifier = Modifier,
    debounceMs: Long = 900
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f          // muted preview; audio belongs to full-screen playback
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Re-tune only when the focused channel settles
    LaunchedEffect(channel?.streamUrl) {
        val url = channel?.streamUrl
        if (url == null) {
            player.stop(); player.clearMediaItems()
        } else {
            delay(debounceMs)
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black)
            .border(1.dp, GuideTheme.LINE, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    isFocusable = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            }
        )
        if (channel == null) {
            Text("Preview", color = GuideTheme.TEXT_DIM, fontSize = 12.sp)
        }
    }
}
