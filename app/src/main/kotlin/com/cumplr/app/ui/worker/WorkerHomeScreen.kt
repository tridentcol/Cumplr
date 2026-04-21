package com.cumplr.app.ui.worker

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
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
import java.time.LocalTime

private data class TabDef(val label: String, val filter: (Task) -> Boolean)

private val TASK_TABS = listOf(
    TabDef("Todas")       { true },
    TabDef("Asignadas")   { it.status == TaskStatus.ASSIGNED },
    TabDef("En progreso") { it.status == TaskStatus.IN_PROGRESS },
    TabDef("Enviadas")    { it.status == TaskStatus.SUBMITTED || it.status == TaskStatus.UNDER_REVIEW },
    TabDef("Cumplidas")   { it.status == TaskStatus.APPROVED },
)

private val NAV_ITEMS = listOf(
    CumplrNavItem("inicio",   Icons.Outlined.Home,         "Inicio"),
    CumplrNavItem("tareas",   Icons.Outlined.CheckCircle,  "Tareas"),
    CumplrNavItem("reportes", Icons.Outlined.BarChart,     "Reportes"),
    CumplrNavItem("perfil",   Icons.Outlined.Person,       "Perfil"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    onLogout: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: WorkerHomeViewModel = hiltViewModel(),
) {
    val didLogOut    by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val tasks        by viewModel.tasks.collectAsStateWithLifecycle()
    val userName     by viewModel.userName.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedNav by remember { mutableStateOf("tareas") }

    Scaffold(
        modifier       = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = CumplrBackground,
        bottomBar = {
            Surface(shadowElevation = 4.dp, color = CumplrSurface) {
                CumplrBottomNav(
                    items          = NAV_ITEMS,
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
                "inicio"   -> WorkerInicioTab(
                    userName    = userName,
                    tasks       = tasks,
                    onTaskClick = onTaskClick,
                )
                "tareas"   -> WorkerTareasTab(
                    tasks        = tasks,
                    isLoading    = isLoading,
                    isRefreshing = isRefreshing,
                    onRefresh    = { viewModel.refresh() },
                    onTaskClick  = onTaskClick,
                )
                "reportes" -> PlaceholderSection(
                    icon     = Icons.Outlined.BarChart,
                    title    = "Reportes",
                    subtitle = "Próximamente podrás ver tu historial y métricas aquí.",
                )
                "perfil"   -> WorkerPerfilTab(
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
                    task     = task,
                    onClick  = { onTaskClick(task.id) },
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
                        TaskCard(task = task, onClick = { onTaskClick(task.id) })
                    }
                }
            }
        }
    }
}

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

// ── Placeholder tab ───────────────────────────────────────────────────────────

@Composable
private fun PlaceholderSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(icon = icon, title = title, subtitle = subtitle)
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
