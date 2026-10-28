# ScreenPact: plan de lanzamiento

> Desfasado desde el 28-10-2026: la app pasa a llamarse Scrollban y cambian el posicionamiento y las funciones (ver `docs/PRODUCTO.md`, sección 0). Las fechas y fases de abajo se rehacen después del rediseño.

Preparado el 19-10-2026. Fechas orientativas: se ajustan cuando Kimy fije el día del lanzamiento.

## La idea en una frase

Lanzar gratis en Android en noviembre para aprender con usuarios reales, y guardar el gran empujón (con Pro ya a la venta) para enero, cuando medio mundo se propone usar menos el móvil.

Por qué así:
- **Pro todavía no existe:** faltan el cobro y sus funciones (horarios, rachas, modo estricto). Lanzar sin cobrar no es un problema; lanzar sin saber si la gente sella pactos, sí.
- **Enero es la fecha de los propósitos de Año Nuevo.** Es cuando más gente busca dejar el móvil. Llegar con la app probada, testimonios reales y Pro listo multiplica cada esfuerzo.
- **El crecimiento es de boca a boca por diseño:** cada persona necesita a un amigo. Lo que más importa al principio es que el primer pacto funcione sin fallos, no el alcance.

## Compromisos que ya tenemos

- **Código WOB2026** (en kimycars.com/wob): a quien se apuntó por esa página le prometimos **acceso anticipado** y **la primera función de pago gratis para siempre**. Las altas con `source: "wob"` entran las primeras en la beta, y cuando salga Pro reciben gratis la primera función de pago (propuesta: horarios). Hay que guardar esa lista aparte.
- **Lista de espera:** a todos se les dijo "te aviso el día que se pueda descargar". Un solo correo, sin newsletters.

## Fases

### Fase 0 · Preparar (20 oct – 1 nov)

| Tarea | Quién | Estado |
|---|---|---|
| Probar la versión nueva con un amigo en dos móviles reales: pacto, límite, bloqueo, llave, desbloqueo | Kimy | Pendiente |
| Probar en al menos otra marca (Samsung o Pixel), sobre todo los permisos | Kimy, con un amigo | Pendiente |
| Desplegar la landing y la pestaña de estadísticas en /admin | Sesión de kimycars | Prompt preparado |
| Arreglar `gradlew`, crear la clave de firma fija y compilar la versión 1.0.0 de release | Claude | Pendiente |
| Botón "Invitar a un amigo" en Amigos y al final de la bienvenida: comparte el enlace de la landing | Claude | Pendiente |
| Página de descarga de la beta (APK + instrucciones de instalación paso a paso, con los ajustes restringidos explicados) | Claude | Pendiente |
| Canal de la beta: grupo de Telegram para dudas y fallos | Kimy | Pendiente |
| Cuentas @screenpact en TikTok e Instagram (comprobar que el nombre está libre) | Kimy | Pendiente |

### Fase 1 · Beta cerrada (2 – 15 nov)

- **Quién:** 10-15 parejas (20-30 personas). Por orden: amigos de Kimy, las altas de /wob (compromiso WOB2026) y las altas de la landing.
- **Cómo:** mensaje personal con el enlace a la página de la beta y al grupo de Telegram. Mejor parejas que ya se conozcan, porque una persona sola no puede usar la app.
- **Qué se mide** (pestaña ScreenPact en /admin, con quien active las estadísticas):
  - Cuántos terminan la bienvenida.
  - Cuántos sellan un pacto y ponen un límite el mismo día (activación).
  - Cuántos pactos siguen activos a los 7 días.
  - En qué marcas fallan los permisos.
- **Qué se recoge:**
  - 3 testimonios con nombre y foto, con permiso para usarlos.
  - Capturas y una grabación de pantalla reales.
  - La lista de fallos.
- **Para pasar a la siguiente fase:** más del 50 % de los beta testers con un pacto activo a los 7 días y ningún fallo grave abierto.

### Fase 2 · Lanzamiento suave en Android (semana del 16 nov)

