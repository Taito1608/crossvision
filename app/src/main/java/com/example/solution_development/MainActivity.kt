package com.example.solution_development

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Intent
import android.widget.Toast
import com.example.solution_development.R
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity(){
    private lateinit var etId: EditText
    private lateinit var etPass: EditText
    private lateinit var tvShowPass: TextView
    private lateinit var btnLogin: Button

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etId = findViewById(R.id.etId)
        etPass = findViewById(R.id.etPass)
        tvShowPass = findViewById(R.id.tvShowPass)
        btnLogin = findViewById(R.id.btnLogin)

        tvShowPass.setOnClickListener{
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible){
                etPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                tvShowPass.text = "パスワード非表示"
            }else{
                etPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                tvShowPass.text = "パスワード表示"
            }
            etPass.setSelection(etPass.text.length)
        }

        btnLogin.setOnClickListener{
            val token = BuildConfig.CROSSVISION_API_TOKEN ?: ""
            if (token.isEmpty()){
                Toast.makeText(this, "APIトークンが設定されていません。gradle.properties の crossvisionApiToken を設定してください", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 取得結果にかかわらず画面遷移を許可
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)

            Log.d("MainActivity", "Starting product code fetch; token length=${token.length}")

            Thread {
                val codes = fetchProductCodes(token)
                if (codes != null && codes.isNotEmpty()){
                    ProductCodeStore.saveCodes(this, codes)
                    runOnUiThread {
                        Toast.makeText(this, "製品コードを取得しました: ${codes.size}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "製品コードの取得に失敗しました", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }

    private fun fetchProductCodes(token: String): List<String>? {
        try {
            Log.d("MainActivity", "fetchProductCodes: opening connection to candidates endpoint")
            val url = URL("https://crossvision-api.hirocr-api.workers.dev/candidates")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val code = conn.responseCode
            Log.d("MainActivity", "HTTP response code: $code")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            Log.d("MainActivity", "HTTP response body: ${response.take(1000)}")

            if (code !in 200..299) {
                return null
            }

            // Try multiple possible JSON shapes
            val results = mutableListOf<String>()

            if (response.trim().startsWith("{")) {
                val obj = JSONObject(response)
                // handle common response shapes: "candidates" or "results" arrays of objects
                if (obj.has("candidates") || obj.has("results")) {
                    val arr = when {
                        obj.has("candidates") -> obj.getJSONArray("candidates")
                        else -> obj.getJSONArray("results")
                    }
                    for (i in 0 until arr.length()) {
                        val item = arr.get(i)
                        if (item is JSONObject) {
                            val codeStr = when {
                                item.has("product_code") -> item.getString("product_code")
                                item.has("code") -> item.getString("code")
                                item.has("productCode") -> item.getString("productCode")
                                else -> item.toString()
                            }
                            results.add(codeStr)
                        } else if (item is String) {
                            results.add(item)
                        } else {
                            results.add(item.toString())
                        }
                    }
                } else {
                    // Fallback: scan object properties for any JSON arrays and extract codes from objects inside
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = obj.get(k)
                        if (v is JSONArray) {
                            for (i in 0 until v.length()) {
                                val itm = v.get(i)
                                if (itm is JSONObject) {
                                    val codeStr = when {
                                        itm.has("product_code") -> itm.getString("product_code")
                                        itm.has("code") -> itm.getString("code")
                                        itm.has("productCode") -> itm.getString("productCode")
                                        else -> itm.toString()
                                    }
                                    results.add(codeStr)
                                } else if (itm is String) {
                                    results.add(itm)
                                }
                            }
                        }
                    }
                }
            } else if (response.trim().startsWith("[")) {
                val arr = JSONArray(response)
                for (i in 0 until arr.length()) {
                    val item = arr.get(i)
                    if (item is JSONObject) {
                        val codeStr = when {
                            item.has("product_code") -> item.getString("product_code")
                            item.has("code") -> item.getString("code")
                            item.has("productCode") -> item.getString("productCode")
                            else -> item.toString()
                        }
                        results.add(codeStr)
                    } else if (item is String) results.add(item)
                }
            }

            return results
        } catch (e: Exception) {
            Log.e("MainActivity", "fetchProductCodes error", e)
            return null
        }
    }
}