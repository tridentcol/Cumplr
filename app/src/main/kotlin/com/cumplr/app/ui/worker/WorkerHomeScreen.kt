package com.cumplr.app.ui.worker

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.AppNotification
import com.cumplr.core.domain.model.Task
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
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class WorkerDateFilter { ALL, TODAY, WEEK, MONTH }

private fun Task.matchesWorkerDateFilter(filter: WorkerDateFilter): Boolean {
    if (filter == WorkerDateFilter.ALL) return true
    val d = deadline?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        ?: return false
    val today = LocalDate.now()
    return when (filter) {
        WorkerDateFilter.TODAY -> d == today
        WorkerDateFilter.WEEK  -> !d.isBefore(today.with(DayOfWeek.MONDAY)) && !d.isAfter(today.with(DayOfWeek.SUNDAY))
        WorkerDateFilter.MONTH -> d.month == today.month && d.year == today.year
        WorkerDateFilter.ALL   -> true
    }
}

private val ACTIVE_STATUSES  = setOf(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED)
private val HISTORY_STATUSES = setOf(TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW, TaskStatus.APPROVED)

private data class TabDef(val label: String, val filter: (Task) -> Boolean)

private val TASK_TABS = listOf(
    TabDef("Todas")       { true },
    TabDef("Asignadas")   { it.status == TaskStatus.ASSIGNED },
    TabDef("En progreso") { it.status == TaskStatus.IN_PROGRESS },
    TabDef("Enviadas")    { it.status == TaskStatus.SUBMITTED || it.status == TaskStatus.UNDER_REVIEW },
    TabDef("Cumplidas")   { it.status == TaskStatus.APPROVED },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    onLogout: () -> Unit,
    onTaskClick: (String) -> Unit,
    onHistoryTaskClick: (String) -> Unit,
    viewModel: WorkerHomeViewModel = hiltViewModel(),
) {
    val didLogOut     by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isLoading     by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing  by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val tasks         by viewModel.tasks.collectAsStateWithLifecycle()
    val userName      by viewModel.userName.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount   by viewModel.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedNav by remember { mutableStateOf("tareas") }

    val navItems = listOf(
        CumplrNavItem("inicio",         Icons.Outlined.Home,          "Inicio"),
        CumplrNavItem("tareas",         Icons.Outlined.CheckCircle,   "Tareas"),
        CumplrNavItem("notificaciones", Icons.Outlined.Notifications, "Avisos", badgeCount = unreadCount),
        CumplrNavItem("perfil",         Icons.Outlined.Person,        "Perfil"),
    )

    Scaffold(
        modifier             = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor       = CumplrBackground,
        // CumplrBottomNav handles navigationBars insets internally via windowInsetsBottomHeight,
        // so we clear them here to prevent Scaffold from adding a duplicate bottom offset.
        contentWindowInsets  = androidx.compose.foundation.layout.WindowInsets(0),
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
                    "inicio" -> WorkerInicioTab(
                        userName           = userName,
                        tasks              = tasks,
                        isRefreshing       = isRefreshing,
                        onRefresh          = { viewModel.refresh() },
                        onTaskClick        = onTaskClick,
                        onHistoryTaskClick = onHistoryTaskClick,
                    )
                    "tareas" -> WorkerTareasTab(
                        tasks              = tasks,
                        isLoading          = isLoading,
                        isRefreshing       = isRefreshing,
                        onRefresh          = { viewModel.refresh() },
                        onTaskClick        = onTaskClick,
                        onHistoryTaskClick = onHistoryTaskClick,
                    )
                    "notificaciones" -> WorkerNotificationsTab(
                        notifications = notifications,
                        onMarkRead    = { viewModel.markNotificationRead(it) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onClearAll    = { viewModel.clearAllNotifications() },
                        onTaskClick   = { taskId ->
                            val task = tasks.find { it.id == taskId }
                            if (task != null && task.status in HISTORY_STATUSES) onHistoryTaskClick(taskId)
                            else onTaskClick(taskId)
                        },
                    )
                    "perfil" -> WorkerPerfilTab(
                        name     = userName,
                        tasks    = tasks,
                        onLogout = { viewModel.signOut() },
                    )
                }
            }
        }
    }
}

