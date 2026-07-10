package com.opencookie.app.util

import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.security.MessageDigest

data class PublicKey(val bytes: ByteArray) {

    init {
        require(bytes.size == SIZE) { "PublicKey must be $SIZE bytes, got ${bytes.size}" }
    }

    constructor(base58: String) : this(Base58.decode(base58))

    fun toBase58(): String = Base58.encode(bytes)

    fun toTruncated(): String {
        val b58 = toBase58()
        return if (b58.length > 8) "${b58.take(4)}...${b58.takeLast(4)}" else b58
    }

    override fun equals(other: Any?): Boolean =
        other is PublicKey && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = toBase58()

    companion object {
        const val SIZE = 32

        val SYSTEM_PROGRAM = PublicKey("11111111111111111111111111111111")
        val ZERO = PublicKey(ByteArray(SIZE))

        fun isValid(input: String): Boolean = runCatching {
            Base58.decode(input).size == SIZE
        }.getOrDefault(false)

        fun findProgramAddress(seeds: List<ByteArray>, programId: PublicKey): Pair<PublicKey, Int> {
            var nonce = 255
            while (nonce != 0) {
                val candidate = createProgramAddress(seeds, nonce, programId)
                if (candidate != null) return candidate to nonce
                nonce--
            }
            error("Could not find PDA")
        }

        private fun createProgramAddress(
            seeds: List<ByteArray>,
            bump: Int,
            programId: PublicKey,
        ): PublicKey? {
            val hash = MessageDigest.getInstance("SHA-256")
            seeds.forEach { hash.update(it) }
            hash.update(byteArrayOf(bump.toByte()))
            hash.update(programId.bytes)
            hash.update("ProgramDerivedAddress".toByteArray())
            val candidate = hash.digest()
            return if (isOnCurve(candidate)) null else PublicKey(candidate)
        }

        fun isOnCurve(point: ByteArray): Boolean {
            if (point.size != SIZE) return false
            return Ed25519.validatePublicKeyPartial(point, 0)
        }
    }
}
