package com.screenpact.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            // Tras actualizar la app el servicio muere; sin esto el hueco contaría como trampa.
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            UsageMonitorService.start(context)
        }
    }
}
