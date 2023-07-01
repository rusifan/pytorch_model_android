package com.example.pytorch_model_prepare

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.pytorch.Tensor
import java.util.*
import java.util.concurrent.ExecutorService
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pytorch_model_prepare.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            Log.d("output", "all permissions granted")
            startCamera()

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
//        fun requestPermissions() {
//            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
//        }
//        this is the python part of the code

        // working code from here

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

//        var img: Bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
//        val resizedBitmap = Bitmap.createScaledBitmap(img, 256, 256, true)
//        val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
////        convert the image to a float array
//        var imgData: FloatArray = convertBitmapToFloatArray(normalizedBitmap)
////        val imageTensor = preprocessImage(resizedBitmap)
////        val inputTensor = Tensor.fromBlob(resizedBitmap, longArrayOf(1, resizedBitmap))
//        Log.d("output", "image loaded")
////        load the pytorch model
//            var module = LiteModuleLoader.load(assetFilePath("model.ptl"))
//        Log.d("output", "model loaded")
//
////        convert the image to tensor of shape [1, 3, 256, 256]
//
//        var inputTensor = Tensor.fromBlob(imgData, longArrayOf(1, 3, 256, 256))
//        Log.d("output", "tensor loaded")
//        Log.d("output", inputTensor.dtype().toString())
//
//        val outputTensor = module.forward(IValue.from(inputTensor)).toTuple()
//        Log.d("output", "output loaded")
////        val outputTuple = arrayOf(outputTensor)
//        val out_3d = outputTensor[0].toTensor()
//        val out_2d = outputTensor[1].toTensor()
//        Log.d("output", out_3d.toString())
////        convert the tensor to array
//        val out_3d_array = out_3d.dataAsFloatArray
////        Log.d("output", out_3d_array.toString())
////        val out_2d_array = out_2d.getDataAsFloatArray()
////        Log.d("output", out_3d_array.size.toString())
////        Log.d("output", out_2d_array.toString())
//        Log.d("output", "array loaded")
//
//// add chacopy and python script to the project for the next step
//        val py = Python.getInstance()
//        val obj: PyObject = py.getModule("plot")
////        convert out_3d tensor to numpy array keeping the shape
//
//        val obj1: PyObject = obj.callAttr("main", out_3d_array, out_2d)
//        Log.d("output", "python script loaded")
//        val str = obj1.toString()
////        Log.d("output", str)
//        val data: ByteArray = Base64.getDecoder().decode(str)
//        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
//        val image = findViewById<ImageView>(R.id.imageView)
//        image.setImageBitmap(bmp)


        // ???????????????? above works
