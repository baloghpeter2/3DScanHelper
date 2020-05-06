package com.example.a3dscanhelper

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.*
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import kotlinx.android.synthetic.main.select_device_layout.*
import java.io.File
import java.io.IOException

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

        //Log.e("externalmediadir", getExternalFilesDir(null).toString())
        val allExtDirs=getExternalFilesDirs(null)

//        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
//        //val storageVolumes = storageManager.storageVolumes
//        Log.e("root",Environment.getRootDirectory().toString())
//        //val whatisthis=applicationContext.filesDir.absolutePath
//        Log.e("filesDirAbs",applicationContext.filesDir.absolutePath)
//        //Context.getFilesDir()
//        //val dcimDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
//        Log.e("isEmulated",Environment.isExternalStorageEmulated().toString())
//        Log.e("abspath",Environment.getExternalStorageDirectory().absolutePath)
//        val dcimDir=Environment.getExternalStorageDirectory()
//        val picsDir=File(dcimDir,"CustomPicsFolder")
//        try {
//            picsDir.mkdir();
//            Log.e("folder",picsDir.toString())
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        val picFile=File(picsDir,"picName.png")
////        Log.e("dcimDir", dcimDir.toString())
////        Log.e("picsDir",picsDir.toString())
////        Log.e("fullPicPath",picFile.toString())

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