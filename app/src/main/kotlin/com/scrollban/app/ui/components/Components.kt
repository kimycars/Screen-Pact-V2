package com.scrollban.app.ui.components

import com.scrollban.app.Brand
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollban.app.R
import com.scrollban.app.ui.theme.Bricolage
import com.scrollban.app.ui.theme.Mono
import com.scrollban.app.ui.theme.Sb
import com.scrollban.app.ui.theme.Space
import com.scrollban.app.ui.theme.Sizes
import com.scrollban.app.ui.theme.Strokes
import com.scrollban.app.ui.theme.Shapes
import com.scrollban.app.ui.theme.TextStyles
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults


@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Sb.Llave,
    content: Color = Sb.Tinta
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(Sizes.button),
        shape = Shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = Sb.Superficie,
            disabledContentColor = Sb.TextoSuave
        )
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Sizes.button),
        shape = Shapes.pill,
        border = BorderStroke(Strokes.bold, Sb.Llave),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Sb.Llave)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Sb.TextoSuave) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = Sizes.touch)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.Center)
    }
}

/** Botón pequeño con borde, para acciones dentro de una tarjeta. */
@Composable
fun ChipButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(Sizes.chip),
        shape = Shapes.pill,
        border = BorderStroke(Strokes.hairline, Sb.Borde),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Sb.Niebla)
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}

/**
 * Opción de un grupo pequeño (días de un reto, minutos…). Píldora amarilla si está elegida.
 * Va dentro de un contenedor con `Modifier.selectableGroup()` para los lectores de pantalla.
 */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = Sizes.chip)
            .clip(Shapes.pill)
            .background(if (selected) Sb.Llave else Sb.Tinta)
            .border(BorderStroke(Strokes.hairline, if (selected) Sb.Llave else Sb.Borde), Shapes.pill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = Space.m),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Sb.Tinta else Sb.Niebla)
    }
}

@Composable
fun SbCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(Sb.Superficie)
            .padding(horizontal = Space.l, vertical = Space.l),
        content = content
    )
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineLarge, modifier = modifier)
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconAction(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onBack, tint = Sb.Niebla)
}

/** El icono de la app (candado de dos) sobre su cuadro amarillo. */
@Composable
fun LogoTile(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * Shapes.logoCornerRatio))
            .background(Sb.Llave),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.ic_logo),
            contentDescription = "${Brand.NAME}",
            modifier = Modifier.size(size * Shapes.logoInnerRatio)
        )
    }
}

/** Inicial del amigo en un círculo. */
@Composable
fun Avatar(name: String, size: Dp = Sizes.avatar, color: Color = Sb.Llave) {
    Box(
        Modifier.size(size).clip(Shapes.pill).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            fontFamily = Bricolage,
            fontSize = (size.value * TextStyles.avatarRatio).sp,
            color = Sb.Tinta
        )
    }
}

@Composable
fun UsageBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = Sb.Superficie) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Sizes.usageBar)
            .clip(Shapes.pill)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(Sizes.usageBar)
                .clip(Shapes.pill)
                .background(color)
        )
    }
}

@Composable
fun SbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = Shapes.field,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Sb.Llave,
            unfocusedBorderColor = Sb.Borde,
            focusedLabelColor = Sb.Llave,
            unfocusedLabelColor = Sb.TextoSuave,
            cursorColor = Sb.Llave
        )
    )
}

/**
 * Seis casillas para la llave. Un BasicTextField invisible recoge el teclado y las casillas
 * pintan cada cifra. [onYellow] es la variante para la pantalla de bloqueo.
 */
@Composable
fun CodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onYellow: Boolean = false,
    autoFocus: Boolean = true
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val filledBg = if (onYellow) Sb.Tinta else Sb.Tinta
    val digitColor = Sb.Llave
    val emptyBorder = if (onYellow) Sb.Tinta else Sb.Borde
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = code,
            onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) onCodeChange(v) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focus)
                .semantics { contentDescription = "Llave de 6 cifras" }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    runCatching { focus.requestFocus() }
                },
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            for (i in 0 until 6) {
                val digit = code.getOrNull(i)
                CodeBox(digit, filledBg, digitColor, emptyBorder)
            }
        }
    }
}

@Composable
private fun RowScope.CodeBox(digit: Char?, filledBg: Color, digitColor: Color, emptyBorder: Color) {
    val base = Modifier.weight(1f).height(Sizes.codeBox).clip(Shapes.field)
    Box(
        if (digit != null) base.background(filledBg) else base.border(Strokes.bold, emptyBorder, Shapes.field),
        contentAlignment = Alignment.Center
    ) {
        if (digit != null) Text(digit.toString(), style = TextStyles.keyDigit, color = digitColor)
    }
}

/**
 * Un permiso del sistema: qué es, para qué lo queremos y cómo darlo. Lo usan la bienvenida y la
 * pantalla Hoy (cuando falta un permiso). [help] explica el camino en Ajustes y solo se ve
 * mientras el permiso falta.
 */
