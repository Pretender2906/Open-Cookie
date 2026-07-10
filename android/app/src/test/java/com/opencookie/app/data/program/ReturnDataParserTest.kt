package com.opencookie.app.data.program

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReturnDataParserTest {

    @Test
    fun parseCookieResult_readsU16CallsToday() {
        val data = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(999)
            .putInt(42)
            .putShort(256)
            .array()

        val result = ReturnDataParser.parseCookieResult(data)

        assertEquals(CookieResult(999, 42, 256), result)
    }
}
