package com.opencookie.app.data.program

import com.opencookie.app.util.PublicKey
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
        if (action is ResolvedAction.CloseUser) {
            return listOf(buildCloseUser(wallet))
        }

        val main = when (action) {
            ResolvedAction.BreakCookie -> buildBreakCookie(wallet)
            ResolvedAction.InitializeUser -> error("handled above")
            ResolvedAction.CloseUser -> error("handled above")
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

    fun buildBreakCookie(wallet: PublicKey): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("break_cookie")
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

    fun buildCloseUser(wallet: PublicKey): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("close_user")
        val (configPda) = ProgramAddresses.config()
        val (userProfilePda) = ProgramAddresses.userProfile(wallet)

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(wallet, isSigner = true, isWritable = true),
                AccountMeta(configPda, isSigner = false, isWritable = false),
                AccountMeta(userProfilePda, isSigner = false, isWritable = true),
            ),
            data = discriminator,
        )
    }
}
