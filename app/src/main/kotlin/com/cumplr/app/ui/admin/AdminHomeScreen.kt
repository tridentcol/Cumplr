package com.cumplr.app.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.EmptyState
import com.cumplr.core.ui.theme.Spacing

@Composable
fun AdminHomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.Business,
                title = "Sin empresas registradas",
                subtitle = "Agrega tu primera empresa para comenzar a gestionar tareas operativas.",
                action = {
                    CumplrButton(
                        text = "Agregar empresa",
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xxl),
                    )
                },
            )
        }
    }
}
