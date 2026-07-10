package com.opencookie.app.data.program

import com.opencookie.app.util.PublicKey

data class TransactionInstruction(
    val programId: PublicKey,
    val keys: List<AccountMeta>,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is TransactionInstruction && programId == other.programId &&
            keys == other.keys && data.contentEquals(other.data)

    override fun hashCode(): Int = programId.hashCode() xor keys.hashCode() xor data.contentHashCode()
}

data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean,
)

sealed interface ResolvedAction {
    data object BreakCookie : ResolvedAction
    data object InitializeUser : ResolvedAction
}
