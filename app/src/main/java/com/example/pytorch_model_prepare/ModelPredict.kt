package com.example.pytorch_model_prepare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

class ModelPredict {

    private val module: Module
    private val context: Context

    constructor(context: Context) {
        this.context = context
        module = LiteModuleLoader.load(assetFilePath(context, "model_fixes_quantized.ptl"))
    }

    fun predict(bitmap:Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val normalizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val imgData = convertBitmapToFloatArray(normalizedBitmap)

        val inputTensor = Tensor.fromBlob(imgData, longArrayOf(1, 3, 256, 256))

//         run the model
        val outputTensor = module.forward(IValue.from(inputTensor)).toTuple()
        val out_3d = outputTensor[0].toTensor()
        val out_2d = outputTensor[1].toTensor()

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


        } catch (e: Exception) {
            Log.d("output", e.toString())
        }
        val data: ByteArray = Base64.getDecoder().decode(str)
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        return bmp
    }

    fun getBitmapFromAsset(context: Context, filePath: String): Bitmap {
        val assetManager = context.assets
        val istr: InputStream = assetManager.open(filePath)
        val bitmap: Bitmap = BitmapFactory.decodeStream(istr)
        return bitmap
    }

    private fun assetFilePath(context: Context, s: String): String? {
        val file = File(context.filesDir, s)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            context.assets.open(s).use { `is` ->
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

    private fun convertBitmapToFloatArray(bitmap: Any): FloatArray {
        val intValues = IntArray(256 * 256)
        val floatValues = FloatArray(256 * 256 * 3)
        val bitmap = bitmap as Bitmap
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
}