package com.thiaguinho.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter by lazy { bluetoothManager?.adapter }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _pairedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Desconectado")
    override val connectionStatus: StateFlow<String> get() = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>()
    override val incomingMessages: Flow<String> get() = _incomingMessages.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toDomain()
            if (newDevice in devices || newDevice.name.isNullOrBlank()) devices else devices + newDevice
        }
    }

    private var currentSocket: BluetoothSocket? = null
    private var dataTransferJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        updatePairedDevices()
        context.registerReceiver(foundDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            _connectionStatus.value = "Permissão de Scan negada."
            return
        }
        updatePairedDevices()
        _scannedDevices.value = emptyList()
        bluetoothAdapter?.startDiscovery()
        _connectionStatus.value = "Procurando dispositivos..."
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun connectToDevice(deviceAddress: String) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            _connectionStatus.value = "Permissão de Conexão negada."
            return
        }
        _connectionStatus.value = "Conectando..."
        coroutineScope.launch {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: run {
                _connectionStatus.value = "Dispositivo não encontrado."
                return@launch
            }
            stopDiscovery()

            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                currentSocket = socket
                socket.connect()
                _isConnected.value = true
                _connectionStatus.value = "Conectado a ${device.name ?: "dispositivo"}"
                dataTransferJob = launch { listenForIncomingMessages(socket) }
            } catch (e: IOException) {
                e.printStackTrace()
                _isConnected.value = false
                _connectionStatus.value = "Falha na conexão. Tente novamente."
                currentSocket?.close()
                currentSocket = null
            }
        }
    }

    override fun disconnect() {
        _connectionStatus.value = "Desconectando..."
        dataTransferJob?.cancel()
        try { currentSocket?.close() } catch (e: IOException) { /* Ignorar */ }
        currentSocket = null
        _isConnected.value = false
        _connectionStatus.value = "Desconectado"
    }

    override fun sendMessage(message: String) {
        if (!_isConnected.value || currentSocket == null) return
        coroutineScope.launch {
            try {
                currentSocket?.outputStream?.write((message + "\r").toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                _isConnected.value = false
                _connectionStatus.value = "Erro ao enviar dados. Conexão perdida."
            }
        }
    }

    private suspend fun listenForIncomingMessages(socket: BluetoothSocket) {
        val inputStream = socket.inputStream
        val buffer = ByteArray(1024)
        while (_isConnected.value) {
            try {
                val byteCount = inputStream.read(buffer)
                if (byteCount > 0) {
                    val message = buffer.decodeToString(0, byteCount).replace(">", "").trim()
                    if (message.isNotBlank()) {
                        _incomingMessages.emit(message)
                    }
                }
            } catch (e: IOException) {
                _connectionStatus.value = "Conexão perdida."
                _isConnected.value = false
                break
            }
        }
    }

    override fun release() {
        try { context.unregisterReceiver(foundDeviceReceiver) } catch (e: Exception) { /* Ignorar */ }
        disconnect()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        bluetoothAdapter?.bondedDevices?.map { it.toDomain() }?.also { devices ->
            _pairedDevices.update { devices }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
