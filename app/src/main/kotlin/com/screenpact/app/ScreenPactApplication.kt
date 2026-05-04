package com.screenpact.app

import android.app.Application
import com.screenpact.app.services.UsageMonitorService
import com.screenpact.app.util.PermissionsHelper
import com.screenpact.app.util.UsageStatsHelper

class ScreenPactApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Solo arrancamos el servicio si los permisos están concedidos.
        if (UsageStatsHelper.hasUsageAccess(this) && PermissionsHelper.canDrawOverlays(this)) {
            UsageMonitorService.start(this)
        }
    }
}
