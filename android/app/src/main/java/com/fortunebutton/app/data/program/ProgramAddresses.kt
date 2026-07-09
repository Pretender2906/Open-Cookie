package com.fortunebutton.app.data.program

import com.fortunebutton.app.util.PublicKey

object ProgramAddresses {

    val PROGRAM_ID = PublicKey("FrtnBtnPK86hRM2pMF7FesE38MYDi59z9dMuNyfxiq")

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
