package com.cumplr.app.ui.chief

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.component.TaskCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import java.time.LocalTime

private data class ChiefTabDef(val label: String, val filter: (TaskWithWorker) -> Boolean)

private val CHIEF_TABS = listOf(
    ChiefTabDef("Por revisar") { it.task.status == TaskStatus.SUBMITTED || it.task.status == TaskStatus.UNDER_REVIEW },
    ChiefTabDef("En ejecución") { it.task.status == TaskStatus.ASSIGNED || it.task.status == TaskStatus.IN_PROGRESS },
    ChiefTabDef("Cumplidas") { it.task.status == TaskStatus.APPROVED },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiefHomeScreen(
    onLogout: () -> Unit,
    onTaskReview: (String) -> Unit,
    onTeamClick: () -> Unit,
    onAssignTask: () -> Unit,
    viewModel: ChiefHomeViewModel = hiltViewModel(),
) {
    val didLogOut          by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isRefreshing       by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val tasksWithWorkers   by viewModel.tasksWithWorkers.collectAsStateWithLifecycle()
    val activeTasksCount   by viewModel.activeTasksCount.collectAsStateWithLifecycle()
    val completionRate     by viewModel.completionRate.collectAsStateWithLifecycle()
    val overdueCount       by viewModel.overdueCount.collectAsStateWithLifecycle()
    val activeWorkersCount by viewModel.activeWorkersCount.collectAsStateWithLifecycle()
    val chiefName          by viewModel.chiefName.collectAsStateWithLifecycle()
    val workers            by viewModel.workers.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val filtered = remember(tasksWithWorkers, selectedTab) {
        tasksWithWorkers.filter(CHIEF_TABS[selectedTab].filter)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

                CumplrAppBar(
                    title = "Panel del Equipo",
                    actions = {
                        IconButton(onClick = onTeamClick) {
                            Icon(Icons.Outlined.Group, "Equipo", tint = CumplrFgMuted)
                        }
                        IconButton(onClick = { viewModel.signOut() }) {
                            Icon(Icons.AutoMirrored.Outlined.ExitToApp, "Cerrar sesión", tint = CumplrFgMuted)
                        }
                    },
                )

                // ── Greeting ───────────────────────────────────────────────────
                ChiefGreeting(name = chiefName)

                // ── Dashboard ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Row 1: Activas | Trabajadores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        MetricCard(
                            modifier   = Modifier.weight(1f),
                            label      = "Tareas activas",
                            value      = "$activeTasksCount",
                        )
                        WorkersCard(
                            modifier       = Modifier.weight(1f),
                            workersCount   = activeWorkersCount,
                            workers        = workers,
                        )
                    }

                    // Row 2: Cumplimiento (full width)
                    CompletionCard(
                        modifier = Modifier.fillMaxWidth(),
                        rate     = completionRate,
                    )

                    // Row 3: Vencidas warning (only if > 0)
                    if (overdueCount > 0) {
                        OverdueWarning(count = overdueCount)
                    }
                }

                // ── Tabs ───────────────────────────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = CumplrBackground,
                    contentColor     = CumplrAccent,
                    edgePadding      = 0.dp,
                    indicator        = { tabPositions ->
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = CumplrAccent,
                        )
                    },
                ) {
                    CHIEF_TABS.forEachIndexed { index, tab ->
                        val count = tasksWithWorkers.count(tab.filter)
                        Tab(
                            selected               = selectedTab == index,
                            onClick                = { selectedTab = index },
                            selectedContentColor   = CumplrAccent,
                            unselectedContentColor = CumplrFgMuted,
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                ) {
                                    Text(tab.label, style = MaterialTheme.typography.labelSmall)
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(if (selectedTab == index) CumplrAccent else CumplrSurface2)
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text  = "$count",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selectedTab == index) CumplrAccentInk else CumplrFgMuted,
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                // ── Task list ──────────────────────────────────────────────────
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh    = { viewModel.refresh() },
                    modifier     = Modifier.weight(1f),
                ) {
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon     = Icons.Outlined.Group,
                                title    = "Sin tareas",
                                subtitle = "No hay tareas en esta categoría.",
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding      = PaddingValues(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier            = Modifier.fillMaxSize(),
                        ) {
                            items(filtered, key = { it.task.id }) { tw ->
                                val workerLabel = tw.worker?.let { w ->
                                    if (w.position != null) "${w.position} · ${w.name}" else w.name
                                }
                                TaskCard(
                                    task         = tw.task,
                                    assignerName = workerLabel,
                                    onClick      = {
                                        if (tw.task.status == TaskStatus.SUBMITTED ||
                                            tw.task.status == TaskStatus.UNDER_REVIEW
                                        ) onTaskReview(tw.task.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── FAB ───────────────────────────────────────────────────────────
            ExtendedFloatingActionButton(
                onClick           = onAssignTask,
                modifier          = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.lg),
                containerColor    = CumplrAccent,
                contentColor      = CumplrAccentInk,
                icon              = { Icon(Icons.Outlined.Add, "Asignar tarea") },
                text              = { Text("Asignar", style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun ChiefGreeting(name: String) {
    if (name.isBlank()) return
    val hour = LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
    val firstName = name.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: name
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Text(
            text  = "$salutation, $firstName.",
            style = MaterialTheme.typography.titleLarge,
            color = CumplrFg,
        )
        Text(
            text  = "Operación de hoy",
            style = MaterialTheme.typography.bodyMedium,
            color = CumplrFgMuted,
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = CumplrFg,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            Spacer(Modifier.height(Spacing.xs))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        }
    }
}

@Composable
private fun WorkersCard(
    modifier: Modifier = Modifier,
    workersCount: Int,
    workers: List<User>,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Column {
            Text("Trabajadores", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            Spacer(Modifier.height(Spacing.xs))
            Text("$workersCount", style = MaterialTheme.typography.headlineMedium, color = CumplrFg)
            if (workers.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                WorkerAvatarStack(workers = workers.filter { it.active }.take(4))
            }
        }
    }
}

@Composable
private fun WorkerAvatarStack(workers: List<User>) {
    Row {
        workers.forEachIndexed { idx, worker ->
            val initial = worker.name.firstOrNull()?.uppercase() ?: "?"
            Box(
                modifier = Modifier
                    .offset(x = (-idx * 10).dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(CumplrSurface3)
                    .border(1.5.dp, CumplrSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = CumplrFgMuted,
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(modifier: Modifier = Modifier, rate: Float) {
    val pct = (rate * 100).toInt()
    val color = when {
        rate >= 0.75f -> CumplrStatusDoneFg
        rate >= 0.5f  -> CumplrStatusProgressFg
        else          -> CumplrStatusOverdueFg
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cumplimiento", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                Text("$pct%", style = MaterialTheme.typography.titleLarge, color = color)
            }
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress    = { rate.coerceIn(0f, 1f) },
                modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color       = color,
                trackColor  = CumplrSurface2,
            )
        }
    }
}

@Composable
private fun OverdueWarning(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrStatusOverdueBg)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(Icons.Outlined.Warning, "Vencidas", tint = CumplrStatusOverdueFg, modifier = Modifier.size(18.dp))
        Text(
            text  = "$count ${if (count == 1) "tarea vencida" else "tareas vencidas"}",
            style = MaterialTheme.typography.bodySmall,
            color = CumplrStatusOverdueFg,
        )
    }
}
