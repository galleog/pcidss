@file:JvmName("ClientConfiguration")

package ru.whyhappen.pcidss.iso8583.reactor.client

import ru.whyhappen.pcidss.iso8583.reactor.ConnectorConfiguration

/**
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
class ClientConfiguration(builder: Builder) : ConnectorConfiguration(builder) {
    /**
     * Client reconnect interval in milliseconds.
     *
     * @return interval between reconnects, in milliseconds.
     */
    val reconnectInterval: Int = builder.reconnectInterval

    companion object {
        /**
         * Default client reconnect interval in milliseconds.
         */
        const val DEFAULT_RECONNECT_INTERVAL: Int = 100

        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @Suppress("unused")
        @JvmStatic
        fun getDefault(): ClientConfiguration = newBuilder().build()
    }

    @Suppress("unused")
    data class Builder(
        var reconnectInterval: Int = DEFAULT_RECONNECT_INTERVAL,
    ) : ConnectorConfiguration.Builder<Builder>() {
        fun reconnectInterval(reconnectInterval: Int): Builder =
            apply { this.reconnectInterval = reconnectInterval }

        fun build(): ClientConfiguration = ClientConfiguration(this)
    }
}
