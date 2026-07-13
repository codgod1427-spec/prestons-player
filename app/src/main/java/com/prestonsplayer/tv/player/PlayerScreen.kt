package com.prestonsplayer.tv.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    // Pause when the app leaves the foreground (no background/double audio), release on exit.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                Lifecycle.Event.ON_START -> player.playWhenReady = true
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            player.release()
        }
    }

    var showInfo by remember { mutableStateOf(true) }
    LaunchedEffect(index.value) {
        showInfo = true
        delay(2800)
        showInfo = false
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionUp, Key.ChannelUp -> {
                        if (channels.size > 1) index.value = (index.value - 1 + channels.size) % channels.size
                        true
                    }
                    Key.DirectionDown, Key.ChannelDown -> {
                        if (channels.size > 1) index.value = (index.value + 1) % channels.size
                        true
                    }
                    Key.DirectionCenter, Key.Enter -> { showInfo = true; true }
                    else -> false
                }
            }
            .focusable()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false          // Compose owns the D-pad now
                    isFocusable = false
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
                "▲ ▼  change channel      ◀ Back  to guide",
                color = Color(0x99FFFFFF), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(28.dp)
            )
        }
    }
}