// ── Inicio tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerInicioTab(
    userName: String,
    tasks: List<Task>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (String) -> Unit,
    onHistoryTaskClick: (String) -> Unit,
) {
    val hour       = remember { java.time.LocalTime.now().hour }
    val salutation = remember(hour) {
        when {
            hour < 12 -> "Buenos días"
            hour < 18 -> "Buenas tardes"
            else      -> "Buenas noches"
        }
    }
    val firstName      = remember(userName) { userName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: userName }
    val assigned       = remember(tasks) { tasks.count { it.status == TaskStatus.ASSIGNED } }
    val inProgress     = remember(tasks) { tasks.count { it.status == TaskStatus.IN_PROGRESS } }
    val completed      = remember(tasks) { tasks.count { it.status == TaskStatus.APPROVED } }
    val inProgressTask = remember(tasks) { tasks.firstOrNull { it.status == TaskStatus.IN_PROGRESS } }
    val upcoming       = remember(tasks) {
        tasks
            .filter { it.status == TaskStatus.ASSIGNED }
            .sortedWith(compareBy(nullsLast()) { it.deadline })
            .take(3)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = onRefresh,
        modifier     = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(top = Spacing.lg, bottom = Spacing.md),
            ) {
                Text(salutation, style = MaterialTheme.typography.bodyLarge, color = CumplrFgMuted)
                Text(firstName,  style = MaterialTheme.typography.headlineMedium, color = CumplrFg)
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                WorkerStatCard(Modifier.weight(1f), "Asignadas",  "$assigned",   CumplrStatusProgressFg)
                WorkerStatCard(Modifier.weight(1f), "En curso",   "$inProgress", CumplrAccent)
                WorkerStatCard(Modifier.weight(1f), "Cumplidas",  "$completed",  CumplrStatusDoneFg)
            }

            // In-progress widget
            if (inProgressTask != null) {
                InProgressWidget(
                    task    = inProgressTask,
                    onClick = { onTaskClick(inProgressTask.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.md),
                )
            }

            // Upcoming assigned tasks
            if (upcoming.isNotEmpty()) {
                Text(
                    text     = "Próximas tareas",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = CumplrFgMuted,
                    modifier = Modifier
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.sm),
                )
                upcoming.forEach { task ->
                    TaskCard(
                        task     = task,
                        onClick  = { onTaskClick(task.id) },
                        modifier = Modifier
                            .padding(horizontal = Spacing.lg)
                            .padding(bottom = Spacing.sm),
                    )
                }
            } else if (inProgressTask == null && tasks.isNotEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No hay tareas pendientes.", style = MaterialTheme.typography.bodyMedium, color = CumplrFgMuted)
                }
            }
        }
    }
}

@Composable
private fun InProgressWidget(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsedSeconds by remember(task.id) { mutableLongStateOf(0L) }
    LaunchedEffect(task.id) {
        val start = task.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() }
        while (isActive) {
            elapsedSeconds = if (start != null) Duration.between(start, Instant.now()).seconds else 0L
            delay(1_000)
        }
    }
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val timerText = if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrAccent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CumplrAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector        = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint               = CumplrAccent,
                modifier           = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = task.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = CumplrFg,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text("En curso · $timerText", style = MaterialTheme.typography.labelSmall, color = CumplrAccent)
        }
    }
}

@Composable
private fun WorkerStatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color = CumplrFg) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
    }
}

