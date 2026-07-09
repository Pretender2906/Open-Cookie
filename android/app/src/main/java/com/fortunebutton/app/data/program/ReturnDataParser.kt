package com.fortunebutton.app.data.program

import com.fortunebutton.app.util.Borsh
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class FortuneResult(
    val fortuneIndex: Int,
    val totalCalls: Long,
    val callsToday: Int,
)

object ReturnDataParser {

    fun parseFortuneResult(data: ByteArray): FortuneResult {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val fortuneIndex = Borsh.readU16(buf)
        val totalCalls = Borsh.readU32(buf)
        val callsToday = Borsh.readU8(buf)
        return FortuneResult(fortuneIndex, totalCalls, callsToday)
    }
}
