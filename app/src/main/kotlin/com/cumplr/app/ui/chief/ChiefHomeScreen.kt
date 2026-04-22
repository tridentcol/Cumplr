package com.cumplr.app.ui.chief

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrBottomNav
import com.cumplr.core.ui.component.CumplrNavItem
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

private val CHIEF_TASK_TABS = listOf(
    ChiefTabDef("Por revisar")  { it.task.status == TaskStatus.SUBMITTED || it.task.status == TaskStatus.UNDER_REVIEW },
    ChiefTabDef("En ejecución") { it.task.status == TaskStatus.ASSIGNED  || it.task.status == TaskStatus.IN_PROGRESS },
    ChiefTabDef("Cumplidas")    { it.task.status == TaskStatus.APPROVED },
)

private val CHIEF_NAV_ITEMS = listOf(
    CumplrNavItem("inicio",   Icons.Outlined.Home,        "Inicio"),
    CumplrNavItem("equipo",   Icons.Outlined.Group,       "Equipo"),
    CumplrNavItem("tareas",   Icons.Outlined.CheckCircle, "Tareas"),
    CumplrNavItem("perfil",   Icons.Outlined.Person,      "Perfil"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiefHomeScreen(
    onLogout: () -> Unit,
    onTaskReview: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskSummary: (String) -> Unit,
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
    val pendingReviewCount by viewModel.pendingReviewCount.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedNav by remember { mutableStateOf("inicio") }

    val navItems = CHIEF_NAV_ITEMS.map { item ->
        if (item.key == "tareas" && pendingReviewCount > 0) item.copy(badgeCount = pendingReviewCount)
        else item
    }

    Scaffold(
        modifier       = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = CumplrBackground,
        bottomBar = {
            Surface(shadowElevation = 4.dp, color = CumplrSurface) {
                CumplrBottomNav(
                    items          = navItems,
                    selectedKey    = selectedNav,
                    onItemSelected = { selectedNav = it },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedNav) {
                "inicio" -> ChiefInicioTab(
                    chiefName          = chiefName,
                    activeTasksCount   = activeTasksCount,
                    activeWorkersCount = activeWorkersCount,
                    completionRate     = completionRate,
                    overdueCount       = overdueCount,
                    workers            = workers,
                    pendingReviewCount = pendingReviewCount,
                    onGoToTareas       = { selectedNav = "tareas" },
                )
                "equipo" -> ChiefEquipoTab(workers = workers)
                "tareas" -> ChiefTareasTab(
                    tasksWithWorkers = tasksWithWorkers,
                    isRefreshing     = isRefreshing,
                    onRefresh        = { viewModel.refresh() },
                    onTaskReview     = onTaskReview,
                    onTaskEdit       = onTaskEdit,
                    onTaskSummary    = onTaskSummary,
                    onAssignTask     = onAssignTask,
                )
                "perfil" -> ChiefPerfilTab(
                    name     = chiefName,
                    onLogout = { viewModel.signOut() },
                )
            }
        }
    }
}

// ── Inicio tab ────────────────────────────────────────────────────────────────

@Composable
private fun ChiefInicioTab(
    chiefName: String,
    activeTasksCount: Int,
    activeWorkersCount: Int,
    completionRate: Float,
    overdueCount: Int,
    workers: List<User>,
    pendingReviewCount: Int,
    onGoToTareas: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
    val firstName = chiefName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: chiefName

    val pct   = (completionRate * 100).toInt()
    val rateColor = when {
        completionRate >= 0.75f -> CumplrStatusDoneFg
        completionRate >= 0.5f  -> CumplrStatusProgressFg
        else                    -> CumplrStatusOverdueFg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Greeting
        if (chiefName.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(top = Spacing.lg, bottom = Spacing.md),
            ) {
                Text(salutation, style = MaterialTheme.typography.bodyLarge, color = CumplrFgMuted)
                Text(firstName,  style = MaterialTheme.typography.headlineMedium, color = CumplrFg)
                Spacer(Modifier.height(2.dp))
                Text("Operación de hoy", style = MaterialTheme.typography.bodyMedium, color = CumplrFgMuted)
            }
        }

        // 2×2 metric grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Tareas activas",
                    value    = "$activeTasksCount",
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Trabajadores",
                    value    = "$activeWorkersCount",
                    extra    = {
                        if (workers.isNotEmpty()) {
                            Spacer(Modifier.height(Spacing.xs))
                            WorkerAvatarStack(workers = workers.filter { it.active }.take(4))
                        }
                    },
                )
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Completion card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CumplrSurface)
                        .padding(Spacing.md),
                ) {
                    Column {
                        Text("Cumplimiento", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                        Spacer(Modifier.height(Spacing.xs))
                        Text("$pct%", style = MaterialTheme.typography.headlineMedium, color = rateColor)
                        Spacer(Modifier.height(Spacing.sm))
                        LinearProgressIndicator(
                            progress    = { completionRate.coerceIn(0f, 1f) },
                            modifier    = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color       = rateColor,
                            trackColor  = CumplrSurface2,
                        )
                    }
                }
                MetricCard(
                    modifier    = Modifier.weight(1f),
                    label       = "Vencidas",
                    value       = "$overdueCount",
                    valueColor  = if (overdueCount > 0) CumplrStatusOverdueFg else CumplrFg,
                )
            }
        }

        // Overdue banner
        if (overdueCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.sm)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrStatusOverdueBg)
                    .padding(Spacing.md),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(Icons.Outlined.Warning, null, tint = CumplrStatusOverdueFg, modifier = Modifier.size(18.dp))
                Text(
                    text  = "$overdueCount ${if (overdueCount == 1) "tarea vencida" else "tareas vencidas"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrStatusOverdueFg,
                )
            }
        }

        // Pending review banner
        if (pendingReviewCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.md)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrAccent.copy(alpha = 0.10f))
                    .clickable(onClick = onGoToTareas)
                    .padding(Spacing.md),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = CumplrAccent, modifier = Modifier.size(18.dp))
                Text(
                    text     = "$pendingReviewCount ${if (pendingReviewCount == 1) "tarea por revisar" else "tareas por revisar"}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = CumplrAccent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = CumplrFg,
    extra: (@Composable () -> Unit)? = null,
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
            extra?.invoke()
        }
    }
}

