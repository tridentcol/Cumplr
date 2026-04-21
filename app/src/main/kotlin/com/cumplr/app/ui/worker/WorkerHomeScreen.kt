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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.component.TaskCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing
import java.time.LocalTime

private data class TabDef(val label: String, val filter: (Task) -> Boolean)

private val TABS = listOf(
    TabDef("Todas")      { true },
    TabDef("Asignadas")  { it.status == TaskStatus.ASSIGNED },
    TabDef("En progreso"){ it.status == TaskStatus.IN_PROGRESS },
    TabDef("Enviadas")   { it.status == TaskStatus.SUBMITTED || it.status == TaskStatus.UNDER_REVIEW },
    TabDef("Cumplidas")  { it.status == TaskStatus.APPROVED },
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val filteredTasks = remember(tasks, selectedTab) {
        tasks.filter(TABS[selectedTab].filter)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

            GreetingHeader(
                name      = userName,
                taskCount = tasks.count { it.status == TaskStatus.ASSIGNED || it.status == TaskStatus.IN_PROGRESS },
            )

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
                TABS.forEachIndexed { index, tab ->
                    val count = tasks.count(tab.filter)
                    Tab(
                        selected              = selectedTab == index,
                        onClick               = { selectedTab = index },
                        selectedContentColor  = CumplrAccent,
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
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedTab == index) CumplrAccent else CumplrSurface2
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
                onRefresh    = { viewModel.refresh() },
                modifier     = Modifier.weight(1f),
            ) {
                when {
                    isLoading -> SkeletonList()
                    filteredTasks.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon     = Icons.Outlined.CheckCircle,
                            title    = "Sin tareas",
                            subtitle = "No hay tareas en este filtro por ahora.",
                        )
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskCard(task = task, onClick = { onTaskClick(task.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String, taskCount: Int) {
    if (name.isBlank()) return
    val hour = LocalTime.now().hour
    val salutation = when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
    val firstName = name.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: name
    val subtitle = when (taskCount) {
        0    -> "No hay tareas pendientes."
        1    -> "Tienes 1 tarea activa."
        else -> "Tienes $taskCount tareas activas."
    }
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
        Spacer(Modifier.height(2.dp))
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = CumplrFgMuted,
        )
    }
}

@Composable
private fun SkeletonList() {
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(4) { TaskCardSkeleton() }
    }
}

@Composable
private fun TaskCardSkeleton() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
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
                modifier = Modifier.fillMaxWidth(),
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
