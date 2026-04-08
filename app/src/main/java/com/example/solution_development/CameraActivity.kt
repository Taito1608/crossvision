package com.example.solution_development

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceStatus: Bundle?){
        super.onCreate(savedInstanceStatus)
        setContentView(R.layout.activity_camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "スキャン画面"

        previewView = findViewById(R.id.previewView)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
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
}