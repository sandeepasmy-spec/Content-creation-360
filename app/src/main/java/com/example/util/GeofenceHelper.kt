package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.wifi.WifiManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class OfficeVerificationResult(
    val isWithinGeofence: Boolean,
    val distanceMeters: Float,
    val latitude: Double,
    val longitude: Double,
    val detectedWifiSsid: String,
    val isWifiMatched: Boolean,
    val errorMessage: String? = null
)

object GeofenceHelper {

    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }

    @Suppress("DEPRECATION")
    fun getCurrentWifiSsid(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val rawSsid = wifiManager?.connectionInfo?.ssid ?: ""
            val cleaned = rawSsid.removePrefix("\"").removeSuffix("\"")
            if (cleaned == "<unknown ssid>" || cleaned.isBlank()) "Unavailable / Mobile Data" else cleaned
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getSingleCurrentLocation(context: Context): Location? {
        return suspendCancellableCoroutine { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(location)
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last -> cont.resume(last) }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
            } catch (_: Exception) {
                cont.resume(null)
            }
        }
    }

    fun generateDynamicQrToken(nowMillis: Long = System.currentTimeMillis()): String {
        val bucket = nowMillis / (5 * 60 * 1000L) // Rotates every 5 minutes
        val code = ((bucket * 7919L + 104729L) % 900000L + 100000L).toInt()
        return "SF360-OFFICE-$code"
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
    }

    fun formatTime(millis: Long?): String {
        if (millis == null || millis <= 0L) return "--:--"
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "Never"
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(millis))
    }

    fun formatDateShort(millis: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(millis))
    }
}
