package com.opencookie.app.data.transaction

import com.opencookie.app.data.program.TransactionInstruction
import com.opencookie.app.util.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object ComputeBudgetInstructions {
    private val PROGRAM_ID = PublicKey("ComputeBudget111111111111111111111111111111")
    private const val IX_SET_COMPUTE_UNIT_LIMIT: Byte = 2
    private const val IX_SET_COMPUTE_UNIT_PRICE: Byte = 3

    fun prependTo(
        instructions: List<TransactionInstruction>,
        computeUnitLimit: Int,
        computeUnitPriceMicrolamports: Long,
    ): List<TransactionInstruction> = listOf(
        setComputeUnitLimit(computeUnitLimit),
        setComputeUnitPrice(computeUnitPriceMicrolamports),
    ) + instructions

    private fun setComputeUnitLimit(units: Int): TransactionInstruction {
        val data = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        data.put(IX_SET_COMPUTE_UNIT_LIMIT)
        data.putInt(units)
        return TransactionInstruction(PROGRAM_ID, emptyList(), data.array())
    }

    private fun setComputeUnitPrice(microLamports: Long): TransactionInstruction {
        val data = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
        data.put(IX_SET_COMPUTE_UNIT_PRICE)
        data.putLong(microLamports)
        return TransactionInstruction(PROGRAM_ID, emptyList(), data.array())
    }
}
