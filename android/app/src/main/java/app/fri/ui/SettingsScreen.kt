package app.fri.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fri.data.RepoSettings
import app.fri.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }

    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var token by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }
    var authorEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val s = store.current()
        owner = s.owner
        repo = s.repo
        branch = s.branch
        token = s.token
        authorName = s.authorName
        authorEmail = s.authorEmail
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Token: create a fine-grained PAT on github.com limited to the blog " +
                "repo with Contents read & write. Nothing else.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(owner, { owner = it }, label = { Text("GitHub user/org") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(repo, { repo = it }, label = { Text("Repository") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(branch, { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            token,
            { token = it },
            label = { Text("Personal access token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(authorName, { authorName = it }, label = { Text("Commit author name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(authorEmail, { authorEmail = it }, label = { Text("Commit author email") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                scope.launch {
                    store.save(
                        RepoSettings(
                            owner.trim(),
                            repo.trim(),
                            branch.trim().ifBlank { "main" },
                            token.trim(),
                            authorName.trim(),
                            authorEmail.trim(),
                        ),
                    )
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    nav.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}
