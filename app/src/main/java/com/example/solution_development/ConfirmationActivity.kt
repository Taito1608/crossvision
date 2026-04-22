package com.example.solution_development

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class ConfirmationActivity : AppCompatActivity() {

    private data class StatusOption(val code: String, val label: String)
    private data class ScannedItem(val code: String, var status: String)

    private lateinit var confirmationMessage: TextView
    private lateinit var statusGroup: RadioGroup
    private lateinit var confirmButton: Button
    private lateinit var continueButton: Button
    private lateinit var scannedListView: ListView
    private lateinit var scannedItemAdapter: ScannedItemAdapter
    private val scannedItems = mutableListOf<ScannedItem>()

    private val statusOptions = listOf(
        StatusOption("STARTED", "開始"),
        StatusOption("COMPLETED", "完了"),
        StatusOption("STOPPED", "中止"),
        StatusOption("DEFECT", "不良"),
        StatusOption("CANCELLED", "取消")
    )

    private var selectedStatus: String = "STARTED"

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
        val productNumber = intent.getStringExtra("productNumber") ?: "未取得"
        val process = intent.getStringExtra("process") ?: "未選択"
        val construction = intent.getStringExtra("construction") ?: "未選択"
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
            //サーバーにデータを送信する処理

            // val timestamp = ZonedDateTime.now()
            //     .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            // val json = mapOf(
            //     "productNumber" to productNumber,
            //     "timestamp" to timestamp,
            //     "status" to selectedStatus,
            //     "process" to process,
            //     "construction" to construction
            // )

            // println("送信データ: $json")

            // Toast.makeText(this, "送信しました", Toast.LENGTH_SHORT).show()

            // finish()
            val intent = Intent(this,HomeActivity::class.java)
            startActivity(intent)
        }

        // ===== 続行ボタン =====
        continueButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
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

            val codeText = view.findViewById<TextView>(R.id.text_code)
            val statusSpinner = view.findViewById<Spinner>(R.id.spinner_status)
            val item = items[position]

            codeText.text = item.code

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