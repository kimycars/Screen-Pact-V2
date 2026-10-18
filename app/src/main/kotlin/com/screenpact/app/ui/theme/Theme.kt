package com.screenpact.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.screenpact.app.R

/**
 * Tokens de marca de ScreenPact (ver docs/PRODUCTO.md, sección 5). Única fuente de colores y
 * tipografías: ninguna pantalla debe escribir un hex ni una fuente a mano.
 */
object Sp {
    // Primitivos
    val Tinta = Color(0xFF171A33)
    val Superficie = Color(0xFF22264A)
    val Linea = Color(0xFF2C3160)
    val Borde = Color(0xFF3A3F6E)
    val Niebla = Color(0xFFF4F5FA)
    val TextoSuave = Color(0xFFA9AEC9)
    val Llave = Color(0xFFFFC23D)
    val Pacto = Color(0xFF6FD9B8)
    val Aviso = Color(0xFFFF7A6B)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Titulares y cifras grandes. */
val Bricolage = FontFamily(
    variable(R.font.bricolage_grotesque, 600),
    variable(R.font.bricolage_grotesque, 800)
)

/** Texto corrido e interfaz. */
val Figtree = FontFamily(
    variable(R.font.figtree, 400),
    variable(R.font.figtree, 500),
    variable(R.font.figtree, 600)
)

/** Solo para la llave de 6 cifras. */
val Mono = FontFamily(
    variable(R.font.jetbrains_mono, 700)
)

private val SpTypography = Typography(
    displayLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val SpColors = darkColorScheme(
    primary = Sp.Llave,
    onPrimary = Sp.Tinta,
    secondary = Sp.Pacto,
    onSecondary = Sp.Tinta,
    background = Sp.Tinta,
    onBackground = Sp.Niebla,
    surface = Sp.Tinta,
    onSurface = Sp.Niebla,
    surfaceVariant = Sp.Superficie,
    onSurfaceVariant = Sp.TextoSuave,
    surfaceContainerHigh = Sp.Superficie,
    surfaceContainer = Sp.Superficie,
    outline = Sp.Borde,
    outlineVariant = Sp.Linea,
    error = Sp.Aviso,
    onError = Sp.Tinta,
)

/** La marca es oscura: la app usa siempre este esquema, tenga el móvil el tema que tenga. */
@Composable
fun ScreenPactTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SpColors, typography = SpTypography, content = content)
}
