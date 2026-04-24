package com.example.solution_development

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ConfirmationActivity : AppCompatActivity() {

    private data class StatusOption(val code: String, val label: String)
    private data class ScannedItem(var code: String, var status: String)

    private lateinit var confirmationMessage: TextView
    private lateinit var statusGroup: RadioGroup
    private lateinit var confirmButton: Button
    private lateinit var continueButton: Button
    private lateinit var scannedListView: ListView
    private lateinit var scannedItemAdapter: ScannedItemAdapter
    private val scannedItems = mutableListOf<ScannedItem>()
    private var isSubmitting = false

    private val apiEndpoint = "https://crossvision-api.hirocr-api.workers.dev/products"
    private val authToken = BuildConfig.CROSSVISION_API_TOKEN

    private val statusOptions = listOf(
        StatusOption("STARTED", "開始"),
        StatusOption("COMPLETED", "完了"),
        StatusOption("STOPPED", "中止"),
        StatusOption("DEFECT", "不良"),
        StatusOption("CANCELLED", "取消")
    )

    private var selectedStatus: String = "STARTED"

    private data class ProductRequest(
        val code: String,
        val status: String,
        val process: String,
        val construction: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        // ===== View取得 =====
        confirmationMessage = findViewById(R.id.confirmation_message)
        statusGroup = findViewById(R.id.status_group)
        confirmButton = findViewById(R.id.confirm_button)
        continueButton = findViewById(R.id.continue_button)
        scannedListView = findViewById(R.id.scanned_list_view)

        // ===== Intentからデータ取得 =====
        val process = intent.getStringExtra("process").orEmpty().ifBlank { "unknown" }
        val construction = intent.getStringExtra("construction").orEmpty().ifBlank { "unknown" }
        val scannedList = intent.getStringArrayListExtra("scannedList") ?: arrayListOf()

        confirmationMessage.text = "写真エリア"

        // ===== スキャン結果をListViewに表示 =====
        scannedItems.clear()
        scannedItems.addAll(scannedList.map { code -> ScannedItem(code, selectedStatus) })

        val labelByCode = statusOptions.associate { it.code to it.label }
        val codeByLabel = statusOptions.associate { it.label to it.code }

        scannedItemAdapter = ScannedItemAdapter(
            items = scannedItems,
            statusLabels = statusOptions.map { it.label },
            labelByCode = labelByCode,
            codeByLabel = codeByLabel
        )
        scannedListView.adapter = scannedItemAdapter

        // 初期表示時は全体ステータスに合わせる
        if (scannedItems.isNotEmpty()) {
            scannedItems.forEach { it.status = selectedStatus }
            scannedItemAdapter.notifyDataSetChanged()
        }

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

            // 全体ステータス変更時はリスト全件へ反映
            if (scannedItems.isNotEmpty()) {
                scannedItems.forEach { it.status = selectedStatus }
                scannedItemAdapter.notifyDataSetChanged()
            }
        }

        // ===== 送信ボタン =====
        confirmButton.setOnClickListener {
            if (isSubmitting) {
                return@setOnClickListener
            }

            if (scannedItems.isEmpty()) {
                Toast.makeText(this, "送信対象の製品がありません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duplicateCodes = scannedItems
                .groupingBy { it.code }
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .toList()
            if (duplicateCodes.isNotEmpty()) {
                val preview = duplicateCodes.take(3).joinToString(",")
                val suffix = if (duplicateCodes.size > 3) " ほか${duplicateCodes.size - 3}件" else ""
                Toast.makeText(
                    this,
                    "重複した製品コードがあります: ${preview}${suffix}",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (authToken.isBlank()) {
                Toast.makeText(this, "APIトークンが未設定です", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val payload = scannedItems.map {
                ProductRequest(
                    code = it.code,
                    status = toApiStatus(it.status),
                    process = process,
                    construction = construction
                )
            }

            submitProducts(payload)
        }

        // ===== 続行ボタン =====
        continueButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun submitProducts(payload: List<ProductRequest>) {
        isSubmitting = true
        confirmButton.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    postProducts(payload)
                }
            }

            isSubmitting = false
            confirmButton.isEnabled = true

            result.fold(
                onSuccess = { success ->
                    if (success) {
                        Toast.makeText(this@ConfirmationActivity, "送信しました", Toast.LENGTH_SHORT).show()
                        val homeIntent = Intent(this@ConfirmationActivity, HomeActivity::class.java)
                        homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(homeIntent)
                        finish()
                    } else {
                        Toast.makeText(this@ConfirmationActivity, "送信に失敗しました", Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@ConfirmationActivity,
                        "送信エラー: ${error.message ?: "unknown"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun postProducts(payload: List<ProductRequest>): Boolean {
        val requestBody = JSONArray().apply {
            payload.forEach { product ->
                put(
                    JSONObject().apply {
                        put("code", product.code)
                        put("status", product.status)
                        put("process", product.process)
                        put("construction", product.construction)
                    }
                )
            }
        }.toString()

        val connection = (URL(apiEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $authToken")
        }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            if (statusCode !in 200..299) {
                return false
            }

            if (responseText.isBlank()) {
                return true
            }

            JSONObject(responseText).optBoolean("success", false)
        } finally {
            connection.disconnect()
        }
    }

    private fun toApiStatus(status: String): String {
        return when (status) {
            "STARTED" -> "active"
            "COMPLETED" -> "completed"
            "STOPPED" -> "stopped"
            "DEFECT" -> "defect"
            "CANCELLED" -> "cancelled"
            else -> status.lowercase()
        }
    }

    private inner class ScannedItemAdapter(
        private val items: MutableList<ScannedItem>,
        private val statusLabels: List<String>,
        private val labelByCode: Map<String, String>,
        private val codeByLabel: Map<String, String>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ConfirmationActivity)
                .inflate(R.layout.item_scanned, parent, false)

            val codeEdit = view.findViewById<EditText>(R.id.text_code)
            val statusSpinner = view.findViewById<Spinner>(R.id.spinner_status)
            val deleteButton = view.findViewById<Button>(R.id.button_delete)
            val item = items[position]

            if (codeEdit.text.toString() != item.code) {
                codeEdit.setText(item.code)
            }
            codeEdit.tag = position
            codeEdit.setOnFocusChangeListener { editView, hasFocus ->
                if (!hasFocus) {
                    val itemPosition = editView.tag as? Int ?: return@setOnFocusChangeListener
                    val editedCode = (editView as EditText).text.toString().trim()
                    if (itemPosition in items.indices) {
                        items[itemPosition].code = editedCode
                    }
                }
            }

            deleteButton.tag = position
            deleteButton.setOnClickListener { clickedView ->
                val itemPosition = clickedView.tag as? Int ?: return@setOnClickListener
                if (itemPosition in items.indices) {
                    items.removeAt(itemPosition)
                    notifyDataSetChanged()
                }
            }

            if (statusSpinner.adapter == null) {
                val spinnerAdapter = ArrayAdapter(
                    this@ConfirmationActivity,
                    android.R.layout.simple_spinner_item,
                    statusLabels
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                statusSpinner.adapter = spinnerAdapter
            }

            val currentLabel = labelByCode[item.status] ?: statusLabels.first()
            val selectedIndex = statusLabels.indexOf(currentLabel).let { if (it >= 0) it else 0 }

            statusSpinner.onItemSelectedListener = null
            statusSpinner.tag = position
            statusSpinner.setSelection(selectedIndex, false)
            statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    selectedView: View?,
                    selectedPosition: Int,
                    id: Long
                ) {
                    val itemPosition = statusSpinner.tag as? Int ?: return
                    val selectedLabel = statusLabels[selectedPosition]
                    items[itemPosition].status = codeByLabel[selectedLabel] ?: "STARTED"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

            return view
        }
    }
}