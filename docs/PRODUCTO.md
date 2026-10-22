# ScreenPact: definición de producto para el lanzamiento público

Documento vivo. Recoge las decisiones de octubre de 2026 para pasar del MVP a una versión pública.

Decisiones de partida (Kimy, 01-10-2026):
- Público principal: gente de 18 a 30 años.
- Modelo: freemium. Cobro con Stripe (solo comisión por venta).
- Distribución: primero Zapstore y APK desde web; Play Store e iOS más adelante.
- Servidor: solo si es imposible que dé pérdidas. Ningún coste fijo sin ingresos detrás.

---

## 1. Qué es y por qué existe

**Una frase:** ScreenPact es un pacto con un amigo. Si te pasas de tu límite en una app, solo él puede darte la llave.

**El trabajo que el usuario contrata:** "quiero dejar de perder horas en TikTok/Instagram y sé que con fuerza de voluntad sola no basta".

**Por qué funciona:** los bloqueadores normales se saltan con un toque, porque la persona que bloquea y la que desbloquea es la misma. ScreenPact separa esos dos papeles. Pedirle el código a un amigo cuesta un poco de vergüenza, y esa fricción social es el producto.

### Competencia (búsqueda del 01-10-2026)

| App | Cómo desbloquea | Precio | Necesita cuenta/servidor |
|---|---|---|---|
| Opal | Sesiones; un amigo aprueba salir antes | ~60-100 $/año | Sí |
| one sec | Pausa con respiración | 39,99 $/año | Sí (premium) |
| ScreenZen | Pausa antes de abrir | Gratis (donaciones) | No |
| Brick | Llavero NFC físico | 59 $ pago único | Sí |
| PeerPause, SelfBlock, Lock In, Sheppie, Screen Time Buddy | Un amigo aprueba desde su app | Suscripción | Sí, todas |

Lectura: la idea de "un amigo aprueba" ya existe, pero casi siempre en iOS, con cuenta y suscripción. Kimy creó ScreenPact precisamente porque no encontró nada así para Android. Ese es el mercado de entrada.

### Posicionamiento

**Ahora (solo Android):** el pacto entre amigos que en Android no existía. Que no haga falta cuenta ayuda, pero no basta para diferenciarse: es un argumento de confianza, no el gancho.

**Cuando llegue iOS:** da igual qué móvil tenga tu amigo. Las apps de iOS que hacen esto solo funcionan entre usuarios de iPhone. Eso nos diferencia de todas.

**Para** gente joven que quiere usar menos el móvil, **ScreenPact es** el pacto con un amigo en el que **solo él puede darte más tiempo**. **A diferencia de** los bloqueadores normales, no puedes saltártelo tú solo. **A diferencia de** Opal o PeerPause, existe para Android y no te pide cuenta.

Tres pilares de mensaje, por orden de peso:
1. **Tu amigo tiene la llave.** No puedes desbloquearte solo, de verdad.
2. **Por fin en Android** (y más adelante: da igual el móvil de tu amigo).
3. **Sin cuenta.** Tus minutos, tus amigos y tus llaves no salen de tu móvil.

---

## 2. Objetivos y métricas

**Métrica estrella:** pactos activos a los 7 días. Un pacto activo es una pareja emparejada con al menos un límite activo que sigue instalada una semana después.

**Activación (el momento "ajá"):** emparejarse con alguien y poner el primer límite el mismo día. Todo el onboarding empuja hacia ahí.

**Bucle de crecimiento natural:** cada usuario necesita a otro para usar la app. Cada instalación trae, por diseño, una segunda instalación. No hace falta un programa de referidos con premios: el producto ya es la invitación.

Objetivos de los primeros 90 días tras el lanzamiento (orientativos, a revisar con datos reales):
- 500 instalaciones.
- 40 % de los que instalan llegan a emparejarse.
- 30 % de los pactos siguen activos a los 7 días.
- 3-5 % de los usuarios activos compran Pro.

