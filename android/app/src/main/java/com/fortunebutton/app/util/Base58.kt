package com.fortunebutton.app.util

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128) { -1 }.also { arr ->
        ALPHABET.forEachIndexed { i, c -> arr[c.code] = i }
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val temp = input.copyOf()
        var zeros = 0
        while (zeros < temp.size && temp[zeros] == 0.toByte()) zeros++

        val encoded = CharArray(temp.size * 2)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < temp.size) {
            val remainder = divmod(temp, inputStart, 256, 58)
            if (temp[inputStart] == 0.toByte()) inputStart++
            encoded[--outputStart] = ALPHABET[remainder]
        }
        while (outputStart < encoded.size && encoded[outputStart] == ALPHABET[0]) outputStart++
        repeat(zeros) { encoded[--outputStart] = ALPHABET[0] }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            require(digit >= 0) { "Invalid Base58 character: $c" }
            input58[i] = digit.toByte()
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros] == 0.toByte()) zeros++

        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            val remainder = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart] == 0.toByte()) inputStart++
            decoded[--outputStart] = remainder.toByte()
        }
        while (outputStart < decoded.size && decoded[outputStart] == 0.toByte()) outputStart++
        return ByteArray(zeros + decoded.size - outputStart).also {
            System.arraycopy(decoded, outputStart, it, zeros, decoded.size - outputStart)
        }
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Int {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder
    }
}
