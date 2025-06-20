package com.example.fridgemate.ui.Theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable

@Composable
fun FridgeMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = typography,
        content = content
    )
}