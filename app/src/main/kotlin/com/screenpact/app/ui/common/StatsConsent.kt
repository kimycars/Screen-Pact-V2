package com.screenpact.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.Stats
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.components.SpSwitch

/**
 * Interruptor de las estadísticas anónimas. Desactivado hasta que el usuario lo enciende.
 * Se usa en la bienvenida (para preguntar una vez) y en Ajustes (para cambiarlo cuando quiera).
 */
@Composable
fun StatsConsentCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Stats.isEnabled(context)) }
    SpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Estadísticas anónimas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    "Ayúdame a mejorar ScreenPact: una vez al día manda cuántos pactos y límites tienes y qué apps limitas. Nunca nombres, llaves ni minutos de uso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
            }
            Spacer(Modifier.width(Space.s))
            SpSwitch(checked = enabled, onCheckedChange = {
                enabled = it
                Stats.setEnabled(context, it)
            })
        }
    }
}
