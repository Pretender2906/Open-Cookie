package com.opencookie.app.data.program

import com.opencookie.app.domain.model.ProgramConfig
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.util.Borsh
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AccountDeserializer {

    private const val DISCRIMINATOR_SIZE = 8

    fun deserializeConfig(data: ByteArray): ProgramConfig {
        val buf = wrap(data)
        skipDiscriminator(buf)
        val adminAuthority = Borsh.readPubkey(buf)
        val pendingAdmin = Borsh.readPubkey(buf)
        val priceLamports = Borsh.readU64(buf)
        val treasuryBump = Borsh.readU8(buf)
        val configBump = Borsh.readU8(buf)
        return ProgramConfig(adminAuthority, pendingAdmin, priceLamports, treasuryBump, configBump)
    }

    fun deserializeUserProfile(data: ByteArray): UserProfile {
        val buf = wrap(data)
        skipDiscriminator(buf)
        val owner = Borsh.readPubkey(buf)
        val totalCalls = Borsh.readU32(buf)
        val lastDay = Borsh.readI32(buf)
        val callsToday = Borsh.readU8(buf)
        val bump = Borsh.readU8(buf)
        return UserProfile(owner, totalCalls, lastDay, callsToday, bump)
    }

    private fun wrap(data: ByteArray): ByteBuffer =
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    private fun skipDiscriminator(buf: ByteBuffer) {
        buf.position(buf.position() + DISCRIMINATOR_SIZE)
    }
}
