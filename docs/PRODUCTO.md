# ScrollBan (antes ScreenPact): definición de producto

Documento vivo. El 28-10-2026 se replanteó la estrategia entera: nuevo nombre, nuevo posicionamiento y nuevas funciones. Lo que sigue es lo vigente; al final está el historial de cómo se llegó aquí y lo técnico que sigue valiendo de la etapa ScreenPact.

## 0. Decisiones vigentes (28-10-2026)

| Tema | Decisión |
|---|---|
| Nombre | **ScrollBan**. El código y la web todavía dicen ScreenPact: el cambio se hace en el rediseño. |
| Dominio | No se compra hasta que haya ingresos. La web sigue en kimycars.com mientras tanto. Si alguien registra scrollban.com, se usa scrollban.app o scrollban.net. |
| Objetivo | Un proyecto real de portfolio con usuarios activos de verdad. Los ingresos son secundarios al principio. |
| Posicionamiento | El reto de soltar el móvil con tus amigos o tu pareja. No es un bloqueador ni un candado. |
| Público | Jóvenes (unos 16-30 años), parejas y grupos de amigos. Los exámenes no son el foco: son fechas puntuales. |
| Esencia | Entre juego o reto social y recuperar tiempo real. |
| Base del producto | La llave entre amigos: solo otra persona puede darte más tiempo. |
| Funciones nuevas | Pactos de 2 que se pueden ampliar a grupo, retos con fecha, tiempo ganado del grupo y pacto mixto con iPhone. |
| Servidor | Supabase, plan gratuito, en una cuenta nueva solo para ScrollBan (separada de Vestiarium). |
| Gasto | Cero hasta que haya ingresos: ni Supabase Pro, ni Vercel Pro, ni dominio. |
| Canal | Boca a boca (cada usuario invita al menos a otro) y colaboraciones con gente que ya habla a quien quiere usar menos el móvil. No depende de que Kimy haga contenido propio. |
| Plataformas | Android. No hay recursos para una app de iPhone; los amigos con iPhone entran por el pacto mixto. |

---

## 1. Qué es y por qué existe

**Una frase:** ScrollBan es el reto de soltar el móvil con tus amigos o tu pareja. Si te pasas de tu límite, solo ellos pueden darte más tiempo, y juntos veis cuánto tiempo habéis recuperado.

**El trabajo que el usuario contrata:** "quiero dejar de perder horas en TikTok e Instagram y sé que con fuerza de voluntad sola no basta; con mis amigos sí."

**Por qué funciona:** los bloqueadores normales se saltan con un toque, porque la persona que bloquea y la que desbloquea es la misma. ScrollBan separa esos dos papeles. Pedir la llave a un amigo cuesta un poco de vergüenza, y hacerlo en grupo convierte el esfuerzo en un pique. Esa fricción social es el producto.

**Por qué es también el canal:** nadie puede usar ScrollBan solo. Cada usuario trae, por diseño, al menos a otro. Esa es la diferencia con Vestiarium, donde la gente se registraba y no volvía porque usarlo sola no le daba nada.

### Competencia

Primera búsqueda (01-10-2026):

| App | Cómo desbloquea | Precio | Cuenta o servidor |
|---|---|---|---|
| Opal | Sesiones; un amigo aprueba salir antes | ~60-100 $/año | Sí |
| one sec | Pausa con respiración | 39,99 $/año | Sí (premium) |
| ScreenZen | Pausa antes de abrir | Gratis (donaciones) | No |
| Brick | Llavero NFC físico | 59 $ pago único | Sí |
| PeerPause, SelfBlock, Lock In, Sheppie, Screen Time Buddy | Un amigo aprueba desde su app | Suscripción | Sí, todas |

Revisión del 28-10-2026: ya hay competencia en Android.

| App | Cómo desbloquea | Precio | Cuenta | Descargas (Play) |
|---|---|---|---|---|
| FriendLock (`design.sayne.friendlock`) | El amigo aprueba con un toque o dicta un código de 6 dígitos; emparejan por QR o enlace | Gratis hasta 3 apps con anuncios; Pro con 7 días de prueba | No | 1 mil+ |
| Snitch: Screen Time Control | Tiempo compartido en grupo; desbloqueo con un gesto que el grupo ve, o aprobación del grupo | Anuncios y compras | Sí (grupos) | 10 mil+ |
| Screen Challenge Buddy | El compañero ve tu uso; retos con dinero; no aprueba desbloqueos | — | — | — |
| Trustablee | Avisa al compañero si desinstalas; centrada en pornografía | — | — | — |