//        val obj1: PyObject = obj.callAttr("main", out_3d, out_2d)
//        Log.d("output", "python script loaded")
//        val str = obj1.toString()
//        Log.d("output", str)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    private fun convertBitmapToFloatArray(bitmap: Any): FloatArray {
        var intValues = IntArray(256 * 256)
        var floatValues = FloatArray(256 * 256 * 3)
        var bitmap = bitmap as Bitmap
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0..255) {
            for (j in 0..255) {
                val pixelValue = intValues[i * 256 + j]
                floatValues[i * 256 + j] = ((pixelValue shr 16 and 0xFF) - 0) / 255.0f
                floatValues[256 * 256 + i * 256 + j] = ((pixelValue shr 8 and 0xFF) - 0) / 255.0f
                floatValues[2 * 256 * 256 + i * 256 + j] = ((pixelValue and 0xFF) - 0) / 255.0f
            }
        }
        return floatValues

    }

    private fun assetFilePath(s: String): String? {
        val file = File(filesDir, s)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            applicationContext.assets.open(s).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (`is`.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e("pyto", "Error process asset $s to file path")
        }
        return null
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera() {
        Log.d("output", "camera started")
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        }catch (e: Exception){
            Log.d("output", e.toString())
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
//                .setTargetResolution( Size(640, 480))
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // image analysis

            val  imageAnalyzer= ImageAnalysis.Builder()
                 .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(Size(256, 256))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val analyzer = ImageAnalysis.Analyzer { image_input ->

                // load the model
                var module = LiteModuleLoader.load(assetFilePath("model.ptl"))
                Log.d("output", "model loaded")
//                 convert image to bitmap
//                try {
////                    print the size of the image
//                    Log.d("output", image_input.width.toString())
//                    Log.d("output", image_input.height.toString())
//                    val planes = image_input.planes
//                    Log.d("output", planes.size.toString())
////                    double the capacity of the buffer
//                    val buffer = planes[0].buffer
//
////                    log the size of the buffer
//                    Log.d("output", buffer.capacity().toString())
////                    make the buffer large enough for the input image
//
//                    val pixelStride = planes[0].pixelStride
//                    val rowStride = planes[0].rowStride
//                    val width = image_input.width
//                    val height = image_input.height
//
//                    val bitmap = Bitmap.createBitmap(
//                        width + (pixelStride - 1) * (width / pixelStride),
//                        height,
//                        Bitmap.Config.ALPHA_8
//                    )
//                    buffer.rewind()
//                    bitmap.copyPixelsFromBuffer(buffer)
//                    //center crop the image to 400x400
//                    val croppedBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
//
//                    val xOffset = (bitmap.width - 400) / 2
//                    val yOffset = (bitmap.height - 400) / 2
//
//                    val canvas = Canvas(croppedBitmap)
//                    canvas.drawBitmap(bitmap, Rect(xOffset, yOffset, xOffset + 400, yOffset + 400), Rect(0, 0, 400, 400), null)
////                 resize the image to 256x256
//                    val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 256, 256, true)
//                    val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
//                }
//                catch (e: Exception){
//                    Log.d("output", e.toString())
//                }
                val planes = image_input.planes
                val buffer = planes[0].buffer
                val bytes = ByteArray(buffer.capacity())

                buffer.get(bytes)
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val width = image_input.width
                val height = image_input.height

                val bitmap = Bitmap.createBitmap(
                    width ,
                    height,
                    Bitmap.Config.ARGB_8888
//                            Bitmap.Config.ALPHA_8

                )
//                get bitmap from preview view

//                var bitmap2 = previewView.getbitmap()

                buffer.rewind()
//                bitmap.copyPixelsFromBuffer(buffer)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
                image_input.close()

                //center crop the image to 400x400
                val croppedBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)

                val xOffset = (bitmap.width - 400) / 2
                val yOffset = (bitmap.height - 400) / 2

                val canvas = Canvas(croppedBitmap)
                canvas.drawBitmap(bitmap, Rect(xOffset, yOffset, xOffset + 400, yOffset + 400), Rect(0, 0, 400, 400), null)
//                 resize the image to 256x256
//                log the  size of the bitmap
                Log.d("output", croppedBitmap.width.toString())
                Log.d("output", croppedBitmap.height.toString())
                val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 256, 256, true)
                val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        convert the image to a float array
                var imgData: FloatArray = convertBitmapToFloatArray(normalizedBitmap)

// testing som ecode here #########################################################################
//                var img: Bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
//                val resizedBitmap = Bitmap.createScaledBitmap(img, 256, 256, true)
//                val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
////        convert the image to a float array
//                var imgData: FloatArray = convertBitmapToFloatArray(normalizedBitmap)
        Log.d("output", "image loaded")
//        convert the image to tensor of shape [1, 3, 256, 256]

                var inputTensor = Tensor.fromBlob(imgData, longArrayOf(1, 3, 256, 256))

                // run the model
//                var outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
                val outputTensor = module.forward(IValue.from(inputTensor)).toTuple()

//                val outputTuple = arrayOf(outputTensor)
//                try {
//                    val out_3d = outputTensor[0].toTensor()
//                    val out_2d = outputTensor[1].toTensor()
//                    Log.d("output", "works")
//                }catch (e: Exception){
//                    Log.d("output", e.toString())
//                }
                val out_3d = outputTensor[0].toTensor()
                val out_2d = outputTensor[1].toTensor()
                Log.d("output", out_3d.toString())
//        convert the tensor to array
                val out_3d_array = out_3d.dataAsFloatArray




                val py = Python.getInstance()
                val obj: PyObject = py.getModule("plot")
//        convert out_3d tensor to numpy array keeping the shape

                val obj1: PyObject = obj.callAttr("main", out_3d_array, out_2d)
                Log.d("output", "python script loaded")
                val str = obj1.toString()
//        Log.d("output", str)
                try {
                    val data: ByteArray = Base64.getDecoder().decode(str)
                    val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val image_to_view = findViewById<ImageView>(R.id.imageView)
                    image_to_view.setImageBitmap(bmp)

                }
                catch (e: Exception){
                    Log.d("output", e.toString())
                }
                val data: ByteArray = Base64.getDecoder().decode(str)
                val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
                val image_to_view = findViewById<ImageView>(R.id.imageView)

//                try {
//                    image_to_view.setImageBitmap(bmp)
//                    image_input.close()
//                    Log.d("output", "no problem here")
//                }
//                catch (e: Exception){
//                    Log.d("output", e.toString())
//                }
                image_to_view.setImageBitmap(bmp)
//                image_input.close()
            }
            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)



            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
                        Log.d("output", "life cycle bound")


            } catch(exc: Exception) {
                Log.d("hello", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}