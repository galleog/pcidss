package ru.whyhappen.pcidss.iso8583.spec

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.Resource

/**
 * [MessageSpecBuilder] that uses its JSON representation.
 */
class JsonResourceMessageSpecBuilder(
    private val objectMapper: ObjectMapper,
    private val resource: Resource
) : MessageSpecBuilder {
    override fun build(): MessageSpec =
        resource.inputStream.use {
            objectMapper.readValue(it, MessageSpec::class.java)
        }
}