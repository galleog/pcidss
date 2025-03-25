package ru.whyhappen.pcidss.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.bc.KeyManager

/**
 * Configuration for keystore.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KeystoreProperties::class)
class KeystoreConfiguration {
    @Bean
    fun keyManager(properties: KeystoreProperties) = KeyManager(
        properties.path,
        properties.password,
        properties.keyPassword,
        properties.currentKeyAlias,
        properties.previousKeyAlias
    )
}