FriendLock es casi la misma app que era ScreenPact: llave en manos de un amigo, código de 6 dígitos, QR o enlace, rachas, 3 apps gratis, sin cuenta y ficha en español. También existen LockPact (iOS, anuncia Android "pronto") y dos productos llamados ScreenPact (screenpact.com, un temporizador de Windows para familias, y "ScreenPact: Your Focus Partner" en iOS, con la idea del amigo con la llave). Por eso se cambia de nombre y de enfoque.

### Posicionamiento

**Para** gente joven que quiere usar menos el móvil, **ScrollBan es** el reto con tus amigos o tu pareja en el que **solo ellos pueden darte más tiempo**. **A diferencia de** FriendLock, que vende un candado, ScrollBan es un pique entre amigos: grupos, retos con fecha y tiempo recuperado. **A diferencia de** Snitch, no te desbloqueas tú solo con un gesto: hace falta la llave real de un amigo.

No es un océano azul del todo: es un mar morado. Hay competencia, pero ninguna junta la llave de un amigo, los grupos, los retos, el tiempo ganado y el pacto con amigos de iPhone, y sin anuncios.

Pilares del mensaje, por orden de peso:
1. **Juntos.** Un pacto con tu amigo, tu pareja o tu grupo. Retos con fecha y el tiempo que habéis recuperado.
2. **Tu amigo tiene la llave.** No puedes desbloquearte solo, de verdad.
3. **Da igual su móvil.** Tus amigos con iPhone también pueden hacer el pacto contigo.
4. **Sin anuncios.** Tus minutos solo los ve tu grupo.

### Qué se quita, se baja, se sube y se crea

| | Qué |
|---|---|
| Quitar | El mensaje de castigo y candado. Los anuncios. Las cuentas con correo y contraseña. |
| Bajar | Las funciones de bloqueo como argumento principal de venta. |
| Subir | Lo social: pactos de 2 que se amplían a grupo. |
| Crear | Tiempo ganado del grupo ("este mes habéis recuperado 31 h"), retos con fecha ("una semana sin TikTok") y pacto mixto con iPhone. |

---

## 2. Funciones nuevas

Detalle y orden de construcción se deciden en el rediseño. Lo acordado hasta ahora:

- **Pactos de 2 y grupos.** Se empieza con un amigo, como siempre, y el pacto se puede ampliar a un grupo (unas 3-8 personas). En grupo, cualquier miembro puede dar la llave.
- **Ranking del grupo.** Quién ha usado menos el móvil esta semana. Kimy cree que lo mirarían, pero que el perdedor no pagaría nada en la vida real: el ranking informa, no castiga.
- **Tiempo ganado.** Lo que no gastas frente a tu media se acumula y se ve en grupo. Es el gancho de "recuperar tiempo real" y lo que se comparte en el chat.
- **Retos con fecha.** Retos cortos que empiezan y acaban: "una semana sin TikTok", "un finde desconectados".
- **Pacto mixto con iPhone.** El amigo con iPhone usa el Tiempo de uso que ya trae Apple, con un código que pone su amigo de Android y que guarda ScrollBan. Es mutuo y no hay que pagar a Apple. Límite conocido: para darle más tiempo, el amigo tiene que meter el código en el iPhone o dictarlo, y si lo dicta hay que cambiarlo después en persona. Encaja bien con parejas y peor con amigos que no se ven.

Lo que no cambia: la llave entre amigos sale de Diffie-Hellman y nunca sale del móvil; endurecer es libre y aflojar pide llave; las protecciones contra trampas siguen igual.

---

## 3. Servidor (Supabase)

Hace falta un servidor pequeño para grupos, ranking, retos y tiempo ganado: los minutos de cada uno tienen que llegar a su grupo. Kimy descartó Cloudflare por miedo a facturas sorpresa. Vercel no sirve como servidor de grupos: necesitaría una base de datos aparte y su plan Hobby no permite uso comercial. Se usa Supabase y la app habla directamente con su API.

### Cuotas (precios oficiales, consultados el 28-10-2026)

- Plan gratuito: peticiones ilimitadas, 500 MB de base de datos por proyecto, 5 GB de tráfico de salida al mes y 50.000 usuarios activos al mes en Auth. 2 proyectos gratis por persona. Un proyecto se pausa tras 1 semana sin actividad.
- El tráfico y los usuarios de Auth se cuentan por organización. Por eso ScrollBan va en una cuenta propia, separada de Vestiarium.
- En el plan gratuito nunca se cobra: si se pasa una cuota, se restringe el servicio.
- Plan Pro: 25 $ al mes con tope de gasto activado por defecto, 250 GB de salida, 8 GB de disco y 100.000 usuarios al mes.

### Cálculo de capacidad

