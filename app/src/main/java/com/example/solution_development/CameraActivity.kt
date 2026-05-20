package com.example.solution_development

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.os.Looper
import android.view.MenuItem
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.util.AttributeSet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import androidx.graphics.*
import android.graphics.Rect
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt


class CameraActivity : AppCompatActivity() {

    // ===== UI =====
    private lateinit var previewView: PreviewView
    private lateinit var overlay: ScanOverlayView
    private lateinit var btnSelectImage: Button

    // ===== CameraX =====
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    // ===== OCR =====
    private lateinit var engine: OnnxOcrEngine
    private var engineReady = false

    // ===== State =====
    private var isCaptured = false
    private val scannedList = mutableListOf<String>()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // ===== Gallery =====
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processGalleryImage(it) }
        }

    // ===== Processing =====
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "スキャン画面"

            previewView = findViewById(R.id.previewView)
            overlay = findViewById(R.id.overlay)
            btnSelectImage = findViewById(R.id.btnSelectImage)

            // Ensure PreviewView can receive key events
            previewView.isFocusable = true
            previewView.isFocusableInTouchMode = true
            previewView.requestFocus()
            window?.decorView?.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
            }

            val btnComplete = findViewById<Button>(R.id.btnComplete)

            btnComplete.setOnClickListener {
                val intent = Intent(this, ConfirmationActivity::class.java)
                intent.putStringArrayListExtra("scannedList", ArrayList(scannedList))
                startActivity(intent)
            }

            btnSelectImage.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            // ONNXエンジン初期化
            engine = OnnxOcrEngine(applicationContext)
            engineReady = engine.loadModel()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    1001
                )
            }
        } catch (e: Exception) {
            Log.e("CameraActivity", "onCreate error", e)
            Toast.makeText(this, "初期化中にエラーが発生しました", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                        imageCapture
                    )

                    overlay.post {
                        startAnalyzer()
                    }
                } catch (e: Exception) {
                    Log.e("CameraActivity", "Camera binding error", e)
                    Toast.makeText(this@CameraActivity, "カメラバインドエラー: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("CameraActivity", "startCamera error", e)
            Toast.makeText(this, "カメラ初期化エラー", Toast.LENGTH_LONG).show()
        }
    }

    /** ImageAnalyzer — ONNX OCRエンジンで認識 */
    private fun startAnalyzer() {
        if (!engineReady) {
            Log.w("CameraActivity", "エンジン未初期化、スキャンスキップ")
            return
        }

        try {
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this)
            ) { imageProxy ->

                if (isProcessing || isCaptured) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                isProcessing = true

                val mediaImage = imageProxy.image

                if (mediaImage != null) {
                    val bitmap = imageProxy.toBitmap()
                    val rect = overlay.getScanRect()

                    if (rect.width() <= 0 || rect.height() <= 0) {
                        imageProxy.close()
                        isProcessing = false
                        return@setAnalyzer
                    }

                    val cropLeft = rect.left.coerceIn(0, bitmap.width)
                    val cropTop = rect.top.coerceIn(0, bitmap.height)
                    val cropWidth = (rect.right - rect.left).coerceAtLeast(1).coerceAtMost(bitmap.width - cropLeft)
                    val cropHeight = (rect.bottom - rect.top).coerceAtLeast(1).coerceAtMost(bitmap.height - cropTop)

                    if (cropWidth <= 0 || cropHeight <= 0) {
                        imageProxy.close()
                        isProcessing = false
                        return@setAnalyzer
                    }

                    val cropped = Bitmap.createBitmap(
                        bitmap,
                        cropLeft,
                        cropTop,
                        cropWidth,
                        cropHeight
                    )

                    try {
                        // ONNX OCR実行
                        val results = engine.extractProductCode(cropped)

                        if (results.isNotEmpty()) {
                            val text = results[0]
                            // 数値のみ抽出（既存の挙動を維持）
                            val numbers = text.replace(Regex("[^0-9]"), "")

                            if (numbers.length >= 6) {
                                if (!scannedList.contains(numbers)) {
                                    scannedList.add(numbers)
                                }
                                triggerCapture()
                            }
                        }
                    } finally {
                        cropped.recycle()
                    }
                }

                imageProxy.close()
                isProcessing = false
            }
        } catch (e: Exception) {
            Log.e("CameraActivity", "startAnalyzer error", e)
        }
    }

    private fun triggerCapture() {
        Log.d("CameraActivity", "triggerCapture called isCaptured=$isCaptured")
        if (isCaptured) {
            Log.d("CameraActivity", "triggerCapture aborted: already captured")
            return
        }
        isCaptured = true

        Log.d("CameraActivity", "scheduling captureImage in 400ms")
        Handler(Looper.getMainLooper()).postDelayed({
            captureImage()
        }, 400)
    }

    private fun captureImage() {
        Log.d("CameraActivity", "captureImage called: initialized=${::imageCapture.isInitialized}")
        val filename = "scan_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        try {
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri
                        Log.d("SAVE", "保存成功: $savedUri")

                        Toast.makeText(
                            this@CameraActivity,
                            "保存成功",
                            Toast.LENGTH_SHORT
                        ).show()

                        savedUri?.let { uri ->
                            var uriForAI: Uri = uri
                            try {
                                val input = contentResolver.openInputStream(uri)
                                val fullBitmap = BitmapFactory.decodeStream(input)
                                input?.close()

                                if (fullBitmap != null) {
                                    val rect = overlay.getScanRect()
                                    val pvW = previewView.width
                                    val pvH = previewView.height

                                    if (pvW > 0 && pvH > 0) {
                                        val mapped = mapOverlayToBitmapRect(
                                            overlayRect = rect,
                                            bitmapWidth = fullBitmap.width,
                                            bitmapHeight = fullBitmap.height,
                                            viewWidth = pvW,
                                            viewHeight = pvH
                                        )

                                        val cropped = Bitmap.createBitmap(
                                            fullBitmap,
                                            mapped.left,
                                            mapped.top,
                                            mapped.width(),
                                            mapped.height()
                                        )

                                        val baos = ByteArrayOutputStream()
                                        cropped.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                        val bytes = baos.toByteArray()
                                        baos.close()

                                        CapturedImageStore.addImage(bytes)

                                        val croppedUri = saveBitmapToMediaStore(cropped)
                                        if (croppedUri != null) {
                                            uriForAI = croppedUri
                                            contentResolver.delete(uri, null, null)
                                            Log.d("CameraActivity", "saved cropped and deleted original=$uri")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CameraActivity", "crop saved image error", e)
                            }

                            sendToAI(uriForAI)
                        }

                        isCaptured = false
                        Log.d("CameraActivity", "capture flow completed: isCaptured reset to false")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        Log.e("CameraActivity", "onImageSaved onError: ${exception.message}", exception)
                        Log.e("CAMERA_ERROR", "保存失敗: ${exception.message}")
                        Toast.makeText(this@CameraActivity, "保存エラー", Toast.LENGTH_LONG).show()
                        isCaptured = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraActivity", "takePicture threw", e)
            isCaptured = false
        }
    }

    /** ImageProxy → Bitmap 変換 */
    fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /** ギャラリー画像処理 — ONNX OCR */
    private fun processGalleryImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null && engineReady) {
                val results = engine.extractProductCode(bitmap)
                if (results.isNotEmpty()) {
                    val text = results[0]
                    val numbers = text.replace(Regex("[^0-9]"), "")
                    if (numbers.isNotEmpty()) {
                        scannedList.add(numbers)
                    }
                }
                Toast.makeText(this, "検出完了", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendToAI(uri: Uri) {
        println("AI送信: $uri")
    }

    private fun mapOverlayToBitmapRect(
        overlayRect: Rect,
        bitmapWidth: Int,
        bitmapHeight: Int,
        viewWidth: Int,
        viewHeight: Int
    ): Rect {
        val scale = max(
            viewWidth.toFloat() / bitmapWidth.toFloat(),
            viewHeight.toFloat() / bitmapHeight.toFloat()
        )
        val displayedWidth = bitmapWidth * scale
        val displayedHeight = bitmapHeight * scale
        val dx = (viewWidth - displayedWidth) / 2f
        val dy = (viewHeight - displayedHeight) / 2f

        val left = ((overlayRect.left - dx) / scale).roundToInt().coerceIn(0, bitmapWidth - 1)
        val top = ((overlayRect.top - dy) / scale).roundToInt().coerceIn(0, bitmapHeight - 1)
        val right = ((overlayRect.right - dx) / scale).roundToInt().coerceIn(left + 1, bitmapWidth)
        val bottom = ((overlayRect.bottom - dy) / scale).roundToInt().coerceIn(top + 1, bitmapHeight)

        return Rect(left, top, right, bottom)
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri? {
        val croppedName = "scan_crop_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, croppedName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            contentResolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            } ?: run {
                contentResolver.delete(uri, null, null)
                return null
            }
            uri
        } catch (e: Exception) {
            Log.e("CameraActivity", "saveBitmapToMediaStore error", e)
            contentResolver.delete(uri, null, null)
            null
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                denied.toTypedArray(),
                100
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("CameraActivity", "onKeyDown keyCode=$keyCode event=$event")
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.d("CameraActivity", "onKeyDown triggerCapture for key=$keyCode")
                triggerCapture()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.d("CameraActivity", "dispatchKeyEvent action=${event.action} keyCode=${event.keyCode}")
        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                    Log.d("CameraActivity", "dispatchKeyEvent ACTION_UP triggerCapture key=${event.keyCode}")
                    triggerCapture()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("CameraActivity", "onKeyUp keyCode=$keyCode event=$event")
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.d("CameraActivity", "onKeyUp triggerCapture for key=$keyCode")
                triggerCapture()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        previewView.requestFocus()
        window?.decorView?.requestFocus()
        Log.d("CameraActivity", "onResume requested focus on preview and decorView")
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
