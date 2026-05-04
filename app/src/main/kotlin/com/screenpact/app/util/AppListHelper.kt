package com.screenpact.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object AppListHelper {
    /** Apps lanzables (no del sistema sin UI). */
    fun listLaunchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, 0)
        val seen = HashSet<String>()
        val out = ArrayList<InstalledApp>(resolved.size)
        for (r in resolved) {
            val pkg = r.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!seen.add(pkg)) continue
            val info: ApplicationInfo = r.activityInfo.applicationInfo
            out.add(
                InstalledApp(
                    packageName = pkg,
                    label = info.loadLabel(pm).toString(),
                    icon = runCatching { info.loadIcon(pm) }.getOrNull()
                )
            )
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    fun loadLabel(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
