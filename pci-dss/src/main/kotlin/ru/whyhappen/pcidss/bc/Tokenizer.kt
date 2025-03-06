package ru.whyhappen.pcidss.bc

import java.security.*
import javax.crypto.Mac
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Object to calculate HMAC and generate tokens.
 */
object Tokenizer {
    /**
     * Calculates HMAC SHA256 using [Bouncy Castle FIPS](https://www.bouncycastle.org/download/bouncy-castle-java-fips/).
     *
     * @param key the key to calculate the hash
     * @param data the [ByteArray] that should be hashed
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class
    )
    fun calculateHmac(key: Key, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256", "BCFIPS").run {
            init(key)
            doFinal(data)
        }

    /**
     * Generates a token using [SecureRandom].
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generateToken(): String {
        val randomBytes = ByteArray(24)
        RandomHolder.secureRandom.nextBytes(randomBytes)
        return Base64.encode(randomBytes)
    }
}