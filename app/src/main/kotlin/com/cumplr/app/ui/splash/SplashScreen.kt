package com.cumplr.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBackground

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRole: (UserRole) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (val dest = destination) {
            is SplashDestination.Login        -> onNavigateToLogin()
            is SplashDestination.RoleHome     -> onNavigateToRole(dest.role)
            null                              -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CumplrBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Cumplr",
            style = MaterialTheme.typography.displayLarge,
            color = CumplrAccent,
        )
    }
}
