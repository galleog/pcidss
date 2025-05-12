package ru.whyhappen.service.bcfips

import org.bouncycastle.crypto.EntropySourceProvider
import org.bouncycastle.crypto.fips.FipsDRBG
import org.bouncycastle.crypto.util.BasicEntropySourceProvider
import java.security.SecureRandom

/**
 * Holder for [SecureRandom].
 */
object RandomHolder {
    private val nonce = ByteArray(32)

    /**
     * Gets [SecureRandom] used to generate keys.
     */
    val secureRandom: SecureRandom by lazy {
        val entSource: EntropySourceProvider = BasicEntropySourceProvider(SecureRandom(), true)
        val drbgBuilder = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource)
            .setSecurityStrength(256)
            .setEntropyBitsRequired(256)
        drbgBuilder.build(nonce, false)
    }

    init {
        SecureRandom().nextBytes(nonce)
    }
}