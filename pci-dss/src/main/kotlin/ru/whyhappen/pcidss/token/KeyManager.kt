package ru.whyhappen.pcidss.token

import org.bouncycastle.crypto.EntropySourceProvider
import org.bouncycastle.crypto.fips.FipsDRBG
import org.bouncycastle.crypto.util.BasicEntropySourceProvider
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manager for the keystore and secret key the application uses to hash ISO8583 secret fields.
 * Uses Bouncy Castle FIPS keystore to save keys.
 */
class KeyManager (
    /**
     * Path to the keystore to save secret keys.
     */
    keystorePath: String,
    /**
     * Keystore password.
     */
    private val keystorePassword: String
) {
    val path: Path = Path.of(keystorePath)

    /**
     * Verifies if the keystore exists.
     */
    val keystoreExists: Boolean
        get() = Files.isRegularFile(path)

    /**
     * Gets the secret key from the keystore.
     *
     * @param keyAlias the alias for the new secret key
     * @param keyPassword the password to protect the key
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    fun getSecretKey(keyAlias: String, keyPassword: String): Key {
        require(keystoreExists) { "Keystore $path doesn't exist" }

        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        Files.newInputStream(path, StandardOpenOption.READ).use {
            keystore.load(it, keystorePassword.toCharArray())
        }

        return keystore.getKey(keyAlias, keyPassword.toCharArray())
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     *
     * @param currentKeyAlias the alias for the new secret key
     * @param previousKeyAlias the alias to store the previous secret key
     * @param keyPassword the password to protect the keys
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    fun updateSecretKey(currentKeyAlias: String, previousKeyAlias: String, keyPassword: String) {
        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        if (keystoreExists) {
            Files.newInputStream(path, StandardOpenOption.READ).use {
                keystore.load(it, keystorePassArray)
            }

            // store the previous key under the previous key alias
            val previousKey = keystore.getKey(currentKeyAlias, keyPassArray)
            keystore.setKeyEntry(currentKeyAlias, generateKey(), keyPassArray, null)
            keystore.setKeyEntry(previousKeyAlias, previousKey, keyPassArray, null)
        } else {
            keystore.load(null, null)

            keystore.setKeyEntry(currentKeyAlias, generateKey(), keyPassArray, null)
        }

        Files.newOutputStream(
            path,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
        ).use {
            keystore.store(it, keystorePassArray)
        }
    }

    private fun generateKey(): SecretKey = with(KeyGenerator.getInstance("AES", "BCFIPS")) {
        init(256, secureRandom)
        generateKey()
    }

    companion object {
        private val nonce = ByteArray(32)

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
}