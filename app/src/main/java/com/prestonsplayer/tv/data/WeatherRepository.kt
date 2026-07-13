package com.prestonsplayer.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Weather(
    val tempC: Int,
    val condition: String,
    val icon: String,      // emoji glyph, renders fine on Fire OS
    val city: String
)

/**
 * Open-Meteo: free, no API key, no signup.
 * Two calls: geocode the city name -> fetch current conditions.
 */
object WeatherRepository {

    suspend fun fetch(city: String): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(city.trim(), "UTF-8")
            val geo = JSONObject(
                get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=en&format=json")
            )
            val results = geo.optJSONArray("results") ?: return@runCatching null
            if (results.length() == 0) return@runCatching null
            val place = results.getJSONObject(0)
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")
            val name = place.optString("name", city)

            val wx = JSONObject(
                get(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code" +
                        "&timezone=auto"
                )
            ).getJSONObject("current")

            val code = wx.getInt("weather_code")
            Weather(
                tempC = Math.round(wx.getDouble("temperature_2m")).toInt(),
                condition = describe(code),
                icon = iconFor(code),
                city = name
            )
        }.getOrNull()
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "PrestonsPlayer/0.1")
        return conn.inputStream.use { it.readBytes().decodeToString() }
    }

    // WMO weather interpretation codes
    private fun describe(c: Int) = when (c) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "—"
    }

    private fun iconFor(c: Int) = when (c) {
        0 -> "\u2600\uFE0F"                    // sun
        1, 2 -> "\u26C5"                       // sun behind cloud
        3, 45, 48 -> "\u2601\uFE0F"            // cloud
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "\uD83C\uDF27\uFE0F" // rain
        71, 73, 75, 77, 85, 86 -> "\u2744\uFE0F" // snowflake
        95, 96, 99 -> "\u26C8\uFE0F"           // thunder
        else -> "\uD83C\uDF21\uFE0F"           // thermometer
    }
}