Medición (decidido el 19-10-2026): estadísticas anónimas dentro de la app; desde el 19-10-2026 salen encendidas en la bienvenida (se pueden apagar ahí y en Ajustes). Una vez al día se envía a kimycars.com/api/sp-stats (Vercel Blob, sin coste): versión, fabricante, número de amigos y límites, qué apps se limitan y contadores de eventos (bienvenida terminada, pacto sellado, límite añadido, bloqueo, bloqueo por trampa, desbloqueo). Nunca nombres, llaves ni minutos. Con eso se calculan activación y pactos activos a 7 días. Además: descargas de Zapstore y de la web, pagos de Stripe y, en Play, su consola.

---

## 3. Modelo de negocio

### Principio
Coste fijo cero. Solo se gasta cuando alguien paga.

### Gratis vs Pro

| | Gratis | Pro |
|---|---|---|
| Amigos emparejados | Ilimitados | Ilimitados |
| Apps con límite | 3 | Ilimitadas |
| Límite diario por app | Sí | Sí |
| Pantalla de bloqueo + código de amigo | Sí | Sí |
| Protecciones básicas anti-trampa (aviso si cambias la hora o quitas permisos) | Sí | Sí |
| Horarios (por ejemplo, Instagram bloqueado de 0:00 a 8:00) | No | Sí |
| Historial y estadísticas semanales | No | Sí |
| Rachas del pacto (días seguidos cumpliendo) | No | Sí |
| Modo estricto (impide desinstalar sin código de amigo) | No | Sí |
| Iconos alternativos de la app | No | Sí |

Por qué los amigos no se limitan: cada amigo es una instalación nueva. Cobrar por amigos frenaría el único canal de crecimiento que tenemos.

Por qué el límite está en 3 apps: casi todo el mundo empieza por TikTok, Instagram y YouTube. Con eso el gratis es útil de verdad, y el que quiere controlar más ya está comprometido y paga.

### Precio
Decidido el 19-10-2026 (antes era un pago único de 7,99 €; se cambió para tener ingresos recurrentes):

| Plan | Precio | Por qué |
|---|---|---|
| Mensual | 2,99 €/mes | Para quien no quiere pagar mucho de golpe. Hace que el anual parezca la buena opción. |
| Anual (por defecto) | 19,99 €/año | Sale a 1,67 €/mes. Es el plan recomendado y la base de ingresos recurrentes. |
| De por vida | 49,99 € | Unos 2,5 años de anual, el criterio habitual. Para quien odia suscribirse. |
| Pack Pacto | 29,99 €/año | El anual para los dos amigos del pacto. |

- Referencia: Opal cuesta más de 60 $ al año. No se iguala porque su público es iPhone y EE. UU.; para 18-30 en España, 60 € de golpe frena. Se revisa con datos reales.
- Costes fijos previstos: dominio propio (~15 €/año), Google Play (25 $ una vez) y Apple (99 $/año, solo con iOS). Comisiones: Google y Apple 15 % en suscripciones, Stripe ~1,5 % + 0,25 €. Con unas 7 suscripciones anuales al año se cubren.
- En la app los precios viven en `data/Plans.kt`.

### Cómo se cobra sin servidor propio: Stripe
Stripe no tiene cuota fija, solo comisión por venta (en la UE, alrededor de 1,5 % + 0,25 € por tarjeta europea; comprobar la tarifa vigente en stripe.com antes de lanzar).

1. El botón "Hazte Pro" abre un Payment Link de Stripe (página de pago alojada por Stripe: no tocamos datos de tarjeta).
2. Cuando se completa el pago, Stripe llama por webhook a una función gratuita (Vercel o Cloudflare en plan gratis) que firma una licencia con una clave privada y se la envía al comprador por email o la muestra en la página de gracias.
3. La app guarda la licencia y la comprueba sin conexión con la clave pública incluida en el APK.

La función solo se ejecuta cuando entra un pago, y el plan gratuito de esos servicios aguanta miles al mes. No hay escenario de pérdidas.

Cuando la app esté en Play Store, Google obliga a cobrar dentro de la app con su sistema (15 %). La licencia de Stripe seguirá valiendo para quien instale el APK desde Zapstore o la web.

---

## 4. Plataformas y orden de lanzamiento

