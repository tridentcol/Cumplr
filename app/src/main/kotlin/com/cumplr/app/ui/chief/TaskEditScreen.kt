package com.cumplr.app.ui.chief

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is EditTaskUiState.Success) onSuccess()
    }

    var showDatePicker  by remember { mutableStateOf(false) }
    var showWorkerMenu  by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.deadlineMillis,
    )

    val showTimePicker: (onPick: (Int, Int) -> Unit) -> Unit = { onPick ->
        TimePickerDialog(
            context,
            { _, h, m -> onPick(h, m) },
            viewModel.deadlineHour,
            viewModel.deadlineMinute,
            true,
        ).show()
    }

    val isLoading    = uiState is EditTaskUiState.Loading
    val isSubmitting = uiState is EditTaskUiState.Submitting

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
                    text  = "Editar tarea",
                    style = MaterialTheme.typography.titleMedium,
                    color = CumplrFg,
                )
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = CumplrAccent,
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
                    placeholder   = { Text("Título de la tarea", color = CumplrFgMuted) },
                    singleLine    = true,
                    enabled       = !isLoading,
                    colors        = fieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                )

                FormLabel("Descripción")
                OutlinedTextField(
                    value         = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    modifier      = Modifier.fillMaxWidth().height(110.dp),
                    placeholder   = { Text("Instrucciones adicionales...", color = CumplrFgMuted) },
                    maxLines      = 5,
                    enabled       = !isLoading,
                    colors        = fieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                )

                FormLabel("Ubicación")
                OutlinedTextField(
                    value         = viewModel.location,
                    onValueChange = { viewModel.location = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Ej. Bodega principal", color = CumplrFgMuted) },
                    singleLine    = true,
                    enabled       = !isLoading,
                    leadingIcon   = {
                        Icon(Icons.Outlined.LocationOn, null, tint = CumplrFgMuted, modifier = Modifier.size(18.dp))
                    },
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
                            .clickable(enabled = !isLoading) { showWorkerMenu = !showWorkerMenu }
                            .padding(horizontal = Spacing.md, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
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

                FormLabel("Fecha y hora límite")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CumplrSurface)
                        .clickable(enabled = !isLoading) { showDatePicker = true }
                        .padding(horizontal = Spacing.md, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val millis = viewModel.deadlineMillis
                    Text(
                        text  = if (millis != null)
                            formatDeadlineDisplay(millis, viewModel.deadlineHour, viewModel.deadlineMinute)
                        else "Sin fecha límite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (millis != null) CumplrFg else CumplrFgMuted,
                    )
                    Icon(Icons.Outlined.CalendarToday, "Fecha", tint = CumplrFgMuted, modifier = Modifier.size(18.dp))
                }

                if (uiState is EditTaskUiState.Error) {
                    Text(
                        text  = (uiState as EditTaskUiState.Error).message,
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
                enabled  = viewModel.isFormValid && !isLoading && !isSubmitting,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CumplrAccent,
                    contentColor   = CumplrAccentInk,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CumplrAccentInk, strokeWidth = 2.dp)
                } else {
                    Text("Guardar cambios", style = MaterialTheme.typography.labelLarge)
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
                    val selectedMillis = datePickerState.selectedDateMillis
                    viewModel.deadlineMillis = selectedMillis
                    showDatePicker = false
                    if (selectedMillis != null) {
                        showTimePicker { h, m ->
                            viewModel.deadlineHour   = h
                            viewModel.deadlineMinute = m
                        }
                    }
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
