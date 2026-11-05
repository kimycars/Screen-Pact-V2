package com.scrollban.app.ui.common

import com.scrollban.app.Brand
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
import com.scrollban.app.data.Stats
import com.scrollban.app.ui.components.SbCard
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space
import com.scrollban.app.ui.components.SbSwitch

/**
 * Interruptor de las estadísticas anónimas. En la bienvenida ([onByDefault] = true) sale
 * encendido la primera vez y la persona puede apagarlo ahí mismo; en Ajustes muestra lo que haya.
 */
@Composable
fun StatsConsentCard(onByDefault: Boolean = false) {
    val context = LocalContext.current
    var enabled by remember {
        if (onByDefault && !Stats.wasAsked(context)) Stats.setEnabled(context, true)
        mutableStateOf(Stats.isEnabled(context))
    }
    SbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Estadísticas anónimas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    "Ayúdame a mejorar ${Brand.NAME}: una vez al día manda cuántos pactos y límites tienes y qué apps limitas. Nunca nombres, llaves ni minutos de uso. Puedes apagarlas cuando quieras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sb.TextoSuave
                )
            }
            Spacer(Modifier.width(Space.s))
            SbSwitch(checked = enabled, onCheckedChange = {
                enabled = it
                Stats.setEnabled(context, it)
            })
        }
    }
}
