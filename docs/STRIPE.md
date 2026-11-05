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

## Pago dentro de la app (05-11-2026)

El pago se abre en una Custom Tab (pestaña del navegador superpuesta a la app, con los colores de ScrollBan), no con el pago "integrado" de Stripe ni con su SDK nativo. Motivo: con los Payment Links no hace falta otra función en Vercel (el plan Hobby ya está en el límite de 12), Stripe sigue encargándose de la verificación del banco y de Google Pay, y la app recoge la licencia sola al cerrar la pestaña. Si algún día se quiere la hoja de pago nativa (PaymentSheet del SDK de Stripe), habrá que crear los pagos desde el servidor en vez de con Payment Links. En Play Store, Google obliga a usar su sistema de pagos para Pro de todos modos.
