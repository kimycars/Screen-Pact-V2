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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
 * Flujo:
 *  - Pestaña "Mi QR": genero un secreto aleatorio, lo muestro en un QR. Cuando mi amigo
 *    lo haya escaneado y guardado, pulso "Listo", introduzco el nombre del amigo y el
 *    secreto se guarda en mi extremo con ese nombre. Ambos extremos quedan con el MISMO
 *    secreto compartido.
 *  - Pestaña "Escanear": leo el QR del amigo y guardo automáticamente (su nombre va dentro del QR).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    var tab by remember { mutableIntStateOf(0) }
    var ownPayload by remember { mutableStateOf<PairingPayload?>(null) }

    LaunchedEffect(Unit) {
        val name = Prefs.getOwnName(context).ifBlank { "Yo" }
        ownPayload = PairingPayload(name = name, secret = TOTPManager.generateSecret())
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mi QR") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Escanear") })
            }
            when (tab) {
                0 -> ownPayload?.let { payload ->
                    OwnQrTab(payload) { friendName ->
                        scope.launch {
                            db.friendDao().insert(Friend(name = friendName, secret = payload.secret))
                            onDone()
                        }
                    }
                }
                1 -> ScanTab { scanned ->
                    val parsed = PairingPayload.decode(scanned)
                    if (parsed != null) {
                        scope.launch {
                            db.friendDao().insert(Friend(name = parsed.name, secret = parsed.secret))
                            onDone()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnQrTab(payload: PairingPayload, onSave: (String) -> Unit) {
    val bitmap = remember(payload) { QrUtils.generate(payload.encode()) }
    var friendName by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Que tu amigo escanee este QR:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR de emparejamiento",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )
        Spacer(Modifier.height(12.dp))
        Card {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Cuando tu amigo lo haya escaneado, escribe su nombre aquí y guarda. " +
                        "Quedaréis con el mismo secreto compartido.",
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = friendName,
            onValueChange = { friendName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre del amigo") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(friendName.trim()) },
            enabled = friendName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar amigo") }
    }
}

@Composable
private fun ScanTab(onResult: (String) -> Unit) {
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
}
