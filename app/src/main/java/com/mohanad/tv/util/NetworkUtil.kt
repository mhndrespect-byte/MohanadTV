package com.mohanad.tv.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * أداة خفيفة لفحص حالة الشبكة الحالية، بدون أي مكتبة خارجية.
 */
object NetworkUtil {

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isMeteredOrWeak(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return true
        val capabilities = cm.getNetworkCapabilities(network) ?: return true
        // إن كانت الشبكة مقيدة (بيانات جوال محدودة) نعتبرها "ضعيفة" احترازياً لتقليل الاستهلاك
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
