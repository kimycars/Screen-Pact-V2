package com.screenpact.app.ui.friends

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
import com.screenpact.app.data.Stats

/**
 * Pantalla de emparejamiento con secretos direccionales.
 *
 * Cada lado genera su propio secreto de verificación (verifyKey) y lo muestra en su QR.
 * Al escanear el QR del amigo se obtiene el generateKey con el que produciremos los
 * códigos que desbloquean AL AMIGO (no a nosotros mismos).
 *
 * El flujo requiere que ambas personas se escaneen mutuamente:
 *   Paso 1 — Enseña TU QR al amigo (él lo escanea → obtiene tu verifyKey como su generateKey).
 *   Paso 2 — Escanea el QR del amigo (tú obtienes su verifyKey como tu generateKey).
 */
@Composable
fun AddFriendScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    // Generated once per screen session: this is OUR verify key.
    // We show it as a QR; the friend scans it and stores it as *their* generateKey for us.
    var myVerifyKey by remember { mutableStateOf<ByteArray?>(null) }
    var ownPayload by remember { mutableStateOf<PairingPayload?>(null) }

    var myInstallId by remember { mutableStateOf<String?>(null) }

    // When true, shows the full-screen camera view for scanning the friend's QR.
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    // Nombre del amigo cuyo QR ya escaneamos. La pantalla sigue abierta para que él pueda
    // escanear el nuestro: si se cerrase, nuestro QR (y su clave) se perderían.
    var pairedName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val key = TOTPManager.generateSecret()
        val installId = Prefs.getInstallId(context)
        myVerifyKey = key
        myInstallId = installId
        val name = Prefs.getOwnName(context).ifBlank { "Yo" }
        ownPayload = PairingPayload(name = name, secret = key, installId = installId)
    }

    // When scanning is active, render just the camera — no Scaffold (it takes the full screen).
    if (scanning) {
        ScanView(
            onResult = { text ->
                val parsed = PairingPayload.decode(text)
                val vk = myVerifyKey
                val ownId = myInstallId
                if (parsed == null || vk == null || ownId == null) {
                    scanError = "Ese QR no es un código de emparejamiento de ScreenPact."
                    scanning = false
                } else {
                    scope.launch {
                        val existing = db.friendDao().getAll()
                        // Emparejarse con uno mismo daría la misma clave para generar y verificar,
                        // y los códigos de "Amigos" desbloquearían tu propio bloqueo.
                        scanError = when {
                            parsed.installId.isBlank() ->
                                "Este QR es de una versión antigua. Pide a tu amigo que actualice ScreenPact."
                            parsed.installId == ownId || parsed.secret.contentEquals(vk) ->
                                "Ese es tu propio QR. Tienes que escanear el del móvil de tu amigo."
                            existing.any { f ->
                                parsed.secret.contentEquals(f.generateKey) || parsed.secret.contentEquals(f.verifyKey)
                            } ->
                                "Ya estás emparejado con ese QR. Pide a tu amigo que salga y vuelva a entrar en 'Sellar un pacto'."
                            else -> null
                        }
                        if (scanError != null) {
                            scanning = false
                            return@launch
                        }
                        db.friendDao().insert(
                            Friend(
                                name = parsed.name,
                                generateKey = parsed.secret,  // friend's verifyKey → our generateKey
                                verifyKey = vk                // our secret → friend stores as their generateKey
                            )
                        )
                        pairedName = parsed.name
                        Stats.count(context, Stats.Event.PACT_SEALED)
                        scanning = false
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
                "Juntaos con los dos móviles. Cada uno escanea el QR del otro, en el orden que queráis.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sp.TextoSuave
            )
        }

        SpCard {
            Text("Enséñale tu QR", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.xxs))
            Text(
                "Con él, tu amigo podrá darte la llave cuando te pases.",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            Spacer(Modifier.height(Space.m))
            ownPayload?.let { payload ->
                val bitmap = remember(payload) { QrUtils.generate(payload.encode()) }
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(Sizes.qr)
                        .clip(Shapes.field)
                        .background(Sp.Niebla)
                        .padding(Space.xs)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Tu QR de emparejamiento",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        SpCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Escanea el suyo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pairedName != null) Text("Hecho", style = MaterialTheme.typography.labelMedium, color = Sp.Pacto)
            }
            Spacer(Modifier.height(Space.xxs))
            Text(
                if (pairedName != null) "Ya tienes la llave de $pairedName."
                else "Así tú también tendrás su llave. Sin este paso, el pacto solo va en un sentido.",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            if (pairedName == null) {
                Spacer(Modifier.height(Space.m))
                PrimaryButton("Abrir la cámara", { scanError = null; scanning = true })
            }
        }

        scanError?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = Sp.Aviso)
        }

        if (pairedName != null) {
            Text(
                "Antes de cerrar, comprueba que $pairedName también ha escaneado tu QR. Si sales de esta pantalla, tu QR cambia.",
                style = MaterialTheme.typography.bodySmall,
                color = Sp.TextoSuave
            )
            PrimaryButton("Listo", onDone)
        }
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar", tint = Color.White)
        }
    }
}
