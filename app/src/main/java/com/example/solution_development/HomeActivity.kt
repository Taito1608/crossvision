package com.example.solution_development

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.AutoCompleteTextView
import android.widget.ListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeActivity : AppCompatActivity() {
    private lateinit var etConstructionText: AutoCompleteTextView
    private lateinit var etProcessText: AutoCompleteTextView
    private lateinit var btnConstruction: Button
    private lateinit var btnProcess: Button

    private val constructionItems = listOf("工事A", "工事B", "工事C", "工事D", "工事E", "工事F", "工事G", "工事H")
    private val processItems = listOf("工程A", "工程B", "工程C", "工程D", "工程E", "工程F", "工程G", "工程H")

    private fun showLogoutDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("ログアウト")
        builder.setMessage("本当にログアウトしますか？")

        builder.setPositiveButton("はい"){ _, _ ->
            logout()
        }

        builder.setNegativeButton("キャンセル"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun logout(){
        val intent = Intent(this, MainActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?){
//        etConstructionText = findViewById(R.id.etConstructionText)
//        etProcessText = findViewById(R.id.etProcessText)
//
//        btnConstruction = findViewById(R.id.btnConstruction)
//        btnProcess = findViewById(R.id.btnProcess)

//        btnConstruction.setOnClickListener{
//            showBottomSheetForConstruction()
//        }

//        btnProcess.setOnClickListener{
//            showBottomSheetForProcess()
//        }
        //new code...2026_01_07_05:35
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homescreen)

        btnConstruction = findViewById(R.id.btnConstruction)
        btnProcess = findViewById(R.id.btnProcess)

        etConstructionText = findViewById(R.id.etConstructionText)
        etProcessText = findViewById(R.id.etProcessText)

        val constructionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, constructionItems)
        etConstructionText.setAdapter(constructionAdapter)
        etConstructionText.threshold = 1
        etConstructionText.setOnClickListener {
            etConstructionText.showDropDown()
        }

        val processAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, processItems)
        etProcessText.setAdapter(processAdapter)
        etProcessText.threshold = 1
        etProcessText.setOnClickListener {
            etProcessText.showDropDown()
        }

        btnConstruction.setOnClickListener { showConstructionSheet() }
        btnProcess.setOnClickListener { showProcessSheet() }

        val btnNumberReg = findViewById<Button>(R.id.btnNumberReg)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        btnNumberReg.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("process", etProcessText.text?.toString()?.trim().orEmpty())
            intent.putExtra("construction", etConstructionText.text?.toString()?.trim().orEmpty())
            startActivity(intent)
        }

        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showConstructionSheet(){
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_construction, null, false)

        val listView = view.findViewById<ListView>(R.id.listConstruction)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, constructionItems)
        listView.adapter = adapter

        listView.setOnItemClickListener{ _, _, position, _ ->
            etConstructionText.setText(constructionItems[position], false)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showProcessSheet(){
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_process, null, false)

        val listView = view.findViewById<ListView>(R.id.listProcess)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, processItems)
        listView.adapter = adapter

        listView.setOnItemClickListener{ _, _, position, _ ->
            etProcessText.setText(processItems[position], false)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_logout -> {
                // delete login data
                // SharedPreferences

                // return login screen
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                showLogoutDialog()
                startActivity(intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    private fun showBottomSheetForConstruction(){
//        val dialog = BottomSheetDialog(this)
//        val view = layoutInflater.inflate(R.layout.bottomsheet_construction, null, false)
//
//        val listView = view.findViewById<ListView>(R.id.listViewConstruction)
//
//        val items = listOf("工事A","工事B","工事C","工事D","工事E","工事F","工事G")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
//
//        listView.adapter = adapter
//
//        listView.setOnItemClickListener { _, _, position, _ ->
//            etConstructionText.setText(items[position])
//            dialog.dismiss()
//        }
//
//        dialog.setContentView(view)
//        dialog.show()
//    }

//    private fun showBottomSheetForProcess(){
//        val dialog = BottomSheetDialog(this)
//        val view = layoutInflater.inflate(R.layout.bottomsheet_process, null, false)
//
//        val listView = view.findViewById<ListView>(R.id.listViewProcess)
//
//        val items = listOf("工程1","工程2","工程3","工程4","工程5","工程6","工程7")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
//        listView.adapter = adapter
//
//        listView.setOnItemClickListener { _, _, position, _ ->
//            etProcessText.setText(items[position])
//            dialog.dismiss()
//        }
//
//        dialog.setContentView(view)
//        dialog.show()
//    }

}