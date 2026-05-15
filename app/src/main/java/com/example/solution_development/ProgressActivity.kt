package com.example.solution_development

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Intent
import com.example.solution_development.R
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper

class ProgressActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "確認画面"

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val rv = findViewById<RecyclerView>(R.id.rvOcrList)
        val imgPreview = findViewById<ImageView>(R.id.imgPreview)

        // prepare images list and adapter
        val images = CapturedImageStore.getImages().toMutableList()
        val adapter = CapturedImagesAdapter(images) { index ->
            val bytes = images[index]
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imgPreview.setImageBitmap(bmp)
        }
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = adapter
        if (images.isNotEmpty()) {
            // show latest image in preview
            val first = images[0]
            val bmp = BitmapFactory.decodeByteArray(first, 0, first.size)
            imgPreview.setImageBitmap(bmp)
            rv.scrollToPosition(0)
        }

        // listen for new captured images and update UI
        val listener: (ByteArray) -> Unit = { bytes ->
            Handler(Looper.getMainLooper()).post {
                images.add(0, bytes)
                adapter.notifyItemInserted(0)
                rv.scrollToPosition(0)
                // show in preview
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgPreview.setImageBitmap(bmp)
            }
        }
        CapturedImageStore.addListener(listener)

        // remove listener on destroy
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                CapturedImageStore.removeListener(listener)
            }
        })

        btnRegister.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean{
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    //val btnRegister = findViewById<Button>(R.id.btnRegister)
}