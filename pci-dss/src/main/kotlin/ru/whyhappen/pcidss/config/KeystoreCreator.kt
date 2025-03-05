package ru.whyhappen.pcidss.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import ru.whyhappen.pcidss.token.KeyManager

/**
 * Creates the keystore to save the secret key used to hash ISO8583 sensitive fields.
 */
@Component
class KeystoreCreator(private val keyManager: KeyManager, private val properties: Iso8583Properties) :
    CommandLineRunner {
    override fun run(vararg args: String) {
        if (!keyManager.keystoreExists) {
            keyManager.updateSecretKey(
                properties.keystore.currentKeyAlias,
                properties.keystore.previousKeyAlias,
                properties.keystore.keyPassword
            )

            logger.info("Keystore ${keyManager.path} has been created")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(KeystoreCreator::class.java)
    }
}