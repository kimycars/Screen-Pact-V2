package com.screenpact.app.data

import android.content.Context
import com.screenpact.app.data.crypto.PactKeys
import com.screenpact.app.data.crypto.PairingPayload
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend

/**
 * Único punto donde se sella un pacto, sea en persona (QR) o a distancia (enlace).
 *
 * En persona: cada uno escanea el QR del otro y los dos quedan emparejados.
 * A distancia: A manda su invitación; B la abre y su app le da una respuesta para mandar a A;
 * A abre la respuesta y queda emparejado. Ningún enlace lleva nada secreto (ver PactKeys).
 *
 * Para que esto no sirva para saltarse un límite:
 *  - No vale la invitación propia ni una creada en el mismo móvil (apps clonadas, segundo
 *    espacio): se detecta por el id de instalación y por la hora de encendido del móvil.
 *  - Un amigo añadido a distancia SIEMPRE espera 24 h antes de poder dar llaves, aunque sea el
 *    primero (FriendCodes.canUnlock). Así, aunque alguien se las apañe para ser su propio amigo
 *    con otro móvil, no le sirve para salir de un bloqueo en el momento.
 *  - Cada respuesta solo vale una vez y las invitaciones caducan a los 7 días.
 *
 * Pacto con iPhone: la otra mitad la hace la página web kimycars.com/screenpact/iphone con las
 * mismas cuentas (Diffie-Hellman en el navegador). Como esa página la puede abrir cualquiera,
 * un pacto con iPhone espera siempre 24 h, se haga en persona o a distancia. Tú le das a tu
 * amigo el código de Tiempo de uso de su iPhone ([IosCodes]).
 */
object Pairing {

    sealed class Result {
        /** Pacto sellado. Si [reply] no es null, hay que mandársela al amigo para que él también lo tenga. */
        data class Paired(val name: String, val reply: PairingPayload?) : Result()
        data class Failed(val message: String) : Result()
    }

    /** Invitación propia nueva (para enseñar como QR o mandar como enlace). */
    suspend fun newInvite(context: Context): Pair<PactKeys.Invite, PairingPayload> {
        val invite = PactKeys.newInvite(context)
        return invite to payloadFor(context, invite, replyTo = null)
    }

    private suspend fun payloadFor(context: Context, invite: PactKeys.Invite, replyTo: String?) = PairingPayload(
        name = Prefs.getOwnName(context).ifBlank { "Tu amigo" },
        installId = Prefs.getInstallId(context),
        publicKey = invite.publicKey,
        bootTime = PactKeys.bootTime(),
        inviteId = invite.id,
        replyTo = replyTo
    )

    /**
     * Acepta la invitación (o respuesta) [payload] de otra persona.
     * [remote] = llegó por enlace o pegada, no escaneada con la cámara.
     * [screenInvite] = la invitación propia que se está enseñando como QR (solo en persona).
     */
    suspend fun accept(context: Context, payload: PairingPayload, remote: Boolean, screenInvite: PactKeys.Invite? = null): Result {
        val db = AppDatabase.get(context)
        val myId = Prefs.getInstallId(context)
        if (payload.installId == myId) return Result.Failed("Esa invitación es tuya. El pacto tiene que ser con el móvil de otra persona.")
        if (PactKeys.sameDevice(payload.bootTime)) return Result.Failed("Esa invitación se ha creado en este mismo móvil. El pacto tiene que ser con el móvil de otra persona.")
        if (db.friendDao().getAll().any { it.peerId == payload.installId }) return Result.Failed("Ya tienes un pacto con ${payload.name}.")

        val isReply = payload.replyTo != null
        val mine = when {
            isReply -> PactKeys.pending(context, payload.replyTo!!)
                ?: return Result.Failed("Esta respuesta es de una invitación que ya se usó o caducó (duran 7 días). Mándale una nueva.")
            screenInvite != null -> screenInvite
            else -> PactKeys.newInvite(context)
        }
        val (verifyKey, generateKey) = runCatching {
            PactKeys.derive(myId, mine.privateKey, mine.publicKey, payload.installId, payload.publicKey)
        }.getOrElse { return Result.Failed("Esa invitación está dañada. Pide que te la vuelvan a mandar.") }

        val ios = payload.platform == "ios"
        db.friendDao().insert(
            Friend(
                name = payload.name,
                generateKey = generateKey,
                verifyKey = verifyKey,
                pairedRemotely = remote || isReply || ios,
                peerId = payload.installId,
                ios = ios,
                screenTimeCode = if (ios) IosCodes.newCode() else null
            )
        )
        Stats.count(context, Stats.Event.PACT_SEALED)

        // A distancia, quien recibe la invitación devuelve una respuesta. Su clave privada ya no
        // hace falta: la respuesta solo lleva la pública.
        val reply = if (!isReply && screenInvite == null) payloadFor(context, mine, replyTo = payload.inviteId) else null
        if (isReply || reply != null) PactKeys.consume(context, mine.id)
        return Result.Paired(payload.name, reply)
    }
}
