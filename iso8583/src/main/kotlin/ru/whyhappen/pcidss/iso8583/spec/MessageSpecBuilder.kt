package ru.whyhappen.pcidss.iso8583.spec

/**
 * Builder for [MessageSpec].
 */
interface MessageSpecBuilder {
    /**
     * Builds a message spec.
     */
    fun build(): MessageSpec
}