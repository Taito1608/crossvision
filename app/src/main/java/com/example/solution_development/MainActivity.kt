package com.example.solution_development

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Intent
import com.example.solution_development.R

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
                val intent = Intent(this,HomeActivity::class.java)
                startActivity(intent)
        }
    }
}