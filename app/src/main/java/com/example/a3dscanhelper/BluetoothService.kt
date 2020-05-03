package com.example.a3dscanhelper

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService() : Application() {

    var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        var bluetoothSocket: BluetoothSocket?=null
        var isConnected:Boolean=false
        var currentAddress:String=""
        lateinit var bluetoothDevice: BluetoothDevice
        lateinit var bluetoothAdapter: BluetoothAdapter
        val getBluetoothService = BluetoothService()
    }

    fun setUpBluetoothConnection(address: String) {
        try {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                currentAddress=address
                val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(currentAddress)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID)
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                bluetoothSocket!!.connect()
                bluetoothDevice = device
                if (bluetoothSocket!!.isConnected) {
                    isConnected = true
                }
            }
        } catch (e: IOException) {
            isConnected = false
            Log.e("exception", e.message)
        }
    }

    fun getCurrentBluetoothConnection(): BluetoothSocket? {
        return bluetoothSocket
    }

    fun disconnect(){
        if(bluetoothSocket!=null){
            bluetoothSocket!!.close()
            bluetoothSocket=null
            isConnected=false
        }
        Log.e("isdisconnected", bluetoothSocket?.isConnected.toString())
    }
}