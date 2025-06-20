package com.example.fridgemate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fridgemate.ui.CameraScreen
import com.example.fridgemate.ui.FridgeScreen
import com.example.fridgemate.ui.TopScreen// Ensure this is correctly imported
import com.example.fridgemate.ui.Theme.FridgeMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FridgeMateTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "top") {
        composable("top") { TopScreen(navController) }
        composable("camera") { CameraScreen(navController) }
        composable("fridge") { FridgeScreen() }
    }
}