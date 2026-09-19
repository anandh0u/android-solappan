package com.solappan.agent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.solappan.agent.tools.ToolConfirmation
import com.solappan.agent.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { AgentScreen() }
            }
        }
    }
}

private enum class TimelineStatus { RUNNING, SUCCESS, FAILED, CANCELLED }

private data class TimelineEntry(
    val toolName: String,
    val message: String,
    val status: TimelineStatus,
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
    var goal by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var agentState by remember { mutableStateOf(AgentState.IDLE) }
    var timeline by remember { mutableStateOf<List<TimelineEntry>>(emptyList()) }
    var pendingApproval by remember { mutableStateOf<PendingApproval?>(null) }
    var contactsGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val contactPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsGranted = it
    }
    val scope = rememberCoroutineScope()

    fun reset() {
        goal = ""
        response = ""
        error = null
        timeline = emptyList()
        agentState = AgentState.IDLE
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Solappan Agent", style = MaterialTheme.typography.headlineMedium)
        Text("Turn a goal into safe Android actions.", style = MaterialTheme.typography.bodyLarge)

        StateCard(agentState)

        pendingApproval?.let { pending ->
            ConfirmationCard(
                request = pending.request,
                onCancel = {
                    if (pending.decision.complete(false)) pendingApproval = null
                },
                onApprove = {
                    if (pending.decision.complete(true)) pendingApproval = null
                },
            )
        }

        if (!contactsGranted) {
            Button(onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                Text("Enable contact tools")
            }
        }

        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What do you want to accomplish?") },
            minLines = 3,
            enabled = !loading,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !loading && goal.isNotBlank(),
                onClick = {
                    loading = true
                    response = ""
                    error = null
                    timeline = emptyList()
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            controller.run(
                                goal = goal.trim(),
                                onEvent = { event ->
                                    withContext(Dispatchers.Main) {
                                        when (event) {
                                            is AgentEvent.StateChanged -> agentState = event.state
                                            is AgentEvent.ToolStarted -> timeline = timeline + TimelineEntry(
                                                event.toolName,
                                                "Starting",
                                                TimelineStatus.RUNNING,
                                            )
                                            is AgentEvent.ToolFinished -> {
                                                val index = timeline.indexOfLast {
                                                    it.toolName == event.toolName && it.status == TimelineStatus.RUNNING
                                                }
                                                val status = when {
                                                    event.errorCode == "USER_CANCELLED" -> TimelineStatus.CANCELLED
                                                    event.success -> TimelineStatus.SUCCESS
                                                    else -> TimelineStatus.FAILED
                                                }
                                                if (index >= 0) {
                                                    timeline = timeline.toMutableList().also {
                                                        it[index] = TimelineEntry(event.toolName, event.message, status)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                requestConfirmation = { request ->
                                    val decision = CompletableDeferred<Boolean>()
                                    withContext(Dispatchers.Main) {
                                        pendingApproval = PendingApproval(request, decision)
                                    }
                                    decision.await().also {
                                        withContext(Dispatchers.Main) { pendingApproval = null }
                                    }
                                },
                            )
                        }
                        result.fold(
                            onSuccess = { response = it.finalText },
                            onFailure = { error = it.message ?: "The request failed. Please try again." },
                        )
                        loading = false
                    }
                },
            ) { Text("Run goal") }

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
                Text("$marker ${entry.toolName}: ${entry.message}")
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
private fun StateCard(state: AgentState) {
    val color = when (state) {
        AgentState.COMPLETED -> Color(0xFF137333)
        AgentState.FAILED, AgentState.CANCELLED -> MaterialTheme.colorScheme.error
        AgentState.WAITING_FOR_CONFIRMATION -> Color(0xFF9A6700)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Agent state", style = MaterialTheme.typography.labelMedium)
            Text(stateLabel(state), color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ConfirmationCard(
    request: ToolConfirmation,
    onCancel: () -> Unit,
    onApprove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Approval required", style = MaterialTheme.typography.titleMedium)
            Text(request.summary)
            Text("Risk: ${request.riskLevel.name}", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}

private fun stateLabel(state: AgentState): String = state.name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
