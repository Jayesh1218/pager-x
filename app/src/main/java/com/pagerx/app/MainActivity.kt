package com.pagerx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PagerXTheme {
                PagerXApp()
            }
        }
    }
}

enum class ConnectionType {
    REST,
    WEBSOCKET
}

data class TelemetryEvent(
    val id: Int,
    val service: String,
    val severity: String,
    val message: String,
    val timestamp: String,
    val latencyMs: Int
)

class PagerViewModel : ViewModel() {
    var connectionType by mutableStateOf(ConnectionType.REST)
        private set

    var endpoint by mutableStateOf("https://api.example.com/telemetry")
    var connected by mutableStateOf(false)
        private set

    var redactionEnabled by mutableStateOf(true)
    var autoRefresh by mutableStateOf(true)

    var events by mutableStateOf(initialEvents())
        private set

    fun setConnectionType(type: ConnectionType) {
        connectionType = type
        endpoint = if (type == ConnectionType.REST) {
            "https://api.example.com/telemetry"
        } else {
            "wss://stream.example.com/events"
        }
        connected = false
    }

    fun connect() {
        connected = true
        events = initialEvents()
    }

    fun disconnect() {
        connected = false
    }

    fun refresh() {
        events = listOf(
            TelemetryEvent(
                id = Random.nextInt(1000, 9999),
                service = listOf("payments", "auth", "checkout", "search").random(),
                severity = listOf("INFO", "WARN", "ERROR").random(),
                message = listOf(
                    "Request completed successfully",
                    "Elevated response latency detected",
                    "Retry budget is being consumed",
                    "Dependency health check completed"
                ).random(),
                timestamp = now(),
                latencyMs = Random.nextInt(80, 900)
            )
        ) + events.take(7)
    }

    fun summary(): String {
        val errors = events.count { it.severity == "ERROR" }
        val warnings = events.count { it.severity == "WARN" }
        val averageLatency = if (events.isEmpty()) {
            0
        } else {
            events.map { it.latencyMs }.average().toInt()
        }

        return when {
            errors > 0 ->
                "PagerX detected $errors error event(s). Investigate the affected service and check dependency health. Average latency is ${averageLatency} ms."
            warnings > 0 ->
                "System health is mostly stable, with $warnings warning event(s). Monitor latency and retry activity. Average latency is ${averageLatency} ms."
            else ->
                "No critical anomalies detected. Telemetry is stable and average latency is ${averageLatency} ms."
        }
    }

    fun displayMessage(event: TelemetryEvent): String {
        return if (redactionEnabled) {
            redact(event.message)
        } else {
            event.message
        }
    }

    private fun redact(value: String): String {
        return value
            .replace(Regex("""[\w.+-]+@[\w.-]+\.\w+"""), "[REDACTED_EMAIL]")
            .replace(Regex("""\b(?:\d[ -]*?){13,16}\b"""), "[REDACTED_NUMBER]")
            .replace(Regex("""Bearer\s+\S+""", RegexOption.IGNORE_CASE), "Bearer [REDACTED_TOKEN]")
    }

    companion object {
        private fun now(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }

        private fun initialEvents(): List<TelemetryEvent> {
            return listOf(
                TelemetryEvent(
                    1,
                    "checkout",
                    "ERROR",
                    "Payment dependency returned HTTP 503",
                    "10:42:18",
                    812
                ),
                TelemetryEvent(
                    2,
                    "auth",
                    "WARN",
                    "Elevated token refresh latency",
                    "10:41:54",
                    420
                ),
                TelemetryEvent(
                    3,
                    "search",
                    "INFO",
                    "Request completed successfully",
                    "10:41:32",
                    96
                ),
                TelemetryEvent(
                    4,
                    "payments",
                    "INFO",
                    "Dependency health check completed",
                    "10:40:11",
                    132
                )
            )
        }
    }
}

@Composable
fun PagerXApp(viewModel: PagerViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel.autoRefresh, viewModel.connected) {
        while (viewModel.autoRefresh && viewModel.connected) {
            delay(5000)
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PagerX",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telemetry command center",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    ConnectionBadge(connected = viewModel.connected)
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = "Connection")
                    },
                    label = { Text("Connection") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(Icons.Default.Lock, contentDescription = "Privacy")
                    },
                    label = { Text("Privacy") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> DashboardScreen(
                modifier = Modifier.padding(padding),
                viewModel = viewModel
            )

            1 -> ConnectionScreen(
                modifier = Modifier.padding(padding),
                viewModel = viewModel
            )

            else -> PrivacyScreen(
                modifier = Modifier.padding(padding),
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    viewModel: PagerViewModel
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live telemetry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitor operational signals in one place",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        item {
            SummaryCard(summary = viewModel.summary())
        }

        item {
            MetricsRow(events = viewModel.events)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (viewModel.connected) "Streaming" else "Mock data",
                    color = if (viewModel.connected) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        items(viewModel.events, key = { it.id }) { event ->
            EventCard(
                event = event,
                message = viewModel.displayMessage(event)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectionScreen(
    modifier: Modifier,
    viewModel: PagerViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connection",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Choose how PagerX receives telemetry. This MVP uses mock data and does not transmit credentials.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.connectionType == ConnectionType.REST,
                onClick = {
                    viewModel.setConnectionType(ConnectionType.REST)
                },
                label = { Text("REST") },
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                }
            )

            FilterChip(
                selected = viewModel.connectionType == ConnectionType.WEBSOCKET,
                onClick = {
                    viewModel.setConnectionType(ConnectionType.WEBSOCKET)
                },
                label = { Text("WebSocket") },
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                }
            )
        }

        OutlinedTextField(
            value = viewModel.endpoint,
            onValueChange = { viewModel.endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Endpoint") },
            singleLine = true
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Connection status",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (viewModel.connected) {
                        "Connected to mock ${viewModel.connectionType.name} telemetry"
                    } else {
                        "Disconnected"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.connected) {
                    OutlinedButton(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect")
                    }
                }
            }
        }

        Text(
            text = "Production integration note",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "A production version should add authenticated HTTP/WebSocket clients, certificate validation, retry policies, and secure credential storage.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyScreen(
    modifier: Modifier,
    viewModel: PagerViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Privacy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local redaction",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mask common emails, numbers, and bearer tokens before display.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = viewModel.redactionEnabled,
                        onCheckedChange = { viewModel.redactionEnabled = it }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic refresh",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Refresh mock telemetry every five seconds while connected.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = viewModel.autoRefresh,
                        onCheckedChange = { viewModel.autoRefresh = it }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Redaction happens locally on the device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AI-assisted summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = summary)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MVP summary generated from local telemetry state.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricsRow(events: List<TelemetryEvent>) {
    val errors = events.count { it.severity == "ERROR" }
    val warnings = events.count { it.severity == "WARN" }
    val averageLatency = if (events.isEmpty()) {
        0
    } else {
        events.map { it.latencyMs }.average().toInt()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            title = "Events",
            value = events.size.toString(),
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Alerts",
            value = (errors + warnings).toString(),
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Avg ms",
            value = averageLatency.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EventCard(
    event: TelemetryEvent,
    message: String
) {
    val severityColor = when (event.severity) {
        "ERROR" -> Color(0xFFC62828)
        "WARN" -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (event.severity == "INFO") {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = severityColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = event.service,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = event.severity,
                    color = severityColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${event.latencyMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionBadge(connected: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = if (connected) {
                    Color(0xFFE8F5E9)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (connected) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = if (connected) "Connected" else "Offline",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun PagerXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
