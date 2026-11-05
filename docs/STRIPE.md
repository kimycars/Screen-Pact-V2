# Stripe: qué crear para cobrar Scrollban Pro

Lo hace Kimy en su panel de Stripe (decidido el 05-11-2026). Primero en **modo de prueba**, se prueba un pago completo y después se repite en modo real.

El cobro ya está programado en kimycars.com: `api/sp-stripe-webhook.js` crea la licencia cuando llega el pago y `screenpact/gracias` se la entrega a la persona. La app solo necesita los cuatro enlaces de pago.

## 1. Cuatro productos con su precio

| Producto | Precio | Tipo | Metadato obligatorio |
|---|---|---|---|
| Scrollban Pro mensual | 2,99 EUR | Recurrente, cada mes | `plan` = `monthly` |
| Scrollban Pro anual | 19,99 EUR | Recurrente, cada año | `plan` = `yearly` |
| Scrollban Pro de por vida | 49,99 EUR | Pago único | `plan` = `lifetime` |
| Scrollban Pack Pacto | 29,99 EUR | Recurrente, cada año | `plan` = `pack` |

Ruta: Catálogo de productos → Añadir producto.

## 2. Un Payment Link por producto

Ruta: Payment Links → Nuevo. En cada uno:

1. Elige el producto.
2. En **Metadatos** (opciones avanzadas), añade la clave `plan` con el valor de la tabla. Sin este metadato el servidor ignora el pago y no crea licencia.
3. En **Después del pago**, elige "No mostrar la página de confirmación: redirigir a tu web" y pon exactamente:
   `https://www.kimycars.com/screenpact/gracias?session_id={CHECKOUT_SESSION_ID}`
4. Opcional: pedir el correo (Stripe lo pide siempre para suscripciones). Sirve para recuperar la licencia.

## 3. El webhook

Ruta: Desarrolladores → Webhooks → Añadir endpoint.

- URL: `https://www.kimycars.com/api/sp-stripe-webhook`
- Eventos: `checkout.session.completed` y `invoice.paid`
- Copia el **secreto de firma** (`whsec_…`) y ponlo en Vercel como variable `STRIPE_WEBHOOK_SECRET` del proyecto de kimycars. No lo pegues en el chat.
- En Vercel también tienen que estar `STRIPE_SECRET_KEY` (la clave secreta de Stripe) y `SP_LICENSE_PRIVATE_KEY` (el contenido de `Documents\claves\screenpact-license-private.pem`).

Ojo: si kimycars.com ya tiene otro webhook de Stripe para los pagos de webs con un `STRIPE_WEBHOOK_SECRET` distinto, hay que decírmelo antes, porque los dos usarían la misma variable.

## 4. Pásame los cuatro enlaces

Los enlaces `https://buy.stripe.com/…` son públicos y se pueden pegar en el chat. Van en `app/src/main/kotlin/com/screenpact/app/data/Plans.kt` (campo `link` de cada plan) y en la landing. Mientras estén vacíos, el botón de comprar sale como "Muy pronto".

## 5. Prueba

Con los enlaces de modo de prueba: compra el anual con la tarjeta `4242 4242 4242 4242` (cualquier fecha futura y CVC), comprueba que `/screenpact/gracias` muestra la licencia y que la app la acepta. Después se crean los de modo real y se cambian los enlaces.
