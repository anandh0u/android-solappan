package com.solappan.agent

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.solappan.agent.accessibility.AccessibilityProtocol
import com.solappan.agent.assistant.SolWakeWordService
import com.solappan.agent.tools.ToolConfirmation
import com.solappan.agent.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = SolColors) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SolChatScreen()
                }
            }
        }
    }
}

private val SolColors = darkColorScheme(
    primary = Color(0xFF8B7CFF),
    onPrimary = Color.White,
    secondary = Color(0xFF4FE0B5),
    background = Color(0xFF090B12),
    surface = Color(0xFF141722),
    surfaceVariant = Color(0xFF202432),
    onSurface = Color(0xFFF5F4FA),
    onSurfaceVariant = Color(0xFFA8ACBA),
    outline = Color(0xFF343949),
    error = Color(0xFFFF747D),
)

private data class PendingApproval(
    val request: ToolConfirmation,
    val decision: CompletableDeferred<Boolean>,
)

@Composable
private fun SolChatScreen() {
    val context = LocalContext.current
    val controller = remember(context) {
        AgentController(registry = ToolRegistry.sessionThree(context.applicationContext))
    }
    val runtime = remember(controller) { AgentRuntimeCoordinator(controller) }
    var goal by remember { mutableStateOf("") }
    var submittedGoal by remember { mutableStateOf("") }
    var runtimeState by remember { mutableStateOf(AgentRuntimeUiState()) }
    var showSetup by remember { mutableStateOf(false) }
    var pendingApproval by remember { mutableStateOf<PendingApproval?>(null) }
    var approvalReady by remember { mutableStateOf(false) }
    var approvalReviewed by remember { mutableStateOf(false) }
    var contactsGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var microphoneGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var notificationGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    var accessibilityEnabled by remember { mutableStateOf(AccessibilityProtocol.isEnabled(context)) }
    var wakeEnabled by remember { mutableStateOf(isWakeServiceRunning(context)) }
    val scope = rememberCoroutineScope()

    val roleManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java) else null
    }
    val roleAvailable = remember(roleManager) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
    }
    var roleHeld by remember(roleManager) {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true)
    }

    val assistantRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsGranted = it
    }
    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        microphoneGranted = it
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        accessibilityEnabled = AccessibilityProtocol.isEnabled(context)
    }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to SOL")
        }
    }
    val speechAvailable = remember { speechIntent.resolveActivity(context.packageManager) != null }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()?.takeIf(String::isNotEmpty)?.let { goal = it }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = AccessibilityProtocol.isEnabled(context)
                roleHeld = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
                wakeEnabled = isWakeServiceRunning(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }
    var ttsReady by remember { mutableStateOf(false) }
    var lastSpoken by remember { mutableStateOf("") }
    DisposableEffect(context) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsHolder[0]?.language = Locale.getDefault()
        }
        ttsHolder[0] = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsHolder[0] = null
        }
    }
    LaunchedEffect(runtimeState.loading, runtimeState.response, runtimeState.error, ttsReady) {
        if (!runtimeState.loading && ttsReady) {
            val spoken = runtimeState.response.ifBlank { runtimeState.error.orEmpty() }.takeIf(String::isNotBlank)
            if (spoken != null && spoken != lastSpoken) {
                lastSpoken = spoken
                ttsHolder[0]?.speak(spoken.take(1_200), TextToSpeech.QUEUE_FLUSH, null, "sol_main_response")
            }
        }
    }

    LaunchedEffect(pendingApproval) {
        approvalReady = false
        approvalReviewed = false
        if (pendingApproval != null) {
            delay(750)
            approvalReady = true
        }
    }

    fun submit() {
        if (runtimeState.loading || goal.isBlank()) return
        val request = goal.trim()
        submittedGoal = request
        goal = ""
        runtimeState = runtimeState.beginRun()
        scope.launch {
            withContext(Dispatchers.IO) {
                runtime.run(
                    goal = request,
                    initialState = runtimeState,
                    onState = { state -> withContext(Dispatchers.Main) { runtimeState = state } },
                    requestConfirmation = { confirmation ->
                        val decision = CompletableDeferred<Boolean>()
                        withContext(Dispatchers.Main) { pendingApproval = PendingApproval(confirmation, decision) }
                        decision.await().also { withContext(Dispatchers.Main) { pendingApproval = null } }
                    },
                )
            }
        }
    }

    fun toggleWakeWord() {
        if (wakeEnabled) {
            context.stopService(Intent(context, SolWakeWordService::class.java))
            wakeEnabled = false
            return
        }
        if (!microphoneGranted) {
            microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        runCatching {
            context.startForegroundService(
                Intent(context, SolWakeWordService::class.java).setAction(SolWakeWordService.ACTION_START),
            )
            wakeEnabled = true
        }.onFailure { Log.w("SolWakeWord", "Wake service could not start", it) }
    }

    pendingApproval?.let { pending ->
        ApprovalDialog(
            pending.request,
            approvalReady,
            approvalReviewed,
            { approvalReviewed = it },
            { if (pending.decision.complete(false)) pendingApproval = null },
            { if (pending.decision.complete(true)) pendingApproval = null },
        )
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
        SolHeader(wakeEnabled, ::toggleWakeWord) { showSetup = !showSetup }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showSetup) {
                item {
                    SetupPanel(
                        BuildConfig.OPENAI_API_KEY.isNotBlank(),
                        contactsGranted,
                        microphoneGranted,
                        roleAvailable,
                        roleHeld,
                        accessibilityEnabled,
                        { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        { microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        {
                            runCatching {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager
                                    ?.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                                    ?.let(assistantRoleLauncher::launch)
                                    ?: context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                                else context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                            }
                        },
                        { accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                }
            }
            if (submittedGoal.isBlank() && runtimeState.response.isBlank() && runtimeState.error == null) {
                item { EmptyConversation() }
            } else {
                if (submittedGoal.isNotBlank()) item { ChatBubble(submittedGoal, true) }
                if (runtimeState.timeline.isNotEmpty()) item { TimelineCard(runtimeState.timeline) }
                runtimeState.error?.let { message -> item { ChatBubble(message, false, true) } }
                if (runtimeState.response.isNotBlank()) item { ChatBubble(runtimeState.response, false) }
            }
        }

        if (runtimeState.loading) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stateLabel(runtimeState.agentState), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = speechAvailable && !runtimeState.loading,
                onClick = {
                    if (microphoneGranted) speechLauncher.launch(speechIntent)
                    else microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            ) { Text("Mic") }
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask SOL…") },
                minLines = 1,
                maxLines = 4,
                enabled = !runtimeState.loading,
            )
            Button(enabled = goal.isNotBlank() && !runtimeState.loading, onClick = ::submit) { Text("Send") }
        }
    }
}

