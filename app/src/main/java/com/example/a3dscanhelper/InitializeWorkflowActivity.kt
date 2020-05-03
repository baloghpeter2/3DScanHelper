package com.example.a3dscanhelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.initialize_workflow_layout.*
import java.io.IOException
import java.util.*

class InitializeWorkflowActivity : AppCompatActivity() {

    var selectedScanningMode: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.initialize_workflow_layout)

        val address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        ConnectToDevice().execute(address)
        populateSpinner(180)

        startScanningButton.setOnClickListener { startTakingPictures() }
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
                        selectedScanningMode = 1
                    }
                R.id.automaticModeRadioButton ->
                    if (checked) {
                        startScanningButton.isEnabled = true
                        selectedScanningMode = 2
                    }
            }
        }
    }

    private fun startTakingPictures() {
        val intent = Intent(this, CameraActivity::class.java)
        if (BluetoothService.getBluetoothService.getCurrentBluetoothConnection() != null) {
            startActivity(intent)
        }
    }

    inner class ConnectToDevice : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            loadingProgressBar.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: String?): String? {
            BluetoothService.getBluetoothService.setUpBluetoothConnection(params[0].toString())
            return null
        }

        override fun onPostExecute(result: String?) {
            loadingProgressBar.visibility = View.GONE
            if (BluetoothService.isConnected) {
                //Show initial steps
                Log.e("state", "successful")
                initialSetupGrid.visibility = View.VISIBLE
            } else {
                Log.e("state", "failed")
                failedConnectionTextView.visibility = View.VISIBLE
            }
        }
    }
}