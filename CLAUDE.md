# Scrollban (antes ScreenPact): reglas del proyecto

App Android (Kotlin + Jetpack Compose): el reto de soltar el móvil con tus amigos o tu pareja, donde
solo ellos pueden darte más tiempo. El 28-10-2026 cambiaron el nombre (ScreenPact pasa a Scrollban),
el posicionamiento y las funciones previstas (grupos, retos, tiempo ganado, pacto mixto con iPhone,
servidor en Supabase). El código y la web todavía dicen ScreenPact hasta el rediseño.
Antes de tocar producto, marca o precios, lee `docs/PRODUCTO.md` (sección 0: decisiones vigentes).
`docs/screenpact-resumen.html` y `docs/LANZAMIENTO.md` son de la etapa ScreenPact y están desfasados.

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
- Los pactos se sellan solo en `data/Pairing.kt` (en persona por QR o a distancia por enlace). Las llaves salen de Diffie-Hellman (`data/crypto/PactKeys.kt`): ni el QR ni el enlace llevan nada secreto. No se acepta una invitación propia ni una creada en el mismo móvil (apps clonadas), y un pacto a distancia espera siempre 24 h antes de poder dar llaves, aunque sea el primero.
- Las trampas se detectan en `util/TamperGuard.kt`. No añadir caminos que se salten ninguna de las dos cosas.
- Privacidad en revisión (28-10-2026): con los grupos, los minutos de cada día por app subirán a Supabase y solo los verá el grupo; las llaves no salen nunca del móvil y no habrá correo ni contraseña. Hasta que se construya, rige lo de abajo, y el cambio exige actualizar la política de privacidad antes de lanzar.
- Los datos de la persona no salen del móvil (minutos, amigos, llaves). Las únicas conexiones a Internet son las estadísticas anónimas de `data/Stats.kt` (encendidas en la bienvenida, se apagan en Ajustes) y la renovación de la licencia de Pro en `data/Pro.kt` (manda solo el id de licencia). Cualquier dato nuevo que se quiera enviar pasa por ahí, se añade a la política de privacidad de la app (kimycars.com/screenpact/privacidad, en la web de kimycars) y se habla antes con Kimy.
- Precios: `data/Plans.kt` es la fuente dentro de la app; la landing y `docs/PRODUCTO.md` llevan los mismos.
- Pro: `data/Pro.kt` es la única fuente de verdad (`Pro.isPro`, `rememberIsPro()` en la interfaz). Las licencias las firma el servidor de kimycars.com (`api/sp-stripe-webhook.js`, `api/sp-license.js`, `lib/sp-license.js`) con la clave privada de `Documents\claves\screenpact-license-private.pem`; la app solo lleva la pública. En la versión de pruebas hay un interruptor "Pro de prueba" en Ajustes.
- Base de datos: cada cambio de estructura sube la versión de `AppDatabase` y lleva su `Migration`. Nunca `fallbackToDestructiveMigration`: borraría los pactos de la gente al actualizar.

## Compilar

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
./gradlew assembleDebug     # pruebas: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # publicar: app/build/outputs/apk/release/app-release.apk (firmado)
```

- El release se firma con la clave fija de `C:\Users\Usuario\Documents\claves\screenpact-release.jks` (contraseñas en `screenpact-keystore.properties`, en la misma carpeta, fuera del repo). Perder esa clave = no poder actualizar la app nunca más: hay que tener copia de la carpeta `claves`.
- Instala siempre APK compilados en este PC: los de GitHub Actions se firman con una clave aleatoria y no se pueden actualizar encima.
