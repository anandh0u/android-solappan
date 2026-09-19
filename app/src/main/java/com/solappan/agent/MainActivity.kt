package com.solappan.agent

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solappan.agent.tools.ToolConfirmation
import com.solappan.agent.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = SolappanColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AgentScreen()
                }
            }
        }
    }
}

private val SolappanColors = darkColorScheme(
    primary = Color(0xFFFF9B54),
    onPrimary = Color(0xFF21110B),
    primaryContainer = Color(0xFF4A2630),
    onPrimaryContainer = Color(0xFFFFD8C2),
    secondary = Color(0xFFE66BCB),
    background = Color(0xFF100D1A),
    surface = Color(0xFF1A1527),
    surfaceVariant = Color(0xFF251E35),
    onSurface = Color(0xFFF3EEF7),
    onSurfaceVariant = Color(0xFFB7AEC3),
    outline = Color(0xFF5E536B),
    error = Color(0xFFFF716A),
)

private data class PendingApproval(
    val request: ToolConfirmation,
    val decision: CompletableDeferred<Boolean>,
)

@Composable
private fun AgentScreen() {
    val context = LocalContext.current
    val controller = remember(context) {
        AgentController(registry = ToolRegistry.sessionThree(context.applicationContext))
    }
    val runtime = remember(controller) { AgentRuntimeCoordinator(controller) }
    var goal by remember { mutableStateOf("") }
    var runtimeState by remember { mutableStateOf(AgentRuntimeUiState()) }
    val response = runtimeState.response
    val error = runtimeState.error
    val loading = runtimeState.loading
    val agentState = runtimeState.agentState
    val timeline = runtimeState.timeline
    var pendingApproval by remember { mutableStateOf<PendingApproval?>(null) }
    var approvalReady by remember { mutableStateOf(false) }
    var approvalReviewed by remember { mutableStateOf(false) }
    var contactsGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var microphoneGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val roleManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java)
        else null
    }
    val assistantRoleAvailable = remember(roleManager) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
    }
    var assistantRoleHeld by remember(roleManager) {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true)
    }
    fun openAssistantSettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
            .onFailure { Log.w("SolappanAssistant", "Assistant settings are unavailable") }
    }
    val assistantRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        assistantRoleHeld = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        if (!assistantRoleHeld) openAssistantSettings()
    }
    val contactPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsGranted = it
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        microphoneGranted = it
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { goal = it }
        }
    }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe an Android workflow")
        }
    }
    val speechAvailable = remember { speechIntent.resolveActivity(context.packageManager) != null }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pendingApproval) {
        approvalReady = false
        approvalReviewed = false
        if (pendingApproval != null) {
            // Prevent the gesture that launched a workflow from clicking through
            // into a confirmation button when the layout changes underneath it.
            delay(750)
            approvalReady = true
        }
    }

    fun reset() {
        goal = ""
        runtimeState = AgentRuntimeUiState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("● Model ready", BuildConfig.OPENAI_API_KEY.isNotBlank())
            StatusPill("● Contacts", contactsGranted)
            StatusPill("● Microphone", microphoneGranted)
        }

        AssistantRoleSetup(
            available = assistantRoleAvailable,
            held = assistantRoleHeld,
            onRequestRole = {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager
                        ?.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                        ?.let(assistantRoleLauncher::launch)
                        ?: openAssistantSettings()
                    else openAssistantSettings()
                }.onFailure { openAssistantSettings() }
            },
        )

        StateCard(agentState)

        pendingApproval?.let { pending ->
            ConfirmationCard(
                request = pending.request,
                actionsEnabled = approvalReady,
                reviewed = approvalReviewed,
                onReviewedChange = { approvalReviewed = it },
                onCancel = {
                    Log.i("SolappanApproval", "User denied ${pending.request.toolName}")
                    if (pending.decision.complete(false)) pendingApproval = null
                },
                onApprove = {
                    Log.i("SolappanApproval", "User confirmed ${pending.request.toolName}")
                    if (pending.decision.complete(true)) pendingApproval = null
                },
            )
        }

        if (!contactsGranted) {
            OutlinedButton(onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                Text("Enable contact tools")
            }
        }

        if (!microphoneGranted) {
            OutlinedButton(onClick = { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Enable assistant microphone")
            }
        }

        if (!loading && goal.isBlank() && agentState == AgentState.IDLE) {
            Text("Try a workflow", style = MaterialTheme.typography.titleMedium)
            DemoPrompts(onSelect = { goal = it })
        }

        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What do you want to accomplish?") },
            placeholder = { Text("Describe the outcome — the agent handles the steps") },
            minLines = 3,
            enabled = !loading,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (speechAvailable && !loading) {
                OutlinedButton(onClick = { speechLauncher.launch(speechIntent) }) {
                    Text("Speak")
                }
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !loading && goal.isNotBlank(),
                onClick = {
                    if (runtimeState.loading) return@Button
                    val submittedGoal = goal.trim()
                    runtimeState = runtimeState.beginRun()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runtime.run(
                                goal = submittedGoal,
                                initialState = runtimeState,
                                onState = { state ->
                                    withContext(Dispatchers.Main) {
                                        runtimeState = state
                                    }
                                },
                                requestConfirmation = { request ->
                                    val decision = CompletableDeferred<Boolean>()
                                    withContext(Dispatchers.Main) {
                                        Log.i("SolappanApproval", "Showing confirmation for ${request.toolName}")
                                        pendingApproval = PendingApproval(request, decision)
                                    }
                                    decision.await().also { approved ->
                                        Log.i("SolappanApproval", "Decision for ${request.toolName}: $approved")
                                        withContext(Dispatchers.Main) { pendingApproval = null }
                                    }
                                },
                            )
                        }
                    }
                },
            ) { Text(if (loading) "Running…" else "Run workflow") }

            if (!loading && agentState != AgentState.IDLE) {
                OutlinedButton(onClick = ::reset) { Text("Reset") }
            }
        }

        if (loading && pendingApproval == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(stateLabel(agentState))
            }
        }

        if (timeline.isNotEmpty()) {
            Text("Execution timeline", style = MaterialTheme.typography.titleMedium)
            timeline.forEach { entry ->
                val marker = when (entry.status) {
                    TimelineStatus.RUNNING -> "●"
                    TimelineStatus.SUCCESS -> "✓"
                    TimelineStatus.FAILED -> "!"
                    TimelineStatus.CANCELLED -> "×"
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(marker, color = timelineColor(entry.status), style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(entry.toolName.replace('_', ' '), style = MaterialTheme.typography.titleSmall)
                            Text(entry.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        error?.let {
            Text("Couldn’t complete the goal", style = MaterialTheme.typography.titleMedium)
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (response.isNotBlank()) {
            Text("Result", style = MaterialTheme.typography.titleMedium)
            Text(response)
        }
    }
}

@Composable
private fun AssistantRoleSetup(
    available: Boolean,
    held: Boolean,
    onRequestRole: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("ASSISTANT STATUS", style = MaterialTheme.typography.labelSmall)
            Text(
                when {
                    held -> "✓ Selected as default assistant"
                    available -> "⚠ Not selected as default assistant"
                    else -> "Assistant role is not available on this device"
                },
                color = if (held) Color(0xFF75D8B7) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (available && !held) {
                OutlinedButton(onClick = onRequestRole) { Text("Set as Default Assistant") }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.size(54.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(17.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("✦", color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp)
            }
        }
        Column {
            Text("SOLAPPAN  /  LOCAL AGENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("Android Agent Runtime", style = MaterialTheme.typography.headlineSmall)
            Text("TRACK 04 · PRODUCTIVITY + AUTOMATION", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(
        color = if (active) Color(0xFF17342F) else Color(0xFF3B2028),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = if (active) Color(0xFF75D8B7) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DemoPrompts(onSelect: (String) -> Unit) {
    val prompts = listOf(
        "Open Spotify.",
        "Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur.",
        "Find Afnan, prepare a message saying I will reach 20 minutes late, and open Maps to GEC Thrissur.",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        prompts.forEachIndexed { index, prompt ->
            AssistChip(
                onClick = { onSelect(prompt) },
                label = { Text("${index + 1}. $prompt") },
            )
        }
    }
}

private fun timelineColor(status: TimelineStatus): Color = when (status) {
    TimelineStatus.RUNNING -> Color(0xFFFF9B54)
    TimelineStatus.SUCCESS -> Color(0xFF75D8B7)
    TimelineStatus.FAILED, TimelineStatus.CANCELLED -> Color(0xFFFF716A)
}

@Composable
private fun StateCard(state: AgentState) {
    val color = when (state) {
        AgentState.COMPLETED -> Color(0xFF75D8B7)
        AgentState.FAILED, AgentState.CANCELLED -> MaterialTheme.colorScheme.error
        AgentState.WAITING_FOR_CONFIRMATION -> Color(0xFFFFB86B)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    color = color.copy(alpha = 0.13f),
                    shape = CircleShape,
                ) {}
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = color.copy(alpha = 0.28f),
                    shape = CircleShape,
                ) {}
                Surface(
                    modifier = Modifier.size(28.dp),
                    color = color,
                    shape = CircleShape,
                ) {}
            }
            Text("AGENT STATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stateLabel(state), color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ConfirmationCard(
    request: ToolConfirmation,
    actionsEnabled: Boolean,
    reviewed: Boolean,
    onReviewedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onApprove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (actionsEnabled) onCancel() },
        title = { Text("Approval required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(request.summary)
                Text("Risk: ${request.riskLevel.name}", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reviewed,
                        enabled = actionsEnabled,
                        onCheckedChange = onReviewedChange,
                    )
                    Text("I reviewed this action")
                }
            }
        },
        dismissButton = {
            OutlinedButton(enabled = actionsEnabled, onClick = onCancel) { Text("Cancel") }
        },
        confirmButton = {
            Button(enabled = actionsEnabled && reviewed, onClick = onApprove) { Text("Approve action") }
        },
    )
}

private fun stateLabel(state: AgentState): String = state.name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
