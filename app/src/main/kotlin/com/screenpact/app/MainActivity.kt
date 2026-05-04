package com.screenpact.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.screenpact.app.ui.MainNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) darkColorScheme(
                primary = Color(0xFFD32F2F),
                onPrimary = Color.White
            ) else lightColorScheme(
                primary = Color(0xFFD32F2F),
                onPrimary = Color.White
            )
            MaterialTheme(colorScheme = colors) {
                Surface { MainNav() }
            }
        }
    }
}
