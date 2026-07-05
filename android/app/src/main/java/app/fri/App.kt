package app.fri

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                TRACKING_CHANNEL,
                "Route recording",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        const val TRACKING_CHANNEL = "tracking"
    }
}
