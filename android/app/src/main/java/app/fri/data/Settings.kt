package app.fri.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

/**
 * The GitHub token is a fine-grained PAT limited to the blog repo with
 * Contents read/write only. It lives in app-private storage on a personal
 * device — good enough for a single-user tool.
 */
data class RepoSettings(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
) {
    val configured: Boolean
        get() = owner.isNotBlank() && repo.isNotBlank() && token.isNotBlank()
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val owner = stringPreferencesKey("owner")
        val repo = stringPreferencesKey("repo")
        val branch = stringPreferencesKey("branch")
        val token = stringPreferencesKey("token")
        val authorName = stringPreferencesKey("author_name")
        val authorEmail = stringPreferencesKey("author_email")
    }

    val settings: Flow<RepoSettings> = context.store.data.map { p ->
        RepoSettings(
            owner = p[Keys.owner] ?: "",
            repo = p[Keys.repo] ?: "",
            branch = p[Keys.branch] ?: "main",
            token = p[Keys.token] ?: "",
            authorName = p[Keys.authorName] ?: "",
            authorEmail = p[Keys.authorEmail] ?: "",
        )
    }

    suspend fun current(): RepoSettings = settings.first()

    suspend fun save(s: RepoSettings) {
        context.store.edit { p ->
            p[Keys.owner] = s.owner
            p[Keys.repo] = s.repo
            p[Keys.branch] = s.branch
            p[Keys.token] = s.token
            p[Keys.authorName] = s.authorName
            p[Keys.authorEmail] = s.authorEmail
        }
    }
}
