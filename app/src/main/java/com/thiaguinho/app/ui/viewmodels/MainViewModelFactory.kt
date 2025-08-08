package com.thiaguinho.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thiaguinho.app.data.BluetoothController

class MainViewModelFactory(private val bluetoothController: BluetoothController) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(bluetoothController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
