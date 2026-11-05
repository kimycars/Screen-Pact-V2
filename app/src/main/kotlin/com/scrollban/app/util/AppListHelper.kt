package com.scrollban.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val label: String
)

/** App que usas mucho y aún no tiene límite, con su media diaria real y un límite propuesto. */
data class Suggestion(val app: InstalledApp, val averageMs: Long) {
    /** Un 30 % menos que tu media, redondeado a 5 minutos, nunca menos de 10. */
    val suggestedMinutes: Int get() = (((averageMs / 60_000L) * 7 / 10) / 5 * 5).toInt().coerceAtLeast(10)
}

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
                    label = info.loadLabel(pm).toString()
                )
            )
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    /**
     * Las apps que más usas (media de los últimos 7 días) entre las que se pueden abrir, sin las
     * de [exclude] ni el escritorio. Solo las que pasan de [minAverageMs] al día.
     */
    fun suggestions(context: Context, exclude: Set<String>, max: Int = 3, minAverageMs: Long = 10 * 60_000L): List<Suggestion> {
        val pm = context.packageManager
        val home = pm.queryIntentActivities(
            android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.toSet()
        val launchable = listLaunchable(context).associateBy { it.packageName }
        return UsageStatsHelper.dailyAverageMs(context)
            .filter { (pkg, avg) -> pkg in launchable && pkg !in exclude && pkg !in home && avg >= minAverageMs }
            .entries.sortedByDescending { it.value }
            .take(max)
            .map { Suggestion(launchable.getValue(it.key), it.value) }
    }

    fun loadLabel(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
