package com.scrollban.app

import android.app.Application
import com.scrollban.app.services.UsageMonitorService
import com.scrollban.app.util.PermissionsHelper
import com.scrollban.app.util.TamperGuard
import com.scrollban.app.util.UsageStatsHelper

class ScrollBanApplication : Application() {
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
