package com.solappan.agent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AccountPanel() {
    val context = LocalContext.current
    val session = remember { SupabaseSession(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var account by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { session.accountEmail() } }
            .onSuccess { account = it }.onFailure { message = "Sign in to restore your account session." }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (BuildConfig.USE_GATEWAY) "Secure gateway mode" else "Local developer demo mode")
        if (BuildConfig.SUPABASE_URL.isBlank()) {
            Text("Account gateway is not configured in this build.")
        } else if (account == null) {
            Text("Beta account · your account must be enabled by the project owner.")
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && email.isNotBlank() && password.isNotEmpty(), onClick = {
                busy = true
                val suppliedPassword = password
                password = ""
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { session.signIn(email, suppliedPassword); session.accountEmail() } }
                    result.onSuccess { account = it; message = "Signed in. Gateway deployment and beta access are still required." }
                        .onFailure { message = it.message ?: "Sign-in failed safely." }
                    busy = false
                }
            }) { Text(if (busy) "Signing in…" else "Sign in") }
        } else {
            Text("Signed in: $account")
            OutlinedButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { session.signOut() } }
                    account = null
                    message = if (result.isSuccess) "Signed out." else "Local session cleared. Remote sign-out failed; revoke sessions from your account."
                    busy = false
                }
            }) { Text("Sign out") }
        }
        if (message.isNotBlank()) Text(message)
    }
}