1. **Zapstore + APK en web (lanzamiento).** Sin revisión de Google. El público de Zapstore (nostr, bitcoin, privacidad) encaja exactamente con el pilar "sin cuenta". Más adelante se puede añadir pago en bitcoin para ese público.
2. **Play Store (cuando haya tracción).** La cuenta de desarrollador cuesta 25 $, una vez. Habrá que justificar ante Google el acceso de uso y la superposición de pantalla; el argumento es honesto: bienestar digital elegido por el propio usuario.
3. **iOS (solo con ingresos).** Apple obliga a usar su API de Tiempo de uso (FamilyControls), que requiere un permiso especial, un Mac y 99 $ al año. Es casi otra app entera. Se aborda cuando Pro haya pagado esos costes.

---

## 5. Identidad de marca

### Personalidad
Cómplice, directa, con algo de humor y nunca de sermón. Habla como el amigo que te quita el móvil en la cena, no como un médico.

- Sí: "Te has comido tus 30 minutos de TikTok. Pídele el código a Carla."
- No: "Has superado tu límite de uso diario. Introduce el código de verificación."

### Metáfora central
**Dos llaves.** Técnicamente cada pacto tiene dos secretos, uno por sentido: tú tienes la llave de tu amigo y él tiene la tuya. El logo y el lenguaje salen de ahí. Se habla de llave y de pacto, no de código TOTP ni de emparejamiento.

**Logo propuesto:** dos medias llaves que se cruzan formando una "P", o dos eslabones con forma de llave enganchados. Sencillo, reconocible a 24 px en el icono de la app.

### Color
Se evitan a propósito el rojo de alarma (castiga) y el negro con un acento neón (el tópico de las apps "de productividad").

| Nombre | Hex | Uso |
|---|---|---|
| Tinta | `#171A33` | Fondo en modo oscuro, texto en modo claro |
| Superficie | `#22264A` | Tarjetas en modo oscuro |
| Niebla | `#F4F5FA` | Fondo en modo claro |
| Llave (acento) | `#FFC23D` | Acción principal, pantalla de bloqueo, la marca |
| Pacto (éxito) | `#6FD9B8` | Desbloqueado, racha cumplida, límite respetado |
| Aviso | `#FF7A6B` | Solo errores (código inválido, trampa detectada) |

Un único acento (Llave). El verde menta solo indica estado y el coral solo errores.

### Tipografía (alojada en el APK, licencias libres)
- **Bricolage Grotesque** para titulares y cifras grandes. Tiene carácter sin dejar de leerse bien.
- **Figtree** para el texto corrido y la interfaz.
- **JetBrains Mono** solo para el código de 6 dígitos, que tiene que leerse sin confundir el 1 con el 7.

### Forma
Esquinas de 16 dp en tarjetas, botones en píldora y campos de 12 dp. Una sola escala en toda la app.

---

## 6. Pantallas

### Elemento firma: la pantalla de bloqueo
Hoy es una pantalla roja que regaña. Pasa a ser una pantalla amarilla (Llave) a toda pantalla donde el protagonista es **el nombre de tu amigo**:

> Se acabó TikTok por hoy.
> 32 de 30 min
> **Pídele la llave a Carla**
> [ campo de 6 dígitos ]

Es lo que la gente fotografiará y subirá a redes. Es nuestro anuncio gratuito.

### Mapa de pantallas
1. **Bienvenida (3 pasos):** tu nombre, permisos explicados uno a uno con el porqué y el primer pacto. Termina en la pantalla de emparejar, no en la de inicio.
2. **Emparejar:** flujo guiado en dos pasos con los dos móviles cara a cara. Primero "enséñale tu QR" y después "escanea el suyo", con una animación de las dos llaves al cerrarse el pacto.
3. **Hoy (inicio):** una barra por app con el uso frente al límite, el estado del pacto ("Carla tiene tu llave") y la racha (Pro).
4. **Dar la llave:** el código grande en mono con una cuenta atrás circular de 30 s. Botón "Copiar" para mandarlo por WhatsApp.
5. **Límites:** la lista de apps con las reglas visibles ("bajar es libre, subir pide llave").
6. **Amigos:** los pactos activos y desde cuándo.
7. **Pro:** se muestra después de la primera semana o al intentar añadir una cuarta app, nunca durante la bienvenida. Siempre con un "Ahora no" bien visible.
8. **Estados vacíos con acción:** "Aún no tienes pacto. Queda con alguien y escanead vuestros QR."

