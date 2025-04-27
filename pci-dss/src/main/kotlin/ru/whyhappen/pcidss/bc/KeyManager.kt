package ru.whyhappen.pcidss.bc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Manager for the keystore and secret key the application uses to hash ISO8583 secret fields.
 * Uses Bouncy Castle FIPS keystore to save keys.
 */
class KeyManager(
    /**
     * Path to the keystore to save secret keys.
     */
    keystorePath: String,
    /**
     * Keystore password.
     */
    private val keystorePassword: String,
    /**
     * Key password.
     */
    private val keyPassword: String,
    /**
     * Current key alias.
     */
    private val currentKeyAlias: String,
    /**
     * Previous key alias.
     */
    private val previousKeyAlias: String
) {
    val path: Path = Path.of(keystorePath)

    internal lateinit var currentKey: Key
    internal var previousKey: Key? = null

    /**
     * Verifies if the keystore exists.
     */
    private val keystoreExists: Boolean
        get() = Files.exists(path)

    /**
     * Reads keys from the keystore or creates it if it doesn't exist.
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    fun init() {
        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        if (keystoreExists) {
            // read keys from the keystore
            Files.newInputStream(path, READ).use {
                keystore.load(it, keystorePassArray)
            }

            currentKey = keystore.getKey(currentKeyAlias, keyPassArray)
            previousKey = keystore.getKey(previousKeyAlias, keyPassArray)

            logger.info("Keystore $path has been read")
        } else {
            // create a new keystore
            keystore.load(null, null)

            currentKey = generateHmacKey()
            keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)

            logger.info("New keystore $path has been created")

            Files.newOutputStream(path, WRITE, CREATE_NEW).use {
                keystore.store(it, keystorePassArray)
            }
        }
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class
    )
    fun updateSecretKey() {
        require(keystoreExists) { "Keystore $path doesn't exist" }

        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        keystore.load(null, null)

        // generate a new key and store the previous one under the previous key alias
        previousKey = currentKey
        currentKey = generateHmacKey()
        keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)
        keystore.setKeyEntry(previousKeyAlias, previousKey, keyPassArray, null)

        logger.info("Secret key has been updated in keystore $path")

        Files.newOutputStream(path, WRITE, TRUNCATE_EXISTING).use {
            keystore.store(it, keystorePassArray)
        }
    }

    /**
     * Calculates HMAC SHA256 using [Bouncy Castle FIPS](https://www.bouncycastle.org/download/bouncy-castle-java-fips/).
     *
     * @param data the [ByteArray] that should be hashed
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class
    )
    fun calculateCurrentHmac(data: ByteArray): ByteArray = calculateHmac(currentKey, data)

    /**
     * Calculates HMAC SHA256 using the previous key.
     *
     * @param data the [ByteArray] that should be hashed
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class
    )
    fun calculatePreviousHmac(data: ByteArray): ByteArray? = previousKey?.run { calculateHmac(this, data) }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KeyManager::class.java)

        private fun generateHmacKey(): SecretKey =
            KeyGenerator.getInstance("HmacSHA256", "BCFIPS").run {
                init(256, RandomHolder.secureRandom)
                generateKey()
            }

        private fun calculateHmac(key: Key, data: ByteArray): ByteArray =
            Mac.getInstance("HmacSHA256", "BCFIPS").run {
                init(key)
                doFinal(data)
            }
    }
}