package com.cumplr.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cumplr.app.ui.admin.AdminHomeScreen
import com.cumplr.app.ui.auth.LoginScreen
import com.cumplr.app.ui.chief.ChiefHomeScreen
import com.cumplr.app.ui.splash.SplashScreen
import com.cumplr.app.ui.worker.WorkerHomeScreen
import com.cumplr.core.domain.enums.UserRole

sealed class CumplrRoute(val route: String) {
    object Splash    : CumplrRoute("splash")
    object Login     : CumplrRoute("login")
    object WorkerHome: CumplrRoute("worker_home")
    object ChiefHome : CumplrRoute("chief_home")
    object AdminHome : CumplrRoute("admin_home")
}

private fun UserRole.homeRoute() = when (this) {
    UserRole.WORKER -> CumplrRoute.WorkerHome.route
    UserRole.CHIEF  -> CumplrRoute.ChiefHome.route
    UserRole.ADMIN  -> CumplrRoute.AdminHome.route
}

@Composable
fun CumplrNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = CumplrRoute.Splash.route,
    ) {
        composable(CumplrRoute.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(CumplrRoute.Login.route) {
                        popUpTo(CumplrRoute.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRole = { role ->
                    navController.navigate(role.homeRoute()) {
                        popUpTo(CumplrRoute.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(CumplrRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate(role.homeRoute()) {
                        popUpTo(CumplrRoute.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(CumplrRoute.WorkerHome.route) { WorkerHomeScreen() }
        composable(CumplrRoute.ChiefHome.route)  { ChiefHomeScreen() }
        composable(CumplrRoute.AdminHome.route)  {
            AdminHomeScreen(
                onLogout = {
                    navController.navigate(CumplrRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
