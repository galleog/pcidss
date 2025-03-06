package ru.whyhappen.pcidss.config

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import ru.whyhappen.pcidss.bc.KeyManager
import java.io.IOException
import java.security.Security
import java.security.UnrecoverableKeyException
import kotlin.system.exitProcess

/**
 * Verifies the specified passwords to get the secret key used to hash ISO8583 sensitive fields
 * if the keystore exists or creates a new one otherwise.
 */
@Component
class KeystoreCreator(private val keyManager: KeyManager, private val properties: KeystoreProperties) :
    CommandLineRunner {
    override fun run(vararg args: String) {
        // add support for Bouncy Castle FIPS
        Security.addProvider(BouncyCastleFipsProvider())

        if (keyManager.keystoreExists) {
            // check specified passwords
            runCatching {
                keyManager.getSecretKey(properties.currentKeyAlias, properties.password)
            }.onFailure { e ->
                val msg = when (e) {
                    is IOException -> {
                        if (e.cause is UnrecoverableKeyException) "Password for keystore '${keyManager.path}' is invalid"
                        else "Keystore '${keyManager.path}' can't be loaded"
                    }

                    is UnrecoverableKeyException -> "Key password is invalid"
                    else -> throw e
                }

                logger.error(msg, e)
                println(msg)

                // failed to get the secret key => exit the application
                exitProcess(1)
            }
        } else {
            // create new keystore
            keyManager.updateSecretKey(properties.currentKeyAlias, properties.previousKeyAlias, properties.keyPassword)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KeystoreCreator::class.java)
    }
}