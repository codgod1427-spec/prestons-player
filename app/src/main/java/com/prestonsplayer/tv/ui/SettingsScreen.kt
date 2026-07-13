package com.prestonsplayer.tv.ui

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
    onSave: (playlist: String, epg: String, city: String) -> Unit
) {
    var playlist by remember { mutableStateOf(initialPlaylist) }
    var epg by remember { mutableStateOf(initialEpg) }
    var city by remember { mutableStateOf(initialCity) }

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
            "Add your playlist. Guide data and weather are optional.",
            color = GuideTheme.TEXT_DIM, fontSize = 14.sp
        )
        Spacer(Modifier.height(26.dp))

        OutlinedTextField(
            value = playlist, onValueChange = { playlist = it },
            label = { Text("M3U playlist URL") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = epg, onValueChange = { epg = it },
            label = { Text("XMLTV guide URL (.xml or .xml.gz)") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = city, onValueChange = { city = it },
            label = { Text("Weather city (e.g. Calgary)") },
            singleLine = true, colors = fieldColors,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = { onSave(playlist, epg, city) },
            enabled = playlist.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = GuideTheme.ACCENT)
        ) {
            Text("Load guide", fontSize = 16.sp)
        }
    }
}
