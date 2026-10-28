package com.screenpact.app.ui.home

import com.screenpact.app.Brand
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.screenpact.app.data.Prefs
import com.screenpact.app.ui.common.PermissionCards
import com.screenpact.app.ui.common.rememberPermissionState
import com.screenpact.app.services.UsageMonitorService
import com.screenpact.app.ui.components.LogoTile
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.StepLine
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.components.SpTextField
import com.screenpact.app.ui.theme.Sp
import kotlinx.coroutines.launch
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Shapes
import com.screenpact.app.ui.common.StatsConsentCard
import com.screenpact.app.data.Stats
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.OutlineButton

/**
 * Bienvenida en tres pasos: presentación, nombre y permisos. Termina proponiendo el primer
 * pacto, que es el momento en que la app empieza a tener sentido.
 */
@Composable
fun OnboardingScreen(onFinished: (pairNow: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingSetup by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (Prefs.isSetupDone(context)) {
            onFinished(false)
            return@LaunchedEffect
        }
        name = Prefs.getOwnName(context)
        checkingSetup = false
    }
    if (checkingSetup) return

    BackHandler(enabled = step > 0) { step-- }

    fun finish(pairNow: Boolean) {
        scope.launch {
            Prefs.setOwnName(context, name.trim().ifEmpty { "Yo" })
            Prefs.setSetupDone(context)
            Stats.count(context, Stats.Event.ONBOARDING_DONE)
            UsageMonitorService.start(context)
            onFinished(pairNow)
        }
    }

    when (step) {
        0 -> WelcomeStep(onStart = { step = 1 })
        1 -> NameStep(name = name, onName = { name = it }, onNext = { step = 2 })
        2 -> PermissionsStep(onDone = { step = 3 })
        else -> FirstPactStep(onPair = { finish(true) }, onLater = { finish(false) })
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(top = Space.huge, bottom = Space.xl)) {
        LogoTile(Sizes.logoHero)
        Spacer(Modifier.height(Space.xxxl))
        Text(
            buildAnnotatedString {
                append("Tu amigo tiene la ")
                withStyle(SpanStyle(color = Sp.Llave)) { append("llave") }
                append(".")
            },
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(Space.l))
        Text(
            "Ponle límite a TikTok, Instagram o lo que te robe horas. Cuando te pases, solo tu amigo puede darte más tiempo.",
            style = MaterialTheme.typography.bodyLarge,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.xxxl))
        StepLine(number = 1, text = "Dinos cómo te llamas")
        StepLine(number = 2, text = "Da dos permisos (te explicamos por qué)")
        StepLine(number = 3, text = "Sella tu primer pacto con un amigo")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Empezar", onStart)
        Row(
            Modifier.fillMaxWidth().padding(top = Space.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, null, tint = Sp.TextoSuave, modifier = Modifier.size(Sizes.iconS))
            Spacer(Modifier.width(Space.xs))
            Text("Sin cuenta. Nada sale de tu móvil.", style = MaterialTheme.typography.bodySmall, color = Sp.TextoSuave)
        }
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(top = Space.huge, bottom = Space.xl)) {
        Text("¿Cómo te llamas?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Space.xs))
        Text(
            "Es el nombre que verá tu amigo cuando le toque darte la llave.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.xxl))
        SpTextField(value = name, onValueChange = { if (it.length <= 24) onName(it) }, label = "Tu nombre")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Siguiente", onNext, enabled = name.isNotBlank())
    }
}

@Composable
private fun PermissionsStep(onDone: () -> Unit) {
    val state = rememberPermissionState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.huge, bottom = Space.xl)
    ) {
        Text("Dos permisos", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Space.xs))
        Text(
            "Sin ellos ${Brand.NAME} no puede saber cuánto llevas ni pararte. Nada de esto sale de tu móvil.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.height(Space.xl))
        PermissionCards(state)
        Spacer(Modifier.height(Space.s))
        StatsConsentCard(onByDefault = true)
        Spacer(Modifier.height(Space.xxl))
        PrimaryButton("Siguiente", onDone, enabled = state.requiredOk)
    }
}

@Composable
private fun FirstPactStep(onPair: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.xl)
            .padding(top = Space.huge, bottom = Space.xl)
    ) {
        Text("Ahora, tu primer pacto", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Space.s))
        Text(
            "Queda con alguien de confianza que también tenga ${Brand.NAME}. Os escaneáis el QR y cada uno guarda la llave del otro.",
            style = MaterialTheme.typography.bodyLarge,
            color = Sp.TextoSuave
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Sellar el pacto ahora", onPair)
        Spacer(Modifier.height(Space.xs))
        OutlineButton("Invitar a un amigo", { Invite.share(context) })
        Spacer(Modifier.height(Space.xs))
        QuietButton("Lo haré luego", onLater, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
