package app.fri.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fri.data.GitHubClient
import app.fri.data.PublishQueue
import app.fri.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Edits src/data/stats.json — the key-value panel next to the map. */
@Composable
fun StatsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows = remember { mutableStateListOf<Pair<String, String>>() }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    LaunchedEffect(Unit) {
        val settings = SettingsStore(context).current()
        if (!settings.configured) {
            toast("Configure the repo in settings first")
            loaded = true
            return@LaunchedEffect
        }
        try {
            val text = withContext(Dispatchers.IO) {
                GitHubClient(settings).getFileText("src/data/stats.json")
            }
            if (text != null) {
                Json.parseToJsonElement(text).jsonArray.forEach { el ->
                    val o = el.jsonObject
                    rows.add(
                        (o["label"]?.jsonPrimitive?.content ?: "") to
                            (o["value"]?.jsonPrimitive?.content ?: ""),
                    )
                }
            }
        } catch (e: Exception) {
            toast("Could not load current stats: ${e.message}")
        }
        loaded = true
    }

    fun save() {
        busy = true
        scope.launch {
            try {
                val arr = buildJsonArray {
                    for ((label, value) in rows) {
                        if (label.isBlank()) continue
                        add(
                            buildJsonObject {
                                put("label", label.trim())
                                put("value", value.trim())
                            },
                        )
                    }
                }
                withContext(Dispatchers.IO) {
                    PublishQueue.enqueue(
                        context,
                        "Update stats",
                        mapOf("src/data/stats.json" to (arr.toString() + "\n").toByteArray()),
                    )
                }
                toast("Queued — publishes when there's signal")
                nav.popBackStack()
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Trip stats", style = MaterialTheme.typography.headlineMedium)

        if (!loaded) {
            Text("Loading current stats…")
        } else {
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        label,
                        { rows[i] = it to value },
                        label = { Text("Stat") },
                        modifier = Modifier.weight(1.2f),
                    )
                    OutlinedTextField(
                        value,
                        { rows[i] = label to it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { rows.removeAt(i) }) { Text("✕") }
                }
            }
            OutlinedButton(onClick = { rows.add("" to "") }) { Text("Add stat") }
            Button(onClick = ::save, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "Queueing…" else "Save & queue for publish")
            }
        }
    }
}
