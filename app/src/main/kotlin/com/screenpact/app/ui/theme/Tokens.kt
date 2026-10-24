package com.screenpact.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Tokens de medida de ScreenPact. Junto con Theme.kt (colores y tipografía) son la ÚNICA fuente
 * de valores de diseño: fuera de ui/theme no se escribe ningún número con .dp/.sp ni ningún
 * Color(0x…). La tarea de Gradle `checkDesignTokens` hace fallar el build si alguien lo hace.
 * Si falta un valor, se añade aquí con un nombre que diga para qué sirve.
 */

/** Espaciado: márgenes, separaciones y huecos. Escala cerrada; no hay valores intermedios. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 56.dp
    val hero = 72.dp

    /** Margen lateral de todas las pantallas. */
    val screen = xl
}

/** Tamaños fijos de piezas. */
object Sizes {
    /** Mínimo táctil (Material y WCAG). */
    val touch = 48.dp
    val button = 56.dp
    val chip = 44.dp
    val iconS = 16.dp
    val iconM = 20.dp
    val iconL = 24.dp
    val avatar = 44.dp
    val badge = 32.dp
    val stepBadge = 36.dp
    val codeBox = 60.dp
    val usageBar = 10.dp
    val bottomBar = 72.dp
    val logoHero = 88.dp
    val qr = 232.dp
    val keyRing = 260.dp
    val appIcon = 32.dp
    val pickerList = 360.dp
    /** Círculo de cada día en la tira de la racha. */
    val dayToken = 36.dp
    /** Anillo del uso de hoy en la pantalla Hoy. */
    val todayRing = 96.dp
    /** Insignia de una meta de racha. */
    val medal = 56.dp
}

/** Grosor de bordes y trazos. */
object Strokes {
    val hairline = 1.dp
    val bold = 2.dp
    val ring = 8.dp
}

/** Formas. Una sola escala de esquinas en toda la app. */
object Shapes {
    val card = RoundedCornerShape(16.dp)
    val field = RoundedCornerShape(12.dp)
    val pill = CircleShape
    /** Esquina del cuadro del logo, proporcional a su lado. */
    const val logoCornerRatio = 0.25f
    /** Parte del cuadro que ocupa el dibujo del logo. */
    const val logoInnerRatio = 0.74f
}

/** Estilos de texto que no son de Material (la llave en mono). */
object TextStyles {
    val keyLarge = TextStyle(fontFamily = Mono, fontSize = 44.sp, letterSpacing = 4.sp)
    val keyDigit = TextStyle(fontFamily = Mono, fontSize = 26.sp)
    /** Inicial dentro del avatar, en proporción a su tamaño. */
    const val avatarRatio = 0.42f
}
