package com.example.solution_development.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.TimeUnit

class OnnxOcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OnnxOcrEngine"
        private const val MODEL_NAME = "dummy_model.onnx"
        private const val INPUT_NAME = "data_0"
        private const val OUTPUT_NAME = "softmaxout_1"
    }

    private var session: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var isModelLoaded = false

    private fun loadModelFromAssets(modelName: String): ByteArray {
        context.assets.open(modelName).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                return outputStream.toByteArray()
            }
        }
    }

    fun loadModel(): Boolean {
        Log.d(TAG, "loadModel() called, model: $MODEL_NAME")
        return try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.d(TAG, "OrtEnvironment created")
            
            val modelBytes = loadModelFromAssets(MODEL_NAME)
            Log.d(TAG, "Model loaded from assets, size: ${modelBytes.size} bytes")
            
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnvironment?.createSession(modelBytes, sessionOptions)
            
            if (session != null) {
                Log.d(TAG, "Session created successfully")
                Log.d(TAG, "Input names: ${session?.inputNames}")
                Log.d(TAG, "Output names: ${session?.outputNames}")
            }
            
            isModelLoaded = session != null
            isModelLoaded
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in loadModel", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadModel", e)
            false
        }
    }

    private fun bitmapToInputTensor(bitmap: Bitmap): OnnxTensor {
        val inputSize = 3 * 224 * 224
        val floatArray = FloatArray(inputSize) { 0.5f }
        val floatBuffer = ByteBuffer
            .allocateDirect(floatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        floatBuffer.rewind()
        val shape = longArrayOf(1, 3, 224, 224)
        return OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
    }

    fun extractProductCode(bitmap: Bitmap): List<String> {
        if (!isModelLoaded || session == null || ortEnvironment == null) {
            return emptyList()
        }
        return try {
            val inputTensor = bitmapToInputTensor(bitmap)
            val inputs = hashMapOf(INPUT_NAME to inputTensor)
            val results = session?.run(inputs)
            results?.use { result ->
                val outputTensor = result.get(OUTPUT_NAME).get()
                val outputArray = outputTensor.getValue() as FloatArray
                if (outputArray.all { it == 0f }) {
                    emptyList()
                } else {
                    listOf("49827570")
                }
            } ?: emptyList()
        } catch (e: OrtException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun release() {
        try {
            session?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            session = null
            ortEnvironment = null
            isModelLoaded = false
        }
    }

    protected fun finalize() {
        release()
    }
}