@Composable
fun PermissionCard(
    title: String,
    why: String,
    granted: Boolean,
    onGrant: () -> Unit,
    action: String = "Dar",
    help: String? = null
) {
    SbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xxs))
                Text(why, style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave)
            }
            Spacer(Modifier.width(Space.s))
            if (granted) {
                Box(
                    Modifier.size(Sizes.badge).clip(Shapes.pill).background(Sb.Pacto),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, contentDescription = "Concedido", tint = Sb.Tinta, modifier = Modifier.size(Sizes.iconM)) }
            } else {
                ChipButton(action, onGrant)
            }
        }
        if (!granted && help != null) {
            Spacer(Modifier.height(Space.s))
            Text(help, style = MaterialTheme.typography.bodySmall, color = Sb.Niebla)
        }
    }
}

/** Botón solo con icono, con el área táctil mínima. Siempre con descripción para lectores de pantalla. */
@Composable
fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Sb.TextoSuave
) {
    IconButton(onClick = onClick, modifier = modifier.size(Sizes.touch)) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

/** Acción secundaria a lo ancho con forma de tarjeta (por ejemplo, "Añadir otra app"). */
@Composable
fun CardButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Sizes.button),
        shape = Shapes.card,
        border = BorderStroke(Strokes.bold, Sb.Borde),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Sb.Niebla)
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

/** Interruptor con los colores de la marca. */
@Composable
fun SbSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Sb.Tinta,
            checkedTrackColor = Sb.Llave,
            uncheckedThumbColor = Sb.TextoSuave,
            uncheckedTrackColor = Sb.Superficie,
            uncheckedBorderColor = Sb.Borde
        )
    )
}

/**
 * Tarjeta que se elige de un grupo (planes de Pro, iconos…). Borde amarillo grueso si está
 * elegida. Va dentro de un contenedor con `Modifier.selectableGroup()` para los lectores de pantalla.
 */
@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(Shapes.card)
            .background(Sb.Superficie)
            .border(BorderStroke(if (selected) Strokes.bold else Strokes.hairline, if (selected) Sb.Llave else Sb.Borde), Shapes.card)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(Space.m),
        content = content
    )
}

/** Etiqueta pequeña en píldora amarilla ("Recomendado"). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Sb.Tinta,
        modifier = modifier
            .clip(Shapes.pill)
            .background(Sb.Llave)
            .padding(horizontal = Space.xs, vertical = Space.xxs)
    )
}

/**
 * Anillo de progreso (el uso de hoy). [fraction] de 0 a 1; el centro lleva lo que se le pase.
 * Dentro de una SbCard, la pista va en Sb.Linea para que se vea.
 */
@Composable
fun UsageRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.todayRing,
    track: Color = Sb.Linea,
    content: @Composable () -> Unit = {}
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = Strokes.ring.toPx()
            val inset = w / 2
            val arc = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, tl, arc, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            drawArc(
                color, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, arc,
                style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        content()
    }
}

/**
 * Hueco vacío con borde punteado: un sitio libre para algo que aún no está (otra app con
 * límite, por ejemplo). Toda la tarjeta es el botón.
 */
@Composable
fun SlotCard(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Sb.TextoSuave) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.button)
            .clip(Shapes.card)
            .drawBehind {
                val outline = Shapes.card.createOutline(size, layoutDirection, this)
                val dash = Strokes.dash.toPx()
                drawOutline(
                    outline, Sb.Borde,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        Strokes.bold.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                    )
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(Sizes.iconL))
        Spacer(Modifier.width(Space.s))
        Text(text, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}

/**
 * Mini gráfica de barras de una semana. [values] en la misma unidad que [limit]; las barras que
 * llegan al límite van en [overColor]. La línea discontinua marca el límite.
 */
@Composable
fun WeekBars(
    values: List<Long>,
    limit: Long,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = Sb.Pacto,
    overColor: Color = Sb.Llave
) {
    val max = maxOf(values.maxOrNull() ?: 0L, limit, 1L).toFloat()
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(Sizes.weekChart)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                values.forEach { v ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((v / max).coerceIn(0.02f, 1f))
                            .clip(Shapes.field)
                            .background(if (limit > 0 && v >= limit) overColor else color)
                    )
                }
            }
            if (limit > 0) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1f - limit / max)
                    val dash = Strokes.dash.toPx()
                    drawLine(
                        Sb.TextoSuave, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = Strokes.hairline.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.xxs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            labels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Sb.TextoSuave, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Un paso de una explicación: insignia con número o icono y su texto. [badge] es el fondo de la
 * insignia (Sb.Linea si va dentro de una SbCard, para que se vea).
 */
@Composable
fun StepLine(
    text: String,
    modifier: Modifier = Modifier,
    number: Int? = null,
    icon: ImageVector? = null,
    detail: String? = null,
    badge: Color = Sb.Superficie
) {
    Row(modifier.padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(Sizes.stepBadge).clip(Shapes.field).background(badge),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> Icon(icon, null, tint = Sb.Llave, modifier = Modifier.size(Sizes.iconM))
                number != null -> Text("$number", style = MaterialTheme.typography.titleMedium, color = Sb.Llave)
            }
        }
        Spacer(Modifier.width(Space.m))
        Column {
            Text(text, style = if (detail != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Sb.TextoSuave) }
        }
    }
}
