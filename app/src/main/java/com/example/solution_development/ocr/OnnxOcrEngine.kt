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

class OnnxOcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OnnxOcrEngine"
        private const val MODEL_NAME = "dummy_model.onnx"
        private const val INPUT_NAME = "input"
        private const val OUTPUT_NAME = "output"
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
        Log.d(TAG, "extractProductCode() called")
        if (!isModelLoaded || session == null || ortEnvironment == null) {
            Log.w(TAG, "Engine not ready")
            return emptyList()
        }
        return try {
            Log.d(TAG, "Creating input tensor")
            val inputTensor = bitmapToInputTensor(bitmap)
            Log.d(TAG, "Input tensor created")
            val inputs = hashMapOf(INPUT_NAME to inputTensor)
            Log.d(TAG, "Running session.run() with input: $INPUT_NAME")
            val results = session?.run(inputs)
            Log.d(TAG, "Session run completed, result is null: ${results == null}")
            if (results == null) {
                Log.w(TAG, "Session.run returned null")
                return emptyList()
            }
            results.use { result ->
                Log.d(TAG, "Result keys: ${result.keys}")
                val outputTensor = result.get(OUTPUT_NAME)
                Log.d(TAG, "Output tensor obtained")
                val outputArray = outputTensor.getValue() as FloatArray
                Log.d(TAG, "Output array size: ${outputArray.size}, first 5: ${outputArray.take(5).toList()}")
                if (outputArray.all { it == 0f }) {
                    Log.d(TAG, "All zeros, returning empty list")
                    emptyList()
                } else {
                    Log.d(TAG, "Non-zero output found, returning dummy code")
                    listOf("49827570")
                }
            }
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in extractProductCode", e)
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in extractProductCode", e)
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
