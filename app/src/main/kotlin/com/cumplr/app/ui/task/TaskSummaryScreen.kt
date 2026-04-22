package com.cumplr.app.ui.task

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.component.CumplrChip
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // ── Header: title + status ────────────────────────────────────────────
        CumplrCard {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color    = CumplrFg,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                CumplrChip(status = task.status)
            }

            if (!task.description.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text  = task.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFgMuted,
                )
            }

            if (!task.location.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.LocationOn, null, tint = CumplrFgMuted, modifier = Modifier.size(14.dp))
                    Text(task.location.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
                }
            }
        }

        // ── Worker ────────────────────────────────────────────────────────────
        if (worker != null) {
            CumplrCard {
                Row(
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
                            Text(worker.position.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
                        }
                    }
                }
            }
        }

        // ── Timeline ──────────────────────────────────────────────────────────
        CumplrCard {
            val elapsed = formatElapsed(task.startTime, task.endTime)
            val startFmt = formatDateTime(task.startTime)
            val endFmt   = formatDateTime(task.endTime)

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(Icons.Outlined.AccessTime, null, tint = CumplrAccent, modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Duración", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                    Text(elapsed, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CumplrFg)
                }
            }

            if (startFmt != null || endFmt != null) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (startFmt != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inicio", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                            Text(startFmt, style = MaterialTheme.typography.bodySmall, color = CumplrFg)
                        }
                    }
                    if (endFmt != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Finalización", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                            Text(endFmt, style = MaterialTheme.typography.bodySmall, color = CumplrFg)
                        }
                    }
                }
            }

            if (!task.deadline.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = CumplrFgMuted, modifier = Modifier.size(13.dp))
                    Text(
                        text  = "Deadline: ${task.deadline.orEmpty().take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CumplrFgMuted,
                    )
                }
            }
        }

        // ── Photos ────────────────────────────────────────────────────────────
        if (task.photoStartUrl != null || task.photoEndUrl != null) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SummaryPhotoCard("Foto inicio", task.photoStartUrl, Modifier.weight(1f))
                SummaryPhotoCard("Foto final",  task.photoEndUrl,   Modifier.weight(1f))
            }
        }

        // ── Worker observations ───────────────────────────────────────────────
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

        // ── Chief feedback ────────────────────────────────────────────────────
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusOverdueBg)
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment     = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Motivo del rechazo", style = MaterialTheme.typography.labelSmall, color = CumplrStatusOverdueFg)
                    Text(task.rejectionReason.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun SummaryPhotoCard(label: String, url: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(CumplrSurface3),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
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
