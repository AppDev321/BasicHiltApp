package com.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.core.utils.AppLogger

class BackgroundService: LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         super.onStartCommand(intent, flags, startId)

        schedulePostConnectivityChange()
        return START_STICKY
    }

    private fun schedulePostConnectivityChange() {

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val triggerAtMillis: Long =
            SystemClock.elapsedRealtime() + 1 * 1000
        val intent = Intent(this, ConnectionReceiver::class.java)
        intent.action = ConnectionReceiver.ACTION_SIMPLE_ENQUE
        try {
            val pendingIntent =
                PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: RuntimeException) {
           AppLogger.e(
                "BackgroundService",
                "unable to schedule alarm for post connectivity change",
                e
            )
        }
    }

}