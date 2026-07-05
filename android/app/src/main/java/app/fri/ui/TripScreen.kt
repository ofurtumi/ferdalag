package app.fri.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fri.data.PublishQueue
import app.fri.data.RouteLog
import app.fri.data.SettingsStore
import app.fri.service.TrackService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun TripScreen(nav: NavController) {
    val context = LocalContext.current
    var tracking by remember { mutableStateOf(TrackService.running) }
    var pendingCount by remember { mutableIntStateOf(PublishQueue.pending(context).size) }
    var pointCount by remember { mutableIntStateOf(RouteLog.pointCount(context)) }
    val configured = remember {
        runBlocking { SettingsStore(context).settings.first().configured }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            TrackService.start(context)
            tracking = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Fri Camper", style = MaterialTheme.typography.headlineMedium)

        if (!configured) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Not connected to the blog repo yet.")
                    Button(onClick = { nav.navigate("settings") }) { Text("Open settings") }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Record route", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = tracking,
                        onCheckedChange = { on ->
                            if (on) {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                                if (Build.VERSION.SDK_INT >= 33) {
                                    perms += Manifest.permission.POST_NOTIFICATIONS
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            } else {
                                TrackService.stop(context)
                                tracking = false
                            }
                        },
                    )
                }
                Text(
                    "$pointCount GPS points logged so far. The trace goes up with the next publish.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Publish queue", style = MaterialTheme.typography.titleMedium)
                if (pendingCount == 0) {
                    Text("Nothing waiting — all published.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "$pendingCount bundle(s) waiting for signal.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = {
                        PublishQueue.schedule(context)
                        pendingCount = PublishQueue.pending(context).size
                    }) { Text("Try publishing now") }
                }
            }
        }

        HorizontalDivider()

        Button(onClick = { nav.navigate("post") }, modifier = Modifier.fillMaxWidth()) {
            Text("New post")
        }
        OutlinedButton(onClick = { nav.navigate("stats") }, modifier = Modifier.fillMaxWidth()) {
            Text("Trip stats")
        }
        OutlinedButton(onClick = { nav.navigate("settings") }, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
