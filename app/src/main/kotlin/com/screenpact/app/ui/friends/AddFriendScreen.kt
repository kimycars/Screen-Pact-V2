package com.screenpact.app.ui.friends

import com.screenpact.app.Brand
import com.screenpact.app.ui.components.ChoiceChip
import androidx.compose.foundation.selection.selectableGroup
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import com.screenpact.app.ui.components.BackButton
import com.screenpact.app.ui.components.PrimaryButton
import com.screenpact.app.ui.components.SpCard
import com.screenpact.app.ui.theme.Sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.screenpact.app.data.Prefs
import com.screenpact.app.data.crypto.PairingPayload
import com.screenpact.app.data.crypto.QrUtils
import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend
import kotlinx.coroutines.launch
import com.screenpact.app.ui.theme.Space
import com.screenpact.app.ui.theme.Sizes
import com.screenpact.app.ui.theme.Shapes
import com.screenpact.app.data.Pairing
import com.screenpact.app.data.crypto.PactKeys
import com.screenpact.app.ui.common.Invite
import com.screenpact.app.ui.components.OutlineButton
import com.screenpact.app.ui.components.QuietButton
import com.screenpact.app.ui.components.SpTextField

/**
 * Sellar un pacto. En persona: cada uno escanea el QR del otro. A distancia: se manda la
 * invitación por enlace y el amigo devuelve su respuesta (ver data/Pairing.kt). El QR y el
 * enlace son la misma invitación y no llevan nada secreto.
 */
@Composable
fun AddFriendScreen(onDone: () -> Unit, onOpenInvite: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Invitación propia de esta pantalla: se enseña como QR y se puede mandar como enlace.
    var mine by remember { mutableStateOf<Pair<PactKeys.Invite, PairingPayload>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    // Nombre del amigo cuyo QR ya escaneamos. La pantalla sigue abierta para que él pueda
    // escanear el nuestro.
    var pairedName by remember { mutableStateOf<String?>(null) }
    var pasted by remember { mutableStateOf("") }
    // Amigo con iPhone: misma invitación, pero el QR y el enlace abren la página web del pacto.
    var forIphone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { mine = Pairing.newInvite(context) }

    if (scanning) {
        ScanView(
            onResult = { text ->
                scanning = false
                val parsed = PairingPayload.decode(text)
                val screen = mine?.first
                if (parsed == null || screen == null) {
                    scanError = if (PairingPayload.isLegacy(text)) "Ese QR es de una versión antigua. Pide a tu amigo que actualice ${Brand.NAME}."
                    else "Ese QR no es un pacto de ${Brand.NAME}."
                } else scope.launch {
                    when (val r = Pairing.accept(context, parsed, remote = false, screenInvite = screen)) {
                        is Pairing.Result.Paired -> { pairedName = r.name; scanError = null }
                        is Pairing.Result.Failed -> scanError = r.message
                    }
                }
            },
            onBack = { scanning = false }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xs, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        BackButton(onDone)
        Column {
            Text("Sellad el pacto", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Space.xs))
            Text(
                if (forIphone) "Tu amigo no tiene que instalar nada: sella el pacto desde una página web y tú le pones el código de Tiempo de uso de su iPhone."
                else "Si estáis juntos, cada uno escanea el QR del otro. Si no, mándale la invitación por WhatsApp o donde habléis.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sp.TextoSuave
            )
            Spacer(Modifier.height(Space.m))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                ChoiceChip("Tiene Android", !forIphone, { forIphone = false })
                ChoiceChip("Tiene iPhone", forIphone, { forIphone = true })
            }
        }

        // ── En persona ──
        SpCard {
            Text(if (forIphone) "Que escanee tu QR con la cámara" else "Enséñale tu QR", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.m))
            mine?.second?.let { payload ->
                val bitmap = remember(payload, forIphone) { QrUtils.generate(if (forIphone) payload.iphoneLink() else payload.link()) }
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(Sizes.qr)
                        .clip(Shapes.field)
                        .background(Sp.Niebla)
                        .padding(Space.xs)
                ) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Tu QR del pacto", modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(Space.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Y escanea el suyo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pairedName != null) Text("Hecho", style = MaterialTheme.typography.labelMedium, color = Sp.Pacto)
            }
            Text(
                when {
                    pairedName != null && forIphone -> "Pacto sellado con $pairedName. Ahora ponle el código de Tiempo de uso: lo tienes en Juntos."
                    pairedName != null -> "Ya tienes la llave de $pairedName. Comprueba que también ha escaneado tu QR."
                    forIphone -> "Al sellarlo, su iPhone le enseña un QR. Escanéalo para cerrar el pacto."
                    else -> "Así el pacto va en los dos sentidos."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            if (pairedName == null) {
                Spacer(Modifier.height(Space.m))
                PrimaryButton("Abrir la cámara", { scanError = null; scanning = true })
            }
        }

        // ── A distancia ──
        SpCard {
            Text("¿No estáis juntos?", style = MaterialTheme.typography.titleMedium)
            Text(
                if (forIphone) "Mándale el enlace. Al abrirlo en su iPhone le saldrá una respuesta para que te la mande a ti. Un pacto con iPhone tarda 24 h en poder darte llaves."
                else "Mándale tu invitación. Al abrirla, su app le dará una respuesta para que te la mande a ti. Un pacto a distancia tarda 24 h en poder dar llaves.",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            Spacer(Modifier.height(Space.m))
            OutlineButton("Mandar invitación", { mine?.second?.let { Invite.sharePact(context, it, iphone = forIphone) } })
            Spacer(Modifier.height(Space.l))
            Text("¿Te ha llegado una invitación o una respuesta?", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Space.xs))
            SpTextField(value = pasted, onValueChange = { pasted = it }, label = "Pega aquí el enlace")
            Spacer(Modifier.height(Space.xs))
            QuietButton("Abrir", { if (pasted.isNotBlank()) onOpenInvite(pasted.trim()) }, color = Sp.Llave)
        }

        scanError?.let { msg -> Text(msg, style = MaterialTheme.typography.bodyMedium, color = Sp.Aviso) }

        if (pairedName != null) PrimaryButton("Listo", onDone)
    }
}

/**
 * Full-screen camera composable for scanning a friend's QR code.
 * Replaces the whole screen while active; [onBack] dismisses it without saving.
 */
@Composable
private fun ScanView(onResult: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCamera = it
    }
    LaunchedEffect(Unit) {
        if (!hasCamera) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCamera) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Concede permiso de cámara para escanear el QR de tu amigo.", color = Sp.Niebla)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        var done by remember { mutableStateOf(false) }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                DecoratedBarcodeView(ctx).apply {
                    decodeContinuous(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult?) {
                            if (done) return
                            val text = result?.text ?: return
                            done = true
                            pause()
                            onResult(text)
                        }

                        override fun possibleResultPoints(p0: MutableList<com.google.zxing.ResultPoint>?) {}
                    })
                    resume()
                }
            }
        )
        // Back button overlaid on top of the camera preview.
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(Space.xs)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar", tint = Sp.Niebla)
        }
    }
}
