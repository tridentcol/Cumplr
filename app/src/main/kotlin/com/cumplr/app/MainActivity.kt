package com.cumplr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.cumplr.app.navigation.CumplrNavGraph
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
                CumplrNavGraph(navController = navController)
            }
        }
    }
}