@Composable
private fun SolHeader(wakeEnabled: Boolean, onWakeToggle: () -> Unit, onSetupToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(46.dp).background(
                Brush.linearGradient(listOf(Color(0xFF8B7CFF), Color(0xFF4FE0B5))),
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) { Text("✦", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SOL", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Android agent", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        TextButton(onClick = onWakeToggle) { Text(if (wakeEnabled) "Hey SOL: On" else "Hey SOL: Off") }
        TextButton(onClick = onSetupToggle) { Text("Setup") }
    }
}

@Composable
private fun EmptyConversation() {
    Column(
        Modifier.fillMaxWidth().padding(top = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(92.dp).background(Color(0xFF202432), CircleShape), contentAlignment = Alignment.Center) {
            Text("✦", color = MaterialTheme.colorScheme.secondary, fontSize = 46.sp)
        }
        Text("What can I do for you?", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Text("Type, tap Mic, or enable Hey SOL", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean, error: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = when {
                error -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                fromUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(20.dp),
        ) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) }
    }
}

@Composable
private fun TimelineCard(entries: List<TimelineEntry>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            entries.takeLast(6).forEach { entry ->
                val marker = when (entry.status) {
                    TimelineStatus.RUNNING -> "●"
                    TimelineStatus.SUCCESS -> "✓"
                    TimelineStatus.FAILED -> "!"
                    TimelineStatus.CANCELLED -> "×"
                }
                Text(
                    "$marker  ${entry.toolName.replace('_', ' ')} — ${entry.message}",
                    color = if (entry.status == TimelineStatus.SUCCESS) MaterialTheme.colorScheme.secondary
                    else if (entry.status == TimelineStatus.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SetupPanel(
    modelReady: Boolean,
    contactsGranted: Boolean,
    microphoneGranted: Boolean,
    roleAvailable: Boolean,
    roleHeld: Boolean,
    accessibilityEnabled: Boolean,
    onContacts: () -> Unit,
    onMicrophone: () -> Unit,
    onRole: () -> Unit,
    onAccessibility: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Setup", fontWeight = FontWeight.SemiBold)
            Text(statusLine("Model", modelReady))
            Text(statusLine("Microphone", microphoneGranted))
            Text(statusLine("Contacts", contactsGranted))
            Text(statusLine("Default assistant", roleHeld))
            Text(statusLine("Screen control", accessibilityEnabled))
            if (!microphoneGranted) OutlinedButton(onClick = onMicrophone) { Text("Enable microphone") }
            if (!contactsGranted) OutlinedButton(onClick = onContacts) { Text("Enable contacts") }
            if (roleAvailable && !roleHeld) OutlinedButton(onClick = onRole) { Text("Set as default assistant") }
            if (!accessibilityEnabled) OutlinedButton(onClick = onAccessibility) { Text("Enable screen control") }
        }
    }
}

@Composable
private fun ApprovalDialog(
    request: ToolConfirmation,
    enabled: Boolean,
    reviewed: Boolean,
    onReviewed: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onApprove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (enabled) onCancel() },
        title = { Text("Approve action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(request.summary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reviewed, onCheckedChange = onReviewed, enabled = enabled)
                    Text("I reviewed this action")
                }
            }
        },
        dismissButton = { OutlinedButton(enabled = enabled, onClick = onCancel) { Text("Cancel") } },
        confirmButton = { Button(enabled = enabled && reviewed, onClick = onApprove) { Text("Approve") } },
    )
}

private fun statusLine(name: String, enabled: Boolean) = "${if (enabled) "✓" else "○"} $name"

@Suppress("DEPRECATION")
private fun isWakeServiceRunning(context: Context): Boolean =
    context.getSystemService(ActivityManager::class.java)
        ?.getRunningServices(Int.MAX_VALUE)
        ?.any { it.service.className == SolWakeWordService::class.java.name } == true

private fun stateLabel(state: AgentState): String = state.name.lowercase(Locale.ROOT)
    .replace('_', ' ').replaceFirstChar(Char::uppercase)
