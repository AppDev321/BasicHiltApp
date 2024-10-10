package com.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.core.BaseApplication
import com.core.extensions.TAG
import com.core.utils.AppLogger


class ConnectionWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {

        AppLogger.e(
            TAG, "Connection Worker initiated")
        tryToStartService(BaseApplication.instance)

        return Result.success()
    }

    private fun tryToStartService(context: Context) {
        val intent = Intent(context, BackgroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            context,
            101,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeToWake = SystemClock.elapsedRealtime() + 2 * 1000
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            timeToWake,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,  // repeat every 15 minutes
            pendingIntent
        )
    }

    companion object {
        val TAG: String = ConnectionWorker::class.java.simpleName

        const val NAME_PERIODIC: String = "ConnectionWorker-Periodic"
        const val NAME_ONETIME: String = "ConnectionWorker-OneTime"
    }
}
