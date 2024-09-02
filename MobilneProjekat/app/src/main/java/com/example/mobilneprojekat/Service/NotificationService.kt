
package com.example.mobilneprojekat.Service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.mobilneprojekat.R
import com.example.mobilneprojekat.MainActivity
import com.google.android.gms.location.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.ConcurrentHashMap

class NotificationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = Firebase.firestore

    private val lastNotificationTimes = ConcurrentHashMap<String, Long>()
    private val notificationInterval = 10 * 1 * 1000 //30 minuta

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()

        startForeground(1, getNotification("Tracking location..."))

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    checkNearbyObjects(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000  // svakih 10 sec za dobijanje lokacije
            fastestInterval = 5000  // Najkraći interval izmedju 2 azuriranja
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun checkNearbyObjects(location: Location) {
        firestore.collection("objects")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val lat = document.getDouble("latitude")
                    val lon = document.getDouble("longitude")
                    val name = document.getString("name")
                    val phone = document.getString("phone")
                    val type = document.getString("type")
                    if (lat != null && lon != null) {
                        val objectLocation = Location("").apply {
                            latitude = lat
                            longitude = lon
                        }
                        val distance = location.distanceTo(objectLocation)
                        if (distance < 100) {
                            val currentTime = System.currentTimeMillis()
                            val lastNotificationTime = lastNotificationTimes[document.id] ?: 0
                            /*ovde dole je ona provera od 20 minuta pre slanja notification za isti objekat*/
                            if (currentTime - lastNotificationTime > notificationInterval) {
                                sendNotification("Object Nearby", "You're near $name - $type.\nPhone: $phone. Check it out!")
                                lastNotificationTimes[document.id] = currentTime
                            }
                        }
                    }
                }
            }
    }

    private fun sendNotification(title: String, content: String) {
        val intent = Intent(this, MainActivity::class.java)
        //omogucava povratak u aplikaciju klikom na notifikaciju
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_restaurant_24)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    //za prikaz notifikacija
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Location Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_restaurant_24)
            .setContentIntent(pendingIntent)
            .build()
    }
}
