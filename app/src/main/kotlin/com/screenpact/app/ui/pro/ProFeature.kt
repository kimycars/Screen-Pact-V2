package com.screenpact.app.ui.pro

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Desde dónde se abre la pantalla de Pro. Cada función de Pro abre la pantalla con su propia
 * cabecera ("El modo estricto es de Pro"…), así la persona sabe por qué está ahí.
 * [GENERAL] es la cabecera de siempre, para los "Ver Pro" que no vienen de ninguna función.
 */
enum class ProFeature(val id: String, val icon: ImageVector, val title: String, val body: String, val benefit: String) {
    APPS(
        "apps", Icons.Default.Apps,
        "Más de 3 apps es de Pro",
        "En el plan gratis vigilas 3 apps. Con Pro, todas las que te roben tiempo.",
        "Todas las apps que quieras",
    ),
    SCHEDULE(
        "schedule", Icons.Default.Bedtime,
        "Los horarios son de Pro",
        "Cierra una app a ciertas horas, aunque te queden minutos. Por ejemplo, Instagram de 0:00 a 8:00.",
        "Horarios: Instagram cerrado de 0 a 8 h",
    ),
    HISTORY(
        "history", Icons.Default.Whatshot,
        "El historial es de Pro",
        "Tus últimas 4 semanas día a día, tus récords y tus insignias por cada meta de días sin pedir llave.",
        "Historial, récords e insignias",
    ),
    STRICT(
        "strict", Icons.Default.Lock,
        "El modo estricto es de Pro",
        "Con el modo estricto, para desinstalar ScreenPact primero hay que quitarlo, y quitarlo pide la llave de un amigo.",
        "Modo estricto: desinstalar pide llave",
    ),
    ICONS(
        "icons", Icons.Default.Palette,
        "Cambiar el icono es de Pro",
        "Elige el icono de ScreenPact en tu escritorio: amarillo, noche o menta.",
        "Iconos de la app a elegir",
    ),
    GENERAL(
        "general", Icons.Default.Star,
        "Llévalo más lejos con Pro",
        "Mensual, anual o de por vida. Tú eliges.",
        "",
    );

    companion object {
        fun byId(id: String?): ProFeature = entries.firstOrNull { it.id == id } ?: GENERAL
    }
}
