package ru.whyhappen.pcidss.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for [WebClient]
 */
@Configuration(proxyBeanMethods = false)
class WebClientConfiguration {
    @Bean
    fun webClient(builder: WebClient.Builder, @Value("\${antifraud.base-url}") baseUrl: String) =
        builder.baseUrl(baseUrl)
            .build()
}