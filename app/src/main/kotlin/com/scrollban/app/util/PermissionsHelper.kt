package com.scrollban.app.util

import com.scrollban.app.Brand
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Accesos directos a los ajustes del sistema que ScrollBan necesita. Cada uno intenta abrir la
 * pantalla más concreta posible y, si el fabricante no la tiene, cae a la genérica.
 */
object PermissionsHelper {

    /**
     * Ajustes "restringidos" para apps instaladas desde un APK suelto: Android 13 empezó con
     * accesibilidad y notificaciones, y Android 15 añadió el acceso de uso y mostrarse encima
     * de otras apps (los dos que usamos). Algunas capas (MIUI/HyperOS) ya lo aplicaban antes.
     * Se desbloquea desde la ficha de la app, menú ⋮, "Permitir ajustes restringidos".
     * Instalando desde una tienda (Zapstore, Play) no aparece.
     */
    val mayHaveRestrictedSettings: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Ajuste propio del fabricante para que no cierre ScrollBan en segundo plano (o lo arranque
     * al encender el móvil). Null si el fabricante no tiene nada especial. Nombres de pantalla
     * sacados de dontkillmyapp.com y de las librerías que abren estos ajustes (AutoStarter):
     * cambian entre versiones, por eso cada uno tiene varios candidatos y, si ninguno abre,
     * se cae a la ficha de la app.
     */
    val backgroundSetting: BackgroundSetting?
        get() = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> BackgroundSetting(
                "Inicio automático",
                "Busca ${Brand.NAME} y activa su interruptor.",
                listOf(cn("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            )
            "huawei", "honor" -> BackgroundSetting(
                "Inicio de aplicaciones",
                "Busca ${Brand.NAME}, desactiva \"Gestionar automáticamente\" y deja activadas las tres opciones.",
                listOf(
                    cn("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                    cn("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                    cn("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                )
            )
            "oppo", "realme" -> BackgroundSetting(
                "Inicio automático",
                "Busca ${Brand.NAME} y actívalo. En la ficha de la app, permite también la actividad en segundo plano.",
                listOf(
                    cn("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    cn("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                    cn("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                )
            )
            "vivo", "iqoo" -> BackgroundSetting(
                "Inicio automático",
                "Busca ${Brand.NAME} y activa su interruptor.",
                listOf(
                    cn("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                    cn("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                    cn("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                )
            )
            "oneplus" -> BackgroundSetting(
                "Inicio automático",
                "Busca ${Brand.NAME} y actívalo.",
                listOf(cn("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
            )
            "samsung" -> BackgroundSetting(
                "Apps que nunca se suspenden",
                "En Batería, entra en \"Límites de uso en segundo plano\" y añade ${Brand.NAME} a \"Apps que nunca se suspenden\".",
                listOf(cn("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            else -> null
        }

    data class BackgroundSetting(val title: String, val help: String, val screens: List<Intent>)

    /** Intenta ir directo al interruptor de ScrollBan; si el móvil no lo admite, abre la lista. */
    fun openUsageAccessSettings(context: Context) {
        val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + context.packageName))
        if (!tryStart(context, direct)) tryStart(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val direct = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
        if (!tryStart(context, direct)) tryStart(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }

    /** Ficha de ScrollBan en Ajustes: desde su menú ⋮ se permiten los ajustes restringidos. */
    fun openAppDetails(context: Context) {
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Pide directamente quitar la optimización de batería (un solo "Permitir"); si no, abre la lista. */
    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName))
        if (!tryStart(context, direct)) tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /** Abre el ajuste del fabricante; si ninguna de sus pantallas existe en este móvil, la ficha de la app. */
    fun openBackgroundSetting(context: Context) {
        val opened = backgroundSetting?.screens?.any { tryStart(context, it) } ?: false
        if (!opened) openAppDetails(context)
    }

    private fun cn(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

    private fun tryStart(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
