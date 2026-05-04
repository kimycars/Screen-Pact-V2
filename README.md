# ScreenPact

App Android de control de tiempo de pantalla **entre amigos**. Tú solo no puedes desbloquearte.

## Cómo funciona

1. Tú y un amigo os emparejáis: cada uno enseña su QR y el otro lo escanea con la cámara. El QR contiene un secreto compartido único para esa pareja.
2. Eliges qué apps monitorizar (Instagram, TikTok, etc.) y un límite diario en minutos para cada una.
3. Si superas el límite en alguna app, aparece una **pantalla roja bloqueante** que no se puede cerrar.
4. La única manera de salir es introducir un **código de 6 dígitos** que solo puede generar tu amigo en su app en ese momento. El código rota cada 30 segundos (RFC 6238 TOTP), así que no sirve apuntarlo.
5. Tras un código válido tienes 5 minutos de gracia antes de que pueda volver a aparecer la pantalla.

Sin servidor, sin cuenta, sin red — todo el emparejamiento y la verificación pasan por QR + criptografía local.

## Compilar el APK sin Android Studio

### Opción A — Cloud (recomendado, no necesitas instalar nada)

1. Crea un repo en GitHub y sube esta carpeta:
   ```bash
   cd screenpact
   git init && git add . && git commit -m "Initial"
   git branch -M main
   git remote add origin git@github.com:TU_USUARIO/screenpact.git
   git push -u origin main
   ```
2. Ve a la pestaña **Actions** del repo. El workflow `Build APK` se lanza solo en cada push.
3. Cuando termine (~3-5 min) entra al run y descarga el artefacto `ScreenPact-debug`. Dentro está el `.apk`.
4. Pásalo al móvil (correo, Drive, USB) y ábrelo. Permite instalación de orígenes desconocidos cuando lo pida.

Para versionar releases, crea un tag `v0.1.0`: GitHub Actions adjunta el APK a una release automáticamente.

### Opción B — Build local

Necesitas:
- JDK 17
- Android SDK Platform 34 + build-tools 34.x (vía `cmdline-tools`)
- Variable `ANDROID_HOME` apuntando al SDK
- Gradle 8.5 (o usa el wrapper tras generarlo: `gradle wrapper`)

```bash
gradle wrapper --gradle-version 8.5
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permisos que pedirá la app al primer inicio

| Permiso | Por qué |
|---|---|
| Acceso al uso (Usage Stats) | Para medir cuánto llevas en cada app hoy. |
| Dibujar sobre otras apps | Para mostrar la pantalla roja sobre la app que has agotado. |
| Cámara | Solo para escanear el QR de tu amigo al emparejar. |
| Ignorar optimización de batería | Recomendado: evita que el sistema mate el servicio en background. |

Todos se piden desde la pantalla de onboarding, abriendo Ajustes para que los actives.

## Limitaciones conocidas (MVP)

- Si el sistema mata el servicio (Xiaomi/Huawei agresivos), el monitoreo se detiene hasta que reabras la app.
- El overlay no puede cubrir literalmente el 100% del sistema (la barra de estado/navigation sigue accesible) — Android no permite eso a apps de terceros, pero sí cubre toda la app que has agotado.
- El usuario podría desinstalar la app saltándose el bloqueo. Versiones futuras: device admin para impedir desinstalación durante un período.
- La hora del sistema debe estar razonablemente sincronizada en ambos dispositivos (TOTP usa tolerancia de ±30s).

## Estructura

```
app/src/main/kotlin/com/screenpact/app/
├── MainActivity.kt
├── ScreenPactApplication.kt
├── data/
│   ├── crypto/        # TOTP + QR + payload
│   ├── db/            # Room: Friend, AppLimit, UnlockGrant
│   └── Prefs.kt
├── services/
│   ├── UsageMonitorService.kt    # foreground service polling cada 5s
│   ├── OverlayService.kt         # ventana TYPE_APPLICATION_OVERLAY
│   └── BootReceiver.kt
├── ui/
│   ├── home/          # onboarding + dashboard
│   ├── friends/       # lista, escaneo QR, generar códigos
│   ├── apps/          # selector + límites
│   └── overlay/       # contenido Compose de la pantalla roja
└── util/              # permisos, listado de apps, usage stats
```
