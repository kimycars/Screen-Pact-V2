# ScrollBan: plan de lanzamiento

Rehecho el 09-11-2026. Sustituye al plan de la etapa ScreenPact (queda en el historial de git). Las fechas son orientativas: lo único que lleva calendario es lo que depende de otras personas.

## Decisión de distribución (Kimy, 09-11-2026)

Google Play queda fuera hasta nuevo aviso. Quien vende en la UE se declara comerciante y su dirección sale en la ficha, y Kimy no tiene más dirección que la de su casa. ScrollBan se distribuye como APK.

Ventaja: Pro se sigue cobrando con Stripe, sin la comisión ni la obligación de usar el sistema de pagos de Google.

Si algún día se vuelve a Play: una dirección virtual o un apartado postal podrían resolver lo del domicilio. Falta comprobar que Google los acepta.

## Dónde se descarga

| Sitio | Para qué | Estado |
|---|---|---|
| `kimycars.com/scrollban` (APK directo) | Canal principal; a él apuntan todos los enlaces | Falta el botón y el fichero |
| Uptodown | Tienda de APK de empresa española, con consola de desarrolladores gratuita | Falta comprobar sus requisitos dentro de la consola |
| Zapstore | Público pequeño, pero Kimy ya lo usa con Vestiarium | Pendiente |

Product Hunt, G2 y los directorios de SaaS o IA no encajan (app de consumo, en español y sin tienda oficial). De directorios, solo AlternativeTo, como alternativa a Opal y a StayFree.

## El problema real: instalar un APK

Cada paso es un sitio donde se pierde gente: descargar, aviso del navegador, permitir orígenes desconocidos, aviso de Play Protect, instalar y, en Android 13 o superior, «Permitir ajustes restringidos» antes de poder dar los permisos que la app necesita.

Respuesta: **instalar con el amigo delante.** ScrollBan necesita a otra persona, así que el momento natural es en persona. Quien ya la tiene se la pasa por Quick Share o enseña el QR de descarga, le ayuda con los permisos y sellan el pacto por QR en ese momento (en persona no hay espera de 24 h). Toda la comunicación empuja a esto: «instaládla juntos».

Objeción esperada, «¿por qué no está en Play?». Respuesta honesta y corta en la web: lo hace un chaval de 17 años solo y Play obliga a publicar la dirección de casa. Además, la huella de la firma publicada para comprobar que el APK es el bueno.

## Riesgo: verificación de desarrolladores de Android (2027)

Google exige desde el 30-09-2026 en Brasil, Indonesia, Singapur y Tailandia que las apps vengan de un desarrollador verificado, y lo extenderá a todo el mundo en 2027. No hay fecha para España.

Sin verificar, instalar o actualizar pedirá activar el modo desarrollador, reiniciar y esperar un día: para este público equivale a no instalar. Con desarrollador verificado, la persona no nota ningún cambio.

Salida prevista: cuenta Full Distribution en la Android Developer Console, sin publicar en Play. Cuesta 25 $ una vez y pide documento de identidad y justificante de domicilio. Se registra `com.scrollban.app` con la huella de la firma.

Sin confirmar en la documentación de Google: si esa dirección se hace pública y si hay edad mínima. La cuenta gratuita limitada no sirve (entre 20 y 50 dispositivos; la documentación se contradice). Hacerlo antes de que llegue a España, con los primeros ingresos.

## Fases

### 0. Dejarlo listo (ahora)

Código y web (horas, Claude):
- Aviso de versión nueva dentro de la app. Sin Play no hay actualizaciones automáticas: hoy, un fallo publicado se queda para siempre. Es una conexión nueva, así que necesita el visto bueno de Kimy y una línea en la política de privacidad.
- La landing pasa de «Apúntame» a «Descargar». Se corrige la pregunta frecuente de dónde se descarga.
- Página `/scrollban/instalar`: pasos con capturas, ajustes restringidos, marcas conflictivas y huella de la firma.
- La página del pacto a distancia lleva a la descarga directa y explica que hay que volver al enlace.
- APK release 1.0.0 subido a la web.

Kimy:
- Compra real de comprobación (ver `docs/STRIPE.md`).
- Prueba en dos móviles reales, con al menos una marca distinta (Samsung o Xiaomi).
- Cuentas de ScrollBan en Instagram y TikTok.
- Grupo de Telegram para la beta.

### 1. Beta con gente conocida (unas 2 semanas)

10-15 parejas o grupos que se conozcan. Mensaje personal, sin publicar nada. Kimy instala con los primeros en persona y apunta dónde se atascan.

Pasa de fase si más del 50 % tiene un pacto activo a los 7 días y no queda ningún fallo grave abierto. Se recogen testimonios con permiso y una grabación de pantalla real.

### 2. Lanzamiento suave (principios de diciembre)

Descarga abierta, Uptodown, Zapstore, AlternativeTo y una publicación honesta en Reddit (r/nosurf, r/digitalminimalism), pidiendo opiniones y no votos. Publicaciones de Kimy en sus redes.

### 3. Reto de enero (el empujón grande)

En enero mucha gente se propone usar menos el móvil. «Enero sin scroll»: un reto con fecha, con la función de retos que ya existe, para hacer con tu grupo.

- Colaboraciones con quien ya le habla a este público; es el canal elegido en `docs/PRODUCTO.md`.
- Prensa de Android y tecnología en español. Ángulo: un chaval de 17 años que hace la app porque el móvil le robaba horas.

## Qué se mide

Con las estadísticas anónimas y el panel de `/admin`:
- Bienvenidas completadas.
- Pactos sellados (objetivo 40 % de los que instalan).
- Pactos activos a los 7 días (objetivo 30 %).
- Fallos de permisos por marca.
- Compras de Pro.

El objetivo de 500 instalaciones en 90 días se fijó pensando en una tienda. Con APK será más difícil; se revisa con los datos de la beta.

## Lo que no se hace

- Anuncios de pago: gasto cero hasta que haya ingresos.
- Abrir la descarga antes de que la instalación esté probada en dos marcas.
- Pedir valoraciones en directorios sin usuarios reales.

## Pendiente legal

Antes de abrir las ventas al público hay que aclarar quién figura como titular y qué domicilio aparece. El aviso legal de kimycars.com solo dice «Kimy», y la ley española de servicios de internet (LSSI) pide nombre, domicilio y NIF a quien vende por una web. Es el mismo problema de la dirección que con Play, y conviene consultarlo con alguien que sepa (gestoría) junto con el titular de la cuenta de Stripe.