Supuestos por usuario activo al día (sin Realtime: la app consulta el grupo cuando se abre):
- Uso normal: 20 subidas de minutos (unos 0,4 KB cada respuesta), 10 consultas del grupo (unos 2 KB) y 5 peticiones más (unos 1 KB). Unos 33 KB al día, 1 MB al mes.
- Uso intenso: 64 subidas, 30 consultas y 10 más. Unos 96 KB al día, 2,9 MB al mes.
- Base de datos: detalle de 8 días por app (unas 5 apps) más totales mensuales para el tiempo ganado. Unos 6-10 KB por usuario.

Resultado con el plan gratuito, con un 50 % de margen sobre el tráfico calculado:
- Lo primero que se agota son los 5 GB de salida: unos 3.400 usuarios activos al día en uso normal y unos 1.200 en uso intenso.
- La base de datos de 500 MB da para unos 50.000 usuarios registrados.
- La máquina compartida gratuita no publica un límite de peticiones. A unas 1-10 peticiones por segundo debería bastar, pero se mide con usuarios reales.

Con el Pro, el tráfico deja de ser límite (250 GB dan para más de 100.000 activos al día) y el límite pasa a ser la potencia de la máquina. 25 $ son unos 21-23 € al mes; con la comisión de Stripe (1,5 % + 0,25 €) se cubren con unas 14 suscripciones anuales (19,99 €) o unas 9 mensuales (2,99 €). Los usuarios totales serán más que los activos al día; cuántos más depende de la retención real, así que no se da cifra.

### Privacidad

Cambia la regla de "nada sale del móvil". Ahora:
- Sube al servidor: los minutos de cada día por app y lo necesario para grupos y retos. Solo lo ve tu grupo.
- No sale nunca del móvil: las llaves de los pactos.
- La identidad es anónima: sin correo ni contraseña.
- Antes de lanzar hay que actualizar la política de privacidad y explicarlo claro en la app ("tu grupo ve tus minutos").

---

## 4. Objetivos y métricas

**Métrica estrella:** pactos activos a los 7 días. Un pacto activo es un pacto o grupo con al menos un límite activo que sigue en uso una semana después.

**Activación (el momento "ajá"):** emparejarse con alguien y poner el primer límite el mismo día. Todo el onboarding empuja hacia ahí.

**Bucle de crecimiento:** cada usuario necesita a otro, y cada grupo trae a varios. No hace falta un programa de referidos con premios: el producto ya es la invitación.

Objetivos de los primeros 90 días tras el lanzamiento (orientativos, a revisar con datos reales):
- 500 instalaciones.
- 40 % de los que instalan llegan a emparejarse.
- 30 % de los pactos siguen activos a los 7 días.
- 3-5 % de los usuarios activos compran Pro.

Medición (decidido el 19-10-2026): estadísticas anónimas dentro de la app, encendidas en la bienvenida y apagables ahí y en Ajustes. Una vez al día se envía a kimycars.com/api/scrollban-stats: versión, fabricante, número de amigos y límites, qué apps se limitan y contadores de eventos. Nunca nombres, llaves ni minutos. Con el servidor de grupos habrá que revisar si estas estadísticas pasan a Supabase.

---

## 5. Modelo de negocio

### Principio
Coste fijo cero. Solo se gasta cuando alguien paga.

### Gratis y Pro (vigente desde la 1.0.0; se revisa en el rediseño)

| | Gratis | Pro |
|---|---|---|
| Amigos emparejados | Ilimitados | Ilimitados |
| Apps con límite | 3 | Ilimitadas |
| Límite diario por app | Sí | Sí |
| Pantalla de bloqueo y llave de amigo | Sí | Sí |
| Protecciones básicas contra trampas | Sí | Sí |
| Rachas y la semana en Hoy | Sí | Sí |
| Horarios (por ejemplo, Instagram cerrado de 0:00 a 8:00) | No | Sí |
| Historial de 4 semanas, récords e insignias | No | Sí |
| Modo estricto (impide desinstalar sin llave de amigo) | No | Sí |
| Iconos alternativos | No | Sí |

Por qué los amigos no se limitan: cada amigo es una instalación nueva. Cobrar por amigos frenaría el único canal de crecimiento. Por el mismo motivo, los grupos, los retos y el tiempo ganado básicos deberían ser gratis; qué parte va a Pro se decide en el rediseño.

### Precio (decidido el 19-10-2026)

| Plan | Precio | Por qué |
|---|---|---|
| Mensual | 2,99 €/mes | Para quien no quiere pagar mucho de golpe. Hace que el anual parezca la buena opción. |
| Anual (por defecto) | 19,99 €/año | Sale a 1,67 €/mes. Plan recomendado y base de ingresos recurrentes. |
| De por vida | 49,99 € | Unos 2,5 años de anual. Para quien odia suscribirse. |
| Pack Pacto | 29,99 €/año | El anual para los dos amigos del pacto. Con grupos habrá que repensarlo. |

