package ru.whyhappen.pcidss.service

/**
 * Interface to tokenize sensitive fields.
 */
interface TokenService {
    /**
     * Tokenizes a string.
     *
     * @param value the string to get a token for
     * @return the calculated token
     */
    suspend fun getToken(value: String): String
}