package com.prestonsplayer.tv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class GuideRepository(context: Context) {

    private val prefs = context.getSharedPreferences("prestonsplayer", Context.MODE_PRIVATE)

    var playlistUrl: String
        get() = prefs.getString("playlist_url", "") ?: ""
        set(v) { prefs.edit().putString("playlist_url", v.trim()).apply() }

    var epgUrl: String
        get() = prefs.getString("epg_url", "") ?: ""
        set(v) { prefs.edit().putString("epg_url", v.trim()).apply() }

    /** City name for the header weather widget, e.g. "Calgary" or "Toronto, Ontario". */
    var weatherCity: String
        get() = prefs.getString("weather_city", "") ?: ""
        set(v) { prefs.edit().putString("weather_city", v.trim()).apply() }

    suspend fun load(): GuideData = withContext(Dispatchers.IO) {
        val channels = M3uParser.parse(fetchText(playlistUrl))
        val programmes = if (epgUrl.isNotBlank()) {
            runCatching {
                open(epgUrl).use { XmltvParser.parse(it) }
            }.getOrDefault(emptyMap())
        } else emptyMap()
        GuideData(channels, programmes)
    }

    private fun open(url: String): java.io.InputStream {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "PrestonsPlayer/0.1")
        val stream = BufferedInputStream(conn.inputStream)
        // Many EPG feeds ship as .xml.gz
        return if (url.endsWith(".gz") || conn.contentEncoding == "gzip") GZIPInputStream(stream) else stream
    }

    private fun fetchText(url: String): String = open(url).use { it.readBytes().decodeToString() }
}
