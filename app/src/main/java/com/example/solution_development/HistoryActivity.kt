package com.example.solution_development

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// import com.example.solution_development.adapter.HistoryAdapter
// import com.example.solution_development.model.HistoryItem
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val recyclerView = findViewById<RecyclerView>(R.id.historyRecyclerView)

        // val sampleList = listOf(
        //     HistoryItem("A123", "工程A", "開始"),
        //     HistoryItem("B456", "工程B", "完了")
        // )

        // recyclerView.layoutManager = LinearLayoutManager(this)
        // recyclerView.adapter = HistoryAdapter(sampleList)
    }
}
