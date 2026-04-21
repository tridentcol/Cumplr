package com.cumplr.app.ui.chief

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is CreateTaskUiState.Success) onSuccess()
    }

    var showDatePicker   by remember { mutableStateOf(false) }
    var showWorkerMenu   by remember { mutableStateOf(false) }
    val datePickerState  = rememberDatePickerState()
    val isSubmitting     = uiState is CreateTaskUiState.Submitting

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                }
                Text(
                    text  = "Asignar tarea",
                    style = MaterialTheme.typography.titleMedium,
                    color = CumplrFg,
                )
            }

            // ── Form ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Spacer(Modifier.height(Spacing.sm))

                FormLabel("Título *")
                OutlinedTextField(
                    value         = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Ej. Revisión de inventario", color = CumplrFgMuted) },
                    singleLine    = true,
                    colors        = fieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                )

                FormLabel("Descripción")
                OutlinedTextField(
                    value         = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    modifier      = Modifier.fillMaxWidth().height(120.dp),
                    placeholder   = { Text("Instrucciones adicionales...", color = CumplrFgMuted) },
                    maxLines      = 5,
                    colors        = fieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                )

                FormLabel("Asignar a *")
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CumplrSurface)
                            .clickable { showWorkerMenu = !showWorkerMenu }
                            .padding(horizontal = Spacing.md, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val selectedWorker = viewModel.selectedWorker
                        if (selectedWorker != null) {
                            Column {
                                Text(selectedWorker.name, style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
                                val pos = selectedWorker.position.orEmpty()
                                if (pos.isNotBlank()) {
                                    Text(pos, style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
                                }
                            }
                        } else {
                            Text("Seleccionar trabajador", color = CumplrFgMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Outlined.ArrowDropDown, "Expandir", tint = CumplrFgMuted)
                    }

                    DropdownMenu(
                        expanded         = showWorkerMenu,
                        onDismissRequest = { showWorkerMenu = false },
                        modifier         = Modifier.background(CumplrSurface2),
                    ) {
                        if (workers.isEmpty()) {
                            DropdownMenuItem(
                                text    = { Text("Sin trabajadores", color = CumplrFgMuted) },
                                onClick = { showWorkerMenu = false },
                            )
                        }
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(worker.name, style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
                                        val pos = worker.position.orEmpty()
                                        if (pos.isNotBlank()) {
                                            Text(pos, style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectedWorker = worker
                                    showWorkerMenu = false
                                },
                            )
                        }
                    }
                }

                FormLabel("Fecha límite")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CumplrSurface)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = Spacing.md, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val millis = viewModel.deadlineMillis
                    Text(
                        text  = if (millis != null) formatDate(millis) else "Sin fecha límite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (millis != null) CumplrFg else CumplrFgMuted,
                    )
                    Icon(Icons.Outlined.CalendarToday, "Fecha", tint = CumplrFgMuted, modifier = Modifier.size(18.dp))
                }

                if (uiState is CreateTaskUiState.Error) {
                    Text(
                        text  = (uiState as CreateTaskUiState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(Spacing.sm))
            }

            // ── Submit button ────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                enabled  = viewModel.isFormValid && !isSubmitting,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CumplrAccent,
                    contentColor   = CumplrAccentInk,
                ),
                shape    = RoundedCornerShape(10.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CumplrAccentInk, strokeWidth = 2.dp)
                } else {
                    Text("Asignar tarea", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deadlineMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Confirmar", color = CumplrAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = CumplrFgMuted)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = CumplrFgMuted)
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = CumplrFg,
    unfocusedTextColor      = CumplrFg,
    focusedContainerColor   = CumplrSurface,
    unfocusedContainerColor = CumplrSurface,
    focusedBorderColor      = CumplrAccent,
    unfocusedBorderColor    = CumplrBorder,
    cursorColor             = CumplrAccent,
)

private fun formatDate(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val zdt     = instant.atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()).format(zdt)
}
