package com.cumplr.app.ui.admin

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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrRoleAdminBg
import com.cumplr.core.ui.theme.CumplrRoleAdminFg
import com.cumplr.core.ui.theme.CumplrRoleChiefBg
import com.cumplr.core.ui.theme.CumplrRoleChiefFg
import com.cumplr.core.ui.theme.CumplrRoleWorkerBg
import com.cumplr.core.ui.theme.CumplrRoleWorkerFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    viewModel: AdminHomeViewModel = hiltViewModel(),
) {
    val didLogOut     by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isRefreshing  by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val users         by viewModel.users.collectAsStateWithLifecycle()
    val adminName     by viewModel.adminName.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            CumplrAppBar(
                title = "Administrador",
                actions = {
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Outlined.ExitToApp, "Cerrar sesión", tint = CumplrFgMuted)
                    }
                },
            )

            if (adminName.isNotBlank()) {
                Text(
                    text     = "Hola, ${adminName.split(" ").firstOrNull() ?: adminName}.",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = CumplrFg,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = CumplrAccent,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    selectedContentColor   = CumplrAccent,
                    unselectedContentColor = CumplrFgMuted,
                    icon = { Icon(Icons.Outlined.Group, "Usuarios", modifier = Modifier.size(18.dp)) },
                    text = { Text("Usuarios", style = MaterialTheme.typography.labelSmall) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    selectedContentColor   = CumplrAccent,
                    unselectedContentColor = CumplrFgMuted,
                    icon = { Icon(Icons.Outlined.Business, "Empresa", modifier = Modifier.size(18.dp)) },
                    text = { Text("Empresa", style = MaterialTheme.typography.labelSmall) },
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh    = { viewModel.refresh() },
                modifier     = Modifier.weight(1f),
            ) {
                when (selectedTab) {
                    0 -> UsersTab(users = users)
                    1 -> CompanyTab()
                }
            }
        }
    }
}

@Composable
private fun UsersTab(users: List<User>) {
    if (users.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon     = Icons.Outlined.Group,
                title    = "Sin usuarios",
                subtitle = "No hay usuarios registrados en esta empresa.",
            )
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier            = Modifier.fillMaxSize(),
        ) {
            items(users, key = { it.id }) { user -> UserCard(user = user) }
        }
    }
}

@Composable
private fun CompanyTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon     = Icons.Outlined.Business,
            title    = "Información de empresa",
            subtitle = "Próximamente podrás gestionar los datos de tu empresa aquí.",
        )
    }
}

@Composable
private fun UserCard(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(avatarBgFor(user.role)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = user.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleSmall,
                color = avatarFgFor(user.role),
            )
        }

        // Name + position
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = user.name,
                style    = MaterialTheme.typography.bodyMedium,
                color    = CumplrFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(user.position, user.email).joinToString(" · ")
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

        // Role badge
        RoleBadge(role = user.role)

        // Active indicator
        if (!user.active) {
            Text(
                text  = "Inactivo",
                style = MaterialTheme.typography.labelSmall,
                color = CumplrFgSubtle,
            )
        }
    }
}

@Composable
private fun RoleBadge(role: UserRole) {
    val (bg, fg, label) = when (role) {
        UserRole.ADMIN  -> Triple(CumplrRoleAdminBg,  CumplrRoleAdminFg,  "Admin")
        UserRole.CHIEF  -> Triple(CumplrRoleChiefBg,  CumplrRoleChiefFg,  "Jefe")
        UserRole.WORKER -> Triple(CumplrRoleWorkerBg, CumplrRoleWorkerFg, "Trabajador")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

private fun avatarBgFor(role: UserRole) = when (role) {
    UserRole.ADMIN  -> CumplrRoleAdminBg
    UserRole.CHIEF  -> CumplrRoleChiefBg
    UserRole.WORKER -> CumplrRoleWorkerBg
}

private fun avatarFgFor(role: UserRole) = when (role) {
    UserRole.ADMIN  -> CumplrRoleAdminFg
    UserRole.CHIEF  -> CumplrRoleChiefFg
    UserRole.WORKER -> CumplrRoleWorkerFg
}
