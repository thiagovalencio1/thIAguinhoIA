package com.thiaguinho.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiaguinho.app.data.BluetoothController
import com.thiaguinho.app.data.BluetoothDeviceDomain
import com.thiaguinho.app.network.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var dataPollingJob: Job? = null

    init {
        // Combina todos os fluxos do controller em um único state
        combine(
            bluetoothController.scannedDevices,
            bluetoothController.pairedDevices,
            bluetoothController.isConnected,
            bluetoothController.connectionStatus
        ) { scanned, paired, isConnected, status ->
            _state.update {
                it.copy(
                    scannedDevices = scanned,
                    pairedDevices = paired,
                    isConnected = isConnected,
                    connectionStatus = status
                )
            }
        }.launchIn(viewModelScope)

        // Observa as mensagens recebidas
        bluetoothController.incomingMessages
            .onEach { message -> parseObdResponse(message) }
            .launchIn(viewModelScope)

        // Observa o status da conexão para iniciar/parar o polling
        bluetoothController.isConnected
            .onEach { isConnected ->
                if (isConnected) {
                    startDataPolling()
                } else {
                    stopDataPolling()
                    _state.update { it.copy(vehicleData = VehicleData(), dtcCodes = emptyList()) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun getAiAnalysisForDtc(dtcCode: String) {
        _state.update { it.copy(isAiLoading = true, aiAnalysisResult = null) }
        viewModelScope.launch {
            val result = GeminiRepository.getDtcExplanation(dtcCode)
            _state.update {
                it.copy(
                    isAiLoading = false,
                    aiAnalysisResult = result
                )
            }
        }
    }

    fun clearAiResult() {
        _state.update { it.copy(aiAnalysisResult = null) }
    }

    fun startScan() { bluetoothController.startDiscovery() }
    fun stopScan() { bluetoothController.stopDiscovery() }
    fun connectToDevice(address: String) { bluetoothController.connectToDevice(address) }
    fun disconnect() { bluetoothController.disconnect() }

    private fun startDataPolling() {
        dataPollingJob?.cancel()
        dataPollingJob = viewModelScope.launch(Dispatchers.IO) {
            bluetoothController.sendMessage("ATZ")
            delay(1000)
            bluetoothController.sendMessage("ATE0")
            delay(500)
            bluetoothController.sendMessage("ATSP0")
            delay(500)

            while (true) {
                if (!_state.value.isConnected) break
                bluetoothController.sendMessage("010C") // RPM
                delay(500)
                if (!_state.value.isConnected) break
                bluetoothController.sendMessage("010D") // Velocidade
                delay(500)
                if (!_state.value.isConnected) break
                bluetoothController.sendMessage("0105") // Temp. Arrefecimento
                delay(500)
                if (!_state.value.isConnected) break
                bluetoothController.sendMessage("03") // Pedir DTCs
                delay(2000)
            }
        }
    }

    private fun stopDataPolling() {
        dataPollingJob?.cancel()
    }

    private fun parseObdResponse(response: String) {
        val cleanResponse = response.replace("\r", " ").replace("\n", " ").trim()
        val parts = cleanResponse.split(" ").filter { it.isNotBlank() }

        if (parts.isEmpty() || parts[0].contains("SEARCHING") || parts[0] == "NO" && parts.getOrNull(1) == "DATA") return

        try {
            when {
                // RPM: 41 0C XX YY
                parts.size >= 4 && parts[0] == "41" && parts[1] == "0C" -> {
                    val rpm = (parts[2].toInt(16) * 256 + parts[3].toInt(16)) / 4
                    _state.update { it.copy(vehicleData = it.vehicleData.copy(rpm = rpm)) }
                }
                // Velocidade: 41 0D XX
                parts.size >= 3 && parts[0] == "41" && parts[1] == "0D" -> {
                    val speed = parts[2].toInt(16)
                    _state.update { it.copy(vehicleData = it.vehicleData.copy(speed = speed)) }
                }
                // Temperatura: 41 05 XX
                parts.size >= 3 && parts[0] == "41" && parts[1] == "05" -> {
                    val temp = parts[2].toInt(16) - 40
                    _state.update { it.copy(vehicleData = it.vehicleData.copy(coolantTemp = temp)) }
                }
                // DTCs: 43 XX YY XX YY...
                parts.isNotEmpty() && parts[0] == "43" -> {
                    val dtcs = parseDtcCodes(parts.drop(1))
                    _state.update { it.copy(dtcCodes = dtcs) }
                }
                // Resposta ao comando 03 sem DTCs
                cleanResponse.startsWith("43") && cleanResponse.length <= 6 -> {
                     if(cleanResponse.substring(2).replace(" ","").all { it == '0' }) {
                        _state.update { it.copy(dtcCodes = emptyList()) }
                     }
                }
            }
        } catch (e: Exception) {
            // Ignorar erros de parsing por enquanto
        }
    }

    private fun parseDtcCodes(data: List<String>): List<String> {
        val codes = mutableListOf<String>()
        val hexString = data.joinToString("").replace(" ", "")

        for (i in 0 until hexString.length step 4) {
            if (i + 4 > hexString.length) break
            val codeHex = hexString.substring(i, i + 4)
            if (codeHex == "0000") continue

            val byte1 = codeHex.substring(0, 2).toInt(16)
            val byte2 = codeHex.substring(2, 4)

            val firstChar = when (byte1 shr 6) { // Bits 7,6
                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                3 -> 'U'
                else -> '?'
            }

            val secondChar = when ((byte1 shr 4) and 0x03) { // Bits 5,4
                0 -> '0'
                1 -> '1'
                2 -> '2'
                3 -> '3'
                else -> '?'
            }

            val thirdChar = (byte1 and 0x0F).toString(16).uppercase() // Bits 3,2,1,0

            codes.add("$firstChar$secondChar$thirdChar$byte2")
        }
        return codes.distinct()
    }


    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}

data class MainUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isConnected: Boolean = false,
    val connectionStatus: String = "Desconectado",
    val vehicleData: VehicleData = VehicleData(),
    val dtcCodes: List<String> = emptyList(),
    val isAiLoading: Boolean = false,
    val aiAnalysisResult: Result<String>? = null
)

data class VehicleData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolantTemp: Int = 0
)
