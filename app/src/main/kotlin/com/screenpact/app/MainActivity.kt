package com.screenpact.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.screenpact.app.ui.MainNav
import com.screenpact.app.ui.theme.ScreenPactTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenPactTheme {
                Surface { MainNav() }
            }
        }
    }
}