Día de lanzamiento propuesto: **martes 17 de noviembre**.

- **Dónde se descarga:**
  - **Zapstore:** se publica con la cuenta de nostr de Kimy; se encarga de las actualizaciones automáticas.
  - **Descarga directa desde kimycars.com/screenpact.**
  - **Uptodown:** tienda de APK muy usada en España y Latinoamérica, que acepta subidas de desarrolladores. Es la más cercana a nuestro público y no exige Google Play.
- **El día del lanzamiento:**
  - El botón principal de la landing pasa de "Apúntame" a "Descargar".
  - Correo único a la lista de espera.
  - Publicaciones de Kimy en Instagram, X y Telegram, y en nostr.
  - Mensaje a la comunidad de WOB.
- **Esa semana:**
  - Ficha en AlternativeTo como alternativa a Opal, StayFree y Bienestar Digital.
  - Una publicación honesta en Reddit (r/nosurf, r/digitalminimalism) contando cómo funciona y pidiendo opiniones, no votos.
- **Prensa:** contactar con 3-5 medios de Android en español (Xataka Android, Andro4all, El Androide Libre, Hipertextual). El ángulo con más gancho: un chaval de 17 años de Madrid que hizo la app porque el móvil le robaba horas, y que funciona sin cuenta.

### Fase 3 · Pro y "Reto de enero" (diciembre – enero)

- **Hasta mediados de diciembre:** Pro listo, con cobro con Stripe, licencia en la app, horarios y rachas. Se activa el límite de 3 apps del plan gratis.
- **Antes de Navidad:** enviar la ficha a Google Play (25 $). La revisión de los permisos puede tardar.
- **Del 26 de diciembre al 31 de enero, campaña "Reto de enero: 31 días con tu guardián":**
  - Cada persona elige un amigo guardián y una app.
  - Contenido diario en TikTok e Instagram.
  - Los testimonios de la beta, en la landing.
- **Descuento de lanzamiento de Pro** solo para quien estaba en la beta o en la lista de espera. Y la función gratis prometida a WOB2026.

### Fase 4 · Después

- Google Play cuando la retención a 7 días se mantenga.
- iPhone cuando Pro pague los 99 $/año de Apple. Ese será un nuevo lanzamiento con mensaje propio: da igual qué móvil tenga tu amigo.

## Contenido para redes

Cuatro formatos que encajan con el público de 18 a 30 años:

1. **La pantalla amarilla en acción:** alguien en mitad de TikTok, salta el bloqueo y tiene que pedirle la llave a su amigo. Gracia y vergüenza a partes iguales.
2. **"Le doy la llave de mi TikTok a…"** mi madre, mi pareja, mi compañero de piso. Formato de reto, fácil de copiar.
3. **Hecho por un chaval de 17 años:** Kimy contando por qué la hizo y cómo, mostrando su propio uso de pantalla antes y después.
4. **Diario del reto de 30 días:** una pareja de amigos contando cada día cómo va.

El vídeo demo de la landing (`marketing/demo-video`) sirve como base para los primeros vídeos.

## Métricas y objetivos (90 días desde el lanzamiento suave)

| Métrica | Objetivo | Dónde se ve |
|---|---|---|
| Descargas | 500 | Zapstore, Uptodown, contador de la web |
| De quienes instalan, cuántos sellan un pacto | 40 % | /admin, pestaña ScreenPact |
| Pactos activos a 7 días | 30 % | /admin |
| Compran Pro (desde enero) | 3-5 % de los activos | Stripe |

## Decisiones pendientes de Kimy

1. Fecha del lanzamiento suave (propuesta: martes 17 de noviembre).
2. Quiénes son las 10-15 parejas de la beta.
3. Si se abren cuentas propias @screenpact o se usa solo la de Kimy al principio (propuesta: propias, porque el contenido de producto no siempre encaja en la marca personal).
4. Qué función de Pro es "la primera de pago" que se regala a WOB2026 (propuesta: horarios).
