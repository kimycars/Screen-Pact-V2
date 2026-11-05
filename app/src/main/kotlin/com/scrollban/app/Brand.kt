package com.scrollban.app

/**
 * Nombre de la app tal y como lo ve la persona. Única fuente para los textos de la interfaz;
 * el nombre del icono del escritorio sale de `app_name` en res/values/strings.xml.
 *
 * La app se llamó ScreenPact hasta noviembre de 2026. Por compatibilidad quedan a propósito:
 *  - los enlaces antiguos screenpact://license, screenpact://pair y kimycars.com/screenpact/…,
 *    que se siguen aceptando (ver OLD_SCHEME);
 *  - los nombres de almacenamiento (screenpact.db, screenpact_prefs, screenpact_* …) y la
 *    etiqueta "screenpact-unlock:" de PactKeys: cambiarlos borraría los datos o rompería los
 *    pactos ya sellados. La persona no los ve nunca.
 */
object Brand {
    const val NAME = "ScrollBan"

    /** Esquema de los enlaces que abren la app (scrollban://license, scrollban://pair). */
    const val SCHEME = "scrollban"

    /** Esquema del nombre antiguo; se sigue aceptando en los enlaces que lleguen. */
    const val OLD_SCHEME = "screenpact"

    fun isAppScheme(scheme: String?): Boolean = scheme == SCHEME || scheme == OLD_SCHEME

    /** Rutas de la web de la app en kimycars.com, la actual y la antigua (redirige a la actual). */
    const val WEB_PATH = "/scrollban"
    const val OLD_WEB_PATH = "/screenpact"
}
