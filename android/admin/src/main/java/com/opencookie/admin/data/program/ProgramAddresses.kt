package com.opencookie.admin.data.program

import com.opencookie.admin.util.PublicKey

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

    fun programData(): Pair<PublicKey, Int> {
        val bpfLoader = PublicKey("BPFLoaderUpgradeab1e11111111111111111111111")
        return PublicKey.findProgramAddress(listOf(PROGRAM_ID.bytes), bpfLoader)
    }
}
