package com.example.solution_development

import android.content.Context
import org.json.JSONArray

object ProductCodeStore {
    private const val PREFS_NAME = "crossvision_prefs"
    private const val KEY_CODES = "product_codes_json"

    fun saveCodes(context: Context, codes: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (c in codes) arr.put(c)
        prefs.edit().putString(KEY_CODES, arr.toString()).apply()
    }

    fun getCodes(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CODES, null) ?: return emptyList()
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
        prefs.edit().remove(KEY_CODES).apply()
    }
}
