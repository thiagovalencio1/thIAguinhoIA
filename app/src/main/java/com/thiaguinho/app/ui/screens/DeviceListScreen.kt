package com.thiaguinho.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thiaguinho.app.data.BluetoothDeviceDomain
import com.thiaguinho.app.ui.viewmodels.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    state: MainUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Conectar ao Veículo") })
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = onStartScan) {
                        Text(text = "Procurar")
                    }
                    Button(onClick = onStopScan) {
                        Text(text = "Parar")
                    }
                }
                Text(
                    text = state.connectionStatus,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        BluetoothDeviceList(
            pairedDevices = state.pairedDevices,
            scannedDevices = state.scannedDevices,
            onClick = onDeviceClick,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDeviceDomain>,
    scannedDevices: List<BluetoothDeviceDomain>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "Dispositivos Pareados",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (pairedDevices.isEmpty()) {
            item { Text("Nenhum dispositivo pareado encontrado.", modifier = Modifier.padding(horizontal = 16.dp)) }
        } else {
            items(pairedDevices) { device ->
                DeviceRow(device = device, onClick = onClick)
            }
        }

        item {
            Text(
                text = "Dispositivos Encontrados",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (scannedDevices.isEmpty()) {
            item { Text("Nenhum dispositivo novo encontrado. Clique em 'Procurar'.", modifier = Modifier.padding(horizontal = 16.dp)) }
        } else {
            items(scannedDevices) { device ->
                DeviceRow(device = device, onClick = onClick)
            }
        }
    }
}

@Composable
fun DeviceRow(device: BluetoothDeviceDomain, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(device.address) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = device.name ?: "(Sem nome)", fontWeight = FontWeight.Bold)
        Text(text = device.address, fontSize = 12.sp)
    }
}
