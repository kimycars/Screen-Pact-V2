# Stripe: qué crear para cobrar ScrollBan Pro

Lo hace Kimy en su panel de Stripe (decidido el 05-11-2026). Primero en **modo de prueba**, se prueba un pago completo y después se repite en modo real.

El cobro ya está programado en kimycars.com: el webhook (`lib/scrollban-webhook.js`, servido por `api/scrollban-license.js`) crea la licencia cuando llega el pago y `scrollban/gracias` se la entrega a la persona. La app solo necesita los cuatro enlaces de pago.

## 0. Cuenta nueva de ScrollBan

En el mismo inicio de sesión que Vestiarium: menú de la cuenta (arriba a la izquierda) → Nueva cuenta → nombre "ScrollBan". Web de la empresa: `https://www.kimycars.com/scrollban`. Descriptor del extracto bancario: `SCROLLBAN`. Así los cobros, las claves y los webhooks no se mezclan con Vestiarium ni con el cobro de webs de clientes.

Conviene activar el portal de clientes (Configuración → Facturación → Portal de clientes) para que quien tenga una suscripción pueda cancelarla sola.

## 1. Cuatro productos con su precio

| Producto | Precio | Tipo | Metadato obligatorio |
|---|---|---|---|
| ScrollBan Pro mensual | 2,99 EUR | Recurrente, cada mes | `plan` = `monthly` |
| ScrollBan Pro anual | 19,99 EUR | Recurrente, cada año | `plan` = `yearly` |
| ScrollBan Pro de por vida | 49,99 EUR | Pago único | `plan` = `lifetime` |
| ScrollBan Pack Pacto | 29,99 EUR | Recurrente, cada año | `plan` = `pack` |

Ruta: Catálogo de productos → Añadir producto.

## 2. Un Payment Link por producto

Ruta: Payment Links → Nuevo. En cada uno:

1. Elige el producto.
2. En **Metadatos** (opciones avanzadas), añade la clave `plan` con el valor de la tabla. Sin este metadato el servidor ignora el pago y no crea licencia.
3. En **Después del pago**, elige "No mostrar la página de confirmación: redirigir a tu web" y pon exactamente:
   `https://www.kimycars.com/scrollban/gracias?session_id={CHECKOUT_SESSION_ID}`
4. Opcional: pedir el correo (Stripe lo pide siempre para suscripciones). Sirve para recuperar la licencia.

## 3. El webhook

Ruta: Desarrolladores → Webhooks → Añadir endpoint.

- URL: `https://www.kimycars.com/api/scrollban-stripe-webhook`
- Eventos: `checkout.session.completed` y `invoice.paid`
- Copia el **secreto de firma** (`whsec_…`) y ponlo en Vercel como variable `SCROLLBAN_STRIPE_WEBHOOK_SECRET` del proyecto de kimycars. No lo pegues en el chat.
- En Vercel también tienen que estar `SCROLLBAN_STRIPE_SECRET_KEY` (la clave secreta de Stripe; no `STRIPE_SECRET_KEY`, que es del cobro de webs de clientes) y `SCROLLBAN_LICENSE_PRIVATE_KEY` (el contenido de `Documents\claves\screenpact-license-private.pem`).

Las variables de ScrollBan llevan el prefijo `SCROLLBAN_` para no chocar con `STRIPE_SECRET_KEY`, que usa el cobro de webs de clientes (`api/pago.js`).

## 4. Pásame los cuatro enlaces

Los enlaces `https://buy.stripe.com/…` son públicos y se pueden pegar en el chat. Van en `app/src/main/kotlin/com/scrollban/app/data/Plans.kt` (campo `link` de cada plan) y en la landing. Mientras estén vacíos, el botón de comprar sale como "Muy pronto".

## 5. Prueba

Con los enlaces de modo de prueba: compra el anual con la tarjeta `4242 4242 4242 4242` (cualquier fecha futura y CVC), comprueba que `/scrollban/gracias` muestra la licencia y que la app la acepta. Después se crean los de modo real y se cambian los enlaces.

## Estado (05-11-2026)

Cuenta "ScrollBan" creada en Stripe. Probado en modo de prueba: compra con redirección a la página de gracias y compra cerrando la pestaña nada más pagar (la app recogió la licencia anual sola al volver). Los cuatro Payment Links de **modo de prueba** están en `data/Plans.kt` y las tres variables `SCROLLBAN_*` están en Vercel (Production), con la web ya redesplegada. Falta la compra de prueba completa y después repetirlo todo en modo real (productos, enlaces, webhook y variables con las claves `sk_live_`). La landing no lleva botones de compra a propósito: Pro se compra desde la app, porque sin la app la licencia no sirve.

## Pasar a modo real (06-11-2026)

