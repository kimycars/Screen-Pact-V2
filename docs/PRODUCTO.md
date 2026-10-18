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
3. **Sin cuenta.** Nada sale de tu móvil.

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

Medición sin servidor: no habrá analítica dentro de la app (sería contradecir el pilar "sin cuenta"). Se medirá con las descargas de Zapstore y de la web, los pagos en Stripe y una encuesta opcional dentro de la app que abre un enlace externo.

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
| Temas e icono alternativo | No | Sí |

Por qué los amigos no se limitan: cada amigo es una instalación nueva. Cobrar por amigos frenaría el único canal de crecimiento que tenemos.

Por qué el límite está en 3 apps: casi todo el mundo empieza por TikTok, Instagram y YouTube. Con eso el gratis es útil de verdad, y el que quiere controlar más ya está comprometido y paga.

### Precio
**Pro de por vida: 7,99 €.** Sin suscripción.
- Nosotros no tenemos costes recurrentes, así que no hay nada que la suscripción deba cubrir.
- A la gente de 18 a 30 años le cansan las suscripciones. "Pagas una vez" es un argumento en sí mismo.
- Referencias: Opal cuesta más de 60 $ al año y Brick 59 $. Frente a eso, 7,99 € una vez parece regalado.
- **Pack Pacto: 11,99 € por dos licencias**, para que la pareja de amigos lo compre a medias.

### Cómo se cobra sin servidor propio: Stripe
Stripe no tiene cuota fija, solo comisión por venta (en la UE, alrededor de 1,5 % + 0,25 € por tarjeta europea; comprobar la tarifa vigente en stripe.com antes de lanzar).

1. El botón "Hazte Pro" abre un Payment Link de Stripe (página de pago alojada por Stripe: no tocamos datos de tarjeta).
2. Cuando se completa el pago, Stripe llama por webhook a una función gratuita (Vercel o Cloudflare en plan gratis) que firma una licencia con una clave privada y se la envía al comprador por email o la muestra en la página de gracias.
3. La app guarda la licencia y la comprueba sin conexión con la clave pública incluida en el APK.

La función solo se ejecuta cuando entra un pago, y el plan gratuito de esos servicios aguanta miles al mes. No hay escenario de pérdidas.

Cuando la app esté en Play Store, Google obliga a cobrar dentro de la app con su sistema (15 %). La licencia de Stripe seguirá valiendo para quien instale el APK desde Zapstore o la web.

---

## 4. Plataformas y orden de lanzamiento

1. **Zapstore + APK en web (lanzamiento).** Sin revisión de Google. El público de Zapstore (nostr, bitcoin, privacidad) encaja exactamente con el pilar "nada sale de tu móvil". Más adelante se puede añadir pago en bitcoin para ese público.
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

> Se acabó el TikTok por hoy.
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

Pendiente:
- Modo estricto (Pro) con administrador de dispositivo.
- Arreglar `gradlew` (el jar del wrapper está roto) y preparar el APK firmado de release.
- Licencias de Pro con Stripe y firma verificable sin conexión.
- Rediseño de las pantallas con la nueva marca (mockup en el canvas de diseño).
- Textos de la app con la voz de la marca (sección 5).

## 8. Ideas para más adelante (sin coste fijo)
- **Pedir la llave a distancia por nostr:** la petición viaja como mensaje cifrado por relays públicos gratuitos. Sin servidor propio y encaja con el público de Zapstore.
- **Reto de 30 días:** el pacto como formato de contenido para TikTok/Instagram ("30 días con Carla de guardiana").
