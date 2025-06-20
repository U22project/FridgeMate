package com.example.fridgemate.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class FridgeViewModel : ViewModel() {
    val foodList = mutableStateListOf<String>()

    fun addFoodItems(items: List<String>) {
        foodList.addAll(items)
    }
}