package ru.whyhappen.pcidss.service

/**
 * Interface to tokenize sensitive fields.
 */
interface TokenService {
    /**
     * Tokenizes a field value.
     *
     * @param value the byte array to get a token for
     * @return the calculated token
     */
    suspend fun getToken(value: ByteArray): String
}