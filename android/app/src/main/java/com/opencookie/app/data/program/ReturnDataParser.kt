package com.opencookie.app.data.program

import com.opencookie.app.util.Borsh
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class CookieResult(
    val messageIndex: Int,
    val totalCalls: Long,
    val callsToday: Int,
)

object ReturnDataParser {

    fun parseCookieResult(data: ByteArray): CookieResult {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val messageIndex = Borsh.readU16(buf)
        val totalCalls = Borsh.readU32(buf)
        val callsToday = Borsh.readU16(buf)
        return CookieResult(messageIndex, totalCalls, callsToday)
    }
}
