package com.cumplr.app.ui.chief

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.component.TaskCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing

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
    viewModel: ChiefHomeViewModel = hiltViewModel(),
) {
    val didLogOut         by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isRefreshing      by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val tasksWithWorkers  by viewModel.tasksWithWorkers.collectAsStateWithLifecycle()
    val activeTasksCount  by viewModel.activeTasksCount.collectAsStateWithLifecycle()
    val completionRate    by viewModel.completionRate.collectAsStateWithLifecycle()
    val overdueCount      by viewModel.overdueCount.collectAsStateWithLifecycle()
    val activeWorkersCount by viewModel.activeWorkersCount.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val filtered = remember(tasksWithWorkers, selectedTab) {
        tasksWithWorkers.filter(CHIEF_TABS[selectedTab].filter)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

            // ── Dashboard ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MetricCard(modifier = Modifier.weight(1f), label = "Activas", value = "$activeTasksCount")
                CompletionMetricCard(modifier = Modifier.weight(1f), rate = completionRate)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Vencidas",
                    value    = "$overdueCount",
                    valueColor = if (overdueCount > 0) CumplrStatusOverdueFg else CumplrFg,
                )
                MetricCard(modifier = Modifier.weight(1f), label = "Trabajadores", value = "$activeWorkersCount")
            }

            // ── Tabs ───────────────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
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
                                            color = if (selectedTab == index) com.cumplr.core.ui.theme.CumplrAccentInk else CumplrFgMuted,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }

            // ── Task list ──────────────────────────────────────────────────────
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
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            Spacer(Modifier.height(Spacing.xs))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        }
    }
}

@Composable
private fun CompletionMetricCard(modifier: Modifier = Modifier, rate: Float) {
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
            Text(text = "Cumplimiento", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            Spacer(Modifier.height(Spacing.xs))
            Text(text = "$pct%", style = MaterialTheme.typography.headlineMedium, color = color)
            Spacer(Modifier.height(Spacing.xs))
            LinearProgressIndicator(
                progress        = { rate.coerceIn(0f, 1f) },
                modifier        = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color           = color,
                trackColor      = CumplrSurface2,
            )
        }
    }
}
