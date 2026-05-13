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
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.example.solution_development.ocr.OnnxOcrEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.regex.Pattern

class CameraActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
    }

    // TODO ===== UI =====
    private lateinit var previewView: PreviewView
    private lateinit var overlay: ScanOverlayView
    private lateinit var btnSelectImage: Button

    // TODO ===== CameraX =====
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: androidx.camera.core.ImageAnalysis

    // TODO ===== OCR (ONNX PaddleOCR) =====
    private lateinit var engine: OnnxOcrEngine

    // TODO ===== ??? =====
    private var isCaptured = false
    private val scannedList = mutableListOf<String>()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // 鉄鋼製品の使用可能文字のみを抽出する正規表現
    // 許可: 0-9, A-Z, a-z, +, -, ., /, _, スペース
    private val ALLOWED_CHARS_PATTERN = Pattern.compile("[A-Za-z0-9+_.\\-/ ]{6,}")

    // 推論中のフレームを防止するフラグ
    private var isProcessing = false

    // TODO ===== AI =====
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processGalleryImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "スキャン画面"

            // ONNX PaddleOCR の初期化
            engine = OnnxOcrEngine(this)

            previewView = findViewById(R.id.previewView)
            overlay = findViewById(R.id.overlay)
            val btnComplete = findViewById<Button>(R.id.btnComplete)
            btnSelectImage = findViewById(R.id.btnSelectImage)

            btnComplete.setOnClickListener {
                val intent = Intent(this, ConfirmationActivity::class.java)
                
//                 val process = this.intent.getStringExtra("process")
//                 val construction = this.intent.getStringExtra("construction")

//                 // ダミーデータ：スキャン結果を追加
//                 if (scannedList.isEmpty()) {
//                     scannedList.add("123456")
//                     scannedList.add("789012")
//                     scannedList.add("345678")
//                 }

//                 confirmationIntent.putStringArrayListExtra("scannedList", ArrayList(scannedList))
//                 confirmationIntent.putExtra("process", process)
//                 confirmationIntent.putExtra("construction", construction)

                intent.putStringArrayListExtra("scannedList", ArrayList(scannedList))

                startActivity(intent)
            }

            btnSelectImage.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            if(ContextCompat.checkSelfPermission(
                this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ){
                startCamera()
            }else{
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

        // ONNX engine is initialized in onCreate above
        }.start()

        if(ContextCompat.checkSelfPermission(
            this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if(requestCode == 1001){
            if(grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startCamera()
            }else{
                Toast.makeText(this, "カメラ権限が必要です",Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera(){
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also{
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

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

                    // Viewのレイアウトが完了した後に Analyzer を開始
                    overlay.post {
                        startAnalyzer()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Camera binding error", e)
                    Toast.makeText(this@CameraActivity, "カメラバインドエラー: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "startCamera error", e)
            Toast.makeText(this, "カメラ初期化エラー", Toast.LENGTH_LONG).show()
        }
    }

    private fun startAnalyzer() {
        try {
            imageAnalysis.setAnalyzer(
                cameraExecutor
            ) { imageProxy ->

                if (isProcessing || isCaptured) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                isProcessing = true

                // ONNX PaddleOCR inference
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    try {
                        val bitmap = imageProxyToBitmap(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        if (bitmap != null) {
                            Thread {
                                try {
                                    val codes = engine.extractProductCode(bitmap)
                                    if (codes.isNotEmpty()) {
                                        runOnUiThread {
                                            for (code in codes) {
                                                if (!scannedList.contains(code)) {
                                                    scannedList.add(code)
                                                    Log.d(TAG, "New code detected: $code")
                                                }
                                            }

                                            if (!isCaptured) {
                                                isCaptured = true
                                                captureImage()
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    isCaptured = false
                                                }, 2000)
                                            }
                                        }
                                    }
                                } catch (e2: Exception) {
                                    Log.e(TAG, "ONNX OCR error", e2)
                                } finally {
                                    runOnUiThread { isProcessing = false }
                                    imageProxy.close()
                                }
                            }.start()
                        } else {
                            isProcessing = false
                            imageProxy.close()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Image conversion error", e)
                        isProcessing = false
                        imageProxy.close()
                    }
                } else {
                    isProcessing = false
                    imageProxy.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startAnalyzer error", e)
        }
    }

    private fun triggerCapture() {
        if (isCaptured) return
        isCaptured = true

        //imageAnalysis.clearAnalyzer()

        Handler(Looper.getMainLooper()).postDelayed({
            captureImage()
        }, 400)

        //Handler(Looper.getMainLooper()).postDelayed({
        //    isCaptured = false
//
        //    startAnalyzer()
        //}, 2500)
    }

    /**
     * ImageProxy (YUV_420_888) を Bitmap に変換
     */
    private fun imageProxyToBitmap(image: android.media.Image, rotationDegrees: Int): Bitmap? {
        return try {
            val planes = image.planes
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

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val imageBytes = out.toByteArray()
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Apply rotation if needed
            if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap", e)
            null
        }
    }

    private fun captureImage() {
        val filename = "scan_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
            //put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        //val uri = contentResolver.insert(
        //    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        //    values
        //) ?: run {
        //    Toast.makeText(this, "URI作成失敗", Toast.LENGTH_SHORT).show()
        //    return
        //}

        //if (uri == null) {
        //    Toast.makeText(this, "保存失敗", Toast.LENGTH_SHORT).show()
        //    return
        //}



        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //val uri = Uri.fromFile(file)

                    val savedUri = outputFileResults.savedUri

                    Log.d("SAVE", "保存成功: $savedUri")

                    Toast.makeText(
                        this@CameraActivity,
                        "保存成功",
                        Toast.LENGTH_SHORT
                    ).show()

                    savedUri?.let {
                        sendToAI(it)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()

                    Log.e("CAMERA_ERROR", "保存失敗: ${exception.message}")
                    Toast.makeText(this@CameraActivity, "保存エラー", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

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

    private fun processGalleryImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            Thread {
                try {
                    val codes = engine.extractProductCode(bitmap)
                    if (codes.isNotEmpty()) {
                        runOnUiThread {
                            for (code in codes) {
                                if (!scannedList.contains(code)) {
                                    scannedList.add(code)
                                }
                            }
                            Toast.makeText(this, "検出完了", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gallery OCR error", e)
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun sendToAI(uri: Uri) {
        println("AI送信: $uri")
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

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
