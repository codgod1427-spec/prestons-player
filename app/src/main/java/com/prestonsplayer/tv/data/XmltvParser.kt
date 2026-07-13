package com.prestonsplayer.tv.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses XMLTV guide data (<programme start=".." stop=".." channel="..">).
 * XMLTV timestamps look like: 20260711183000 -0400
 */
object XmltvParser {

    private val fmtZoned = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val fmtLocal = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    private fun parseTime(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return runCatching { fmtZoned.parse(s)?.time }.getOrNull()
            ?: runCatching { fmtLocal.parse(s.take(14))?.time }.getOrNull()
    }

    fun parse(input: InputStream): Map<String, List<Programme>> {
        val result = mutableMapOf<String, MutableList<Programme>>()
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        var channelId: String? = null
        var start: Long? = null
        var end: Long? = null
        var title: String? = null
        var desc: String? = null
        var inProgramme = false
        var currentTag: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (parser.name == "programme") {
                        inProgramme = true
                        channelId = parser.getAttributeValue(null, "channel")
                        start = parseTime(parser.getAttributeValue(null, "start"))
                        end = parseTime(parser.getAttributeValue(null, "stop"))
                        title = null; desc = null
                    }
                }
                XmlPullParser.TEXT -> if (inProgramme) {
                    when (currentTag) {
                        "title" -> title = (title ?: "") + parser.text
                        "desc" -> desc = (desc ?: "") + parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme") {
                        val cid = channelId
                        val s = start; val e = end
                        if (cid != null && s != null && e != null && e > s) {
                            result.getOrPut(cid) { mutableListOf() } +=
                                Programme(cid, title?.trim() ?: "Untitled", s, e, desc?.trim())
                        }
                        inProgramme = false
                    }
                    currentTag = null
                }
            }
            event = parser.next()
        }
        result.values.forEach { it.sortBy { p -> p.startMs } }
        return result
    }
}
