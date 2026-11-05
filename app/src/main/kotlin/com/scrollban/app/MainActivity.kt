package com.scrollban.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.scrollban.app.data.AppIcons
import com.scrollban.app.data.Pro
import com.scrollban.app.ui.friends.IncomingPact
import com.scrollban.app.ui.MainNav
import com.scrollban.app.ui.theme.ScrollBanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLicenseLink(intent)
        IncomingPact.from(intent)
        setContent {
            ScrollBanTheme {
                Surface { MainNav() }
            }
        }
    }

    private var claiming: Job? = null

    // Al volver de pagar en Stripe se recoge la licencia aunque la página de gracias no se haya
    // abierto. El aviso de Stripe puede tardar unos segundos, así que se pregunta varias veces.
    override fun onResume() {
        super.onResume()
        if (!Pro.hasPending(this) || claiming?.isActive == true) return
        claiming = lifecycleScope.launch {
            repeat(10) {
                if (Pro.claimPending(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Ya tienes ${Brand.NAME} Pro. ¡Gracias!", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (!Pro.hasPending(this@MainActivity)) return@launch
                delay(3_000)
            }
        }
    }

    // Al salir de la app (botón de inicio o recientes, o cerrándola) se pone el icono elegido en
    // Ajustes. No se hace antes porque Android cerraría la app en ese momento (ver AppIcons).
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        AppIcons.applyPending(this)
    }

    override fun onDestroy() {
        if (isFinishing) AppIcons.applyPending(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLicenseLink(intent)
        IncomingPact.from(intent)
    }

    /** scrollban://license?token=… : enlace que entrega la licencia de Pro tras pagar. */
    private fun handleLicenseLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!Brand.isAppScheme(uri.scheme) || uri.host != "license") return
        val token = uri.getQueryParameter("token").orEmpty()
        val ok = Pro.activate(this, token)
        Toast.makeText(
            this,
            if (ok) "Ya tienes ${Brand.NAME} Pro. ¡Gracias!" else "Ese enlace de licencia no es válido. Pega el código a mano en Ajustes.",
            Toast.LENGTH_LONG
        ).show()
    }
}
