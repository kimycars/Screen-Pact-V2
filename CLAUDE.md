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
- La única fuente de verdad para decidir si una llave vale es `data/FriendCodes.kt`. Vale la llave de un amigo con pacto o la de un grupo de 3 o más (24 h después de entrar en él). Salir de un grupo pide llave, como romper un pacto.
- Los pactos se sellan solo en `data/Pairing.kt` (en persona por QR o a distancia por enlace). Las llaves salen de Diffie-Hellman (`data/crypto/PactKeys.kt`): ni el QR ni el enlace llevan nada secreto. No se acepta una invitación propia ni una creada en el mismo móvil (apps clonadas), y un pacto a distancia espera siempre 24 h antes de poder dar llaves, aunque sea el primero. Un pacto con iPhone (la otra mitad la hace la página web `kimycars.com/screenpact/iphone`, con las mismas cuentas en `screenpact/pacto-ios.js` de la web) espera siempre 24 h, también en persona. Si se cambia `PactKeys.derive` o `PairingPayload`, hay que cambiar también `pacto-ios.js`.
- Las trampas se detectan en `util/TamperGuard.kt`. No añadir caminos que se salten ninguna de las dos cosas.
- Privacidad: los datos de la persona no salen del móvil salvo lo que ella active. Las conexiones a Internet son:
  - Estadísticas anónimas de `data/Stats.kt` (encendidas en la bienvenida, se apagan en Ajustes).
  - Renovación de la licencia de Pro en `data/Pro.kt` (manda solo el id de licencia).
  - Ranking con amigos en Supabase (`data/cloud/`, desde el 05-11-2026): apagado de inicio. Si se activa, suben los minutos por día de las apps con límite, las llaves usadas, la media de antes y el reto, y solo los ve la gente con la que hay pacto o grupo. Nunca suben nombres ni llaves en claro: en los grupos de 3 o más, el nombre y el id de instalación suben cifrados con el secreto del grupo, y el secreto viaja cifrado con la clave del pacto (`data/crypto/GroupKeys.kt`). Apagarlo borra todo del servidor. `data/cloud/Cloud.kt` es la única conexión con Supabase y la estructura del servidor está en `supabase/schema.sql`.
  Cualquier dato nuevo que se quiera enviar pasa por uno de esos sitios, se añade a la política de privacidad (kimycars.com/screenpact/privacidad, en la web de kimycars) y se habla antes con Kimy. La política aún no recoge el ranking: hay que actualizarla antes de lanzar.
- Precios: `data/Plans.kt` es la fuente dentro de la app; la landing y `docs/PRODUCTO.md` llevan los mismos.
- Pro: `data/Pro.kt` es la única fuente de verdad (`Pro.isPro`, `rememberIsPro()` en la interfaz). Las licencias las firma el servidor de kimycars.com (`api/sp-stripe-webhook.js`, `api/sp-license.js`, `lib/sp-license.js`) con la clave privada de `Documents\claves\screenpact-license-private.pem`; la app solo lleva la pública. En la versión de pruebas hay un interruptor "Pro de prueba" en Ajustes.
- Base de datos: cada cambio de estructura sube la versión de `AppDatabase` y lleva su `Migration`. Nunca `fallbackToDestructiveMigration`: borraría los pactos de la gente al actualizar.

## Commit y despliegue (Kimy, 05-11-2026)

Commit y push a GitHub (`origin main`) cada vez que se termina algo, sin preguntar. Lo que se ejecuta en Supabase se guarda también en `supabase/schema.sql` en el mismo commit.

## Compilar

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
./gradlew assembleDebug     # pruebas: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # publicar: app/build/outputs/apk/release/app-release.apk (firmado)
```

- El release se firma con la clave fija de `C:\Users\Usuario\Documents\claves\screenpact-release.jks` (contraseñas en `screenpact-keystore.properties`, en la misma carpeta, fuera del repo). Perder esa clave = no poder actualizar la app nunca más: hay que tener copia de la carpeta `claves`.
- Instala siempre APK compilados en este PC: los de GitHub Actions se firman con una clave aleatoria y no se pueden actualizar encima.
