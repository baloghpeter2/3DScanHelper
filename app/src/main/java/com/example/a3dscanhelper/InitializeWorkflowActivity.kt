package com.example.a3dscanhelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Camera
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.initialize_workflow_layout.*
import java.io.IOException
import java.util.*
import kotlin.properties.Delegates

class InitializeWorkflowActivity : AppCompatActivity() {

    var selectedScanningMode: Int = -1
    lateinit var connectedDevice:BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.initialize_workflow_layout)
        val address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        startScanningButton.isEnabled = false
        ConnectToDevice(this, address).execute()
        populateSpinner(180);
        startScanningButton.setOnClickListener { startTakingPictures() }

//        control_led_on.setOnClickListener { sendCommand("a") }
//        control_led_off.setOnClickListener { sendCommand("b") }
//        control_led_disconnect.setOnClickListener { disconnect() }
    }

    private fun populateSpinner(size: Int) {
        val spinnerArray = Array(size) { i -> (i + 1).toString() }
        val arrayAdapter = ArrayAdapter(this, R.layout.spinner_item_layout, spinnerArray)
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        imageCountSpinner.adapter = arrayAdapter
    }

    fun onScanningModeSelectionChanged(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                R.id.manualModeRadioButton ->
                    if (checked) {
                        startScanningButton.isEnabled = true
                        selectedScanningMode=1
                    }
                R.id.automaticModeRadioButton ->
                    if (checked) {
                        startScanningButton.isEnabled = true
                        selectedScanningMode=2
                    }
            }
        }
    }

    private fun startTakingPictures() {
        val intent= Intent(this,CameraActivity::class.java)
        if(connectedDevice!=null){
            intent.putExtra("passedConnectedDevice",connectedDevice)
            startActivity(intent)
        }
    }


    inner class ConnectToDevice(c: Context, address: String) : AsyncTask<Void, Void, String>() {

        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        var m_address: String = address
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            loadingProgressBar.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                    connectedDevice=device
                }
            } catch (e: IOException) {
                connectSuccess = false
                Log.e("exception",e.message)
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            loadingProgressBar.visibility = View.GONE
            if (connectSuccess) {
                m_isConnected = true
                //Show initial steps
                Log.e("state","successful")
                initialSetupGrid.visibility = View.VISIBLE
            }
            else {
                Log.e("state","failed")
                failedConnectionTextView.visibility = View.VISIBLE
            }
        }
    }
}