El servidor ya acepta los dos modos a la vez: las variables `SCROLLBAN_STRIPE_SECRET_KEY` y `SCROLLBAN_STRIPE_WEBHOOK_SECRET` son las del modo real y, si existen `SCROLLBAN_STRIPE_TEST_SECRET_KEY` y `SCROLLBAN_STRIPE_TEST_WEBHOOK_SECRET`, también se aceptan los avisos del modo de prueba. La app usa los enlaces de prueba en la versión de pruebas y los reales en la que se publica (`data/Plans.kt`, `TEST_LINKS` y `LIVE_LINKS`). El aviso de Telegram marca las compras de prueba con «[modo prueba]».

Pasos (los hace Kimy en Stripe y en Vercel; las claves no pasan por el chat):

1. **Activar la cuenta para cobrar.** Stripe pide los datos de quien cobra: tipo de negocio, documento de identidad, dirección, IBAN para los pagos y la web (`https://www.kimycars.com/scrollban`). Stripe exige que el representante de la cuenta sea mayor de edad; con 17 años, el titular tiene que ser un padre o una madre (o esperar a los 18). Comprobarlo en la ayuda de Stripe antes de rellenar nada.
2. **Copiar los productos.** En modo de prueba, en cada uno de los cuatro productos: botón «Copiar en modo real». El precio y el tipo (mensual, anual, único) se copian.
3. **Crear los cuatro Payment Links en modo real.** Los enlaces no se copian: se crean igual que en la sección 2, con el metadato `plan` y la redirección a `https://www.kimycars.com/scrollban/gracias?session_id={CHECKOUT_SESSION_ID}`.
4. **Webhook en modo real.** Igual que en la sección 3 (misma URL, mismos dos eventos). Da un secreto `whsec_…` nuevo.
5. **Clave de la API.** Mejor una clave restringida (Desarrolladores → Claves de API → Crear clave restringida) con un solo permiso: Suscripciones, lectura. Es lo único que usa el servidor. Si se filtra, no sirve para cobrar ni para devolver dinero.
6. **Variables en Vercel** (proyecto kimy-cars, Production):
   - Crear `SCROLLBAN_STRIPE_TEST_SECRET_KEY` y `SCROLLBAN_STRIPE_TEST_WEBHOOK_SECRET` con los valores de prueba que hay ahora (si Vercel no deja verlos, se sacan otra vez de Stripe en modo de prueba).
   - Cambiar `SCROLLBAN_STRIPE_SECRET_KEY` por la clave real del paso 5 y `SCROLLBAN_STRIPE_WEBHOOK_SECRET` por el secreto del paso 4.
   - `SCROLLBAN_LICENSE_PRIVATE_KEY` no cambia.
   - Avisar a Claude para redesplegar: Vercel solo lee las variables nuevas en el siguiente despliegue.
7. **Portal de clientes en modo real.** La configuración va por modo: activarlo también en modo real.
8. **Pasar los cuatro enlaces reales** (`https://buy.stripe.com/…`, sin `test_`). Van en `LIVE_LINKS` de `data/Plans.kt`.
9. **Compra real de comprobación.** Con la versión que se publica, comprar el mensual con una tarjeta propia, comprobar que llega la licencia y el aviso de Telegram sin «[modo prueba]», y después devolver el pago y cancelar la suscripción desde Stripe. La licencia sigue valiendo hasta su fecha de fin.

### Estado del paso a real (06-11-2026)

Hecho: productos, marca y Payment Links copiados al modo real (Stripe ofrece copiarlos, con metadatos y redirección revisados por Kimy); webhook real creado y su secreto puesto en `SCROLLBAN_STRIPE_WEBHOOK_SECRET`; enlaces reales en `LIVE_LINKS`; web redesplegada. Kimy borró el secreto del webhook de prueba y no creó las variables `_TEST_`: las compras de prueba de la versión debug ya no dan licencia (si se quiere volver a probar, se crean esas dos variables con un webhook de prueba nuevo).

Falta: la clave restringida real (Suscripciones, lectura) en `SCROLLBAN_STRIPE_SECRET_KEY`, que ahora sigue siendo la de prueba. Mientras tanto, las suscripciones reales no crean licencia (el webhook no puede leer la suscripción y Stripe reintenta hasta 3 días); el pago de por vida sí funciona. Portal de clientes en real configurado el 06-11-2026 (cancelar al final del periodo, sin cambio de plan). Después de la clave: redesplegar y la compra real de comprobación.

## Pago dentro de la app (05-11-2026)

El pago se abre en una Custom Tab (pestaña del navegador superpuesta a la app, con los colores de ScrollBan), no con el pago "integrado" de Stripe ni con su SDK nativo. Motivo: con los Payment Links no hace falta otra función en Vercel (el plan Hobby ya está en el límite de 12), Stripe sigue encargándose de la verificación del banco y de Google Pay, y la app recoge la licencia sola al cerrar la pestaña. Si algún día se quiere la hoja de pago nativa (PaymentSheet del SDK de Stripe), habrá que crear los pagos desde el servidor en vez de con Payment Links. En Play Store, Google obliga a usar su sistema de pagos para Pro de todos modos.
