package com.example.solution_development

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import java.io.File
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage


private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

class CameraActivity : AppCompatActivity() {

    // TODO ===== UI =====
    private lateinit var previewView: PreviewView
    //private lateinit var btnComplete: Button
    private lateinit var btnSelectImage: Button

    // TODO ===== CameraX =====
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    // TODO ===== ??? =====
    private var isCaptured = false
    private val scannedList = mutableListOf<String>()

    // TODO ===== AI =====
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                sendToAI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "スキャン画面"

        previewView = findViewById(R.id.previewView)

        val btnComplete = findViewById<Button>(R.id.btnComplete)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)

        btnComplete.setOnClickListener {
            val intent = Intent(this, ConfirmationActivity::class.java)

            startActivity(intent)
        }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
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

            imageAnalysis = ImageAnalysis.Builder().build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this)
            ) { imageProxy: ImageProxy ->

                // TODO: OCR
                val mediaImage = imageProxy.image

                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text

                            // OCR Result
                            if (text.isNotEmpty()) {
                                val numbers = text.filter { it.isDigit() }

                                if (numbers.length >= 6) {
                                    if (!scannedList.contains(numbers)) {
                                        scannedList.add(numbers)
                                    }

                                    if (!isCaptured) {
                                        isCaptured = true
                                        captureImage()
                                    }
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
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
                    preview
                )
            }catch (e: Exception){
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
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

    private fun fakeScanCheck(): Boolean {
        // now random but OCR insert later
        return (0..50).random() == 0
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
}