@file:JvmName("ServerConfiguration")

package ru.whyhappen.pcidss.iso8583.reactor.server

import ru.whyhappen.pcidss.iso8583.reactor.ConnectorConfiguration

@SuppressWarnings("WeakerAccess")
class ServerConfiguration(builder: Builder) : ConnectorConfiguration(builder) {
    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @Suppress("unused")
        @JvmStatic
        fun getDefault(): ServerConfiguration = newBuilder().build()
    }

    class Builder : ConnectorConfiguration.Builder<Builder>() {
        fun build(): ServerConfiguration = ServerConfiguration(this)
    }
}
