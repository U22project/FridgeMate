package com.example.fridgemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TopScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FridgeMate", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { navController.navigate("camera") }) {
            Text("📸 撮影して追加")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { navController.navigate("fridge") }) {
            Text("🧊 冷蔵庫の中を見る")
        }
    }
}
