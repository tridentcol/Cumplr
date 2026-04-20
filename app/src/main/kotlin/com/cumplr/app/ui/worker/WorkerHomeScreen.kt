package com.cumplr.app.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.TaskCard
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant
import java.time.temporal.ChronoUnit

private val sampleTasks = listOf(
    Task(
        id = "1",
        companyId = "c1",
        title = "Revisar inventario de bodega A1–A5",
        description = "Contar y registrar todos los artículos en las estanterías del área A. Actualizar hoja de cálculo compartida.",
        assignedTo = "worker1",
        assignedBy = "chief1",
        status = TaskStatus.IN_PROGRESS,
        priority = TaskPriority.HIGH,
        deadline = Instant.now().plus(36, ChronoUnit.HOURS).toString(),
        startTime = null,
        endTime = null,
        photoStartUrl = null,
        photoEndUrl = null,
        observations = null,
        feedback = null,
        rejectionReason = null,
        createdAt = Instant.now().toString(),
        updatedAt = Instant.now().toString(),
    ),
    Task(
        id = "2",
        companyId = "c1",
        title = "Limpieza área de producción",
        description = "Limpiar y desinfectar equipos según protocolo estándar. Verificar existencia de insumos.",
        assignedTo = "worker1",
        assignedBy = "chief1",
        status = TaskStatus.ASSIGNED,
        priority = TaskPriority.MEDIUM,
        deadline = Instant.now().minus(2, ChronoUnit.HOURS).toString(),
        startTime = null,
        endTime = null,
        photoStartUrl = null,
        photoEndUrl = null,
        observations = null,
        feedback = null,
        rejectionReason = null,
        createdAt = Instant.now().toString(),
        updatedAt = Instant.now().toString(),
    ),
)

@Composable
fun WorkerHomeScreen(
    onLogout: () -> Unit,
    viewModel: WorkerHomeViewModel = hiltViewModel(),
) {
    val didLogOut by viewModel.didLogOut.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) {
        if (didLogOut) onLogout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            CumplrAppBar(
                title = "Mis Tareas",
                actions = {
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = CumplrFgMuted,
                        )
                    }
                },
            )

            LazyColumn(
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(sampleTasks) { task ->
                    TaskCard(
                        task = task,
                        onClick = {},
                        assignerName = "Jefe Carlos",
                    )
                }
            }
        }
    }
}
