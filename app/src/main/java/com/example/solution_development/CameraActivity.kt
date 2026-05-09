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
import java.util.concurrent.Executors
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream


class CameraActivity : AppCompatActivity() {

    // TODO ===== UI =====
    private lateinit var previewView: PreviewView
    private lateinit var overlay: ScanOverlayView
    //private lateinit var btnComplete: Button
    private lateinit var btnSelectImage: Button

    // TODO ===== CameraX =====
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    // TODO ===== OCR =====
    private lateinit var recognizer: com.google.mlkit.vision.text.TextRecognizer

    // TODO ===== ??? =====
    private var isCaptured = false
    private val scannedList = mutableListOf<String>()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // TODO ===== AI =====
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processGalleryImage(it) }
        }

    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "スキャン画面"

            // ML Kit の初期化
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            previewView = findViewById(R.id.previewView)
            overlay = findViewById(R.id.overlay)

            val btnComplete = findViewById<Button>(R.id.btnComplete)
            val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)

            btnComplete.setOnClickListener {
                val intent = Intent(this, ConfirmationActivity::class.java)

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

        //btnShutter.setOnClickListener{
        //    val photoFile = File(cacheDir, "ocr_temp.jpg")

        //    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        //    imageCapture.takePicture(
        //        outputOptions,
        //        ContextCompat.getMainExecutor(this),
        //        object : ImageCapture.OnImageSavedCallback {
        //            override fun onImageSaved(outputFileResults: ImageCapture.outputFileResults){

        //                val savedUri = Uri.fromFile(photoFile)
        //                Toast.makeText(this@CameraActivity, "スキャン成功", Toast.LENGTH_SHORT).show()

        //                // send to OCR(filepass)
        //                runOCR(savedUri)

        //                //finish()
        //            }

        //            override fun onError(exception: ImageCaptureException){
        //                Toast.makeText(this@CameraActivity,"スキャン成功: ${exception.message}", Toast.LENGTH_SHORT).show()
        //            }
        //        }
        //    )
        //}

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
                    Log.e("CameraActivity", "Camera binding error", e)
                    Toast.makeText(this@CameraActivity, "カメラバインドエラー: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("CameraActivity", "startCamera error", e)
            Toast.makeText(this, "カメラ初期化エラー", Toast.LENGTH_LONG).show()
        }
    }

    private fun startAnalyzer() {
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

                // Rect が有効かどうか確認
                if (rect.width() <= 0 || rect.height() <= 0) {
                    imageProxy.close()
                    isProcessing = false
                    return@setAnalyzer
                }

                // Bitmap の範囲内でクロップ領域を計算
                val cropLeft = rect.left.coerceIn(0, bitmap.width)
                val cropTop = rect.top.coerceIn(0, bitmap.height)
                val cropWidth = (rect.right - rect.left).coerceAtLeast(1).coerceAtMost(bitmap.width - cropLeft)
                val cropHeight = (rect.bottom - rect.top).coerceAtLeast(1).coerceAtMost(bitmap.height - cropTop)

                // Crop サイズが有効かチェック
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

                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                try {
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            val numbers = text.replace(Regex("[^0-9]"), "")

                            if (numbers.length >= 6) {
                                if (!scannedList.contains(numbers)) {
                                    scannedList.add(numbers)
                                }

                                triggerCapture()
                            }
                        }
                        .addOnCompleteListener {
                            isProcessing = false
                            imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e("CameraActivity", "OCR processing error", e)
                    isProcessing = false
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
                isProcessing = false
            }
            }
        } catch (e: Exception) {
            Log.e("CameraActivity", "startAnalyzer error", e)
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

    //private fun fakeScanCheck(): Boolean {
    //    // now random but OCR insert later
    //    return (0..50).random() == 0
    //}

    private fun processGalleryImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val numbers = text.replace(Regex("[^0-9]"), "")

                    if (numbers.isNotEmpty()) {
                        scannedList.add(numbers)
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
}