package com.example.solution_development

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solution_development.ocr.OnnxOcrEngine
import java.io.File

/**
 * Verification tool for PP-OCRv3 recognition.
 *
 * Loads real-capture test images (scan_crop_*) from assets/test-images/ and
 * runs recognition directly on each full image. No detection/post-process/crop
 * needed because the images are already cropped (user took a close-up photo).
 *
 * Logcat filter:
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
    private lateinit var tvRecognitionResult: TextView

    // Test images (scan_crop_* from real device + original test images)
    private val testImages = listOf(
        "scan_crop_1778821975286.jpg",
        "scan_crop_1778826667328.jpg",
        "scan_crop_1778826676179.jpg",
        "scan_crop_1778826683985.jpg",
        "scan_crop_1778826712572.jpg",
        "scan_crop_1778826723948.jpg",
        "scan_crop_1778826731216.jpg",
        "scan_crop_1778826745518.jpg",
        "scan_crop_1778826749319.jpg",
        "scan_crop_1778826753677.jpg",
        "scan_crop_1778826762276.jpg",
        "scan_crop_1778826766632.jpg",
        "scan_crop_1778826776148.jpg",
        "scan_crop_1778826788419.jpg",
        "scan_crop_1778826793476.jpg",
        "scan_crop_1778826801021.jpg",
        "scan_crop_1778826812497.jpg",
        "scan_crop_1778826823373.jpg",
        "scan_crop_1778826828561.jpg",
        "scan_crop_1778826832742.jpg",
        "scan_crop_1778826839878.jpg",
        "M4sb29-2.jpg"
    )
    private var currentImageIndex = 0
    private var currentBitmap: Bitmap? = null
    private var modelLoaded = false

    // Accumulated log
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "PP-OCRv3 Recognition Verification"

        engine = OnnxOcrEngine(this)
        engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCR_V3_EN)

        // Bind UI
        ivPreview = findViewById(R.id.ivPreview)
        tvImageName = findViewById(R.id.tvImageName)
        tvImageInfo = findViewById(R.id.tvImageInfo)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        tvStage0 = findViewById(R.id.tvStage0Result)
        tvRecognitionResult = findViewById(R.id.tvRecognitionResult)

        findViewById<Button>(R.id.btnPrevImage).setOnClickListener { navigateImage(-1) }
        findViewById<Button>(R.id.btnNextImage).setOnClickListener { navigateImage(1) }
        findViewById<Button>(R.id.btnRecognize).setOnClickListener { runRecognition() }

        appendLog("=== PP-OCRv3 Recognition Verification Started ===")
        appendLog("Model preset: ${OnnxOcrEngine.ModelPreset.PP_OCR_V3_EN.name}")
        appendLog("Test images: ${testImages.size}")
        appendLog("Engine: rec loaded=${engine.isRecLoaded()}, det loaded=${engine.isDetLoaded()}")

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
                    appendLog("MODEL LOAD FAILED — check OnnxOcrEngine logs")
                    Toast.makeText(this, "Model load failed", Toast.LENGTH_LONG).show()
                } else {
                    appendLog("Model loaded successfully")
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
            scrollLog.post { scrollLog.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun navigateImage(delta: Int) {
        val newIndex = currentImageIndex + delta
        if (newIndex in testImages.indices) {
            currentImageIndex = newIndex
            loadImage(currentImageIndex)
            tvRecognitionResult.text = "(not run)"
        }
    }

    private fun loadImage(index: Int) {
        val filename = testImages[index]
        try {
            // Copy asset to temp file for EXIF reading
            val tempFile = File(cacheDir, "exif_$filename")
            tempFile.outputStream().use { out ->
                assets.open("test-images/$filename").use { inp ->
                    inp.copyTo(out)
                }
            }

            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            val corrected = applyOrientation(tempFile.absolutePath, bitmap)
            tempFile.delete()

            currentBitmap = corrected
            ivPreview.setImageBitmap(corrected)

            tvImageName.text = "$filename (${index + 1}/${testImages.size})"
            tvImageInfo.text = """
                |Format : ${corrected.config}
                |Size   : ${corrected.width} x ${corrected.height} px
                |Pixels : ${corrected.width * corrected.height}
            """.trimMargin()

            val memInfo = "bitmap=${corrected.width}x${corrected.height}"
            tvStage0.text = "Loaded: $memInfo"
            appendLog("Image loaded: $filename, $memInfo")
        } catch (e: Exception) {
            tvImageName.text = filename
            tvImageInfo.text = "Load failed: ${e.message}"
            tvStage0.text = "Error: ${e.message}"
            currentBitmap = null
            appendLog("Image load failed: $filename — ${e.message}")
            Log.e(TAG, "Failed to load test image: $filename", e)
        }
    }

    /**
     * Apply EXIF orientation, then rotate portrait images to landscape
     * so product code text is horizontal for recognition.
     */
    private fun applyOrientation(filePath: String, bitmap: Bitmap): Bitmap {
        var result = bitmap

        // Step 1: EXIF rotation
        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
                if (rotated != result) result.recycle()
                result = rotated
                appendLog("EXIF rotation applied: $orientation")
            }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF read failed, skipping: ${e.message}")
        }

        // Step 2: portrait → landscape (-90°) so text is horizontal
        if (result.height > result.width) {
            val m = Matrix().apply { postRotate(-90f) }
            val rotated = Bitmap.createBitmap(result, 0, 0, result.width, result.height, m, true)
            if (rotated != result) result.recycle()
            result = rotated
            appendLog("Rotated portrait → landscape")
        }

        return result
    }

    // ─── Recognition (direct, no detection) ─────────────────────

    private fun runRecognition() {
        val bitmap = currentBitmap ?: run {
            tvRecognitionResult.text = "No image loaded"
            appendLog("Recognize: no image loaded")
            return
        }
        if (!modelLoaded) {
            tvRecognitionResult.text = "Model not loaded yet"
            appendLog("Recognize: model not loaded")
            return
        }
        tvRecognitionResult.text = "Running recognition..."
        appendLog("=== Recognition ===")
        appendLog("Input: ${bitmap.width}x${bitmap.height}")

        Thread {
            try {
                val t0 = System.currentTimeMillis()
                val text = engine.recognizeRegion(bitmap)
                val elapsed = System.currentTimeMillis() - t0

                val displayText = if (text.isBlank()) "(empty / no text recognized)" else "'$text'"
                val result = "Result: $displayText (${elapsed}ms)"
                appendLog("Recognition result: $displayText (${elapsed}ms)")
                runOnUiThread { tvRecognitionResult.text = result }
            } catch (e: Exception) {
                appendLog("Recognition EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                Log.e(TAG, "Recognition exception", e)
                runOnUiThread { tvRecognitionResult.text = "Exception: ${e.message}" }
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
