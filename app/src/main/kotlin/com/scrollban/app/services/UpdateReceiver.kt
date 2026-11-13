package com.scrollban.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.scrollban.app.data.SelfUpdate

/**
 * Respuesta del instalador de Android a la actualización que lanza SelfUpdate: le pide a la
 * persona que confirme y, si falla (por ejemplo, otra firma), la tarjeta ofrece la web.
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirm == null) { SelfUpdate.failed(); return }
                runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { SelfUpdate.failed() }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // Android cierra la app; BootReceiver avisa.
            PackageInstaller.STATUS_FAILURE_ABORTED -> SelfUpdate.reset() // Pulsó "Cancelar".
            else -> SelfUpdate.failed()
        }
    }
}
