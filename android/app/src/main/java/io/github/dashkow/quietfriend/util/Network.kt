package io.github.dashkow.quietfriend.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Есть ли подключение к сети (проверка перед входом, сценарий 6а). */
fun Context.isOnline(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
