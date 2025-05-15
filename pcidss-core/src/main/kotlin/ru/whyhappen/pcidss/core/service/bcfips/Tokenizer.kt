package ru.whyhappen.pcidss.core.service.bcfips

import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Object to calculate HMAC and generate tokens.
 */
object Tokenizer {
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