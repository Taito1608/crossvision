package com.example.solution_development

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var confirmationMessage: TextView
    private lateinit var statusGroup: RadioGroup
    private lateinit var confirmButton: Button

    private var selectedStatus: String = "STARTED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        // ===== View取得 =====
        confirmationMessage = findViewById(R.id.confirmation_message)
        statusGroup = findViewById(R.id.status_group)
        confirmButton = findViewById(R.id.confirm_button)

        // ===== Intentからデータ取得 =====
        val productNumber = intent.getStringExtra("productNumber") ?: "未取得"
        val process = intent.getStringExtra("process") ?: "未選択"
        val construction = intent.getStringExtra("construction") ?: "未選択"

        confirmationMessage.text =
            "製品番号: $productNumber\n工程: $process\n工事: $construction"

        // ===== ステータス選択 =====
        statusGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedStatus = when (checkedId) {
                R.id.status_start -> "STARTED"
                R.id.status_complete -> "COMPLETED"
                R.id.status_stop -> "STOPPED"
                R.id.status_defect -> "DEFECT"
                R.id.status_cancel -> "CANCELLED"
                else -> "STARTED"
            }
        }

        // ===== 送信ボタン =====
        confirmButton.setOnClickListener {

            val timestamp = ZonedDateTime.now()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val json = mapOf(
                "productNumber" to productNumber,
                "timestamp" to timestamp,
                "status" to selectedStatus,
                "process" to process,
                "construction" to construction
            )

            println("送信データ: $json")

            Toast.makeText(this, "送信しました", Toast.LENGTH_SHORT).show()

            finish()
        }
    }
}