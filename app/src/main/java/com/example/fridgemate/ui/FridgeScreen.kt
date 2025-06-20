package com.example.fridgemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FridgeScreen(fridgeViewModel: FridgeViewModel = viewModel()) {
    val items = fridgeViewModel.foodList

    Column(modifier = Modifier.padding(16.dp)) {
        Text("冷蔵庫の中", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (items.isEmpty()) {
            Text("まだ何も入っていません")
        } else {
            LazyColumn {
                items(items) { item ->
                    Text(text = "・$item", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
