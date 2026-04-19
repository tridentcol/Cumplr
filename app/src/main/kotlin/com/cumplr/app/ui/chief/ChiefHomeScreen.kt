package com.cumplr.app.ui.chief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrCard
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.Spacing

@Composable
fun ChiefHomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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
