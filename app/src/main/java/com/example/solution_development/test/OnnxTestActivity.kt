package com.example.solution_development.test

import android.graphics.Bitmap
import android.graphics.Color
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
    private lateinit var dummyBitmap: Bitmap
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onnx_test)
        
        Log.d("OnnxTest", "Activity created")
        Log.d("OnnxTest", "Initializing ONNX engine...")
        
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        
        // ダミー画像生成（224x224 白色）
        dummyBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        
        // ONNX Runtime Engine initialization
        engine = OnnxOcrEngine(this)
        
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
                    val codes = engine.extractProductCode(dummyBitmap)
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
    
    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
