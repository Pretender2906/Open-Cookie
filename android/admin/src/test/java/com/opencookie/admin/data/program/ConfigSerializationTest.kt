package com.opencookie.admin.data.program

import com.opencookie.admin.util.Borsh
import com.opencookie.admin.util.PublicKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AccountDeserializerTest {

    @Test
    fun deserializeConfig_readsMaxCallsPerDayAfterPrice() {
        val admin = PublicKey(ByteArray(32) { 1 })
        val pending = PublicKey(ByteArray(32) { 2 })
        val data = ByteBuffer.allocate(8 + 32 + 32 + 8 + 2 + 1 + 1)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(ByteArray(8))
            .put(admin.bytes)
            .put(pending.bytes)
            .putLong(7_500)
            .putShort(50)
            .put(4)
            .put(6)
            .array()

        val config = AccountDeserializer.deserializeConfig(data)

        assertEquals(
            ProtocolConfig(
                adminAuthority = admin,
                pendingAdmin = pending,
                priceLamports = 7_500,
                maxCallsPerDay = 50,
                treasuryBump = 4,
                configBump = 6,
            ),
            config,
        )
    }
}

class AdminInstructionBuilderTest {

    @Test
    fun buildUpdateConfig_serializesMaxCallsPerDay() {
        val admin = PublicKey(ByteArray(32) { 9 })
        val pending = PublicKey.DEFAULT
        val params = UpdateConfigParams(
            pendingAdmin = pending,
            priceLamports = 10_000,
            maxCallsPerDay = 100,
        )

        val instruction = AdminInstructionBuilder.buildUpdateConfig(admin, params)
        val args = instruction.data.copyOfRange(8, instruction.data.size)
        val buf = ByteBuffer.wrap(args).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(pending, Borsh.readPubkey(buf))
        assertEquals(10_000L, Borsh.readU64(buf))
        assertEquals(100, Borsh.readU16(buf))
    }
}
