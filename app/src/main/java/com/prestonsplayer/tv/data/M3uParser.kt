package com.prestonsplayer.tv.data

/**
 * Parses standard IPTV M3U / M3U8 playlists.
 *
 * Handles lines like:
 * #EXTINF:-1 tvg-id="CBC.ca" tvg-logo="https://.../cbc.png" group-title="News",CBC Toronto
 * https://example.com/stream.m3u8
 */
object M3uParser {

    private val ATTR = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var number = 1

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingAttrs = ATTR.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    pendingName = line.substringAfterLast(",").trim().ifEmpty { null }
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val name = pendingName ?: "Channel $number"
                    val tvgId = pendingAttrs["tvg-id"].orEmpty()
                    channels += Channel(
                        id = tvgId.ifEmpty { null } ?: name,
                        name = name,
                        number = number++,
                        logoUrl = pendingAttrs["tvg-logo"]?.ifEmpty { null },
                        group = pendingAttrs["group-title"]?.ifEmpty { null },
                        streamUrl = line,
                        country = countryOf(tvgId),
                        categories = categoriesOf(pendingAttrs["group-title"])
                    )
                    pendingName = null
                    pendingAttrs = emptyMap()
                }
            }
        }
        return channels
    }

    /** iptv-org encodes country in the tvg-id: "00sReplay.us@SD" -> "us", "3ABNCanada.ca" -> "ca". */
    private fun countryOf(tvgId: String): String? {
        if (tvgId.isBlank()) return null
        val code = tvgId.substringBefore('@').substringAfterLast('.', "")
        return if (code.length == 2 && code.all { it.isLetter() }) code.lowercase() else null
    }

    /** group-title may hold several categories, e.g. "News;Public" -> ["News","Public"]. */
    private fun categoriesOf(group: String?): List<String> =
        group?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() && !it.equals("Undefined", true) }
            ?: emptyList()
}
