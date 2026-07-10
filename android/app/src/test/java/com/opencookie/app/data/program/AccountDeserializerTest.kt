package com.opencookie.app.data.program

import com.opencookie.app.domain.model.ProgramConfig
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.util.PublicKey
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
            .put(ByteArray(8) { 0xAB.toByte() })
            .put(admin.bytes)
            .put(pending.bytes)
            .putLong(10_000)
            .putShort(100)
            .put(7)
            .put(9)
            .array()

        val config = AccountDeserializer.deserializeConfig(data)

        assertEquals(admin, config.adminAuthority)
        assertEquals(pending, config.pendingAdmin)
        assertEquals(10_000L, config.priceLamports)
        assertEquals(100, config.maxCallsPerDay)
        assertEquals(7, config.treasuryBump)
        assertEquals(9, config.configBump)
    }

    @Test
    fun deserializeUserProfile_readsCallsTodayAsU16() {
        val owner = PublicKey(ByteArray(32) { 3 })
        val data = ByteBuffer.allocate(8 + 32 + 4 + 4 + 2 + 1)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(ByteArray(8))
            .put(owner.bytes)
            .putInt(42)
            .putInt(20_260_710)
            .putShort(256)
            .put(5)
            .array()

        val profile = AccountDeserializer.deserializeUserProfile(data)

        assertEquals(
            UserProfile(owner, 42, 20_260_710, 256, 5),
            profile,
        )
    }
}
