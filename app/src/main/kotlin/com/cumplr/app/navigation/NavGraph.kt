package com.cumplr.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cumplr.app.ui.admin.AdminHomeScreen
import com.cumplr.app.ui.chief.ChiefHomeScreen
import com.cumplr.app.ui.worker.WorkerHomeScreen

sealed class CumplrRoute(val route: String) {
    object WorkerHome : CumplrRoute("worker_home")
    object ChiefHome : CumplrRoute("chief_home")
    object AdminHome : CumplrRoute("admin_home")
    // Future: Splash, Login, nested role graphs
}

@Composable
fun CumplrNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = CumplrRoute.WorkerHome.route
    ) {
        composable(CumplrRoute.WorkerHome.route) { WorkerHomeScreen() }
        composable(CumplrRoute.ChiefHome.route)  { ChiefHomeScreen() }
        composable(CumplrRoute.AdminHome.route)  { AdminHomeScreen() }
    }
}
