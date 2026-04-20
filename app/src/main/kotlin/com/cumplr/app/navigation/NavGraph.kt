package com.cumplr.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cumplr.app.ui.admin.AdminHomeScreen
import com.cumplr.app.ui.auth.LoginScreen
import com.cumplr.app.ui.chief.ChiefHomeScreen
import com.cumplr.app.ui.chief.TaskReviewScreen
import com.cumplr.app.ui.chief.TeamScreen
import com.cumplr.app.ui.splash.SplashScreen
import com.cumplr.app.ui.worker.TaskDetailScreen
import com.cumplr.app.ui.worker.TaskExecutionScreen
import com.cumplr.app.ui.worker.WorkerHomeScreen
import com.cumplr.core.domain.enums.UserRole

sealed class CumplrRoute(val route: String) {
    object Splash        : CumplrRoute("splash")
    object Login         : CumplrRoute("login")
    object WorkerHome    : CumplrRoute("worker_home")
    object ChiefHome     : CumplrRoute("chief_home")
    object AdminHome     : CumplrRoute("admin_home")
    object TaskDetail    : CumplrRoute("task_detail/{taskId}")
    object TaskExecution : CumplrRoute("task_execution/{taskId}")
    object TaskReview    : CumplrRoute("task_review/{taskId}")
    object ChiefTeam     : CumplrRoute("chief_team")
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

        composable(CumplrRoute.WorkerHome.route) {
            WorkerHomeScreen(
                onLogout = {
                    navController.navigate(CumplrRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
            )
        }

        composable(
            route = CumplrRoute.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToExecution = { taskId ->
                    navController.navigate("task_execution/$taskId")
                },
            )
        }

        composable(
            route = CumplrRoute.TaskExecution.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskExecutionScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(CumplrRoute.WorkerHome.route) {
                        popUpTo(CumplrRoute.WorkerHome.route) { inclusive = true }
                    }
                },
            )
        }
        composable(CumplrRoute.ChiefHome.route) {
            ChiefHomeScreen(
                onLogout = {
                    navController.navigate(CumplrRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onTaskReview = { taskId -> navController.navigate("task_review/$taskId") },
                onTeamClick  = { navController.navigate(CumplrRoute.ChiefTeam.route) },
            )
        }

        composable(
            route     = CumplrRoute.TaskReview.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskReviewScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(CumplrRoute.ChiefHome.route) {
                        popUpTo(CumplrRoute.ChiefHome.route) { inclusive = true }
                    }
                },
            )
        }

        composable(CumplrRoute.ChiefTeam.route) {
            TeamScreen(onBack = { navController.popBackStack() })
        }
        composable(CumplrRoute.AdminHome.route) {
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
