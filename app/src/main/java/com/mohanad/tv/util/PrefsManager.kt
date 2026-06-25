package com.mohanad.tv.util

import android.content.Context

/**
 * تخزين خفيف جداً باستخدام SharedPreferences فقط (بدون Room أو أي قاعدة بيانات).
 * يحفظ آخر رابط قائمة استخدمه المستخدم لتسهيل إعادة الفتح.
 */
object PrefsManager {

    private const val PREFS_NAME = "mohanad_tv_prefs"
    private const val KEY_LAST_URL = "last_playlist_url"

    fun saveLastUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_URL, url).apply()
    }

    fun getLastUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_URL, null)
    }
}
