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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch
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
import com.cumplr.core.ui.component.OfflineBanner
import com.cumplr.core.ui.component.TaskCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneBg
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrStatusSubmittedFg
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

private enum class DateFilter { ALL, TODAY, WEEK, MONTH }

private fun com.cumplr.core.domain.model.Task.matchesDateFilter(filter: DateFilter): Boolean {
    if (filter == DateFilter.ALL) return true
    val d = deadline?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        ?: return false
    val today = LocalDate.now()
    return when (filter) {
        DateFilter.TODAY -> d == today
        DateFilter.WEEK  -> !d.isBefore(today.with(DayOfWeek.MONDAY)) && !d.isAfter(today.with(DayOfWeek.SUNDAY))
        DateFilter.MONTH -> d.month == today.month && d.year == today.year
        DateFilter.ALL   -> true
    }
}

private data class ChiefTabDef(val label: String, val filter: (TaskWithWorker) -> Boolean)

private val CHIEF_TASK_TABS = listOf(
    ChiefTabDef("Por revisar")  { it.task.status == TaskStatus.SUBMITTED || it.task.status == TaskStatus.UNDER_REVIEW },
    ChiefTabDef("En ejecución") { it.task.status == TaskStatus.ASSIGNED  || it.task.status == TaskStatus.IN_PROGRESS },
    ChiefTabDef("Finalizadas")  { it.task.status == TaskStatus.APPROVED  || it.task.status == TaskStatus.REJECTED },
)

private val CHIEF_NAV_ITEMS = listOf(
    CumplrNavItem("inicio",     Icons.Outlined.Home,        "Inicio"),
    CumplrNavItem("equipo",     Icons.Outlined.Group,       "Equipo"),
    CumplrNavItem("tareas",     Icons.Outlined.CheckCircle, "Tareas"),
    CumplrNavItem("dashboard",  Icons.Outlined.BarChart,    "Stats"),
    CumplrNavItem("perfil",     Icons.Outlined.Person,      "Perfil"),
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
    val pendingReviewCount    by viewModel.pendingReviewCount.collectAsStateWithLifecycle()
    val approvedThisMonth     by viewModel.approvedThisMonthCount.collectAsStateWithLifecycle()
    val topWorkers            by viewModel.topWorkers.collectAsStateWithLifecycle()
    val selectedTaskIds       by viewModel.selectedTaskIds.collectAsStateWithLifecycle()
    val isSelectionMode       by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val errorMessage          by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
            viewModel.clearError()
        }
    }

    var selectedNav by remember { mutableStateOf("inicio") }

    val navItems = CHIEF_NAV_ITEMS.map { item ->
        if (item.key == "tareas" && pendingReviewCount > 0) item.copy(badgeCount = pendingReviewCount)
        else item
    }

    Scaffold(
        modifier             = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor       = CumplrBackground,
        // CumplrBottomNav handles navigationBars insets internally via windowInsetsBottomHeight,
        // so we clear them here to prevent Scaffold from adding a duplicate bottom offset.
        contentWindowInsets  = androidx.compose.foundation.layout.WindowInsets(0),
        snackbarHost         = { SnackbarHost(snackbarHostState) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OfflineBanner()
        Box(modifier = Modifier.weight(1f)) {
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
                "equipo" -> ChiefEquipoTab(workers = workers, tasksWithWorkers = tasksWithWorkers)
                "tareas" -> ChiefTareasTab(
                    tasksWithWorkers  = tasksWithWorkers,
                    workers           = workers,
                    isRefreshing      = isRefreshing,
                    onRefresh         = { viewModel.refresh() },
                    onTaskReview      = onTaskReview,
                    onTaskEdit        = onTaskEdit,
                    onTaskSummary     = onTaskSummary,
                    onAssignTask      = onAssignTask,
                    onDeleteTask      = { viewModel.deleteTask(it) },
                    onReassignTask    = { taskId, workerId -> viewModel.reassignTask(taskId, workerId) },
                    selectedTaskIds   = selectedTaskIds,
                    isSelectionMode   = isSelectionMode,
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onClearSelection  = { viewModel.clearSelection() },
                    onBulkApprove     = { viewModel.bulkApprove(it) },
                    onBulkReject      = { ids, reason -> viewModel.bulkReject(ids, reason) },
                    onBulkDelete      = { viewModel.bulkDelete(it) },
                )
                "dashboard" -> ChiefDashboardTab(
                    activeTasksCount  = activeTasksCount,
                    overdueCount      = overdueCount,
                    pendingReview     = pendingReviewCount,
                    approvedThisMonth = approvedThisMonth,
                    topWorkers        = topWorkers,
                    tasksWithWorkers  = tasksWithWorkers,
                )
                "perfil" -> ChiefPerfilTab(
                    name     = chiefName,
                    onLogout = { viewModel.signOut() },
                )
            }
        }
        } // close Column
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
private fun ChiefEquipoTab(workers: List<User>, tasksWithWorkers: List<TaskWithWorker>) {
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
            val workerTasks = tasksWithWorkers.filter { it.task.assignedTo == worker.id }
            val approved    = workerTasks.count { it.task.status == TaskStatus.APPROVED }
            val finished    = workerTasks.count { it.task.status == TaskStatus.APPROVED || it.task.status == TaskStatus.REJECTED }
            val rate        = if (finished > 0) approved.toFloat() / finished else null
            WorkerRow(worker = worker, approvalRate = rate, taskCount = workerTasks.size)
        }
    }
}