En la app los precios viven en `data/Plans.kt`.

### Cobro con Stripe
1. El botón de Pro abre un Payment Link de Stripe (página de pago de Stripe: no tocamos datos de tarjeta).
2. Al completarse el pago, Stripe llama por webhook a una función de kimycars.com que firma una licencia con la clave privada y la entrega en la página de gracias.
3. La app comprueba la licencia sin conexión con la clave pública que lleva dentro.

Falta crear los productos y Payment Links en Stripe y poner sus enlaces en `data/Plans.kt`. Cuando la app esté en Play Store, Google obliga a cobrar con su sistema (15 %); la licencia de Stripe seguirá valiendo fuera de Play.

Riesgo conocido: el plan Hobby de Vercel solo permite uso personal no comercial. Si kimycars.com está en Hobby, vender webs y firmar licencias desde ahí incumple sus condiciones, y Vercel podría pausar o cerrar la cuenta (con la web y las licencias). Pasarse de los límites de Hobby no cobra, pero pausa la función 30 días. Opciones: aceptar el riesgo mientras haya pocos ingresos, pagar Pro (20 $ al mes) o mover el webhook y las licencias a Supabase al montar el servidor de grupos (falta comprobar que el plan gratuito de Supabase permite uso comercial). Pendiente de que Kimy confirme su plan de Vercel.

---

## 6. Plataformas y orden de lanzamiento

1. **Zapstore y APK en la web (lanzamiento).** Sin revisión de Google.
2. **Play Store (cuando haya tracción).** 25 $ una vez. Habrá que justificar ante Google el acceso de uso y la superposición de pantalla: bienestar digital elegido por el propio usuario.
3. **iOS: sin app propia.** Apple exige su API de Tiempo de uso, un Mac y 99 $ al año. Los amigos con iPhone entran con el pacto mixto (Tiempo de uso de Apple con un código que guarda su amigo de Android).

---

## 7. Nombre y dominio

**Elegido: ScrollBan** (Kimy, 28-10-2026). Es el más fácil de entender para un público que no habla inglés.

Comprobado el 28-10-2026:
- **Dominio:** scrollban.com está libre y a precio normal, no premium: 11,08 $ al año en Porkbun, renovación igual. scrollban.app cuesta 8,75 $ el primer año y 14,93 $ después. No se compra hasta que haya ingresos.
- **Play Store:** no hay ninguna app con ese nombre. Lo más parecido es "ScrollBank".
- **Marcas** (TMview: OEPM, EUIPO, USPTO y UKIPO): nada en España ni en la UE. En EE. UU. hay una solicitud de SCROLLBANE (USPTO 50110329, 15-09-2026, clase 9 de software, a nombre de una persona; no se ha encontrado app publicada) y en Reino Unido está registrada SCROLLBAND (clases 9 y 26). En España y la UE no hay choque; en EE. UU. ScrollBane podría oponerse si algún día se lanza allí.
- **Pendiente:** App Store y cuentas de Instagram y TikTok.

Cómo se llegó al nombre: se descartaron nombres en español (Kimy prefiere inglés porque casi ninguna app usa una palabra en español), nombres con "scroll" que sonaban a hacer más scroll (Scrollduel, Outscrolled), Scrollquitters (existe ScrollQuitter en iOS), Scrollvow (en España no se entendería) y Scrolldiet (existe una extensión de Chrome "Scroll Diet" y es más genérico). Ojo: hay muchas apps con "scroll" en el nombre (ScrollStop, ScrollFree, ScrollGuard, ScrollToll, Scroll Brake…), así que la marca tiene que distinguirse por el producto y el tono.

---

## 8. Identidad de marca (de la etapa ScreenPact; se revisa en el rediseño)

### Personalidad
Cómplice, directa, con algo de humor y nunca de sermón. Habla como el amigo que te quita el móvil en la cena, no como un médico.

- Sí: "Te has comido tus 30 minutos de TikTok. Pídele la llave a Carla."
- No: "Has superado tu límite de uso diario. Introduce el código de verificación."

### Metáfora
La llave: tú tienes la de tu amigo y él tiene la tuya. Se habla de llave y de pacto, no de código ni de emparejamiento. Con ScrollBan se suma la idea de reto y de grupo.

### Color, tipografía y forma (actuales)

