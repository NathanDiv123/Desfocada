package com.mirage.auto

import android.content.Context

/** Guarda as coordenadas calibradas e os parâmetros do ciclo. */
object Config {
    private fun p(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun pt(c: Context, key: String): Pair<Int, Int>? {
        val s = p(c)
        return if (s.contains(key + "_x")) {
            Pair(s.getInt(key + "_x", 0), s.getInt(key + "_y", 0))
        } else null
    }

    fun setPt(c: Context, key: String, x: Int, y: Int) {
        p(c).edit().putInt(key + "_x", x).putInt(key + "_y", y).apply()
    }

    fun clearPt(c: Context, key: String) {
        p(c).edit().remove(key + "_x").remove(key + "_y").apply()
    }

    fun int(c: Context, key: String, def: Int): Int = p(c).getInt(key, def)

    fun setInt(c: Context, key: String, v: Int) {
        p(c).edit().putInt(key, v).apply()
    }
}
