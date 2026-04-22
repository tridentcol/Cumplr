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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.AppNotification
import com.cumplr.core.domain.model.Task
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
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ACTIVE_STATUSES  = setOf(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS)
private val HISTORY_STATUSES = setOf(TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW, TaskStatus.APPROVED, TaskStatus.REJECTED)

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
        CumplrNavItem("inicio",          Icons.Outlined.Home,          "Inicio"),
        CumplrNavItem("tareas",          Icons.Outlined.CheckCircle,   "Tareas"),
        CumplrNavItem("notificaciones",  Icons.Outlined.Notifications, "Avisos", badgeCount = unreadCount),
        CumplrNavItem("perfil",          Icons.Outlined.Person,        "Perfil"),
    )

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
                "inicio" -> WorkerInicioTab(
                    userName           = userName,
                    tasks              = tasks,
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
                    onTaskClick   = { taskId, status ->
                        if (status in HISTORY_STATUSES) onHistoryTaskClick(taskId) else onTaskClick(taskId)
                    },
                )
                "perfil" -> WorkerPerfilTab(
                    name     = userName,
                    onLogout = { viewModel.signOut() },
                )
            }
        }
    }
}

// ── Inicio tab ────────────────────────────────────────────────────────────────

@Composable
private fun WorkerInicioTab(
    userName: String,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onHistoryTaskClick: (String) -> Unit,
) {
    val hour = LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
    val firstName = userName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: userName

    val assigned   = tasks.count { it.status == TaskStatus.ASSIGNED }
    val inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS }
    val completed  = tasks.count { it.status == TaskStatus.APPROVED }

    val upcoming = tasks
        .filter { it.status == TaskStatus.ASSIGNED || it.status == TaskStatus.IN_PROGRESS }
        .sortedWith(compareBy(nullsLast()) { it.deadline })
        .take(3)

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
            Text(
                text  = salutation,
                style = MaterialTheme.typography.bodyLarge,
                color = CumplrFgMuted,
            )
            Text(
                text  = firstName,
                style = MaterialTheme.typography.headlineMedium,
                color = CumplrFg,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WorkerStatCard(Modifier.weight(1f), "Asignadas",  "$assigned",   CumplrStatusProgressFg)
            WorkerStatCard(Modifier.weight(1f), "En curso",   "$inProgress", CumplrAccent)
            WorkerStatCard(Modifier.weight(1f), "Cumplidas",  "$completed",  CumplrStatusDoneFg)
        }

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
                    task    = task,
                    onClick = {
                        if (task.status in ACTIVE_STATUSES) onTaskClick(task.id)
                        else onHistoryTaskClick(task.id)
                    },
                    modifier = Modifier
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.sm),
                )
            }
        } else if (tasks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "No hay tareas pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFgMuted,
                )
            }
        }
    }
}

@Composable
private fun WorkerStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color = CumplrFg,
) {
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val filteredTasks = remember(tasks, selectedTab) {
        tasks.filter(TASK_TABS[selectedTab].filter)
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
            TASK_TABS.forEachIndexed { index, tab ->
                val count = tasks.count(tab.filter)
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
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedTab == index) CumplrAccent else CumplrSurface2,
                                        )
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

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = onRefresh,
            modifier     = Modifier.weight(1f),
        ) {
            when {
                isLoading          -> SkeletonList()
                filteredTasks.isEmpty() -> Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon     = Icons.Outlined.CheckCircle,
                        title    = "Sin tareas",
                        subtitle = "No hay tareas en este filtro por ahora.",
                    )
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
    onTaskClick: (String, TaskStatus?) -> Unit,
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
            Text(
                text  = "Notificaciones",
                style = MaterialTheme.typography.titleMedium,
                color = CumplrFg,
            )
            if (unread > 0) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text  = "Marcar todo como leído",
                        style = MaterialTheme.typography.labelSmall,
                        color = CumplrAccent,
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon     = Icons.Outlined.Notifications,
                    title    = "Sin notificaciones",
                    subtitle = "Aquí aparecerán los avisos de tus tareas.",
                )
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(notifications.sortedByDescending { it.createdAt }, key = { it.id }) { notif ->
                    NotificationRow(
                        notif   = notif,
                        onClick = {
                            if (!notif.read) onMarkRead(notif.id)
                            if (notif.taskId != null) onTaskClick(notif.taskId, null)
                        },
                    )
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
            Text(
                text  = notif.title,
                style = MaterialTheme.typography.bodyMedium,
                color = CumplrFg,
            )
            Text(
                text  = notif.body,
                style = MaterialTheme.typography.bodySmall,
                color = CumplrFgMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = formatNotifTime(notif.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = CumplrFgMuted,
            )
        }
    }
}

private fun formatNotifTime(iso: String): String = try {
    val instant = Instant.parse(iso)
    val zdt     = instant.atZone(ZoneId.systemDefault())
    zdt.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault()))
} catch (_: Exception) { "" }

// ── Perfil tab ────────────────────────────────────────────────────────────────

@Composable
private fun WorkerPerfilTab(name: String, onLogout: () -> Unit) {
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
            androidx.compose.material3.Icon(
                imageVector        = Icons.AutoMirrored.Outlined.ExitToApp,
                contentDescription = "Salir",
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CumplrFgMuted.copy(alpha = alpha))
                )
                Spacer(Modifier.width(Spacing.sm))
                Box(
                    Modifier
                        .size(60.dp, 20.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(CumplrFgMuted.copy(alpha = alpha))
                )
            }
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CumplrFgMuted.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CumplrFgMuted.copy(alpha = alpha))
            )
        }
    }
}