| Nombre | Hex | Uso |
|---|---|---|
| Tinta | `#171A33` | Fondo en modo oscuro, texto en modo claro |
| Superficie | `#22264A` | Tarjetas en modo oscuro |
| Niebla | `#F4F5FA` | Fondo en modo claro |
| Llave (acento) | `#FFC23D` | Acción principal, pantalla de bloqueo, la marca |
| Pacto (éxito) | `#6FD9B8` | Desbloqueado, racha cumplida, límite respetado |
| Aviso | `#FF7A6B` | Solo errores |

Tipografías: Bricolage Grotesque (titulares y cifras), Figtree (texto e interfaz) y JetBrains Mono (solo la llave de 6 dígitos). Esquinas de 16 dp en tarjetas, botones en píldora y campos de 12 dp.

---

## Rediseño: decisiones de partida (Kimy, 28-10-2026)

- Identidad: se evoluciona la actual (tinta y amarillo Llave) con el nombre nuevo. El logo se mantiene: el "candado de dos" actual (Kimy, 28-10-2026; se descartaron "prohibido con llave" y "dos llaves en X"). No se rehace el sistema de diseño.
- Id de la app: pasa de com.scrollban.app a com.scrollban.app (hecho el 28-10-2026). La versión de pruebas anterior hay que desinstalarla; sus datos se pierden. El paquete de Kotlin sigue siendo com.scrollban.app porque es interno.
- El nombre visible sale de una sola constante, `Brand.NAME`, y del `app_name` de strings.xml.
- Los esquemas de enlace (`scrollban://license`, `scrollban://pair`) y la ruta web `kimycars.com/scrollban/...` no cambian hasta rehacer la web, para no romper las licencias ni las invitaciones.
- Orden: primero identidad y pantallas con lo que funciona sin servidor (tiempo ganado propio y retos de dos); mientras, Kimy crea la cuenta de Supabase; después grupos y ranking.
- La web (landing, página del pacto y privacidad) se rehace después de la app.

## Construcción en tandas (Kimy, 05-11-2026)

Kimy ya tiene cuenta de Supabase y de Stripe. Decidido:
- Primero todo lo que funciona sin servidor; después lo que necesita Supabase.
- Las tablas de Supabase las monta Claude desde el panel de Kimy en Chrome, con la URL del proyecto y la clave publishable (pública, va dentro de la app). La clave secreta nunca pasa por el chat.
- Los productos y Payment Links de Stripe los crea Kimy siguiendo `docs/STRIPE.md` y pasa los cuatro enlaces.
- Qué va en Gratis y qué en Pro con las funciones nuevas se decide cuando esté todo montado. Hasta entonces, todo lo nuevo está abierto.

Hecho sin servidor (05-11-2026):
- **Tiempo ganado en Hoy.** La primera vez que una app tiene límite se guarda su media diaria de los 7 días anteriores (tabla `baselines`). Cada día terminado se compara con esa media; es neto (un día peor que antes resta) y no cuenta el día de hoy. La tarjeta enseña lo recuperado en los últimos 7 días terminados, antes frente a ahora y el total. Lógica en `data/TimeWon.kt`. En las apps que ya tenían límite antes de esta versión, la media sale de días ya limitados y se queda corta.
- **Retos con fecha** (pestaña Juntos). Una app con límite, un tope al día (nada, 15 o 30 min) y 3, 7 o 14 días. Empieza hoy si hoy aún vas dentro del tope; si no, mañana. Se mide con el registro diario de las rachas, con 1 minuto de margen. No bloquea nada por sí mismo. Se puede dejar sin llave (no afloja ningún límite). El resultado se comparte por el chat. Cada reto lleva un UUID para poder subirlo al grupo cuando haya servidor. Lógica en `data/Challenges.kt`.
- **Amigos pasa a llamarse Juntos** y va en medio de la barra: Hoy, Juntos, Límites.
- Base de datos v6 (tablas `baselines` y `challenges`, con su migración).

