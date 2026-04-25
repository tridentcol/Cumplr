package com.cumplr.app.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrChip
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrStatusDoneBg
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskSummaryScreen(
    onBack: () -> Unit,
    viewModel: TaskSummaryViewModel = hiltViewModel(),
) {
    val task   by viewModel.task.collectAsStateWithLifecycle()
    val worker by viewModel.worker.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = CumplrBackground) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            CumplrAppBar(
                title = "Resumen de tarea",
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                    }
                },
            )
            val t = task
            if (t == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CumplrAccent)
                }
            } else {
                SummaryContent(task = t, worker = worker)
            }
        }
    }
}

@Composable
private fun SummaryContent(task: Task, worker: User?) {
    var fullscreenUrl by remember { mutableStateOf<String?>(null) }

    // Fullscreen photo viewer dialog
    fullscreenUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenUrl = null },
            properties       = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier         = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model              = ImageRequest.Builder(LocalContext.current)
                        .data(url).crossfade(true).build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick  = { fullscreenUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.lg),
                ) {
                    Icon(Icons.Outlined.Close, "Cerrar", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        // ── Hero ──────────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color    = CumplrFg,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                CumplrChip(status = task.status)
            }
            if (!task.description.isNullOrBlank()) {
                Text(task.description.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFgMuted)
            }
            if (!task.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, null, tint = CumplrFgSubtle, modifier = Modifier.size(14.dp))
                    Text(task.location.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CumplrFgSubtle)
                }
            }
            if (!task.deadline.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = CumplrFgSubtle, modifier = Modifier.size(13.dp))
                    Text("Deadline: ${task.deadline.orEmpty().take(10)}", style = MaterialTheme.typography.bodySmall, color = CumplrFgSubtle)
                }
            }
        }

        // ── Photos (4:3, fullscreen on tap) ───────────────────────────────────
        if (task.photoStartUrl != null || task.photoEndUrl != null) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PhotoCard("Foto inicio", task.photoStartUrl, Modifier.weight(1f)) {
                    task.photoStartUrl?.let { fullscreenUrl = it }
                }
                PhotoCard("Foto final",  task.photoEndUrl,   Modifier.weight(1f)) {
                    task.photoEndUrl?.let { fullscreenUrl = it }
                }
            }
        }

        // ── Status timeline ───────────────────────────────────────────────────
        StatusTimeline(task = task)

        // ── Worker ────────────────────────────────────────────────────────────
        if (worker != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface)
                    .padding(Spacing.md),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CumplrAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = worker.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = CumplrAccent,
                    )
                }
                Column {
                    Text(worker.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = CumplrFg)
                    if (!worker.position.isNullOrBlank()) {
                        Text(worker.position.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
                    }
                }
            }
        }

        // ── Observations ──────────────────────────────────────────────────────
        if (!task.observations.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Observaciones del trabajador", style = MaterialTheme.typography.labelSmall, color = CumplrAccent)
                Text(task.observations.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            }
        }

        // ── Feedback ──────────────────────────────────────────────────────────
        if (!task.feedback.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusDoneBg)
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment     = Alignment.Top,
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = CumplrStatusDoneFg, modifier = Modifier.size(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Feedback del jefe", style = MaterialTheme.typography.labelSmall, color = CumplrStatusDoneFg)
                    Text(task.feedback.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
                }
            }
        }

        // ── Rejection reason ──────────────────────────────────────────────────
        if (task.status == TaskStatus.REJECTED && !task.rejectionReason.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusOverdueBg)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Motivo del rechazo", style = MaterialTheme.typography.labelSmall, color = CumplrStatusOverdueFg)
                Text(task.rejectionReason.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            }
        }

        Spacer(Modifier.height(Spacing.md))
    }
}

// ── Photo card ────────────────────────────────────────────────────────────────

