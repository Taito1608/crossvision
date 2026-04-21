package com.example.solution_development.test

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.solution_development.ocr.OnnxOcrEngine
import com.example.solution_development.R

class OnnxTestActivity : AppCompatActivity() {
    
    private lateinit var engine: OnnxOcrEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onnx_test)
        
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        
        // ONNX Runtime Engine initialization
        engine = OnnxOcrEngine(this)
        
        // Load model button
        val loadBtn = findViewById<Button>(R.id.loadBtn)
        loadBtn.setOnClickListener {
            statusText.text = "Loading model..."
            Thread {
                val success = engine.loadModel()
                runOnUiThread {
                    statusText.text = if (success) "Model loaded successfully" else "Failed to load model"
                }
            }.start()
        }
        
        // Run inference button
        val runBtn = findViewById<Button>(R.id.runBtn)
        runBtn.setOnClickListener {
            runInferenceTest()
        }
        
        statusText.text = "Tap 'Load Model' to initialize ONNX Runtime"
    }
    
    private fun runInferenceTest() {
        if (!::engine.isInitialized) {
            statusText.text = "Engine not initialized"
            return
        }
        
        statusText.text = "Creating dummy bitmap..."
        
        // Create dummy bitmap for testing
        Thread {
            val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(128, 128, 128))
            
            val startTime = System.currentTimeMillis()
            val productCodes = engine.extractProductCode(bitmap)
            val endTime = System.currentTimeMillis()
            
            runOnUiThread {
                if (productCodes.isNotEmpty()) {
                    resultText.text = """
                        Inference completed in ${endTime - startTime}ms
                        Product codes found: ${productCodes.joinToString(", ")}
                    """.trimIndent()
                    statusText.text = "Success!"
                } else {
                    resultText.text = "No product codes found (may need real model)"
                    statusText.text = "Inference completed (no results)"
                }
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
