package com.example.a3dscanhelper

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.camera_layout.*
import java.io.IOException
import java.io.InputStream

class CameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)

        testButton.setOnClickListener { sendToArduinoCommand("testMessage") }
        val handler = Handler()
        beginListenForData(handler)
    }

    private fun sendToArduinoCommand(input: String) {
        //TODO check if BT connection exists
//        Log.e("btService1", BluetoothService.bluetoothSocket.toString())


        if (BluetoothService.bluetoothSocket != null) {
            try {
                Log.e("sendcommand", input)
                BluetoothService.bluetoothSocket?.outputStream?.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    }

    private fun beginListenForData(handler: Handler) {
        val inputStreamFromArduino = BluetoothService.bluetoothSocket?.inputStream
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        var readBufferPosition: Int = 0
        val readBuffer: ByteArray? = ByteArray(1024)

        val workerThread = Thread(Runnable {
            /*!Thread.currentThread().isInterrupted && !stopWorker
            * ide ez kellene de valamiert atallitodik es nem lep bele*/
            while (true) {
                try {
                    if (inputStreamFromArduino != null) {
                        val bytesAvailable = inputStreamFromArduino.available()
                        if (bytesAvailable > 0) {
                            val packetBytes = ByteArray(bytesAvailable)
                            inputStreamFromArduino.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer,
                                        0,
                                        encodedBytes,
                                        0,
                                        encodedBytes.size
                                    )
                                    val data = String(encodedBytes)
                                    readBufferPosition = 0
                                    handler.post(Runnable { testTextView.text = data })
                                } else if (readBuffer != null) {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    }
                } catch (ex: IOException) {
                    Log.e("exceptionAtThread",ex.message)
                    //stopWorker = true
                }
            }
        })
        if (workerThread != null) {
            workerThread!!.start()
        }
    }
}