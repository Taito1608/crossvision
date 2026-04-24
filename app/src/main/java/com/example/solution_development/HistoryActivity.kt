package com.example.solution_development

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HistoryActivity : AppCompatActivity() {

    private data class ProductHistoryItem(
        val code: String,
        val status: String,
        val process: String,
        val construction: String
    )

    private val historyItems = mutableListOf<ProductHistoryItem>()
    private lateinit var historyListView: ListView
    private lateinit var historyAdapter: ProductHistoryAdapter

    private val apiEndpoint = "https://crossvision-api.hirocr-api.workers.dev/products"
    private val authToken = BuildConfig.CROSSVISION_API_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyListView = findViewById(R.id.history_list_view)
        historyAdapter = ProductHistoryAdapter(historyItems)
        historyListView.adapter = historyAdapter

        if (authToken.isBlank()) {
            Toast.makeText(this, "APIトークンが未設定です", Toast.LENGTH_LONG).show()
        } else {
            fetchProducts()
        }

        val btnBackToHome = findViewById<Button>(R.id.back_to_home_button)
        btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchProducts() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { requestProducts() }
            }

            result.fold(
                onSuccess = { fetched ->
                    val latestByCode = linkedMapOf<String, ProductHistoryItem>()
                    fetched.forEach { item ->
                        // Keep the latest entry when same code appears multiple times.
                        latestByCode.remove(item.code)
                        latestByCode[item.code] = item
                    }

                    historyItems.clear()
                    historyItems.addAll(latestByCode.values)
                    historyAdapter.notifyDataSetChanged()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@HistoryActivity,
                        "履歴取得エラー: ${error.message ?: "unknown"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun requestProducts(): List<ProductHistoryItem> {
        val connection = (URL(apiEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $authToken")
        }

        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode")
            }

            val jsonArray = when {
                responseText.trimStart().startsWith("[") -> JSONArray(responseText)
                responseText.trimStart().startsWith("{") -> {
                    val root = JSONObject(responseText)
                    root.optJSONArray("results")
                        ?: root.optJSONArray("products")
                        ?: JSONArray()
                }
                else -> JSONArray()
            }

            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    add(
                        ProductHistoryItem(
                            code = item.optString("code", ""),
                            status = item.optString("status", ""),
                            process = item.optString("process", ""),
                            construction = item.optString("construction", "")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun statusToJapanese(status: String): String {
        return when (status.lowercase()) {
            "active", "started" -> "開始"
            "completed" -> "完了"
            "stopped" -> "中止"
            "defect" -> "不良"
            "cancelled", "canceled" -> "取消"
            else -> status
        }
    }

    private inner class ProductHistoryAdapter(
        private val items: List<ProductHistoryItem>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@HistoryActivity)
                .inflate(R.layout.item_history_product, parent, false)

            val item = items[position]
            val codeText = view.findViewById<TextView>(R.id.text_code)
            val statusText = view.findViewById<TextView>(R.id.text_status)
            val processText = view.findViewById<TextView>(R.id.text_process)
            val constructionText = view.findViewById<TextView>(R.id.text_construction)

            codeText.text = item.code
            statusText.text = "進捗: ${statusToJapanese(item.status)}"
            processText.text = "工程: ${item.process}"
            constructionText.text = "工事: ${item.construction}"

            return view
        }
    }
}
