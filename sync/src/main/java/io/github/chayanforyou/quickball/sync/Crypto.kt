package io.github.chayanforyou.quickball.sync

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM, format base64(nonce[12] || ct+tag). Key is SHA-256(passphrase).
 * Compatible with the server: passphrase = "deviceId:teamId".
 */
class Crypto(passphrase: String) {

    private val key = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray(Charsets.UTF_8)),
        "AES"
    )
    private val rnd = SecureRandom()

    fun encrypt(plain: String): String {
        val nonce = ByteArray(NONCE).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(nonce + ct, Base64.NO_WRAP)
    }

    fun decrypt(b64: String): String {
        val raw = Base64.decode(b64, Base64.NO_WRAP)
        require(raw.size > NONCE) { "short" }
        val nonce = raw.copyOfRange(0, NONCE)
        val ct = raw.copyOfRange(NONCE, raw.size)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        return String(c.doFinal(ct), Charsets.UTF_8)
    }

    private companion object {
        const val NONCE = 12
        const val TAG_BITS = 128
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}