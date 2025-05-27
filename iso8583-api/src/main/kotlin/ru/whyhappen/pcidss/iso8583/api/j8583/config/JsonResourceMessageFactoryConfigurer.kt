package ru.whyhappen.pcidss.iso8583.api.j8583.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.MessageFactory
import org.springframework.core.io.Resource
import ru.whyhappen.pcidss.iso8583.api.j8583.config.model.Iso8583Config

/**
 * Implementation of [MessageFactoryConfigurer] that uses JSON resources for configuration.
 */
abstract class JsonResourceMessageFactoryConfigurer<T: IsoMessage>(
    private val objectMapper: ObjectMapper,
    private val resources: List<Resource>
) : AbstractMessageFactoryConfigurer<T>() {
    override fun configure(messageFactory: MessageFactory<T>) {
        applyConfigs(messageFactory, resources.map { resource ->
            resource.inputStream.use {
                objectMapper.readValue(it, Iso8583Config::class.java)
            }
        })
    }
}