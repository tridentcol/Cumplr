package com.cumplr.app.ui.chief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.Spacing

@Composable
fun ChiefHomeScreen(
    onLogout: () -> Unit,
    viewModel: ChiefHomeViewModel = hiltViewModel(),
) {
    val didLogOut by viewModel.didLogOut.collectAsStateWithLifecycle()

    LaunchedEffect(didLogOut) {
        if (didLogOut) onLogout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CumplrAppBar(
                title = "Jefe de Área",
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.Center,
            ) {
                CumplrCard {
                    Text(
                        text = "Resumen del equipo",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CumplrFg,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "3 tareas activas · 1 pendiente de revisión",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CumplrFgMuted,
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
                CumplrButton(
                    text = "Ver todas las tareas",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
