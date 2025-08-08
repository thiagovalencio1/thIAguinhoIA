package com.thiaguinho.app.data

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String
)

fun BluetoothDevice.toDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = try { name } catch (e: SecurityException) { "Dispositivo sem nome" },
        address = address
    )
}