@Composable
private fun WorkerAvatarStack(workers: List<User>) {
    Row {
        workers.forEachIndexed { idx, worker ->
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
                    text  = worker.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = CumplrFgMuted,
                )
            }
        }
    }
}

// ── Equipo tab ────────────────────────────────────────────────────────────────

@Composable
private fun ChiefEquipoTab(workers: List<User>) {
    if (workers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon     = Icons.Outlined.Group,
                title    = "Sin trabajadores",
                subtitle = "No hay trabajadores registrados en tu equipo.",
            )
        }
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier            = Modifier.fillMaxSize(),
    ) {
        items(workers, key = { it.id }) { worker ->
            WorkerRow(worker = worker)
        }
    }
}

@Composable
private fun WorkerRow(worker: User) {
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
                .size(42.dp)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = worker.name,
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = CumplrFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(worker.position, worker.email).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    text     = sub,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = CumplrFgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!worker.active) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(CumplrSurface2)
                    .padding(horizontal = Spacing.sm, vertical = 3.dp),
            ) {
                Text("Inactivo", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CumplrStatusDoneFg),
            )
        }
    }
}

// ── Tareas tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChiefTareasTab(
    tasksWithWorkers: List<TaskWithWorker>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskReview: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskSummary: (String) -> Unit,
    onAssignTask: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val filtered = remember(tasksWithWorkers, selectedTab) {
        tasksWithWorkers.filter(CHIEF_TASK_TABS[selectedTab].filter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            CHIEF_TASK_TABS.forEachIndexed { index, tab ->
                val count = tasksWithWorkers.count(tab.filter)
                Tab(
                    selected               = selectedTab == index,
                    onClick                = { selectedTab = index },
                    selectedContentColor   = CumplrAccent,
                    unselectedContentColor = CumplrFgMuted,
                    text = {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
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

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh    = onRefresh,
                modifier     = Modifier.fillMaxSize(),
            ) {
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon     = Icons.Outlined.CheckCircle,
                            title    = "Sin tareas",
                            subtitle = "No hay tareas en esta categoría.",
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(
                            start  = Spacing.lg,
                            end    = Spacing.lg,
                            top    = Spacing.lg,
                            bottom = 88.dp,
                        ),
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
                                    when (tw.task.status) {
                                        TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW -> onTaskReview(tw.task.id)
                                        TaskStatus.APPROVED, TaskStatus.REJECTED       -> onTaskSummary(tw.task.id)
                                        else                                            -> onTaskEdit(tw.task.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick        = onAssignTask,
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.lg),
                containerColor = CumplrAccent,
                contentColor   = CumplrAccentInk,
                icon           = { Icon(Icons.Outlined.Add, "Asignar tarea") },
                text           = { Text("Asignar", style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

// ── Perfil tab ────────────────────────────────────────────────────────────────

@Composable
private fun ChiefPerfilTab(name: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CumplrAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = CumplrAccent,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Text(
            text  = name,
            style = MaterialTheme.typography.titleLarge,
            color = CumplrFg,
        )

        Spacer(Modifier.height(Spacing.xl))

        Button(
            onClick  = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor = CumplrSurface,
                contentColor   = CumplrStatusOverdueFg,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.ExitToApp,
                contentDescription = "Salir",
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
    }
}
