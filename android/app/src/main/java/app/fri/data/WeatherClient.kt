package app.fri.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.roundToInt

data class Weather(val temp: Int, val description: String, val windKmh: Int)

/**
 * Same Open-Meteo lookup the site's fetch-weather script does: archive first
 * (lags ~5 days), then the forecast API which covers the recent past.
 * Blocking; run on Dispatchers.IO.
 */
class WeatherClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun fetch(date: String, lat: Double, lng: Double): Weather? {
        val daily = "temperature_2m_mean,weather_code,wind_speed_10m_max"
        val params =
            "latitude=$lat&longitude=$lng&start_date=$date&end_date=$date&daily=$daily&timezone=auto"
        val urls = listOf(
            "https://archive-api.open-meteo.com/v1/archive?$params",
            "https://api.open-meteo.com/v1/forecast?$params",
        )
        for (url in urls) {
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { res ->
                    if (!res.isSuccessful) return@use
                    val root = json.parseToJsonElement(res.body?.string().orEmpty()).jsonObject
                    val d = root["daily"]?.jsonObject ?: return@use
                    val temp =
                        d["temperature_2m_mean"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
                            ?: return@use
                    val code = d["weather_code"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
                    val wind =
                        d["wind_speed_10m_max"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
                    return Weather(
                        temp = temp.roundToInt(),
                        description = describe(code?.roundToInt()),
                        windKmh = (wind ?: 0.0).roundToInt(),
                    )
                }
            } catch (_: Exception) {
                // try the next source
            }
        }
        return null
    }

    private fun describe(code: Int?): String = when {
        code == null -> "unknown"
        code == 0 -> "clear"
        code <= 2 -> "partly cloudy"
        code == 3 -> "overcast"
        code == 45 || code == 48 -> "fog"
        code <= 57 -> "drizzle"
        code <= 67 -> "rain"
        code <= 77 -> "snow"
        code <= 82 -> "rain showers"
        code <= 86 -> "snow showers"
        else -> "thunderstorm"
    }
}
