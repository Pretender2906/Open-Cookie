package com.opencookie.app.data.program

import com.opencookie.app.util.PublicKey

object ProgramAddresses {

    val PROGRAM_ID = PublicKey("CooknomesWJ3KdJUUYfgXBycS19hqDNS9riBavo2Gfuf")

    fun config(): Pair<PublicKey, Int> =
        PublicKey.findProgramAddress(listOf("config".toByteArray()), PROGRAM_ID)

    fun treasuryVault(): Pair<PublicKey, Int> =
        PublicKey.findProgramAddress(listOf("treasury-vault".toByteArray()), PROGRAM_ID)

    fun userProfile(wallet: PublicKey): Pair<PublicKey, Int> =
        PublicKey.findProgramAddress(
            listOf("user".toByteArray(), wallet.bytes),
            PROGRAM_ID,
        )
}
