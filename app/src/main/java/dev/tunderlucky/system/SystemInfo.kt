package dev.tunderlucky.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class SysInfo(
    val battery: Int, val charging: Boolean, val freeGb: Float, val totalGb: Float,
    val model: String, val android: String, val network: String
)

object SystemInfo {
    fun read(ctx: Context): SysInfo {
        val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lvl = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val st = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (lvl >= 0 && scale > 0) lvl * 100 / scale else -1
        val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val sf = StatFs(Environment.getDataDirectory().path)
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val net = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            else -> "Connected"
        }
        return SysInfo(pct, charging, sf.availableBytes / 1e9f, sf.totalBytes / 1e9f,
            "${Build.MANUFACTURER} ${Build.MODEL}", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", net)
    }
}

object Flashlight {
    fun set(ctx: Context, on: Boolean): String {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = try {
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        } catch (e: Exception) { null } ?: return "No flashlight available on this device"
        return try { cm.setTorchMode(id, on); if (on) "Flashlight on" else "Flashlight off" }
        catch (e: Exception) { "Flashlight unavailable: ${e.message}" }
    }
}
