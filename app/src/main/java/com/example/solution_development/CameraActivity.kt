package com.example.solution_development

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import java.util.regex.Pattern

// ============================================================
// [ML Kit code - preserved for reference]
// import com.google.mlkit.vision.text.TextRecognition
// import com.google.mlkit.vision.text.latin.TextRecognizerOptions
// import com.google.mlkit.vision.common.InputImage
//
// private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
// ============================================================

class CameraActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
    }

    // TODO ===== UI =====
    private lateinit var previewView: PreviewView
    private lateinit var btnSelectImage: Button

    // TODO ===== CameraX =====
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: androidx.camera.core.ImageAnalysis

    // TODO ===== OCR (ONNX PaddleOCR v3) =====
    private lateinit var engine: OnnxOcrEngine

    // TODO ===== ??? =====
    private var isCaptured = false
    private val scannedList = mutableListOf<String>()

    // 鉄鋼製品の使用可能文字のみを抽出する正規表現
    // 許可: 0-9, A-Z, a-z, +, -, ., /, _, スペース
    private val ALLOWED_CHARS_PATTERN = Pattern.compile("[A-Za-z0-9+_.\\-/ ]{6,}")

    // 推論中のフレームを防止するフラグ
    private var isProcessing = false

    // TODO ===== AI =====
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                Toast.makeText(this, "画像選択完了", Toast.LENGTH_SHORT).show()
                sendToAI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "スキャン画面"

        previewView = findViewById(R.id.previewView)

        val btnComplete = findViewById<Button>(R.id.btnComplete)
        btnSelectImage = findViewById(R.id.btnSelectImage)

        btnComplete.setOnClickListener {
            val intent = Intent(this, ConfirmationActivity::class.java)

            // ダミーデータ：スキャン結果を追加
            if (scannedList.isEmpty()) {
                scannedList.add("123456")
                scannedList.add("789012")
                scannedList.add("345678")
                scannedList.add("789012")
                scannedList.add("345678")
                scannedList.add("789012")
                scannedList.add("345678")
                scannedList.add("789012")
                scannedList.add("345678")
            }

            intent.putStringArrayListExtra("scannedList", ArrayList(scannedList))

            startActivity(intent)
        }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // ============================================================
        // [ML Kit shutter button code - preserved for reference]
        // btnShutter.setOnClickListener{
        //     val photoFile = File(cacheDir, "ocr_temp.jpg")
        //     val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        //     imageCapture.takePicture(
        //         outputOptions,
        //         ContextCompat.getMainExecutor(this),
        //         object : ImageCapture.OnImageSavedCallback {
        //             override fun onImageSaved(outputFileResults: ImageCapture.outputFileResults){
        //                 val savedUri = Uri.fromFile(photoFile)
        //                 Toast.makeText(this@CameraActivity, "スキャン成功", Toast.LENGTH_SHORT).show()
        //                 runOCR(savedUri)
        //             }
        //             override fun onError(exception: ImageCaptureException){
        //                 Toast.makeText(this@CameraActivity,"スキャン成功: ${exception.message}", Toast.LENGTH_SHORT).show()
        //             }
        //         }
        //     )
        // }
        // ============================================================

        // Initialize ONNX OCR Engine
        engine = OnnxOcrEngine(this)
        engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCR_V3_RAPIDOCR)

        // Load model in background thread
        Thread {
            val success = engine.loadModel()
            Log.d(TAG, "ONNX model load result: $success")
            runOnUiThread {
                if (success) {
                    Log.d(TAG, "ONNX model loaded successfully")
                } else {
                    Log.e(TAG, "Failed to load ONNX model")
                    Toast.makeText(this, "モデルの読み込みに失敗しました", Toast.LENGTH_LONG).show()
                }
            }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also{
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this)
            ) { imageProxy: ImageProxy ->

                if (isProcessing) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                isProcessing = true

                // ============================================================
                // [ML Kit OCR code - preserved for reference]
                // val mediaImage = imageProxy.image
                // if (mediaImage != null) {
                //     val image = InputImage.fromMediaImage(
                //         mediaImage,
                //         imageProxy.imageInfo.rotationDegrees
                //     )
                //     recognizer.process(image)
                //         .addOnSuccessListener { visionText ->
                //             val text = visionText.text
                //             val numbers = text.replace(Regex("[^0-9]"), "")
                //             if (numbers.length >= 6) {
                //                 if (!scannedList.contains(numbers)) {
                //                     scannedList.add(numbers)
                //                 }
                //                 if (!isCaptured) {
                //                     isCaptured = true
                //                     captureImage()
                //                     Handler(Looper.getMainLooper()).postDelayed({
                //                         isCaptured = false
                //                     }, 2000)
                //                 }
                //             }
                //         }
                //         .addOnCompleteListener {
                //             isProcessing = false
                //             imageProxy.close()
                //         }
                // } else {
                //     imageProxy.close()
                // }
                // ============================================================

                // ONNX PaddleOCR v3 inference
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

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
            }catch (e: Exception){
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
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
        val file = File(
            getExternalFilesDir(null),
            "scan_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(file)
                    Toast.makeText(this@CameraActivity, "スキャン完了", Toast.LENGTH_SHORT).show()

                    sendToAI(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }

    private fun sendToAI(uri: Uri) {
        println("AI送信: $uri")
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
