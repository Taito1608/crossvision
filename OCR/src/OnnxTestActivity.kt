package com.example.solution_development.test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.solution_development.ocr.OnnxOcrEngine
import com.example.solution_development.R

class OnnxTestActivity : AppCompatActivity() {

    private lateinit var engine: OnnxOcrEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var testBitmap: Bitmap

    // Evaluation image filenames (without extension, in assets/onnx/images/)
    private val evalImages = listOf(
        "IMG_0787.jpg",
        "IMG20250416160821.jpg",
        "M5sb24-10.jpg",
        "multi_001_sunny_yellow.JPG",
        "IMG_0422.jpg",
        "IMG_1548.jpg",
        "IMG20250416153020.jpg",
        "IMG_0771.jpg",
        "IMG_1537.jpg",
        "N5G15-X1Y1.jpg",
        "IMG_1571.jpg",
        "IMG_1536.jpg",
        "015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG",
        "multi_008_yellow_green.JPG"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onnx_test)

        Log.d("OnnxTest", "Activity created")

        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)

        // Default test image
        testBitmap = loadAssetBitmap("onnx/images/test_label.jpg")
            ?: run {
                Log.e("OnnxTest", "Failed to load test image from assets")
                finish()
                return
            }
        Log.d("OnnxTest", "Test image loaded: ${testBitmap.width}x${testBitmap.height}")

        // ONNX Runtime Engine initialization
        engine = OnnxOcrEngine(this)
        engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCRV3_OFFICIAL)

        // Load model button
        findViewById<Button>(R.id.loadBtn).setOnClickListener {
            Log.d("OnnxTest", "Load Model button clicked")
            statusText.text = "Loading model..."
            Thread {
                val success = engine.loadModel()
                Log.d("OnnxTest", "Load Model result: $success")
                runOnUiThread {
                    statusText.text = if (success) "Model loaded ✓" else "Load failed ✗"
                }
            }.start()
        }

        // Single inference button
        findViewById<Button>(R.id.runBtn).setOnClickListener {
            Log.d("OnnxTest", "Run Inference button clicked")
            statusText.text = "Running..."
            Thread {
                try {
                    val codes = engine.extractProductCode(testBitmap)
                    Log.d("OnnxTest", "Inference result: $codes")
                    runOnUiThread {
                        resultText.text = "Single test: $codes"
                        statusText.text = "Done"
                    }
                } catch (e: Exception) {
                    Log.e("OnnxTest", "Inference error", e)
                    runOnUiThread {
                        resultText.text = "Error: ${e.message}"
                        statusText.text = "Error!"
                    }
                }
            }.start()
        }

        // Batch test button — run all 14 evaluation images
        findViewById<Button>(R.id.batchBtn).setOnClickListener {
            Log.d("OnnxTest", "Batch Test button clicked")
            statusText.text = "Batch testing..."
            Thread {
                try {
                    val sb = StringBuilder()
                    sb.appendLine("=== Batch OCR Test (${evalImages.size} images) ===")
                    sb.appendLine()

                    var successCount = 0
                    var failCount = 0

                    for ((idx, filename) in evalImages.withIndex()) {
                        val assetPath = "onnx/images/$filename"
                        val bitmap = loadAssetBitmap(assetPath)
                        if (bitmap == null) {
                            sb.appendLine("[${idx + 1}/${evalImages.size}] $filename — SKIP (load failed)")
                            failCount++
                            continue
                        }

                        try {
                            val codes = engine.extractProductCode(bitmap)
                            val codesStr = if (codes.isEmpty()) "(none)" else codes.joinToString(", ")
                            sb.appendLine("[${idx + 1}/${evalImages.size}] $filename")
                            sb.appendLine("  → $codesStr")
                            successCount++
                        } catch (e: Exception) {
                            sb.appendLine("[${idx + 1}/${evalImages.size}] $filename — ERROR: ${e.message}")
                            failCount++
                        } finally {
                            bitmap.recycle()
                        }
                    }

                    sb.appendLine()
                    sb.appendLine("=== Summary ===")
                    sb.appendLine("Success: $successCount / ${evalImages.size}")
                    sb.appendLine("Failed:  $failCount / ${evalImages.size}")

                    Log.d("OnnxTest", sb.toString())
                    runOnUiThread {
                        resultText.text = sb.toString()
                        statusText.text = "Batch done ✓ ($successCount/${evalImages.size})"
                    }
                } catch (e: Exception) {
                    Log.e("OnnxTest", "Batch error", e)
                    runOnUiThread {
                        resultText.text = "Batch Error: ${e.message}"
                        statusText.text = "Error!"
                    }
                }
            }.start()
        }

        statusText.text = "Tap 'Load Model' to initialize ONNX Runtime"
    }

    private fun loadAssetBitmap(assetPath: String): Bitmap? {
        return try {
            assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("OnnxTest", "Failed to load asset bitmap: $assetPath", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
