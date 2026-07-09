package com.fortunebutton.admin.data.program

import java.security.MessageDigest

object AnchorDiscriminator {
    fun forInstruction(instructionName: String): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("global:$instructionName".toByteArray())
        return hash.copyOfRange(0, 8)
    }
}
