package com.screenpact.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.screenpact.app.data.AppIcons
import com.screenpact.app.data.Pro
import com.screenpact.app.ui.friends.IncomingPact
import com.screenpact.app.ui.MainNav
import com.screenpact.app.ui.theme.ScreenPactTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLicenseLink(intent)
        IncomingPact.from(intent)
        setContent {
            ScreenPactTheme {
                Surface { MainNav() }
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

    /** screenpact://license?token=… : enlace que entrega la licencia de Pro tras pagar. */
    private fun handleLicenseLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "screenpact" || uri.host != "license") return
        val token = uri.getQueryParameter("token").orEmpty()
        val ok = Pro.activate(this, token)
        Toast.makeText(
            this,
            if (ok) "Ya tienes ${Brand.NAME} Pro. ¡Gracias!" else "Ese enlace de licencia no es válido. Pega el código a mano en Ajustes.",
            Toast.LENGTH_LONG
        ).show()
    }
}
