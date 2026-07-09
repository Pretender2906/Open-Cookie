package com.fortunebutton.app.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Borsh {

    fun readU8(buf: ByteBuffer): Int = buf.get().toInt() and 0xFF

    fun readU16(buf: ByteBuffer): Int = buf.order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

    fun readU32(buf: ByteBuffer): Long = buf.order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL

    fun readI32(buf: ByteBuffer): Int = buf.order(ByteOrder.LITTLE_ENDIAN).int

    fun readU64(buf: ByteBuffer): Long = buf.order(ByteOrder.LITTLE_ENDIAN).long

    fun readPubkey(buf: ByteBuffer): PublicKey {
        val bytes = ByteArray(32)
        buf.get(bytes)
        return PublicKey(bytes)
    }

    fun writeU8(buf: ByteBuffer, value: Int) {
        buf.put(value.toByte())
    }

    fun writeU32(buf: ByteBuffer, value: Long) {
        buf.order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt())
    }

    fun writeU64(buf: ByteBuffer, value: Long) {
        buf.order(ByteOrder.LITTLE_ENDIAN).putLong(value)
    }

    fun writePubkey(buf: ByteBuffer, pubkey: PublicKey) {
        buf.put(pubkey.bytes)
    }
}
