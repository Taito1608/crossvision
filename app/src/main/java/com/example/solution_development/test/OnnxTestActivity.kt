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
import java.io.ByteArrayOutputStream

class OnnxTestActivity : AppCompatActivity() {

    private lateinit var engine: OnnxOcrEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var testBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onnx_test)

        Log.d("OnnxTest", "Activity created")
        Log.d("OnnxTest", "Initializing ONNX engine...")

        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)

        // テスト画像を assets から読み込む（テキスト認識に読みやすい320x80の画像）
        testBitmap = loadAssetBitmap("onnx/images/test_label.jpg")
            ?: run {
                Log.e("OnnxTest", "Failed to load test image from assets")
                finish()
                return
            }
        Log.d("OnnxTest", "Test image loaded: ${testBitmap.width}x${testBitmap.height}")

        // ONNX Runtime Engine initialization - 高精度なRapidOCR PP-OCRv3モデルを使用
        engine = OnnxOcrEngine(this)
        engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCR_V3_RAPIDOCR)
        
        // Load model button
        val loadBtn = findViewById<Button>(R.id.loadBtn)
        loadBtn.setOnClickListener {
            Log.d("OnnxTest", "Load Model button clicked")
            statusText.text = "Loading model..."
            Thread {
                val success = engine.loadModel()
                Log.d("OnnxTest", "Load Model result: $success")
                runOnUiThread {
                    if (success) {
                        statusText.text = "Model loaded ✓"
                    } else {
                        statusText.text = "Load failed ✗"
                    }
                }
            }.start()
        }
        
        // Run inference button
        val runBtn = findViewById<Button>(R.id.runBtn)
        runBtn.setOnClickListener {
            Log.d("OnnxTest", "Run Inference button clicked")
            statusText.text = "Running..."
            Thread {
                try {
                    val codes = engine.extractProductCode(testBitmap)
                    Log.d("OnnxTest", "Inference result: $codes")
                    runOnUiThread {
                        resultText.text = codes.toString()
                        statusText.text = "Success!"
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

        statusText.text = "Tap 'Load Model' to initialize ONNX Runtime"
    }

    /** Load a bitmap from assets WITHOUT scaling - let the engine handle preprocessing */
    private fun loadAssetBitmap(assetPath: String): Bitmap? {
        return try {
            assets.open(assetPath).use { inputStream ->
                // BitmapFactory.decodeStream() automatically applies EXIF orientation.
                // This image has EXIF orientation=8 (Rotate 90 CCW), so Android gives
                // us the correctly oriented 2048x1536 landscape bitmap automatically.
                // DO NOT add manual rotation — that would double-rotate back to portrait.
                BitmapFactory.decodeStream(inputStream)
                    ?: return null
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
