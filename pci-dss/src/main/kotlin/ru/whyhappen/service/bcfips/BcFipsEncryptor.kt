package ru.whyhappen.service.bcfips

import ru.whyhappen.service.Encryptor
import java.security.Key
import javax.crypto.Mac

/**
 * [Encryptor] that using HMAC SHA256 algorithm provided by
 * [Bouncy Castle FIPS](https://www.bouncycastle.org/download/bouncy-castle-java-fips/).
 */
class BcFipsEncryptor : Encryptor {
    override fun encrypt(key: Key, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256", "BCFIPS").run {
            init(key)
            doFinal(data)
        }
}