---

## 7. Antes de publicar (técnico)

Arreglado el 01-10-2026:
- Uso sobrecontado (unos 8 minutos): ahora solo cuenta con la pantalla encendida y el móvil desbloqueado.
- Ya no se puede emparejar con el propio QR.
- Los límites solo se endurecen sin llave. Quitar apps, subir límites, pausar o borrar amigos pide código.

Cerrado el 01-10-2026 (segunda tanda):
- **Hora manual:** con la fecha y hora en manual, todas las apps con límite quedan bloqueadas hasta volver a hora automática. Cambiar la fecha ya no sirve para reiniciar el contador.
- **Quitar permisos:** si se quita el acceso de uso o el de superposición, o el servicio se para más de 15 minutos sin reiniciar el móvil, queda registrada una trampa. Al volver, todas las apps con límite están bloqueadas hasta que un amigo dé su llave.
- **Segundo móvil o app clonada:** un amigo nuevo no puede desbloquear hasta pasadas 24 h (salvo el primero). En la lista de amigos se ve cuánto le falta.

No se puede cerrar del todo:
- **Desinstalar o borrar datos de la app.** Android no deja impedirlo sin ser administrador total del dispositivo, y eso exige reiniciar el móvil de fábrica. El modo estricto de Pro (administrador de dispositivo) añadirá pasos que dan pereza, pero quien quiera irse siempre podrá. Es el mismo límite que tienen Opal y compañía.
- **El primer amigo puede ser un segundo móvil tuyo.** Solo se evita si el usuario quiere de verdad el pacto. Es una trampa contra uno mismo, no contra el sistema.

Hecho el 19-10-2026 (versión 1.0.0):
- Pro completo: licencias firmadas (ECDSA) que la app comprueba sin conexión, límite de 3 apps en el plan gratis, horarios de cierre, historial y rachas, modo estricto (administrador de dispositivo sin permisos reales) e iconos alternativos.
- Cobro con Stripe preparado en kimycars.com (webhook, entrega de licencias y página de gracias). Falta crear los productos y Payment Links en Stripe y poner sus enlaces en `data/Plans.kt`.
- Release firmado con clave fija y `gradlew` arreglado.
- Base de datos con migraciones: actualizar ya no borra nada.

Pendiente:
- Google Play: cuando esté la cuenta, cobrar con Google Play Billing en esa versión (Google lo exige para compras dentro de la app). La versión de Zapstore y la web siguen con Stripe.

## 8. Ideas para más adelante (sin coste fijo)
- **Pedir la llave a distancia por nostr:** la petición viaja como mensaje cifrado por relays públicos gratuitos. Sin servidor propio y encaja con el público de Zapstore.
- **Reto de 30 días:** el pacto como formato de contenido para TikTok/Instagram ("30 días con Carla de guardiana").

## Medición del uso frente a "Horas de pantalla" de Xiaomi (22-10-2026)

Kimy vio que ScreenPact le bloqueó Instagram con 2 h cuando "Horas de pantalla" de su Redmi (HyperOS, Android 16) marcaba 1 h 30. Se analizó el registro de uso del móvil por adb:
- ScreenPact coincide con los registros de Android (Instagram 182 min frente a 184 en el registro del sistema) y no cuenta tiempo con la pantalla apagada o bloqueada.
- "Horas de pantalla" de Xiaomi no cuenta el tiempo cuando desbloqueas el móvil y aparece directamente la app que ya tenías abierta (Android registra la app como abierta un instante antes del desbloqueo). Solo empieza a contar cuando abres una app con el móvil ya desbloqueado. Reproduciendo esa regla salen sus cifras hora a hora (Instagram 144 frente a 145, Telegram 19 frente a 20, TikTok 41 frente a 41).
- Esos ratos son uso real (la app delante, desbloqueada, minutos seguidos), así que la cifra correcta es la de ScreenPact.

Pendiente de decidir con Kimy: seguir contando el uso real (recomendado) y explicarlo en la app y en las preguntas frecuentes, o imitar la regla de Xiaomi.
