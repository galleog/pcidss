package ru.whyhappen.pcidss.iso8583.pad

/**
 * [Padder] that does nothing.
 */
class NoOpPadder : Padder {
    override fun pad(bytes: ByteArray, length: Int): ByteArray = bytes

    override fun unpad(bytes: ByteArray): ByteArray = bytes
}