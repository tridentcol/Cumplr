package com.cumplr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cumplr.app.navigation.CumplrNavGraph
import com.cumplr.app.navigation.CumplrRoute
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.CumplrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CumplrTheme {
                val navController = rememberNavController()
                Box(modifier = Modifier.fillMaxSize()) {
                    CumplrNavGraph(navController = navController)
                    DevRoleSwitcher(navController = navController)
                }
            }
        }
    }
}

// TODO Layer 3: remove when login flow is implemented
@Composable
private fun BoxScope.DevRoleSwitcher(navController: NavHostController) {
    val buttons = listOf(
        "W" to CumplrRoute.WorkerHome.route,
        "J" to CumplrRoute.ChiefHome.route,
        "A" to CumplrRoute.AdminHome.route,
    )
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            .background(CumplrSurface2, RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        buttons.forEach { (label, route) ->
            TextButton(onClick = {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }) {
                Text(label, color = CumplrAccent)
            }
        }
    }
}
