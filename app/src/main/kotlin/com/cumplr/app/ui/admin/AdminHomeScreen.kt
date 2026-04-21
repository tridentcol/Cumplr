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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrBottomNav
import com.cumplr.core.ui.component.CumplrNavItem
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrFgSubtle
import com.cumplr.core.ui.theme.CumplrRoleAdminBg
import com.cumplr.core.ui.theme.CumplrRoleAdminFg
import com.cumplr.core.ui.theme.CumplrRoleChiefBg
import com.cumplr.core.ui.theme.CumplrRoleChiefFg
import com.cumplr.core.ui.theme.CumplrRoleWorkerBg
import com.cumplr.core.ui.theme.CumplrRoleWorkerFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.Spacing

private val ADMIN_NAV_ITEMS = listOf(
    CumplrNavItem("usuarios", Icons.Outlined.Group,    "Usuarios"),
    CumplrNavItem("empresa",  Icons.Outlined.Business, "Empresa"),
    CumplrNavItem("reportes", Icons.Outlined.BarChart, "Reportes"),
    CumplrNavItem("perfil",   Icons.Outlined.Person,   "Perfil"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    viewModel: AdminHomeViewModel = hiltViewModel(),
) {
    val didLogOut    by viewModel.didLogOut.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val users        by viewModel.users.collectAsStateWithLifecycle()
    val adminName    by viewModel.adminName.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) { if (didLogOut) onLogout() }

    var selectedNav by remember { mutableStateOf("usuarios") }

    Scaffold(
        modifier       = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = CumplrBackground,
        bottomBar = {
            Surface(shadowElevation = 4.dp, color = CumplrSurface) {
                CumplrBottomNav(
                    items          = ADMIN_NAV_ITEMS,
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
                "usuarios" -> AdminUsuariosTab(
                    users        = users,
                    isRefreshing = isRefreshing,
                    onRefresh    = { viewModel.refresh() },
                )
                "empresa"  -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon     = Icons.Outlined.Business,
                        title    = "Información de empresa",
                        subtitle = "Próximamente podrás gestionar los datos de tu empresa aquí.",
                    )
                }
                "reportes" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon     = Icons.Outlined.BarChart,
                        title    = "Reportes",
                        subtitle = "Próximamente podrás ver métricas y reportes aquí.",
                    )
                }
                "perfil"   -> AdminPerfilTab(
                    name     = adminName,
                    onLogout = { viewModel.signOut() },
                )
            }
        }
    }
}

// ── Usuarios tab ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUsuariosTab(
    users: List<User>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { u ->
            u.name.contains(searchQuery, ignoreCase = true) ||
            u.email.contains(searchQuery, ignoreCase = true) ||
            u.position.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            placeholder   = { Text("Buscar usuario...", color = CumplrFgMuted) },
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = CumplrFgMuted, modifier = Modifier.size(20.dp)) },
            trailingIcon  = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Outlined.Clear, null, tint = CumplrFgMuted) } }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = CumplrFg,
                unfocusedTextColor      = CumplrFg,
                focusedContainerColor   = CumplrSurface,
                unfocusedContainerColor = CumplrSurface,
                focusedBorderColor      = CumplrAccent,
                unfocusedBorderColor    = CumplrBorder,
                cursorColor             = CumplrAccent,
            ),
            shape = RoundedCornerShape(10.dp),
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = onRefresh,
            modifier     = Modifier.weight(1f),
        ) {
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon     = Icons.Outlined.Group,
                        title    = if (searchQuery.isBlank()) "Sin usuarios" else "Sin resultados",
                        subtitle = if (searchQuery.isBlank())
                            "No hay usuarios registrados en esta empresa."
                        else
                            "No se encontraron usuarios con \"$searchQuery\".",
                    )
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(
                        start  = Spacing.lg,
                        end    = Spacing.lg,
                        bottom = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier            = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { user ->
                        AdminUserCard(user = user)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(user: User) {
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
                .size(44.dp)
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = user.name,
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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

        Column(horizontalAlignment = Alignment.End) {
            RoleBadge(role = user.role)
            if (!user.active) {
                Spacer(Modifier.height(3.dp))
                Text("Inactivo", style = MaterialTheme.typography.labelSmall, color = CumplrFgSubtle)
            }
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

// ── Perfil tab ────────────────────────────────────────────────────────────────

@Composable
private fun AdminPerfilTab(name: String, onLogout: () -> Unit) {
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
                .background(CumplrRoleAdminBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = CumplrRoleAdminFg,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Text(text = name, style = MaterialTheme.typography.titleLarge, color = CumplrFg)
        Text(text = "Administrador", style = MaterialTheme.typography.bodyMedium, color = CumplrFgMuted)

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
            Icon(Icons.AutoMirrored.Outlined.ExitToApp, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

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