// ── Tareas tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerTareasTab(
    tasks: List<Task>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (String) -> Unit,
    onHistoryTaskClick: (String) -> Unit,
) {
    var selectedTab  by remember { mutableIntStateOf(0) }
    var dateFilter   by remember { mutableStateOf(WorkerDateFilter.ALL) }
    val filteredTasks = remember(tasks, selectedTab, dateFilter) {
        tasks
            .filter(TASK_TABS[selectedTab].filter)
            .filter { it.matchesWorkerDateFilter(dateFilter) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor   = CumplrBackground,
            contentColor     = CumplrAccent,
            edgePadding      = 0.dp,
            indicator        = { tabPositions ->
                SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = CumplrAccent)
            },
        ) {
            TASK_TABS.forEachIndexed { index, tab ->
                val count = tasks.count(tab.filter)
                Tab(
                    selected               = selectedTab == index,
                    onClick                = { selectedTab = index },
                    selectedContentColor   = CumplrAccent,
                    unselectedContentColor = CumplrFgMuted,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(tab.label, style = MaterialTheme.typography.labelSmall)
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selectedTab == index) CumplrAccent else CumplrSurface2)
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("$count", style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedTab == index) CumplrAccentInk else CumplrFgMuted)
                                }
                            }
                        }
                    },
                )
            }
        }

        // Date filter chips
        Row(
            modifier              = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            listOf(WorkerDateFilter.ALL to "Todos", WorkerDateFilter.TODAY to "Hoy",
                   WorkerDateFilter.WEEK to "Semana", WorkerDateFilter.MONTH to "Mes").forEach { (f, label) ->
                FilterChip(
                    selected = dateFilter == f,
                    onClick  = { dateFilter = f },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            when {
                isLoading -> SkeletonList()
                filteredTasks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Outlined.CheckCircle, "Sin tareas", "No hay tareas en este filtro por ahora.")
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier            = Modifier.fillMaxSize(),
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task    = task,
                            onClick = {
                                if (task.status in ACTIVE_STATUSES) onTaskClick(task.id)
                                else onHistoryTaskClick(task.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Notificaciones tab ────────────────────────────────────────────────────────

@Composable
private fun WorkerNotificationsTab(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onTaskClick: (String) -> Unit,
) {
    val unread = notifications.count { !it.read }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg, bottom = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("Notificaciones", style = MaterialTheme.typography.titleMedium, color = CumplrFg)
            if (notifications.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (unread > 0) {
                        TextButton(onClick = onMarkAllRead) {
                            Text("Marcar todo leído", style = MaterialTheme.typography.labelSmall, color = CumplrAccent)
                        }
                    }
                    TextButton(
                        onClick = onClearAll,
                        colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Limpiar todo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Outlined.Notifications, "Sin notificaciones", "Aquí aparecerán los avisos de tus tareas.")
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(notifications.sortedByDescending { it.createdAt }, key = { it.id }) { notif ->
                    NotificationRow(notif = notif, onClick = {
                        if (!notif.read) onMarkRead(notif.id)
                        notif.taskId?.let { onTaskClick(it) }
                    })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: AppNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (notif.read) CumplrSurface2 else CumplrAccent),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(notif.title, style = MaterialTheme.typography.bodyMedium, color = CumplrFg)
            Text(notif.body,  style = MaterialTheme.typography.bodySmall,  color = CumplrFgMuted)
            Spacer(Modifier.height(2.dp))
            Text(formatNotifTime(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
        }
    }
}

private fun formatNotifTime(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault()))
} catch (_: Exception) { "" }

// ── Perfil tab ────────────────────────────────────────────────────────────────

@Composable
private fun WorkerPerfilTab(name: String, tasks: List<Task>, onLogout: () -> Unit) {
    val approved     = tasks.count { it.status == TaskStatus.APPROVED }
    val rejected     = tasks.count { it.status == TaskStatus.REJECTED }
    val totalFinished = approved + rejected
    val approvalRate = if (totalFinished > 0) approved.toFloat() / totalFinished else null

    val avgMinutes = tasks
        .filter { it.status == TaskStatus.APPROVED && it.startTime != null && it.endTime != null }
        .mapNotNull { t ->
            runCatching {
                Duration.between(Instant.parse(t.startTime!!), Instant.parse(t.endTime!!)).toMinutes()
            }.getOrNull()
        }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

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
        Text(name, style = MaterialTheme.typography.titleLarge, color = CumplrFg)
        Spacer(Modifier.height(Spacing.lg))

        // Stats grid
        if (totalFinished > 0 || avgMinutes != null) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (approvalRate != null) {
                    PerfilStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Tasa de aprobación",
                        value    = "${(approvalRate * 100).toInt()}%",
                        color    = if (approvalRate >= 0.8f) CumplrStatusDoneFg else if (approvalRate >= 0.5f) CumplrStatusProgressFg else CumplrStatusOverdueFg,
                    )
                }
                PerfilStatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Tareas cumplidas",
                    value    = "$approved",
                    color    = CumplrStatusDoneFg,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (avgMinutes != null) {
                    val avgDisplay = if (avgMinutes >= 60) "${avgMinutes / 60}h ${avgMinutes % 60}m" else "${avgMinutes}m"
                    PerfilStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Duración promedio",
                        value    = avgDisplay,
                        color    = CumplrAccent,
                    )
                }
                if (rejected > 0) {
                    PerfilStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Rechazadas",
                        value    = "$rejected",
                        color    = CumplrStatusOverdueFg,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        // Approval rate progress bar
        if (approvalRate != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface)
                    .padding(Spacing.md),
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Rendimiento", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                    Text("$approved / $totalFinished tareas aprobadas", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
                }
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress    = { approvalRate.coerceIn(0f, 1f) },
                    modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color       = if (approvalRate >= 0.8f) CumplrStatusDoneFg else if (approvalRate >= 0.5f) CumplrStatusProgressFg else CumplrStatusOverdueFg,
                    trackColor  = CumplrSurface2,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        Button(
            onClick  = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = CumplrSurface, contentColor = CumplrStatusOverdueFg),
            shape    = RoundedCornerShape(10.dp),
        ) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ExitToApp, "Salir", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PerfilStatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color = CumplrFg) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
    }
}

// ── Skeleton loading ──────────────────────────────────────────────────────────

@Composable
private fun SkeletonList() {
    LazyColumn(
        contentPadding      = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier            = Modifier.fillMaxSize(),
    ) {
        items(4) { TaskCardSkeleton() }
    }
}

@Composable
private fun TaskCardSkeleton() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label         = "alpha",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(CumplrSurface3))
        Column(Modifier.weight(1f).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(CumplrFgMuted.copy(alpha = alpha)))
                Spacer(Modifier.width(Spacing.sm))
                Box(Modifier.size(60.dp, 20.dp).clip(RoundedCornerShape(99.dp)).background(CumplrFgMuted.copy(alpha = alpha)))
            }
            Box(Modifier.fillMaxWidth(0.7f).height(13.dp).clip(RoundedCornerShape(4.dp)).background(CumplrFgMuted.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(CumplrFgMuted.copy(alpha = alpha)))
        }
    }
}
