package ru.whyhappen.pcidss.iso8583.spec

/**
 * Interface to pack/unpack field values.
 */
interface Packer {
    /**
     * Packs the specified byte array so that it can be used as a field's binary representation.
     */
    fun pack(bytes: ByteArray): ByteArray
    /**
     * Unpacks the specified byte array to a field's internal form.
     *
     * @return the resulted byte array and the number of read bytes
     */
    fun unpack(bytes: ByteArray): Pair<ByteArray, Int>
}