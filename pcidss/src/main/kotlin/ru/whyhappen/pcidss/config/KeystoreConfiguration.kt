package ru.whyhappen.pcidss.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.service.KeystoreManager
import ru.whyhappen.pcidss.service.bcfips.BcFipsKeystoreManager

/**
 * Configuration for the keystore for secret keys used to cipher sensitive data of ISO messages.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KeystoreProperties::class)
class KeystoreConfiguration {
    @Bean
    @ConditionalOnMissingBean(KeystoreManager::class)
    fun keystoreManager(properties: KeystoreProperties) = BcFipsKeystoreManager(
        properties.path,
        properties.password,
        properties.keyPassword,
        properties.currentKeyAlias,
        properties.previousKeyAlias
    )
}