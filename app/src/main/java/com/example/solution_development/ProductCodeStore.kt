package com.example.solution_development

import android.content.Context
import org.json.JSONArray

object ProductCodeStore {
    private const val PREFS_NAME = "crossvision_prefs"
    private const val KEY_CANDIDATES = "product_candidates_json"

    fun saveCandidates(context: Context, candidates: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (c in candidates) arr.put(c)
        prefs.edit().putString(KEY_CANDIDATES, arr.toString()).apply()
    }

    fun getCandidates(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CANDIDATES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CANDIDATES).apply()
    }
}
