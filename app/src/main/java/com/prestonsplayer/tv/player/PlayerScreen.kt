package com.prestonsplayer.tv.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.prestonsplayer.tv.data.Channel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(channels: List<Channel>, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val index = remember { mutableStateOf(startIndex.coerceIn(0, (channels.size - 1).coerceAtLeast(0))) }
    val channel = channels[index.value]

    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    // Show the channel banner briefly whenever the channel changes.
    var showInfo by remember { mutableStateOf(true) }
    LaunchedEffect(index.value) {
        showInfo = true
        delay(2800)
        showInfo = false
    }

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                                if (channels.size > 1)
                                    index.value = (index.value - 1 + channels.size) % channels.size
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                if (channels.size > 1)
                                    index.value = (index.value + 1) % channels.size
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (!isControllerFullyVisible) { showController(); true } else false
                            }
                            else -> false
                        }
                    }
                }
            },
            update = { it.player = player }
        )

        if (showInfo) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(28.dp)
                    .background(Color(0xCC0A0F1A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${channel.number}", color = Color(0xFF6FCC63), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text(channel.name, color = Color.White, fontSize = 20.sp)
            }
            Text(
                "▲ ▼  change channel",
                color = Color(0x99FFFFFF), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(28.dp)
            )
        }
    }
}
