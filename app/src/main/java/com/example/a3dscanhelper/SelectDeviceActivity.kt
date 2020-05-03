package com.example.a3dscanhelper

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.select_device_layout.*

class SelectDeviceActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_ADDRESS: String = "PassedDeviceAddress"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_device_layout)

        Log.e("selectdeviceactivity","Wearehere")

        enableBluetooth()
        select_device_refresh.setOnClickListener { pairedDeviceList() }
    }

    private fun enableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(
                applicationContext,
                "This device doesn't support bluetooth",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }
    }
    private fun pairedDeviceList() {
        BluetoothService.getBluetoothService.disconnect()
        pairedDevices = bluetoothAdapter.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()

        if (!pairedDevices.isEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
                list.add(device)
            }
        } else {
            Toast.makeText(
                applicationContext,
                "No paired bluetooth devices found",
                Toast.LENGTH_LONG
            ).show()
        }

        val adapter = SelectDeviceAdapter(this, list)
        select_device_list.adapter = adapter
        select_device_list.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val device: BluetoothDevice = list[position]
                val address: String = device.address
                val intent = Intent(this, InitializeWorkflowActivity::class.java)
                if (address!= BluetoothService.currentAddress){
                    BluetoothService.getBluetoothService.disconnect()
                }
                intent.putExtra(EXTRA_ADDRESS, address)
                startActivity(intent)
            }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (bluetoothAdapter.isEnabled) {
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth has been enabled",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Bluetooth has been disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(
                    applicationContext,
                    "Bluetooth enabling has been canceled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}