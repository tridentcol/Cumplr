package com.cumplr.app.ui.chief

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.model.User
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing

@Composable
fun TeamScreen(
    onBack: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val filteredUsers by viewModel.filteredUsers.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchMode    by viewModel.searchMode.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            CumplrAppBar(
                title = "Equipo",
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = CumplrFgMuted)
                    }
                },
            )

            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder   = { Text("Buscar trabajador...") },
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = CumplrAccent,
                    unfocusedBorderColor    = CumplrBorder,
                    cursorColor             = CumplrAccent,
                    focusedTextColor        = CumplrFg,
                    unfocusedTextColor      = CumplrFg,
                    focusedContainerColor   = CumplrSurface,
                    unfocusedContainerColor = CumplrSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(top = Spacing.md),
            )

            // Search mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf("Cargo", "Nombre", "Ambos").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = searchMode == idx,
                        onClick  = { viewModel.setSearchMode(idx) },
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CumplrAccent,
                            selectedLabelColor     = com.cumplr.core.ui.theme.CumplrAccentInk,
                            containerColor         = CumplrSurface2,
                            labelColor             = CumplrFgMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled          = true,
                            selected         = searchMode == idx,
                            borderColor      = CumplrBorder,
                            selectedBorderColor = CumplrAccent,
                        ),
                    )
                }
            }

            LazyColumn(
                contentPadding      = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    WorkerItem(
                        user           = user,
                        completionRate = viewModel.completionRateFor(user.id),
                        taskCount      = viewModel.taskCountFor(user.id),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerItem(user: User, completionRate: Float, taskCount: Int) {
    val pct = (completionRate * 100).toInt()
    val color = when {
        completionRate >= 0.75f -> CumplrStatusDoneFg
        completionRate >= 0.5f  -> CumplrStatusProgressFg
        else                    -> CumplrStatusOverdueFg
    }

    CumplrCard {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = user.position ?: "Sin cargo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CumplrFg,
                )
                Text(
                    text  = user.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrFgMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$pct%", style = MaterialTheme.typography.bodySmall, color = color)
                Text(text = "$taskCount tareas", style = MaterialTheme.typography.labelSmall, color = CumplrFgMuted)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress   = { completionRate.coerceIn(0f, 1f) },
            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color      = color,
            trackColor = CumplrSurface2,
        )
    }
}
