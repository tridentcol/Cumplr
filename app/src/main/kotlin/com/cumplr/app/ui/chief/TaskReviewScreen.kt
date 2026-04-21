package com.cumplr.app.ui.chief

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneBg
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReviewScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskReviewViewModel = hiltViewModel(),
) {
    val task    by viewModel.task.collectAsStateWithLifecycle()
    val worker  by viewModel.worker.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ReviewUiState.Success) onSuccess()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRejectSheet by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val t = task
        if (t == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CumplrAccent)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                CumplrAppBar(
                    title = t.title,
                    leadingIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                        }
                    },
                )
                ReviewContent(
                    task           = t,
                    worker         = worker,
                    uiState        = uiState,
                    onApprove      = { feedback -> viewModel.approve(feedback) },
                    onRejectClick  = { showRejectSheet = true },
                )
            }
        }
    }

    if (showRejectSheet) {
        RejectBottomSheet(
            sheetState = sheetState,
            onDismiss  = { showRejectSheet = false },
            onConfirm  = { reason ->
                showRejectSheet = false
                viewModel.reject(reason)
            },
        )
    }
}

@Composable
private fun ReviewContent(
    task: Task,
    worker: User?,
    uiState: ReviewUiState,
    onApprove: (String) -> Unit,
    onRejectClick: () -> Unit,
) {
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Worker header
        if (worker != null) {
            CumplrCard {
                Text(
                    text  = worker.position ?: "Sin cargo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFg,
                )
                Text(
                    text  = worker.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrFgMuted,
                )
            }
        }

        // Photos side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PhotoCard(label = "Foto inicio", url = task.photoStartUrl, modifier = Modifier.weight(1f))
            PhotoCard(label = "Foto final",  url = task.photoEndUrl,   modifier = Modifier.weight(1f))
        }

        // Time info
        CumplrCard {
            val elapsed = formatElapsed(task.startTime, task.endTime)
            LabeledRow("Tiempo empleado", elapsed)
            if (!task.deadline.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                LabeledRow("Deadline", task.deadline.orEmpty().take(10))
            }
        }

        // Worker observations
        if (!task.observations.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface)
                    .padding(Spacing.md),
            ) {
                Text(
                    text  = "Observaciones del trabajador",
                    style = MaterialTheme.typography.labelSmall,
                    color = CumplrAccent,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text  = task.observations.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFg,
                )
            }
        }

        // Chief feedback
        OutlinedTextField(
            value         = feedback,
            onValueChange = { if (it.length <= 500) feedback = it },
            label         = { Text("Feedback (opcional)") },
            placeholder   = { Text("Escribe tu retroalimentación...") },
            minLines      = 3,
            maxLines      = 6,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = CumplrAccent,
                unfocusedBorderColor    = CumplrBorder,
                focusedLabelColor       = CumplrAccent,
                unfocusedLabelColor     = CumplrFgMuted,
                cursorColor             = CumplrAccent,
                focusedTextColor        = CumplrFg,
                unfocusedTextColor      = CumplrFg,
                focusedContainerColor   = CumplrSurface,
                unfocusedContainerColor = CumplrSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text  = "${feedback.length}/500",
            style = MaterialTheme.typography.bodySmall,
            color = CumplrFgMuted,
        )

        // Error message
        val err = (uiState as? ReviewUiState.Error)?.message
        if (err != null) {
            Text(text = err, style = MaterialTheme.typography.bodySmall, color = CumplrStatusOverdueFg)
        }

        val isSubmitting = uiState is ReviewUiState.Submitting
        if (isSubmitting) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CumplrAccent, modifier = Modifier.size(32.dp))
            }
        } else {
            // Approve button
            Button(
                onClick  = { onApprove(feedback) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CumplrStatusDoneBg,
                    contentColor   = CumplrStatusDoneFg,
                ),
            ) {
                Text("Aprobar", style = MaterialTheme.typography.labelSmall)
            }

            // Reject button
            OutlinedButton(
                onClick  = onRejectClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, CumplrStatusOverdueFg),
            ) {
                Text("Rechazar", style = MaterialTheme.typography.labelSmall, color = CumplrStatusOverdueFg)
            }
        }
    }
}

@Composable
private fun PhotoCard(label: String, url: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CumplrSurface3),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
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

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = CumplrFg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CumplrSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text  = "Motivo del rechazo",
                style = MaterialTheme.typography.headlineSmall,
                color = CumplrFg,
            )
            OutlinedTextField(
                value         = reason,
                onValueChange = { if (it.length <= 300) reason = it },
                placeholder   = { Text("Describe el motivo...") },
                minLines      = 3,
                maxLines      = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = CumplrStatusOverdueFg,
                    unfocusedBorderColor    = CumplrBorder,
                    focusedLabelColor       = CumplrStatusOverdueFg,
                    cursorColor             = CumplrStatusOverdueFg,
                    focusedTextColor        = CumplrFg,
                    unfocusedTextColor      = CumplrFg,
                    focusedContainerColor   = CumplrSurface,
                    unfocusedContainerColor = CumplrSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            CumplrButton(
                text     = "Confirmar rechazo",
                onClick  = { if (reason.isNotBlank()) onConfirm(reason) },
                enabled  = reason.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatElapsed(startTime: String?, endTime: String?): String {
    if (startTime == null || endTime == null) return "—"
    return try {
        val secs = java.time.Duration.between(Instant.parse(startTime), Instant.parse(endTime)).seconds
        val h = secs / 3600; val m = (secs % 3600) / 60; val s = secs % 60
        "%02d:%02d:%02d".format(h, m, s)
    } catch (_: Exception) { "—" }
}
