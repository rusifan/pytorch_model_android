package com.example.pytorch_model_prepare

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.pytorch.Tensor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var img: Bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
        val resizedBitmap = Bitmap.createScaledBitmap(img, 256, 256, true)
        val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        convert the image to a float array
        var imgData: FloatArray = convertBitmapToFloatArray(normalizedBitmap)
//        val imageTensor = preprocessImage(resizedBitmap)
//        val inputTensor = Tensor.fromBlob(resizedBitmap, longArrayOf(1, resizedBitmap))
        Log.d("output", "image loaded")
//        load the pytorch model
            var module = LiteModuleLoader.load(assetFilePath("model.ptl"))
        Log.d("output", "model loaded")

//        convert the image to tensor of shape [1, 3, 256, 256]

        var inputTensor = Tensor.fromBlob(imgData, longArrayOf(1, 3, 256, 256))
        Log.d("output", "tensor loaded")
        Log.d("output", inputTensor.dtype().toString())
//        try {
//            val output = module.forward(IValue.from(inputTensor)).toTuple()
//        }catch (e: Exception){
//            Log.d("output", e.toString())
//        }

        val outputTensor = module.forward(IValue.from(inputTensor)).toTuple()
        Log.d("output", "output loaded")
//        val outputTuple = arrayOf(outputTensor)
        val out_3d = outputTensor[0].toTensor()
        val out_2d = outputTensor[1].toTensor()
        Log.d("output", out_3d.toString())

    }

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
            Log.e("pytorchandroid", "Error process asset $s to file path")
        }
        return null
    }
}