@Composable
private fun PhotoCard(label: String, url: String?, modifier: Modifier = Modifier, onTap: () -> Unit) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(CumplrSurface3)
                .then(if (url != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onTap,
                ) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model              = ImageRequest.Builder(LocalContext.current)
                        .data(url).crossfade(true).build(),
                    contentDescription = label,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Text("Sin foto", style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Status timeline ───────────────────────────────────────────────────────────

private data class TimelineNode(
    val label: String,
    val time: String?,
    val reached: Boolean,
    val nodeColor: Color,
    val subtitle: String? = null,
)

private fun buildTimelineNodes(task: Task): List<TimelineNode> {
    val wasRejected        = !task.rejectionReason.isNullOrBlank()
    val currentlyRejected  = task.status == TaskStatus.REJECTED
    val currentlyApproved  = task.status == TaskStatus.APPROVED
    val finalState         = currentlyRejected || currentlyApproved
    val reopenedNoResub    = wasRejected && task.status == TaskStatus.IN_PROGRESS && task.endTime == null
    val resubmitted        = wasRejected && !currentlyRejected && task.endTime != null

    return buildList {
        add(TimelineNode("Asignada",  formatDateTime(task.createdAt),  reached = true,                   nodeColor = CumplrFgMuted))
        add(TimelineNode("Iniciada",  formatDateTime(task.startTime),  reached = task.startTime != null, nodeColor = CumplrAccent))

        if (!wasRejected) {
            add(TimelineNode("Enviada", formatDateTime(task.endTime), reached = task.endTime != null, nodeColor = CumplrAccent))
            add(TimelineNode(
                label     = when (task.status) {
                    TaskStatus.APPROVED -> "Aprobada"
                    TaskStatus.REJECTED -> "Rechazada"
                    else                -> "Pendiente de revisión"
                },
                time      = if (finalState) formatDateTime(task.updatedAt) else null,
                reached   = finalState || task.status == TaskStatus.SUBMITTED || task.status == TaskStatus.UNDER_REVIEW,
                nodeColor = when (task.status) {
                    TaskStatus.APPROVED                          -> CumplrStatusDoneFg
                    TaskStatus.REJECTED                          -> CumplrStatusOverdueFg
                    TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW -> CumplrAccent
                    else                                         -> CumplrFgSubtle
                },
            ))
        } else {
            // First submission — timestamp only available if task is currently REJECTED
            // (reopenTask clears endTime, so after reopen we lose the first submission time)
            val firstSubmitTime = if (currentlyRejected) formatDateTime(task.endTime) else null
            add(TimelineNode("1er envío", firstSubmitTime, reached = true, nodeColor = CumplrAccent))

            // Rejection node
            add(TimelineNode(
                label    = "Rechazada",
                time     = if (currentlyRejected) formatDateTime(task.updatedAt) else null,
                reached  = true,
                nodeColor = CumplrStatusOverdueFg,
                subtitle  = task.rejectionReason,
            ))

            when {
                currentlyRejected -> Unit // rejection is the final node

                reopenedNoResub -> {
                    add(TimelineNode("Re-abierta", null, reached = true, nodeColor = CumplrAccent))
                    add(TimelineNode("Pendiente de re-envío", null, reached = false, nodeColor = CumplrFgSubtle))
                }

                resubmitted -> {
                    add(TimelineNode("Re-enviada", formatDateTime(task.endTime), reached = true, nodeColor = CumplrAccent))
                    add(TimelineNode(
                        label     = if (currentlyApproved) "Aprobada" else "Pendiente de revisión",
                        time      = if (currentlyApproved) formatDateTime(task.updatedAt) else null,
                        reached   = currentlyApproved || task.status == TaskStatus.SUBMITTED || task.status == TaskStatus.UNDER_REVIEW,
                        nodeColor = if (currentlyApproved) CumplrStatusDoneFg else CumplrAccent,
                    ))
                }
            }
        }
    }
}

@Composable
private fun StatusTimeline(task: Task) {
    val nodes = remember(task) { buildTimelineNodes(task) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        if (task.startTime != null && task.endTime != null) {
            Text("Duración total", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            Text(
                formatElapsed(task.startTime, task.endTime),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CumplrFg,
            )
            Spacer(Modifier.height(Spacing.md))
        }
        nodes.forEachIndexed { i, node ->
            TimelineRow(node = node, showConnector = i < nodes.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(node: TimelineNode, showConnector: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (node.reached) node.nodeColor else CumplrSurface3),
            )
            if (showConnector) {
                Box(modifier = Modifier.width(2.dp).height(28.dp).background(CumplrSurface3))
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) Spacing.xs else 0.dp),
        ) {
            Text(
                text  = node.label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (node.reached) CumplrFg else CumplrFgSubtle,
            )
            if (node.subtitle != null) {
                Text(
                    text  = "\"${node.subtitle}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = CumplrStatusOverdueFg,
                )
            }
            if (node.time != null) {
                Text(node.time, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatElapsed(startTime: String?, endTime: String?): String {
    if (startTime == null || endTime == null) return "—"
    return try {
        val secs = Duration.between(Instant.parse(startTime), Instant.parse(endTime)).seconds
        val h = secs / 3600; val m = (secs % 3600) / 60; val s = secs % 60
        when {
            h > 0 -> "%dh %02dm %02ds".format(h, m, s)
            m > 0 -> "%dm %02ds".format(m, s)
            else  -> "${s}s"
        }
    } catch (_: Exception) { "—" }
}

private fun formatDateTime(iso: String?): String? = try {
    iso?.let {
        Instant.parse(it).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault()))
    }
} catch (_: Exception) { null }
