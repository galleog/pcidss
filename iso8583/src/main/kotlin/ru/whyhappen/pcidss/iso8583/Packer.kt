package ru.whyhappen.pcidss.iso8583

/**
 * Interface to serialize and deserialize objects to/from bytes.
 */
interface Packer {
    /**
     * Serializes the object into its binary representation.
     */
    fun pack(): ByteArray
    /**
     * Deserializes the object from its binary representation.
     *
     * @return the number of read bytes
     */
    fun unpack(bytes: ByteArray): Int
}