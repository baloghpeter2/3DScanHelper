package com.example.a3dscanhelper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import kotlinx.android.synthetic.main.camera_layout.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.properties.ObservableProperty

class CameraActivity : AppCompatActivity() {

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    var fotoapparat: Fotoapparat? = null
    var fotoapparatState: FotoapparatState? = null
    var flashState: FlashState? = null
    var savedPicturesPathList = mutableListOf<String>()
    var isWorkflowPaused = false
    var stopWorker = false
    var numberOfPicturesTaken: Int = 0
    lateinit var numberOfPicturesPerRev: String
    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)

        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF
        createFotoapparat()
        handler = Handler()
        stopWorker = false
        beginListenForData(handler)

        captureImageButton.isEnabled=false
        captureImageButton.setBackgroundColor(resources.getColor(R.color.colorRed))
        stepBackAndDeleteButton.isEnabled=false
        stepBackAndDeleteButton.setBackgroundColor(resources.getColor(R.color.colorRed))
        pauseButton.setBackgroundColor(resources.getColor(R.color.colorGreen))

        numberOfPicturesPerRev = intent.getStringExtra("numberOfPicturesPerRevolution")
        sendToArduinoCommand("revcounter|" + numberOfPicturesPerRev)
        numberOfPicturesTaken=0
        numberOfPicturesTakenLabel.text=numberOfPicturesTaken.toString()
        supposedNumberOfPicturesLabel.text = "/$numberOfPicturesPerRev"

        startWorkflowButton.setOnClickListener {
            startCapturingImages();
        }
        captureImageButton.setOnClickListener {
            sendToArduinoCommand("captureOneImage")
        }
        stepBackAndDeleteButton.setOnClickListener {
            stepBackAndDelete()
        }
        pauseButton.setOnClickListener {
            handlePausingWorkflow()
        }
        flashButton.setOnClickListener {
            changeFlashState()
        }

        try {
            this.supportActionBar?.hide()
        } catch (ex: Exception) {
            Log.e("ActionBarHideException", ex.message)
        }
    }

    private fun startCapturingImages() {
        startWorkflowButton.visibility = View.GONE
        sendToArduinoCommand("takepicturesAuto")

        /*while (numberOfPicturesTaken<numberOfPicturesPerRev.toInt()){
            captureImage()
            sendToArduinoCommand("rotate")
            if (rotationFinished){
                rotationFinished=false
                numberOfPicturesTaken++
            }
        }*/
    }

    private fun stepBackAndDelete() {
        sendToArduinoCommand("backAndDelete")
        deleteLastImage()
    }

    private fun handlePausingWorkflow() {
        if (!isWorkflowPaused) {
            isWorkflowPaused = true
            sendToArduinoCommand("paused")
            captureImageButton.isEnabled = true
            captureImageButton.setBackgroundColor(resources.getColor(R.color.colorGreen))
            stepBackAndDeleteButton.isEnabled = true
            stepBackAndDeleteButton.setBackgroundColor(resources.getColor(R.color.colorGreen))
            pauseButton.setImageResource(android.R.drawable.ic_media_play)
        } else {
            isWorkflowPaused = false
            sendToArduinoCommand("notPaused")
            captureImageButton.isEnabled = false
            captureImageButton.setBackgroundColor(resources.getColor(R.color.colorRed))
            stepBackAndDeleteButton.isEnabled = false
            stepBackAndDeleteButton.setBackgroundColor(resources.getColor(R.color.colorRed))
            pauseButton.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun changeFlashState() {
        Log.e("Flash", "Flash state changed")
        fotoapparat?.updateConfiguration(
            CameraConfiguration(
                flashMode = if (flashState == FlashState.TORCH) off() else torch()
            )
        )
        if (flashState == FlashState.TORCH) flashState = FlashState.OFF
        else flashState = FlashState.TORCH
    }

    private fun captureImage() {
        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")

        if (hasNoPermissions()) {
            val permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, 0)
        } else {
            Log.e("savePath", file.path)
            fotoapparat?.takePicture()?.saveToFile(file)
            numberOfPicturesTaken++
            numberOfPicturesTakenLabel.text=numberOfPicturesTaken.toString()
            savedPicturesPathList.add(file.path)
            Toast.makeText(baseContext, "Photo capture succeeded: ${file.path}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun deleteLastImage() {
        if (!savedPicturesPathList.isEmpty()) {
            val pictureToDelete = File(savedPicturesPathList.last())
            if (pictureToDelete.exists()) {
                savedPicturesPathList.remove(pictureToDelete.path)
                pictureToDelete.delete()
                numberOfPicturesTaken--
                numberOfPicturesTakenLabel.text=numberOfPicturesTaken.toString()
                Log.e("picDeleted", pictureToDelete.path)
                Toast.makeText(baseContext, "Picture deleted", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(baseContext, "Picture folder is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleIncomingDataFromArduino(data: String) {
        if (data.contains('C')) {
            captureImage()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.e("reset","resetSent")
        sendToArduinoCommand("reset")
        stopWorker = true
    }

    private fun sendToArduinoCommand(input: String) {
        //TODO check if BT connection exists. If not, cut the rotation of the platform in auto mode
        //only rotate when phone sends signal to arduino

        if (BluetoothService.bluetoothSocket != null) {
            try {
                Log.e("sendcommand", input)
                //BluetoothService.bluetoothSocket?.outputStream?.write(input.toByteArray())
                BluetoothService.bluetoothSocket?.outputStream?.write(input.toByteArray())
            } catch (ex: Exception) {
                Log.e("SendCommandException", ex.message)
            }
        }
    }

    private fun beginListenForData(handler: Handler) {
        val inputStreamFromArduino = BluetoothService.bluetoothSocket?.inputStream
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        var readBufferPosition: Int = 0
        val readBuffer: ByteArray? = ByteArray(1024)

        val workerThread = Thread(Runnable {
            while (!stopWorker) {
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
                                        readBuffer, 0, encodedBytes, 0, encodedBytes.size
                                    )
                                    //val data = String(encodedBytes).replace("\r", "")
                                    val data = String(encodedBytes)
                                    readBufferPosition = 0
                                    Log.e("data", data)
                                    handler.post({ handleIncomingDataFromArduino(data) })
                                } else if (readBuffer != null) {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("exceptionAtThread", ex.message)
                    stopWorker = true
                }
            }
        })
        if (workerThread != null) {
            workerThread.start()
        }
    }

    private fun createFotoapparat() {
        fotoapparat = Fotoapparat(
            context = this,
            view = camera_view,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            cameraConfiguration = CameraConfiguration(
                focusMode = firstAvailable(
                    macro(),
                    autoFocus()
                )
            ),
            focusView = focus_View,
            cameraErrorCallback = { error ->
                Log.e("cameraErrorCallback", error.message)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        println("Onstart")

        if (hasNoPermissions()) {
            requestPermission()
        } else {
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
        }
    }

    private fun hasNoPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
        FotoapparatState.OFF
    }

    override fun onPause() {
        super.onPause()
        println("OnPause")
    }

    override fun onResume() {
        super.onResume()
        println("OnResume")

        println(fotoapparatState)

        if (!hasNoPermissions() && fotoapparatState == FotoapparatState.OFF) {
            val intent = Intent(baseContext, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

enum class FlashState {
    TORCH, OFF
}

enum class FotoapparatState {
    ON, OFF
}
