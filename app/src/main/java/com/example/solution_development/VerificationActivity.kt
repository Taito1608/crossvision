package com.example.solution_development

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solution_development.ocr.OnnxOcrEngine

/**
 * Verification tool for PP-OCRv5 pipeline stages.
 *
 * Loads test images from assets/test-images/ and runs each
 * pipeline stage independently, displaying detailed results.
 *
 * Stages:
 *   0. Image Load — verify asset loading and bitmap metadata
 *   1. Detection — run det.onnx inference, show output stats + timing
 *   2. Post-process — DBNet threshold → BBox extraction
 *   3. Crop — extract BBox regions from original image
 *   4. Recognition — run rec.onnx on each cropped region
 *   5. Full Pipeline — det → crop → rec end-to-end
 *
 * Logcat filters:
 *   adb logcat -s VerificationActivity:* OnnxOcrEngine:*
 */
class VerificationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VerificationActivity"
    }

    private lateinit var engine: OnnxOcrEngine

    // UI
    private lateinit var ivPreview: ImageView
    private lateinit var tvImageName: TextView
    private lateinit var tvImageInfo: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView

    private lateinit var tvStage0: TextView
    private lateinit var tvStage1: TextView
    private lateinit var tvStage2: TextView
    private lateinit var tvStage3: TextView
    private lateinit var tvStage4: TextView
    private lateinit var tvStage5: TextView

    // Test images
    private val testImages = listOf(
        "M4sb29-2.jpg",
        "M5sb24-10.jpg",
        "multi_001_sunny_yellow.JPG",
        "multi_008_yellow_green.JPG"
    )
    private var currentImageIndex = 0
    private var currentBitmap: Bitmap? = null
    private var modelLoaded = false

    // Accumulated log for CC to read
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pipeline Stage Verification"

        engine = OnnxOcrEngine(this)
        engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCR_V5_CUSTOM)

        // Bind UI
        ivPreview = findViewById(R.id.ivPreview)
        tvImageName = findViewById(R.id.tvImageName)
        tvImageInfo = findViewById(R.id.tvImageInfo)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        tvStage0 = findViewById(R.id.tvStage0Result)
        tvStage1 = findViewById(R.id.tvStage1Result)
        tvStage2 = findViewById(R.id.tvStage2Result)
        tvStage3 = findViewById(R.id.tvStage3Result)
        tvStage4 = findViewById(R.id.tvStage4Result)
        tvStage5 = findViewById(R.id.tvStage5Result)

        findViewById<Button>(R.id.btnPrevImage).setOnClickListener { navigateImage(-1) }
        findViewById<Button>(R.id.btnNextImage).setOnClickListener { navigateImage(1) }

        findViewById<Button>(R.id.btnStage1).setOnClickListener { runStage1() }
        findViewById<Button>(R.id.btnStage2).setOnClickListener { runStage2() }
        findViewById<Button>(R.id.btnStage3).setOnClickListener { runStage3() }
        findViewById<Button>(R.id.btnStage4).setOnClickListener { runStage4() }
        findViewById<Button>(R.id.btnStage5).setOnClickListener { runStage5() }

        appendLog("=== Pipeline Verification Started ===")
        appendLog("Model preset: ${OnnxOcrEngine.ModelPreset.PP_OCR_V5_CUSTOM.name}")
        appendLog("Test images: ${testImages.size}")

        // Load model in background
        appendLog("Loading model in background thread...")
        Thread {
            val t0 = System.currentTimeMillis()
            val success = engine.loadModel()
            val elapsed = System.currentTimeMillis() - t0
            modelLoaded = success

            Log.i(TAG, "Model load result: $success, time: ${elapsed}ms")
            appendLog("Model load: success=$success, time=${elapsed}ms")

            runOnUiThread {
                if (!success) {
                    appendLog("❌ MODEL LOAD FAILED — check OnnxOcrEngine logs")
                    Toast.makeText(this, "Model load failed", Toast.LENGTH_LONG).show()
                } else {
                    appendLog("✅ Model loaded successfully")
                }
                // Load first image after model is ready
                loadImage(currentImageIndex)
            }
        }.start()
    }

    private fun appendLog(message: String) {
        val line = "[${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}] $message"
        Log.d(TAG, message)
        logBuilder.appendLine(line)
        runOnUiThread {
            tvLog.text = logBuilder.toString()
            // Auto-scroll to bottom
            scrollLog.post { scrollLog.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun navigateImage(delta: Int) {
        val newIndex = currentImageIndex + delta
        if (newIndex in testImages.indices) {
            currentImageIndex = newIndex
            loadImage(currentImageIndex)
            clearResults()
        }
    }

    private fun loadImage(index: Int) {
        val filename = testImages[index]
        try {
            val inputStream = assets.open("test-images/$filename")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            currentBitmap = bitmap
            ivPreview.setImageBitmap(bitmap)

            tvImageName.text = "$filename (${index + 1}/${testImages.size})"
            tvImageInfo.text = """
                |Format : ${bitmap.config}
                |Size   : ${bitmap.width} x ${bitmap.height} px
                |Pixels : ${bitmap.width * bitmap.height}
            """.trimMargin()

            val memInfo = "bitmap=${bitmap.width}x${bitmap.height}, rowBytes=${bitmap.rowBytes}, totalBytes=${bitmap.rowBytes * bitmap.height}"
            tvStage0.text = "✅ Loaded: $memInfo"
            appendLog("Image loaded: $filename, $memInfo")
        } catch (e: Exception) {
            tvImageName.text = filename
            tvImageInfo.text = "❌ Load failed: ${e.message}"
            tvStage0.text = "❌ Error: ${e.message}"
            currentBitmap = null
            appendLog("❌ Image load failed: $filename — ${e.message}")
            Log.e(TAG, "Failed to load test image: $filename", e)
        }
    }

    private fun clearResults() {
        tvStage1.text = "(not run)"
        tvStage2.text = "(not run)"
        tvStage3.text = "(not run)"
        tvStage4.text = "(not run)"
        tvStage5.text = "(not run)"
    }

    // ─── Stage 1: Detection ───────────────────────────────────

    private fun runStage1() {
        val bitmap = currentBitmap ?: run {
            tvStage1.text = "❌ No image loaded"
            appendLog("Stage1: no image loaded")
            return
        }
        if (!modelLoaded) {
            tvStage1.text = "❌ Model not loaded yet"
            appendLog("Stage1: model not loaded")
            return
        }
        tvStage1.text = "⏳ Running det.onnx..."
        appendLog("=== Stage 1: Detection ===")
        appendLog("Input: ${bitmap.width}x${bitmap.height}")

        Thread {
            try {
                val tTotal = System.currentTimeMillis()
                val regions = engine.detectTextRegions(bitmap)
                val totalMs = System.currentTimeMillis() - tTotal

                val sb = StringBuilder()
                sb.appendLine("⏱ Total: ${totalMs}ms")
                sb.appendLine("Regions: ${regions.size}")

                if (regions.isEmpty()) {
                    sb.appendLine("⚠️ No text regions detected")
                    sb.appendLine("→ Check logcat: OnnxOcrEngine:Det output stats")
                    sb.appendLine("→ Check if det output values > DET_DB_THRESH(0.3)")
                } else {
                    for ((i, region) in regions.withIndex()) {
                        sb.appendLine("  #$i: rect=${region.rect}, conf=${"%.4f".format(region.confidence)}")
                    }
                }

                val result = sb.toString().trimEnd()
                appendLog("Stage1 result: total=${totalMs}ms, regions=${regions.size}")
                if (regions.isEmpty()) {
                    appendLog("Stage1: ⚠️ No regions — det model may not be producing valid output")
                }
                runOnUiThread { tvStage1.text = result }

            } catch (e: Exception) {
                appendLog("Stage1 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                Log.e(TAG, "Stage1 exception", e)
                runOnUiThread { tvStage1.text = "❌ Exception: ${e.message}" }
            }
        }.start()
    }

    // ─── Stage 2: Post-process ────────────────────────────────

    private fun runStage2() {
        val bitmap = currentBitmap ?: run {
            tvStage2.text = "❌ No image loaded"
            return
        }
        if (!modelLoaded) {
            tvStage2.text = "❌ Model not loaded yet"
            return
        }
        tvStage2.text = "⏳ Running post-process..."
        appendLog("=== Stage 2: Post-process (BBox extraction) ===")

        Thread {
            try {
                val t0 = System.currentTimeMillis()
                val regions = engine.detectTextRegions(bitmap)
                val elapsed = System.currentTimeMillis() - t0

                val sb = StringBuilder()
                sb.appendLine("⏱ Total: ${elapsed}ms")
                sb.appendLine("Components: ${regions.size}")

                if (regions.isNotEmpty()) {
                    sb.appendLine("--- BBox details ---")
                    for ((i, r) in regions.withIndex()) {
                        val w = r.rect.width()
                        val h = r.rect.height()
                        val area = w.toLong() * h
                        sb.appendLine("  #$i: x=${r.rect.left}, y=${r.rect.top}, ${w}x${h}, area=$area, conf=${"%.4f".format(r.confidence)}")
                    }
                } else {
                    sb.appendLine("No regions. Possible causes:")
                    sb.appendLine("  - det output values < DET_DB_THRESH (0.3)?")
                    sb.appendLine("  - Check logcat: OnnxOcrEngine:Det output stats")
                    sb.appendLine("  - det model input preprocessing mismatch?")
                }

                appendLog("Stage2 result: ${regions.size} regions, ${elapsed}ms")
                runOnUiThread { tvStage2.text = sb.toString().trimEnd() }

            } catch (e: Exception) {
                appendLog("Stage2 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { tvStage2.text = "❌ Exception: ${e.message}" }
            }
        }.start()
    }

    // ─── Stage 3: Crop ────────────────────────────────────────

    private fun runStage3() {
        val bitmap = currentBitmap ?: run {
            tvStage3.text = "❌ No image loaded"
            return
        }
        if (!modelLoaded) {
            tvStage3.text = "❌ Model not loaded yet"
            return
        }
        tvStage3.text = "⏳ Detecting and cropping..."
        appendLog("=== Stage 3: Crop ===")

        Thread {
            try {
                val t0 = System.currentTimeMillis()
                val regions = engine.detectTextRegions(bitmap)

                if (regions.isEmpty()) {
                    appendLog("Stage3: no regions to crop")
                    runOnUiThread { tvStage3.text = "❌ No regions to crop (detection returned empty)" }
                    return@Thread
                }

                val sb = StringBuilder()
                sb.appendLine("Detected ${regions.size} region(s)")
                val crops = mutableListOf<Bitmap>()

                for ((i, region) in regions.withIndex()) {
                    val r = region.rect
                    val safeRect = Rect(
                        maxOf(0, r.left),
                        maxOf(0, r.top),
                        minOf(bitmap.width, r.right),
                        minOf(bitmap.height, r.bottom)
                    )
                    if (safeRect.width() > 0 && safeRect.height() > 0) {
                        val crop = Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
                        crops.add(crop)
                        sb.appendLine("  #$i: ${crop.width}x${crop.height} (from ${safeRect.toShortString()})")
                    } else {
                        sb.appendLine("  #$i: skip (invalid rect: ${safeRect.toShortString()})")
                    }
                }

                sb.appendLine("Total crops: ${crops.size}")
                val elapsed = System.currentTimeMillis() - t0
                sb.appendLine("⏱ Time: ${elapsed}ms")

                appendLog("Stage3 result: ${crops.size} crops, ${elapsed}ms")
                runOnUiThread { tvStage3.text = sb.toString().trimEnd() }

            } catch (e: Exception) {
                appendLog("Stage3 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { tvStage3.text = "❌ Exception: ${e.message}" }
            }
        }.start()
    }

    // ─── Stage 4: Recognition ─────────────────────────────────

    private fun runStage4() {
        val bitmap = currentBitmap ?: run {
            tvStage4.text = "❌ No image loaded"
            return
        }
        if (!modelLoaded) {
            tvStage4.text = "❌ Model not loaded yet"
            return
        }
        tvStage4.text = "⏳ Detecting regions then recognizing..."
        appendLog("=== Stage 4: Recognition ===")

        Thread {
            try {
                val tTotal = System.currentTimeMillis()
                val regions = engine.detectTextRegions(bitmap)

                if (regions.isEmpty()) {
                    appendLog("Stage4: no regions detected")
                    runOnUiThread { tvStage4.text = "❌ No regions — try Stage 1 first" }
                    return@Thread
                }

                val sb = StringBuilder()
                sb.appendLine("Recognizing ${regions.size} region(s)")

                for ((i, region) in regions.withIndex()) {
                    val r = region.rect
                    val safeRect = Rect(
                        maxOf(0, r.left),
                        maxOf(0, r.top),
                        minOf(bitmap.width, r.right),
                        minOf(bitmap.height, r.bottom)
                    )
                    if (safeRect.width() <= 0 || safeRect.height() <= 0) {
                        sb.appendLine("  #$i: skip (invalid rect)")
                        continue
                    }

                    val crop = Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
                    val tRec = System.currentTimeMillis()
                    val text = engine.recognizeRegion(crop)
                    val recTime = System.currentTimeMillis() - tRec
                    crop.recycle()

                    val displayText = if (text.isBlank()) "(empty)" else text
                    sb.appendLine("  #$i: '$displayText' (${recTime}ms)")
                    appendLog("Stage4 region $i: '$displayText' (${recTime}ms)")
                }

                val totalMs = System.currentTimeMillis() - tTotal
                sb.appendLine("⏱ Total: ${totalMs}ms")
                appendLog("Stage4 result: ${regions.size} regions, ${totalMs}ms total")
                runOnUiThread { tvStage4.text = sb.toString().trimEnd() }

            } catch (e: Exception) {
                appendLog("Stage4 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { tvStage4.text = "❌ Exception: ${e.message}" }
            }
        }.start()
    }

    // ─── Stage 5: Full Pipeline ───────────────────────────────

    private fun runStage5() {
        val bitmap = currentBitmap ?: run {
            tvStage5.text = "❌ No image loaded"
            return
        }
        if (!modelLoaded) {
            tvStage5.text = "❌ Model not loaded yet"
            return
        }
        tvStage5.text = "⏳ Running full pipeline (det → crop → rec)..."
        appendLog("=== Stage 5: Full Pipeline ===")

        Thread {
            try {
                val t0 = System.currentTimeMillis()
                val (codes, regions) = engine.extractProductCode(bitmap)
                val elapsed = System.currentTimeMillis() - t0

                val sb = StringBuilder()
                sb.appendLine("⏱ Total: ${elapsed}ms")
                sb.appendLine("Regions detected: ${regions.size}")

                if (regions.isNotEmpty()) {
                    sb.appendLine("Codes found: ${codes.size}")
                    for (code in codes) {
                        sb.appendLine("  → '$code'")
                    }
                }

                if (codes.isEmpty() && regions.isEmpty()) {
                    sb.appendLine("⚠️ Nothing detected. Check logcat for details.")
                } else if (codes.isEmpty() && regions.isNotEmpty()) {
                    sb.appendLine("⚠️ Regions found but no codes extracted.")
                    sb.appendLine("  Run Stage 4 to see raw recognition output.")
                }

                appendLog("Stage5 result: ${regions.size} regions, ${codes.size} codes, ${elapsed}ms")
                for (code in codes) {
                    appendLog("Stage5 code: '$code'")
                }
                runOnUiThread { tvStage5.text = sb.toString().trimEnd() }

            } catch (e: Exception) {
                appendLog("Stage5 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { tvStage5.text = "❌ Exception: ${e.message}" }
            }
        }.start()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
