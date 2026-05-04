package com.screenpact.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateCodeScreen(friendId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    var friend by remember { mutableStateOf<Friend?>(null) }
    var code by remember { mutableStateOf("------") }
    var seconds by remember { mutableIntStateOf(30) }

    LaunchedEffect(friendId) {
        friend = db.friendDao().getAll().firstOrNull { it.id == friendId }
    }

    LaunchedEffect(friend) {
        val f = friend ?: return@LaunchedEffect
        while (true) {
            code = TOTPManager.currentCode(f.secret)
            seconds = TOTPManager.secondsRemaining()
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(friend?.name ?: "Amigo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Código para tu amigo", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            Text(
                code.chunked(3).joinToString(" "),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { seconds / 30f },
                modifier = Modifier.padding(horizontal = 32.dp).height(6.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Cambia en ${seconds}s", fontSize = 12.sp)
            Spacer(Modifier.height(40.dp))
            Text(
                "Léeselo a tu amigo. Si lo introduce en su pantalla roja, se desbloqueará durante 5 minutos.",
                fontSize = 14.sp
            )
        }
    }
}
