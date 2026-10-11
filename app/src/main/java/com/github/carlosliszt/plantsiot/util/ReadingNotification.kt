package com.github.carlosliszt.plantsiot.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.carlosliszt.plantsiot.R
import com.github.carlosliszt.plantsiot.model.PlantReading

object ReadingNotification {
    private const val CHANNEL_ID = "mqtt_readings_alerts"

    fun prepare(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Leituras MQTT",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Alertas de novas leituras das plantas"
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 250, 150, 250)
            channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun requestPermission(context: Context, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            (context as? android.app.Activity)?.requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }

    fun show(context: Context, plantName: String, reading: PlantReading) {
        if (reading.timestamp == 0L) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val status = PlantHealth.status(reading)
        val text = "Temp. %.1f°C • Solo %.1f%% • Ar %.1f%% • pH %.1f".format(
            reading.temperatureC, reading.soilMoisture, reading.airHumidity, reading.ph
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.potted_plant_24px)
            .setContentTitle("$plantName: $status (${reading.healthScore}/100)")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nStatus: $status"))
            .setColor(PlantHealth.color(reading))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(reading.timestamp.hashCode(), notification)
    }
}
