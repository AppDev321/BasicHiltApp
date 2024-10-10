package com.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.core.BaseApplication
import com.core.extensions.TAG
import com.core.utils.AppLogger


class ConnectionReceiver : BroadcastReceiver() {
    companion object {
        val ACTION_SIMPLE_ENQUE = "com.is.core.app.SIMPLE_ENQUE_CONNECTION_WORKER"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action
        if (action != null) {
            when (action) {
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.REBOOT",
                "android.intent.action.ACTION_BOOT_COMPLETED",
                "android.intent.action.QUICKBOOT_POWERON",
                "com.htc.intent.action.QUICKBOOT_POWERON",
                ACTION_SIMPLE_ENQUE,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED
                -> {
                    tryToStartXmppService()
                }
            }
        }

    }

    private fun tryToStartXmppService() {
        AppLogger.e(
            TAG, "tryToStartXmppService")
       BaseApplication.instance.enqueueOnetimeConnectionWorker()
    }
}