package com.fortunebutton.admin.data.program

import com.fortunebutton.admin.util.Borsh
import com.fortunebutton.admin.util.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class TransactionInstruction(
    val programId: PublicKey,
    val keys: List<AccountMeta>,
    val data: ByteArray,
)

data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean,
)

data class UpdateConfigParams(
    val pendingAdmin: PublicKey,
    val priceLamports: Long,
)

object AdminInstructionBuilder {

    fun buildInitializeConfig(admin: PublicKey, params: UpdateConfigParams): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("initialize_config")
        val (configPda) = ProgramAddresses.config()
        val (treasuryPda) = ProgramAddresses.treasuryVault()
        val (programDataPda) = ProgramAddresses.programData()

        val argsBuf = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
        Borsh.writePubkey(argsBuf, params.pendingAdmin)
        Borsh.writeU64(argsBuf, params.priceLamports)

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(admin, isSigner = true, isWritable = true),
                AccountMeta(configPda, isSigner = false, isWritable = true),
                AccountMeta(treasuryPda, isSigner = false, isWritable = true),
                AccountMeta(programDataPda, isSigner = false, isWritable = false),
                AccountMeta(PublicKey.SYSTEM_PROGRAM, isSigner = false, isWritable = false),
            ),
            data = discriminator + argsBuf.array(),
        )
    }

    fun buildUpdateConfig(admin: PublicKey, params: UpdateConfigParams): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("update_config")
        val (configPda) = ProgramAddresses.config()

        val argsBuf = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
        Borsh.writePubkey(argsBuf, params.pendingAdmin)
        Borsh.writeU64(argsBuf, params.priceLamports)

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(admin, isSigner = true, isWritable = false),
                AccountMeta(configPda, isSigner = false, isWritable = true),
            ),
            data = discriminator + argsBuf.array(),
        )
    }

    fun buildAcceptAdmin(pendingAdmin: PublicKey): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("accept_admin")
        val (configPda) = ProgramAddresses.config()

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(pendingAdmin, isSigner = true, isWritable = false),
                AccountMeta(configPda, isSigner = false, isWritable = true),
            ),
            data = discriminator,
        )
    }

    fun buildWithdrawTreasury(
        admin: PublicKey,
        destination: PublicKey,
        lamports: Long,
    ): TransactionInstruction {
        val discriminator = AnchorDiscriminator.forInstruction("withdraw_treasury")
        val (configPda) = ProgramAddresses.config()
        val (treasuryPda) = ProgramAddresses.treasuryVault()

        val argsBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        Borsh.writeU64(argsBuf, lamports)

        return TransactionInstruction(
            programId = ProgramAddresses.PROGRAM_ID,
            keys = listOf(
                AccountMeta(admin, isSigner = true, isWritable = false),
                AccountMeta(configPda, isSigner = false, isWritable = false),
                AccountMeta(treasuryPda, isSigner = false, isWritable = true),
                AccountMeta(destination, isSigner = false, isWritable = true),
                AccountMeta(PublicKey.SYSTEM_PROGRAM, isSigner = false, isWritable = false),
            ),
            data = discriminator + argsBuf.array(),
        )
    }
}
