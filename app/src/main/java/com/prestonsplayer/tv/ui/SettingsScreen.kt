package com.prestonsplayer.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialPlaylist: String,
    initialEpg: String,
    initialCity: String,
    canCancel: Boolean = false,
    onCancel: () -> Unit = {},
    onSave: (playlist: String, epg: String, city: String) -> Unit
) {
    var playlist by remember { mutableStateOf(initialPlaylist) }
    var epg by remember { mutableStateOf(initialEpg) }
    var city by remember { mutableStateOf(initialCity) }

    // Press Back on the remote to return to the guide (only if one is already loaded).
    if (canCancel) BackHandler { onCancel() }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = GuideTheme.TEXT,
        unfocusedTextColor = GuideTheme.TEXT,
        focusedBorderColor = GuideTheme.ACCENT,
        unfocusedBorderColor = GuideTheme.LINE,
        focusedLabelColor = GuideTheme.ACCENT,
        unfocusedLabelColor = GuideTheme.TEXT_DIM,
        cursorColor = GuideTheme.ACCENT
    )

    Column(
        Modifier.fillMaxSize().background(GuideTheme.BG).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Preston's Player Setup", color = GuideTheme.TEXT, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste your IPTV playlist link below. The other two fields are optional — you can leave them blank.",
            color = GuideTheme.TEXT_DIM, fontSize = 14.sp
        )
        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = playlist, onValueChange = { playlist = it },
            label = { Text("M3U playlist URL  —  required") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Text(
            "Example: https://iptv-org.github.io/iptv/index.m3u",
            color = GuideTheme.TEXT_DIM, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(0.75f).padding(top = 4.dp, start = 4.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = epg, onValueChange = { epg = it },
            label = { Text("XMLTV guide URL  —  optional (.xml or .xml.gz)") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = city, onValueChange = { city = it },
            label = { Text("Weather city  —  optional (e.g. Calgary)") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Spacer(Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canCancel) {
                OutlinedButton(onClick = { onCancel() }) { Text("Back") }
                Spacer(Modifier.width(14.dp))
            }
            Button(
                onClick = { onSave(playlist, epg, city) },
                enabled = playlist.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GuideTheme.ACCENT)
            ) {
                Text("Load guide", fontSize = 16.sp)
            }
        }
        if (canCancel) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Or press Back on your remote to return to the guide.",
                color = GuideTheme.TEXT_DIM, fontSize = 12.sp
            )
        }
    }
}