Segunda tanda, con Supabase (hecho el 05-11-2026, probado en el emulador con un amigo simulado y con pruebas directas al servidor):
- **Proyecto**: ScrollBan (`rsdtrjecytejtlihulof`), plan gratuito. Tablas y funciones en `supabase/schema.sql`, ejecutado desde el editor SQL. Inicio de sesión anónimo activado. Sin captcha por ahora.
- **Acceso**: ninguna tabla se lee ni se escribe directamente (RLS sin políticas y permisos retirados). Todo pasa por cinco funciones que comprueban la identidad: `join_group`, `leave_group`, `push_usage`, `my_board` y `delete_my_data`. Comprobado que una identidad ajena no ve nada, que un secreto equivocado no entra en un grupo y que la clave publishable sola no puede llamar a nada.
- **Grupos**: cada pacto es un grupo de 2. Los dos móviles calculan el mismo id de grupo y el mismo secreto con las llaves del pacto (`Together.groupOf`), así que vale también para los pactos ya sellados y no hay que mandar nada más. El servidor guarda solo el hash del secreto. Tope de 8 personas por grupo y 50 grupos por persona, pensado para los grupos de 3 o más.
- **Qué sube**: por día y app con límite, los minutos, el límite, las llaves usadas y la media de antes; y el reto activo. No suben nombres (cada móvil usa el nombre que ya tiene del amigo) ni llaves. Se guarda 60 días.
- **Opcional y apagado de inicio**: en Juntos sale "Ranking con tus amigos" con lo que se comparte explicado antes de pulsar "Activar el ranking". En Ajustes hay un interruptor; al apagarlo se borra del servidor la identidad y todo lo suyo.
- **Cuándo sube**: el servicio sube cada 15 minutos mientras funciona (aparte del bucle de bloqueo, para que la red nunca retrase un bloqueo), y la pantalla Juntos sube y pide el grupo al abrirse y cada minuto mientras está abierta.
- **En Juntos**: lo que habéis recuperado juntos en los últimos 30 días (con botón de compartir); el ranking de los últimos 7 días por uso de las apps con límite, de menos a más, con lo que ha recuperado cada uno; quien no ha activado el ranking sale al final sin cifras; los retos de los amigos con sus días y "Unirme al reto" si tienes límite en esa app; tu reto dice "Con Lucía" si lo hacéis juntos. Romper un pacto saca del grupo.
- **Unirse a un reto** (Kimy, 05-11-2026): cuenta desde que te unes. Es el mismo reto (mismo id, app y tope) y acaba el mismo día que el de tu amigo, pero para ti empieza hoy (o mañana si hoy ya te has pasado del tope), así que tiene menos días. La tarjeta lo avisa: "Si te unes, cuenta desde hoy: quedan 5 días".
- El código de Supabase vive en `data/cloud/` (`Cloud.kt` es la única conexión con el servidor; `Together.kt`, la lógica). La cuenta del tiempo ganado y del progreso de los retos sirve igual para tus datos y los de un amigo (`TimeWon.summaryOf`, `Challenges.progressOf`).

Tercera tanda: grupos de 3 o más (hecho el 05-11-2026, probado en el emulador con dos amigos simulados y con pruebas directas al servidor):
- **Cómo se forma**: con el ranking activado, cualquiera crea un grupo con un nombre y mete a amigos con los que ya tiene pacto. El invitado ve la invitación en Juntos ("Lucía te invita a su grupo") y decide si entra. Tope de 8 personas contando invitaciones pendientes. Solo se puede invitar a alguien con quien ya compartes un pacto (lo comprueba el servidor), cuyo pacto ya da llaves (pasó la espera de 24 h) y que tiene el ranking activado.
- **Llaves**: cualquiera del grupo puede darte la llave. Cada grupo tiene un secreto de 32 bytes; la llave de cada miembro sale de ese secreto y de su id de instalación (`data/crypto/GroupKeys.kt`). Las llaves de un grupo valen 24 h después de entrar en él, siempre (`FriendCodes.canUnlock(group)`). Si alguien del grupo también tiene pacto contigo, se le da la llave del pacto. La pantalla de bloqueo nombra también a los grupos ("Pídele la llave a … o a alguien del grupo Piso").
- **Qué viaja y cifrado**: el secreto del grupo pasa de quien invita al invitado por el servidor, cifrado con una clave que sale de su pacto (AES-GCM). Cada miembro sube su nombre y su id de instalación cifrados con el secreto del grupo, para que el resto le vea aunque no tenga pacto con él. El servidor guarda esas cajas cifradas y el hash del secreto, pero no puede leer nada. Es un dato nuevo que sube (cifrado): hay que contarlo en la política de privacidad.
- **Salir**: salir de un grupo pide llave, igual que romper un pacto (te deja con menos gente que pueda dártela). Apagar el ranking borra todo del servidor pero los grupos se quedan en el móvil y sus llaves siguen valiendo; al volver a activarlo se vuelve a entrar.
- **En Juntos**: invitaciones arriba; el ranking y lo recuperado juntos incluyen a la gente de tus grupos aunque no tengas pacto con ella; sus retos salen y te puedes unir; cada grupo es una tarjeta con sus miembros, "Darle su llave", "Añadir a un amigo" y "Salir del grupo".
- **Servidor**: columna `profile` en `group_members`, tabla `group_invites` y funciones `set_profile`, `invite_to_group` y `decline_invite` (en `supabase/schema.sql`, ya ejecutado). `join_group` borra la invitación al entrar y `my_board` devuelve los perfiles cifrados y las invitaciones.
- Base de datos v7 (tabla `pact_groups`, con su migración).

