package com.thiaguinho.app.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiaguinho.app.data.AndroidBluetoothController
import com.thiaguinho.app.ui.navigation.AppNavigation
import com.thiaguinho.app.ui.theme.ThIAguinhoTheme
import com.thiaguinho.app.ui.viewmodels.MainViewModel
import com.thiaguinho.app.ui.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy { applicationContext.getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter by lazy { bluetoothManager?.adapter }
    private val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* O estado será recomposto automaticamente */ }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if (canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION))
        }

        val bluetoothController = AndroidBluetoothController(applicationContext)
        val factory = MainViewModelFactory(bluetoothController)

        setContent {
            ThIAguinhoTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                val state by viewModel.state.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        state = state,
                        onStartScan = viewModel::startScan,
                        onStopScan = viewModel::stopScan,
                        onDeviceClick = viewModel::connectToDevice,
                        onDisconnect = viewModel::disconnect,
                        onGetAiAnalysis = viewModel::getAiAnalysisForDtc,
                        onClearAiResult = viewModel::clearAiResult
                    )
                }
            }
        }
    }
}
