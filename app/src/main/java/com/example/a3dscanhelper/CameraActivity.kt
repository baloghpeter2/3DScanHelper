package com.example.a3dscanhelper

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class CameraActivity : AppCompatActivity() {

    companion object {
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)

        val incomingConnectedDevice =
            intent.getParcelableExtra<BluetoothDevice>("passedConnectedDevice")
        var bluetoothSocket =
            incomingConnectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID)

        bluetoothSocket.outputStream.write("asd".toByteArray())

        Log.e("passedDeviceName", incomingConnectedDevice.name)
    }
}