Pendiente de esta parte (la política de privacidad ya está actualizada, 06-11-2026): decidir si hace falta captcha contra registros falsos, limpiar identidades anónimas abandonadas (por ejemplo, una tarea mensual que borre las que llevan 90 días sin subir nada), probar con dos móviles reales.

Cuarta tanda: pacto mixto con iPhone (hecho el 05-11-2026; la app está lista y probada en el emulador contra la página web servida en local; páginas web desplegadas en kimycars.com el 05-11-2026 con el OK de Kimy). Kimy cree que poca gente con iPhone lo usará y no hay dinero para una app de iPhone, así que no es prioridad:
- **Cómo se sella**: en "Sellar un pacto" se elige "Tiene iPhone". El QR y el enlace abren `kimycars.com/scrollban/iphone`. Allí el amigo pone su nombre y pulsa "Sellar el pacto": su navegador hace el mismo Diffie-Hellman que la app (`scrollban/pacto-ios.js` en la web) y le enseña un QR con la respuesta (o se la manda por WhatsApp). El de Android la escanea o la abre y el pacto queda cerrado. La clave privada no sale del iPhone y nada pasa por ningún servidor.
- **Él te da la llave a ti**: desde la página `kimycars.com/scrollban/llave`, con un código de 6 cifras que cambia cada 30 s, igual que en la app. La llave va tras "#" en el enlace y en el almacenamiento del navegador del iPhone; se recomienda añadir la página a la pantalla de inicio. Esa página no lleva analítica.
- **Tú le das la llave a él**: con el código de 4 cifras de Tiempo de uso de su iPhone, que genera ScrollBan (sin códigos fáciles) y que pones tú en su iPhone sin que lo vea. En Juntos, "Ver su código de Tiempo de uso" lo enseña con los pasos. "Cambiar el código" propone uno nuevo y solo lo guarda cuando confirmas que ya lo has cambiado en su iPhone, para no perder el que vale.
- **Seguridad**: un pacto con iPhone espera siempre 24 h antes de dar llaves, aunque se haga en persona, porque la página web la puede abrir cualquiera (también tú mismo en otro navegador). Los amigos con iPhone no tienen la app, así que no salen en el ranking ni se pueden meter en grupos.
- Base de datos v8 (columnas `ios` y `screenTimeCode` en `friends`). La respuesta del iPhone lleva `"o": "ios"` (`PairingPayload.platform`). Código de Tiempo de uso en `data/IosCodes.kt` y `ui/friends/IosCodeScreen.kt`.
- **Por comprobar con un iPhone real**: los nombres exactos de los menús de Tiempo de uso en español (ahora dice "Bloquear ajustes de Tiempo de uso", "Límites de uso", "Bloquear al final del límite" y "Cambiar código de Tiempo de uso"); si al poner el código pide un Apple ID para recuperarlo (si se puede recuperar con el Apple ID del dueño, podría quitarse el código sin su amigo); y que "Añadir a pantalla de inicio" guarde el enlace de la llave con lo que va tras "#".

## 9. Pendientes

- **Renombrar todo a ScrollBan antes de montar Stripe** (Kimy, 05-11-2026). Se escribe **ScrollBan**, con B mayúscula: separa las dos palabras al leerlo. En direcciones, ids y código va en minúsculas (`scrollban`). Qué cambia:
  - Textos de la app donde aún pone ScreenPact.
  - Paquete Kotlin de `com.scrollban.app` a `com.scrollban.app`. El applicationId ya es `com.scrollban.app`, así que no se pierden datos.
  - La web pasa de `kimycars.com/scrollban` a `kimycars.com/scrollban`, con redirecciones desde las rutas viejas para que los enlaces de pacto ya mandados sigan funcionando.
  - Enlace de la app `scrollban://`, aceptando también `scrollban://`.
  - Docs y nombres del servidor de licencias.
  - Al dar de alta Stripe, la web de la empresa será `https://www.kimycars.com/scrollban`.

- Rediseño: nombre en la app y en la web, funciones nuevas, pantallas y, si hace falta, identidad visual.
- Servidor de grupos en Supabase y grupos de 3 o más: hecho el 05-11-2026; falta probarlo con dos móviles reales.
- Pacto mixto con iPhone: hecho el 05-11-2026. Páginas web desplegadas el mismo día; falta probarlo con un iPhone real. Baja prioridad.
- Política de privacidad y textos de la app con el nuevo reparto de datos.
- Revisar qué va en Gratis y qué en Pro con las funciones nuevas, y el Pack Pacto.
- Confirmar el plan de Vercel de kimycars.com.
- Comprobar App Store y cuentas de Instagram y TikTok para ScrollBan.
- Stripe: productos y Payment Links (Kimy, guía en `docs/STRIPE.md`).
- Copia de la carpeta `Documents\claves` (Kimy).

