package app.fri.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * GPS trace, appended locally by TrackService as JSON-lines (cheap appends,
 * survives crashes) and serialized to src/data/route.json on publish. The app
 * owns that file in the repo — single author, no merge worries.
 */
object RouteLog {
    private fun file(context: Context) = File(context.filesDir, "track.jsonl")

    fun append(context: Context, lat: Double, lng: Double, timeMillis: Long) {
        val line = buildJsonObject {
            put("lat", lat)
            put("lng", lng)
            put("t", timeMillis / 1000)
        }
        file(context).appendText(line.toString() + "\n")
    }

    fun pointCount(context: Context): Int {
        val f = file(context)
        if (!f.exists()) return 0
        return f.useLines { lines -> lines.count { it.isNotBlank() } }
    }

    /** Full trace as the route.json payload, or null if nothing logged yet. */
    fun toRouteJsonBytes(context: Context): ByteArray? {
        val f = file(context)
        if (!f.exists()) return null
        val points = f.readLines().filter { it.isNotBlank() }
        if (points.isEmpty()) return null
        val arr = buildJsonArray {
            for (p in points) add(Json.parseToJsonElement(p))
        }
        return arr.toString().toByteArray()
    }
}
