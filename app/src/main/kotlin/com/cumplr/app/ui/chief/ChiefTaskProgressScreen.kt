package com.cumplr.app.ui.chief

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
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
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrChip
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChiefTaskProgressScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ChiefTaskProgressViewModel = hiltViewModel(),
) {
    val task   by viewModel.task.collectAsStateWithLifecycle()
    val worker by viewModel.worker.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = CumplrBackground) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, "Editar tarea", tint = CumplrAccent)
                }
            }

            val t = task
            if (t == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CumplrAccent)
                }
            } else {
                ProgressContent(task = t, worker = worker)
            }
        }
    }
}

@Composable
private fun ProgressContent(task: Task, worker: User?) {
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

        // ── Worker card ───────────────────────────────────────────────────────
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
                    Text(
                        text  = worker.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CumplrFg,
                    )
                    if (!worker.position.isNullOrBlank()) {
                        Text(
                            text  = worker.position.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = CumplrFgMuted,
                        )
                    }
                }
            }
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CumplrSurface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Person, null, tint = CumplrFgMuted, modifier = Modifier.size(16.dp))
                }
                Text("Sin trabajador asignado", style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
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

            if (task.status == TaskStatus.IN_PROGRESS && !task.startTime.isNullOrBlank()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Outlined.Timer, null, tint = CumplrStatusProgressFg, modifier = Modifier.size(15.dp))
                    Text(
                        text  = "En ejecución hace ${elapsedSince(task.startTime!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CumplrStatusProgressFg,
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
                Spacer(Modifier.height(2.dp))
                Text(task.description.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            }
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun deadlineColor(iso: String): Color = try {
    val hours = Duration.between(Instant.now(), Instant.parse(iso)).toHours()
    when {
        hours < 0  -> CumplrStatusOverdueFg
        hours < 24 -> CumplrStatusProgressFg
        else       -> CumplrStatusDoneFg
    }
} catch (_: Exception) { CumplrFgMuted }

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

private fun elapsedSince(iso: String): String = try {
    val mins = Duration.between(Instant.parse(iso), Instant.now()).toMinutes()
    when {
        mins < 60  -> "${mins}min"
        mins < 1440 -> "${mins / 60}h ${mins % 60}min"
        else       -> "${mins / 1440}d ${(mins % 1440) / 60}h"
    }
} catch (_: Exception) { "—" }

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
