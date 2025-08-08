package com.thiaguinho.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
    val connectionStatus: StateFlow<String>
    val incomingMessages: Flow<String>

    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(deviceAddress: String)
    fun disconnect()
    fun sendMessage(message: String)
    fun release()
}
