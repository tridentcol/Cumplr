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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrButtonVariant
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.component.CumplrChip
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.Spacing

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val task by viewModel.task.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            CumplrAppBar(
                title = "Detalle de tarea",
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = CumplrFgMuted,
                        )
                    }
                },
            )

            val t = task
            if (t == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CumplrAccent)
                }
            } else {
                TaskDetailContent(
                    task = t,
                    onStartTask = { onNavigateToExecution(t.id) },
                )
            }
        }
    }
}

@Composable
private fun TaskDetailContent(task: Task, onStartTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CumplrCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = CumplrFg,
                    modifier = Modifier.weight(1f),
                )
                CumplrChip(status = task.status)
            }

            if (!task.description.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text  = task.description.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CumplrFgMuted,
                )
            }
        }

        CumplrCard {
            DetailRow(label = "Prioridad", value = task.priority.name.lowercase().replaceFirstChar { it.uppercase() })
            if (!task.deadline.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                DetailRow(label = "Vence", value = task.deadline.orEmpty().take(10))
            }
        }

        if (!task.observations.isNullOrBlank()) {
            CumplrCard {
                DetailRow(label = "Observaciones", value = task.observations.orEmpty())
            }
        }

        if (!task.feedback.isNullOrBlank()) {
            CumplrCard {
                DetailRow(label = "Retroalimentación", value = task.feedback.orEmpty())
            }
        }

        if (!task.rejectionReason.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CumplrStatusOverdueBg, RoundedCornerShape(12.dp))
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text  = "Motivo del rechazo",
                    style = MaterialTheme.typography.labelSmall,
                    color = CumplrStatusOverdueFg,
                )
                Text(
                    text  = "\"${task.rejectionReason.orEmpty()}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFg,
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        if (task.status == TaskStatus.ASSIGNED) {
            CumplrButton(
                text     = "Iniciar tarea",
                onClick  = onStartTask,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (task.status == TaskStatus.REJECTED) {
            CumplrButton(
                text     = "Reintentar tarea",
                onClick  = onStartTask,
                variant  = CumplrButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = CumplrFg)
    }
}
