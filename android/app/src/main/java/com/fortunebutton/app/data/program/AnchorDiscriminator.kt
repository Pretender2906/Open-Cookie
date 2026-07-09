package com.fortunebutton.app.data.program

import java.security.MessageDigest

object AnchorDiscriminator {

    fun forAccount(accountName: String): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("account:$accountName".toByteArray())
        return hash.copyOfRange(0, 8)
    }

    fun forInstruction(instructionName: String): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("global:$instructionName".toByteArray())
        return hash.copyOfRange(0, 8)
    }
}
