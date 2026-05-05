package com.screenpact.app.ui.friends

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    // Generated once per screen session: this is OUR verify key.
    // We show it as a QR; the friend scans it and stores it as *their* generateKey for us.
    var myVerifyKey by remember { mutableStateOf<ByteArray?>(null) }
    var ownPayload by remember { mutableStateOf<PairingPayload?>(null) }

    // When true, shows the full-screen camera view for scanning the friend's QR.
    var scanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val key = TOTPManager.generateSecret()
        myVerifyKey = key
        val name = Prefs.getOwnName(context).ifBlank { "Yo" }
        ownPayload = PairingPayload(name = name, secret = key)
    }

    // When scanning is active, render just the camera — no Scaffold (it takes the full screen).
    if (scanning) {
        ScanView(
            onResult = { text ->
                val parsed = PairingPayload.decode(text)
                val vk = myVerifyKey
                if (parsed != null && vk != null) {
                    scope.launch {
                        db.friendDao().insert(
                            Friend(
                                name = parsed.name,
                                generateKey = parsed.secret,  // friend's verifyKey → our generateKey
                                verifyKey = vk                // our secret → friend stores as their generateKey
                            )
                        )
                        onDone()
                    }
                }
            },
            onBack = { scanning = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emparejar amigo") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: Show own QR
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Paso 1 — Enseña este QR a tu amigo", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tu amigo lo escanea desde su pantalla de 'Emparejar amigo'. " +
                            "Esto le permite generarte códigos para desbloquearte.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    ownPayload?.let { payload ->
                        val bitmap = remember(payload) { QrUtils.generate(payload.encode()) }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Mi QR de emparejamiento",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                    }
                }
            }

            // Step 2: Scan friend's QR
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Paso 2 — Escanea el QR de tu amigo", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tu amigo te muestra su QR (el que aparece en su Paso 1). " +
                            "Esto te permite generarle códigos para desbloquearlo a él.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { scanning = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Escanear QR del amigo") }
                }
            }

            Text(
                "Ambas personas deben completar los dos pasos. " +
                    "Los códigos que ves en 'Amigos' solo desbloquean al otro, no a ti mismo.",
                fontSize = 12.sp
            )
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
            Text("Concede permiso de cámara para escanear el QR.")
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
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar", tint = Color.White)
        }
    }
}
