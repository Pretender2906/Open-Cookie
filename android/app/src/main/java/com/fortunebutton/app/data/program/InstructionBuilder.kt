package com.fortunebutton.app.data.program

import com.fortunebutton.app.util.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstructionBuilder @Inject constructor() {

    fun buildInstructions(
        action: ResolvedAction,
        wallet: PublicKey,
        prependInitializeUser: Boolean,
    ): List<TransactionInstruction> {
        if (action is ResolvedAction.InitializeUser) {
            return listOf(buildInitializeUser(wallet))
        }

        val main = when (action) {
            ResolvedAction.Fortune -> buildFortune(wallet)
            ResolvedAction.InitializeUser -> error("handled above")
        }

        return if (prependInitializeUser) {
            listOf(buildInitializeUser(wallet), main)
        } else {
            listOf(main)
        }
    }

    fun buildInitializeUser(wallet: PublicKey): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("initialize_user")
        val (configPda) = ProgramAddresses.config()
        val (userProfilePda) = ProgramAddresses.userProfile(wallet)

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(wallet, isSigner = true, isWritable = true),
                AccountMeta(configPda, isSigner = false, isWritable = false),
                AccountMeta(userProfilePda, isSigner = false, isWritable = true),
                AccountMeta(PublicKey.SYSTEM_PROGRAM, isSigner = false, isWritable = false),
            ),
            data = discriminator,
        )
    }

    fun buildFortune(wallet: PublicKey): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("fortune")
        val (configPda) = ProgramAddresses.config()
        val (userProfilePda) = ProgramAddresses.userProfile(wallet)
        val (treasuryVaultPda) = ProgramAddresses.treasuryVault()

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(wallet, isSigner = true, isWritable = true),
                AccountMeta(configPda, isSigner = false, isWritable = false),
                AccountMeta(userProfilePda, isSigner = false, isWritable = true),
                AccountMeta(treasuryVaultPda, isSigner = false, isWritable = true),
                AccountMeta(PublicKey.SYSTEM_PROGRAM, isSigner = false, isWritable = false),
            ),
            data = discriminator,
        )
    }
}
