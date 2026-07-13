package com.prestonsplayer.tv.data

data class Channel(
    val id: String,          // tvg-id (links to XMLTV) or generated
    val name: String,
    val number: Int,         // display channel number
    val logoUrl: String?,
    val group: String?,
    val streamUrl: String,
    val country: String? = null,        // 2-letter code from tvg-id, e.g. "us", "ca"
    val categories: List<String> = emptyList()  // from group-title, e.g. ["News"], ["Movies","Kids"]
)

data class Programme(
    val channelId: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val description: String? = null
) {
    fun isAiringAt(t: Long) = t in startMs until endMs
}

data class GuideData(
    val channels: List<Channel>,
    val programmes: Map<String, List<Programme>> // channelId -> sorted by start
)
