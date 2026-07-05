package app.fri.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.fri.data.GitHubClient
import app.fri.data.PublishQueue
import app.fri.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Drains the publish queue oldest-first; each bundle becomes one commit. */
class PublishWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = SettingsStore(applicationContext).current()
        if (!settings.configured) return@withContext Result.failure()

        val client = GitHubClient(settings)
        for ((bundle, message) in PublishQueue.pending(applicationContext)) {
            try {
                val files = PublishQueue.filesOf(bundle)
                if (files.isNotEmpty()) client.commit(message, files)
                bundle.deleteRecursively()
            } catch (e: IOException) {
                // no signal or GitHub hiccup — leave the bundle, retry later
                return@withContext Result.retry()
            }
        }
        Result.success()
    }
}