@Composable
private fun WorkerRow(worker: User, approvalRate: Float?, taskCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
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
                Box(Modifier.size(8.dp).clip(CircleShape).background(CumplrStatusDoneFg))
            }
        }

        // Completion score row
        if (taskCount > 0) {
            val rateColor = when {
                approvalRate == null        -> CumplrFgMuted
                approvalRate >= 0.8f        -> CumplrStatusDoneFg
                approvalRate >= 0.5f        -> CumplrStatusProgressFg
                else                        -> CumplrStatusOverdueFg
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                LinearProgressIndicator(
                    progress   = { (approvalRate ?: 0f).coerceIn(0f, 1f) },
                    modifier   = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color      = rateColor,
                    trackColor = CumplrSurface2,
                )
                Text(
                    text  = if (approvalRate != null) "${(approvalRate * 100).toInt()}% · $taskCount tareas" else "$taskCount tareas",
                    style = MaterialTheme.typography.labelSmall,
                    color = rateColor,
                )
            }
        }
    }
}

// ── Tareas tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ChiefTareasTab(
    tasksWithWorkers: List<TaskWithWorker>,
    workers: List<User>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskReview: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskSummary: (String) -> Unit,
    onAssignTask: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onReassignTask: (String, String) -> Unit,
    selectedTaskIds: Set<String>,
    isSelectionMode: Boolean,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onBulkApprove: (Set<String>) -> Unit,
    onBulkReject: (Set<String>, String) -> Unit,
    onBulkDelete: (Set<String>) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var dateFilter  by remember { mutableStateOf(DateFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(tasksWithWorkers, selectedTab, dateFilter, searchQuery) {
        tasksWithWorkers
            .filter(CHIEF_TASK_TABS[selectedTab].filter)
            .filter { tw -> tw.task.matchesDateFilter(dateFilter) }
            .filter { tw ->
                searchQuery.isBlank() ||
                tw.task.title.contains(searchQuery, ignoreCase = true) ||
                tw.task.location?.contains(searchQuery, ignoreCase = true) == true ||
                tw.workerName?.contains(searchQuery, ignoreCase = true) == true
            }
    }

    var deleteTargetId  by remember { mutableStateOf<String?>(null) }
    var reassignTargetId by remember { mutableStateOf<String?>(null) }

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

        // Search bar
        TextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder   = { Text("Buscar por tarea, lugar o worker…", style = MaterialTheme.typography.bodySmall, color = CumplrFgSubtle) },
            leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = CumplrFgMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon  = if (searchQuery.isNotEmpty()) {{ IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Outlined.Close, null, tint = CumplrFgMuted, modifier = Modifier.size(16.dp))
            }}} else null,
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodySmall.copy(color = CumplrFg),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = CumplrSurface2,
                unfocusedContainerColor = CumplrSurface,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = CumplrAccent,
            ),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )

        // Date filter chips
        Row(
            modifier              = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            listOf(DateFilter.ALL to "Todos", DateFilter.TODAY to "Hoy",
                   DateFilter.WEEK to "Semana", DateFilter.MONTH to "Mes").forEach { (f, label) ->
                FilterChip(
                    selected = dateFilter == f,
                    onClick  = { dateFilter = f },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
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
                            val isSelected = tw.task.id in selectedTaskIds
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) onToggleSelection(tw.task.id)
                                        else when (tw.task.status) {
                                            TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW -> onTaskReview(tw.task.id)
                                            TaskStatus.APPROVED, TaskStatus.REJECTED       -> onTaskSummary(tw.task.id)
                                            else                                            -> onTaskEdit(tw.task.id)
                                        }
                                    },
                                    onLongClick = { onToggleSelection(tw.task.id) },
                                ),
                            ) {
                                TaskCard(
                                    task         = tw.task,
                                    assignerName = workerLabel,
                                    onClick      = {},
                                    modifier     = if (isSelected) Modifier.border(2.dp, CumplrAccent, RoundedCornerShape(12.dp)) else Modifier,
                                    onMenuClick  = if (isSelectionMode) null else { menuExpanded = true },
                                )
                                if (!isSelectionMode) {
                                    DropdownMenu(
                                        expanded         = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text        = { Text("Eliminar") },
                                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                            onClick     = {
                                                menuExpanded = false
                                                deleteTargetId = tw.task.id
                                            },
                                        )
                                        if (tw.task.status == TaskStatus.ASSIGNED) {
                                            DropdownMenuItem(
                                                text        = { Text("Reasignar") },
                                                leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) },
                                                onClick     = {
                                                    menuExpanded = false
                                                    reassignTargetId = tw.task.id
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSelectionMode) {
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

            // Bulk action bar
            var showBulkRejectDialog by remember { mutableStateOf(false) }
            AnimatedVisibility(
                visible  = isSelectionMode,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter    = slideInVertically { it },
                exit     = slideOutVertically { it },
            ) {
                androidx.compose.material3.Surface(
                    shadowElevation = 8.dp,
                    color           = CumplrSurface,
                    shape           = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            "${selectedTaskIds.size} seleccionada(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = CumplrFg,
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Button(
                                onClick  = { onBulkApprove(selectedTaskIds) },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = CumplrStatusDoneBg, contentColor = CumplrStatusDoneFg),
                                shape    = RoundedCornerShape(10.dp),
                            ) { Text("Aprobar", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick  = { showBulkRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, CumplrStatusOverdueFg),
                            ) { Text("Rechazar", style = MaterialTheme.typography.labelSmall, color = CumplrStatusOverdueFg) }
                            IconButton(onClick = { onBulkDelete(selectedTaskIds) }) {
                                Icon(Icons.Outlined.Delete, null, tint = CumplrStatusOverdueFg)
                            }
                        }
                        TextButton(onClick = onClearSelection, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar selección", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                        }
                    }
                }
            }

            if (showBulkRejectDialog) {
                var bulkRejectReason by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showBulkRejectDialog = false },
                    title   = { Text("Motivo de rechazo") },
                    text    = {
                        androidx.compose.material3.OutlinedTextField(
                            value         = bulkRejectReason,
                            onValueChange = { bulkRejectReason = it },
                            placeholder   = { Text("Describe el motivo…") },
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (bulkRejectReason.isNotBlank()) {
                                onBulkReject(selectedTaskIds, bulkRejectReason)
                                showBulkRejectDialog = false
                                bulkRejectReason = ""
                            }
                        }) { Text("Rechazar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBulkRejectDialog = false }) { Text("Cancelar") }
                    },
                )
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title            = { Text("Eliminar tarea") },
            text             = { Text("Esta acción no se puede deshacer. La tarea se eliminará también para el trabajador.") },
            confirmButton    = {
                TextButton(
                    onClick = {
                        onDeleteTask(deleteTargetId!!)
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("Cancelar") }
            },
        )
    }

    // Reassign worker picker dialog
    if (reassignTargetId != null) {
        AlertDialog(
            onDismissRequest = { reassignTargetId = null },
            title            = { Text("Reasignar tarea") },
            text             = {
                Column {
                    workers.forEach { w ->
                        TextButton(
                            onClick = {
                                onReassignTask(reassignTargetId!!, w.id)
                                reassignTargetId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text     = if (w.position != null) "${w.name} · ${w.position}" else w.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton   = {},
            dismissButton   = {
                TextButton(onClick = { reassignTargetId = null }) { Text("Cancelar") }
            },
        )
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

// ── Dashboard tab ─────────────────────────────────────────────────────────────

@Composable
private fun ChiefDashboardTab(
    activeTasksCount: Int,
    overdueCount: Int,
    pendingReview: Int,
    approvedThisMonth: Int,
    topWorkers: List<WorkerMetric>,
    tasksWithWorkers: List<TaskWithWorker>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Resumen del equipo", style = MaterialTheme.typography.titleMedium, color = CumplrFg)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DashMetricCard(Modifier.weight(1f), "Activas",      "$activeTasksCount", CumplrFg)
            DashMetricCard(Modifier.weight(1f), "Vencidas",     "$overdueCount",
                if (overdueCount > 0) CumplrStatusOverdueFg else CumplrFg)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DashMetricCard(Modifier.weight(1f), "En revisión",       "$pendingReview",     CumplrStatusProgressFg)
            DashMetricCard(Modifier.weight(1f), "Aprobadas (mes)",   "$approvedThisMonth", CumplrStatusDoneFg)
        }

        Spacer(Modifier.height(Spacing.xs))
        Text("Distribución de tareas", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        TaskDistributionChart(tasksWithWorkers)

        if (topWorkers.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text("Top trabajadores", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            topWorkers.forEachIndexed { idx, wm -> TopWorkerRow(idx + 1, wm) }
        }
    }
}

@Composable
private fun DashMetricCard(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color = CumplrFg) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
    }
}

@Composable
private fun TaskDistributionChart(tasksWithWorkers: List<TaskWithWorker>) {
    val bars = listOf(
        "Asignadas"  to tasksWithWorkers.count { it.task.status == TaskStatus.ASSIGNED },
        "En curso"   to tasksWithWorkers.count { it.task.status == TaskStatus.IN_PROGRESS },
        "Revisión"   to tasksWithWorkers.count { it.task.status == TaskStatus.SUBMITTED || it.task.status == TaskStatus.UNDER_REVIEW },
        "Aprobadas"  to tasksWithWorkers.count { it.task.status == TaskStatus.APPROVED },
        "Rechazadas" to tasksWithWorkers.count { it.task.status == TaskStatus.REJECTED },
    )
    val barColors = listOf(CumplrStatusProgressFg, CumplrAccent, CumplrStatusSubmittedFg, CumplrStatusDoneFg, CumplrStatusOverdueFg)
    val maxVal = bars.maxOfOrNull { it.second }.takeIf { it != null && it > 0 } ?: 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
            val gap      = 8.dp.toPx()
            val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
            bars.forEachIndexed { idx, (_, count) ->
                val barH = (count.toFloat() / maxVal) * size.height
                val left = idx * (barWidth + gap)
                drawRect(
                    color   = barColors[idx],
                    topLeft = Offset(left, size.height - barH),
                    size    = Size(barWidth, barH),
                    alpha   = 0.85f,
                )
            }
        }
        Row(
            modifier              = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            bars.forEachIndexed { idx, (label, count) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.weight(1f),
                ) {
                    Text("$count", style = MaterialTheme.typography.labelSmall, color = barColors[idx])
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = CumplrFgMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TopWorkerRow(rank: Int, metric: WorkerMetric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("#$rank", style = MaterialTheme.typography.titleMedium, color = CumplrAccent,
            modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(metric.worker.name, style = MaterialTheme.typography.bodyMedium, color = CumplrFg,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${metric.taskCount} tareas", style = MaterialTheme.typography.bodySmall, color = CumplrFgMuted)
        }
        Text(
            text  = "${(metric.approvalRate * 100).toInt()}%",
            style = MaterialTheme.typography.titleSmall,
            color = when {
                metric.approvalRate >= 0.8f -> CumplrStatusDoneFg
                metric.approvalRate >= 0.5f -> CumplrStatusProgressFg
                else                        -> CumplrStatusOverdueFg
            },
        )
    }
}
