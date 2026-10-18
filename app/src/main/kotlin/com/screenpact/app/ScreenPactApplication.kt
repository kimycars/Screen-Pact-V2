package com.screenpact.app

import android.app.Application
import com.screenpact.app.services.UsageMonitorService
import com.screenpact.app.util.PermissionsHelper
import com.screenpact.app.util.TamperGuard
import com.screenpact.app.util.UsageStatsHelper

class ScreenPactApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Arranca con los permisos concedidos, o si el pacto ya está armado: en ese caso el
        // servicio tiene que correr igualmente para registrar que se quitaron permisos.
        if ((UsageStatsHelper.hasUsageAccess(this) && PermissionsHelper.canDrawOverlays(this)) ||
            TamperGuard.isArmed(this)
        ) {
            UsageMonitorService.start(this)
        }
    }
}
