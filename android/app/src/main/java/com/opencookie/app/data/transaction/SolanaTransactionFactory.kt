package com.opencookie.app.data.transaction

import com.opencookie.app.data.program.AccountMeta
import com.opencookie.app.data.program.TransactionInstruction
import com.opencookie.app.util.Base58
import com.opencookie.app.util.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaTransactionFactory @Inject constructor(
    private val computeBudgetPolicy: ComputeBudgetPolicy,
) {
    fun buildSerializedTransaction(
        instructions: List<TransactionInstruction>,
        feePayer: PublicKey,
        recentBlockhash: String,
        isHeavy: Boolean = false,
    ): ByteArray {
        require(instructions.isNotEmpty()) { "Transaction must contain at least one instruction" }

        val budget = computeBudgetPolicy.resolve(isHeavy)
        val instructionsWithBudget = ComputeBudgetInstructions.prependTo(
            instructions = instructions,
            computeUnitLimit = budget.limit,
            computeUnitPriceMicrolamports = budget.priceMicrolamports,
        )

        val mergedMetas = mutableListOf<AccountMeta>()
        mergedMetas.add(AccountMeta(feePayer, isSigner = true, isWritable = true))
        for (instruction in instructionsWithBudget) {
            mergedMetas.addAll(instruction.keys)
            mergedMetas.add(AccountMeta(instruction.programId, isSigner = false, isWritable = false))
        }

        val metaByPubkey = linkedMapOf<PublicKey, AccountMeta>()
        for (meta in mergedMetas) {
            val existing = metaByPubkey[meta.pubkey]
            metaByPubkey[meta.pubkey] = existing?.let {
                AccountMeta(meta.pubkey, it.isSigner || meta.isSigner, it.isWritable || meta.isWritable)
            } ?: meta
        }

        val uniqueMetas = metaByPubkey.values.toList()
        val signers = uniqueMetas.filter { it.isSigner }.sortedBy { if (it.isWritable) 0 else 1 }
        val nonsigners = uniqueMetas.filter { !it.isSigner }.sortedBy { if (it.isWritable) 0 else 1 }
        val sortedKeys = signers + nonsigners
        val finalKeys = if (sortedKeys[0].pubkey == feePayer) {
            sortedKeys
        } else {
            val feePayerMeta = sortedKeys.find { it.pubkey == feePayer }
                ?: error("Fee payer must appear in transaction account list")
            listOf(feePayerMeta) + sortedKeys.filter { it.pubkey != feePayer }
        }

        val numSigners = finalKeys.count { it.isSigner }
        val numReadonlySigners = finalKeys.count { it.isSigner && !it.isWritable }
        val numReadonlyNonSigners = finalKeys.count { !it.isSigner && !it.isWritable }
        val keyIndex = finalKeys.withIndex().associate { (i, meta) -> meta.pubkey to i }
        val blockhashBytes = Base58.decode(recentBlockhash)

        val messageBody = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
        messageBody.put(numSigners.toByte())
        messageBody.put(numReadonlySigners.toByte())
        messageBody.put(numReadonlyNonSigners.toByte())
        writeCompactU16(messageBody, finalKeys.size)
        for (key in finalKeys) messageBody.put(key.pubkey.bytes)
        messageBody.put(blockhashBytes)
        writeCompactU16(messageBody, instructionsWithBudget.size)
        for (instruction in instructionsWithBudget) {
            messageBody.put(keyIndex[instruction.programId]!!.toByte())
            writeCompactU16(messageBody, instruction.keys.size)
            for (key in instruction.keys) messageBody.put(keyIndex[key.pubkey]!!.toByte())
            writeCompactU16(messageBody, instruction.data.size)
            messageBody.put(instruction.data)
        }

        val messageBytes = ByteArray(messageBody.position())
        messageBody.flip()
        messageBody.get(messageBytes)

        val txBuf = ByteBuffer.allocate(1 + 64 + messageBytes.size)
        writeCompactU16(txBuf, 1)
        txBuf.put(ByteArray(64))
        txBuf.put(messageBytes)
        txBuf.flip()
        return ByteArray(txBuf.remaining()).also { txBuf.get(it) }
    }

    private fun writeCompactU16(buf: ByteBuffer, value: Int) {
        var v = value
        while (true) {
            val b = v and 0x7F
            v = v shr 7
            if (v == 0) {
                buf.put(b.toByte())
                break
            } else {
                buf.put((b or 0x80).toByte())
            }
        }
    }
}
