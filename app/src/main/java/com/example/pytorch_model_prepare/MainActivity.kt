package com.example.pytorch_model_prepare

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.pytorch_model_prepare.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    lateinit var model: ModelPredict
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        model = ModelPredict(this)

        if (allPermissionsGranted()) {
            Log.d("output", "all permissions granted")
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // working code from here

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private lateinit var viewBinding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // image analysis

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(256, 256))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var t1 = System.currentTimeMillis()
            val analyzer = ImageAnalysis.Analyzer { imageProxy ->
                val bitmap = BitmapUtils.getBitmap(imageProxy)
                if (bitmap != null) {
                    runOnUiThread {
                        viewBinding.imageView.setImageBitmap(model.predict(bitmap))
                        val t2 = System.currentTimeMillis()
                        val ms = (t2 - t1).toFloat() / 1000.0
                        viewBinding.fpsTextView.text = "$ms s"
                        t1 = System.currentTimeMillis()
                    }
                }
                imageProxy.close()

            }
            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                Log.d("output", "life cycle bound")


            } catch (exc: Exception) {
                Log.d("hello", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}