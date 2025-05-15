package ru.whyhappen.pcidss.config

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.whyhappen.pcidss.core.service.KeystoreManager
import java.io.IOException
import java.security.Security
import java.security.UnrecoverableKeyException
import kotlin.system.exitProcess

/**
 * Class to initialize the keystore and update the secret key.
 */
@Component
class KeystoreInitializer(private val keystoreManager: KeystoreManager) : CommandLineRunner {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KeystoreInitializer::class.java)
    }

    /**
     * Gets the secret keys used to hash ISO8583 sensitive fields if the keystore exists or creates a new one otherwise.
     */
    override fun run(vararg args: String) {
        // add support for Bouncy Castle FIPS
        Security.addProvider(BouncyCastleFipsProvider())

        runCatching {
            keystoreManager.init()
        }.onFailure { e ->
            val msg = when (e) {
                is IOException -> {
                    if (e.cause is UnrecoverableKeyException) "Password for keystore '${keystoreManager.keystoreFile}' is invalid"
                    else "Keystore '${keystoreManager.keystoreFile}' can't be loaded"
                }

                is UnrecoverableKeyException -> "Key password is invalid"
                else -> throw e
            }

            logger.error(msg, e)
            println(msg)

            // failed to get the secret key => exit the application
            exitProcess(1)
        }
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields regularly.
     */
    @Scheduled(cron = "\${keystore.update-cron:@yearly}")
    @SchedulerLock(name = "updateSecretKey", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1H")
    fun updateSecretKey() {
        keystoreManager.updateSecretKey()
    }
}