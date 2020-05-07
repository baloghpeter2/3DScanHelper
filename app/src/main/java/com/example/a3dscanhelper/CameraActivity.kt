package com.example.a3dscanhelper

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.camera_layout.*
import java.io.IOException
//Imports for camera integration
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private var savedPicturesPathList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)

        viewFinder = findViewById(R.id.view_finder)
        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        //testButton.setOnClickListener { sendToArduinoCommand("testMessage") }
        testButton.setOnClickListener { captureImage() }
        val handler = Handler()
        beginListenForData(handler)
    }

    // Create configuration object for the image capture use case
    val imageCaptureConfig = ImageCaptureConfig.Builder()
        .apply {
            // We don't set a resolution for image capture; instead, we
            // select a capture mode which will infer the appropriate
            // resolution based on aspect ration and requested mode
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

    // Build the image capture use case and attach button click listener
    val imageCapture = ImageCapture(imageCaptureConfig)

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()
        // Build the viewfinder use case
        val preview = Preview(previewConfig)
        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }
        findViewById<ImageButton>(R.id.testButton).setOnClickListener {
            captureImage();
        }

        testButton.isEnabled = true
        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    fun captureImage() {
        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        Log.e("savePath", file.path)
        savedPicturesPathList.add(file.path)
        //val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(file, executor,
            object : ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    exc: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    Log.e("CameraXApp", msg, exc)
                    viewFinder.post {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.path}"
                    Log.d("CameraXApp", msg)
                    viewFinder.post {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun deleteLastImage() {
        if (!savedPicturesPathList.isEmpty()) {
            val pictureToDelete = File(savedPicturesPathList.last())
            Log.e("picToDelete", pictureToDelete.path)
            if (pictureToDelete.exists()) {
                savedPicturesPathList.remove(pictureToDelete.path)
                pictureToDelete.delete()
                Toast.makeText(baseContext, "Picture deleted", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            Toast.makeText(baseContext, "Picture folder is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendToArduinoCommand(input: String) {
        //TODO check if BT connection exists. If not, cut the rotation of the platform in auto mode

        if (BluetoothService.bluetoothSocket != null) {
            try {
                Log.e("sendcommand", input)
                BluetoothService.bluetoothSocket?.outputStream?.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    }

    private fun handleIncomingDataFromArduino(data: String) {
        if (data == "C") {
            //Log.e("handleDataEqualC", "Data is equal to C")
            captureImage()
        }
        if (data == "D") {
            //Log.e("handleDataEqualD", "Data is equal to D")
            deleteLastImage()
        }
    }

    private fun beginListenForData(handler: Handler) {
        val inputStreamFromArduino = BluetoothService.bluetoothSocket?.inputStream
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        //val delimiter: Byte = 13 //This is the ASCII code for a carriage return
        var readBufferPosition: Int = 0
        val readBuffer: ByteArray? = ByteArray(1024)
        var stopWorker = false

        val workerThread = Thread(Runnable {
            /*!Thread.currentThread().isInterrupted && !stopWorker
            * ide ez kellene de valamiert atallitodik es nem lep bele*/
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
                                    val data = String(encodedBytes).replace("\r", "")
                                    readBufferPosition = 0
                                    handler.post(Runnable { testTextView.text = data })
                                    Log.e("data", data)
                                    handler.post({handleIncomingDataFromArduino(data)})
                                    //handleIncomingDataFromArduino(data)
                                } else if (readBuffer != null) {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    }
                } catch (ex: IOException) {
                    Log.e("exceptionAtThread", ex.message)
                    stopWorker = true
                }
            }
        })
        if (workerThread != null) {
            workerThread!!.start()
        }
    }
}