---

## 10. Lo técnico que sigue valiendo

### Protecciones (01-10-2026)
- El uso solo cuenta con la pantalla encendida y el móvil desbloqueado.
- No se puede emparejar con uno mismo.
- Los límites solo se endurecen sin llave. Quitar apps, subir límites, pausar o borrar amigos pide llave.
- Hora manual: con la fecha y hora en manual, todas las apps con límite quedan bloqueadas hasta volver a hora automática.
- Quitar permisos: si se quita el acceso de uso o el de superposición, o el servicio se para más de 15 minutos sin reiniciar el móvil, queda registrada una trampa y todo queda bloqueado hasta que un amigo dé su llave.
- Un amigo nuevo no puede desbloquear hasta pasadas 24 h (salvo el primero en persona).

No se puede cerrar del todo:
- Desinstalar o borrar datos de la app. El modo estricto de Pro (administrador de dispositivo) añade pasos, pero quien quiera irse siempre podrá. Es el mismo límite que tienen todas.
- El primer amigo puede ser un segundo móvil propio. Es una trampa contra uno mismo, no contra el sistema.

### Versión 1.0.0 (19-10-2026)
- Pro completo: licencias firmadas (ECDSA) comprobadas sin conexión, límite de 3 apps gratis, horarios, historial y rachas, modo estricto e iconos alternativos.
- Cobro con Stripe preparado en kimycars.com (webhook, entrega de licencias y página de gracias).
- Release firmado con clave fija. Base de datos con migraciones: actualizar no borra nada.

### Medición del uso frente a "Horas de pantalla" de Xiaomi (22-10-2026)
Kimy vio que la app le bloqueó Instagram con 2 h cuando "Horas de pantalla" de su Redmi marcaba 1 h 30. Por adb se vio que la app coincide con los registros de Android (182 frente a 184 min) y que Xiaomi no cuenta el tiempo cuando desbloqueas el móvil y aparece directamente la app que ya tenías abierta. Reproduciendo esa regla salen sus cifras hora a hora. Esos ratos son uso real, así que la cifra correcta es la nuestra. Decidido por Kimy: se sigue contando el uso real (imitar a Xiaomi abriría una trampa). En la landing hay una pregunta frecuente que lo explica.

### Pactos a distancia (25-10-2026)
Un pacto se puede sellar por enlace: A manda su invitación, B la abre y su app le da una respuesta que manda a A. Para que no sirva para saltarse un límite:
- Ni el QR ni el enlace llevan nada secreto, solo una clave pública. Las llaves salen de Diffie-Hellman en cada móvil. Esto arregló un agujero: el QR antiguo llevaba legible la clave que desbloqueaba a quien lo enseñaba.
- No vale una invitación propia ni una creada en el mismo móvil (apps clonadas o segundo espacio).
- Un pacto a distancia espera siempre 24 h antes de poder dar llaves, aunque sea el primero.
- Cada respuesta vale una vez y las invitaciones caducan a los 7 días.
- Límite conocido: con un segundo móvil uno puede ser su propio amigo; la espera de 24 h hace que no sirva para salir de un bloqueo en el momento.
- Los QR de versiones anteriores ya no valen.

---

## 11. Historial: cómo se llegó a ScrollBan (28-10-2026)

1. Kimy descubrió que ScreenPact y screenpact.com ya existían. Al revisar la competencia apareció FriendLock, casi la misma app en Android, además de Snitch y otras.
2. Kimy pidió definir la estrategia para no entrar en un océano rojo. Respuestas clave: quiere un proyecto real de portfolio con usuarios activos (Vestiarium tiene unos 15 usuarios y ninguno activo, y Kimycars dos clientes amigos); hoy no llega a casi nadie; en su entorno hay mitad iPhone y mitad Android; le funcionan mejor las colaboraciones que las redes; está dispuesto a cambiar lo que haga falta.
3. Se eligió el posicionamiento de reto social y las funciones nuevas (grupos, retos, tiempo ganado, pacto mixto con iPhone). Kimy aceptó un servidor siempre que se supiera cuántos usuarios caben gratis.
4. Servidor: Kimy descartó Cloudflare y se calculó con Supabase. Vercel queda solo para la web.
5. Nombre: tras varias rondas, ScrollBan.

Decisiones de partida que siguen vigentes (01-10-2026): modelo freemium con cobro por Stripe; distribución primero en Zapstore y APK desde la web; ningún coste fijo sin ingresos detrás. El público original (18-30) se amplía a jóvenes de unos 16-30, parejas y grupos.
