package com.cumplr.app.ui.worker

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrButtonVariant
import com.cumplr.core.ui.component.CumplrChip
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.Spacing
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val task         by viewModel.task.collectAsStateWithLifecycle()
    val assignerName by viewModel.assignerName.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = CumplrBackground) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                }
            }

            val t = task
            if (t == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CumplrAccent)
                }
            } else {
                TaskDetailContent(
                    task         = t,
                    assignerName = assignerName,
                    onStartTask  = { onNavigateToExecution(t.id) },
                )
            }
        }
    }
}

@Composable
private fun TaskDetailContent(task: Task, assignerName: String?, onStartTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // ── Hero ──────────────────────────────────────────────────────────────
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

        // ── Assigner row ──────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CumplrAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, null, tint = CumplrAccent, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(
                    text  = if (assignerName != null) "Asignada por $assignerName" else "Asignada por jefe",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = CumplrFg,
                )
                Text(
                    text  = timeAgo(task.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrFgMuted,
                )
            }
        }

        HorizontalDivider(color = CumplrSurface, thickness = 1.dp)

        // ── Metadata card ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CumplrSurface)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Priority
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(task.priority.dotColor()),
                )
                Text(
                    text  = task.priority.label(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = task.priority.dotColor(),
                )
            }

            // Deadline
            if (!task.deadline.isNullOrBlank()) {
                val dlColor = deadlineColor(task.deadline!!)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = dlColor, modifier = Modifier.size(15.dp))
                    Text(
                        text  = formatDeadlineFull(task.deadline!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = dlColor,
                    )
                }
            }

            // Location
            if (!task.location.isNullOrBlank()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Outlined.LocationOn, null, tint = CumplrFgMuted, modifier = Modifier.size(15.dp))
                    Text(
                        text  = task.location.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CumplrFgMuted,
                    )
                }
            }
        }

        // ── Description ───────────────────────────────────────────────────────
        if (!task.description.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Descripción", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                Text(task.description.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
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
                Text("Observaciones", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                Text(task.observations.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            }
        }

        // ── Feedback ──────────────────────────────────────────────────────────
        if (!task.feedback.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusDoneFg.copy(alpha = 0.10f))
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Feedback", style = MaterialTheme.typography.labelSmall, color = CumplrStatusDoneFg)
                Text(task.feedback.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            }
        }

        // ── Rejection reason ──────────────────────────────────────────────────
        if (!task.rejectionReason.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusOverdueBg)
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment     = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Warning, null, tint = CumplrStatusOverdueFg, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Motivo del rechazo", style = MaterialTheme.typography.labelSmall, color = CumplrStatusOverdueFg)
                    Text(task.rejectionReason.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // ── Action button ─────────────────────────────────────────────────────
        when (task.status) {
            TaskStatus.ASSIGNED -> CumplrButton(
                text     = "Iniciar tarea",
                onClick  = onStartTask,
                modifier = Modifier.fillMaxWidth(),
            )
            TaskStatus.IN_PROGRESS -> CumplrButton(
                text     = "Continuar tarea",
                onClick  = onStartTask,
                modifier = Modifier.fillMaxWidth(),
            )
            TaskStatus.REJECTED -> CumplrButton(
                text     = "Corregir y reenviar",
                onClick  = onStartTask,
                variant  = CumplrButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Unit
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun timeAgo(iso: String): String = try {
    val hours = Duration.between(Instant.parse(iso), Instant.now()).toHours()
    when {
        hours < 1  -> "Hace unos minutos"
        hours < 24 -> "Hace ${hours}h"
        hours < 48 -> "Hace 1 día"
        else       -> "Hace ${hours / 24} días"
    }
} catch (_: Exception) { "" }

private fun formatDeadlineFull(iso: String): String = try {
    val zdt   = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val today = LocalDate.now(ZoneId.systemDefault())
    val date  = zdt.toLocalDate()
    val time  = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    when {
        date.isBefore(today)      -> "Venció el ${zdt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))}"
        date == today             -> "Hoy, $time"
        date == today.plusDays(1) -> "Mañana, $time"
        else -> "${zdt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))}, $time"
    }
} catch (_: Exception) { iso.take(10) }

private fun deadlineColor(iso: String): Color = try {
    val hours = Duration.between(Instant.now(), Instant.parse(iso)).toHours()
    when {
        hours < 0  -> CumplrStatusOverdueFg
        hours < 24 -> CumplrStatusProgressFg
        else       -> CumplrStatusDoneFg
    }
} catch (_: Exception) { CumplrFgMuted }

private fun TaskPriority.dotColor(): Color = when (this) {
    TaskPriority.HIGH   -> CumplrStatusOverdueFg
    TaskPriority.MEDIUM -> CumplrStatusProgressFg
    TaskPriority.LOW    -> CumplrFgSubtle
}

private fun TaskPriority.label(): String = when (this) {
    TaskPriority.HIGH   -> "Alta prioridad"
    TaskPriority.MEDIUM -> "Prioridad media"
    TaskPriority.LOW    -> "Baja prioridad"
}
