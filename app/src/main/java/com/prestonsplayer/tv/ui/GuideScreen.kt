package com.prestonsplayer.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prestonsplayer.tv.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHANNEL_COL_WIDTH = 190.dp
private val ROW_HEIGHT = 64.dp
private val DP_PER_MIN = 4.dp
private const val WINDOW_HOURS = 12L
private const val SLOT_MIN = 30L
private val HEADER_HEIGHT = 150.dp

private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
private val clockFmt = SimpleDateFormat("h:mm", Locale.getDefault())
private val ampmFmt = SimpleDateFormat("a", Locale.getDefault())
private val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

@Composable
fun GuideScreen(
    data: GuideData,
    weatherCity: String,
    listState: LazyListState,
    selCategory: String?,
    onSelectCategory: (String?) -> Unit,
    selCountry: String?,
    onSelectCountry: (String?) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val gridStart = remember {
        val n = System.currentTimeMillis(); n - (n % (SLOT_MIN * 60_000))
    }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = System.currentTimeMillis() } }

    // Weather: fetch on open, refresh every 20 minutes
    var weather by remember { mutableStateOf<Weather?>(null) }
    LaunchedEffect(weatherCity) {
        while (weatherCity.isNotBlank()) {
            weather = WeatherRepository.fetch(weatherCity) ?: weather
            delay(20 * 60_000)
        }
    }

    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var focusedProgramme by remember { mutableStateOf<Programme?>(null) }
    val hScroll = rememberScrollState()

    // ---- Filters: type (group-title) and country (from tvg-id) ----
    val allCategories = remember(data.channels) {
        val counts = HashMap<String, Int>()
        data.channels.forEach { c -> c.categories.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        counts.entries.sortedByDescending { it.value }.map { it.key }
    }
    val allCountries = remember(data.channels) {
        val counts = HashMap<String, Int>()
        data.channels.forEach { c -> c.country?.let { counts[it] = (counts[it] ?: 0) + 1 } }
        counts.entries.sortedByDescending { it.value }.map { it.key }
    }
    val channels = remember(data.channels, selCategory, selCountry) {
        data.channels.filter { c ->
            (selCategory == null || c.categories.contains(selCategory)) &&
                (selCountry == null || c.country == selCountry)
        }
    }

    Column(Modifier.fillMaxSize().background(GuideTheme.BG)) {

        // ============ HEADER: parkour marquee behind PiP + info + clock/weather ============
        Box(Modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
            ParkourMarquee(Modifier.matchParentSize())
            // scrim so the letters read as a backdrop and the widgets stay legible
            Box(Modifier.matchParentSize().background(GuideTheme.SURFACE_ALT.copy(alpha = 0.42f)))
            Row(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // --- Live PiP window (16:9) ---
            PipPreview(
                channel = focusedChannel,
                modifier = Modifier.width(198.dp).height(112.dp)
            )

            Spacer(Modifier.width(18.dp))

            // --- Focused programme info ---
            Column(Modifier.weight(1f)) {
                Text(
                    focusedProgramme?.title ?: focusedChannel?.name ?: "TV Guide",
                    color = GuideTheme.TEXT, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val line2 = buildString {
                    focusedChannel?.let { append("${it.number}  ${it.name}") }
                    focusedProgramme?.let {
                        if (isNotEmpty()) append("   •   ")
                        append("${timeFmt.format(Date(it.startMs))} – ${timeFmt.format(Date(it.endMs))}")
                    }
                    if (isEmpty()) append("Select a program")
                }
                Text(line2, color = GuideTheme.TEXT_DIM, fontSize = 14.sp, maxLines = 1)
                focusedProgramme?.description?.let { d ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        d, color = GuideTheme.TEXT_DIM, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            // --- Clock / date / weather stack (right rail) ---
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        clockFmt.format(Date(now)),
                        color = GuideTheme.TEXT, fontSize = 34.sp, fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        ampmFmt.format(Date(now)),
                        color = GuideTheme.TEXT_DIM, fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(dateFmt.format(Date(now)), color = GuideTheme.TEXT_DIM, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                weather?.let { w ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(w.icon, fontSize = 17.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${w.tempC}°",
                            color = GuideTheme.TEXT, fontSize = 17.sp, fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${w.condition} · ${w.city}",
                            color = GuideTheme.TEXT_DIM, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))
            Text(
                "Settings",
                color = GuideTheme.TEXT_DIM, fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            }
        }

        // ================= Filter chips: Type + Country =================
        if (allCategories.isNotEmpty())
            FilterChips("Type", allCategories, selCategory, { it }, onSelectCategory)
        if (allCountries.isNotEmpty())
            FilterChips("Country", allCountries, selCountry, { countryName(it) }, onSelectCountry)

        // ================= Timeline header =================
        Row(Modifier.fillMaxWidth().background(GuideTheme.SURFACE)) {
            Box(Modifier.width(CHANNEL_COL_WIDTH).height(34.dp))
            Row(Modifier.horizontalScroll(hScroll, enabled = false)) {
                repeat((WINDOW_HOURS * 60 / SLOT_MIN).toInt()) { i ->
                    val t = gridStart + i * SLOT_MIN * 60_000
                    Box(
                        Modifier.width(DP_PER_MIN * SLOT_MIN.toInt()).height(34.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            timeFmt.format(Date(t)),
                            color = GuideTheme.TEXT_DIM, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // ================= Channel rows =================
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(channels, key = { _, c -> c.number }) { index, channel ->
                val rowBg = if (index % 2 == 0) GuideTheme.BG else GuideTheme.SURFACE_ALT
                Row(Modifier.fillMaxWidth().height(ROW_HEIGHT).background(rowBg)) {

                    Row(
                        Modifier
                            .width(CHANNEL_COL_WIDTH).fillMaxHeight()
                            .clickable { onPlay(channels, index) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            channel.number.toString(),
                            color = GuideTheme.TEXT_DIM, fontSize = 13.sp,
                            modifier = Modifier.width(34.dp)
                        )
                        channel.logoUrl?.let {
                            AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            channel.name, color = GuideTheme.TEXT, fontSize = 13.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(Modifier.horizontalScroll(hScroll)) {
                        ProgrammeStrip(
                            channel = channel,
                            programmes = data.programmes[channel.id].orEmpty(),
                            gridStart = gridStart,
                            now = now,
                            onFocus = { p -> focusedChannel = channel; focusedProgramme = p },
                            onClick = { onPlay(channels, index) }
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(GuideTheme.LINE))
            }
        }
    }
}

@Composable
private fun ProgrammeStrip(
    channel: Channel,
    programmes: List<Programme>,
    gridStart: Long,
    now: Long,
    onFocus: (Programme?) -> Unit,
    onClick: () -> Unit
) {
    val gridEnd = gridStart + WINDOW_HOURS * 3_600_000
    val visible = programmes.filter { it.endMs > gridStart && it.startMs < gridEnd }

    if (visible.isEmpty()) {
        GuideCell(
            title = if (programmes.isEmpty()) channel.name else "No guide data",
            widthMin = (WINDOW_HOURS * 60).toInt(),
            airingNow = true, onFocus = { onFocus(null) }, onClick = onClick
        )
        return
    }

    var cursor = gridStart
    for (p in visible) {
        if (p.startMs > cursor) {
            val gapMin = ((p.startMs - cursor) / 60_000).toInt()
            if (gapMin > 0) GuideCell("—", gapMin, false, { onFocus(null) }, onClick)
        }
        val cellStart = maxOf(p.startMs, gridStart)
        val cellEnd = minOf(p.endMs, gridEnd)
        val durMin = ((cellEnd - cellStart) / 60_000).toInt().coerceAtLeast(5)
        GuideCell(p.title, durMin, p.isAiringAt(now), { onFocus(p) }, onClick)
        cursor = p.endMs
    }
}

@Composable
private fun GuideCell(
    title: String,
    widthMin: Int,
    airingNow: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bg = when {
        isFocused -> GuideTheme.ACCENT
        airingNow -> GuideTheme.ACCENT_SOFT
        else -> GuideTheme.SURFACE
    }
    Box(
        Modifier
            .width(DP_PER_MIN * widthMin)
            .height(ROW_HEIGHT)
            .padding(horizontal = 1.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            title,
            color = if (isFocused) Color.White else GuideTheme.TEXT,
            fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChips(
    label: String,
    options: List<String>,
    selected: String?,
    display: (String) -> String,
    onSelect: (String?) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(GuideTheme.SURFACE)
            .padding(vertical = 4.dp)
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, color = GuideTheme.TEXT_DIM, fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp).width(58.dp)
        )
        Chip("All", selected == null) { onSelect(null) }
        options.forEach { opt -> Chip(display(opt), selected == opt) { onSelect(opt) } }
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> GuideTheme.ACCENT
        active -> GuideTheme.ACCENT_SOFT
        else -> GuideTheme.SURFACE_ALT
    }
    Text(
        text,
        color = if (focused || active) Color.White else GuideTheme.TEXT,
        fontSize = 13.sp,
        maxLines = 1,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Friendly name for a 2-letter country code; falls back to the uppercased code. */
private fun countryName(code: String): String = COUNTRY_NAMES[code] ?: code.uppercase()

private val COUNTRY_NAMES = mapOf(
    "us" to "USA", "uk" to "UK", "gb" to "UK", "ca" to "Canada", "au" to "Australia",
    "de" to "Germany", "fr" to "France", "es" to "Spain", "it" to "Italy", "nl" to "Netherlands",
    "ru" to "Russia", "br" to "Brazil", "mx" to "Mexico", "ar" to "Argentina", "in" to "India",
    "pk" to "Pakistan", "tr" to "Turkey", "pl" to "Poland", "pt" to "Portugal", "se" to "Sweden",
    "no" to "Norway", "dk" to "Denmark", "fi" to "Finland", "gr" to "Greece", "ua" to "Ukraine",
    "ro" to "Romania", "cz" to "Czechia", "at" to "Austria", "ch" to "Switzerland", "be" to "Belgium",
    "ie" to "Ireland", "jp" to "Japan", "kr" to "Korea", "cn" to "China", "id" to "Indonesia",
    "ph" to "Philippines", "th" to "Thailand", "vn" to "Vietnam", "my" to "Malaysia", "sg" to "Singapore",
    "za" to "S. Africa", "ng" to "Nigeria", "eg" to "Egypt", "ke" to "Kenya", "ma" to "Morocco",
    "sa" to "Saudi Arabia", "ae" to "UAE", "il" to "Israel", "ir" to "Iran", "iq" to "Iraq",
    "co" to "Colombia", "cl" to "Chile", "pe" to "Peru", "ve" to "Venezuela", "nz" to "New Zealand"
)
