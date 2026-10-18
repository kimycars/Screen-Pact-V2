# ScreenPact: reglas del proyecto

App Android (Kotlin + Jetpack Compose) de pacto entre amigos para limitar el uso del móvil.
Antes de tocar producto, marca o precios, lee `docs/PRODUCTO.md`. Visión general para cualquier
área: `docs/screenpact-resumen.html`.

## Sistema de diseño: nada escrito a mano

- Todos los valores de diseño salen de `app/src/main/kotlin/com/screenpact/app/ui/theme/`:
  - `Theme.kt`: colores (`Sp.Tinta`, `Sp.Llave`…) y tipografía (`MaterialTheme.typography.*`).
  - `Tokens.kt`: espaciado (`Space`), tamaños (`Sizes`), trazos (`Strokes`), formas (`Shapes`) y estilos de la llave (`TextStyles`).
- Fuera de `ui/theme` no se escribe ningún número con `.dp`/`.sp` ni ningún `Color(0x…)`. La tarea de Gradle `checkDesignTokens` corre antes de cada build y lo hace fallar si pasa.
- Si falta un valor, se añade a `Tokens.kt` con un nombre que diga para qué sirve. No se añaden valores intermedios a la escala de `Space` sin motivo.
- La interfaz se construye con las piezas de `ui/components/Components.kt` (`PrimaryButton`, `OutlineButton`, `QuietButton`, `ChipButton`, `CardButton`, `IconAction`, `SpCard`, `ScreenTitle`, `BackButton`, `LogoTile`, `Avatar`, `UsageBar`, `SpTextField`, `CodeInput`, `PermissionCard`). Antes de escribir un botón, una tarjeta o un campo, usa la pieza que ya existe.
- Algo nuevo que se vaya a usar en más de un sitio nace como pieza compartida en `ui/components` (o en `ui/common` si lleva lógica, como `FriendCodeDialog` o `PermissionCards`).
- Si al tocar un fichero encuentras algo duplicado o hecho a mano, unifícalo en el mismo cambio.

## Reglas de producto que el código debe respetar

- Endurecer es libre, aflojar pide llave: añadir apps, bajar límites y reactivar son libres; subir límites, pausar, quitar apps y romper pactos piden la llave de un amigo (`FriendCodeDialog`).
- La única fuente de verdad para decidir si una llave vale es `data/FriendCodes.kt`.
- Las trampas se detectan en `util/TamperGuard.kt`. No añadir caminos que se salten ninguna de las dos cosas.
- Nada sale del móvil: la app no tiene permiso de Internet. No añadirlo sin hablarlo con Kimy.

## Compilar

`gradlew` está roto (falta la clase del wrapper). Usa el Gradle de la caché con JDK 17:

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
~/.gradle/wrapper/dists/gradle-8.14.3-all/*/gradle-8.14.3/bin/gradle assembleDebug
```

El APK de pruebas queda en `app/build/outputs/apk/debug/app-debug.apk`. Instálalo siempre compilado en este PC: los APK de GitHub Actions se firman con una clave aleatoria y no se pueden actualizar encima.
