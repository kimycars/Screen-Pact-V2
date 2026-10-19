package com.screenpact.app.ui.pro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.screenpact.app.ui.components.IconAction
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Strokes
import com.screenpact.app.ui.theme.Shapes
import com.screenpact.app.data.Plans
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.screenpact.app.data.rememberIsPro
import com.screenpact.app.ui.common.LicenseDialog

/**
 * Pantalla de Pro. "Hazte Pro" abre el Payment Link de Stripe del plan elegido (Plans.kt); tras
 * pagar, la licencia llega por enlace (screenpact://license) o se pega a mano. Si el plan aún no
 * tiene enlace configurado, el botón sale como "Muy pronto".
 */
@Composable
fun ProScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val isPro = rememberIsPro()
    var plan by remember { mutableStateOf(Plans.DEFAULT) }
    var showLicense by remember { mutableStateOf(false) }
    val link = Plans.byId(plan)?.link.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xs, bottom = Space.xl)
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconAction(Icons.Default.Close, "Cerrar", onClose, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Text(
            buildAnnotatedString {
                append("Llévalo más lejos con ")
                withStyle(SpanStyle(color = Sp.Llave)) { append("Pro") }
            },
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(Space.s))
        Text("Mensual, anual o de por vida. Tú eliges.", style = MaterialTheme.typography.bodyMedium, color = Sp.TextoSuave)

        Spacer(Modifier.height(Space.xxl))
        listOf(
            "Todas las apps que quieras",
            "Horarios: Instagram cerrado de 0 a 8 h",
            "Rachas e historial de tu semana",
            "Modo estricto: desinstalar pide llave",
        ).forEach { Benefit(it) }

        Spacer(Modifier.height(Space.xxxl))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Plans.all.forEach { p -> PlanOption(p.title, p.subtitle, p.price, plan == p.id) { plan = p.id } }
        }
        Spacer(Modifier.height(Space.l))
        when {
            isPro -> PrimaryButton("Ya tienes Pro", {}, enabled = false)
            link.isEmpty() -> PrimaryButton("Muy pronto", {}, enabled = false)
            else -> PrimaryButton("Hazte Pro", {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
        }
        QuietButton("Tengo un código de licencia", { showLicense = true }, modifier = Modifier.align(Alignment.CenterHorizontally))
        QuietButton("Ahora no", onClose, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    if (showLicense) LicenseDialog(onDismiss = { showLicense = false }, onActivated = { showLicense = false })
}

@Composable
private fun Benefit(text: String) {
    Row(Modifier.padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = Sp.Pacto, modifier = Modifier.size(Sizes.iconL))
        Spacer(Modifier.width(Space.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlanOption(title: String, subtitle: String, price: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .border(BorderStroke(if (selected) Strokes.bold else Strokes.hairline, if (selected) Sp.Llave else Sp.Borde), Shapes.card)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Sp.Llave))
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
        }
        Text(price, style = MaterialTheme.typography.titleLarge)
    }
}
