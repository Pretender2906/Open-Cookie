package com.fortunebutton.admin.data.program

import com.fortunebutton.admin.util.Borsh
import com.fortunebutton.admin.util.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ProtocolConfig(
    val adminAuthority: PublicKey,
    val pendingAdmin: PublicKey,
    val priceLamports: Long,
    val treasuryBump: Int,
    val configBump: Int,
) {
    val hasPendingAdmin: Boolean get() = !pendingAdmin.isDefault()
}

object AccountDeserializer {
    private const val DISCRIMINATOR_SIZE = 8

    fun deserializeConfig(data: ByteArray): ProtocolConfig {
        val buf = wrap(data)
        skipDiscriminator(buf)
        val adminAuthority = Borsh.readPubkey(buf)
        val pendingAdmin = Borsh.readPubkey(buf)
        val priceLamports = Borsh.readU64(buf)
        val treasuryBump = Borsh.readU8(buf)
        val configBump = Borsh.readU8(buf)

        return ProtocolConfig(
            adminAuthority = adminAuthority,
            pendingAdmin = pendingAdmin,
            priceLamports = priceLamports,
            treasuryBump = treasuryBump,
            configBump = configBump,
        )
    }

    private fun wrap(data: ByteArray): ByteBuffer =
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    private fun skipDiscriminator(buf: ByteBuffer) {
        buf.position(buf.position() + DISCRIMINATOR_SIZE)
    }
}
