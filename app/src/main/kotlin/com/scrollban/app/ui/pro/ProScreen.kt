package com.scrollban.app.ui.pro

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.toArgb
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.scrollban.app.Brand
import com.scrollban.app.data.Plan
import com.scrollban.app.data.Pro
import com.scrollban.app.ui.common.Invite
import com.scrollban.app.ui.components.OutlineButton
import com.scrollban.app.data.Plans
import com.scrollban.app.data.rememberIsPro
import com.scrollban.app.ui.common.LicenseDialog
import com.scrollban.app.ui.components.IconAction
import com.scrollban.app.ui.components.PrimaryButton
import com.scrollban.app.ui.components.QuietButton
import com.scrollban.app.ui.components.SelectableCard
import com.scrollban.app.ui.components.Tag
import com.scrollban.app.ui.theme.Shapes
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space

/**
 * Pantalla de Pro. Se abre desde una función concreta ([ProFeature]) y la cabecera dice cuál es.
 * Planes con jerarquía: el anual destacado arriba, mensual y de por vida en pequeño, y el Pack
 * Pacto aparte. "Hazte Pro" abre el Payment Link de Stripe del plan elegido (Plans.kt); tras
 * pagar, la licencia llega por enlace (scrollban://license) o se pega a mano. Si el plan aún no
 * tiene enlace, el botón sale como "Muy pronto".
 */
@Composable
fun ProScreen(feature: ProFeature, onClose: () -> Unit) {
    val context = LocalContext.current
    val isPro = rememberIsPro()
    var selected by remember { mutableStateOf(Plans.DEFAULT) }
    var showLicense by remember { mutableStateOf(false) }
    val plan = Plans.byId(selected)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(top = Space.xs, bottom = Space.xl)
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconAction(Icons.Default.Close, "Cerrar", onClose, modifier = Modifier.align(Alignment.CenterEnd))
        }

        // ── Por qué estás aquí ──
        Box(
            Modifier.size(Sizes.button).clip(Shapes.pill).background(Sb.Superficie),
            contentAlignment = Alignment.Center
        ) { Icon(feature.icon, null, tint = Sb.Llave, modifier = Modifier.size(Sizes.iconL)) }
        Spacer(Modifier.height(Space.m))
        Text(
            buildAnnotatedString {
                val head = feature.title.removeSuffix("Pro")
                append(head)
                if (head != feature.title) withStyle(SpanStyle(color = Sb.Llave)) { append("Pro") }
            },
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(Space.s))
        Text(feature.body, style = MaterialTheme.typography.bodyMedium, color = Sb.TextoSuave)

        Spacer(Modifier.height(Space.xl))
        val others = ProFeature.entries.filter { it != feature && it.benefit.isNotEmpty() }
        if (feature != ProFeature.GENERAL) {
            Text("Y además:", style = MaterialTheme.typography.labelMedium, color = Sb.TextoSuave)
            Spacer(Modifier.height(Space.xxs))
        }
        others.forEach { Benefit(it.benefit) }

        // ── Planes ──
        Spacer(Modifier.height(Space.xxl))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Plans.byId(Plans.DEFAULT)?.let { p ->
                SelectableCard(selected == p.id, { selected = p.id }, Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Tag("Recomendado")
                    }
                    Spacer(Modifier.height(Space.xs))
                    Text(p.price, style = MaterialTheme.typography.headlineMedium)
                    Text(p.subtitle, style = MaterialTheme.typography.bodySmall, color = Sb.Pacto)
                }
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Plans.ALTERNATIVES.mapNotNull(Plans::byId).forEach { p ->
                    SmallPlan(p, selected == p.id, { selected = p.id }, Modifier.weight(1f).fillMaxHeight())
                }
            }
            Spacer(Modifier.height(Space.xs))
            Text("¿Hacéis el pacto juntos?", style = MaterialTheme.typography.labelMedium, color = Sb.TextoSuave)
            Plans.byId(Plans.PACK)?.let { p -> SmallPlan(p, selected == p.id, { selected = p.id }, Modifier.fillMaxWidth()) }
        }

        Spacer(Modifier.height(Space.xl))
        when {
            isPro -> PrimaryButton("Ya tienes Pro", {}, enabled = false)
            plan == null || plan.link.isEmpty() -> PrimaryButton("Muy pronto", {}, enabled = false)
            else -> PrimaryButton("Hazte Pro · ${plan.price}", {
                openCheckout(context, Pro.checkoutUrl(context, plan.link))
            })
        }
        if (isPro) {
            Pro.gifts(context).firstOrNull()?.let { gift ->
                Spacer(Modifier.height(Space.m))
                Text("La licencia de tu amigo está lista. Mándasela y que la pegue en Pro, \"Tengo un código de licencia\".", style = MaterialTheme.typography.bodyMedium, color = Sb.TextoSuave)
                Spacer(Modifier.height(Space.xs))
                OutlineButton("Mandar su licencia", {
                    Invite.shareText(
                        context,
                        "Aquí tienes tu licencia de ${Brand.NAME} Pro (Pack Pacto). En ${Brand.NAME}, ve a Pro, pulsa \"Tengo un código de licencia\" y pega esto:\n\n$gift",
                        "Mandar su licencia"
                    )
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (plan != null && plan.id != "lifetime" && !isPro) {
            Spacer(Modifier.height(Space.xs))
            Text(
                "Se renueva sola. Cancela cuando quieras y, si no te convence, te devuelvo el dinero en 14 días.",
                style = MaterialTheme.typography.bodySmall,
                color = Sb.TextoSuave,
                modifier = Modifier.fillMaxWidth()
            )
        }
        QuietButton("Tengo un código de licencia", { showLicense = true }, modifier = Modifier.align(Alignment.CenterHorizontally))
        QuietButton("Ahora no", onClose, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    if (showLicense) LicenseDialog(onDismiss = { showLicense = false }, onActivated = { showLicense = false })
}

/**
 * Abre el pago de Stripe en una pestaña que se superpone a la app (Custom Tab), con los colores
 * de ScrollBan. Al cerrarla se vuelve a la app, que recoge la licencia sola (Pro.claimPending).
 * Si el móvil no tiene un navegador compatible, se abre el navegador normal.
 */
private fun openCheckout(context: Context, url: String) {
    val uri = Uri.parse(url)
    runCatching {
        val colors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(Sb.Tinta.toArgb())
            .setNavigationBarColor(Sb.Tinta.toArgb())
            .build()
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colors)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setUrlBarHidingEnabled(true)
            .build()
            .launchUrl(context, uri)
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun Benefit(text: String) {
    Row(Modifier.padding(vertical = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = Sb.Pacto, modifier = Modifier.size(Sizes.iconM))
        Spacer(Modifier.width(Space.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Plan secundario: nombre, precio y una línea, sin destacar. */
@Composable
private fun SmallPlan(plan: Plan, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    SelectableCard(selected, onSelect, modifier) {
        Text(plan.title, style = MaterialTheme.typography.titleMedium)
        Text(plan.price, style = MaterialTheme.typography.titleLarge)
        Text(plan.subtitle, style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave)
    }
}
