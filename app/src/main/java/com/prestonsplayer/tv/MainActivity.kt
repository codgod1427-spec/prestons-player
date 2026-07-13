package com.prestonsplayer.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.prestonsplayer.tv.data.Channel
import com.prestonsplayer.tv.data.GuideData
import com.prestonsplayer.tv.data.GuideRepository
import com.prestonsplayer.tv.player.PlayerScreen
import com.prestonsplayer.tv.ui.GuideScreen
import com.prestonsplayer.tv.ui.GuideTheme
import com.prestonsplayer.tv.ui.SettingsScreen
import com.prestonsplayer.tv.ui.SplashScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private sealed interface Screen {
    data object Setup : Screen
    data object Loading : Screen
    data class Guide(val data: GuideData) : Screen
    data class Player(val channels: List<Channel>, val index: Int, val data: GuideData) : Screen
    data class Error(val message: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the display awake while the app is open — no Fire TV screensaver mid-watch.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val repo = GuideRepository(this)

        setContent {
            var screen by remember {
                mutableStateOf<Screen>(if (repo.playlistUrl.isBlank()) Screen.Setup else Screen.Loading)
            }
            var city by remember { mutableStateOf(repo.weatherCity) }
            // Remember the last loaded guide so Setup's Back can return to it.
            var lastGuide by remember { mutableStateOf<GuideData?>(null) }
            LaunchedEffect(screen) { (screen as? Screen.Guide)?.let { lastGuide = it.data } }

            // Hoisted here so guide scroll position + filters survive Guide -> Player -> Guide.
            val guideListState = rememberLazyListState()
            var selCategory by remember { mutableStateOf<String?>(null) }
            var selCountry by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(screen) {
                if (screen is Screen.Loading) {
                    screen = try {
                        // Let the parkour splash play at least one full lap
                        val result = coroutineScope {
                            val load = async { repo.load() }
                            delay(2600)
                            load.await()
                        }
                        if (result.channels.isEmpty())
                            Screen.Error("Playlist loaded, but it contained no channels.")
                        else Screen.Guide(result)
                    } catch (e: Exception) {
                        Screen.Error(e.message ?: "Could not load the playlist")
                    }
                }
            }

            when (val s = screen) {
                is Screen.Setup -> SettingsScreen(
                    initialPlaylist = repo.playlistUrl,
                    initialEpg = repo.epgUrl,
                    initialCity = repo.weatherCity,
                    canCancel = lastGuide != null,
                    onCancel = { lastGuide?.let { screen = Screen.Guide(it) } },
                    onSave = { pl, epg, c ->
                        repo.playlistUrl = pl
                        repo.epgUrl = epg
                        repo.weatherCity = c
                        city = c.trim()
                        screen = Screen.Loading
                    }
                )

                is Screen.Loading -> SplashScreen(status = "Loading channels and guide data…")

                is Screen.Guide -> GuideScreen(
                    data = s.data,
                    weatherCity = city,
                    listState = guideListState,
                    selCategory = selCategory,
                    onSelectCategory = { selCategory = it },
                    selCountry = selCountry,
                    onSelectCountry = { selCountry = it },
                    onPlay = { chs, idx -> screen = Screen.Player(chs, idx, s.data) },
                    onOpenSettings = { screen = Screen.Setup }
                )

                is Screen.Player -> PlayerScreen(s.channels, s.index) { screen = Screen.Guide(s.data) }

                is Screen.Error -> Box(
                    Modifier.fillMaxSize().background(GuideTheme.BG),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Error: ${s.message}\nPress Back to reopen setup.",
                        color = GuideTheme.TEXT, fontSize = 16.sp
                    )
                    BackHandler { screen = Screen.Setup }
                }
            }
        }
    }
}
