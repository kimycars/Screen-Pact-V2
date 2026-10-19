package com.screenpact.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.screenpact.app.BuildConfig
import com.screenpact.app.data.AppIcons
import com.screenpact.app.data.Plans
import com.screenpact.app.data.Pro
import com.screenpact.app.data.StrictMode
import com.screenpact.app.data.rememberIsPro
import com.screenpact.app.ui.common.FriendCodeDialog
import com.screenpact.app.ui.common.LicenseDialog
import com.screenpact.app.ui.common.StatsConsentCard
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.ChipButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.ScreenTitle
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.theme.Sp
import com.screenpact.app.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.screenpact.app.ui.components.SpSwitch
import com.screenpact.app.ui.components.SelectableCard
import com.screenpact.app.ui.pro.ProFeature
import com.screenpact.app.ui.theme.Shapes
import com.screenpact.app.ui.theme.Sizes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

/** Ajustes: Pro, modo estricto, icono, estadísticas y privacidad. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPro: (ProFeature) -> Unit) {
    val context = LocalContext.current
    val isPro = rememberIsPro()
    var showLicense by remember { mutableStateOf(false) }
    var strictActive by remember { mutableStateOf(StrictMode.isActive(context)) }
    var askKeyForStrict by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf(AppIcons.chosen(context)) }
    val enableStrict = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        strictActive = StrictMode.isActive(context)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Space.screen, vertical = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.s)
    ) {
        item { BackButton(onBack) }
        item { ScreenTitle("Ajustes") }

        // ── Pro ──
        item {
            SpCard {
                val license = Pro.license(context)
                if (isPro) {
                    Text("Tienes ScreenPact Pro", style = MaterialTheme.typography.titleMedium, color = Sp.Llave)
                    Text(
                        when {
                            license == null -> "Pro de prueba (solo en la versión de pruebas)."
                            license.exp == 0L -> "${Plans.byId(license.plan)?.title ?: "Pro"}: para siempre."
                            else -> "${Plans.byId(license.plan)?.title ?: "Pro"}: se renueva el ${SimpleDateFormat("d/M/yyyy", Locale("es", "ES")).format(Date(license.exp))}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Sp.TextoSuave
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ScreenPact Pro", style = MaterialTheme.typography.titleMedium)
                            Text("Todas las apps, horarios, rachas y modo estricto.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
                        }
                        Spacer(Modifier.width(Space.s))
                        ChipButton("Ver Pro", { onOpenPro(ProFeature.GENERAL) })
                    }
                }
                Spacer(Modifier.height(Space.xs))
                QuietButton("Tengo un código de licencia", { showLicense = true })
                if (BuildConfig.DEBUG) {
                    var dev by remember { mutableStateOf(Pro.isDevPro(context)) }
                    ToggleRow("Pro de prueba (solo versión de pruebas)", dev) { dev = it; Pro.setDevPro(context, it) }
                }
            }
        }

        // ── Modo estricto (Pro) ──
        item {
            SpCard {
                ToggleRow(
                    title = "Modo estricto",
                    checked = strictActive,
                    subtitle = if (isPro) "Para desinstalar ScreenPact primero hay que quitarlo, y quitarlo pide la llave de un amigo."
                    else "Con Pro: desinstalar ScreenPact pide la llave de un amigo."
                ) { on ->
                    when {
                        !on -> askKeyForStrict = true
                        isPro -> enableStrict.launch(StrictMode.enableIntent(context))
                        else -> onOpenPro(ProFeature.STRICT)
                    }
                }
            }
        }

        // ── Icono (Pro) ──
        item {
            SpCard {
                Text("Icono de la app", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !isPro -> "Con Pro puedes elegir otro icono."
                        icon != AppIcons.current(context) -> "Se cambia en tu escritorio al salir de ScreenPact."
                        else -> "El que verás en tu escritorio."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Sp.TextoSuave
                )
                Spacer(Modifier.height(Space.s))
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    AppIcons.all.forEach { option ->
                        IconChoice(option, selected = option.alias == icon, modifier = Modifier.weight(1f)) {
                            if (!isPro) onOpenPro(ProFeature.ICONS)
                            else {
                                AppIcons.choose(context, option.alias)
                                icon = option.alias
                            }
                        }
                    }
                }
            }
        }

        item { StatsConsentCard() }

        item {
            QuietButton("Privacidad y condiciones", {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
        }
        item { Text("ScreenPact ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave) }
    }

    if (showLicense) LicenseDialog(onDismiss = { showLicense = false }, onActivated = { showLicense = false })

    if (askKeyForStrict) {
        FriendCodeDialog(
            reason = "Quitar el modo estricto afloja tu pacto, así que necesitas la llave de un amigo.",
            onDismiss = { askKeyForStrict = false },
            onVerified = {
                askKeyForStrict = false
                StrictMode.disable(context)
                strictActive = false
            }
        )
    }
}

private const val PRIVACY_URL = "https://www.kimycars.com/screenpact/privacidad"

/** Fondo de cada icono, el mismo que su adaptive-icon de res/drawable. */
private fun iconBackground(alias: String) = when (alias) {
    "LauncherNoche" -> Sp.Tinta
    "LauncherMenta" -> Sp.Pacto
    else -> Sp.Llave
}

/** Un icono de la app para elegir: cómo se verá y su nombre. */
@Composable
private fun IconChoice(option: AppIcons.Icon, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    SelectableCard(selected, onSelect, modifier) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(Sizes.button)
                .clip(Shapes.pill)
                .background(iconBackground(option.alias))
        ) {
            Image(painterResource(option.foreground), null, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            option.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Sp.Llave else Sp.Niebla,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave) }
        }
        Spacer(Modifier.width(Space.s))
        SpSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
